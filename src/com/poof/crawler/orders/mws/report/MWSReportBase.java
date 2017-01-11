package com.poof.crawler.orders.mws.report;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.log4j.Logger;

import com.amazonaws.mws.MarketplaceWebServiceClient;
import com.amazonaws.mws.MarketplaceWebServiceConfig;
import com.amazonaws.mws.MarketplaceWebServiceException;
import com.amazonaws.mws.model.GetReportRequest;
import com.amazonaws.mws.model.GetReportRequestListRequest;
import com.amazonaws.mws.model.GetReportRequestListResponse;
import com.amazonaws.mws.model.GetReportRequestListResult;
import com.amazonaws.mws.model.GetReportResponse;
import com.amazonaws.mws.model.GetReportResult;
import com.amazonaws.mws.model.IdList;
import com.amazonaws.mws.model.ReportRequestInfo;
import com.amazonaws.mws.model.RequestReportRequest;
import com.amazonaws.mws.model.RequestReportResponse;
import com.poof.crawler.orders.mws.MWSOrders;

/**
 * @author wilkey 
 * @mail admin@wilkey.vip
 * @Date 2017年1月10日 下午4:27:12
 */
public abstract class MWSReportBase {

	enum AmazonReportStatus {
		SUBMITTED("_SUBMITTED_"), IN_PROGRESS("_IN_PROGRESS_"), CANCELLED("_CANCELLED_"), DONE("_DONE_"), DONE_NO_DATA("_DONE_NO_DATA_");
		protected String value;

		private AmazonReportStatus(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

	}
	
	enum AmazonReportOrderStatus {
		Canceled("Cancelled"), Default("All orders"), ItemsToShip("Unshipped"), Pending("Pending"), Shipped("Shipped");

		protected String value;

		private AmazonReportOrderStatus(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

	}
	
	enum AmazonReportType {
		GET_FLAT_FILE_OPEN_LISTINGS_DATA("_GET_FLAT_FILE_OPEN_LISTINGS_DATA_"),     //可售商品报告 （“库存报告”）
		GET_MERCHANT_LISTINGS_DATA_BACK_COMPAT("_GET_MERCHANT_LISTINGS_DATA_BACK_COMPAT_"),     //可售商品报告
		GET_MERCHANT_LISTINGS_DATA("_GET_MERCHANT_LISTINGS_DATA_"),     //卖家商品报告 （“在售商品报告”）
		GET_CONVERGED_FLAT_FILE_SOLD_LISTINGS_DATA("_GET_CONVERGED_FLAT_FILE_SOLD_LISTINGS_DATA_"),     //已售商品报告
		GET_AFN_INVENTORY_DATA("_GET_AFN_INVENTORY_DATA_"),     //亚马逊物流库存报告(FBA)
		GET_FBA_FULFILLMENT_CURRENT_INVENTORY_DATA("_GET_FBA_FULFILLMENT_CURRENT_INVENTORY_DATA_"),     //亚马逊物流每日库存历史报告
		GET_SELLER_FEEDBACK_DATA("_GET_SELLER_FEEDBACK_DATA_"),     //业绩报告，返回买家对您的业绩的负面和中性反馈（1 到 3 星）
		GET_MERCHANT_LISTINGS_ALL_DATA("_GET_MERCHANT_LISTINGS_ALL_DATA_"),     //所有listing报告
		GET_FLAT_FILE_ORDERS_DATA("_GET_FLAT_FILE_ORDERS_DATA_"),     //订单报告
		GET_ORDERS_DATA("_GET_ORDERS_DATA_"),     //订单报告
		GET_FLAT_FILE_ALL_ORDERS_DATA_BY_ORDER_DATE("_GET_FLAT_FILE_ALL_ORDERS_DATA_BY_ORDER_DATE_"),     //文本文件格式的、按订单日期排列的订单报告
		GET_PADS_PRODUCT_PERFORMANCE_OVER_TIME_DAILY_DATA_XML("_GET_PADS_PRODUCT_PERFORMANCE_OVER_TIME_DAILY_DATA_XML_"),     //按 SKU 排列的商品广告每天业绩报告，XML 文件
		GET_XML_BROWSE_TREE_DATA("_GET_XML_BROWSE_TREE_DATA_");	//分类树报告

		protected String value;

	    private AmazonReportType(String v) {
	        value = v;
	    }

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

	}
	private static Logger log = Logger.getLogger(MWSReportBase.class);
	protected static String TMP_REPORT_PATH = "";
	protected static String accessKey;
	protected static String secretKey;
	protected static String appName;
	protected static String appVersion;
	protected static String serviceURL;
	protected static String mwsAuthToken;
	protected static String sellerId;
	protected static String marketplaceId;

	protected static MarketplaceWebServiceClient client;
	static {
		try {
			Properties p = new Properties();
			p.load(MWSOrders.class.getResourceAsStream("/config.properties"));
			TMP_REPORT_PATH = p.getProperty("report.path");
			appName = "poof";
			appVersion = "V1.0";
			accessKey = p.getProperty("mws.accessKey");
			secretKey = p.getProperty("mws.secretKey");
			serviceURL = p.getProperty("mws.serviceURL");
			mwsAuthToken = p.getProperty("mws.mwsAuthToken");
			sellerId = p.getProperty("mws.sellerId");
			marketplaceId = p.getProperty("mws.marketplaceId");
			MarketplaceWebServiceConfig config = new MarketplaceWebServiceConfig();
			config.setServiceURL(serviceURL);

			client = new MarketplaceWebServiceClient(accessKey, secretKey, appName, appVersion, config);
		} catch (IOException e) {
			e.printStackTrace();
			log.error(log.getName() + " : program error: " + e);
		}
	}
	public static SimpleDateFormat reportFormat = new SimpleDateFormat("yyyyMMddHHmmss");

	public static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	protected static final int SLEEP_GET_REPORT_REQUEST_LIST_BY_NEXT_TOKEN = 2 * 1000;
	protected static final int SLEEP_GET_REPORT = 60 * 1000;

	protected static final int GET_REPORT_REQUEST_LIST_MAX_COUNT = 100;

	protected static String getReportFilePath(AmazonReportType type) {
		return TMP_REPORT_PATH + "/" + appName + "/" + type.value;
	}

	public static XMLGregorianCalendar convertToXMLGregorianCalendar(String date) throws Exception {
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(date));
		XMLGregorianCalendar gc = null;
		try {
			gc = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return gc;
	}

	public static Date convertToDate(XMLGregorianCalendar cal) throws Exception {
		return cal.toGregorianCalendar().getTime();
	}

	/**
	 * 获取当前系统时间-3分钟
	 */
	public static String getCreatedBefore() {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.MINUTE, -3);
		return dateFormat.format(c.getTime());
	}

	public static String getCurrentTime() {
		return dateFormat.format(new Date());
	}

	public static String genReportNum() {
		return reportFormat.format(new Date());
	}

	/**
	 * Consume the stream and return its Base-64 encoded MD5 checksum.
	 * 
	 * <br/>
	 * Tips: 对下载的报告计算 MD5 校验和
	 */
	public static String computeContentMD5Header(InputStream inputStream) {
		// Consume the stream to compute the MD5 as a side effect.
		DigestInputStream s = null;
		try {
			s = new DigestInputStream(inputStream, MessageDigest.getInstance("MD5"));
			// drain the buffer, as the digest is computed as a side-effect
			byte[] buffer = new byte[8192];
			while (s.read(buffer) > 0)
				;
			return new String(org.apache.commons.codec.binary.Base64.encodeBase64(s.getMessageDigest().digest()), "UTF-8");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (s != null) {
				try {
					s.close();
					s = null;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/*******
	 * step 1
	 *******/
	protected static String invokeRequestReport(AmazonReportType reportType, XMLGregorianCalendar startDate, XMLGregorianCalendar endDate) throws Exception {
		RequestReportRequest request = new RequestReportRequest()
																.withMerchant(sellerId).withMarketplace(marketplaceId)
																.withReportType(reportType.value);
//																.withReportOptions("ShowSalesChannel=true");
		// //只针对_GET_FLAT_FILE_ACTIONABLE_ORDER_DATA_和_GET_FLAT_FILE_ORDERS_DATA_
		if (startDate != null)
			request.setStartDate(startDate);
		if (endDate != null)
			request.setEndDate(endDate);

		RequestReportResponse response = null;
		try {
			response = client.requestReport(request);

		} catch (MarketplaceWebServiceException ex) {
			log.error(log.getName() + " : program error: " + ex);
		}

		return processRequestReportResponse(response);

	}

	/*******
	 * step 2
	 *******/
	protected static String processRequestReportResponse(RequestReportResponse response) {
		String reportRequestID = "";
		if (response != null) {
			ReportRequestInfo result = response.getRequestReportResult().getReportRequestInfo();
			reportRequestID = result.getReportRequestId();
			log.info(log.getName() + " : StartDate: " + result.getStartDate() + " :::: ReportRequestID: " + reportRequestID);
		}
		return reportRequestID;
	}

	/*******
	 * step 3
	 *******/
	protected static String invokeGetReportRequestList(final String reportRequestID) throws Exception {
		String reportID = null;
		GetReportRequestListRequest request = new GetReportRequestListRequest()
				.withMerchant(sellerId)
				.withReportRequestIdList(new IdList(Arrays.asList(reportRequestID)));

		GetReportRequestListResponse response = null;

		try {
			response = client.getReportRequestList(request);
		} catch (MarketplaceWebServiceException ex) {
			log.error(log.getName() + " : program error: " + ex);
		}

		if (response != null) {
			GetReportRequestListResult result = response.getGetReportRequestListResult();
			List<ReportRequestInfo> requestList = result.getReportRequestInfoList();
			if (requestList.size() > 1) {
				log.error(log.getName() + " : program error: Problem in invokeGetReportRequestList, found 2 similar requestID");

			}
			String status = requestList.get(0).getReportProcessingStatus();
			if (status.equals(AmazonReportStatus.DONE.value)) {
				reportID = requestList.get(0).getGeneratedReportId();
				log.info(log.getName() + " : Recieved a reportID: " + reportID + " from the requestList.");
			} else {
				Thread.sleep(SLEEP_GET_REPORT);
				return invokeGetReportRequestList(reportRequestID);
			}
		}

		return reportID;
	}

	/*******
	 * step 4
	 * @return 
	 *******/
	@SuppressWarnings("rawtypes")
	protected static List invokeGetReport(AmazonReportType reportType, String reportId) throws Exception {
		File dir = new File(getReportFilePath(reportType));
		if (!dir.exists())
			dir.mkdirs();
		File file = new File(getReportFilePath(reportType) + "/" + genReportNum() + "_" + reportId + ".txt");
		file.createNewFile();
		GetReportRequest request = new GetReportRequest()
															.withMerchant(sellerId)
															.withMarketplace(marketplaceId).withReportId(reportId);

		GetReportResponse response = null;
		try {
			FileOutputStream report = new FileOutputStream(file);
			request.setReportOutputStream(report);
			log.info(log.getName() + " : Found a report! Creating a new OutPutStream: " + (new String(reportId.toCharArray())) + " --- File: " + file);
			response = client.getReport(request);
		} catch (MarketplaceWebServiceException ex) {
			log.error(log.getName() + " : program error: " + ex);
		} catch (FileNotFoundException ex) {
			log.error(log.getName() + " : program error: " + ex);
		}

		// process the response
		if (response != null) {
			try {
				return processGetReport(response.getGetReportResult(), reportType, file);
			} catch (Exception ex) {
				log.error(log.getName() + " : program error: " + ex);
				throw ex;
			}
		} else {
			log.error(log.getName() + " : program error: Can't process report, no response recieved.");
		}
		return null;
	}

	/*******
	 * step 5
	 *******/
	@SuppressWarnings("rawtypes")
	protected static List processGetReport(GetReportResult result, AmazonReportType reportType, File file) throws Exception {
		System.err.println("orginal md5 : " + result.getMD5Checksum());
		System.err.println("file md5 : " + computeContentMD5Header(new FileInputStream(file)));
		try {
			if (AmazonReportType.GET_FLAT_FILE_ALL_ORDERS_DATA_BY_ORDER_DATE.equals(reportType)) { // 针对商城和卖家平台卖家
				List<String[]> entities = processFlatAllOrdersReportFromFile(file);
				return entities;
			}
		} catch (Exception ex) {
			throw ex;
		}
		return new ArrayList();
	}

	/*******
	 * step 6
	 *******/
	protected static List<String[]> processFlatAllOrdersReportFromFile(File file) throws Exception{
		BufferedReader report = null;
		List<String[]> listEntities = new ArrayList<String[]>();
		try {
			String charset = "UTF-8";
			if(serviceURL.indexOf("eu") != -1 || serviceURL.indexOf("in") != -1){
				charset = "ISO-8859-1";
			} else if (serviceURL.indexOf("jp") != -1) {
				charset = "Shift-JIS";
			}
			report = new BufferedReader(new InputStreamReader( new FileInputStream(file), charset));
			String str;          
			report.readLine();//Skips the first row with title
			if(serviceURL.indexOf("jp") != -1){	//jp站点报表头不一样
				
				while((str=report.readLine())!=null){
					String[] items = str.split("\t");
					listEntities.add(items);
				}
			}else {
				
				while((str=report.readLine())!=null){
					String[] items = str.split("\t");
					listEntities.add(items);
				}
			}
			return listEntities;
		} catch (Exception ex) {
			throw ex;
		} finally{
			if(report!= null) report.close();
		}
	}
}