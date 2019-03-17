package edu.upenn.cis.cis455.storage;

import static spark.Spark.halt;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.SortedMap;

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

import edu.upenn.cis.cis455.crawler.CrawlerUtils;
import edu.upenn.cis.cis455.crawler.info.URLInfo;
import edu.upenn.cis.cis455.model.DBDocument;
import edu.upenn.cis.cis455.model.URLDetail;
import edu.upenn.cis.cis455.model.User;

public class StorageInstance implements StorageInterface {
	private static Logger logger = LogManager.getLogger(StorageInstance.class);

	private Environment env = null;

	private Database userDB = null;
	SortedMap<String, User> userMap = null;

	private Database docDB = null;
	SortedMap<String, DBDocument> docMap = null;

	private Database urlDB = null;
	SortedMap<String, URLDetail> urlMap = null;

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

			TupleBinding<String> userKeyBinding = TupleBinding.getPrimitiveBinding(String.class);
			EntryBinding<User> userEntryBinding = new SerialBinding<User>(catalog, User.class);
			userDB = env.openDatabase(null, "UserDB", dbConfig);
			userMap = new StoredSortedMap<String, User>(userDB, userKeyBinding, userEntryBinding, true);

			TupleBinding<String> docKeyBinding = TupleBinding.getPrimitiveBinding(String.class);
			EntryBinding<DBDocument> docEntryBinding = new SerialBinding<DBDocument>(catalog, DBDocument.class);
			docDB = env.openDatabase(null, "DocDB", dbConfig);
			docMap = new StoredSortedMap<String, DBDocument>(docDB, docKeyBinding, docEntryBinding, true);

			TupleBinding<String> urlKeyBinding = TupleBinding.getPrimitiveBinding(String.class);
			EntryBinding<URLDetail> urlEntryBinding = new SerialBinding<URLDetail>(catalog, URLDetail.class);
			urlDB = env.openDatabase(null, "UrlDB", dbConfig);
			urlMap = new StoredSortedMap<String, URLDetail>(urlDB, urlKeyBinding, urlEntryBinding, true);

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
	public int addUser(User user) {
		synchronized (userDB) {
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
	}

	@Override
	public User getSessionForUser(String username, String password) {
		synchronized (userDB) {
			if (!userMap.containsKey(username)) {
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
				logger.debug("authenticated: " + username);
				return user;
			}
			logger.debug("authentication failed: " + username);
			return null;
		}
	}

	@Override
	public void close() {
		logger.debug("closing the storage db system");
		if (userDB != null)
			userDB.close();
		if (env != null) {
			env.removeDatabase(null, "UserDB");
			env.cleanLog();
			env.close();
		}
	}

	@Override
	public int getCorpusSize() {
		// TODO Auto-generated method stub
		synchronized (docMap) {
			return docMap.size();
		}
	}

	@Override
	public String addDocument(String doc, String type) {
		synchronized (docMap) {
			String key = CrawlerUtils.gentMD5Sign(doc);
			int linkedUrls = 1;
			if (docMap.containsKey(key)) {
				linkedUrls += docMap.get(key).getLinkedUrls();
			}
			DBDocument ddoc = new DBDocument(key, linkedUrls, doc, type);
			docMap.put(key, ddoc);
			return key;
		}

	}
	
	@Override
	public void decreUrlCount(String docId) {
		synchronized (docMap) {
			DBDocument doc = docMap.remove(docId);
			if (doc == null) return;
			if (doc.getLinkedUrls() == 1) return;
			DBDocument newDoc = new DBDocument(docId, doc.getLinkedUrls() - 1, doc.getContent(), doc.getType());
			docMap.put(docId, newDoc);
		}

	}

	@Override
	public boolean isHtmlDoc(String url) {
		synchronized (urlMap) {
			synchronized (docMap) {
				URLDetail detail = urlMap.getOrDefault(url, null);
				if (detail == null) return false;
				DBDocument doc = docMap.getOrDefault(detail.getDocId(), null);
				return doc == null ? false : "text/html".equals(doc.getType());
			}
		}
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
	public String getDocument(String url) {
		synchronized (urlMap) {
			synchronized (docMap) {
				URLDetail detail = urlMap.getOrDefault(url, null);
				if (detail == null) return null;
				DBDocument doc = docMap.getOrDefault(detail.getDocId(), null);
				return doc == null ? null : doc.getContent();
			}
		}

	}
	
	@Override
	public String getDocType(String url) {
		synchronized (urlMap) {
			synchronized (docMap) {
				URLDetail detail = urlMap.getOrDefault(url, null);
				if (detail == null) return null;
				DBDocument doc = docMap.getOrDefault(detail.getDocId(), null);
				return doc == null ? null : doc.getType();
			}
		}
	}

	@Override
	public URLDetail getUrlDetial(URLInfo url) {
		synchronized (urlMap) {
			String urlStr = CrawlerUtils.genURL(url.getHostName(), url.getPortNo(), url.isSecure(), url.getFilePath());
			return urlMap.getOrDefault(urlStr, null);
		}
	}

	@Override
	public void addUrlDetail(URLDetail urlDetail) {
		synchronized (urlMap) {
			urlMap.put(urlDetail.getUrl(), urlDetail);
		}

	}

}
