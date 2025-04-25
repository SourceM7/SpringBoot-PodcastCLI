package com.mycompany.app.podcastcli.service;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FeedSaxParser {

    public static class Episode {
        private String title;
        private String description;
        private String audioUrl;

        public Episode(String title, String description, String audioUrl) {
            this.title = title;
            this.description = description;
            this.audioUrl = audioUrl;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getAudioUrl() {
            return audioUrl;
        }
    }

    public List<Episode> parseFeed(String feedUrl) {
        List<Episode> episodes = new ArrayList<>();

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
                        String titleStr = title.toString().trim();
                        String descriptionStr = description.toString().trim();

                        episodes.add(new Episode(titleStr, descriptionStr, enclosureUrl));

                        itemCount++;
                        if (itemCount >= 10) throw new RuntimeException("Done after 10 items");

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
            // Expected exception for limiting items
        } catch (Exception e) {
            System.out.println("Parsing error: " + e.getMessage());
        }

        return episodes;
    }
}