package com.poof.crawler.test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang3.StringUtils;

public class HttpTest {
	/**
	 * 获取网页源码
	 * 
	 * @return
	 */
	private String parseByHttpURLConnection(String targetUrl) {
		String content = null;

		HttpURLConnection connection = null;
		try {
			URL url = new URL(targetUrl);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");

			connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.116 Safari/537.36");
			connection.setRequestProperty("Accept", "application/json, text/javascript, */*; q=0.01");
			connection.setRequestProperty("Accept-Encoding", "gzip, deflate, sdch, br");
			connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.8");
			connection.setRequestProperty("Referer", targetUrl);
			connection.setRequestProperty("Connection", "keep-alive");
			connection.setRequestProperty("X-Requested-With", "XMLHttpRequest");
			connection.setRequestProperty("JSESSIONID", "688D16234DF047D5A836210E197EC84E");
			connection.setRequestProperty("DWRSESSIONID", "4qlZyiDnuT8b3oCR2VVaCTecVAl");
			connection.setRequestProperty("Hm_lvt_dfbcf49928b89df58afe0ed688157deb", "1482395162,1482395453,1482479803,1482723208");
			connection.setUseCaches(false);
			connection.setConnectTimeout(30 * 1000);
			connection.setReadTimeout(30 * 1000);
			connection.setDoOutput(true);
			connection.setDoInput(true);

			connection.connect();

			if (200 == connection.getResponseCode()) {
				InputStream inputStream = null;
				if (StringUtils.isNotEmpty(connection.getContentEncoding())) {
					String encode = connection.getContentEncoding().toLowerCase();
					if (StringUtils.isNotEmpty(encode) && encode.indexOf("gzip") >= 0) {
						inputStream = new GZIPInputStream(connection.getInputStream());
					}
				}

				if (null == inputStream) {
					inputStream = connection.getInputStream();
				}

				BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "utf-8"));
				StringBuilder builder = new StringBuilder();
				String line = null;
				while ((line = reader.readLine()) != null) {
					builder.append(line).append("\n");
				}
				content = builder.toString();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}

		return content;
	}

	public static void main(String[] args) {
		String content = new HttpTest().parseByHttpURLConnection("http://amazon.malllib.com/amazon/ambsrranksales/get_bsrranksales.jhtml?categoryid=11&rank=25");
		System.err.println(content);
	}
}
