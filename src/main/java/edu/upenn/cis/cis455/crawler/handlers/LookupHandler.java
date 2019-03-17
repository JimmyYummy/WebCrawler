package edu.upenn.cis.cis455.crawler.handlers;

import static spark.Spark.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import spark.Request;
import spark.Route;
import spark.Response;
import spark.HaltException;
import edu.upenn.cis.cis455.storage.StorageInterface;

public class LookupHandler implements Route {
	private static Logger logger = LogManager.getLogger(LookupHandler.class);

	StorageInterface db;

	public LookupHandler(StorageInterface db) {
		this.db = db;
	}

	@Override
	public String handle(Request req, Response resp) throws HaltException {
		String url = null;
		logger.debug("lookup request from " + req);
		try {
			url = req.queryParams("url");
		} catch (Exception e) {
			halt(400);
		}
		if (url == null) halt(400, "no valid url");
		logger.debug("lookup request from " + req  + "on " + url);
		String doc = db.getDocument(url);
		if (doc == null) {
			halt(404, "Lookup URL not found");
		}
		String docType = db.getDocType(url);
		resp.type(docType);
		return db.getDocument(url);
	}
}
