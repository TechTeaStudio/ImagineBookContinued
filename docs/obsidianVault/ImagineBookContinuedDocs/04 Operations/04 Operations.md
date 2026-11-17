---
type: index
status: active
priority: high
tags: [operations, runbooks, index]
aliases: []
summary: "Build, test and release runbooks for the mod: local Gradle-free build, Mixin checks, CI, and the Modrinth upload."
related:
  - "[[Vault Map]]"
  - "[[Build and Test]]"
  - "[[Release to Modrinth]]"
created: 2026-11-17
updated: 2026-11-17
---

# 04 Operations

How the mod is built, checked and shipped to players.

%% Begin Waypoint %%
%% End Waypoint %%

## Key documents

- [[Build and Test]]: `tools/build_local.py`, `tools/mixin_smoke.py`, the in-game test, Gradle and the CI workflow on `product`
- [[Release to Modrinth]]: version bump, commit, merge to `product`, the two files and the Modrinth settings

## Active decisions

- [[Private repository, sources jar on every upload]]
- [[Single Fabric 26.3 target]]

## Status

- Two runbooks, matching version 1.3.0.0.
- There is no version bump script yet; `VERSION` and `CHANGELOG.md` are edited by hand and `VersionDriftTest` checks them.

## All notes

```dataview
TABLE WITHOUT ID file.link AS "Note", summary AS "Summary", status AS "Status", updated AS "Updated"
FROM "04 Operations"
WHERE file.name != this.file.name
SORT file.name ASC
```
