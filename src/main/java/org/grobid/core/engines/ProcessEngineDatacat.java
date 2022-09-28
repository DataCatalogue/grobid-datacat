package org.grobid.core.engines;

import org.apache.commons.lang3.StringUtils;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.factory.GrobidDatacatFactory;
import org.grobid.core.main.batch.GrobidMainArgs;
import org.grobid.core.main.batch.GrobidDatacatMainArgs;
import org.grobid.core.utilities.IOUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Perform the batch processing for the different engine methods.
 *
 * @author Damien, Patrice
 */

public class ProcessEngineDatacat implements Closeable {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessEngineDatacat.class);

    /**
     * The engine.
     */
    private static EngineDatacat engine;

    /**
     * @return the engine instance.
     */

    protected EngineDatacat getEngine() {
        if (engine == null) {
            engine = GrobidDatacatFactory.getInstance().createEngine();
        }
        return engine;
    }

    /**
     * Close engine resources.
     */
    @Override
    public void close() throws IOException {
        if (engine != null) {
            engine.close();
        }
        System.exit(0);
    }

    /**
     * Generate training data for the monograph model from provided directory of PDF documents.
     *
     * @param pGbdArgs The parameters.
     * @throws Exception
     */
    public void createTrainingSegmenter(final GrobidDatacatMainArgs pGbdArgs) throws Exception {
        inferPdfInputPath(pGbdArgs);
        inferOutputPath(pGbdArgs);
        int result = getEngine().batchCreateTrainingSegmenter(pGbdArgs.getPath2Input(), pGbdArgs.getPath2Output(), -1);
        LOGGER.info(result + " files processed.");
    }

    public void createTrainingBody(final GrobidDatacatMainArgs pGbdArgs) throws Exception {
        inferPdfInputPath(pGbdArgs);
        inferOutputPath(pGbdArgs);
        int result = getEngine().batchCreateTrainingBody(pGbdArgs.getPath2Input(), pGbdArgs.getPath2Output(), -1);
        LOGGER.info(result + " files processed.");
    }

    /**
     * Generate blank training data from provided directory of PDF documents, i.e. where TEI files are text only
     * without tags. This can be used to start from scratch any new model.
     *
     * @param pGbdArgs The parameters.
     * @throws Exception
     */
    public void createTrainingBlank(final GrobidDatacatMainArgs pGbdArgs) throws Exception {
        inferPdfInputPath(pGbdArgs);
        inferOutputPath(pGbdArgs);
        int result = getEngine().batchCreateTrainingBlank(pGbdArgs.getPath2Input(), pGbdArgs.getPath2Output(), -1);
        LOGGER.info(result + " files processed.");
    }

    public void createTrainingBlankBody(final GrobidDatacatMainArgs pGbdArgs) throws Exception {
        inferPdfInputPath(pGbdArgs);
        inferOutputPath(pGbdArgs);
        int result = getEngine().batchCreateTrainingBlankBody(pGbdArgs.getPath2Input(), pGbdArgs.getPath2Output(), -1);
        LOGGER.info(result + " files processed.");
    }

    /**
     * Generate blank training data from provided directory of PDF documents, i.e. where TEI files are text only
     * without tags. This can be used to start from scratch any new model.
     *
     * @param pGbdArgs The parameters.
     * @throws Exception
     */
    public void extractTxtFromPDF(final GrobidDatacatMainArgs pGbdArgs) throws Exception {
        inferPdfInputPath(pGbdArgs);
        inferOutputPath(pGbdArgs);
        int result = getEngine().batchExtractTxtFromPDF(pGbdArgs.getPath2Input(), pGbdArgs.getPath2Output(), -1);
        LOGGER.info(result + " files processed.");
    }

    /**
     * List the engine methods that can be called.
     *
     * @return List<String> containing the list of the methods.
     */
    public final static List<String> getUsableMethods() {
        final Class<?> pClass = new ProcessEngineDatacat().getClass();
        final List<String> availableMethods = new ArrayList<String>();
        for (final Method method : pClass.getMethods()) {
            if (isUsableMethod(method.getName())) {
                availableMethods.add(method.getName());
            }
        }
        return availableMethods;
    }

    /**
     * Check if the method is usable.
     *
     * @param pMethod method name.
     * @return if it is usable
     */
    protected final static boolean isUsableMethod(final String pMethod) {
        boolean isUsable = StringUtils.equals("wait", pMethod);
        isUsable |= StringUtils.equals("equals", pMethod);
        isUsable |= StringUtils.equals("toString", pMethod);
        isUsable |= StringUtils.equals("hashCode", pMethod);
        isUsable |= StringUtils.equals("getClass", pMethod);
        isUsable |= StringUtils.equals("notify", pMethod);
        isUsable |= StringUtils.equals("notifyAll", pMethod);
        isUsable |= StringUtils.equals("isUsableMethod", pMethod);
        isUsable |= StringUtils.equals("getUsableMethods", pMethod);
        isUsable |= StringUtils.equals("inferPdfInputPath", pMethod);
        isUsable |= StringUtils.equals("inferOutputPath", pMethod);
        isUsable |= StringUtils.equals("close", pMethod);
        return !isUsable;
    }

    /**
     * Infer the input path for pdfs if not given in arguments.
     *
     * @param pGbdArgs The GrobidDatacatMainArgs.
     */
    protected final static void inferPdfInputPath(final GrobidDatacatMainArgs pGbdArgs) {
        String tmpFilePath;
        if (pGbdArgs.getPath2Input() == null) {
            tmpFilePath = new File(".").getAbsolutePath();
            LOGGER.info("No path set for the input directory. Using: " + tmpFilePath);
            pGbdArgs.setPath2Input(tmpFilePath);
        }
    }

    /**
     * Infer the output path if not given in arguments.
     *
     * @param pGbdArgs The GrobidDatacatReportMainArgs.
     */
    protected final static void inferOutputPath(final GrobidDatacatMainArgs pGbdArgs) {
        String tmpFilePath;
        if (pGbdArgs.getPath2Output() == null) {
            tmpFilePath = new File(".").getAbsolutePath();
            LOGGER.info("No path set for the output directory. Using: " + tmpFilePath);
            pGbdArgs.setPath2Output(tmpFilePath);
        }
    }

}
