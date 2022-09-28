package org.grobid.core.data;

import org.grobid.core.GrobidModels;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.TextUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class EntryItem {

    protected static final Logger LOGGER = LoggerFactory.getLogger(EntryItem.class);

    private List<BoundingBox> coordinates = null;

    // map of labels (e.g. <affiliation> or <org>) to LayoutToken
    private Map<String, List<LayoutToken>> labeledTokens;

    private List<LayoutToken> entry = new ArrayList<>();

}
