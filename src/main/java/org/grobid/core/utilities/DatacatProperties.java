package org.grobid.core.utilities;

import java.io.IOException;
import java.io.InputStream;

public class DatacatProperties {

    public static String get(String key) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream stream = classLoader.getResourceAsStream("grobid-datacat.properties");

        java.util.Properties properties = new java.util.Properties();
        try {
            properties.load(stream);
        } catch (IOException e1) {
            return null;
        }

        return properties.getProperty(key);
    }

}
