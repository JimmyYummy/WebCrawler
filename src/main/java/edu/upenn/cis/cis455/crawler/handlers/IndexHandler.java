package edu.upenn.cis.cis455.crawler.handlers;

import spark.Request;
import spark.Route;
import spark.Response;
import spark.HaltException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.model.User;
import edu.upenn.cis.cis455.storage.StorageInterface;

public class IndexHandler implements Route {
	private static Logger logger = LogManager.getLogger(Route.class);

	StorageInterface db;

	public IndexHandler(StorageInterface db) {
		this.db = db;
	}

	@Override
	public String handle(Request req, Response resp) throws HaltException {
		try {
			logger.debug("get request from req" + req);
			User user = (User) req.session().attribute("userModel");
			resp.header("content-type", "text/html");
			//TODO:
			return "Welcome, " + user.getFirstName() + " " + user.getLastName();
		} catch (Exception e) {
			logger.catching(Level.DEBUG, e);
			logger.catching(e);
			throw e;
		}
	}
}
