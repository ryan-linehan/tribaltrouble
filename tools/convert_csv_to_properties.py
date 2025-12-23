#!/usr/bin/env python3
"""
Convert translations.csv back to individual .properties files.
This is the reverse of convert_properties_to_csv.py.

Use this script after editing translations.csv to regenerate all .properties files.
"""

import csv
import os
from pathlib import Path
from collections import defaultdict

# Base directory for i18n files
I18N_DIR = Path(__file__).parent.parent / "tt" / "i18n"
CSV_FILE = I18N_DIR / "translations.csv"

# Locale mappings: CSV column -> properties file suffix
LOCALE_SUFFIXES = {
    "en": "",      # English is the default (no suffix)
    "da": "_da",
    "de": "_de",
    "es": "_es",
    "it": "_it",
}


def escape_properties_value(value):
    """Escape special characters for .properties file format."""
    if not value:
        return ""

    # Handle newlines - convert to escaped form with continuation
    if '\n' in value:
        lines = value.split('\n')
        escaped_lines = []
        for i, line in enumerate(lines):
            if i < len(lines) - 1:
                escaped_lines.append(line + "\\n\\")
            else:
                escaped_lines.append(line)
        value = "\n\t\t\t  ".join(escaped_lines)

    return value


def parse_key(full_key):
    """Parse a full key into bundle path and property key.

    Example: com.oddlabs.tt.form.GameMenu.game_caption
    Returns: ("com/oddlabs/tt/form/GameMenu", "game_caption")
    """
    parts = full_key.rsplit(".", 1)
    if len(parts) != 2:
        return None, None

    bundle_path = parts[0].replace(".", "/")
    property_key = parts[1]
    return bundle_path, property_key


def load_csv():
    """Load translations from CSV file."""
    # Structure: {bundle_path: {property_key: {locale: value}}}
    translations = defaultdict(lambda: defaultdict(dict))

    with open(CSV_FILE, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            full_key = row.get('key', '')
            bundle_path, property_key = parse_key(full_key)

            if not bundle_path or not property_key:
                print(f"Warning: Skipping invalid key: {full_key}")
                continue

            for locale in LOCALE_SUFFIXES.keys():
                value = row.get(locale, '')
                if value:  # Only store non-empty values
                    translations[bundle_path][property_key][locale] = value

    return translations


def write_properties_file(filepath, properties):
    """Write a .properties file with the given key-value pairs."""
    filepath.parent.mkdir(parents=True, exist_ok=True)

    with open(filepath, 'w', encoding='utf-8') as f:
        for key in sorted(properties.keys()):
            value = escape_properties_value(properties[key])
            f.write(f"{key}={value}\n")


def generate_properties_files(translations):
    """Generate all .properties files from translations dict."""
    files_written = 0

    for bundle_path, properties in translations.items():
        for locale, suffix in LOCALE_SUFFIXES.items():
            # Collect all properties for this locale
            locale_properties = {}
            for prop_key, locale_values in properties.items():
                if locale in locale_values:
                    locale_properties[prop_key] = locale_values[locale]

            if not locale_properties:
                continue

            # Determine file path
            filename = bundle_path.split("/")[-1] + suffix + ".properties"
            dir_path = "/".join(bundle_path.split("/")[:-1])
            filepath = I18N_DIR / dir_path / filename

            write_properties_file(filepath, locale_properties)
            files_written += 1

    return files_written


def main():
    if not CSV_FILE.exists():
        print(f"Error: CSV file not found: {CSV_FILE}")
        print("Run convert_properties_to_csv.py first to create it.")
        return 1

    print(f"Reading translations from {CSV_FILE}")
    translations = load_csv()

    total_bundles = len(translations)
    total_keys = sum(len(props) for props in translations.values())
    print(f"Found {total_bundles} bundles with {total_keys} unique keys")

    print(f"Writing .properties files to {I18N_DIR}")
    files_written = generate_properties_files(translations)
    print(f"Written {files_written} .properties files")

    return 0


if __name__ == "__main__":
    exit(main())
