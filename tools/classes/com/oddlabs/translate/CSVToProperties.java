package com.oddlabs.translate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Converts translations.csv to individual .properties files.
 *
 * Usage: java com.oddlabs.translate.CSVToProperties <i18n-directory>
 *
 * The CSV file should be at <i18n-directory>/translations.csv
 * Properties files will be written to <i18n-directory>/com/oddlabs/tt/...
 */
public class CSVToProperties {

    // Locale mappings: CSV column -> properties file suffix
    private static final Map<String, String> LOCALE_SUFFIXES = new LinkedHashMap<>();
    static {
        LOCALE_SUFFIXES.put("en", "");      // English is the default (no suffix)
        LOCALE_SUFFIXES.put("da", "_da");
        LOCALE_SUFFIXES.put("de", "_de");
        LOCALE_SUFFIXES.put("es", "_es");
        LOCALE_SUFFIXES.put("it", "_it");
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java com.oddlabs.translate.CSVToProperties <i18n-directory>");
            System.exit(1);
        }

        Path i18nDir = Paths.get(args[0]);
        Path csvFile = i18nDir.resolve("translations.csv");

        if (!Files.exists(csvFile)) {
            System.err.println("Error: CSV file not found: " + csvFile);
            System.exit(1);
        }

        System.out.println("Reading translations from " + csvFile);

        // Structure: bundlePath -> propertyKey -> locale -> value
        Map<String, Map<String, Map<String, String>>> translations = loadCSV(csvFile);

        int totalBundles = translations.size();
        int totalKeys = translations.values().stream()
                .mapToInt(Map::size)
                .sum();
        System.out.println("Found " + totalBundles + " bundles with " + totalKeys + " unique keys");

        System.out.println("Writing .properties files to " + i18nDir);
        int filesWritten = generatePropertiesFiles(i18nDir, translations);
        System.out.println("Written " + filesWritten + " .properties files");
    }

    private static Map<String, Map<String, Map<String, String>>> loadCSV(Path csvFile) throws IOException {
        Map<String, Map<String, Map<String, String>>> translations = new TreeMap<>();

        try (BufferedReader reader = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8)) {
            // Read header line
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return translations;
            }

            String[] headers = parseCSVLine(headerLine);
            Map<String, Integer> localeIndices = new HashMap<>();
            for (String locale : LOCALE_SUFFIXES.keySet()) {
                for (int i = 0; i < headers.length; i++) {
                    if (locale.equals(headers[i])) {
                        localeIndices.put(locale, i);
                        break;
                    }
                }
            }

            // Read data lines
            StringBuilder multiLineBuffer = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                multiLineBuffer.append(line);
                String currentLine = multiLineBuffer.toString();

                // Check if we have a complete line (balanced quotes)
                if (isCompleteLine(currentLine)) {
                    String[] values = parseCSVLine(currentLine);
                    if (values.length > 0 && !values[0].isEmpty()) {
                        String fullKey = values[0];
                        String[] keyParts = parseKey(fullKey);
                        if (keyParts != null) {
                            String bundlePath = keyParts[0];
                            String propertyKey = keyParts[1];

                            translations.computeIfAbsent(bundlePath, k -> new TreeMap<>());
                            Map<String, Map<String, String>> bundleProps = translations.get(bundlePath);
                            bundleProps.computeIfAbsent(propertyKey, k -> new HashMap<>());
                            Map<String, String> propLocales = bundleProps.get(propertyKey);

                            for (String locale : LOCALE_SUFFIXES.keySet()) {
                                Integer idx = localeIndices.get(locale);
                                if (idx != null && idx < values.length) {
                                    String value = values[idx];
                                    if (value != null && !value.isEmpty()) {
                                        propLocales.put(locale, value);
                                    }
                                }
                            }
                        }
                    }
                    multiLineBuffer.setLength(0);
                } else {
                    multiLineBuffer.append("\n");
                }
            }
        }

        return translations;
    }

    private static String[] parseKey(String fullKey) {
        int lastDot = fullKey.lastIndexOf('.');
        if (lastDot <= 0) {
            return null;
        }
        String bundlePath = fullKey.substring(0, lastDot).replace('.', '/');
        String propertyKey = fullKey.substring(lastDot + 1);
        return new String[] { bundlePath, propertyKey };
    }

    private static boolean isCompleteLine(String line) {
        int quoteCount = 0;
        for (char c : line.toCharArray()) {
            if (c == '"') {
                quoteCount++;
            }
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
                    inQuotes = false;
                }
                current.append(c);
                prevWasQuote = false;
            }
        }

        result.add(current.toString());
        return result.toArray(new String[0]);
    }

    private static int generatePropertiesFiles(Path i18nDir,
            Map<String, Map<String, Map<String, String>>> translations) throws IOException {
        int filesWritten = 0;

        for (Map.Entry<String, Map<String, Map<String, String>>> bundleEntry : translations.entrySet()) {
            String bundlePath = bundleEntry.getKey();
            Map<String, Map<String, String>> properties = bundleEntry.getValue();

            for (Map.Entry<String, String> localeEntry : LOCALE_SUFFIXES.entrySet()) {
                String locale = localeEntry.getKey();
                String suffix = localeEntry.getValue();

                // Collect all properties for this locale
                Map<String, String> localeProperties = new TreeMap<>();
                for (Map.Entry<String, Map<String, String>> propEntry : properties.entrySet()) {
                    String propKey = propEntry.getKey();
                    Map<String, String> localeValues = propEntry.getValue();
                    String value = localeValues.get(locale);
                    if (value != null && !value.isEmpty()) {
                        localeProperties.put(propKey, value);
                    }
                }

                if (localeProperties.isEmpty()) {
                    continue;
                }

                // Determine file path
                String[] pathParts = bundlePath.split("/");
                String className = pathParts[pathParts.length - 1];
                String dirPath = String.join("/", Arrays.copyOf(pathParts, pathParts.length - 1));
                String filename = className + suffix + ".properties";
                Path filePath = i18nDir.resolve(dirPath).resolve(filename);

                writePropertiesFile(filePath, localeProperties);
                filesWritten++;
            }
        }

        return filesWritten;
    }

    private static void writePropertiesFile(Path filePath, Map<String, String> properties) throws IOException {
        Files.createDirectories(filePath.getParent());

        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String key = entry.getKey();
                String value = escapePropertiesValue(entry.getValue());
                writer.write(key + "=" + value);
                writer.newLine();
            }
        }
    }

    private static String escapePropertiesValue(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        // Handle newlines - convert to escaped form with continuation
        if (value.contains("\n")) {
            String[] lines = value.split("\n", -1);
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < lines.length; i++) {
                result.append(lines[i]);
                if (i < lines.length - 1) {
                    result.append("\\n\\\n\t\t\t  ");
                }
            }
            return result.toString();
        }

        return value;
    }
}
