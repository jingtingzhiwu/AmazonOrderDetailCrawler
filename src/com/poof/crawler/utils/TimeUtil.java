package com.poof.crawler.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * @author wilkey 
 * @mail admin@wilkey.vip
 * @Date 2017年1月10日 下午4:27:32
 */
public class TimeUtil {
	public static Date formatTimeZone(TimeZone timezone, int days) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		dateFormat.setTimeZone(timezone);
		Calendar c = Calendar.getInstance();
		c.set(Calendar.DATE, c.get(Calendar.DATE) - days);

		SimpleDateFormat dateFormat1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			Date strtodate = dateFormat1.parse(dateFormat.format(c.getTime()));
			return strtodate;
			// System.err.println(strtodate);
			// System.err.println(dateFormat.format(c.getTime()));
			// System.err.println(c.getTime());
			// System.err.println(dateFormat.getCalendar().getTime());
		} catch (ParseException e) {
			return null;
		}
	}

	public static String parseTimeZone(TimeZone timezone, int days) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		dateFormat.setTimeZone(timezone);
		Calendar c = Calendar.getInstance();
		c.set(Calendar.DATE, c.get(Calendar.DATE) - days);
		return dateFormat.format(c.getTime());
	}

	public static void main(String[] args) {
		System.err.println(TimeUtil.formatTimeZone(TimeZone.getTimeZone("PST"), 1));
	}
}
