package com.oddlabs.tt.global;

import java.util.Properties;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;


public class GlobalsConfig {
    private static final Properties props = new Properties();

    static {
        // For some reason mac cannot find the config.properties using the original path of './config.properties'
        // so for mac we need to use the user's home directory or figure out a better way to locate the file.
        String configPath = System.getProperty("user.home") + "/config.properties";
        System.out.println("GlobalsConfig looking for config.properties at: " + new java.io.File(configPath).getAbsolutePath());
        try (InputStream input = new FileInputStream(configPath)) {
            props.load(input);
        } catch (IOException e) {
            System.out.println("Config file missing: " + e);
        }
    }

    public static String get(String key) {
        return props.getProperty(key);
    }
    
    public static String getDomainName() {
        String domainName = get("DOMAIN_NAME");
        return domainName == null ? "domain.local" : domainName;
    }
}
