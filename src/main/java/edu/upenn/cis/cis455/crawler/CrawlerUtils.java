package edu.upenn.cis.cis455.crawler;

import static spark.Spark.halt;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class CrawlerUtils {
	private static Logger logger = LogManager.getLogger(CrawlerUtils.class);

	private static DateTimeFormatter formater1 = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz");
	private static ZoneId zone = ZoneId.of("GMT");
	private static String epochDate = null;

	
	public static String gentMD5Sign(String content) {
		MessageDigest md;
        String signature = null;
		try {
			md = MessageDigest.getInstance("MD5");
			signature = new String(md.digest(content.getBytes()));
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			logger.catching(Level.DEBUG, e);
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
		if (port != 80) {
			sb.append(":");
			sb.append(port);
		}
		return sb.toString();
	}
	
	public static String genURL(String site, int port, boolean isSecure, String filePath) {
		String url = genURL(site, port, isSecure) + "/" + filePath;
		return url.replaceAll("//", "/").replaceFirst("/", "//");
	}
	
	public static String getDate() {
    	
    	ZonedDateTime zonedDT = ZonedDateTime.now(zone);
    	return zonedDT.format(formater1);
	}
	
	public static String epochSecondToDate(long second) {
		Instant instant = Instant.ofEpochSecond(second);
		ZonedDateTime zonedDT = ZonedDateTime.ofInstant(instant, zone);
		return zonedDT.format(formater1);
	}
	
	public static long dateToEpochSecond(String dateStr) {
    	try {
    		ZonedDateTime zonedTime = ZonedDateTime.parse(dateStr, formater1);
    		return zonedTime.toEpochSecond();
    	} catch (DateTimeParseException e) {
    		halt(500);
    	}
    	return 0L;
	}
	
	public static String epochDate() {
		if (epochDate == null) {
			epochDate = epochSecondToDate(0);
		}
		return epochDate;
	}
}
