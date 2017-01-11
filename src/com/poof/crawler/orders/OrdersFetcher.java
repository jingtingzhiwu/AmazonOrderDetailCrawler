package com.poof.crawler.orders;

import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.poof.crawler.db.DBUtil;
import com.poof.crawler.orders.mws.MWSOrders;
import com.poof.crawler.orders.mws.report.MWSReportListings;
import com.poof.crawler.orders.mws.report.MWSReportOrders;
import com.poof.crawler.utils.EncDecUtil;
import com.poof.crawler.utils.TimeUtil;

/**
 * @author wilkey 
 * @mail admin@wilkey.vip
 * @Date 2017年1月10日 下午4:26:41
 */
public class OrdersFetcher {
	private static Logger log = Logger.getLogger(OrdersFetcher.class);
	private static Calendar c = Calendar.getInstance();
	private static String ap_email;
	private static String ap_password;
	static {
		c.setTime(TimeUtil.formatTimeZone(TimeZone.getTimeZone("PST"), 1));		//设置为前1天
		try {
			Properties p = new Properties();
			p.load(OrdersFetcher.class.getResourceAsStream("/config.properties"));
			ap_email = p.getProperty("ap.email");
			ap_password = EncDecUtil.dec(p.getProperty("ap.password"));
		} catch (IOException e) {
			e.printStackTrace();
			log.error(log.getName() + " : program error: " + e);
		}
	}
	public static boolean doLogin(WebClient webClient) throws FailingHttpStatusCodeException, MalformedURLException, IOException {

		/** 1、打开amazom.com后台登录 */
		HtmlPage loginpage = webClient.getPage("https://www.amazon.com/gp/flex/sign-in/select.html");

		/** 2、输入帐号密码 */
		HtmlInput emailinput = (HtmlInput) loginpage.getHtmlElementById("ap_email");
		emailinput.setValueAttribute(ap_email);
		HtmlInput pwdinput = (HtmlInput) loginpage.getHtmlElementById("ap_password");
		pwdinput.setValueAttribute(ap_password);

		/** 3、点击登录按钮 */
		HtmlInput signbtn = (HtmlInput) loginpage.getHtmlElementById("signInSubmit");
		HtmlPage gphomepage = (HtmlPage) signbtn.click();
		if (null == gphomepage.getWebResponse() && gphomepage.getWebResponse().getStatusCode() != 200)
			return false;
		return true;
	}

	/**
	 *  BeiJing CST Time 2016/12/21 10:34 AM
	 *  PST	Time				2016/12/20 6:34 PM
	 *  - 16H
	 *  默认设置为前两天
	 */
	public static void timer() {
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				WebClient webClient = new WebClient();
				webClient.getOptions().setJavaScriptEnabled(false);
				webClient.getOptions().setCssEnabled(false);
				webClient.getOptions().setTimeout(60000);
				webClient.addRequestHeader("referer", "http://www.google.com");

				long startTime = System.currentTimeMillis();
				try {

					/** 1、登录 */
					if (!doLogin(webClient))
						throw new IllegalArgumentException("do login error");

					/** 2、筛选订单*/
					/* preSelectedRange=1					取last day，前一天到当前所有
					 * 
					 * preSelectedRange=exactDates&	取指定时间范围内的
					 * searchDateOption=exactDates&
					 * exactDateBegin=12%2F19%2F16&
					 * exactDateEnd=12%2F19%2F16
					 */
					String date = (c.get(Calendar.MONTH) + 1) + "%2F" + (c.get(Calendar.DATE)) + "%2F" + (c.get(Calendar.YEAR) + "").substring(2);
					String orderUrl = "https://sellercentral.amazon.com/gp/orders-v2/list/ref=ag_myo_tnav_xx_?preSelectedRange=exactDates&searchDateOption=exactDates&"
							+ "exactDateBegin=" + date + "&"
							+ "exactDateEnd=" + date + "&"
							+ "itemsPerPage=100&showPending=0&isBelowTheFold=1&ajaxBelowTheFoldRows=0&currentPage=";

					/**
					 * ajaxBelowTheFoldRows=100 byDate=orderDate currentPage=1
					 * exactDateBegin=12%2F13%2F16 exactDateEnd=12%2F20%2F16
					 * highlightOrderID= isBelowTheFold=1 isDebug=0 isSearch=0
					 * itemsPerPage=100 paymentFilter=Default preSelectedRange=7
					 * searchDateOption=preSelected searchFulfillers=all searchKeyword=
					 * searchLanguage=en_US searchType=0 shipExactDateBegin=11%2F4%2F16
					 * shipExactDateEnd=12%2F27%2F16
					 * shipSearchDateOption=shipPreSelected shipSelectedRange=7
					 * shipmentFilter=Default showCancelled=0 showPending=0
					 * sortBy=OrderStatusDescending statusFilter=Default
					 */

					int maxpage = 1;
					for (int i = 1; i <= maxpage; i++) {

						HtmlPage orderpage = webClient.getPage(orderUrl + i);

						String htmlContent = orderpage.getWebResponse().getContentAsString();
						if (htmlContent.contains("Robot")) {
							throw new IllegalArgumentException("Robot Check !!!  Repeat, Robot Check !!!");
						}

						Document doc = Jsoup.parse(htmlContent);
						if (doc == null)
							return;

						if (maxpage == 1) {
							Elements pagelinks = doc.select(".tiny a.myo_list_orders_link");
							if (pagelinks.size() >= 3)
								maxpage = Integer.valueOf(pagelinks.eq(pagelinks.size() - 2).text().trim());
						}

						if (doc.select("tr[class*=order-row]").isEmpty()) {
							continue;
						} else {
							List<Orders> list = new ArrayList<Orders>();
							Elements rows = doc.select("tr[class*=order-row]");
							for (Element row : rows) {
								Orders order = cleanOrderBlock(row);
								list.add(order);
							}

							BatchInsert(list);

							// sleep thread
							TimeUnit.SECONDS.sleep(new Random().nextInt(100) % (100 - 80 + 1) + 80);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					log.error(log.getName() + " : program error: " + e);
				} finally {
					webClient.close();
				}
				long endTime = System.currentTimeMillis();
				System.err.println("sellercentral orders done.");
				log.info(log.getName() + " : " + String.format("sellercentral orders done，耗时%s秒", (endTime - startTime) / 1000));
			}
		};
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        calendar.set(year, month, day, 10, 00, 00);
        Date date = calendar.getTime();
        Timer timer = new Timer();
        timer.schedule(task, date, 60 * 1000 * 60 * 24);
	}

	/**
	 * save to db
	 * 1、爬虫采集数据（只采集前一天）
	 * 2、清洗数据
	 * Transaction(
	 * 3、删除前一天的数据
	 * 4、插入新数据
	 * )
	 */
	private static void BatchInsert(List<Orders> orders) throws Exception {
		String sql = "insert into bz_orders_sellercentral (order_id, asin, sku, qty, title, es_level, shipped, shipping, buyer_id, buyer_name, merch_fulfilled, marketplace_id, latest_ship_date, is_prime, number_of_items_remaining_toship, order_date, status)values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		Connection conn = null;
		log.info(log.getName() + " : fetching sellercentral size " + orders.size());
		try {
			conn = DBUtil.openConnection();
			conn.setAutoCommit(false);
			conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
			PreparedStatement pstmt = conn.prepareStatement(sql);
			int size = orders.size() / 20;
			size = orders.size() % 20 >= 0 ? size + 1 : size; // 5521,5
			for (int i = 0; i < size; i++) { // 6
				for (int j = 0; j < (i == size - 1 ? orders.size() % 20 : 20); j++) {
					Orders bean = orders.get(i * 20 + j);
					pstmt.setString(1, bean.getId());
					pstmt.setString(2, bean.getAsin());
					pstmt.setString(3, bean.getSku());
					pstmt.setInt(4, bean.getQty());
					pstmt.setString(5, bean.getTitle());
					pstmt.setString(6, bean.getEsLevel());
					pstmt.setBoolean(7, bean.isShipped());
					pstmt.setString(8, bean.getShipping());
					pstmt.setString(9, bean.getBuyerId());
					pstmt.setString(10, bean.getBuyerName());
					pstmt.setBoolean(11, bean.isMerchFulfilled());
					pstmt.setString(12, bean.getMarketplaceId());
					pstmt.setString(13, bean.getLatestShipDate());
					pstmt.setBoolean(14, bean.isPrime());
					pstmt.setInt(15, bean.getNumberOfItemsRemainingToShip());
					pstmt.setString(16, bean.getOrderDate());
					pstmt.setString(17, bean.getStatus());

					pstmt.addBatch();
				}
				pstmt.executeBatch();
				pstmt.clearBatch();
			}
			if (size > 0) {
			}
			conn.commit();
			pstmt.close();
		} catch (Exception e) {
			log.error(log.getName() + " : program error: " + e);
			throw e;
		} finally {
			DBUtil.closeConnection();
		}
	}

	private static Orders cleanOrderBlock(Element row) throws Exception {
		try {
			String id = row.select("input.order-id[type=hidden]").val();
			String eslevel = row.select("input.es-level[type=hidden]").isEmpty() ? "" : row.select("input.es-level[type=hidden]").val();
			boolean shipped = (row.select("input.num-shipped[type=hidden]") == null || "0".equals(row.select("input.num-shipped[type=hidden]").val())) ? false : true;
			String buyerId = row.select("input.cust-id[type=hidden]").isEmpty() ? "" : row.select("input.cust-id[type=hidden]").val();
			boolean merchFulfilled = (row.select("input.merch-fulfilled[type=hidden]") == null || "0".equals(row.select("input.merch-fulfilled[type=hidden]").val())) ? false : true;
			String marketplaceId = row.select("input.marketplace-id[type=hidden]").isEmpty() ? "" : row.select("input.marketplace-id[type=hidden]").val();
			String latestShipDate = row.select("input.latestShipDate[type=hidden]").isEmpty() ? "" : row.select("input.latestShipDate[type=hidden]").val();
			latestShipDate = StringUtils.isNotBlank(latestShipDate) ? new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(Long.valueOf(latestShipDate) * 1000)) : "";
			boolean isPrime = (row.select("input.isPrime[type=hidden]") == null || "0".equals(row.select("input.isPrime[type=hidden]").val())) ? false : true;
			String numberOfItemsRemainingToShip = row.select("input.numberOfItemsRemainingToShip[type=hidden]").isEmpty() ? null : row.select("input.numberOfItemsRemainingToShip[type=hidden]").val();

			String orderDate = row.select("td").isEmpty() ? "" : row.select("td").eq(1).text();
			String title = row.select("td div[id*=orderItem] span[id*=product]").isEmpty() ? "" : row.select("td div[id*=orderItem] span[id*=product]").text();
			String qty = row.select("td div[id*=orderItem] td:contains(QTY)").isEmpty() ? null : row.select("td div[id*=orderItem] td:contains(QTY)").text().replaceAll("[^\\d.]", "");
			String asin = row.select("td div[id*=orderItem] td:contains(ASIN)").isEmpty() ? "" : row.select("td div[id*=orderItem] td:contains(ASIN)").text().replaceAll("ASIN:", "").trim();
			String sku = row.select("td div[id*=orderItem] td:contains(SKU)").isEmpty() ? "" : row.select("td div[id*=orderItem] td:contains(SKU)").text().replaceAll("SKU:", "").trim();
			String buyerName = row.select("td a[id*=buyerName]").isEmpty() ? "" : row.select("td a[id*=buyerName]").text();
			String shipping = row.select("td[class*=order-cell]").isEmpty() ? "" : row.select("td[class*=order-cell]").eq(3).text();
			String status = row.select("td[class*=order-cell]").isEmpty() ? "" : row.select("td[class*=order-cell]").eq(4).select("div:not([style=display:none])").text();
			return new Orders(id, asin, sku, Integer.valueOf(qty), title, eslevel, shipped, shipping, buyerId, buyerName, merchFulfilled, marketplaceId, latestShipDate, isPrime,
					Integer.valueOf(numberOfItemsRemainingToShip), orderDate, status);

		} catch (Exception e) {
			e.printStackTrace();
			log.error(log.getName() + " : program error: " + e);
		}
		return null;
	}

	public static void main(String[] args) throws IOException {
		System.err.println("starting......");
		OrdersFetcher.timer();
		MWSOrders.timer();
		MWSReportOrders.timer();
		MWSReportListings.timer();
		System.in.read();
	}
}
