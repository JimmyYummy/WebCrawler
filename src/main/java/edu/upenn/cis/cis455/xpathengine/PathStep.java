package edu.upenn.cis.cis455.xpathengine;

import java.util.ArrayList;
import java.util.List;

public class PathStep {
	private String nodeName;
	private PathStep next;
	private List<String> texts;
	private List<String> contains;
	
	
	public PathStep(String nodeName) {
		this.nodeName = nodeName;
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
	
	
}