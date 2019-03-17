package edu.upenn.cis.cis455.storage;

import edu.upenn.cis.cis455.crawler.info.URLInfo;
import edu.upenn.cis.cis455.model.URLDetail;
import edu.upenn.cis.cis455.model.User;

public interface StorageInterface {
    
    /**
     * How many documents so far?
     */
	public int getCorpusSize();
	
	/**
	 * Add a new document, getting its ID
	 * @param string 
	 */
	public String addDocument(String doc, String string);
	
	/**
	 * How many keywords so far?
	 */
	public int getLexiconSize();
	
	/**
	 * Gets the ID of a word (adding a new ID if this is a new word)
	 */
	public int addOrGetKeywordId(String keyword);
	
	/**
	 * Adds a user and returns an ID
	 */
	int addUser(User user);
	
	/**
	 * Tries to log in the user, or else throws a HaltException
	 */
	public User getSessionForUser(String username, String password);
	
	/**
	 * Retrieves a document's contents by URL
	 */
	public String getDocument(String url);
		
	/**
	 * Shuts down / flushes / closes the storage system
	 */
	public void close();

	public URLDetail getUrlDetial(URLInfo url);

	public void addUrlDetail(URLDetail urlDetail);

	public void decreUrlCount(String docId);

	public boolean isHtmlDoc(String urlStr);
	
	public String getDocType(String url);

	public boolean hasDocument(String doc);

	public int docLinkCount(String docId);
	
}
