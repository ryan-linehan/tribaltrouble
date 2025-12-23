# Tribal Trouble Translation System

This document describes the translation/internationalization (i18n) system for Tribal Trouble.

## Overview

The game supports two translation backends:
1. **Properties files** (legacy, default): Standard Java ResourceBundle with multiple `.properties` files
2. **Single CSV file** (new): All translations in a single `translations.csv` file

## Supported Languages

- English (en) - default
- Danish (da)
- German (de)
- Spanish (es)
- Italian (it)

## File Locations

### Properties Files (Legacy)
Located in `tt/i18n/com/oddlabs/tt/` with the directory structure mirroring the Java package structure:
```
tt/i18n/
└── com/
    └── oddlabs/
        └── tt/
            ├── Main.properties
            ├── Main_da.properties
            ├── Main_de.properties
            ├── Main_es.properties
            ├── Main_it.properties
            ├── form/
            │   ├── GameMenu.properties
            │   ├── GameMenu_da.properties
            │   └── ...
            └── ...
```

### CSV File (New)
Single file: `tt/i18n/translations.csv`

## CSV Format

The CSV file has the following format:
```csv
key,en,da,de,es,it
com.oddlabs.tt.form.GameMenu.game_caption,Game,Spil,Spiel,Juego,Gioco
com.oddlabs.tt.form.GameMenu.start,Start,Start,Start,Iniciar,Avvia
...
```

- **key**: Fully qualified key in format `package.ClassName.property_key`
- **en, da, de, es, it**: Translation values for each language
- Multi-line values are supported using standard CSV quoting

## Enabling CSV Translations

CSV translations are disabled by default for backward compatibility. To enable:

### Via System Property
```
java -Dtribaltrouble.csv.translations=true -jar tt-code.jar
```

### Programmatically
```java
import com.oddlabs.tt.util.Utils;

// Enable CSV translations
Utils.setUseCSVTranslations(true);
```

## Using Translations in Code

### Getting a ResourceBundle
```java
import com.oddlabs.tt.util.Utils;
import java.util.ResourceBundle;

// Recommended: Use Utils.getBundle() for automatic backend selection
ResourceBundle bundle = Utils.getBundle(GameMenu.class);

// Or with just the class name
ResourceBundle bundle = Utils.getBundle("com.oddlabs.tt.form.GameMenu");
```

### Getting Translated Strings
```java
// Simple string
String text = Utils.getBundleString(bundle, "game_caption");

// String with parameters
String text = Utils.getBundleString(bundle, "player", new Object[]{playerName});
```

## Converting Properties to CSV

A Python script is provided to convert all properties files to a single CSV:

```bash
python3 tools/convert_properties_to_csv.py
```

This will:
1. Read all `.properties` files from `tt/i18n/`
2. Parse all translations for all locales
3. Write a consolidated `tt/i18n/translations.csv`

## Adding New Translations

### With CSV (Recommended)
1. Open `tt/i18n/translations.csv` in a spreadsheet editor or text editor
2. Add a new row with the key and translations for all languages
3. Key format: `package.ClassName.property_key`

### With Properties Files (Legacy)
1. Add the key and English value to the base `.properties` file
2. Add translated values to each locale file (`_da.properties`, `_de.properties`, etc.)

## Build Integration

The CSV file is automatically included in the `tt-i18n.jar` during the build process. The jar includes:
- All `.properties` files (for legacy compatibility)
- The `translations.csv` file (for CSV mode)

## Benefits of CSV Approach

1. **Single file to edit**: All translations in one place
2. **Easy to review**: See all languages side-by-side
3. **Spreadsheet compatible**: Edit in Excel, Google Sheets, etc.
4. **No duplicate structure**: Keys defined once, not in multiple files
5. **Simpler diffing**: Easier to see what changed in version control

## Architecture

### CSVResourceBundle
`com.oddlabs.tt.util.CSVResourceBundle` - Custom ResourceBundle implementation that:
- Loads all translations from `translations.csv` on first access
- Caches translations in memory for fast lookup
- Falls back to English if a translation is missing
- Uses a custom `ResourceBundle.Control` for integration with Java's bundle system

### Utils
`com.oddlabs.tt.util.Utils` provides:
- `getBundle(Class)` - Get bundle for a class
- `getBundle(String)` - Get bundle by name
- `setUseCSVTranslations(boolean)` - Toggle CSV mode
- `getBundleString(bundle, key)` - Get translated string with parameter substitution
