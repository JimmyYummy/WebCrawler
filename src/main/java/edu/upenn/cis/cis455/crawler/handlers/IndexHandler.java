package edu.upenn.cis.cis455.crawler.handlers;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.storage.StorageInterface;
import spark.HaltException;
import spark.Request;
import spark.Response;
import spark.Route;

public class IndexHandler implements Route {
	private static Logger logger = LogManager.getLogger(IndexHandler.class);

	StorageInterface db;

	public IndexHandler(StorageInterface db) {
		this.db = db;
	}

	@Override
	public String handle(Request req, Response resp) throws HaltException {
		List<List<String>> channelInfos = db.getChannelInfos();
		
		StringBuilder sb = new StringBuilder();
		sb.append("<!DOCTYPE html><html lang=\"en\">");
		sb.append("<body>");
		
		sb.append("<ul class=\"channels\">");
		for (List<String> channelInfo : channelInfos) {
			appendChannel(channelInfo, sb);
		}
		sb.append("</ul");
		sb.append("/body");
		sb.append("</html>");
		
		return sb.toString();
	}

	private void appendChannel(List<String> channelInfo, StringBuilder sb) {
		String name = channelInfo.get(0);
		String path = channelInfo.get(1);
		sb.append("<li>");
		sb.append(String.format("<div>Channel name: %s\n Path: %s</div>", name, path));
		sb.append(String.format("<a href='/show?channel=%s'>link</a>", name));
		sb.append("</li>");
	}
}
