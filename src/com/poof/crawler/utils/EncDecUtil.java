package com.poof.crawler.utils;

import org.apache.commons.codec.binary.Base64;

/**
 * 加密解密工具
 * @author wilkey 
 * @mail admin@wilkey.vip
 * @Date 2017年1月10日 下午4:27:27
 */
public class EncDecUtil {

	/**
	 * 解密
	 * 
	 * @param encMsg
	 * @return
	 */
	public static String dec(String encMsg) {
		String decMsg = "";
		try {
			byte[] srcBytes = Des3.decryptMode("6a2b69c6740f46c2b5db1294d01d7c11", Base64.decodeBase64(encMsg));
			// 正式
			decMsg = new String(srcBytes, "UTF-8");
			// 测试
			// decMsg =new String(srcBytes);
		} catch (Exception e) {
			decMsg = "";
			e.printStackTrace();
		}
		return decMsg;
	}

	/**
	 * 加密
	 * 
	 * @param msg
	 * @return
	 */
	public static String enc(String msg) {
		String encMsg = "";
		try {
			// 正式
			byte[] encoded = Des3.encryptMode("6a2b69c6740f46c2b5db1294d01d7c11", msg.getBytes("UTF-8"));
			// 测试
			// byte[] encoded = Des3.encryptMode(key, msg.getBytes());
			encMsg = new String(encoded);
		} catch (Exception e) {
			e.printStackTrace();
			encMsg = "";
		}
		return encMsg;
	}
}