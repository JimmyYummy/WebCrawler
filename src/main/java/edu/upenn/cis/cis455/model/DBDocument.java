package edu.upenn.cis.cis455.model;

import java.io.Serializable;

public class DBDocument implements Serializable {
	
	private String docId;
	private int linkedUrls;
	private String content;
	private String type;
	
	public DBDocument(String docId, int linkedUrls, String content, String type) {
		super();
		this.docId = docId;
		this.linkedUrls = linkedUrls;
		this.content = content;
		this.type = type;
	}

	/**
	 * @return the docId
	 */
	public String getDocId() {
		return docId;
	}

	/**
	 * @return the linkedDocs
	 */
	public int getLinkedUrls() {
		return linkedUrls;
	}

	/**
	 * @return the contnet
	 */
	public String getContent() {
		return content;
	}
	
	public String getType() {
		return type;
	}
}
