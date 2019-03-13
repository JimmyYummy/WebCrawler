package edu.upenn.cis.cis455.crawler.handlers;

import static spark.Spark.*;
import spark.Request;
import spark.Route;
import spark.Response;
import spark.HaltException;
import edu.upenn.cis.cis455.storage.StorageInterface;

public class LookupHandler implements Route {
	StorageInterface db;

	public LookupHandler(StorageInterface db) {
		this.db = db;
	}

	@Override
	public String handle(Request req, Response resp) throws HaltException {
		String url = null;
		try {
			url = req.queryParams("url");
		} catch (Exception e) {
			halt(400);
		}

		System.err.println("Lookup request for " + url);
		return db.getDocument(url);
	}
}
