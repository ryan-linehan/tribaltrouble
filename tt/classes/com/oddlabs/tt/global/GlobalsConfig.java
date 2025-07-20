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

    public static boolean isFullscreen() {
        String fullscreen = get("FULLSCREEN");
        return fullscreen != null && Boolean.parseBoolean(fullscreen);
    }

    public static int getWindowWidth() {
        String width = get("WINDOW_WIDTH");
        return width != null ? Integer.parseInt(width) : 1920; // Default width
    }

    public static int getWindowHeight() {
        String height = get("WINDOW_HEIGHT");
        return height != null ? Integer.parseInt(height) : 1080; // Default height
    }

    public static int getRefreshRate() {
        String refreshRate = get("REFRESH_RATE");
        return refreshRate != null ? Integer.parseInt(refreshRate) : 60; // Default refresh rate
    }
}
