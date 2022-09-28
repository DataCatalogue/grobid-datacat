package org.grobid.core.utilities;

import org.grobid.core.utilities.GrobidConfig.ModelParameters;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public class DatacatConfiguration {
    private static Logger LOGGER = LoggerFactory.getLogger(DatacatConfiguration.class);

    private String grobidHome;

    private String dataPath;

    public DatacatConfiguration getInstance() {
        return getInstance(null);
    }

    public static DatacatConfiguration getInstance(String projectRootPath) {

        DatacatConfiguration DatacatConfiguration = null;
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            if (projectRootPath == null)
                DatacatConfiguration  = mapper.readValue(new File("resources/config/grobid-datacat.yaml"), DatacatConfiguration.class);
            else
                DatacatConfiguration  = mapper.readValue(new File(projectRootPath + "/resources/config/grobid-datacat.yaml"), DatacatConfiguration.class);
        } catch(Exception e) {
            LOGGER.error("The config file does not appear valid, see resources/config/grobid-datacat.yaml", e);
        }
        return DatacatConfiguration ;
    }

    // sequence labeling models
    public List<ModelParameters> models;

    public String getGrobidHome() {
        return this.grobidHome;
    }

    public void setGrobidHome(String grobidHome) {
        this.grobidHome = grobidHome;
    }

    public List<ModelParameters> getModels() {
        return this.models;
    }

    public void getModels(List<ModelParameters> models) {
        this.models = models;
    }


    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

}