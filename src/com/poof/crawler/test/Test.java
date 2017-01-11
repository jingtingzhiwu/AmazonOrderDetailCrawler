package com.poof.crawler.test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Calendar;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class Test {
	public static void main(String[] args) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		
		Calendar c1 = Calendar.getInstance();
		c1.set(Calendar.DATE, c1.get(Calendar.DATE) - 2);
		System.err.println(c1.getTime());
		System.err.println((c1.get(Calendar.YEAR)+"").substring(2));
		System.err.println(c1.get(Calendar.MONTH)+1);
		System.err.println(c1.get(Calendar.DATE));
		

		if(1==1)return;
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(1482393599000L);
		System.out.println(c.getTime());
		String date = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(Long.valueOf(1482393599) * 1000));
		System.err.println(date);
		
		
		
		if(1==1)return;

		WebClient webClient = new WebClient();
		webClient.getOptions().setJavaScriptEnabled(false);
		webClient.getOptions().setCssEnabled(false);
		webClient.getOptions().setTimeout(60000);
		/** 1、打开amazom.com后台登录 */
		HtmlPage loginpage = webClient.getPage("https://www.baidu.com");
		System.err.println(loginpage.getWebResponse().getContentAsString());
	}
}
