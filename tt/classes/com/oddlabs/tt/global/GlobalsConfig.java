package com.oddlabs.tt.global;

import java.util.Properties;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;


public class GlobalsConfig {
    private static final Properties props = new Properties();

    static {
        try (InputStream input = new FileInputStream("./config.properties")) {
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Config file missing", e);
        }
    }

    public static String get(String key) {
        return props.getProperty(key);
    }
}
