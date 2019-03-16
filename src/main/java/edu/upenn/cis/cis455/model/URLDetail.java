package edu.upenn.cis.cis455.model;

import java.io.Serializable;

public class URLDetail implements Serializable {
	private String url;
	private String docId;
	private long epochSecond;
	
	public URLDetail(String url, String docId, long epochSecond) {
		this.url = url;
		this.docId = docId;
		this.epochSecond = epochSecond;
	}

	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}


	/**
	 * @return the docId
	 */
	public String getDocId() {
		return docId;
	}


	/**
	 * @return the epochSecond
	 */
	public long getEpochSecond() {
		return epochSecond;
	}

}
