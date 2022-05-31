package org.grobid.core.engines.label;

import org.grobid.core.GrobidDatacatModels;
import org.grobid.core.GrobidModels;

public class DatacatLabels extends TaggingLabels {

DatacatLabels() {
    super();
}

// Grobid-datacat segmentation labels
    public final static String FRONT_MATTER_LABEL = "<front>";
    public final static String BODY_LABEL = "<body>";
    public final static String BACK_MATTER_LABEL = "<back>";
    public final static String ANNEX_LABEL = "<annex>";

    public static final TaggingLabel FRONT_MATTER = new TaggingLabelImpl(GrobidModels.DATACAT_SEGMENTER, FRONT_MATTER_LABEL);
    public static final TaggingLabel BODY = new TaggingLabelImpl(GrobidModels.DATACAT_SEGMENTER, BODY_LABEL);
    public static final TaggingLabel BACK_MATTER = new TaggingLabelImpl(GrobidModels.DATACAT_SEGMENTER, BACK_MATTER_LABEL);
    public static final TaggingLabel ANNEX = new TaggingLabelImpl(GrobidModels.DATACAT_SEGMENTER, ANNEX_LABEL);

}
