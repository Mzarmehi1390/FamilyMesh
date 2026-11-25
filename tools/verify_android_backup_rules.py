#!/usr/bin/env python3
"""Fail closed if Android backup rules stop excluding FamilyMesh local state."""

from pathlib import Path
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID_NS = "{http://schemas.android.com/apk/res/android}"
EXPECTED_DOMAINS = {
    "root", "file", "database", "sharedpref", "external",
    "device_root", "device_file", "device_database", "device_sharedpref",
}
EXPECTED_SECTIONS = {"cloud-backup", "device-transfer"}


def verify(manifest_path: Path, rules_path: Path) -> None:
    """Validate that every supported Android backup path is explicitly excluded."""
    try:
        manifest = ET.parse(manifest_path).getroot()
        rules = ET.parse(rules_path).getroot()
    except (OSError, ET.ParseError) as error:
        raise ValueError(f"could not parse required Android XML: {error}") from error

    if manifest.tag != "manifest":
        raise ValueError("AndroidManifest.xml must have a manifest root")
    application = manifest.find("application")
    if application is None:
        raise ValueError("manifest has no application element")
    if application.get(ANDROID_NS + "allowBackup") != "false":
        raise ValueError("android:allowBackup must remain false")
    if application.get(ANDROID_NS + "dataExtractionRules") != "@xml/data_extraction_rules":
        raise ValueError("application must reference android:dataExtractionRules=@xml/data_extraction_rules")
    if application.get(ANDROID_NS + "icon") != "@mipmap/ic_launcher":
        raise ValueError("application must declare android:icon=@mipmap/ic_launcher")
    if rules.tag != "data-extraction-rules":
        raise ValueError("data-extraction-rules is the required root element")

    sections = list(rules)
    section_names = {section.tag for section in sections}
    if section_names != EXPECTED_SECTIONS or len(sections) != len(EXPECTED_SECTIONS):
        raise ValueError("only cloud-backup and device-transfer sections are allowed")

    for section in sections:
        exclusions = list(section)
        if any(item.tag != "exclude" for item in exclusions):
            raise ValueError(f"{section.tag} must contain exclusions only")
        domains = [item.get("domain") for item in exclusions]
        if set(domains) != EXPECTED_DOMAINS or len(domains) != len(EXPECTED_DOMAINS):
            raise ValueError(f"{section.tag} must exclude each supported data domain exactly once")
        if any(item.get("path") != "." for item in exclusions):
            raise ValueError(f"{section.tag} exclusions must cover the full domain path")


def main() -> int:
    try:
        verify(
            ROOT / "app/src/main/AndroidManifest.xml",
            ROOT / "app/src/main/res/xml/data_extraction_rules.xml",
        )
    except ValueError as error:
        print(f"Android backup policy verification failed: {error}", file=sys.stderr)
        return 1
    print("Android backup policy verified: cloud backup and device transfer exclude all app data.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
