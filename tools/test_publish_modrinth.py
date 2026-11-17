"""Tests for tools/publish_modrinth.py. Run: python -m unittest discover -s tools -p "test_*.py" """
import json
import tempfile
import unittest
from pathlib import Path

import publish_modrinth as P

CHANGELOG = """# Changelog

Intro text.

## 1.3.0.1

- Second release.

## 1.3.0.0

First release.

- Fixed the crash.

## 1.2.9 (upstream and the 26.x port)

- Old.
"""


class ChangelogSection(unittest.TestCase):
    def test_takes_only_the_section_of_the_version(self):
        self.assertEqual(P.changelog_section(CHANGELOG, "1.3.0.0"), "First release.\n\n- Fixed the crash.")

    def test_newest_section(self):
        self.assertEqual(P.changelog_section(CHANGELOG, "1.3.0.1"), "- Second release.")

    def test_heading_must_match_exactly(self):
        with self.assertRaises(SystemExit):
            P.changelog_section(CHANGELOG, "1.3.0")

    def test_empty_section_is_refused(self):
        with self.assertRaises(SystemExit):
            P.changelog_section("## 1.3.0.2\n\n## 1.3.0.1\n- x\n", "1.3.0.2")


class Tag(unittest.TestCase):
    def test_tag_must_name_the_version(self):
        P.check_tag("v1.3.0.1", "1.3.0.1")
        for tag in ("1.3.0.1", "v1.3.0.0", "v1.3.0.1+26.3", ""):
            with self.assertRaises(SystemExit):
                P.check_tag(tag, "1.3.0.1")


class VersionData(unittest.TestCase):
    def setUp(self):
        self.data = P.version_data("1.3.0.1", "ABCDEFGH", "- Second release.")

    def test_identity(self):
        self.assertEqual(self.data["project_id"], "ABCDEFGH")
        self.assertEqual(self.data["version_number"], "1.3.0.1+26.3")
        self.assertEqual(self.data["name"], "1.3.0.1 for Minecraft 26.3")
        self.assertEqual(self.data["changelog"], "- Second release.")

    def test_target(self):
        self.assertEqual(self.data["game_versions"], ["26.3"])
        self.assertEqual(self.data["loaders"], ["fabric"])
        self.assertEqual(self.data["version_type"], "release")
        self.assertEqual(self.data["environment"], "client_only")

    def test_dependencies_are_ids(self):
        self.assertEqual(self.data["dependencies"], [
            {"project_id": "P7dR8mSH", "dependency_type": "required"},
            {"project_id": "BWloQ9jK", "dependency_type": "optional"},
        ])

    def test_both_files_with_sources_marked(self):
        self.assertEqual(self.data["file_parts"], ["primary", "sources"])
        self.assertEqual(self.data["primary_file"], "primary")
        self.assertEqual(self.data["file_types"], {"sources": "sources-jar"})


class Jars(unittest.TestCase):
    def test_finds_the_mod_and_its_sources(self):
        with tempfile.TemporaryDirectory() as tmp:
            libs = Path(tmp)
            for name in ("imaginebook-continued-fabric-1.3.0.1+26.3.jar",
                         "imaginebook-continued-fabric-1.3.0.1+26.3-sources.jar",
                         "imaginebook-continued-fabric-1.3.0.0+26.3.jar"):
                (libs / name).write_bytes(b"PK")
            jar, sources = P.find_jars(libs, "1.3.0.1")
            self.assertEqual(jar.name, "imaginebook-continued-fabric-1.3.0.1+26.3.jar")
            self.assertEqual(sources.name, "imaginebook-continued-fabric-1.3.0.1+26.3-sources.jar")

    def test_missing_sources_jar_stops_the_release(self):
        with tempfile.TemporaryDirectory() as tmp:
            libs = Path(tmp)
            (libs / "imaginebook-continued-fabric-1.3.0.1+26.3.jar").write_bytes(b"PK")
            with self.assertRaises(SystemExit):
                P.find_jars(libs, "1.3.0.1")


class Multipart(unittest.TestCase):
    def test_data_part_then_files(self):
        with tempfile.TemporaryDirectory() as tmp:
            jar = Path(tmp) / "a.jar"
            jar.write_bytes(b"\x00JAR\xff")
            body, content_type = P.multipart({"data": json.dumps({"k": "v"})}, {"primary": jar})
            boundary = content_type.split("boundary=")[1]
            self.assertTrue(content_type.startswith("multipart/form-data; boundary="))
            text = body.decode("latin-1")
            self.assertIn('Content-Disposition: form-data; name="data"', text)
            self.assertIn('{"k": "v"}', text)
            self.assertIn('Content-Disposition: form-data; name="primary"; filename="a.jar"', text)
            self.assertIn("\x00JAR\xff", text)
            self.assertTrue(text.index('name="data"') < text.index('name="primary"'))
            self.assertTrue(text.endswith(f"--{boundary}--\r\n"))


class Headers(unittest.TestCase):
    def test_token_goes_only_into_authorization(self):
        headers = P.headers("mrp_secret")
        self.assertEqual(headers["Authorization"], "mrp_secret")
        self.assertNotIn("mrp_secret", headers["User-Agent"])
        self.assertIn("TechTeaStudio/ImagineBookContinued", headers["User-Agent"])


if __name__ == "__main__":
    unittest.main()
