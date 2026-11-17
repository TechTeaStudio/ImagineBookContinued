---
type: decision
status: active
priority: high
tags: [adr, build, fabric, minecraft]
aliases: [Fabric only, One target]
summary: "The fork ships one build, Fabric for Minecraft 26.3; the upstream Stonecutter 1.20 to 1.21 build and the NeoForge and 26.2 targets were dropped and remain in the git history."
related:
  - "[[Module Layout]]"
  - "[[Build and Test]]"
  - "[[Release to Modrinth]]"
  - "[[v1.3.0.0 First release of the fork]]"
created: 2026-11-17
updated: 2026-11-17
---

# Single Fabric 26.3 target

## Status

Accepted in v1.3.0.0. In force.

## Context

- The repository the fork started from built many targets: a Stonecutter build for Minecraft 1.20 to 1.21, and under `modern/` four builds for 26.2 and 26.3 on Fabric and NeoForge.
- Every target is a separate jar to build, test and check for Mixin application.
- The fork's CI runs on a private repository where Actions minutes are counted, and local builds go over a metered mobile connection ([[Build and Test]]).

## Decision

Ship one build: Fabric for Minecraft 26.3.

- The root `build.gradle` is a plain Fabric Loom build: `minecraft "com.mojang:minecraft:26.3"`, Fabric Loader 0.20.1, Fabric API, Java 25, no mappings (26.x is unobfuscated).
- `fabric.mod.json` depends on `minecraft` 26.3 exactly, `fabricloader >=0.20.1`, `fabric-api` and `java >=25`.
- The Stonecutter build and the NeoForge and 26.2 targets were removed from the tree. They remain in the git history.
- What is left of the platform split is `platform/PlatformEntrypoint` (the Fabric client entrypoint) and `platform/PlatformPaths` (`FabricLoader` folders).

## Consequences

### Positive

- One build script, one CI job, one jar pair per release.
- The local Gradle-free build compiles against a single MultiMC copy of Minecraft 26.3.
- The unit tests, the Mixin smoke check and the in-game test all cover exactly the build that ships.

### Trade-offs accepted

- Players on 1.20 to 1.21, on 26.2 or on NeoForge get no release of the fork.
- Bringing a target back means restoring it from the git history and adding its own build and checks.

## Alternatives considered

- **Keep the upstream multi-target build.** Not chosen: each target would need its own build, tests and Mixin check, in CI and locally, while the fork is maintained and tested for one setup.

## References

- Version: 1.3.0.0, `CHANGELOG.md` ("Removed the 1.20 to 1.21 Stonecutter build and the NeoForge and 26.2 targets from this fork")
- Repository files: `build.gradle`, `settings.gradle`, `src/main/resources/fabric.mod.json`, `src/main/java/io/github/jumperonjava/imaginebook/platform/`
- Related notes: [[Module Layout]], [[Build and Test]]
