package com.oddlabs.translate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Converts .properties files to a single translations.csv file.
 * This is the reverse of CSVToProperties.
 *
 * Usage: java com.oddlabs.translate.PropertiesToCSV <i18n-directory>
 *
 * Properties files are read from <i18n-directory>/com/oddlabs/tt/...
 * The CSV file will be written to <i18n-directory>/translations.csv
 */
public class PropertiesToCSV {

    private static final String[] LOCALES = {"en", "da", "de", "es", "it"};
    private static final String[] LOCALE_SUFFIXES = {"", "_da", "_de", "_es", "_it"};

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java com.oddlabs.translate.PropertiesToCSV <i18n-directory>");
            System.exit(1);
        }

        Path i18nDir = Paths.get(args[0]);
        Path csvFile = i18nDir.resolve("translations.csv");

        System.out.println("Reading .properties files from " + i18nDir);

        // Structure: bundleName -> propertyKey -> locale -> value
        Map<String, Map<String, Map<String, String>>> translations = new TreeMap<>();

        // Find all .properties files
        Files.walk(i18nDir)
                .filter(p -> p.toString().endsWith(".properties"))
                .forEach(p -> {
                    try {
                        loadPropertiesFile(i18nDir, p, translations);
                    } catch (IOException e) {
                        System.err.println("Error reading " + p + ": " + e.getMessage());
                    }
                });

        int totalBundles = translations.size();
        int totalKeys = translations.values().stream()
                .mapToInt(Map::size)
                .sum();
        System.out.println("Found " + totalBundles + " bundles with " + totalKeys + " unique keys");

        System.out.println("Writing translations to " + csvFile);
        writeCSV(csvFile, translations);
        System.out.println("Done!");
    }

    private static void loadPropertiesFile(Path i18nDir, Path propsFile,
            Map<String, Map<String, Map<String, String>>> translations) throws IOException {

        // Get relative path and determine bundle name and locale
        Path relativePath = i18nDir.relativize(propsFile);
        String filename = propsFile.getFileName().toString();

        // Remove .properties extension
        String baseName = filename.substring(0, filename.length() - ".properties".length());

        // Determine locale from suffix
        String locale = "en";
        String bundleClassName = baseName;
        for (int i = 1; i < LOCALE_SUFFIXES.length; i++) {
            if (baseName.endsWith(LOCALE_SUFFIXES[i])) {
                locale = LOCALES[i];
                bundleClassName = baseName.substring(0, baseName.length() - LOCALE_SUFFIXES[i].length());
                break;
            }
        }

        // Build full bundle name (e.g., com.oddlabs.tt.form.GameMenu)
        Path parentDir = relativePath.getParent();
        String bundleName;
        if (parentDir != null) {
            bundleName = parentDir.toString().replace(File.separatorChar, '.') + "." + bundleClassName;
        } else {
            bundleName = bundleClassName;
        }

        // Parse properties file
        Map<String, String> properties = parsePropertiesFile(propsFile);

        // Add to translations map
        translations.computeIfAbsent(bundleName, k -> new TreeMap<>());
        Map<String, Map<String, String>> bundleTranslations = translations.get(bundleName);

        for (Map.Entry<String, String> entry : properties.entrySet()) {
            bundleTranslations.computeIfAbsent(entry.getKey(), k -> new HashMap<>());
            bundleTranslations.get(entry.getKey()).put(locale, entry.getValue());
        }
    }

    private static Map<String, String> parsePropertiesFile(Path file) throws IOException {
        Map<String, String> properties = new LinkedHashMap<>();

        String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);

        // Handle line continuations (lines ending with \)
        content = content.replaceAll("\\\\\\n\\s*", "");

        for (String line : content.split("\n")) {
            line = line.trim();

            // Skip empty lines and comments
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("!")) {
                continue;
            }

            // Find the separator (= or :)
            Matcher matcher = Pattern.compile("^([^=:]+?)\\s*[=:]\\s*(.*)").matcher(line);
            if (matcher.matches()) {
                String key = matcher.group(1).trim();
                String value = matcher.group(2);

                // Unescape common Java properties escapes
                value = value.replace("\\n", "\n");
                value = value.replace("\\t", "\t");
                value = value.replace("\\\\", "\\");

                properties.put(key, value);
            }
        }

        return properties;
    }

    private static void writeCSV(Path csvFile, Map<String, Map<String, Map<String, String>>> translations)
            throws IOException {

        try (BufferedWriter writer = Files.newBufferedWriter(csvFile, StandardCharsets.UTF_8)) {
            // Write header
            writer.write("key");
            for (String locale : LOCALES) {
                writer.write(",");
                writer.write(locale);
            }
            writer.newLine();

            // Write data rows
            for (Map.Entry<String, Map<String, Map<String, String>>> bundleEntry : translations.entrySet()) {
                String bundleName = bundleEntry.getKey();
                Map<String, Map<String, String>> properties = bundleEntry.getValue();

                for (Map.Entry<String, Map<String, String>> propEntry : properties.entrySet()) {
                    String propKey = propEntry.getKey();
                    Map<String, String> localeValues = propEntry.getValue();

                    String fullKey = bundleName + "." + propKey;
                    writer.write(escapeCSV(fullKey));

                    for (String locale : LOCALES) {
                        writer.write(",");
                        String value = localeValues.get(locale);
                        if (value != null) {
                            writer.write(escapeCSV(value));
                        }
                    }
                    writer.newLine();
                }
            }
        }
    }

    private static String escapeCSV(String value) {
        if (value == null) {
            return "";
        }

        // If value contains comma, quote, or newline, wrap in quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            // Escape quotes by doubling them
            value = value.replace("\"", "\"\"");
            return "\"" + value + "\"";
        }

        return value;
    }
}
