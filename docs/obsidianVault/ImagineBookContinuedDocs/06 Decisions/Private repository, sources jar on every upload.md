---
type: decision
status: active
priority: critical
tags: [adr, licence, lgpl, release]
aliases: [LGPL sources jar, Sources jar rule]
summary: "While the GitHub repository is private, every published mod jar ships with its sources jar (LICENSE inside both), so the LGPL-3.0-only source obligation is met from the same place the binary is downloaded."
related:
  - "[[Release to Modrinth]]"
  - "[[Build and Test]]"
created: 2026-11-17
updated: 2026-11-17
---

# Private repository, sources jar on every upload

## Status

Accepted in v1.3.0.0. In force for as long as the repository is private.

## Context

- ImagineBook is licensed LGPL-3.0-only (`LICENSE`, `"license": "LGPL-3.0-only"` in `fabric.mod.json`), and the fork keeps that licence.
- LGPL-3.0 builds on GPL-3.0. Conveying the mod in object code (a jar) comes with the duty to give recipients the corresponding source; GPL-3.0 section 6(d) allows doing that by offering equivalent access to the source from the same place as the binary.
- The fork's GitHub repository is private, so a repository link cannot serve as the source offer. Linking the upstream repositories would not cover the fork's own changes.

## Decision

Every published mod jar ships with its sources jar, from the same place.

- `build.gradle` has `withSourcesJar()`, and both `jar` and `sourcesJar` include `LICENSE`. `tools/build_local.py` produces the same pair, also with `LICENSE` inside.
- CI uploads both jars as the build artifact, and a `v*` tag attaches both to the GitHub Release.
- On Modrinth, every version carries `imaginebook-continued-fabric-<VERSION>+26.3-sources.jar` as an additional file next to the primary jar ([[Release to Modrinth]]).
- `CiWorkflowTest.sourcesJarIsPublishedNextToTheMod` pins `withSourcesJar()`, the jar glob in the workflow, and keeps the development-only smoke jar out of `build/libs/`.

## Consequences

### Positive

- Anyone who downloads the mod can get the matching source from the same page, without access to the private repository.
- The rule is enforced by the build and a test, not only by memory at release time.

### Trade-offs accepted

- Every Modrinth upload is two files, and forgetting the second one is a licence problem; the release runbook lists it explicitly.
- The sources jar holds `src/main/java` and `src/main/resources` only; build scripts and tests are not in it.

## Alternatives considered

- **Make the repository public.** Not chosen while the repository is private; if it goes public, revisit this decision (the sources jar can stay as a convenience).
- **Link to the upstream repositories.** Not chosen: the fork's changes are not there, so it would not be the corresponding source.

## References

- Version: 1.3.0.0
- Repository files: `LICENSE`, `build.gradle`, `tools/build_local.py`, `.github/workflows/build.yml`, `src/test/java/io/github/jumperonjava/imaginebook/CiWorkflowTest.java`
- Related notes: [[Release to Modrinth]], [[Build and Test]]
