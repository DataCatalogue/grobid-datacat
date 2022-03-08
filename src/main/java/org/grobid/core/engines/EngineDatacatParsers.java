package org.grobid.core.engines;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * A list of parser for the grobid-medical-report sub-project
 */
public class EngineDatacatParsers extends EngineParsers {
    public static final Logger LOGGER = LoggerFactory.getLogger(EngineDatacatParsers.class);

    private DatacatSegmenterParser datacatSegmenterParser = null;

    public DatacatSegmenterParser getDatacatSegmenterParser() {
        if (datacatSegmenterParser == null) {
            synchronized (this) {
                if (datacatSegmenterParser == null) {
                    datacatSegmenterParser = new DatacatSegmenterParser();
                }
            }
        }
        return datacatSegmenterParser;
    }


    /**
     * Init all model, this will also load the model into memory
     */
    public void initAll() {
        datacatSegmenterParser = getDatacatSegmenterParser();
    }

    @Override
    public void close() throws IOException {
        LOGGER.debug("==> Closing all resources...");

        if (datacatSegmenterParser != null) {
            datacatSegmenterParser.close();
            datacatSegmenterParser = null;
            LOGGER.debug("CLOSING datacatSegmenterParser");
        }

        LOGGER.debug("==> All resources closed");

    }
}
