package edu.upenn.cis.cis455.xpathengine;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PathStep {
	private String nodeName;
	PathStep next;
	private List<String> texts;
	private List<String> contains;

	public PathStep(String nodeName) {
		this.nodeName = nodeName.replaceAll(" ", "");
		texts = new ArrayList<>();
		contains = new ArrayList<>();
	}

	/**
	 * @return the nodeName
	 */
	public String getNodeName() {
		return nodeName;
	}

	/**
	 * @return the next
	 */
	public PathStep getNext() {
		return next;
	}

	/**
	 * @return the texts
	 */
	public List<String> getTexts() {
		return texts;
	}

	/**
	 * @return the contains
	 */
	public List<String> getContains() {
		return contains;
	}
	
	public boolean addText(String str) {
		return this.texts.add(parseEscape(str));
	}

	public boolean addContain(String str) {
		return this.contains.add(parseEscape(str));
	}
	
	private String parseEscape(String str) {
		Properties prop = new Properties();     
	    try {
			prop.load(new StringReader("x=" + str + "\n"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	    String parsed = prop.getProperty("x");
	    return parsed;
	}
	
	/**
	 * @param nodeName the nodeName to set
	 */
	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	/**
	 * @param next the next to set
	 */
	public void setNext(PathStep next) {
		this.next = next;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append('/');
		sb.append(this.nodeName);
		sb.append('^');
		for (String text : this.texts) {
			sb.append(String.format("[text()=\"%s\"]", text));
		}
		for (String contain : this.contains) {
			sb.append(String.format("[contains(text(), \"%s\")]", contain));
		}
		if (this.next != null) {
			sb.append(" -> ");
			sb.append(this.next.toString());
		}
		return sb.toString();
	}

}