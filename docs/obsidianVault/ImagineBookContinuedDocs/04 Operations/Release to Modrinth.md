---
type: runbook
status: active
priority: high
tags: [ops, runbook, release, modrinth, licence]
aliases: [Release runbook, Modrinth upload]
summary: "Shipping a version: bump VERSION and CHANGELOG, commit, merge to product, then upload the mod jar plus its sources jar to Modrinth project imaginebook-continued with the right loader, game version, environment and dependencies."
related:
  - "[[Build and Test]]"
  - "[[Private repository, sources jar on every upload]]"
  - "[[Single Fabric 26.3 target]]"
  - "[[05 Summaries]]"
created: 2026-11-17
updated: 2026-11-17
---

# Release to Modrinth

## When to use

Every time a version of ImagineBook Continued goes out to players.

## Impact

A wrong file or missing sources jar on Modrinth either ships the wrong build or breaks the LGPL-3.0 obligation (see [[Private repository, sources jar on every upload]]). Wrong environment or dependency settings make launchers install the mod on servers or without Fabric API.

## Before the release

1. Build and test locally: `python tools/build_local.py --test`, then `python tools/mixin_smoke.py` with Stendhal (see [[Build and Test]]).
2. Optionally run `python tools/ingame_test/run.py` for a real client check.

## Version and commit

1. Bump `VERSION` odometer style, one digit per part: `1.3.0.9` becomes `1.3.1.0`, never `1.3.0.10`.
2. Add a `## <VERSION>` heading at the top of the version list in `CHANGELOG.md` with the changes. `VersionDriftTest` fails if the newest heading is not the content of `VERSION`.
3. Commit with one line: `v<VERSION> <Short description>`.
4. Merge into `product`. The CI run on `product` builds, tests, runs the Mixin smoke and keeps both jars as the artifact `imaginebook-continued`. A `v*` tag also creates a GitHub Release with both jars.

## Files to upload

| File | Modrinth role |
|---|---|
| `imaginebook-continued-fabric-<VERSION>+26.3.jar` | Primary file |
| `imaginebook-continued-fabric-<VERSION>+26.3-sources.jar` | Additional file |

The sources jar goes up with every version, without exception: the GitHub repository is private, so the Modrinth page is where players get the source.

## Modrinth settings

| Setting | Value |
|---|---|
| Project slug | `imaginebook-continued` (also the `homepage` in `fabric.mod.json`) |
| Loader | Fabric |
| Game version | 26.3 |
| Client side | Required |
| Server side | Unsupported |
| Dependency: Fabric API | Required |
| Dependency: Stendhal | Optional |

These match `src/main/resources/fabric.mod.json`: `"environment": "client"`, `depends` on `fabricloader >=0.20.1`, `fabric-api`, `minecraft 26.3` and `java >=25`, and `suggests` Stendhal.

## Verification

- The Modrinth version page lists two files, the primary jar and the sources jar.
- The version shows Fabric, 26.3, client only, and both dependencies.
- The changelog text on Modrinth matches the `CHANGELOG.md` entry.

## Links

- [[Build and Test]]
- [[Private repository, sources jar on every upload]]
- [[05 Summaries]]
