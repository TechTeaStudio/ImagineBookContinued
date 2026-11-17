---
type: runbook
status: active
priority: high
tags: [ops, runbook, build, test, ci]
aliases: [Build runbook, Local build]
summary: "How to build and test the mod: the Gradle-free local build, the windowless Mixin smoke check with Stendhal, the optional in-game test, and the Gradle build that CI runs on product and v* tags."
related:
  - "[[Release to Modrinth]]"
  - "[[Stendhal Compatibility]]"
  - "[[Module Layout]]"
  - "[[Private repository, sources jar on every upload]]"
  - "[[v1.3.0.2 First Modrinth release]]"
created: 2026-11-17
updated: 2026-11-17
---

# Build and Test

## When to use

Before every commit that touches code, before every release, and whenever a CI run on `product` fails.

## Impact

The jar in `build/libs/` is what gets uploaded. A Mixin that does not apply crashes the game at startup or on opening a book, so the Mixin check matters as much as the unit tests.

## Local build without Gradle

The owner works over a slow, metered mobile connection. A first Gradle + Loom build downloads about 460 to 500 MB (Gradle, JDK 25, the Minecraft jar and libraries); `tools/build_local.py` downloads about 6 MB once and compiles against the copy of Minecraft 26.3 that MultiMC already has.

```bash
python tools/build_local.py          # jar + sources jar into build/libs
python tools/build_local.py --test   # also compile and run the JUnit tests
```

- Compiler: ECJ 3.46.100, run as `-25`; tests run with the JUnit Platform console launcher 1.11.4. Both are cached in `tools/.cache/` (ignored by git); the ECJ download is checked against a pinned SHA-1.
- `webp-imageio` 0.1.6 is nested into the jar as a Fabric library with a generated `fabric.mod.json`, the same way Loom's `include` does it.
- Classpath: Minecraft 26.3 and Fabric Loader 0.20.1 from MultiMC's `meta/` and `libraries/`, plus MixinExtras 0.5.5, extracted once from the loader jar's `META-INF/jars/` into `tools/.cache/`. `build.gradle` has the same version `compileOnly`; the loader provides it at runtime ([[Module Layout#MixinExtras]]).
- Environment: `MULTIMC_ROOT` (MultiMC folder, default `D:/UslessTrash/MultiMC`), `IMAGINEBOOK_JAVA` (`java.exe` of a Java 25 runtime; a JRE is enough for ECJ).
- Output: classes in `build/local/`, `imaginebook-continued-fabric-<VERSION>+26.3.jar` and `-sources.jar` in `build/libs/`, both with `LICENSE` inside.
- The script reads `VERSION`, like `build.gradle`; `VersionDriftTest` checks it does.

## Mixin smoke check

Checks that every Mixin applies, with Stendhal installed, without opening a game window. Run it after `build_local.py`.

```bash
python tools/mixin_smoke.py                          # mod + Fabric API + Stendhal
python tools/mixin_smoke.py --no-stendhal            # mod + Fabric API only
python tools/mixin_smoke.py --mods-from <mods folder>
```

- Builds the development-only smoke mod from `src/smoke/` and sets up `build/mixin-smoke/mods/` with the mod jar plus the newest `fabric-api-*.jar` and `stendhal-*.jar` from the mods folder (default: the mods folder of a MultiMC 26.3 instance, `DEFAULT_MODS` in `tools/ingame_test/run.py`).
- The smoke mod's `preLaunch` entrypoint loads the patched classes (`Gui`, `Screen`, `BookViewScreen`, `BookEditScreen`, `MultiLineEditBox`, `MultilineTextField`, `RenderPipelines`) before any window exists, and asserts the image pipeline has culling off. `defaultRequire: 1` makes any missed injection an error.
- Pass: `IMAGINEBOOK_SMOKE_PASSED`, exit code 0. The full log is `build/mixin-smoke/smoke.log`.

## In-game test (optional)

```bash
python tools/ingame_test/run.py [--stendhal-from <mods folder>] [--no-stendhal]
```

- Opens a real Minecraft 26.3 client with the mod, Stendhal and Fabric API for about a minute; needs a desktop session.
- Runs offline as `Tester` in the isolated game folder `build/ingame/`, so real worlds, options and the account are never touched. Copies only `level.dat` and `data/` from a source world (`INGAME_WORLD_FROM`).
- Opens a book and quill, takes screenshots, selects an image, resizes the screen and exits. Prints every `IMAGINEBOOK_TEST` line and the screenshot paths; exit code 0 only on PASSED.

## Gradle build

Used by CI, and locally when bandwidth allows. Fabric Loom 1.19.3 (`net.fabricmc.fabric-loom`); Minecraft 26.x is unobfuscated, so there is no mappings line.

```bash
./gradlew build                              # compile, JUnit tests, jar + sources jar
./gradlew runMixinSmoke -x downloadAssets    # the same smoke mod, from smoke.gradle
```

`smoke.gradle` writes the smoke jar to `build/smoke-libs/`, never to `build/libs/`, so it is never uploaded or released. The Gradle runtime has Fabric API but not Stendhal, so `runMixinSmoke` checks the Mixins without Stendhal; the local `tools/mixin_smoke.py` is the check with Stendhal.

- Known pitfall: do not re-add `src/smoke/java` or `src/smoke/resources` with `srcDir` in `smoke.gradle`. They are already the default folders of the `smoke` source set; listing them again makes `processSmokeResources` fail with "Entry fabric.mod.json is a duplicate", which stopped 1.3.0.1 on CI. `CiWorkflowTest.smokeSourceSetUsesGradleDefaultsOnly` forbids `srcDir` there. The local tools build without Gradle and do not catch it. See [[v1.3.0.2 First Modrinth release]].

## CI

`.github/workflows/build.yml`, one job on `ubuntu-latest` (the repository is private and Actions minutes are counted), 30 minute timeout.

- Triggers: push and pull request to `product`, and `v*` tags.
- Steps: Temurin Java 25, `gradle/actions/setup-gradle`, `./gradlew build`, `python3 -m unittest discover -s tools -p "test_*.py"` (the release script tests), `./gradlew runMixinSmoke -x downloadAssets`, upload of `build/libs/imaginebook-continued-fabric-*.jar` (mod jar and sources jar) as the artifact `imaginebook-continued`, kept 30 days.
- On a `v*` tag only: `tools/publish_modrinth.py` publishes both jars to Modrinth, then a GitHub Release with both jars and generated release notes ([[Release to Modrinth]]).
- `CiWorkflowTest` pins the workflow: `product` only (no `main` or `master`), one workflow file, Java 25 in the workflow, `build.gradle` and `imaginebook.mixins.json`, both Gradle steps present, `withSourcesJar()` and the jar glob, the smoke jar outside `build/libs/`.

## Unit tests

`BookPasteTest`, `RetryPolicyTest`, `ImageTagsTest`, `ImageFitTest`, `LinkCodesTest`, `ControlPanelLayoutTest`, `StendhalCompatSourceTest`, `CiWorkflowTest`, `VersionDriftTest` in `src/test/java/`, JUnit 5; 60 tests at 1.3.1.1 (48 at 1.3.1.0, then 6 more in `ImageTagsTest` and 6 in the new `ImageFitTest`, see [[v1.3.1.1 Bracket text and fitted pictures]]). They run in both `build_local.py --test` and `./gradlew build`.

The release script has its own Python tests, `tools/test_publish_modrinth.py` (unittest, 13 tests at 1.3.1.1): `python -m unittest discover -s tools -p "test_*.py"`.

## Verification

- `build_local.py --test` ends with no failed tests and prints `built` for both jars.
- `mixin_smoke.py` prints `PASSED`.
- The CI run on `product` is green and the artifact holds two jars.

## Links

- [[Release to Modrinth]]
- [[Stendhal Compatibility]]
- [[Module Layout]]
