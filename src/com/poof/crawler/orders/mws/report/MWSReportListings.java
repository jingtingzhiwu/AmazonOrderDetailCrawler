package com.poof.crawler.orders.mws.report;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.poof.crawler.db.DBUtil;


/**
 * @author wilkey 
 * @mail admin@wilkey.vip
 * @Date 2017年1月10日 下午4:27:10
 */
public class MWSReportListings extends MWSReportBase {

	private static Logger log = Logger.getLogger(MWSReportListings.class);
	
	public static synchronized void timer(){
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				long start = System.currentTimeMillis();
				
		        try {
					DatatypeFactory df = DatatypeFactory.newInstance();
					GregorianCalendar calendar = new GregorianCalendar();
					calendar.setTime(new Date());
					calendar.set(Calendar.DATE, calendar.get(Calendar.DATE)-1);	//1天内的报告
					XMLGregorianCalendar startTime = df.newXMLGregorianCalendar(calendar);
					
					String reportRequestID = invokeRequestReport(AmazonReportType.GET_MERCHANT_LISTINGS_ALL_DATA, startTime, null);
					Thread.sleep(SLEEP_GET_REPORT);
					
					String reportID = invokeGetReportRequestList(reportRequestID);
					if(StringUtils.isBlank(reportID))	return;
					
					@SuppressWarnings("unchecked")
					List<String[]> listings = invokeGetReport(AmazonReportType.GET_MERCHANT_LISTINGS_ALL_DATA, reportID);
					BatchInsert(listings);
				} catch (Exception e) {
					e.printStackTrace();
				}
		        
				long end = System.currentTimeMillis();
				log.info(log.getName() + " : " + String.format("listing report done，耗时%s秒", (end - start) / 1000));
			}
		};
		Calendar calendar = Calendar.getInstance();
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH);
		int day = calendar.get(Calendar.DAY_OF_MONTH);
        calendar.set(year, month, day, 9, 15, 00);
		Date date = calendar.getTime();
		Timer timer = new Timer();
		timer.schedule(task, date, 60 * 1000 * 60 * 24);
	}

	private static void BatchInsert(List<String[]> listings) {
		String sql = "insert into bz_listing ( `item-name`,`item-description`,`listing-id`,`seller-sku`,`price`,`quantity`,`open-date`,`image-url`,`item-is-marketplace`,"
				+ "`product-id-type`,`zshop-shipping-fee`,`item-note`,`item-condition`,`zshop-category1`,`zshop-browse-path`,`zshop-storefront-feature`,`asin1`,`asin2`,"
				+ "`asin3`,`will-ship-internationally`,`expedited-shipping`,`zshop-boldface`,`product-id`,`bid-for-featured-placement`,`add-delete`,`pending-quantity`,"
				+ "`fulfillment-channel`,`merchant-shipping-group`) "
				+ "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		Connection conn = null;
		log.info(log.getName() + " : fetching listing report size " + listings.size());
		try {
			conn = DBUtil.openConnection();
			conn.setAutoCommit(false);
			conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
			PreparedStatement pstmt = conn.prepareStatement(sql);
			int size = listings.size() / 50;
			size = listings.size() % 50 >= 0 ? size + 1 : size; // 5521,5
			if (size > 0) {
				 DBUtil.execute(conn, "delete from bz_listing");
			}
			for (int i = 0; i < size; i++) { // 6
				for (int j = 0; j < (i == size - 1 ? listings.size() % 50 : 50); j++) {
					String[] bean = listings.get(i * 50 + j);
					
					pstmt.setString(1, bean[0]);
					pstmt.setString(2, bean[1]);
					pstmt.setString(3, bean[2]);
					pstmt.setString(4, bean[3]);
					if(StringUtils.isNotBlank(bean[4]))	pstmt.setDouble(5, Double.valueOf(bean[4]));	else	pstmt.setNull(5, Types.DOUBLE);
					if(StringUtils.isNotBlank(bean[5]))	pstmt.setInt(6, Integer.valueOf(bean[5]));		else	pstmt.setNull(6, Types.INTEGER);
					pstmt.setString(7, bean[6]);
					pstmt.setString(8, bean[7]);
					pstmt.setString(9, bean[8]);
					pstmt.setString(10, bean[9]);
					pstmt.setString(11, bean[10]);
					pstmt.setString(12, bean[11]);
					if(StringUtils.isNotBlank(bean[12]))	pstmt.setInt(13, Integer.valueOf(bean[12]));		else	pstmt.setNull(13, Types.INTEGER);
					pstmt.setString(14, bean[13]);
					pstmt.setString(15, bean[14]);
					pstmt.setString(16, bean[15]);
					pstmt.setString(17, bean[16]);
					pstmt.setString(18, bean[17]);
					pstmt.setString(19, bean[18]);
					pstmt.setString(20, bean[19]);
					pstmt.setString(21, bean[20]);
					pstmt.setString(22, bean[21]);
					pstmt.setString(23, bean[22]);
					pstmt.setString(24, bean[23]);
					if(StringUtils.isNotBlank(bean[24]))	pstmt.setInt(25, Integer.valueOf(bean[24]));		else	pstmt.setNull(25, Types.INTEGER);
					pstmt.setString(26, bean[25]);
					pstmt.setString(27, bean[26]);
					pstmt.setString(28, bean[27]);

					pstmt.addBatch();
				}
				pstmt.executeBatch();
				pstmt.clearBatch();
			}
			conn.commit();
			pstmt.close();
		} catch (Exception e) {
			e.printStackTrace();
			log.error(log.getName() + " : program error: " + e);
		} finally {
			try {
				DBUtil.closeConnection();
			} catch (SQLException e) {
				e.printStackTrace();
				log.error(log.getName() + " : program error: " + e);
			}
		}
	}
	public static void main(String[] args) {
		System.err.println("starting......");
		MWSReportListings.timer();
		/*try {
			List<String[]> listings = processFlatAllOrdersReportFromFile(new File("D:\\var\\wwwlog\\spider\\poof\\_GET_MERCHANT_LISTINGS_ALL_DATA_\\20170109094153_3896018612017175.txt"));
			BatchInsert(listings);
		} catch (Exception e) {
			e.printStackTrace();
		}*/
	}
}
