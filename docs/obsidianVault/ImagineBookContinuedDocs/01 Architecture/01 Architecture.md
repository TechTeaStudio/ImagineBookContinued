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
created: 2026-11-17
updated: 2026-11-17
---

# 01 Architecture

How the mod is put together, and why its editor hooks sit where they do.

%% Begin Waypoint %%
%% End Waypoint %%

## Key documents

- [[Module Layout]]: every package and class under `io.github.jumperonjava.imaginebook`, the eight Mixins and their targets, MixinExtras
- [[Stendhal Compatibility]]: the `removeButton` crash, the `Screen.init` hook, null tolerance, the control panel that keeps clear of Stendhal's toolbar, and the checks that pin them

## Active decisions

- [[Hook Screen.init instead of BookEditScreen.init]]
- [[Fade tags in the editor, hide them in the reader]]
- [[Single Fabric 26.3 target]]

## Status

- Two notes, both active, matching version 1.3.0.3.
- Open question: the faded tags and the side panel of 1.3.0.3 are not yet verified in game (see [[v1.3.0.3 Tags faded, controls beside the book]]).
- Open question: CI's Mixin smoke runs without Stendhal; only the local `tools/mixin_smoke.py` loads it (see [[Stendhal Compatibility]]).

## All notes

```dataview
TABLE WITHOUT ID file.link AS "Note", summary AS "Summary", status AS "Status", updated AS "Updated"
FROM "01 Architecture"
WHERE file.name != this.file.name
SORT file.name ASC
```
