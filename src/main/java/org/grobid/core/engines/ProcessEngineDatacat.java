package org.grobid.core.engines;

import org.grobid.core.factory.GrobidDatacatFactory;
import org.grobid.core.main.batch.GrobidMainArgs;
import org.grobid.core.utilities.IOUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Perform the batch processing for the different engine methods.
 *
 * @author Damien, Patrice
 */

public class ProcessEngineDatacat extends ProcessEngine {

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
     * Generate training data for all models
     *
     * @param pGbdArgs The parameters.
     */
    public void createTraining(final GrobidMainArgs pGbdArgs) {
        inferPdfInputPath(pGbdArgs);
        inferOutputPath(pGbdArgs);
        int result = getEngine().batchCreateTraining(pGbdArgs.getPath2Input(), pGbdArgs.getPath2Output(), -1);
        LOGGER.info(result + " files processed.");
    }

    /**
     * Generate blank training data from provided directory of PDF documents, i.e. where TEI files are text only
     * without tags. This can be used to start from scratch any new model.
     *
     * @param pGbdArgs The parameters.
     * @throws Exception
     */
    public void createTrainingBlank(final GrobidMainArgs pGbdArgs) throws Exception {
        inferPdfInputPath(pGbdArgs);
        inferOutputPath(pGbdArgs);
        int result = getEngine().batchCreateTrainingBlank(pGbdArgs.getPath2Input(), pGbdArgs.getPath2Output(), -1);
        LOGGER.info(result + " files processed.");
    }

}
