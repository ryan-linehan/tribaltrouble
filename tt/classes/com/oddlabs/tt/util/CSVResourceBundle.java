package com.oddlabs.tt.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * A ResourceBundle implementation that loads translations from a single CSV file.
 * The CSV format is: key,en,da,de,es,it
 * Keys are fully qualified: com.oddlabs.tt.form.GameMenu.game_caption
 *
 * This class provides a drop-in replacement for standard Java ResourceBundle
 * with the advantage of having all translations in a single file for easier management.
 */
public final class CSVResourceBundle extends ResourceBundle {

    private static final String CSV_PATH = "/translations.csv";
    private static final String[] LOCALE_COLUMNS = {"en", "da", "de", "es", "it"};

    // Cache for loaded translations: locale -> (fullKey -> value)
    private static volatile Map<String, Map<String, String>> translationCache = null;
    private static final Object cacheLock = new Object();

    private final String bundleName;
    private final String localeCode;
    private final Map<String, String> bundleTranslations;

    /**
     * Create a CSVResourceBundle for a specific bundle and locale.
     */
    public CSVResourceBundle(String bundleName, Locale locale) {
        this.bundleName = bundleName;
        this.localeCode = getLocaleCode(locale);
        this.bundleTranslations = loadBundleTranslations();
    }

    private String getLocaleCode(Locale locale) {
        if (locale == null || locale.getLanguage().isEmpty()) {
            return "en";
        }
        String lang = locale.getLanguage();
        // Check if this locale is supported
        for (String supported : LOCALE_COLUMNS) {
            if (supported.equals(lang)) {
                return lang;
            }
        }
        // Fall back to English for unsupported locales
        return "en";
    }

    private Map<String, String> loadBundleTranslations() {
        ensureCacheLoaded();
        Map<String, String> localeTranslations = translationCache.get(localeCode);
        if (localeTranslations == null) {
            localeTranslations = translationCache.get("en");
        }
        if (localeTranslations == null) {
            return Collections.emptyMap();
        }

        // Extract only the keys belonging to this bundle
        Map<String, String> result = new HashMap<>();
        String prefix = bundleName + ".";
        for (Map.Entry<String, String> entry : localeTranslations.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                String shortKey = entry.getKey().substring(prefix.length());
                result.put(shortKey, entry.getValue());
            }
        }
        return result;
    }

    private static void ensureCacheLoaded() {
        if (translationCache != null) {
            return;
        }
        synchronized (cacheLock) {
            if (translationCache != null) {
                return;
            }
            translationCache = loadAllTranslations();
        }
    }

    private static Map<String, Map<String, String>> loadAllTranslations() {
        Map<String, Map<String, String>> result = new HashMap<>();
        for (String locale : LOCALE_COLUMNS) {
            result.put(locale, new HashMap<>());
        }

        try (InputStream is = CSVResourceBundle.class.getResourceAsStream(CSV_PATH)) {
            if (is == null) {
                System.err.println("CSVResourceBundle: Could not find " + CSV_PATH);
                return result;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

            // Read header line
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return result;
            }

            String[] headers = parseCSVLine(headerLine);
            int[] localeIndices = new int[LOCALE_COLUMNS.length];
            for (int i = 0; i < LOCALE_COLUMNS.length; i++) {
                localeIndices[i] = -1;
                for (int j = 0; j < headers.length; j++) {
                    if (LOCALE_COLUMNS[i].equals(headers[j])) {
                        localeIndices[i] = j;
                        break;
                    }
                }
            }

            // Read data lines
            String line;
            StringBuilder multiLineBuffer = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                // Handle multi-line values (CSV fields can contain newlines if quoted)
                multiLineBuffer.append(line);
                String currentLine = multiLineBuffer.toString();

                // Check if we have a complete line (even number of quotes)
                if (isCompleteLine(currentLine)) {
                    String[] values = parseCSVLine(currentLine);
                    if (values.length > 0) {
                        String key = values[0];
                        for (int i = 0; i < LOCALE_COLUMNS.length; i++) {
                            if (localeIndices[i] >= 0 && localeIndices[i] < values.length) {
                                String value = values[localeIndices[i]];
                                if (value != null && !value.isEmpty()) {
                                    result.get(LOCALE_COLUMNS[i]).put(key, value);
                                }
                            }
                        }
                    }
                    multiLineBuffer.setLength(0);
                } else {
                    multiLineBuffer.append("\n");
                }
            }

        } catch (IOException e) {
            System.err.println("CSVResourceBundle: Error loading " + CSV_PATH + ": " + e.getMessage());
        }

        return result;
    }

    private static boolean isCompleteLine(String line) {
        int quoteCount = 0;
        boolean escaped = false;
        for (char c : line.toCharArray()) {
            if (c == '"' && !escaped) {
                quoteCount++;
            }
            escaped = (c == '\\');
        }
        return quoteCount % 2 == 0;
    }

    private static String[] parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean prevWasQuote = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && prevWasQuote) {
                    // Escaped quote inside quoted field
                    current.append('"');
                    prevWasQuote = false;
                } else if (inQuotes) {
                    prevWasQuote = true;
                } else {
                    inQuotes = true;
                    prevWasQuote = false;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
                prevWasQuote = false;
            } else {
                if (prevWasQuote) {
                    // End of quoted field
                    inQuotes = false;
                }
                current.append(c);
                prevWasQuote = false;
            }
        }

        // Handle the last field
        if (prevWasQuote) {
            inQuotes = false;
        }
        result.add(current.toString());

        return result.toArray(new String[0]);
    }

    @Override
    protected Object handleGetObject(String key) {
        String value = bundleTranslations.get(key);
        if (value != null) {
            return value;
        }
        // Fall back to English if translation not found
        if (!"en".equals(localeCode)) {
            ensureCacheLoaded();
            Map<String, String> enTranslations = translationCache.get("en");
            if (enTranslations != null) {
                String fullKey = bundleName + "." + key;
                return enTranslations.get(fullKey);
            }
        }
        return null;
    }

    @Override
    public Enumeration<String> getKeys() {
        return Collections.enumeration(bundleTranslations.keySet());
    }

    /**
     * Custom ResourceBundle.Control that creates CSVResourceBundle instances.
     */
    public static class Control extends ResourceBundle.Control {
        public static final Control INSTANCE = new Control();

        private Control() {}

        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format,
                                         ClassLoader loader, boolean reload) {
            return new CSVResourceBundle(baseName, locale);
        }

        @Override
        public List<String> getFormats(String baseName) {
            return Collections.singletonList("csv");
        }

        @Override
        public boolean needsReload(String baseName, Locale locale, String format,
                                    ClassLoader loader, ResourceBundle bundle, long loadTime) {
            return false;
        }
    }

    /**
     * Convenience method to get a ResourceBundle using CSV backend.
     * Use this instead of ResourceBundle.getBundle() for CSV-based translations.
     */
    public static ResourceBundle createBundle(String baseName) {
        return ResourceBundle.getBundle(baseName, Locale.getDefault(), Control.INSTANCE);
    }

    /**
     * Convenience method to get a ResourceBundle using CSV backend with specific locale.
     */
    public static ResourceBundle createBundle(String baseName, Locale locale) {
        return ResourceBundle.getBundle(baseName, locale, Control.INSTANCE);
    }
}
