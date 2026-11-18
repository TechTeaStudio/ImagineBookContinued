---
type: decision
status: active
priority: high
tags: [adr, release, modrinth, ci]
aliases: [Tag-only Modrinth publish, Modrinth publish script]
summary: "Modrinth versions are published by CI only from a v<VERSION> tag, by the standard-library script tools/publish_modrinth.py instead of a third-party action or Gradle plugin."
related:
  - "[[Release to Modrinth]]"
  - "[[Private repository, sources jar on every upload]]"
  - "[[Build and Test]]"
  - "[[v1.3.0.1 Modrinth releases from CI]]"
  - "[[v1.3.0.2 First Modrinth release]]"
  - "[[v1.3.2.1 Release notes since the last upload]]"
created: 2026-11-17
updated: 2026-11-18
---

# Publish to Modrinth from version tags with an in-repo script

## Status

Accepted in v1.3.0.1. Extended in v1.3.2.1: the release notes are the `CHANGELOG.md` sections of every version since the last upload, not only the newest one ([[v1.3.2.1 Release notes since the last upload]]).

## Context

- Releases were uploaded to Modrinth by hand, two jars per version. The goal was automatic Modrinth releases.
- Every Modrinth version notifies the mod's followers (comment in `.github/workflows/build.yml`).
- The workflow already runs on every push and pull request to `product`, including pushes that change only CI or docs.
- Every version must carry its sources jar, because the repository is private and the mod is LGPL-3.0-only ([[Private repository, sources jar on every upload]]).
- The repository is private and Actions minutes are counted, so CI is one job on one runner.
- Versions follow the Minecraft 26.x scheme: `<VERSION>+26.3`.

## Decision

CI publishes to Modrinth only from a `v<VERSION>` tag, using our own standard-library Python script.

- `build.yml` triggers on `v*` tags as well as on `product`. The publish step runs only when `github.ref` starts with `refs/tags/v`, after the Gradle build, the release script tests and the Mixin smoke.
- `tools/publish_modrinth.py` uses only the Python standard library (`urllib`, `json`, `uuid`) and builds the multipart request itself.
- It takes everything from the repository: `VERSION`, the `CHANGELOG.md` section, the two jars in `build/libs/`. Since 1.3.2.1 it also sends the sections of versions that were committed but never uploaded, back to the newest version on Modrinth (`changelog_since`, see [[Release to Modrinth#Release notes since the last upload]]).
- It refuses a tag that is not `v` + `VERSION`, an empty changelog section or a missing jar, and skips a `version_number` that is already on Modrinth, so a re-run is safe.
- `tools/test_publish_modrinth.py` unit-tests it; CI runs those tests before the publish step.
- The token is the repository secret `MODRINTH_TOKEN`.

The full procedure is in [[Release to Modrinth]].

## Consequences

### Positive

- A push to `product` that changes only CI or docs never publishes a version or notifies followers.
- No extra download on CI: no action or plugin to fetch, only `python3` from the runner.
- Full control of the request: every field, the `sources-jar` file type for the sources jar, the User-Agent.
- No dependence on a third-party tool knowing the Minecraft 26.x version scheme.
- The release logic is pinned by unit tests: changelog section, tag check, version data, dependency IDs, jar lookup, multipart body, token only in the `Authorization` header.

### Trade-offs accepted

- The dependency IDs live in the script (`DEPENDENCIES`: `P7dR8mSH` Fabric API, `BWloQ9jK` Stendhal), because the version API takes IDs, not slugs. A new dependency means editing the script and its test.
- The Minecraft version `26.3` is a constant in the script (`MC`), so a new target also means editing it.
- Releasing needs one more deliberate step: tag `product` and push the tag.
- The first automated release is 1.3.0.2, whose mod code is identical to 1.3.0.0 (`CHANGELOG.md`: "The mod is the same as 1.3.0.0"). 1.3.0.1 was never released because the CI Mixin check failed; see [[v1.3.0.2 First Modrinth release]].

## Alternatives considered

- **Publish on every push to `product`.** Not chosen: every Modrinth version notifies followers, so a CI or docs bump would publish one.
- **mc-publish GitHub Action.** Not chosen: an extra download on CI, less control of the request, and dependence on the action knowing the 26.x version scheme.
- **Minotaur Gradle plugin.** Not chosen for the same reasons.
- **Keep uploading by hand.** Not chosen as the main path; it stays as the fallback in [[Release to Modrinth]].

## References

- Version: 1.3.0.1
- Repository files: `.github/workflows/build.yml`, `tools/publish_modrinth.py`, `tools/test_publish_modrinth.py`, `CLAUDE.md` (Release flow), `CHANGELOG.md`
- Related notes: [[Release to Modrinth]], [[Private repository, sources jar on every upload]], [[v1.3.0.1 Modrinth releases from CI]]
