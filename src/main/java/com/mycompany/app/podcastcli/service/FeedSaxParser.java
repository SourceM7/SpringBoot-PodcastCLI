package com.mycompany.app.podcastcli.service;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.net.URL;

public class FeedSaxParser {

    public void parseFeed(String feedUrl) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();

            DefaultHandler handler = new DefaultHandler() {
                boolean inItem = false;
                boolean inTitle = false;
                boolean inDescription = false;
                int itemCount = 0;

                StringBuilder title = new StringBuilder();
                StringBuilder description = new StringBuilder();
                String enclosureUrl = "";

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                    if ("item".equalsIgnoreCase(qName)) {
                        inItem = true;
                    } else if (inItem) {
                        if ("title".equalsIgnoreCase(qName)) inTitle = true;
                        if ("description".equalsIgnoreCase(qName)) inDescription = true;
                        if ("enclosure".equalsIgnoreCase(qName)) {
                            enclosureUrl = attributes.getValue("url");
                        }
                    }
                }

                @Override
                public void characters(char[] ch, int start, int length) {
                    if (inTitle) title.append(ch, start, length);
                    if (inDescription) description.append(ch, start, length);
                }

                @Override
                public void endElement(String uri, String localName, String qName) {
                    if ("item".equalsIgnoreCase(qName)) {
                        System.out.println("Title: " + title.toString().trim());
                        System.out.println("Description: " + description.toString().trim());
                        System.out.println("Enclosure URL: " + enclosureUrl);
                        System.out.println("------");

                        itemCount++;
                        if (itemCount >= 3) throw new RuntimeException("Done after 3 items");

                        title.setLength(0);
                        description.setLength(0);
                        enclosureUrl = "";
                        inItem = false;
                    }

                    if ("title".equalsIgnoreCase(qName)) inTitle = false;
                    if ("description".equalsIgnoreCase(qName)) inDescription = false;
                }
            };

            parser.parse(new URL(feedUrl).openStream(), handler);
        } catch (RuntimeException doneSignal) {

        } catch (Exception e) {
            System.out.println("Parsing error: " + e.getMessage());
        }
    }
}
