---
type: index
status: active
priority: high
tags: [adr, decisions, index]
aliases: []
summary: "ADR-style decision records for ImagineBook Continued, one decision per note."
related:
  - "[[Vault Map]]"
  - "[[Module Layout]]"
created: 2026-11-17
updated: 2026-11-17
---

# 06 Decisions

One decision per note. A replaced decision stays: it gets `status: deprecated`, and the new note links back to it.

%% Begin Waypoint %%
%% End Waypoint %%

## Key documents

- [[Hook Screen.init instead of BookEditScreen.init]]: editor widgets added after `Screen` calls `init()`, because Stendhal cancels `BookEditScreen.init()`
- [[Single Fabric 26.3 target]]: one build, Fabric for Minecraft 26.3; other targets dropped, kept in git history
- [[Private repository, sources jar on every upload]]: LGPL-3.0 compliance while the repository is private

## Status

- Three decisions, all accepted and in force at version 1.3.0.0.
- Nothing superseded yet. Revisit the sources jar decision if the repository goes public.

## Decisions in force

```dataview
TABLE WITHOUT ID file.link AS "Decision", summary AS "Summary", updated AS "Updated"
FROM "06 Decisions"
WHERE file.name != this.file.name AND type = "decision" AND status = "active"
SORT file.name ASC
```

## Superseded and archived

```dataview
TABLE WITHOUT ID file.link AS "Decision", summary AS "Summary", updated AS "Updated"
FROM "06 Decisions"
WHERE file.name != this.file.name AND (status = "deprecated" OR status = "archived")
SORT updated DESC
```
