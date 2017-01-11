package com.poof.crawler.orders.mws;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.log4j.Logger;

import com.amazonservices.mws.client.MwsUtl;
import com.amazonservices.mws.orders._2013_09_01.MarketplaceWebServiceOrdersClient;
import com.amazonservices.mws.orders._2013_09_01.model.GetOrderRequest;
import com.amazonservices.mws.orders._2013_09_01.model.GetOrderResponse;
import com.amazonservices.mws.orders._2013_09_01.model.GetServiceStatusRequest;
import com.amazonservices.mws.orders._2013_09_01.model.GetServiceStatusResponse;
import com.amazonservices.mws.orders._2013_09_01.model.ListOrderItemsByNextTokenRequest;
import com.amazonservices.mws.orders._2013_09_01.model.ListOrderItemsByNextTokenResponse;
import com.amazonservices.mws.orders._2013_09_01.model.ListOrderItemsRequest;
import com.amazonservices.mws.orders._2013_09_01.model.ListOrderItemsResponse;
import com.amazonservices.mws.orders._2013_09_01.model.ListOrdersByNextTokenRequest;
import com.amazonservices.mws.orders._2013_09_01.model.ListOrdersByNextTokenResponse;
import com.amazonservices.mws.orders._2013_09_01.model.ListOrdersRequest;
import com.amazonservices.mws.orders._2013_09_01.model.ListOrdersResponse;
import com.amazonservices.mws.orders._2013_09_01.model.Order;
import com.amazonservices.mws.orders._2013_09_01.model.OrderItem;
import com.amazonservices.mws.orders._2013_09_01.samples.MarketplaceWebServiceOrdersSampleConfig;
import com.poof.crawler.db.DBUtil;

/**
 * @author wilkey 
 * @mail admin@wilkey.vip
 * @Date 2017年1月10日 下午4:27:03
 */
public class MWSOrders {
	private static Logger log = Logger.getLogger(MWSOrders.class);
	private static String mws_marketplaceId = "";
	private static String mws_sellerId = "";
	public static Date lastListOrders = new Date();
	public static Date lastListOrderItems = new Date();
	public static Date lastGetOrder = new Date();
	
	static {
		try {
			Properties p = new Properties();
			p.load(MWSOrders.class.getResourceAsStream("/config.properties"));
			mws_marketplaceId = p.getProperty("mws.marketplaceId");
			mws_sellerId = p.getProperty("mws.sellerId");
		} catch (IOException e) {
			e.printStackTrace();
			log.error(log.getName() + " : program error: " + e);
		}
	}

	/**
	 * 判断当前服务器状态是否正常
	 * 
	 * @return
	 */
	public static boolean getServiceStatus() {
		MarketplaceWebServiceOrdersClient client = MarketplaceWebServiceOrdersSampleConfig.getClient();
		GetServiceStatusRequest request = new GetServiceStatusRequest();
		request.setSellerId(mws_sellerId);
		GetServiceStatusResponse response = client.getServiceStatus(request);
		return "GREEN".equals(response.getGetServiceStatusResult().getStatus());
	}
	
	public static void timer(){
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				long startTime = System.currentTimeMillis();
				List<Order> list = listOrders(TimeZone.getTimeZone("PST"), 24 * 60 * 1);
				BatchInsert(list);
				long endTime = System.currentTimeMillis();
				log.info(log.getName() + " : " + String.format("mwsorders done，耗时%s秒", (endTime - startTime) / 1000));
			}
		};
		Calendar calendar = Calendar.getInstance();
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH);
		int day = calendar.get(Calendar.DAY_OF_MONTH);
        calendar.set(year, month, day, 9, 30, 00);
		Date date = calendar.getTime();
		Timer timer = new Timer();
		timer.schedule(task, date, 60 * 1000 * 60 * 24);
	}

	/**
	 * 可返回您在指定时间段内创建或更新的订单。
	 * 
	 * @param minute
	 *            分钟数
	 * @return
	 * @throws Exception
	 */
	public static List<Order> listOrders(TimeZone timezone, int minute) {
		List<Order> ls = new ArrayList<Order>();
		MarketplaceWebServiceOrdersClient client = MarketplaceWebServiceOrdersSampleConfig.getClient();
		ListOrdersRequest request = new ListOrdersRequest();
		request.setSellerId(mws_sellerId);
		List<String> marketplaceIds = new ArrayList<String>();
		marketplaceIds.add(mws_marketplaceId);
		request.setMarketplaceId(marketplaceIds);
		XMLGregorianCalendar createdAfter = getCreatedAfter(timezone, minute);
		createdAfter.setHour(0);
		createdAfter.setMinute(0);
		createdAfter.setSecond(0);
		createdAfter.setMillisecond(0);
		request.setCreatedAfter(createdAfter);
		ListOrdersResponse response = client.listOrders(request);
		ls.addAll(response.getListOrdersResult().getOrders());
		log.info(log.getName() + " : responsed order... ");
		// 如果超过6条
		String nextToken = response.getListOrdersResult().getNextToken();
		while ((nextToken) != null) {
			log.info(log.getName() + " : responsed order... ");
			ListOrdersByNextTokenRequest listOrdersByNextTokenRequest = new ListOrdersByNextTokenRequest(mws_sellerId, nextToken);
			ListOrdersByNextTokenResponse listOrdersByNextTokenResponse = client.listOrdersByNextToken(listOrdersByNextTokenRequest);
			nextToken = listOrdersByNextTokenResponse.getListOrdersByNextTokenResult().getNextToken();
			ls.addAll(listOrdersByNextTokenResponse.getListOrdersByNextTokenResult().getOrders());
			try {
				Thread.sleep(60 * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				log.error(log.getName() + " : program error: " + e);
			}
		}
		return ls;
	}

	/**
	 * 得 到订单商品信息
	 * 
	 * @param amazonOrderId
	 * @return
	 */
	public static List<OrderItem> listOrderItems(String amazonOrderId) {
		List<OrderItem> ls = new ArrayList<OrderItem>();
		MarketplaceWebServiceOrdersClient client = MarketplaceWebServiceOrdersSampleConfig.getClient();
		ListOrderItemsRequest request = new ListOrderItemsRequest();
		request.setSellerId(mws_sellerId);
		request.setAmazonOrderId(amazonOrderId);
		ListOrderItemsResponse response = client.listOrderItems(request);
		// 如果超过30条
		String nextToken = response.getListOrderItemsResult().getNextToken();
		while ((nextToken) != null) {
			ListOrderItemsByNextTokenRequest listOrderItemsByNextTokenRequest = new ListOrderItemsByNextTokenRequest(mws_sellerId, nextToken);
			ListOrderItemsByNextTokenResponse listOrderItemsByNextTokenResponse = client.listOrderItemsByNextToken(listOrderItemsByNextTokenRequest);
			nextToken = listOrderItemsByNextTokenResponse.getListOrderItemsByNextTokenResult().getNextToken();
			ls.addAll(listOrderItemsByNextTokenResponse.getListOrderItemsByNextTokenResult().getOrderItems());
			try {
				Thread.sleep(2 * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				log.error(log.getName() + " : program error: " + e);
			}
		}
		return ls;
	}

	/**
	 * 得 到订单信息
	 * 
	 * @param amazonOrderId
	 * @return
	 */
	public static List<Order> getOrder(List<String> amazonOrderId) {
		wati(3);
		MarketplaceWebServiceOrdersClient client = MarketplaceWebServiceOrdersSampleConfig.getClient();
		GetOrderRequest request = new GetOrderRequest();
		request.setSellerId(mws_sellerId);
		request.setAmazonOrderId(amazonOrderId);
		GetOrderResponse response = client.getOrder(request);
		return response.getGetOrderResult().getOrders();
	}

	/**
	 * 延时等待 ListOrders 延时 1分钟 ListOrderItems 2 秒 GetOrder 1分钟
	 * 
	 * @param flag
	 *            1: ListOrders 2: ListOrderItems 3: GetOrder
	 */
	public static void wati(int flag) {
		// ListOrders 延时 1分钟
		if (flag == 1) {
			long n = new Date().getTime() - lastListOrders.getTime();
			try {
				if (60000 - n > 0) {
					Thread.sleep(60000 - n + 1000);
				}
				lastListOrders = new Date();
				return;
			} catch (InterruptedException e) {
				e.printStackTrace();
				log.error(log.getName() + " : program error: " + e);
			}
		}
		// ListOrderItems 2 秒
		if (flag == 2) {
			long n = new Date().getTime() - lastListOrderItems.getTime();
			try {
				if (2000 - n > 0) {
					Thread.sleep(2000 - n + 1000);
				}
				lastListOrderItems = new Date();
				return;
			} catch (InterruptedException e) {
				e.printStackTrace();
				log.error(log.getName() + " : program error: " + e);
			}
		}
		// GetOrder 1分钟
		if (flag == 3) {
			long n = new Date().getTime() - lastGetOrder.getTime();
			try {
				if (60000 - n > 0) {
					Thread.sleep(60000 - n + 1000);
				}
				lastGetOrder = new Date();
				return;
			} catch (InterruptedException e) {
				e.printStackTrace();
				log.error(log.getName() + " : program error: " + e);
			}
		}
	}

	/**
	 * 得到创建时间
	 * 
	 * @param timezone
	 * 
	 * @param min
	 *            几分钟之前
	 * @return
	 */
	private static XMLGregorianCalendar getCreatedAfter(TimeZone timezone, int min) {
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.add(Calendar.MINUTE, min * -1);
		if (timezone != null)
			calendar.setTimeZone(timezone);
		XMLGregorianCalendar createdAfter = MwsUtl.getDTF().newXMLGregorianCalendar(calendar);
		return createdAfter;
	}

	private static void BatchInsert(List<Order> orders) {
		String sql = "insert into bz_orders_mws (amazon_order_id, seller_order_id, purchase_date, last_update_date, order_status, fulfillment_channel, "
				+ "sales_channel, order_channel, ship_service_level, shipping_address, order_total, number_of_items_shipped, number_of_items_unshipped, "
				+ "payment_method, marketplace_id, buyer_email, buyer_name, shipment_service_level_category, order_type, earliest_ship_date, latest_ship_date, "
				+ "earliest_delivery_date, latest_delivery_date) " + "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		Connection conn = null;
		log.info(log.getName() + " : fetching mwsorders size " + orders.size());
		try {
			conn = DBUtil.openConnection();
			conn.setAutoCommit(false);
			conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
			PreparedStatement pstmt = conn.prepareStatement(sql);
			int size = orders.size() / 20;
			size = orders.size() % 20 >= 0 ? size + 1 : size; // 5521,5
			for (int i = 0; i < size; i++) { // 6
				for (int j = 0; j < (i == size - 1 ? orders.size() % 20 : 20); j++) {
					Order bean = orders.get(i * 20 + j);
					pstmt.setString(1, bean.getAmazonOrderId());
					pstmt.setString(2, bean.getSellerOrderId());
					pstmt.setString(3, bean.getPurchaseDate() == null ? "" : bean.getPurchaseDate().toString());
					pstmt.setString(4, bean.getLastUpdateDate() == null ? "" : bean.getLastUpdateDate().toString());
					pstmt.setString(5, bean.getOrderStatus());
					pstmt.setString(6, bean.getFulfillmentChannel());
					pstmt.setString(7, bean.getSalesChannel());
					pstmt.setString(8, bean.getOrderChannel());
					pstmt.setString(9, bean.getShipServiceLevel());
					pstmt.setString(10, bean.getShippingAddress() == null ? "" : bean.getShippingAddress().toString());
					pstmt.setString(11, bean.getOrderTotal() == null ? "" : bean.getOrderTotal().toString());
					pstmt.setInt(12, bean.getNumberOfItemsShipped());
					pstmt.setInt(13, bean.getNumberOfItemsUnshipped());
					pstmt.setString(14, bean.getPaymentMethod());
					pstmt.setString(15, bean.getMarketplaceId());
					pstmt.setString(16, bean.getBuyerEmail());
					pstmt.setString(17, bean.getBuyerName());
					pstmt.setString(18, bean.getShipmentServiceLevelCategory());
					pstmt.setString(19, bean.getOrderType());
					pstmt.setString(20, bean.getEarliestShipDate() == null ? "" : bean.getEarliestShipDate().toString());
					pstmt.setString(21, bean.getLatestShipDate() == null ? "" : bean.getLatestShipDate().toString());
					pstmt.setString(22, bean.getEarliestDeliveryDate() == null ? "" : bean.getEarliestDeliveryDate().toString());
					pstmt.setString(23, bean.getLatestDeliveryDate() == null ? "" : bean.getLatestDeliveryDate().toString());

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
		timer();
	}
}
