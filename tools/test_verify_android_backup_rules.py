"""Regression tests for the fail-closed Android backup policy verifier."""

from pathlib import Path
import tempfile
import unittest

from verify_android_backup_rules import verify

ANDROID_NS = 'xmlns:android="http://schemas.android.com/apk/res/android"'
VALID_MANIFEST = f'''<manifest {ANDROID_NS}>
  <application android:allowBackup="false"
      android:dataExtractionRules="@xml/data_extraction_rules"
      android:icon="@mipmap/ic_launcher" />
</manifest>'''
DOMAINS = (
    "root", "file", "database", "sharedpref", "external",
    "device_root", "device_file", "device_database", "device_sharedpref",
)


def make_section(name: str) -> str:
    items = "\n".join(f'<exclude domain="{domain}" path="." />' for domain in DOMAINS)
    return f"<{name}>{items}</{name}>"


VALID_RULES = (
    "<data-extraction-rules>"
    + make_section("cloud-backup")
    + make_section("device-transfer")
    + "</data-extraction-rules>"
)


class AndroidBackupRulesVerifierTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_dir = tempfile.TemporaryDirectory()
        root = Path(self.temp_dir.name)
        self.manifest_path = root / "AndroidManifest.xml"
        self.rules_path = root / "data_extraction_rules.xml"

    def tearDown(self) -> None:
        self.temp_dir.cleanup()

    def assert_rejected(self, message: str, manifest: str = VALID_MANIFEST,
                        rules: str = VALID_RULES) -> None:
        self.manifest_path.write_text(manifest, encoding="utf-8")
        self.rules_path.write_text(rules, encoding="utf-8")
        with self.assertRaisesRegex(ValueError, message):
            verify(self.manifest_path, self.rules_path)

    def test_accepts_complete_cloud_and_device_transfer_exclusions(self) -> None:
        self.manifest_path.write_text(VALID_MANIFEST, encoding="utf-8")
        self.rules_path.write_text(VALID_RULES, encoding="utf-8")
        verify(self.manifest_path, self.rules_path)

    def test_rejects_enabled_android_backup(self) -> None:
        self.assert_rejected("android:allowBackup",
            manifest=VALID_MANIFEST.replace('android:allowBackup="false"', 'android:allowBackup="true"'))

    def test_rejects_missing_application(self) -> None:
        self.assert_rejected("no application", manifest=f"<manifest {ANDROID_NS} />")

    def test_rejects_missing_data_extraction_reference(self) -> None:
        self.assert_rejected("android:dataExtractionRules",
            manifest=VALID_MANIFEST.replace('android:dataExtractionRules="@xml/data_extraction_rules"', ""))

    def test_rejects_missing_launcher_icon_reference(self) -> None:
        self.assert_rejected("android:icon",
            manifest=VALID_MANIFEST.replace('android:icon="@mipmap/ic_launcher"', ""))

    def test_rejects_missing_cloud_backup_section(self) -> None:
        self.assert_rejected("only cloud-backup", rules=VALID_RULES.replace(make_section("cloud-backup"), ""))

    def test_rejects_missing_device_transfer_section(self) -> None:
        self.assert_rejected("only cloud-backup", rules=VALID_RULES.replace(make_section("device-transfer"), ""))

    def test_rejects_unknown_cross_platform_transfer_section(self) -> None:
        self.assert_rejected("only cloud-backup",
            rules=VALID_RULES.replace("</data-extraction-rules>",
                '<cross-platform-transfer platform="ios" /></data-extraction-rules>'))

    def test_rejects_include_rule(self) -> None:
        self.assert_rejected("exclusions only",
            rules=VALID_RULES.replace("<cloud-backup>",
                '<cloud-backup><include domain="file" path="." />'))

    def test_rejects_unknown_child_rule(self) -> None:
        self.assert_rejected("exclusions only",
            rules=VALID_RULES.replace("<cloud-backup>", "<cloud-backup><allow />"))

    def test_rejects_duplicate_domain(self) -> None:
        self.assert_rejected("exactly once",
            rules=VALID_RULES.replace('<exclude domain="file" path="." />',
                '<exclude domain="root" path="." />', 1))

    def test_rejects_missing_domain(self) -> None:
        self.assert_rejected("exactly once",
            rules=VALID_RULES.replace('<exclude domain="device_file" path="." />', "", 1))

    def test_rejects_partial_domain_exclusion(self) -> None:
        self.assert_rejected("full domain path",
            rules=VALID_RULES.replace('<exclude domain="database" path="." />',
                '<exclude domain="database" path="family.db" />', 1))

    def test_rejects_wrong_rules_root(self) -> None:
        self.assert_rejected("data-extraction-rules is the required root",
            rules=VALID_RULES.replace("data-extraction-rules", "full-backup-content"))

    def test_rejects_malformed_xml(self) -> None:
        self.assert_rejected("could not parse", rules="<data-extraction-rules>")


if __name__ == "__main__":
    unittest.main()
