package com.poof.crawler.orders.mws.report;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
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
 * @Date 2017年1月10日 下午4:27:08
 */
public class MWSReportOrders extends MWSReportBase {

	private static Logger log = Logger.getLogger(MWSReportOrders.class);
	
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
					
					String reportRequestID = invokeRequestReport(AmazonReportType.GET_FLAT_FILE_ALL_ORDERS_DATA_BY_ORDER_DATE, startTime, null);
					Thread.sleep(SLEEP_GET_REPORT);
					
					String reportID = invokeGetReportRequestList(reportRequestID);
					if(StringUtils.isBlank(reportID))	return;
					
					@SuppressWarnings("unchecked")
					List<String[]> orders = invokeGetReport(AmazonReportType.GET_FLAT_FILE_ALL_ORDERS_DATA_BY_ORDER_DATE, reportID);
					BatchInsert(orders);
				} catch (Exception e) {
					e.printStackTrace();
				}
		        
				long end = System.currentTimeMillis();
				log.info(log.getName() + " : " + String.format("order report done，耗时%s秒", (end - start) / 1000));
			}
		};
		Calendar calendar = Calendar.getInstance();
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH);
		int day = calendar.get(Calendar.DAY_OF_MONTH);
        calendar.set(year, month, day, 9, 00, 00);
		Date date = calendar.getTime();
		Timer timer = new Timer();
		timer.schedule(task, date, 60 * 1000 * 60 * 24);
	}

	private static void BatchInsert(List<String[]> orders) {
		String sql = "insert into bz_orders_report (`amazon-order-id`, `merchant-order-id`, `purchase-date`, `last-updated-date`, `order-status`, `fulfillment-channel`, "
				+ "`sales-channel`, `order-channel`, `url`, `ship-service-level`, `product-name`, `sku`, `asin`, `item-status`, `quantity`, `currency`, `item-price`, `item-tax`, "
				+ "`shipping-price`, `shipping-tax`, `gift-wrap-price`, `gift-wrap-tax`, `item-promotion-discount`, `ship-promotion-discount`, `ship-city`, `ship-state`, "
				+ "`ship-postal-code`, `ship-country`, `promotion-ids`, `is-business-order`) "
				+ "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		Connection conn = null;
		log.info(log.getName() + " : fetching order report size " + orders.size());
		try {
			for (Iterator<String[]> iterator = orders.iterator(); iterator.hasNext();) {
				String[] item = iterator.next();
				if(!AmazonReportOrderStatus.Shipped.value.equalsIgnoreCase(item[4]))
					iterator.remove();
			}
			
			conn = DBUtil.openConnection();
			conn.setAutoCommit(false);
			conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
			PreparedStatement pstmt = conn.prepareStatement(sql);
			int size = orders.size() / 50;
			size = orders.size() % 50 >= 0 ? size + 1 : size; // 5521,5
			for (int i = 0; i < size; i++) { // 6
				for (int j = 0; j < (i == size - 1 ? orders.size() % 50 : 50); j++) {
					String[] bean = orders.get(i * 50 + j);
					
					pstmt.setString(1, bean[0]);
					pstmt.setString(2, bean[1]);
					pstmt.setString(3, bean[2]);
					pstmt.setString(4, bean[3]);
					pstmt.setString(5, bean[4]);
					pstmt.setString(6, bean[5]);
					pstmt.setString(7, bean[6]);
					pstmt.setString(8, bean[7]);
					pstmt.setString(9, bean[8]);
					pstmt.setString(10, bean[9]);
					pstmt.setString(11, bean[10]);
					pstmt.setString(12, bean[11]);
					pstmt.setString(13, bean[12]);
					pstmt.setString(14, bean[13]);
					if(StringUtils.isNotBlank(bean[14]))	pstmt.setInt(15, Integer.valueOf(bean[14]));		else	pstmt.setNull(15, Types.INTEGER);
					pstmt.setString(16, bean[15]);
					if(StringUtils.isNotBlank(bean[16]))	pstmt.setDouble(17, Double.valueOf(bean[16]));	else	pstmt.setNull(17, Types.DOUBLE);
					pstmt.setString(18, bean[17]);
					if(StringUtils.isNotBlank(bean[18]))	pstmt.setDouble(19, Double.valueOf(bean[18]));	else pstmt.setNull(19, Types.DOUBLE);
					pstmt.setString(20, bean[19]);
					if(StringUtils.isNotBlank(bean[20]))	pstmt.setDouble(21, Double.valueOf(bean[20]));	else pstmt.setNull(21, Types.DOUBLE);
					pstmt.setString(22, bean[21]);
					if(StringUtils.isNotBlank(bean[22]))	pstmt.setDouble(23, Double.valueOf(bean[22]));	else pstmt.setNull(23, Types.DOUBLE);
					if(StringUtils.isNotBlank(bean[23]))	pstmt.setDouble(24, Double.valueOf(bean[23]));	else pstmt.setNull(24, Types.DOUBLE);
					pstmt.setString(25, bean[24]);
					pstmt.setString(26, bean[25]);
					pstmt.setString(27, bean[26]);
					pstmt.setString(28, bean[27]);
					pstmt.setString(29, bean[28]);
					pstmt.setBoolean(30, StringUtils.isBlank(bean[29]) || "false".equalsIgnoreCase(bean[29]) ? false	: true);

					pstmt.addBatch();
				}
				pstmt.executeBatch();
				pstmt.clearBatch();
			}
			if (size > 0) {
				// DBUtil.execute(conn, "delete from bz_orders where
				// date_format(str_to_date(order_date,'%b %d, %Y %h:%i:%s
				// %p'),'%Y-%m-%d') = '" + date + "'");
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
		MWSReportOrders.timer();
		/*try {
			List<String[]> orders = processFlatAllOrdersReportFromFile(new File("D:\\poof\\_GET_FLAT_FILE_ALL_ORDERS_DATA_BY_ORDER_DATE_\\20170106151749_3872777903017172.txt"));
			BatchInsert(orders);
		} catch (Exception e) {
			e.printStackTrace();
		}*/
	}
}
