#!/usr/bin/env python3
"""Fail closed if Android backup rules stop excluding FamilyMesh local state."""

from pathlib import Path
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID_NS = "{http://schemas.android.com/apk/res/android}"
EXPECTED_DOMAINS = {
    "root",
    "file",
    "database",
    "sharedpref",
    "external",
    "device_root",
    "device_file",
    "device_database",
    "device_sharedpref",
}


def fail(message: str) -> None:
    print(f"Android backup policy verification failed: {message}", file=sys.stderr)
    raise SystemExit(1)


def main() -> None:
    manifest_path = ROOT / "app/src/main/AndroidManifest.xml"
    rules_path = ROOT / "app/src/main/res/xml/data_extraction_rules.xml"

    try:
        manifest = ET.parse(manifest_path).getroot()
        rules = ET.parse(rules_path).getroot()
    except (OSError, ET.ParseError) as error:
        fail(f"could not parse required Android XML: {error}")

    application = manifest.find("application")
    if application is None:
        fail("manifest has no application element")
    if application.get(ANDROID_NS + "allowBackup") != "false":
        fail("android:allowBackup must remain false")
    if application.get(ANDROID_NS + "dataExtractionRules") != "@xml/data_extraction_rules":
        fail("application must reference @xml/data_extraction_rules")
    if application.get(ANDROID_NS + "icon") != "@mipmap/ic_launcher":
        fail("application must declare @mipmap/ic_launcher")

    if rules.tag != "data-extraction-rules":
        fail("data-extraction-rules is the required root element")

    for section_name in ("cloud-backup", "device-transfer"):
        section = rules.find(section_name)
        if section is None:
            fail(f"missing {section_name} section")

        if section.findall("include"):
            fail(f"{section_name} must not include app data")

        exclusions = section.findall("exclude")
        domains = [item.get("domain") for item in exclusions]
        if set(domains) != EXPECTED_DOMAINS or len(domains) != len(EXPECTED_DOMAINS):
            fail(f"{section_name} must exclude each supported data domain exactly once")

        if any(item.get("path") != "." for item in exclusions):
            fail(f"{section_name} exclusions must cover the full domain path")

    print("Android backup policy verified: cloud backup and device transfer exclude all app data.")


if __name__ == "__main__":
    main()
