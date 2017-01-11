package com.poof.crawler.test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CaseName {
	public static final char UNDERLINE = '_';

	public static String camelToUnderline(String param) {
		if (param == null || "".equals(param.trim())) {
			return "";
		}
		int len = param.length();
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			char c = param.charAt(i);
			if (Character.isUpperCase(c)) {
				sb.append(UNDERLINE);
				sb.append(Character.toLowerCase(c));
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	public static String underlineToCamel(String param) {
		if (param == null || "".equals(param.trim())) {
			return "";
		}
		int len = param.length();
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			char c = param.charAt(i);
			if (c == UNDERLINE) {
				if (++i < len) {
					sb.append(Character.toUpperCase(param.charAt(i)));
				}
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	public static String underlineToCamel2(String param) {
		if (param == null || "".equals(param.trim())) {
			return "";
		}
		StringBuilder sb = new StringBuilder(param);
		Matcher mc = Pattern.compile("_").matcher(param);
		int i = 0;
		while (mc.find()) {
			int position = mc.end() - (i++);
			// String.valueOf(Character.toUpperCase(sb.charAt(position)));
			sb.replace(position - 1, position + 1, sb.substring(position, position + 1).toUpperCase());
		}
		return sb.toString();
	}

	public static void main(String[] args) {
		System.err.println(camelToUnderline("AmazonOrderId"));
		System.err.println(camelToUnderline("SellerOrderId"));
		System.err.println(camelToUnderline("PurchaseDate"));
		System.err.println(camelToUnderline("LastUpdateDate"));
		System.err.println(camelToUnderline("OrderStatus"));
		System.err.println(camelToUnderline("FulfillmentChannel"));
		System.err.println(camelToUnderline("SalesChannel"));
		System.err.println(camelToUnderline("OrderChannel"));
		System.err.println(camelToUnderline("ShipServiceLevel"));
		System.err.println(camelToUnderline("ShippingAddress"));
		System.err.println(camelToUnderline("OrderTotal"));
		System.err.println(camelToUnderline("NumberOfItemsShipped"));
		System.err.println(camelToUnderline("NumberOfItemsUnshipped"));
		System.err.println(camelToUnderline("PaymentExecutionDetail"));
		System.err.println(camelToUnderline("PaymentMethod"));
		System.err.println(camelToUnderline("MarketplaceId"));
		System.err.println(camelToUnderline("BuyerEmail"));
		System.err.println(camelToUnderline("BuyerName"));
		System.err.println(camelToUnderline("ShipmentServiceLevelCategory"));
		System.err.println(camelToUnderline("ShippedByAmazonTFM"));
		System.err.println(camelToUnderline("TFMShipmentStatus"));
		System.err.println(camelToUnderline("CbaDisplayableShippingLabel"));
		System.err.println(camelToUnderline("OrderType"));
		System.err.println(camelToUnderline("EarliestShipDate"));
		System.err.println(camelToUnderline("LatestShipDate"));
		System.err.println(camelToUnderline("EarliestDeliveryDate"));
		System.err.println(camelToUnderline("LatestDeliveryDate"));
		System.err.println(camelToUnderline("IsBusinessOrder"));
		System.err.println(camelToUnderline("PurchaseOrderNumber"));
		System.err.println(camelToUnderline("IsPrime"));
		System.err.println(camelToUnderline("IsPremiumOrder"));

	}

}
