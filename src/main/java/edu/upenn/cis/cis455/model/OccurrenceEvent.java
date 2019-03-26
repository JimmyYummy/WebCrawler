package edu.upenn.cis.cis455.model;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Parser;

/**
 * TODO: this class encapsulates the data from a keyword "occurrence"
 */
public class OccurrenceEvent {

	private Element root;
	
	public OccurrenceEvent(String xml) {
		if (xml == null) throw new IllegalArgumentException();
		root = Jsoup.parse(xml, "", Parser.xmlParser());
	}
	
	public Element getRootNode() {
		return root;
	}
}
