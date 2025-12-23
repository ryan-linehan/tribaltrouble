#!/usr/bin/env python3
"""
Convert all .properties translation files to a single CSV file.
This script reads all .properties files from tt/i18n and creates
a consolidated translations.csv file.
"""

import os
import csv
import re
from pathlib import Path
from collections import defaultdict

# Base directory for i18n files
I18N_DIR = Path(__file__).parent.parent / "tt" / "i18n"
OUTPUT_FILE = Path(__file__).parent.parent / "tt" / "i18n" / "translations.csv"

# Supported locales (empty string means default/English)
LOCALES = ["", "da", "de", "es", "it"]
LOCALE_HEADERS = ["en", "da", "de", "es", "it"]


def parse_properties_file(filepath):
    """Parse a Java .properties file and return a dict of key-value pairs."""
    properties = {}

    if not filepath.exists():
        return properties

    with open(filepath, 'r', encoding='utf-8', errors='replace') as f:
        content = f.read()

    # Handle line continuations (lines ending with \)
    content = re.sub(r'\\\n\s*', '', content)

    for line in content.split('\n'):
        line = line.strip()

        # Skip empty lines and comments
        if not line or line.startswith('#') or line.startswith('!'):
            continue

        # Find the separator (= or :)
        match = re.match(r'^([^=:]+?)\s*[=:]\s*(.*)', line)
        if match:
            key = match.group(1).strip()
            value = match.group(2)

            # Unescape common Java properties escapes
            value = value.replace('\\n', '\n')
            value = value.replace('\\t', '\t')
            value = value.replace('\\\\', '\\')

            properties[key] = value

    return properties


def get_bundle_name(filepath):
    """Extract the bundle name from a properties file path."""
    # Get relative path from i18n directory
    rel_path = filepath.relative_to(I18N_DIR)

    # Remove locale suffix and .properties extension
    name = rel_path.stem
    for locale in LOCALES:
        if locale and name.endswith(f"_{locale}"):
            name = name[:-len(f"_{locale}")]
            break

    # Convert path to Java package format
    parts = list(rel_path.parent.parts) + [name]
    return ".".join(parts)


def get_locale_from_filename(filepath):
    """Extract the locale from a properties filename."""
    name = filepath.stem
    for locale in LOCALES:
        if locale and name.endswith(f"_{locale}"):
            return locale
    return ""  # Default locale (English)


def collect_all_translations():
    """Collect all translations from .properties files."""
    # Structure: {bundle_name: {key: {locale: value}}}
    translations = defaultdict(lambda: defaultdict(dict))

    # Find all .properties files
    for properties_file in I18N_DIR.rglob("*.properties"):
        bundle_name = get_bundle_name(properties_file)
        locale = get_locale_from_filename(properties_file)
        locale_key = locale if locale else "en"

        properties = parse_properties_file(properties_file)

        for key, value in properties.items():
            full_key = f"{bundle_name}.{key}"
            translations[bundle_name][key][locale_key] = value

    return translations


def write_csv(translations):
    """Write all translations to a single CSV file."""
    # Collect all rows
    rows = []

    for bundle_name in sorted(translations.keys()):
        bundle_trans = translations[bundle_name]
        for key in sorted(bundle_trans.keys()):
            full_key = f"{bundle_name}.{key}"
            locale_values = bundle_trans[key]

            row = [full_key]
            for locale in LOCALE_HEADERS:
                row.append(locale_values.get(locale, ""))
            rows.append(row)

    # Write CSV
    with open(OUTPUT_FILE, 'w', encoding='utf-8', newline='') as f:
        writer = csv.writer(f)
        # Write header
        writer.writerow(["key"] + LOCALE_HEADERS)
        # Write data
        writer.writerows(rows)

    print(f"Written {len(rows)} translation keys to {OUTPUT_FILE}")


def main():
    print(f"Reading properties files from {I18N_DIR}")
    translations = collect_all_translations()

    total_bundles = len(translations)
    total_keys = sum(len(keys) for keys in translations.values())
    print(f"Found {total_bundles} bundles with {total_keys} unique keys")

    write_csv(translations)


if __name__ == "__main__":
    main()
