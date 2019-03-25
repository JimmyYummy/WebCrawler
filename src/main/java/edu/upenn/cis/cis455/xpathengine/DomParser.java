package edu.upenn.cis.cis455.xpathengine;
import org.ccil.cowan.tagsoup.Parser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilder;

public class DomParser {
	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		InputSource is = new InputSource();
		String exampleString = "<hello><second>123</second></hello>";
		InputStream byteStream = new ByteArrayInputStream(exampleString.getBytes(StandardCharsets.UTF_8));
		is.setByteStream(byteStream);
		Document d = dBuilder.parse(is);
		Node n = (Node) d;
		System.out.println(n.getChildNodes().item(0).getTextContent());
	}
}
