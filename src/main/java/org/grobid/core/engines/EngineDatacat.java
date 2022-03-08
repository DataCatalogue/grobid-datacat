
package org.grobid.core.engines;

import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.document.Document;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.exceptions.GrobidException;
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
}