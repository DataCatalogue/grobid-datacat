package org.grobid.core.engines;
// GROBID features for high level segmentation
import org.grobid.core.features.FeaturesVectorDatacatSegmenter;
import org.grobid.core.features.FeaturesVectorSegmentation;

import eugfc.imageio.plugins.PNMRegistry;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.grobid.core.GrobidModels;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.document.*;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.exceptions.GrobidExceptionStatus;
import org.grobid.core.exceptions.GrobidResourceException;
import org.grobid.core.features.FeatureFactory;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.*;
import org.grobid.core.lexicon.FastMatcher;
import org.grobid.core.utilities.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;

import static org.apache.commons.lang3.StringUtils.*;

/**
 * Realise a high level segmentation of a monograph. Monograph is to be understood here in the context library cataloging,
 * basically as a standalone book. The monograph could be an ebook (novels), a conference proceedings volume, a book
 * collection volume, a phd/msc thesis, a standalone report (with toc, etc.), a manual (with multiple chapters).
 * Monographs, here, are NOT magazine volumes, journal issues, newspapers, standalone chapters, standalone scholar articles,
 * tables of content, reference works, dictionaries, encyclopedia volumes, graphic novels.
 *
 * @author Patrice Lopez
 */
public class DatacatSegmenterParser extends AbstractParser {
    /**
     * 17 labels for this model:
     * cover page (front of the book)
     * title page (secondary title page)
     * publisher page (publication information, including usually the copyrights info)
     * summary (include executive summary)
     * biography
     * advertising (other works by the author/publisher)
     * table of content
     * table/list of figures
     * preface (foreword)
     * dedication (I dedicate this label to my family and my thesis director ;)
     * unit (chapter or standalone article)
     * reference (a full chapter of references, not to be confused with references attached to an article)
     * annex
     * index
     * glossary (also abbreviations and acronyms)
     * back cover page
     * other
     */

    private static final Logger LOGGER = LoggerFactory.getLogger(DatacatSegmenterParser.class);

    private LanguageUtilities languageUtilities = LanguageUtilities.getInstance();

    // default bins for relative position
    private static final int NBBINS_POSITION = 12;

    // default bins for inter-block spacing
    private static final int NBBINS_SPACE = 5;

    // default bins for block character density
    private static final int NBBINS_DENSITY = 5;

    // projection scale for line length
    private static final int LINESCALE = 10;

    private FeatureFactory featureFactory = FeatureFactory.getInstance();

    private File tmpPath = null;

    public DatacatSegmenterParser() {
        super(GrobidModels.DATACAT_SEGMENTER);
    }

    /*
     * Machine-learning recognition of the complete monograph structures.
     *
     * Segment a PDF document into high level zones:
     *  cover, title, publisher, summary, biography, advertisement, toc, tof,
     *  preface, dedication, unit, reference, annex, index, glossary, back, other.
     *
     * @param input file
     * @return the built TEI
     */
    /**
     * Segment a PDF document into high level zones: cover page, document header,
     * page footer, page header, body, page numbers, biblio section and annexes.
     *
     * @param documentSource     document source
     * @return Document object with segmentation information
     */
    public Document processing(DocumentSource documentSource, GrobidAnalysisConfig config) {
        try {
            Document doc = new Document(documentSource);
            if (config.getAnalyzer() != null)
                doc.setAnalyzer(config.getAnalyzer());
            doc.addTokenizedDocument(config);
            doc = prepareDocument(doc);

            // if assets is true, the images are still there under directory pathXML+"_data"
            // we copy them to the assetPath directory

            File assetFile = config.getPdfAssetPath();
            if (assetFile != null) {
                dealWithImages(documentSource, doc, assetFile, config);
            }
            return doc;
        } finally {
            // keep it clean when leaving...
            /*if (config.getPdfAssetPath() == null) {
                // remove the pdfalto tmp file
                DocumentSource.close(documentSource, false, true, true);
            } else*/ {
                // remove the pdfalto tmp files, including the sub-directories
                DocumentSource.close(documentSource, true, true, true);
            }
        }
    }

    public Document processing(String text) {
        Document doc = Document.createFromText(text);
        return prepareDocument(doc);
    }

    public Document prepareDocument(Document doc) {

        List<LayoutToken> tokenizations = doc.getTokenizations();
        if (tokenizations.size() > GrobidProperties.getPdfTokensMax()) {
            throw new GrobidException("The document has " + tokenizations.size() + " tokens, but the limit is " + GrobidProperties.getPdfTokensMax(),
                    GrobidExceptionStatus.TOO_MANY_TOKENS);
        }

        doc.produceStatistics();
        String content = getAllLinesFeatured(doc);
        if (isNotEmpty(trim(content))) {
            String labelledResult = label(content);
            // set the different sections of the Document object
            doc = BasicStructureBuilder.generalResultSegmentation(doc, labelledResult, tokenizations);
        }
        return doc;
    }

    private void dealWithImages(DocumentSource documentSource, Document doc, File assetFile, GrobidAnalysisConfig config) {
        if (assetFile != null) {
            // copy the files under the directory pathXML+"_data" (the asset files) into the path specified by assetPath

            if (!assetFile.exists()) {
                // we create it
                if (assetFile.mkdir()) {
                    LOGGER.debug("Directory created: " + assetFile.getPath());
                } else {
                    LOGGER.error("Failed to create directory: " + assetFile.getPath());
                }
            }
            PNMRegistry.registerAllServicesProviders();

            // filter all .jpg and .png files
            File directoryPath = new File(documentSource.getXmlFile().getAbsolutePath() + "_data");
            if (directoryPath.exists()) {
                File[] files = directoryPath.listFiles();
                if (files != null) {
                    int nbFiles = 0;
                    for (final File currFile : files) {
                        if (nbFiles > DocumentSource.PDFALTO_FILES_AMOUNT_LIMIT)
                            break;

                        String toLowerCaseName = currFile.getName().toLowerCase();
                        if (toLowerCaseName.endsWith(".png") || !config.isPreprocessImages()) {
                            try {
                                if (toLowerCaseName.endsWith(".svg")) {
                                    continue;
                                }
                                FileUtils.copyFileToDirectory(currFile, assetFile);
                                nbFiles++;
                            } catch (IOException e) {
                                LOGGER.error("Cannot copy file " + currFile.getAbsolutePath() + " to " + assetFile.getAbsolutePath(), e);
                            }
                        } else if (toLowerCaseName.endsWith(".jpg")
                                || toLowerCaseName.endsWith(".ppm")
                            //	|| currFile.getName().toLowerCase().endsWith(".pbm")
                                ) {

                            String outputFilePath = "";
                            try {
                                final BufferedImage bi = ImageIO.read(currFile);

                                if (toLowerCaseName.endsWith(".jpg")) {
                                    outputFilePath = assetFile.getPath() + File.separator +
                                            toLowerCaseName.replace(".jpg", ".png");
                                }
                                /*else if (currFile.getName().toLowerCase().endsWith(".pbm")) {
                                    outputFilePath = assetFile.getPath() + File.separator +
                                         currFile.getName().toLowerCase().replace(".pbm",".png");
                                }*/
                                else {
                                    outputFilePath = assetFile.getPath() + File.separator +
                                            toLowerCaseName.replace(".ppm", ".png");
                                }
                                ImageIO.write(bi, "png", new File(outputFilePath));
                                nbFiles++;
                            } catch (IOException e) {
                                LOGGER.error("Cannot convert file " + currFile.getAbsolutePath() + " to " + outputFilePath, e);
                            }
                        }
                    }
                }
            }
            // update the path of the image description stored in Document
            if (config.isPreprocessImages()) {
                List<GraphicObject> images = doc.getImages();
                if (images != null) {
                    String subPath = assetFile.getPath();
                    int ind = subPath.lastIndexOf("/");
                    if (ind != -1)
                        subPath = subPath.substring(ind + 1, subPath.length());
                    for (GraphicObject image : images) {
                        String fileImage = image.getFilePath();
                        if (fileImage == null) {
                            continue;
                        }
                        fileImage = fileImage.replace(".ppm", ".png")
                                .replace(".jpg", ".png");
                        ind = fileImage.indexOf("/");
                        image.setFilePath(subPath + fileImage.substring(ind, fileImage.length()));
                    }
                }
            }
        }
    }

    /**
     * Addition of the features at line level for the complete document.
     * <p/>
     * This is an alternative to the token level, where the unit for labeling is the line - so allowing even
     * faster processing and involving less features.
     * Lexical features becomes block prefix and suffix, the feature text unit is the first 10 characters of the
     * block without space.
     * The dictionary flags are at block level (i.e. the block contains a name mention, a place mention, a year, etc.)
     * Regarding layout features: font, size and style are the one associated to the first token of the block.
     */
    public String getAllLinesFeatured(Document doc) {

        List<Block> blocks = doc.getBlocks();
        if ((blocks == null) || blocks.size() == 0) {
            return null;
        }

        //guaranteeing quality of service. Otherwise, there are some PDF that may contain 300k blocks and thousands of extracted "images" that ruins the performance
        if (blocks.size() > GrobidProperties.getPdfBlocksMax()) {
            throw new GrobidException("Post processed document is too big, contains: " + blocks.size(), GrobidExceptionStatus.TOO_MANY_BLOCKS);
        }

        //boolean graphicVector = false;
        //boolean graphicBitmap = false;

        // list of textual patterns at the head and foot of pages which can be re-occur on several pages
        // (typically indicating a publisher foot or head notes)
        Map<String, Integer> patterns = new TreeMap<String, Integer>();
        Map<String, Boolean> firstTimePattern = new TreeMap<String, Boolean>();

        for (Page page : doc.getPages()) {
            // we just look at the two first and last blocks of the page
            if ((page.getBlocks() != null) && (page.getBlocks().size() > 0)) {
                for(int blockIndex=0; blockIndex < page.getBlocks().size(); blockIndex++) {
                    if ( (blockIndex < 2) || (blockIndex > page.getBlocks().size()-2)) {
                        Block block = page.getBlocks().get(blockIndex);
                        String localText = block.getText();
                        if ((localText != null) && (localText.length() > 0)) {
                            String[] lines = localText.split("[\\n\\r]");
                            if (lines.length > 0) {
                                String line = lines[0];
                                String pattern = featureFactory.getPattern(line);
                                if (pattern.length() > 8) {
                                    Integer nb = patterns.get(pattern);
                                    if (nb == null) {
                                        patterns.put(pattern, Integer.valueOf(1));
                                        firstTimePattern.put(pattern, false);
                                    }
                                    else
                                        patterns.put(pattern, Integer.valueOf(nb+1));
                                }
                            }
                        }
                    }
                }
            }
        }

        String featuresAsString = getFeatureVectorsLinesAsString(doc,
            patterns, firstTimePattern);

        return featuresAsString;
    }

    private String getFeatureVectorsLinesAsString(Document doc, Map<String, Integer> patterns,
                                                  Map<String, Boolean> firstTimePattern) {
        StringBuilder monographFeatures = new StringBuilder();
        int documentLength = doc.getDocumentLenghtChar();

        String currentFont = null;
        int currentFontSize = -1;

        boolean newPage;
        boolean start = true;
        int mm = 0; // page position
        int nn = 0; // document position
        int pageLength = 0; // length of the current page
        double pageHeight = 0.0;

        // vector for features
        FeaturesVectorDatacatSegmenter features;
        FeaturesVectorDatacatSegmenter previousFeatures = null;

        for (Page page : doc.getPages()) {
            pageHeight = page.getHeight();
            newPage = true;
            double spacingPreviousBlock = 0.0; // discretized
            double lowestPos = 0.0;
            pageLength = page.getPageLengthChar();
            BoundingBox pageBoundingBox = page.getMainArea();
            mm = 0;
            //endPage = true;

            if ((page.getBlocks() == null) || (page.getBlocks().size() == 0))
                continue;

            for(int blockIndex=0; blockIndex < page.getBlocks().size(); blockIndex++) {
                Block block = page.getBlocks().get(blockIndex);
                /*if (start) {
                    newPage = true;
                    start = false;
                }*/
                boolean graphicVector = false;
                boolean graphicBitmap = false;

                boolean lastPageBlock = false;
                boolean firstPageBlock = false;
                if (blockIndex == page.getBlocks().size()-1) {
                    lastPageBlock = true;
                }

                if (blockIndex == 0) {
                    firstPageBlock = true;
                }

                // check if we have a graphical object connected to the current block
                List<GraphicObject> localImages = Document.getConnectedGraphics(block, doc);
                if (localImages != null) {
                    for(GraphicObject localImage : localImages) {
                        if (localImage.getType() == GraphicObjectType.BITMAP)
                            graphicBitmap = true;
                        if (localImage.getType() == GraphicObjectType.VECTOR || localImage.getType() == GraphicObjectType.VECTOR_BOX)
                            graphicVector = true;
                    }
                }

                if (lowestPos >  block.getY()) {
                    // we have a vertical shift, which can be due to a change of column or other particular layout formatting
                    spacingPreviousBlock = doc.getMaxBlockSpacing() / 5.0; // default
                } else
                    spacingPreviousBlock = block.getY() - lowestPos;

                String localText = block.getText();
                if (localText == null)
                    continue;

                // character density of the block
                double density = 0.0;
                if ( (block.getHeight() != 0.0) && (block.getWidth() != 0.0) &&
                    (block.getText() != null) && (!block.getText().contains("@PAGE")) &&
                    (!block.getText().contains("@IMAGE")) )
                    density = (double)block.getText().length() / (block.getHeight() * block.getWidth());

                // is the current block in the main area of the page or not?
                boolean inPageMainArea = true;
                BoundingBox blockBoundingBox = BoundingBox.fromPointAndDimensions(page.getNumber(),
                    block.getX(), block.getY(), block.getWidth(), block.getHeight());
                if (pageBoundingBox == null || (!pageBoundingBox.contains(blockBoundingBox) && !pageBoundingBox.intersect(blockBoundingBox)))
                    inPageMainArea = false;

                String[] lines = localText.split("[\\n\\r]");
                // set the max length of the lines in the block, in number of characters
                int maxLineLength = 0;
                for(int p=0; p<lines.length; p++) {
                    if (lines[p].length() > maxLineLength)
                        maxLineLength = lines[p].length();
                }
                List<LayoutToken> tokens = block.getTokens();
                if ((tokens == null) || (tokens.size() == 0)) {
                    continue;
                }
                for (int li = 0; li < lines.length; li++) {
                    String line = lines[li];

                    // for the layout information of the block, we take simply the first layout token
                    LayoutToken token = null;
                    if (tokens.size() > 0)
                        token = tokens.get(0);

                    double coordinateLineY = token.getY();

                    features = new FeaturesVectorDatacatSegmenter();
                    features.token = token;
                    features.line = line;

                    if ( (blockIndex < 2) || (blockIndex > page.getBlocks().size()-2)) {
                        String pattern = featureFactory.getPattern(line);
                        Integer nb = patterns.get(pattern);
                        if ((nb != null) && (nb > 1)) {
                            features.repetitivePattern = true;

                            Boolean firstTimeDone = firstTimePattern.get(pattern);
                            if ((firstTimeDone != null) && !firstTimeDone) {
                                features.firstRepetitivePattern = true;
                                firstTimePattern.put(pattern, true);
                            }
                        }
                    }

                    // we consider the first token of the line as usual lexical CRF token
                    // and the second token of the line as feature
                    StringTokenizer st2 = new StringTokenizer(line, " \t\f\u00A0");
                    // alternatively, use a grobid analyser
                    String text = null;
                    String text2 = null;
                    if (st2.hasMoreTokens())
                        text = st2.nextToken();
                    if (st2.hasMoreTokens())
                        text2 = st2.nextToken();

                    if (text == null)
                        continue;

                    // final sanitisation and filtering
                    text = text.replaceAll("[ \n\r]", "");
                    text = text.trim();

                    if ( (text.length() == 0) ||
                        (TextUtilities.filterLine(line))) {
                        continue;
                    }

                    features.string = text;
                    features.secondString = text2;

                    features.firstPageBlock = firstPageBlock;
                    features.lastPageBlock = lastPageBlock;
                    features.lineLength = featureFactory
                        .linearScaling(line.length(), maxLineLength, LINESCALE);

                    features.punctuationProfile = TextUtilities.punctuationProfile(line);

                    if (graphicBitmap) {
                        features.bitmapAround = true;
                    }
                    if (graphicVector) {
                        features.vectorAround = true;
                    }

                    features.lineStatus = null;
                    features.punctType = null;

                    if ((li == 0) ||
                        ((previousFeatures != null) && previousFeatures.blockStatus.equals("BLOCKEND"))) {
                        features.blockStatus = "BLOCKSTART";
                    } else if (li == lines.length - 1) {
                        features.blockStatus = "BLOCKEND";
                        //endblock = true;
                    } else if (features.blockStatus == null) {
                        features.blockStatus = "BLOCKIN";
                    }

                    if (newPage) {
                        features.pageStatus = "PAGESTART";
                        newPage = false;
                        //endPage = false;
                        if (previousFeatures != null)
                            previousFeatures.pageStatus = "PAGEEND";
                    } else {
                        features.pageStatus = "PAGEIN";
                        newPage = false;
                        //endPage = false;
                    }

                    if (text.length() == 1) {
                        features.singleChar = true;
                    }

                    if (Character.isUpperCase(text.charAt(0))) {
                        features.capitalisation = "INITCAP";
                    }

                    if (featureFactory.test_all_capital(text)) {
                        features.capitalisation = "ALLCAP";
                    }

                    if (featureFactory.test_digit(text)) {
                        features.digit = "CONTAINSDIGITS";
                    }

                    if (featureFactory.test_common(text)) {
                        features.commonName = true;
                    }

                    if (featureFactory.test_names(text)) {
                        features.properName = true;
                    }

                    if (featureFactory.test_month(text)) {
                        features.month = true;
                    }

                    Matcher m = featureFactory.isDigit.matcher(text);
                    if (m.find()) {
                        features.digit = "ALLDIGIT";
                    }

                    Matcher m2 = featureFactory.year.matcher(text);
                    if (m2.find()) {
                        features.year = true;
                    }

                    Matcher m3 = featureFactory.email.matcher(text);
                    if (m3.find()) {
                        features.email = true;
                    }

                    Matcher m4 = featureFactory.http.matcher(text);
                    if (m4.find()) {
                        features.http = true;
                    }

                    if (currentFont == null) {
                        currentFont = token.getFont();
                        features.fontStatus = "NEWFONT";
                    } else if (!currentFont.equals(token.getFont())) {
                        currentFont = token.getFont();
                        features.fontStatus = "NEWFONT";
                    } else
                        features.fontStatus = "SAMEFONT";

                    int newFontSize = (int) token.getFontSize();
                    if (currentFontSize == -1) {
                        currentFontSize = newFontSize;
                        features.fontSize = "HIGHERFONT";
                    } else if (currentFontSize == newFontSize) {
                        features.fontSize = "SAMEFONTSIZE";
                    } else if (currentFontSize < newFontSize) {
                        features.fontSize = "HIGHERFONT";
                        currentFontSize = newFontSize;
                    } else if (currentFontSize > newFontSize) {
                        features.fontSize = "LOWERFONT";
                        currentFontSize = newFontSize;
                    }

                    if (token.isBold())
                        features.bold = true;

                    if (token.isItalic())
                        features.italic = true;

                    if (features.capitalisation == null)
                        features.capitalisation = "NOCAPS";

                    if (features.digit == null)
                        features.digit = "NODIGIT";

                    features.relativeDocumentPosition = featureFactory
                        .linearScaling(nn, documentLength, NBBINS_POSITION);

                    features.relativePagePositionChar = featureFactory
                        .linearScaling(mm, pageLength, NBBINS_POSITION);

                    int pagePos = featureFactory
                        .linearScaling(coordinateLineY, pageHeight, NBBINS_POSITION);

                    if (pagePos > NBBINS_POSITION)
                        pagePos = NBBINS_POSITION;
                    features.relativePagePosition = pagePos;

                    if (spacingPreviousBlock != 0.0) {
                        features.spacingWithPreviousBlock = featureFactory
                            .linearScaling(spacingPreviousBlock-doc.getMinBlockSpacing(), doc.getMaxBlockSpacing()-doc.getMinBlockSpacing(), NBBINS_SPACE);
                    }

                    features.inMainArea = inPageMainArea;

                    if (density != -1.0) {
                        features.characterDensity = featureFactory
                            .linearScaling(density-doc.getMinCharacterDensity(), doc.getMaxCharacterDensity()-doc.getMinCharacterDensity(), NBBINS_DENSITY);
                    }

                    if (previousFeatures != null) {
                        String vector = previousFeatures.printVector();
                        monographFeatures.append(vector);
                    }
                    previousFeatures = features;
                }

                // lowest position of the block
                lowestPos = block.getY() + block.getHeight();

                // update page-level and document-level positions
                if (tokens != null) {
                    mm += tokens.size();
                    nn += tokens.size();
                }
            }
        }
        if (previousFeatures != null)
            monographFeatures.append(previousFeatures.printVector());

        return monographFeatures.toString();
    }

    /**
     * Create a blank training data for new monograph model
     *
     * @param inputFile input PDF file
     * @param pathRaw   path to raw monograph featured sequence
     * @param pathTEI   path to TEI, the file is not labeled yet
     * @param id        id
     */
    public void createBlankTrainingFromPDF(File inputFile,
                                               String pathRaw,
                                               String pathTEI,
                                               int id) {
        DocumentSource documentSource = null;

        try {
            if (!inputFile.exists()) {
                throw new GrobidResourceException("Cannot train the Segmenter model, because file '" +
                    inputFile.getAbsolutePath() + "' does not exists.");
            }

            // read from the first until the last page of the document
            documentSource = DocumentSource.fromPdf(inputFile, -1, -1, true, true, true);
            Document doc = new Document(documentSource);

            String pdfFileName = inputFile.getName();
            File outputTEIFile = new File(pathTEI + File.separator + pdfFileName.replace(" ", "_").replace(".pdf", ".training.monograph.tei.xml"));
            File outputRawFile = new File(pathRaw + File.separator + pdfFileName.replace(" ", "_").replace(".pdf", ".training.monograph"));

            doc.addTokenizedDocument(GrobidAnalysisConfig.defaultInstance());

            if (doc.getBlocks() == null) {
                throw new Exception("PDF parsing resulted in empty content");
            }
            doc.produceStatistics();
            List<Block> blocks = doc.getBlocks();

            if (blocks == null) {
                throw new Exception("PDF parsing resulted in empty content");
            } else {
                // detect the language
                String contentSample = "";
                int sampleLength = 0;
                for (int i = 0; i < blocks.size(); i++) {
                    contentSample += doc.getBlocks().get(i).getText();
                    if (sampleLength > 500) // it's assumed we need 500 characters of sample content for detecting the language
                        break;
                }

                Language langID = languageUtilities.getInstance().runLanguageId(contentSample);
                String lang;
                if (langID != null) {
                    lang = langID.getLang();
                } else {
                    lang = "fr"; // by default, id is "english"
                }

                // if we work with blocks
                //String monographFeatures =  getAllBlocksFeatured(doc);

                // if we work with lines
                String segmenterFeatures = getAllLinesFeatured(doc);
                List<LayoutToken> tokenizations = doc.getTokenizations(); // the tokenization for all documents

                // we write the data with features yet unlabeled
                Writer writer = new OutputStreamWriter(new FileOutputStream(outputRawFile, false), "UTF-8");
                writer.write(segmenterFeatures + "\n");
                writer.close();
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid training" +
                " data generation for monograph.", e);
        } finally {
            DocumentSource.close(documentSource, true, true, true);
        }
    }

    /**
     * Create a training data with the TEI labels generated by the monograph model
     *
     * @param inputFile input PDF file
     * @param pathRaw   path to raw monograph featured sequence
     * @param pathTEI   path to TEI, the file is not labeled yet
     * @param id        id
     */
    public void createTrainingFromPDF(File inputFile,
                                      String pathRaw,
                                      String pathTEI,
                                      int id) {
        DocumentSource documentSource = null;

        try {
            if (!inputFile.exists()) {
                throw new GrobidResourceException("Cannot train the Segmenter model, because file '" +
                    inputFile.getAbsolutePath() + "' does not exists.");
            }

            // read from the first until the last page of the document
            documentSource = DocumentSource.fromPdf(inputFile, -1, -1, true, true, true);
            Document doc = new Document(documentSource);

            String pdfFileName = inputFile.getName();
            File outputTEIFile = new File(pathTEI + File.separator + pdfFileName.replace(" ", "_").replace(".pdf", ".training.monograph.tei.xml"));
            File outputRawFile = new File(pathRaw + File.separator + pdfFileName.replace(" ", "_").replace(".pdf", ".training.monograph"));

            doc.addTokenizedDocument(GrobidAnalysisConfig.defaultInstance());

            if (doc.getBlocks() == null) {
                throw new Exception("PDF parsing resulted in empty content");
            }
            doc.produceStatistics();
            List<Block> blocks = doc.getBlocks();

            if (blocks == null) {
                throw new Exception("PDF parsing resulted in empty content");
            } else {
                // detect the language
                String contentSample = "";
                int sampleLength = 0;
                for (int i = 0; i < blocks.size(); i++) {
                    contentSample += doc.getBlocks().get(i).getText();
                    if (sampleLength > 500) // it's assumed we need 500 characters of sample content for detecting the language
                        break;
                }

                Language langID = languageUtilities.getInstance().runLanguageId(contentSample);
                String lang;
                if (langID != null) {
                    lang = langID.getLang();
                } else {
                    lang = "fr"; // by default, id is "english"
                }

                // if we work with blocks
                //String monographFeatures =  getAllBlocksFeatured(doc);

                // if we work with lines
                String segmenterFeatures =  getAllLinesFeatured(doc);
                List<LayoutToken> tokenizations = doc.getTokenizations(); // the tokenization for all documents

                // we write the data with features yet unlabeled
                Writer writer = new OutputStreamWriter(new FileOutputStream(outputRawFile, false), "UTF-8");
                writer.write(segmenterFeatures + "\n");
                writer.close();

                if (segmenterFeatures != null) {
                    // we call the Monograph model for giving labels on the text
                    String rese = label(segmenterFeatures);
                    StringBuffer bufferMonograph = trainingExtraction(rese, tokenizations, doc);

                    // write the TEI file to reflect the extact layout of the text as extracted from the pdf
                    writer = new OutputStreamWriter(new FileOutputStream(outputTEIFile, false), "UTF-8");
                    writer.write("<?xml version=\"1.0\" ?>\n<tei xml:space=\"preserve\">\n\t<teiHeader>\n\t\t<fileDesc xml:id=\"" + id +
                        "\"/>\n\t</teiHeader>\n\t<text xml:lang=\"" + lang + "\">\n");
                    writer.write(bufferMonograph.toString());
                    writer.write("\n\t</text>\n</tei>\n");
                    writer.close();
                }
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid training" +
                " data generation for monograph.", e);
        } finally {
            DocumentSource.close(documentSource, true, true, true);
        }
    }

    public void extractTextFromPdf(File file, String pathFullText, int id) {
        DocumentSource documentSource = null;
        try {
            documentSource = DocumentSource.fromPdf(file, -1, -1, true, true, true);
            Document doc = new Document(documentSource);

            String PDFFileName = file.getName();
            doc.addTokenizedDocument(GrobidAnalysisConfig.defaultInstance());

            if (doc.getBlocks() == null) {
                throw new Exception("PDF parsing resulted in empty content");
            }
            doc.produceStatistics();

            List<LayoutToken> tokenizations = doc.getTokenizations();

            // TODO language identifier here on content text sample
            String lang = null;
            String text = doc.getBlocks().get(0).getText(); // get only the text from the first block as example to recognize the language
            Language langID = languageUtilities.getInstance().runLanguageId(text);
            if (langID != null) {
                lang = langID.getLang();
            } else {
                lang = "fr"; // by default, id = english
            }

            // also write the raw text as seen before segmentation
            StringBuffer rawtxt = new StringBuffer();
            for (LayoutToken txtline : tokenizations) {
                rawtxt.append(txtline.getText());
            }
            String outPathRawtext = pathFullText + File.separator +
                PDFFileName.replace(".pdf", ".datacat.rawtxt");
            FileUtils.writeStringToFile(new File(outPathRawtext), rawtxt.toString(), "UTF-8");

        } catch (Exception e) {
            throw new GrobidException("An exception occured while running Grobid training" +
                " data generation for monograph model.", e);
        } finally {
            DocumentSource.close(documentSource, true, true, true);
        }
    }

    /**
     * Extract results from a labelled full text in the training format without any string modification.
     *
     * @param result        reult
     * @param tokenizations toks
     * @return extraction
     */
    public StringBuffer trainingExtraction(String result,
                                            List<LayoutToken> tokenizations,
                                            Document doc) {
        // this is the main buffer for the whole full text
        StringBuffer buffer = new StringBuffer();
        try {
            List<Block> blocks = doc.getBlocks();
            int currentBlockIndex = 0;
            int indexLine = 0;

            StringTokenizer st = new StringTokenizer(result, "\n");
            String s1 = null; // current label/tag
            String s2 = null; // current lexical token
            String s3 = null; // current second lexical token
            String lastTag = null;

            // current token position
            int p = 0;
            boolean start = true;

            while (st.hasMoreTokens()) {
                boolean addSpace = false;
                String tok = st.nextToken().trim();
                String line = null; // current line

                if (tok.length() == 0) {
                    continue;
                }
                StringTokenizer stt = new StringTokenizer(tok, " \t");
                List<String> localFeatures = new ArrayList<String>();
                int i = 0;

                boolean newLine = true;
                int ll = stt.countTokens();
                while (stt.hasMoreTokens()) {
                    String s = stt.nextToken().trim();
                    if (i == 0) {
                        s2 = TextUtilities.HTMLEncode(s); // lexical token
                    } else if (i == 1) {
                        s3 = TextUtilities.HTMLEncode(s); // second lexical token
                    } else if (i == ll - 1) {
                        s1 = s; // current label
                    } else {
                        localFeatures.add(s); // we keep the feature values in case they appear useful
                    }
                    i++;
                }

                // as we process the document segmentation line by line, we don't use the usual
                // tokenization to rebuild the text flow, but we get each line again from the
                // text stored in the document blocks (similarly as when generating the features)
                line = null;
                while ((line == null) && (currentBlockIndex < blocks.size())) {
                    Block block = blocks.get(currentBlockIndex);
                    List<LayoutToken> tokens = block.getTokens();
                    if (tokens == null) {
                        currentBlockIndex++;
                        indexLine = 0;
                        continue;
                    }
                    String localText = block.getText();
                    if ((localText == null) || (localText.trim().length() == 0)) {
                        currentBlockIndex++;
                        indexLine = 0;
                        continue;
                    }
                    //String[] lines = localText.split("\n");
                    String[] lines = localText.split("[\\n\\r]");
                    if ((lines.length == 0) || (indexLine >= lines.length)) {
                        currentBlockIndex++;
                        indexLine = 0;
                        continue;
                    } else {
                        line = lines[indexLine];
                        indexLine++;
                        if (line.trim().length() == 0) {
                            line = null;
                            continue;
                        }

                        if (TextUtilities.filterLine(line)) {
                            line = null;
                            continue;
                        }
                    }
                }

                line = TextUtilities.HTMLEncode(line);

                if (newLine && !start) {
                    buffer.append("<lb/>");
                }

                String lastTag0 = null;
                if (lastTag != null) {
                    if (lastTag.startsWith("I-")) {
                        lastTag0 = lastTag.substring(2, lastTag.length());
                    } else {
                        lastTag0 = lastTag;
                    }
                }
                String currentTag0 = null;
                if (s1 != null) {
                    if (s1.startsWith("I-")) {
                        currentTag0 = s1.substring(2, s1.length());
                    } else {
                        currentTag0 = s1;
                    }
                }

                //boolean closeParagraph = false;
                if (lastTag != null) {
                    //closeParagraph =
                    testClosingTag(buffer, currentTag0, lastTag0, s1);
                }

                boolean output;

                output = writeField(buffer, line, s1, lastTag0, s2, "<front>", "<front>", addSpace, 3);
                if (!output) {
                    output = writeField(buffer, line, s1, lastTag0, s2, "<other>", "<other>", addSpace, 3);
                }
                if (!output) {
                    output = writeField(buffer, line, s1, lastTag0, s2, "<body>", "<body>",
                        addSpace, 3);
                }
                if (!output) {
                    output = writeField(buffer, line, s1, lastTag0, s2, "<back>", "<back>>",
                        addSpace, 3);
                }
                if (!output) {
                    output = writeField(buffer, line, s1, lastTag0, s2, "<annex>", "<annex>",
                        addSpace, 3);
                }
                lastTag = s1;

                if (!st.hasMoreTokens()) {
                    if (lastTag != null) {
                        testClosingTag(buffer, "", currentTag0, s1);
                    }
                }
                if (start) {
                    start = false;
                }
            }

            return buffer;
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
    }

    /**
     * TODO some documentation...
     *
     * @param buffer
     * @param s1
     * @param lastTag0
     * @param s2
     * @param field
     * @param outField
     * @param addSpace
     * @param nbIndent
     * @return
     */
    private boolean writeField(StringBuffer buffer,
                               String line,
                               String s1,
                               String lastTag0,
                               String s2,
                               String field,
                               String outField,
                               boolean addSpace,
                               int nbIndent) {
        boolean result = false;
        // filter the output path
        if ((s1.equals(field)) || (s1.equals("I-" + field))) {
            result = true;
            line = line.replace("@BULLET", "\u2022");
            // if previous and current tag are the same, we output the token
            if (s1.equals(lastTag0) || s1.equals("I-" + lastTag0)) {
                buffer.append(line);
            } else if (lastTag0 == null) {
                // if previous tagname is null, we output the opening xml tag
                for (int i = 0; i < nbIndent; i++) {
                    buffer.append("\t");
                }
                buffer.append(outField).append(line);
            } else {
                // new opening tag, we output the opening xml tag
                for (int i = 0; i < nbIndent; i++) {
                    buffer.append("\t");
                }
                buffer.append(outField).append(line);
            } /*else {
                // otherwise we continue by ouputting the token
                buffer.append(line);
            }*/
        }
        return result;
    }

    /**
     *
     * @param buffer
     * @param currentTag0
     * @param lastTag0
     * @param currentTag
     * @return
     */
    private boolean testClosingTag(StringBuffer buffer,
                                   String currentTag0,
                                   String lastTag0,
                                   String currentTag) {
        boolean res = false;
        // reference_marker and citation_marker are two exceptions because they can be embedded

        if (!currentTag0.equals(lastTag0)) {
            /*if (currentTag0.equals("<citation_marker>") || currentTag0.equals("<figure_marker>")) {
                return res;
            }*/

            res = false;
            // we close the current tag
            if (lastTag0.equals("<front>")) {
                buffer.append("</front>\n\n");
                res = true;
            } else if (lastTag0.equals("<body>")) {
                buffer.append("</body>\n\n");
                res = true;
            } else if (lastTag0.equals("<back>")) {
                buffer.append("</back>\n\n");
                res = true;
            } else if (lastTag0.equals("<annex>")) {
                buffer.append("</annex>\n\n");
                res = true;
            } else if (lastTag0.equals("<other>")) {
                buffer.append("</other>\n\n");
            } else {
                res = false;
            }

        }
        return res;
    }

    @Override
    public void close() throws IOException {
        super.close();
        // ...
    }

}