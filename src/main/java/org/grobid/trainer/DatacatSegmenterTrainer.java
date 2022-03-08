package org.grobid.trainer;

import org.grobid.core.GrobidModels;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.UnicodeUtil;
import org.grobid.trainer.sax.TEIDatacatSegmenterSaxParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.commons.io.FileUtils;

public class DatacatSegmenterTrainer extends AbstractTrainer {

    public DatacatSegmenterTrainer() {
        super(GrobidModels.DATACAT_SEGMENTER);
    }

    @Override
    public int createCRFPPData(File corpusPath, File outputFile) {
        return addFeaturesDatacatSegmenter(corpusPath.getAbsolutePath() + "/tei",
            corpusPath.getAbsolutePath() + "/raw",
            outputFile, null, 1.0);
    }

    /**
     * Add the selected features for the monograph model
     *
     * @param corpusDir          path where corpus files are located
     * @param trainingOutputPath path where to store the temporary training data
     * @param evalOutputPath     path where to store the temporary evaluation data
     * @param splitRatio         ratio to consider for separating training and evaluation data, e.g. 0.8 for 80%
     * @return the total number of used corpus items
     */
    @Override
    public int createCRFPPData(final File corpusDir,
                               final File trainingOutputPath,
                               final File evalOutputPath,
                               double splitRatio) {
        return addFeaturesDatacatSegmenter(corpusDir.getAbsolutePath() + "/tei",
            corpusDir.getAbsolutePath() + "/raw",
            trainingOutputPath,
            evalOutputPath,
            splitRatio);
    }

    /**
     * Add the selected features for the monograph model
     *
     * @param sourceTEIPathLabel path to corpus TEI files
     * @param sourceRawPathLabel path to corpus raw files
     * @param trainingOutputPath path where to store the temporary training data
     * @param evalOutputPath     path where to store the temporary evaluation data
     * @param splitRatio         ratio to consider for separating training and evaluation data, e.g. 0.8 for 80%
     * @return number of examples
     */
    public int addFeaturesDatacatSegmenter(String sourceTEIPathLabel,
                                    String sourceRawPathLabel,
                                    final File trainingOutputPath,
                                    final File evalOutputPath,
                                    double splitRatio) {
        int totalExamples = 0;
        try {
            System.out.println("sourceTEIPathLabel: " + sourceTEIPathLabel);
            System.out.println("sourceRawPathLabel: " + sourceRawPathLabel);
            System.out.println("trainingOutputPath: " + trainingOutputPath);
            System.out.println("evalOutputPath: " + evalOutputPath);

            // we need first to generate the labeled files from the TEI annotated files
            File input = new File(sourceTEIPathLabel);
            // we process all tei files in the output directory
            File[] refFiles = input.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".tei.xml") || name.endsWith(".tei");
                }
            });

            if (refFiles == null) {
                return 0;
            }

            System.out.println(refFiles.length + " tei files");

            // the file for writing the training data
            OutputStream os2 = null;
            Writer writer2 = null;
            if (trainingOutputPath != null) {
                os2 = new FileOutputStream(trainingOutputPath);
                writer2 = new OutputStreamWriter(os2, "UTF8");
            }

            // the file for writing the evaluation data
            OutputStream os3 = null;
            Writer writer3 = null;
            if (evalOutputPath != null) {
                os3 = new FileOutputStream(evalOutputPath);
                writer3 = new OutputStreamWriter(os3, "UTF8");
            }

            // get a factory for SAX parser
            SAXParserFactory spf = SAXParserFactory.newInstance();

            for (File tf : refFiles) {
                String name = tf.getName();
                LOGGER.info("Processing: " + name);

                TEIDatacatSegmenterSaxParser parser2 = new TEIDatacatSegmenterSaxParser();

                //get a new instance of parser
                SAXParser p = spf.newSAXParser();
                p.parse(tf, parser2);

                List<String> labeled = parser2.getLabeledResult();
                // we can now add the features
                // we open the featured file
                try {
                    File rawFile = new File(sourceRawPathLabel + File.separator +
                        name.replace(".tei.xml", ""));
                    if (!rawFile.exists()) {
                        LOGGER.error("The raw file does not exist: " + rawFile.getPath());
                        continue;
                    }

                    // read the raw CRF file
                    BufferedReader bis = new BufferedReader(
                        new InputStreamReader(new FileInputStream(
                            rawFile), "UTF8"));
                    int q = 0; // current position in the TEI labeled list
                    StringBuilder referenceText = new StringBuilder();

                    // read by lines the raw CRF file and add the tags
                    String line = bis.readLine();
                    String token1 = null, token2 = null;
                    int totFound= 0, lastPositionFound = 0, lastPosition = 0, totData = 0;
                    boolean found = false;
                    while ((line = bis.readLine()) != null) {
                        String lines[] = line.split(" ");
                        // every line of a CRF raw file contains 2 first tokens of each line of Pdf files
                        token1 = UnicodeUtil.normaliseTextAndRemoveSpaces(lines[0]);
                        token2 = UnicodeUtil.normaliseTextAndRemoveSpaces(lines[1]);
                        String currentLocalText = null, currentTag = null;
                        if (lastPosition >= labeled.size() - 1) {
                            lastPosition = lastPositionFound;
                        }

                        for (int i = lastPosition; i < labeled.size(); i++) {
                            currentLocalText = labeled.get(i);
                            currentTag = labeled.get(i);
                            if (currentLocalText.contains(token1) || currentLocalText.contains(token2)) { // if they are found
                                found = true;
                                totFound++;
                                lastPositionFound = i;
                                if (line.contains("BLOCKSTART") && line.contains("PAGESTART")){
                                    referenceText.append(line).append(" I-").append(currentTag);
                                } else {
                                    referenceText.append(line).append(" ").append(currentTag);
                                }
                            }
                            if (found || lastPosition >= labeled.size() - 1) {
                                found = false;
                                break;
                            } else {
                                lastPosition++;
                            }
                        }
                        totData++;
                        line = bis.readLine();
                    }
                    System.out.println("Total data found between CRF and TEI files " + totFound + " from total " + totData + " examples.");
                    bis.close();

                    if ((writer2 == null) && (writer3 != null))
                        writer3.write(referenceText.toString() + "\n");
                    if ((writer2 != null) && (writer3 == null))
                        writer2.write(referenceText.toString() + "\n");
                    else {
                        if (Math.random() <= splitRatio) {
                            writer2.write(referenceText.toString() + "\n");
                        } else if (writer3 != null) {
                            writer3.write(referenceText.toString() + "\n");
                        }
                    }

                } catch (Exception e) {
                    LOGGER.error("Fail to open or process raw file", e);
                }
            }

            if (writer2 != null) {
                writer2.close();
                os2.close();
            }

            if (writer3 != null) {
                writer3.close();
                os3.close();
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occured while running Grobid.", e);
        }
        return totalExamples;
    }

    /**
     * Command line execution.
     *
     * @param args Command line arguments.
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        GrobidProperties.getInstance();
        AbstractTrainer.runTraining(new DatacatSegmenterTrainer());
        System.out.println(AbstractTrainer.runEvaluation(new DatacatSegmenterTrainer()));
        System.exit(0);
    }
}