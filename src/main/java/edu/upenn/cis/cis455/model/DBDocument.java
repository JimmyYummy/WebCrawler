package edu.upenn.cis.cis455.model;

public class DBDocument {
	
	private String docId;
	private int linkedDocs;
	private String content;
	private String type;
	
	public DBDocument(String docId, int linkedDocs, String content, String type) {
		super();
		this.docId = docId;
		this.linkedDocs = linkedDocs;
		this.content = content;
		if (type.toLowerCase().endsWith("html")) this.type = "html";
		else if (type.toLowerCase().endsWith("xml")) this.type = "xml";
		else throw new IllegalArgumentException("unsuppored document type");
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
	public int getLinkedDocs() {
		return linkedDocs;
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
