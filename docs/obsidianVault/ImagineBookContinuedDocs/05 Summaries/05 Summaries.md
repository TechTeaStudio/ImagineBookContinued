---
type: index
status: active
priority: medium
tags: [release, changelog, index]
aliases: []
summary: "Per-version change notes for ImagineBook Continued, one note per version or version range."
related:
  - "[[Vault Map]]"
  - "[[v1.3.0.0 First release of the fork]]"
  - "[[v1.3.0.1 Modrinth releases from CI]]"
  - "[[v1.3.0.2 First Modrinth release]]"
  - "[[v1.3.0.3 Tags faded, controls beside the book]]"
created: 2026-11-17
updated: 2026-11-17
---

# 05 Summaries

What shipped in each version. `CHANGELOG.md` in the repository stays the line-by-line source; these notes add context and follow-ups.

%% Begin Waypoint %%
%% End Waypoint %%

## Key documents

- [[v1.3.0.3 Tags faded, controls beside the book]]: image tags hidden in the reader and faded in the editor; the image controls moved to a panel beside the book, clear of Stendhal's toolbar; not yet verified in game
- [[v1.3.0.2 First Modrinth release]]: mod unchanged; fixes the CI Mixin check that stopped 1.3.0.1; the first version on Modrinth, released 2026-11-17
- [[v1.3.0.1 Modrinth releases from CI]]: mod unchanged; releases published to Modrinth from a version tag by `tools/publish_modrinth.py`; never released (archived)
- [[v1.3.0.0 First release of the fork]]: Stendhal crash fix, download timeouts and backoff, single Fabric 26.3 target

## Status

- Covered: 1.3.0.0, the first release of the fork; 1.3.0.1, which added publishing from CI but was never released; 1.3.0.2, the first version on Modrinth, released 2026-11-17; 1.3.0.3, the fixes from the owner's in-game test of 1.3.0.2 with Stendhal, waiting for its own in-game check before release.
- Upstream history before the fork (1.2.9 and earlier) is summarised only as the starting point inside the 1.3.0.0 note.

## All notes

```dataview
TABLE WITHOUT ID file.link AS "Note", summary AS "Summary", updated AS "Updated"
FROM "05 Summaries"
WHERE file.name != this.file.name
SORT file.name DESC
```
