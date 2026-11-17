---
type: runbook
status: active
priority: high
tags: [ops, runbook, release, modrinth, licence, ci]
aliases: [Release runbook, Modrinth upload, Modrinth release]
summary: "Shipping a version: bump VERSION and CHANGELOG, commit, merge to product, tag v<VERSION> on product and push the tag. CI builds, tests, runs the Mixin smoke, publishes both jars to Modrinth with tools/publish_modrinth.py and creates the GitHub Release. Manual upload is the fallback."
related:
  - "[[Build and Test]]"
  - "[[Publish to Modrinth from version tags with an in-repo script]]"
  - "[[Private repository, sources jar on every upload]]"
  - "[[Single Fabric 26.3 target]]"
  - "[[v1.3.0.1 Modrinth releases from CI]]"
  - "[[05 Summaries]]"
created: 2026-11-17
updated: 2026-11-17
---

# Release to Modrinth

## When to use

Every time a version of ImagineBook Continued goes out to players. A release is a `v<VERSION>` tag on `product`. A push or pull request to `product` without a tag builds and tests but publishes nothing ([[Publish to Modrinth from version tags with an in-repo script]]).

## Impact

Every Modrinth version notifies the mod's followers, so only a deliberate tag publishes. A missing sources jar breaks the LGPL-3.0 obligation (see [[Private repository, sources jar on every upload]]). Wrong environment or dependency settings make launchers install the mod on servers or without Fabric API.

## Before the release

1. Build and test locally: `python tools/build_local.py --test`, then `python tools/mixin_smoke.py` with Stendhal (see [[Build and Test]]). CI's Mixin smoke runs without Stendhal.
2. Optionally run `python tools/ingame_test/run.py` for a real client check.
3. The repository secret `MODRINTH_TOKEN` must exist (see "The token" below).

## Procedure

1. Bump `VERSION` odometer style, one digit per part: `1.3.0.9` becomes `1.3.1.0`, never `1.3.0.10`.
2. Add a `## <VERSION>` heading at the top of the version list in `CHANGELOG.md` with the changes. The section body becomes the Modrinth changelog, so it must not be empty. `VersionDriftTest` fails if the newest heading is not the content of `VERSION`.
3. Commit with one line: `v<VERSION> <Short description>`.
4. Merge into `product`. CI on `product` builds and tests; it does not publish.
5. Tag `product` and push the tag (agents do this only when the owner asks):

```bash
git tag v<VERSION> product
git push origin v<VERSION>
```

## What CI does on the tag

`.github/workflows/build.yml`, one job, steps in order. A failed step stops the job, so nothing is published from a red build.

| Step | Command |
|---|---|
| Build and JUnit tests, jar and sources jar | `bash ./gradlew build` |
| Release script tests | `python3 -m unittest discover -s tools -p "test_*.py"` |
| Mixin smoke | `bash ./gradlew runMixinSmoke -x downloadAssets` |
| Artifact `imaginebook-continued`, 30 days | upload of `build/libs/imaginebook-continued-fabric-*.jar` |
| Publish to Modrinth (tags only) | `python3 tools/publish_modrinth.py --tag "${GITHUB_REF_NAME}"` with `MODRINTH_TOKEN` from secrets and `MODRINTH_PROJECT: imaginebook-continued` |
| GitHub Release (tags only) | `softprops/action-gh-release@v2`, both jars, `fail_on_unmatched_files`, generated release notes |

The workflow has `permissions: contents: write` so softprops can create the release.

## The token

- `MODRINTH_TOKEN` is a repository secret holding a Modrinth personal access token (PAT) with the scopes Create versions, Read projects and Read versions.
- The owner sets it in GitHub: repository Settings, Secrets and variables, Actions. It is never pasted into a chat, and nobody asks for it there.
- The script sends it only in the `Authorization` header and never prints it; `test_token_goes_only_into_authorization` pins that.
- Without it the script stops with `MODRINTH_TOKEN is not set; add it as a repository secret`. On HTTP 401, 403 or 404 it adds the hint to check the token scopes and `MODRINTH_PROJECT`.

## What the script checks

`tools/publish_modrinth.py`, standard library only. It takes everything from the repository and stops before sending anything when a check fails:

1. `CHANGELOG.md` has a heading exactly `## <VERSION>` and its section is not empty.
2. Both jars are in `build/libs/`: `imaginebook-continued-fabric-<VERSION>+26.3.jar` and `imaginebook-continued-fabric-<VERSION>+26.3-sources.jar`.
3. The tag is exactly `v` + `VERSION` (so `1.3.0.1`, `v1.3.0.0` or `v1.3.0.1+26.3` are refused for 1.3.0.1).
4. It resolves `MODRINTH_PROJECT` (slug, default `imaginebook-continued`) to the project ID, lists the project's versions and skips with `<version_number> is already on Modrinth; nothing to do` when that `version_number` is up. Re-running the job is safe.

## What the script sends

| Field | Value |
|---|---|
| `version_number` | `<VERSION>+26.3` |
| `name` | `<VERSION> for Minecraft 26.3` |
| `changelog` | the `## <VERSION>` section of `CHANGELOG.md` |
| `game_versions`, `loaders` | `26.3`, `fabric` |
| `version_type`, `status`, `featured` | `release`, `listed`, false |
| `environment` | `client_only` |
| Files | mod jar as the primary file; sources jar with file type `sources-jar` |
| Dependencies | `P7dR8mSH` Fabric API, required; `BWloQ9jK` Stendhal, optional |

The version API takes project IDs, not slugs, so the dependency IDs are hard-coded in `DEPENDENCIES` (read from `api.modrinth.com/v2/project/<slug>` on 2026-11-17). The User-Agent names the project, because Modrinth blocks generic clients. The settings agree with `src/main/resources/fabric.mod.json`: `"environment": "client"`, `depends` on `fabricloader >=0.20.1`, `fabric-api`, `minecraft 26.3` and `java >=25`, and `suggests` Stendhal.

## Dry run

```bash
python tools/publish_modrinth.py --dry-run
```

From the repository root after a build: prints the version data (project ID shown as `<id of imaginebook-continued>`) and the two file names. It sends nothing and needs no network or token, but it still needs the changelog section and both jars. The tag is not checked.

## Verification

- The CI run for the tag is green, and the publish step prints `published <VERSION>+26.3 (version id ...) to Modrinth project imaginebook-continued`.
- The Modrinth version page lists two files, the primary jar and the sources jar, and shows Fabric, 26.3, client only and both dependencies.
- The changelog on Modrinth matches the `CHANGELOG.md` section.
- The GitHub Release for the tag holds both jars.

## Manual upload (fallback)

If CI cannot publish, upload by hand on the Modrinth project `imaginebook-continued`, using the jars from the CI artifact `imaginebook-continued` or from `python tools/build_local.py`: `imaginebook-continued-fabric-<VERSION>+26.3.jar` as the primary file and `imaginebook-continued-fabric-<VERSION>+26.3-sources.jar` as an additional file, always both, with the values in "What the script sends" (Fabric, 26.3, client only, Fabric API required, Stendhal optional) and the `CHANGELOG.md` section as the changelog. Use exactly `<VERSION>+26.3` as the version number: a later run of the tag job then finds it and skips instead of publishing a duplicate.

## Links

- [[Build and Test]]
- [[Publish to Modrinth from version tags with an in-repo script]]
- [[Private repository, sources jar on every upload]]
- [[v1.3.0.1 Modrinth releases from CI]]
- [[05 Summaries]]
