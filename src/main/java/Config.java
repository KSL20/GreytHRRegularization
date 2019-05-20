package com.regularization;

import java.io.FileInputStream;
import java.util.Properties;

class Config {
    private Properties prop = new Properties();

    public String get(String property) {
        return prop.getProperty(property);
    }

    public Boolean init() {
        Boolean configLoaded = false;
        try {
            FileInputStream ip = new FileInputStream("src/main/resources/Config.properties");
            prop.load(ip);
            configLoaded = true;
        } catch (Exception exception) {
            configLoaded  = false;
        }
        return configLoaded;
    }
}