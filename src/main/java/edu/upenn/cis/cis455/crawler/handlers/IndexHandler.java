package edu.upenn.cis.cis455.crawler.handlers;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.model.User;
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
		logger.debug("Received request on index route");
		logger.debug("getting channel infos");
		List<List<String>> channelInfos = db.getChannelInfos();
		logger.debug("generating index.html");
		resp.type("text/html");
		StringBuilder sb = new StringBuilder();
		sb.append("<!DOCTYPE html><html lang=\"en\">");
		sb.append("<body>");
		User user = (User) req.session().attribute("userModel");
		sb.append(String.format("<div>Welcome %s %s.</div>", user.getFirstName(), user.getLastName()));
		logger.debug("generating divs of channels");
		sb.append("<ul class=\"channels\">");
		for (List<String> channelInfo : channelInfos) {
			appendChannel(channelInfo, sb);
		}
		sb.append("</ul");
		sb.append("/body");
		sb.append("</html>");
		logger.debug("returning the html to client");
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
