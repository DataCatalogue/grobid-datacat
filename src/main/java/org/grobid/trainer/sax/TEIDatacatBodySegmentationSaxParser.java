package org.grobid.trainer.sax;

import org.grobid.core.utilities.TextUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.*;

public class TEIDatacatBodySegmentationSaxParser extends DefaultHandler {

    private static final Logger logger = LoggerFactory.getLogger(TEIFulltextSaxParser.class);

    private StringBuffer accumulator = null; // current accumulated text

    private String output = null;
    private Stack<String> currentTags = null;
    private String currentTag = null;

    private ArrayList<String> labeled = null; // store line by line the labeled data

    private List<String> endTags = Arrays.asList("catEntry");

    public TEIDatacatBodySegmentationSaxParser() {
        labeled = new ArrayList<String>();
        currentTags = new Stack<String>();
        accumulator = new StringBuffer();
    }

    public void characters(char[] buffer, int start, int length) {
        accumulator.append(buffer, start, length);
    }

    public String getText() {
        if (accumulator != null) {
            //System.out.println(accumulator.toString().trim());
            return accumulator.toString().trim();
        } else {
            return null;
        }
    }

    public List<String> getLabeledResult() {
        return labeled;
    }

    public void endElement(java.lang.String uri,
                           java.lang.String localName,
                           java.lang.String qName) throws SAXException {
        if (endTags.contains(qName)) {
            writeData();
            accumulator.setLength(0);
        } else if (qName.equals("entry")) {
            // write remaining test as <other>
            String text = getText();
            if (text != null) {
                if (text.length() > 0) {
                    currentTag = "<entry>";
                    writeData();
                }
            }
            accumulator.setLength(0);
        } else {
            System.out.println(" **** Warning **** Unexpected closing tag " + qName);
        }
    }

    public void startElement(String namespaceURI,
                             String localName,
                             String qName,
                             Attributes atts)
        throws SAXException {
        if (qName.equals("lb")) {
            accumulator.append(" ");
        } else {
            // add acumulated text as <other>
            String text = getText();
            if (text != null) {
                if (text.length() > 0) {
                    currentTag = "<other>";
                    writeData();
                }
            }
            accumulator.setLength(0);
        }

        if (qName.equals("entry")) {
            currentTag = "<entry>";
        }
    }

    private void writeData() {
        if (currentTag == null) {
            return;
        }

        String text = getText();
        // we segment the text
        StringTokenizer st = new StringTokenizer(text, TextUtilities.delimiters, true);
        boolean begin = true;
        while (st.hasMoreTokens()) {
            String tok = st.nextToken().trim();
            if (tok.length() == 0)
                continue;

            String content = tok;
            int i = 0;
            if (content.length() > 0) {
                if (begin) {
                    labeled.add(content + " I-" + currentTag + "\n");
                    begin = false;
                } else {
                    labeled.add(content + " " + currentTag + "\n");
                }
            }
            begin = false;
        }
        accumulator.setLength(0);
    }

    /*private static final Logger logger = LoggerFactory.getLogger(TEIDatacatSegmenterSaxParser.class);

    private StringBuffer accumulator = null; // current accumulated text

    private String output = null;
    //private Stack<String> currentTags = null;
    private String currentTag = null;
    private String upperQname = null;
    private String upperTag = null;
    private List<String> labeled = null; // store line by line the labeled data

    public TEIDatacatBodySegmentationSaxParser() {
        labeled = new ArrayList<String>();
        accumulator = new StringBuffer();
    }

    public void characters(char[] buffer, int start, int length) {
        accumulator.append(buffer, start, length);
    }

    public String getText() {
        if (accumulator != null) {
            //System.out.println(accumulator.toString().trim());
            return accumulator.toString().trim();
        } else {
            return null;
        }
    }

    public List<String> getLabeledResult() {
        return labeled;
    }

    public void endElement(java.lang.String uri,
                           java.lang.String localName,
                           java.lang.String qName) throws SAXException {
        if ((!qName.equals("lb")) && (!qName.equals("pb") )) {
            writeData(qName, currentTag);
        }
        if (qName.equals("catentry") ||
            qName.equals("other")) {
            currentTag = null;
            upperTag = null;
        }
    }

    public void startElement(String namespaceURI,
                             String localName,
                             String qName,
                             Attributes atts)
        throws SAXException {
        if (qName.equals("lb")) {
            accumulator.append(" +L+ ");
        } else if (qName.equals("pb")) {
            accumulator.append(" +PAGE+ ");
        } else if (qName.equals("space")) {
            accumulator.append(" ");
        } else {
            // we have to write first what has been accumulated yet with the upper-level tag
            String text = getText();
            if (text != null) {
                if (text.length() > 0) {
                    writeData(upperQname, upperTag);
                }
            }
            //accumulator.setLength(0);

            if (qName.equals("catentry")) {
                //currentTags.push("<header>");
                currentTag = "<catentry>";
                upperTag = currentTag;
                upperQname = "catentry";
            } else if (qName.equals("other")) {
                //currentTags.push("<other>");
                currentTag = "<other>";
            } *//*else {
                logger.error("Invalid element name: " + qName + " - it will be mapped to the label <other>");
                currentTag = "<other>";
            }*//*
        }
    }

    private void writeData(String qName, String surfaceTag) {
        if (qName == null) {
            qName = "other";
            surfaceTag = "<other>";
        }
        if ((qName.equals("catentry")) || (qName.equals("other"))) {
            String text = getText();
            text = text.replace("\n", " ");
            text = text.replace("\r", " ");
            text = text.replace("  ", " ");
            boolean begin = true;
//System.out.println(text);
            // we segment the text line by line first
            //StringTokenizer st = new StringTokenizer(text, "\n", true);
            String[] tokens = text.split("\\+L\\+");
            //while (st.hasMoreTokens()) {
            boolean page = false;
            for(int p=0; p<tokens.length; p++) {
                //String line = st.nextToken().trim();
                String line = tokens[p].trim();
                if (line.length() == 0)
                    continue;
                if (line.equals("\n") || line.equals("\r"))
                    continue;
                if (line.indexOf("+PAGE+") != -1) {
                    // page break should be a distinct feature
                    //labeled.add("@newpage\n");
                    line = line.replace("+PAGE+", "");
                    page = true;
                }

                //StringTokenizer st = new StringTokenizer(line, " \t");
                StringTokenizer st = new StringTokenizer(line, " \t\f\u00A0");
                if (!st.hasMoreTokens())
                    continue;
                String tok = st.nextToken();

                *//*StringTokenizer st = new StringTokenizer(line, TextUtilities.delimiters, true);
                if (!st.hasMoreTokens())
                    continue;
                String tok = st.nextToken().trim();*//*

                if (tok.length() == 0)
                    continue;

                if (surfaceTag == null) {
                    // this token belongs to a chunk to ignored
                    //System.out.println("\twarning: surfaceTag is null for token '"+tok+"' - it will be tagged with label <other>");
                    surfaceTag = "<other>";
                }

                if (begin && (!surfaceTag.equals("<other>"))) {
                    labeled.add(tok + " I-" + surfaceTag + "\n");
                    begin = false;
                } else {
                    labeled.add(tok + " " + surfaceTag + "\n");
                }
                if (page) {
                    //labeled.add("@newpage\n");
                    page = false;
                }
                //}
            }
            accumulator.setLength(0);
        }
    }*/

}