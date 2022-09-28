package org.grobid.core.engines;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * A list of parser for the grobid-datacat sub-project
 */
public class EngineDatacatParsers extends EngineParsers {
    public static final Logger LOGGER = LoggerFactory.getLogger(EngineDatacatParsers.class);

    private DatacatSegmenterParser datacatSegmenterParser = null;
    private DatacatBodySegmentationParser datacatBodySegmentationParser = null;

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

    public DatacatBodySegmentationParser getDatacatBodySegmentationParser() {
        if (datacatBodySegmentationParser == null) {
            synchronized (this) {
                if (datacatBodySegmentationParser == null) {
                    datacatBodySegmentationParser = new DatacatBodySegmentationParser(this);
                }
            }
        }
        return datacatBodySegmentationParser;
    }


    /**
     * Init all model, this will also load the model into memory
     */
    public void initAll() {
        datacatSegmenterParser = getDatacatSegmenterParser();
        datacatBodySegmentationParser = getDatacatBodySegmentationParser();
    }

    @Override
    public void close() throws IOException {
        LOGGER.debug("==> Closing all resources...");

        if (datacatSegmenterParser != null) {
            datacatSegmenterParser.close();
            datacatSegmenterParser = null;
            LOGGER.debug("CLOSING datacatSegmenterParser");
        }

        if (datacatBodySegmentationParser != null) {
            datacatBodySegmentationParser.close();
            datacatBodySegmentationParser = null;
            LOGGER.debug("CLOSING datacatBodySegmentationParser");
        }

        LOGGER.debug("==> All resources closed");

    }
}
