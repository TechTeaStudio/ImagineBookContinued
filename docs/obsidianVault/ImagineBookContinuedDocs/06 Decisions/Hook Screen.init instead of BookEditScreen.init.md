---
type: decision
status: active
priority: high
tags: [adr, mixin, compatibility, stendhal]
aliases: [ScreenInitMixin decision]
summary: "The editor widgets are added from Screen.init(II)V and Screen.rebuildWidgets()V right after their call to init(), not from BookEditScreen.init(), because Stendhal cancels that method at its head."
related:
  - "[[Stendhal Compatibility]]"
  - "[[Module Layout]]"
  - "[[v1.3.0.0 First release of the fork]]"
created: 2026-11-17
updated: 2026-11-17
---

# Hook Screen.init instead of BookEditScreen.init

## Status

Accepted in v1.3.0.0. In force.

## Context

- ImagineBook added its editor widgets (URL field, Add and Remove, position, size and rotation controls) with an `@Inject` at `TAIL` of `BookEditScreen.init()`.
- Stendhal's `BookEditScreenMixin` injects at `HEAD` of the same method and cancels it to build its own editor. A cancelled method never reaches its tail, so ImagineBook's injection never ran.
- Result: opening a book and quill with Stendhal installed crashed in `extractRenderState` with `Cannot assign field "visible" because "this.removeButton" is null`.

## Decision

Add the editor widgets from the caller of `init()`, not from `init()` itself.

- `mixin/ScreenInitMixin` targets `Screen` and injects into `init(II)V` (first open) and `rebuildWidgets()V` (resize), at the `INVOKE` of `Screen.init()V` with `shift = At.Shift.AFTER`.
- When the screen implements `compat/BookEditScreenHooks` (only `BookEditScreen`, through `NewBookEditScreenMixin`), it calls `imaginebook$afterInit()`, which adds the widgets.
- Every use of the widgets tolerates null, so a screen built in some other way still renders and takes clicks.

## Consequences

### Positive

- Works whoever built the editor, vanilla or Stendhal, and does not depend on the order in which two mods' injectors run.
- First open and resize take the same path, so the widgets are rebuilt after `rebuildWidgets()` clears them.
- Pinned by `StendhalCompatSourceTest`; `tools/mixin_smoke.py` checks the Mixins apply with Stendhal loaded.

### Trade-offs accepted

- A Mixin on `Screen` runs for every screen in the game; the cost is one `instanceof` check per `init()`.
- The widgets are laid out the same way whichever mod built the editor; there is no Stendhal-specific layout.
- Null checks have to be kept on every new widget field.

## Alternatives considered

- **`@Inject` at `HEAD` of `BookEditScreen.init()` with a Mixin priority that runs before Stendhal.** Not chosen: Stendhal cancels the method, so the result would hang on the ordering of two mods' injectors in that one method, and would break again if Stendhal or another editor mod changed its own injection.
- **Requiring a fixed order with Stendhal.** Not chosen for the same reason: any code inside `BookEditScreen.init()` is at the mercy of a mod that cancels it.

## References

- Version: 1.3.0.0, first entry in `CHANGELOG.md`
- Repository files: `src/main/java/io/github/jumperonjava/imaginebook/mixin/ScreenInitMixin.java`, `compat/BookEditScreenHooks.java`, `mixin/NewBookEditScreenMixin.java`, `src/test/java/io/github/jumperonjava/imaginebook/StendhalCompatSourceTest.java`, `tools/mixin_smoke.py`
- Related notes: [[Stendhal Compatibility]]
