package edu.upenn.cis.cis455.storage;

import static spark.Spark.halt;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.collections.StoredSortedMap;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

import edu.upenn.cis.cis455.crawler.handlers.LoginFilter;
import edu.upenn.cis.cis455.crawler.info.URLInfo;
import edu.upenn.cis.cis455.model.URLDetail;
import edu.upenn.cis.cis455.model.User;

public class StorageInstance implements StorageInterface {
	private static Logger logger = LogManager.getLogger(StorageInstance.class);

	private Environment env = null;
	private Database userDB = null;
	SortedMap<String, User> userMap = null;

	public StorageInstance(String directory) {

		try {
			// Open the environment, creating one if it does not exist
			EnvironmentConfig envConfig = new EnvironmentConfig();
			envConfig.setAllowCreate(true);
			envConfig.setTransactional(true);
			env = new Environment(new File(directory), envConfig);

			// Open the database, creating one if it does not exist
			DatabaseConfig dbConfig = new DatabaseConfig();
			dbConfig.setAllowCreate(true);
			dbConfig.setSortedDuplicates(false);
			dbConfig.setTransactional(true);
			
			Database classDb = env.openDatabase(null, "classDb", dbConfig);
			StoredClassCatalog catalog = new StoredClassCatalog(classDb);
			TupleBinding<String> keyBinding = TupleBinding.getPrimitiveBinding(String.class);
			EntryBinding<User> userBinding = new SerialBinding<User>(catalog, User.class);
			
			userDB = env.openDatabase(null, "UserDB", dbConfig);
			
			userMap = new StoredSortedMap<String, User>(userDB, keyBinding, userBinding, true);

		} catch (DatabaseException dbe) {
			// Exception handling
			if (userDB != null)
				userDB.close();
			if (env != null) {
				env.cleanLog();
				env.close();
			}
		}
		System.out.println("instance created");
	}

	@Override
	public int getCorpusSize() {
		// TODO
		return 0;
	}

	@Override
	public int getLexiconSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int addOrGetKeywordId(String keyword) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int addUser(User user) {
		logger.debug("adding user: " + user);
		if (userMap.containsKey(user.getUserName())) {
			logger.debug("duplicate username");
			return 1;
		} else {
			userMap.put(user.getUserName(), user);
			
			logger.debug("creation succeeded");
			return 0;
		}
	}

	@Override
	public User getSessionForUser(String username, String password) {
		if (! userMap.containsKey(username)) {
			logger.debug("user does not exist: " + username);
			return null;
		}
		User user = userMap.get(username);
		MessageDigest md;
        String pass = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
			pass = new String(md.digest(password.getBytes()));
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			halt(500);
		}
		if (user.getPassword().equals(pass)) {
			logger.debug("authenticated: "+ username);
			return user; 
		}
		logger.debug("authentication failed: " + username);
		return null;
	}

	@Override
	public String getDocument(String url) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close() {
		logger.info("closing the storage db system");
		if (userDB != null)
		userDB.close();
		if (env != null) {
			env.removeDatabase(null, "UserDB");
			env.cleanLog();
			env.close();
		}
	}

	@Override
	public String addDocument(String doc) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean putUrl(String url, String docId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public URLDetail getUrlDetial(URLInfo url) {
		// TODO Auto-generated method stub
		return null;
	}

}
