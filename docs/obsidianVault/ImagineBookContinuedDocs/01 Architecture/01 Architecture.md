---
type: index
status: active
priority: high
tags: [architecture, index]
aliases: []
summary: "How ImagineBook Continued is built: the package and Mixin layout, and the Stendhal compatibility design."
related:
  - "[[Vault Map]]"
  - "[[Module Layout]]"
  - "[[Stendhal Compatibility]]"
  - "[[Follow a page's preview picture once]]"
created: 2026-11-17
updated: 2026-11-18
---

# 01 Architecture

How the mod is put together, and why its editor hooks sit where they do.

%% Begin Waypoint %%
%% End Waypoint %%

## Key documents

- [[Module Layout]]: every package and class under `io.github.jumperonjava.imaginebook`, the tag pattern, wire format and link codes (clovi included), the downloader's page-preview step, new-picture fitting, the resolver map, the eight Mixins and their targets, MixinExtras
- [[Stendhal Compatibility]]: the `removeButton` crash, the `Screen.init` hook, null tolerance, the control panel that keeps clear of Stendhal's toolbar, and the checks that pin them

## Active decisions

- [[Hook Screen.init instead of BookEditScreen.init]]
- [[Fade tags in the editor, hide them in the reader]]
- [[Store link codes, not pictures, in book text]]
- [[Follow a page's preview picture once]]
- [[Single Fabric 26.3 target]]

## Status

- Two notes, both active, matching version 1.3.2.1, whose mod code is the same as 1.3.2.0.
- Checked in game: the owner checked 1.3.2.0 on 2026-11-18 and reported that it works. That covers the clovi codes and page previews, the link codes, narrowed tag pattern and fitted new pictures of 1.3.1.0 and 1.3.1.1, and the faded tags and side panel of 1.3.0.3 (see [[v1.3.2.0 clovi.ru and page links]]). Whether Stendhal was installed during that check is not recorded.
- Open question: CI's Mixin smoke runs without Stendhal; only the local `tools/mixin_smoke.py` loads it (see [[Stendhal Compatibility]]).

## All notes

```dataview
TABLE WITHOUT ID file.link AS "Note", summary AS "Summary", status AS "Status", updated AS "Updated"
FROM "01 Architecture"
WHERE file.name != this.file.name
SORT file.name ASC
```
