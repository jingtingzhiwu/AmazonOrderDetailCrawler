package com.poof.crawler.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.Cookie;

/**
 * @author wilkey 
 * @mail admin@wilkey.vip
 * @Date 2017年1月10日 下午4:27:19
 */
public class Cookies {
	protected Map<String, String> getCookies() {

		WebClient webClient = new WebClient();
		webClient.getCookieManager().setCookiesEnabled(true);
		// webClient.getOptions().setJavaScriptEnabled(true);
		// webClient.getOptions().setCssEnabled(true);
		// webClient.getOptions().setThrowExceptionOnScriptError(false);
		// webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		webClient.getOptions().setTimeout(10000);
		webClient.getCookieManager().clearCookies();
		webClient.addRequestHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.116 Safari/537.36");
		webClient.addRequestHeader("Accept", "application/json, text/javascript, */*; q=0.01");
		webClient.addRequestHeader("Accept-Encoding", "gzip, deflate");
		webClient.addRequestHeader("Accept-Language", "zh-CN,zh;q=0.8");
		webClient.addRequestHeader("referer", "http://www.google.com");
		List<String> checkList = new ArrayList<String>();
		Map<String, String> map = new HashMap<>();
		try {
			do {
				checkList.clear();
				map.clear();
				webClient.getPage("https://www.amazon.com/");
				webClient.waitForBackgroundJavaScript(30000);
				Set<Cookie> set = webClient.getCookieManager().getCookies();
				for (Iterator<Cookie> iterator = set.iterator(); iterator.hasNext();) {
					Cookie cookie = iterator.next();
					map.put(cookie.getName(), cookie.getValue());
					checkList.add(cookie.getName());
				}
			} while (!checkList.contains("session-token") && !checkList.contains("session-id"));
			return map;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			webClient.close();
		}
		return new HashMap<String, String>();
	}
}
