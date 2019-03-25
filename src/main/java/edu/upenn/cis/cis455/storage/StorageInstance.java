package edu.upenn.cis.cis455.storage;

import static spark.Spark.halt;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.apache.logging.log4j.Level;
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
import edu.upenn.cis.cis455.model.ChannelMeta;
import edu.upenn.cis.cis455.model.DBDocument;
import edu.upenn.cis.cis455.model.URLDetail;
import edu.upenn.cis.cis455.model.User;

public class StorageInstance implements StorageInterface {
	private static Logger logger = LogManager.getLogger(StorageInstance.class);

	private Environment env = null;

	private Database classDB = null;

	private Database userDB = null;
	SortedMap<String, User> userMap = null;

	private Database docDB = null;
	SortedMap<String, DBDocument> docMap = null;

	private Database urlDB = null;
	SortedMap<String, URLDetail> urlMap = null;

	private Database channelDB = null;
	SortedMap<Integer, ChannelMeta> channelMap = null;

	private boolean isClosed;

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

			classDB = env.openDatabase(null, "ClassDB", dbConfig);
			StoredClassCatalog catalog = new StoredClassCatalog(classDB);

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

			TupleBinding<Integer> channelKeyBinding = TupleBinding.getPrimitiveBinding(Integer.class);
			EntryBinding<ChannelMeta> channelMetaEntryBinding = new SerialBinding<ChannelMeta>(catalog,
					ChannelMeta.class);
			channelDB = env.openDatabase(null, "ChannelDB", dbConfig);
			channelMap = new StoredSortedMap<Integer, ChannelMeta>(channelDB, channelKeyBinding,
					channelMetaEntryBinding, true);

			isClosed = true;
		} catch (DatabaseException dbe) {
			// Exception handling
			if (userDB != null)
				userDB.close();
			if (env != null) {
				env.cleanLog();
				env.close();
			}
		}
		logger.debug("instance created");
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
				logger.catching(Level.DEBUG, e);
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
	public synchronized void closeWithoutFlushing() {
		logger.debug("closing w./o. flushing the storage db system");
		if (userDB != null)
			userDB.close();
		if (urlDB != null)
			urlDB.close();
		if (docDB != null)
			docDB.close();
		if (classDB != null)
			classDB.close();
		if (channelDB != null)
			channelDB.close();
		if (env != null) {
			env.cleanLog();
			env.close();
		}
		isClosed = true;
	}

	@Override
	public synchronized void close() {
		logger.debug("closing w. flushing the storage db system");
		if (userDB != null)
			userDB.close();
		if (urlDB != null)
			urlDB.close();
		if (docDB != null)
			docDB.close();
		if (channelDB != null)
			channelDB.close();
		if (classDB != null)
			classDB.close();
		if (env != null) {
			env.truncateDatabase(null, "UserDB", false);
			env.truncateDatabase(null, "UrlDB", false);
			env.truncateDatabase(null, "DocDB", false);
			env.truncateDatabase(null, "ClassDB", false);
			env.truncateDatabase(null, "ChannelDB", false);
			env.cleanLog();
			env.close();
		}
		isClosed = true;
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
			if (doc == null)
				return;
			if (doc.getLinkedUrls() == 1)
				return;
			DBDocument newDoc = new DBDocument(docId, doc.getLinkedUrls() - 1, doc.getContent(), doc.getType());
			docMap.put(docId, newDoc);
		}

	}

	@Override
	public boolean isHtmlDoc(String url) {
		synchronized (urlMap) {
			synchronized (docMap) {
				URLDetail detail = urlMap.getOrDefault(url, null);
				if (detail == null)
					return false;
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
				if (detail == null)
					return null;
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
				if (detail == null)
					return null;
				DBDocument doc = docMap.getOrDefault(detail.getDocId(), null);
				return doc == null ? null : doc.getType();
			}
		}
	}

	@Override
	public void addUrlDetail(URLDetail urlDetail) {
		synchronized (urlMap) {
			logger.debug("Saving URL: " + urlDetail.getUrl());
			urlMap.put(urlDetail.getUrl(), urlDetail);
		}

	}

	@Override
	public String removeUrlDetail(String urlStr) {
		synchronized (urlMap) {
			logger.debug("Removing URL: " + urlStr);
			return urlMap.remove(urlStr).getDocId();
		}
	}

	@Override
	public URLDetail getUrlDetial(URLInfo url) {
		synchronized (urlMap) {
			String urlStr = CrawlerUtils.genURL(url.getHostName(), url.getPortNo(), url.isSecure(), url.getFilePath());
			logger.debug("checking URL: " + urlStr);
			return urlMap.getOrDefault(urlStr, null);
		}
	}

	@Override
	public boolean hasDocument(String doc) {
		synchronized (docMap) {
			String key = CrawlerUtils.gentMD5Sign(doc);
			return docMap.containsKey(key);
		}
	}

	@Override
	public int docLinkCount(String docId) {
		synchronized (docMap) {
			if (!docMap.containsKey(docId)) {
				return 0;
			}
			return docMap.get(docId).getLinkedUrls();
		}
	}

	@Override
	public synchronized boolean isClosed() {
		return isClosed;
	}

	@Override
	public void addUrlToChannel(int channelNo, String url) {
		ChannelMeta ch = channelMap.get(channelNo);
		if (ch == null)
			return;
		ch.getUrls().add(url);
	}

	@Override
	public void removeUrlFromAllChannels(String url) {
		for (ChannelMeta ch : channelMap.values()) {
			Iterator<String> iter = ch.getUrls().iterator();
			while (iter.hasNext()) {
				if (iter.next().equals(url))
					iter.remove();
			}
		}
	}

	@Override
	public synchronized boolean addChannel(String channelName, String channelCreater, String channelXPath) {
		if (channelName == null || channelCreater == null || channelXPath == null)
			return false;
		for (ChannelMeta ch : channelMap.values()) {
			if (channelName.equals(ch.getChannelName()))
				return false;
		}
		int channelId = channelMap.size();
		ChannelMeta ch = new ChannelMeta(channelId, channelName, channelCreater, channelXPath);
		channelMap.put(channelId, ch);
		return true;
	}

	@Override
	public synchronized List<List<String>> getChannelInfos() {
		List<List<String>> infos = new ArrayList<>();
		int size = channelMap.size();
		for (int i = 0; i < size; i++) {
			ChannelMeta ch = channelMap.get(i);
			List<String> chInfo = new ArrayList<>();
			chInfo.add(ch.getChannelName());
			chInfo.add(ch.getChannelXPath());
			infos.add(chInfo);
		}
		return infos;
	}

	@Override
	public synchronized ChannelMeta getChannelDetail(int channelNo) {
		return channelMap.get(channelNo);
	}

	@Override
	public int getChannelNo(String name) {
		for (ChannelMeta ch : channelMap.values()) {
			if (ch.getChannelName().equals(name))
				return ch.getChannelNo();
		}
		return -1;
	}

	@Override
	public String getCraweledTime(String url) {
		URLDetail ud = urlMap.get(url);
		long epochSec = ud.getEpochSecond();
		ZonedDateTime zdt = Instant.ofEpochSecond(epochSec, 0).atZone(ZoneId.of("EST"));

		return String.format("%s-%s-%sT%s:%s:%s", zdt.getYear(), zdt.getMonth(), zdt.getDayOfMonth(), zdt.getHour(),
				zdt.getMinute(), zdt.getSecond());
	}

}
