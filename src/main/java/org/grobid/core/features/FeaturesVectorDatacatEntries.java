package org.grobid.core.features;

import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.TextUtilities;

public class FeaturesVectorDatacatEntries {

    public LayoutToken token = null; // not a feature, reference value
    public String line = null; // not a feature, the complete processed line

    public String string = null; // first lexical feature
    public String blockStatus = null; // one of BLOCKSTART, BLOCKIN, BLOCKEND
    public String lineStatus = null; // one of LINESTART, LINEIN, LINEEND
    public String fontStatus = null; // one of NEWFONT, SAMEFONT
    public String fontSize = null; // one of HIGHERFONT, SAMEFONTSIZE, LOWERFONT
    public String pageStatus = null; // one of PAGESTART, PAGEIN, PAGEEND
    public boolean bold = false;
    public boolean italic = false;
    public String capitalisation = null; // one of INITCAP, ALLCAPS, NOCAPS
    public String digit;  // one of ALLDIGIT, CONTAINDIGIT, NODIGIT
    public boolean singleChar = false;
    public boolean properName = false;
    public boolean commonName = false;
    public boolean firstName = false;
    public String punctType = null; // one of NOPUNCT, OPENBRACKET, ENDBRACKET, DOT, COMMA, HYPHEN, QUOTE, PUNCT (default)
    public int relativeDocumentPosition = -1;
    public int relativePagePosition = -1;
    public String punctuationProfile = null; // the punctuations of the current line of the token
    public int lineLength = 0;
    public boolean inMainArea = true;
    public boolean repetitivePattern = false; // if true, the textual pattern is repeated at the same position on other pages
    public boolean firstRepetitivePattern = false; // if true, this is a repetitive textual pattern and this is its first occurrence in the doc
    public int spacingWithPreviousBlock = 0; // discretized
    public int characterDensity = 0; // discretized

    public String printVector() {
        if (string == null) return null;
        if (string.length() == 0) return null;
        StringBuffer res = new StringBuffer();

        // token string (0)
        res.append(string);

        // lowercase first string (2)
        res.append(" " + string.toLowerCase());

        // prefix (3-6)
        res.append(" " + TextUtilities.prefix(string, 1));
        res.append(" " + TextUtilities.prefix(string, 2));
        res.append(" " + TextUtilities.prefix(string, 3));
        res.append(" " + TextUtilities.prefix(string, 4));

        // block information (7)
        if (blockStatus != null)
            res.append(" " + blockStatus);

        // line information (8)
        if (lineStatus != null)
            res.append(" " + lineStatus);

        // page information (9)
        res.append(" " + pageStatus);

        // font information (10)
        res.append(" " + fontStatus);

        // font size information (11)
        res.append(" " + fontSize);

        // string type information (12)
        if (bold)
            res.append(" 1");
        else
            res.append(" 0");

        // Italic (13)
        if (italic)
            res.append(" 1");
        else
            res.append(" 0");

        // capitalisation (14)
        if (digit.equals("ALLDIGIT"))
            res.append(" NOCAPS");
        else
            res.append(" " + capitalisation);

        // digit information (15)
        res.append(" " + digit);

        // character information (16)
        if (singleChar)
            res.append(" 1");
        else
            res.append(" 0");

        // lexical information (17-19)
        if (properName)
            res.append(" 1");
        else
            res.append(" 0");

        if (commonName)
            res.append(" 1");
        else
            res.append(" 0");

        if (firstName)
            res.append(" 1");
        else
            res.append(" 0");

        // punctuation information (20)
        if (punctType != null)
            res.append(" " + punctType); // in case the token is a punctuation (NO otherwise)

        // relative document position (21)
        res.append(" " + relativeDocumentPosition);

        // relative page position coordinate (22)
        res.append(" " + relativePagePosition);

        // punctuation profile (23-24)
        if ( (punctuationProfile == null) || (punctuationProfile.length() == 0) ) {
            // string profile
            res.append(" no");
            // number of punctuation symbols in the line
            res.append(" 0");
        }
        else {
            // string profile
            res.append(" " + punctuationProfile);
            // number of punctuation symbols in the line
            res.append(" "+punctuationProfile.length());
        }

        // current line length on a predefined scale and relative to the longest line of the current block (25)
        res.append(" " + lineLength);

        // current block length on a predefined scale and relative to the longest block of the current page
        //res.append(" " + blockLength);

        // (26)
        if (repetitivePattern) {
            res.append(" 1");
        } else {
            res.append(" 0");
        }

        // (24)
        if (firstRepetitivePattern) {
            res.append(" 1");
        } else {
            res.append(" 0");
        }

        // if the block is in the page main area (28)
        if (inMainArea) {
            res.append(" 1");
        } else {
            res.append(" 0");
        }

        res.append("\n");

        return res.toString();
    }

}
