
package org.grobid.core.engines;

import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.document.Document;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.utilities.counters.CntManager;
import org.grobid.core.utilities.counters.impl.CntManagerFactory;
import org.grobid.core.utilities.crossref.CrossrefClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;

/**
 * A class for managing the extraction of medical information from PDF documents or raw text.
 * This class extends the Engine class of Grobid (@author Patrice Lopez)
 */
public class EngineDatacat extends Engine {
    private static final Logger LOGGER = LoggerFactory.getLogger(EngineDatacat.class);

    private final EngineDatacatParsers parsers = new EngineDatacatParsers();

    private static CntManager cntManager = CntManagerFactory.getCntManager();

    // The list of accepted languages
    // the languages are encoded in ISO 3166
    // if null, all languages are accepted.
    private List<String> acceptedLanguages = null;

    /**
     * Constructor for the grobid-datacat engine instance.
     *
     * @param loadModels
     */
    public EngineDatacat(boolean loadModels) {
        super(loadModels);
    }

    /**
     * Create training data for all models based on the application of
     * the current full text model on a new PDF
     *
     * @param inputFile    : the path of the PDF file to be processed
     * @param pathOutput   : the path where to put the CRF feature file and  the annotated TEI representation (the
     *                      file to be corrected for gold-level training data)
     * @param id           : an optional ID to be used in the TEI file, -1 if not used
     */
    public void createTrainingSegmenter(File inputFile, String pathOutput,  int id) {
        parsers.getDatacatSegmenterParser().createTrainingDatacatSegmenter(inputFile, pathOutput, id);
    }

    /**
     * Process all the PDF in a given directory with a segmentation process and
     * produce the corresponding training data format files for manual
     * correction. The goal of this method is to help to produce additional
     * traning data based on an existing model.
     *
     * @param directoryPath - the path to the directory containing PDF to be processed.
     * @param resultPath    - the path to the directory where the results as XML files
     *                      shall be written.
     * @param ind           - identifier integer to be included in the resulting files to
     *                      identify the training case. This is optional: no identifier
     *                      will be included if ind = -1
     * @return the number of processed files.
     */
    public int batchCreateTrainingSegmenter(String directoryPath, String resultPath, int ind) {
        try {
            File path = new File(directoryPath);
            // we process all pdf files in the directory
            File[] refFiles = path.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    System.out.println(name);
                    return name.endsWith(".pdf") || name.endsWith(".PDF");
                }
            });

            if (refFiles == null)
                return 0;

            System.out.println(refFiles.length + " files to be processed.");

            int n = 0;
            if (ind == -1) {
                // for undefined identifier (value at -1), we initialize it to 0
                n = 1;
            }
            for (final File pdfFile : refFiles) {
                try {
                    createTrainingSegmenter(pdfFile, resultPath, ind + n);
                } catch (final Exception exp) {
                    LOGGER.error("An error occurred while processing the following pdf: "
                        + pdfFile.getPath(), exp);
                }
                if (ind != -1)
                    n++;
            }

            return refFiles.length;
        } catch (final Exception exp) {
            throw new GrobidException("An exception occurred while running Grobid batch.", exp);
        }
    }

    public void createTrainingBlank(File inputFile, String pathRaw, String pathTEI, int id) {
        this.parsers.getDatacatSegmenterParser().createBlankTrainingFromPDFWithoutTEI(inputFile, pathRaw, pathTEI, id);
    }

    public int batchCreateTrainingBlankBlockParsing(String directoryPath, String resultPath, int ind) {
        try {
            File path = new File(directoryPath);
            File[] refFiles = path.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    System.out.println(name);
                    return name.endsWith(".pdf") || name.endsWith(".PDF");
                }
            });
            if (refFiles == null) {
                return 0;
            } else {
                System.out.println(refFiles.length + " files to be processed.");
                int n = 0;
                if (ind == -1) {
                    n = 1;
                }

                File[] var7 = refFiles;
                int var8 = refFiles.length;

                for(int var9 = 0; var9 < var8; ++var9) {
                    File pdfFile = var7[var9];

                    try {
                        this.createTrainingBlank(pdfFile, resultPath, resultPath, ind + n);
                    } catch (Exception var12) {
                        LOGGER.error("An error occured while processing the following pdf: " + pdfFile.getPath(), var12);
                    }

                    if (ind != -1) {
                        ++n;
                    }
                }

                return refFiles.length;
            }
        } catch (Exception var13) {
            throw new GrobidException("An exception occured while running Grobid batch.", var13);
        }
    }

    @Override
    public synchronized void close() throws IOException {
        CrossrefClient.getInstance().close();
        parsers.close();
    }

    public static void setCntManager(CntManager cntManager) {
        EngineDatacat.cntManager = cntManager;
    }

    public static CntManager getCntManager() {
        return cntManager;
    }

    public EngineDatacatParsers getParsers() {
        return parsers;
    }
}