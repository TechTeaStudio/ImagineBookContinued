package io.github.jumperonjava.imaginebook;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * VERSION is the single source of the mod version (studio practice BUILD-02, TEST-01/TEST-02).
 * These files have no compile-time link to it, so this test holds them together.
 */
class VersionDriftTest {
    private static String version() throws IOException {
        return Files.readString(Path.of("VERSION")).trim();
    }

    @Test
    void versionIsAnOdometerWithOneDigitPerPart() throws IOException {
        // Studio rule: four parts, one digit each; 1.9.9.9 rolls over to 2.0.0.0, never 1.9.9.10.
        assertTrue(Pattern.matches("\\d\\.\\d\\.\\d\\.\\d", version()), "VERSION must look like 1.3.0.0");
    }

    @Test
    void newestChangelogEntryIsTheCurrentVersion() throws IOException {
        var matcher = Pattern.compile("(?m)^## (\\S+)").matcher(Files.readString(Path.of("CHANGELOG.md")));
        assertTrue(matcher.find(), "CHANGELOG.md needs '## <version>' headings");
        assertEquals(version(), matcher.group(1));
    }

    @Test
    void buildAndMetadataTakeTheVersionFromTheVersionFile() throws IOException {
        assertTrue(Files.readString(Path.of("build.gradle")).contains("file('VERSION')"));
        assertTrue(Files.readString(Path.of("src/main/resources/fabric.mod.json")).contains("\"version\": \"${version}\""));
        assertTrue(Files.readString(Path.of("tools/build_local.py")).contains("VERSION"));
    }
}
