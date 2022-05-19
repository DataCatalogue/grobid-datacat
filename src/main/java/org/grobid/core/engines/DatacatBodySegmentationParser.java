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

public class DatacatBodySegmentationParser {

}
