package org.grobid.trainer.sax;

import org.grobid.core.utilities.TextUtilities;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.List;
import java.util.ArrayList;
import java.util.Stack;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapted from the SAX parser for the TEI format for monograph data. Normally all training data should be in this unique format.
 * The segmentation of tokens must be identical as the one from pdf2xml files so that
 * training and online input tokens are aligned.
 *
 * @author Patrice Lopez
 */
public class TEIDatacatSegmenterSaxParser extends DefaultHandler {

    private static final Logger logger = LoggerFactory.getLogger(TEIDatacatSegmenterSaxParser.class);

    private StringBuffer accumulator = null; // current accumulated text

    private String output = null;
    //private Stack<String> currentTags = null;
    private String currentTag = null;
    private String upperQname = null;
    private String upperTag = null;
    private List<String> labeled = null; // store line by line the labeled data

    public TEIDatacatSegmenterSaxParser() {
        labeled = new ArrayList<String>();
        //currentTags = new Stack<String>();
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
        if (qName.equals("body") ||
            qName.equals("back") ||
            qName.equals("front") ||
            qName.equals("annex") ||
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

            if (qName.equals("front")) {
                //currentTags.push("<header>");
                currentTag = "<front>";
                upperTag = currentTag;
                upperQname = "front";
            } else if (qName.equals("body")) {
                //currentTags.push("<other>");
                currentTag = "<body>";
                upperTag = currentTag;
                upperQname = "body";
            } else if (qName.equals("back")) {
                //currentTags.push("<other>");
                currentTag = "<back>";
                //upperTag = currentTag;
                //upperQname = "titlePage";
            } else if (qName.equals("other")) {
                //currentTags.push("<other>");
                currentTag = "<other>";
            } else if (qName.equals("annex")) {
                currentTag = "<annex>";
                //upperTag = currentTag;
                upperQname = "annex";
            }
        }
    }

    private void writeData(String qName, String surfaceTag) {
        if (qName == null) {
            qName = "other";
            surfaceTag = "<other>";
        }
        if ((qName.equals("front")) || (qName.equals("body")) || (qName.equals("back")) ||
            (qName.equals("annex")) || (qName.equals("other"))
        ) {
            String text = getText();
            text = text.replace("\n", " ");
            text = text.replace("\r", " ");
            text = text.replace("  ", " ");
            boolean begin = true;

            // we segment the text line by line first
            String[] tokens = text.split("\\+L\\+");
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
                    line = line.replace("+PAGE+", "");
                    page = true;
                }

                StringTokenizer st = new StringTokenizer(line, " \t\f\u00A0");
                if (!st.hasMoreTokens())
                    continue;
                String tok = st.nextToken();

                if (tok.length() == 0)
                    continue;

                if (surfaceTag == null) {
                    // this token belongs to a chunk to ignored
                    surfaceTag = "<other>";
                }

                if (begin && (!surfaceTag.equals("<other>"))) {
                    labeled.add(tok + " I-" + surfaceTag + "\n");
                    begin = false;
                } else {
                    labeled.add(tok + " " + surfaceTag + "\n");
                }
                if (page) {
                    page = false;
                }
            }
            accumulator.setLength(0);
        }
    }

}