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
  - "[[v1.3.1.0 Link codes instead of web addresses]]"
  - "[[v1.3.1.1 Bracket text and fitted pictures]]"
  - "[[v1.3.2.0 clovi.ru and page links]]"
  - "[[v1.3.2.1 Release notes since the last upload]]"
created: 2026-11-17
updated: 2026-11-18
---

# 05 Summaries

What shipped in each version. `CHANGELOG.md` in the repository stays the line-by-line source; these notes add context and follow-ups.

%% Begin Waypoint %%
%% End Waypoint %%

## Key documents

- [[v1.3.2.1 Release notes since the last upload]]: mod unchanged from 1.3.2.0; the release notes on Modrinth hold the `CHANGELOG.md` sections of every version since the last upload, so the never-uploaded 1.3.1.0, 1.3.1.1 and 1.3.2.0 reach players; `--dry-run --since` previews them; shorter Modrinth page text; released with the tag `v1.3.2.1` on 2026-11-18
- [[v1.3.2.0 clovi.ru and page links]]: clovi.ru links stored as `clovi:<file id>` or `clovi:post/<number>`; a link to a web page shows the page's preview picture (`og:image`, else `twitter:image`), followed once under the same limits and cached under the page's address; Imgur album and gallery links stay page links instead of dead `i.imgur.com` addresses; carries the fixes of 1.3.1.1 and the link codes of 1.3.1.0; checked in game by the owner on 2026-11-18; committed as `ef44923`, never uploaded on its own, ships as 1.3.2.1
- [[v1.3.1.1 Bracket text and fitted pictures]]: bracketed text such as `[1]` no longer breaks pictures (tags counted by `ImageTags`, link part without commas, brackets or whitespace); new pictures start on the page text area and shrink to fit it; the size-limit log says 8 MB; committed as `584def6`, never released, superseded by 1.3.2.0 (archived)
- [[v1.3.1.0 Link codes instead of web addresses]]: books store `imgur:<id>` or `u:<base64url>` instead of the web address, so servers that filter links accept them; versions before 1.3.1.0 cannot read the codes; committed as `a5bc93c`, never released, superseded by 1.3.1.1 (archived)
- [[v1.3.0.3 Tags faded, controls beside the book]]: image tags hidden in the reader and faded in the editor; the image controls moved to a panel beside the book, clear of Stendhal's toolbar; tagged `v1.3.0.3` on 2026-11-17
- [[v1.3.0.2 First Modrinth release]]: mod unchanged; fixes the CI Mixin check that stopped 1.3.0.1; the first version on Modrinth, released 2026-11-17
- [[v1.3.0.1 Modrinth releases from CI]]: mod unchanged; releases published to Modrinth from a version tag by `tools/publish_modrinth.py`; never released (archived)
- [[v1.3.0.0 First release of the fork]]: Stendhal crash fix, download timeouts and backoff, single Fabric 26.3 target

## Status

- Covered: 1.3.0.0, the first release of the fork; 1.3.0.1, which added publishing from CI but was never released; 1.3.0.2, the first version on Modrinth, released 2026-11-17; 1.3.0.3, the fixes from the owner's in-game test of 1.3.0.2 with Stendhal, pushed and tagged `v1.3.0.3` on commit `b059d7b` on 2026-11-17; 1.3.1.0, link codes instead of web addresses, committed as `a5bc93c` and superseded before release; 1.3.1.1, fixes for bracketed text and fitted new pictures on top of the link codes, committed as `584def6` and superseded before release; 1.3.2.0, clovi.ru codes and page links on top of both, checked in game by the owner on 2026-11-18 and committed as `ef44923`, never uploaded on its own; 1.3.2.1, the current version, the same mod code with release notes that cover every version since the last upload (1.3.0.3), released with the tag `v1.3.2.1` on 2026-11-18.
- Upstream history before the fork (1.2.9 and earlier) is summarised only as the starting point inside the 1.3.0.0 note.

## All notes

```dataview
TABLE WITHOUT ID file.link AS "Note", summary AS "Summary", updated AS "Updated"
FROM "05 Summaries"
WHERE file.name != this.file.name
SORT file.name DESC
```
