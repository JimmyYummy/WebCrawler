package edu.upenn.cis.cis455.crawler.handlers;

import static spark.Spark.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import spark.Request;
import spark.Route;
import spark.Response;
import spark.HaltException;
import edu.upenn.cis.cis455.model.User;
import edu.upenn.cis.cis455.storage.StorageInterface;

public class RegisterHandler implements Route {
	private static Logger logger = LogManager.getLogger(RegisterHandler.class);
	StorageInterface db;

	public RegisterHandler(StorageInterface db) {
		this.db = db;
	}

	@Override
	public String handle(Request req, Response resp) throws HaltException {
		logger.debug("Get register request: " + req);
		String username = null;
		String password = null;
		String firstName = null;
		String lastName = null;
		username = req.queryParams("username");
		password = req.queryParams("password");
		firstName = req.queryParams("firstname");
		lastName = req.queryParams("lastname");
		if (username.length() == 0 || password.length() == 0 
				|| firstName.length() == 0 || lastName.length() == 0)
			resp.redirect("register-err2.html");
			logger.debug("Register request for " + username);
		MessageDigest md;
		String pass = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
			pass = new String(md.digest(password.getBytes()));
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			logger.catching(Level.DEBUG, e);
			logger.catching(e);
			halt(500);
		}
		User user = new User(username, pass, firstName, lastName);

		int status = db.addUser(user);
		if (status == 0) {
			logger.debug("registration success: " + username);
			resp.redirect("/login-form.html");
		} else {
			logger.debug("registration failed: " + username);
			resp.redirect("/register-err.html");
		}

		return "";
	}
}
