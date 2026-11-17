package io.github.jumperonjava.imaginebook;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the CI workflow and the build scripts to each other and to the studio rules (TEST-02):
 * releases come from `product`, the toolchain matches the mod's Java, both jars are published
 * (LGPL-3.0 sources obligation) and the development-only smoke jar never lands among them.
 */
class CiWorkflowTest {
    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }

    @Test
    void workflowRunsOnProductOnly() throws IOException {
        String workflow = read(".github/workflows/build.yml");
        assertTrue(workflow.contains("branches: [\"product\"]"), "push and pull_request must target product");
        assertFalse(workflow.contains("main"), "the release branch is product, not main");
        assertFalse(workflow.contains("master"), "the release branch is product, not master");
        try (var files = Files.list(Path.of(".github/workflows"))) {
            assertEquals(1, files.count(), "one workflow, to spend Actions minutes on purpose");
        }
    }

    @Test
    void workflowUsesTheModJava() throws IOException {
        String workflow = read(".github/workflows/build.yml");
        String gradle = read("build.gradle");
        String mixins = read("src/main/resources/imaginebook.mixins.json");
        assertTrue(workflow.contains("java-version: '25'"));
        assertTrue(gradle.contains("JavaLanguageVersion.of(25)"));
        assertTrue(mixins.contains("\"JAVA_25\""));
    }

    @Test
    void workflowTestsAndChecksMixins() throws IOException {
        String workflow = read(".github/workflows/build.yml");
        assertTrue(workflow.contains("./gradlew build"));
        assertTrue(workflow.contains("./gradlew runMixinSmoke"));
    }

    @Test
    void modrinthReleaseRunsOnlyOnTagsAfterTheChecks() throws IOException {
        String workflow = read(".github/workflows/build.yml");
        int publish = workflow.indexOf("name: Publish to Modrinth");
        assertTrue(publish > 0, "the workflow publishes to Modrinth");
        String step = workflow.substring(publish, workflow.indexOf("- name:", publish));
        assertTrue(step.contains("if: startsWith(github.ref, 'refs/tags/v')"), "a push to product must not publish a version");
        assertTrue(step.contains("MODRINTH_TOKEN: ${{ secrets.MODRINTH_TOKEN }}"), "the token comes from a secret");
        assertTrue(step.contains("python3 tools/publish_modrinth.py --tag \"${GITHUB_REF_NAME}\""));
        assertTrue(publish > workflow.indexOf("./gradlew runMixinSmoke"), "publish only after the build, tests and Mixin check");
        assertTrue(workflow.contains("python3 -m unittest discover -s tools"), "the release script is tested in CI");
    }

    @Test
    void modrinthSlugMatchesTheModHomepage() throws IOException {
        String workflow = read(".github/workflows/build.yml");
        String mod = read("src/main/resources/fabric.mod.json");
        assertTrue(workflow.contains("MODRINTH_PROJECT: imaginebook-continued"));
        assertTrue(mod.contains("https://modrinth.com/mod/imaginebook-continued"));
        assertTrue(read("tools/publish_modrinth.py").contains("\"imaginebook-continued\")"), "script default matches");
    }

    @Test
    void sourcesJarIsPublishedNextToTheMod() throws IOException {
        String workflow = read(".github/workflows/build.yml");
        assertTrue(read("build.gradle").contains("withSourcesJar()"));
        assertTrue(workflow.contains("build/libs/imaginebook-continued-fabric-*.jar"));
        assertTrue(read("smoke.gradle").contains("destinationDirectory = layout.buildDirectory.dir('smoke-libs')"),
                "the smoke jar must stay out of build/libs, which is uploaded and released as it is");
    }
}
