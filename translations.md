# Tribal Trouble Translation System

This document describes the translation/internationalization (i18n) system for Tribal Trouble.

## Overview

Translations are managed using a **single CSV file** as the source of truth, which generates standard Java `.properties` files for runtime use.

### Workflow
1. Edit `tt/i18n/translations.csv` (single file with all translations)
2. Run `python3 tools/convert_csv_to_properties.py` to generate `.properties` files
3. Build and run the game (uses standard Java ResourceBundle)

## Supported Languages

| Code | Language | Status |
|------|----------|--------|
| en   | English  | Default |
| da   | Danish   | Complete |
| de   | German   | Complete |
| es   | Spanish  | Complete |
| it   | Italian  | Complete |

## File Locations

### Source File (Edit This)
```
tt/i18n/translations.csv
```

### Generated Files (Don't Edit Directly)
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

## CSV Format

```csv
key,en,da,de,es,it
com.oddlabs.tt.form.GameMenu.game_caption,Game,Spil,Spiel,Juego,Gioco
com.oddlabs.tt.form.GameMenu.start,Start,Start,Start,Iniciar,Avvia
```

- **key**: Fully qualified key in format `package.ClassName.property_key`
- **en, da, de, es, it**: Translation values for each language
- Multi-line values are supported using standard CSV quoting

## Scripts

### Generate Properties from CSV
After editing `translations.csv`, run:
```bash
python3 tools/convert_csv_to_properties.py
```

This regenerates all 485 `.properties` files from the CSV.

### Generate CSV from Properties (One-time Migration)
If you need to regenerate the CSV from existing properties files:
```bash
python3 tools/convert_properties_to_csv.py
```

## Adding New Translations

1. Open `tt/i18n/translations.csv` in a spreadsheet editor or text editor
2. Add a new row with:
   - Key: `package.ClassName.property_key` (e.g., `com.oddlabs.tt.form.GameMenu.new_button`)
   - Values for each language column
3. Run `python3 tools/convert_csv_to_properties.py`
4. Commit both the CSV and generated `.properties` files

## Adding a New Language

1. Add a new column to `translations.csv` (e.g., `fr` for French)
2. Update `tools/convert_csv_to_properties.py`:
   - Add the locale to `LOCALE_SUFFIXES` dict
3. Add translations in the new column
4. Run the conversion script

## Using Translations in Code

```java
import com.oddlabs.tt.util.Utils;
import java.util.ResourceBundle;

// Get a bundle for a class
ResourceBundle bundle = ResourceBundle.getBundle(GameMenu.class.getName());

// Get translated string
String text = Utils.getBundleString(bundle, "game_caption");

// With parameters
String text = Utils.getBundleString(bundle, "player", new Object[]{playerName});
```

## Benefits of CSV Approach

1. **Single file to edit**: All 1000+ translations in one place
2. **Side-by-side comparison**: See all languages together
3. **Spreadsheet compatible**: Edit in Excel, Google Sheets, LibreOffice Calc
4. **Easy to review**: Simple diffs in version control
5. **No duplicate keys**: Each key defined exactly once
6. **Standard runtime**: Uses proven Java ResourceBundle system

## Tips

- Use a spreadsheet editor for bulk edits
- The CSV uses UTF-8 encoding
- Empty cells mean "use English fallback"
- Run the conversion script before committing
- Commit both CSV and generated files together
