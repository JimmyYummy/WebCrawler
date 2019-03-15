package edu.upenn.cis.cis455.crawler;

import static spark.Spark.halt;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public abstract class CrawlerUtils {
	public static String gentMD5Sign(String content) {
		MessageDigest md;
        String signature = null;
		try {
			md = MessageDigest.getInstance("MD5");
			signature = new String(md.digest(content.getBytes()));
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			halt(500);
		}
		return signature;
	}
	
	public static String genURL(String site, int port, boolean isSecure) {
		StringBuilder sb = new StringBuilder();
		if (isSecure) {
			sb.append("https://");
		} else {
			sb.append("http://");
		}
		sb.append(site);
		sb.append(":");
		sb.append(port);
		return sb.toString();
	}
}
