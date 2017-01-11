package com.poof.crawler.orders;

import java.io.Serializable;

/**
 * @author wilkey 
 * @mail admin@wilkey.vip
 * @Date 2017年1月10日 下午4:26:38
 */
public class Orders implements Serializable {
	private static final long serialVersionUID = -7982141946759625441L;
	private String id;
	private String asin;
	private String sku;
	private Integer qty;
	private String title;
	private String esLevel;
	private boolean shipped;
	private String shipping;
	private String buyerId;
	private String buyerName;
	private boolean merchFulfilled;
	private String marketplaceId;
	private String latestShipDate;
	private boolean isPrime;
	private Integer numberOfItemsRemainingToShip;
	private String orderDate;
	private String status;

	public Orders(String id, String asin, String sku, Integer qty, String title, String esLevel, boolean shipped, String shipping, String buyerId, String buyerName, boolean merchFulfilled,
			String marketplaceId, String latestShipDate, boolean isPrime, Integer numberOfItemsRemainingToShip, String orderDate, String status) {
		super();
		this.id = id;
		this.asin = asin;
		this.sku = sku;
		this.qty = qty;
		this.title = title;
		this.esLevel = esLevel;
		this.shipped = shipped;
		this.shipping = shipping;
		this.buyerId = buyerId;
		this.buyerName = buyerName;
		this.merchFulfilled = merchFulfilled;
		this.marketplaceId = marketplaceId;
		this.latestShipDate = latestShipDate;
		this.isPrime = isPrime;
		this.numberOfItemsRemainingToShip = numberOfItemsRemainingToShip;
		this.orderDate = orderDate;
		this.status = status;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getAsin() {
		return asin;
	}

	public void setAsin(String asin) {
		this.asin = asin;
	}

	public String getSku() {
		return sku;
	}

	public void setSku(String sku) {
		this.sku = sku;
	}

	public Integer getQty() {
		return qty;
	}

	public void setQty(Integer qty) {
		this.qty = qty;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getEsLevel() {
		return esLevel;
	}

	public void setEsLevel(String esLevel) {
		this.esLevel = esLevel;
	}

	public boolean isShipped() {
		return shipped;
	}

	public void setShipped(boolean shipped) {
		this.shipped = shipped;
	}

	public String getShipping() {
		return shipping;
	}

	public void setShipping(String shipping) {
		this.shipping = shipping;
	}

	public String getBuyerId() {
		return buyerId;
	}

	public void setBuyerId(String buyerId) {
		this.buyerId = buyerId;
	}

	public String getBuyerName() {
		return buyerName;
	}

	public void setBuyerName(String buyerName) {
		this.buyerName = buyerName;
	}

	public boolean isMerchFulfilled() {
		return merchFulfilled;
	}

	public void setMerchFulfilled(boolean merchFulfilled) {
		this.merchFulfilled = merchFulfilled;
	}

	public String getMarketplaceId() {
		return marketplaceId;
	}

	public void setMarketplaceId(String marketplaceId) {
		this.marketplaceId = marketplaceId;
	}

	public String getLatestShipDate() {
		return latestShipDate;
	}

	public void setLatestShipDate(String latestShipDate) {
		this.latestShipDate = latestShipDate;
	}

	public boolean isPrime() {
		return isPrime;
	}

	public void setPrime(boolean isPrime) {
		this.isPrime = isPrime;
	}

	public Integer getNumberOfItemsRemainingToShip() {
		return numberOfItemsRemainingToShip;
	}

	public void setNumberOfItemsRemainingToShip(Integer numberOfItemsRemainingToShip) {
		this.numberOfItemsRemainingToShip = numberOfItemsRemainingToShip;
	}

	public String getOrderDate() {
		return orderDate;
	}

	public void setOrderDate(String orderDate) {
		this.orderDate = orderDate;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

}
