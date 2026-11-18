---
type: decision
status: active
priority: high
tags: [adr, mixin, editor, reader, stendhal]
aliases: [Faded image tags, Hidden image tags]
summary: "Image tags stay in the page text. The reader strips them from the page it shows, styles kept; the book and quill draws them at about 15 % alpha, so they no longer cover the picture but the cursor can still find them."
related:
  - "[[Module Layout]]"
  - "[[Stendhal Compatibility]]"
  - "[[v1.3.0.3 Tags faded, controls beside the book]]"
created: 2026-11-17
updated: 2026-11-18
---

# Fade tags in the editor, hide them in the reader

## Status

Accepted in v1.3.0.3. In force. Covered by the owner's in-game check of 1.3.2.0 on 2026-11-18, which carries this change; he reported that it works. Whether Stendhal was installed during that check is not recorded.

## Context

- An image is a text tag in the page, `[url,x,y,width%,height%,rotation]`. Other players and older versions read the tag, so it has to stay in the page.
- Since the 26.x port the images are drawn under the text (`BookScreenMixin` draws them at the head of `extractRenderState`), so the tag text was drawn over its own picture, in the book and quill and in the signed book alike. The old 1.20 to 1.21 code drew editor text at 25 % alpha.
- The owner found this in game on 2026-11-17, testing 1.3.0.2 with Stendhal 1.4.9.
- Stendhal handles the page text itself: it assigns the page field and copies page content, and its formatting codes live in the text.

## Decision

Change how a tag is shown, never the page text: hide tags in the reader, fade them in the editor.

- Reader: `BookScreenMixin.imaginebook$hideTags`, a `@ModifyExpressionValue` on `BookAccess.getPage(I)` inside `BookViewScreen.visitText`, returns `ImageTags.strip(page)`: the page component rebuilt without its tags, every style kept. `visitText` splits and caches the lines from it, so clicks on styled text match what is shown. Images are still parsed from the full page.
- Editor: `MultiLineEditBoxMixin` wraps the three `GuiGraphicsExtractor.text` calls in `MultiLineEditBox.extractContents` (ordinals 0, 1, 2) with `@WrapOperation` and draws each line in pieces: a tag piece at `ImageTags.faded(color)`, alpha `0x26` (about 15 %), without shadow; a text piece as before, starting with the formatting codes active before it. Line begin and end come from `StringView.beginIndex()` and `endIndex()`, recorded with `@ModifyExpressionValue`, because `StringView` is package-private in 26.3. Applies only while the open screen is a `BookEditScreen`, so other text boxes are untouched.

## Consequences

### Positive

- The page text and the tag format are unchanged: players without the mod and older versions read the same tags, and nothing is rewritten on save.
- In the editor a tag is still there to see, select and delete whole.
- Nothing touches the page content Stendhal manages, and Stendhal's formatting carries across a tag.
- Pinned by `ImageTagsTest` (`find`, `strip` for strings and components, `pieces`, `activeFormatting`, `faded`); `tools/mixin_smoke.py` applies both Mixins with Stendhal loaded.

### Trade-offs accepted

- In the editor a faded tag keeps its width in the line; in the reader it is gone and the text closes up, so a page can wrap differently in the two.
- The editor hook depends on the shape of `MultiLineEditBox.extractContents` in 26.3: three `text` calls and the `StringView` index calls. `defaultRequire: 1` turns a moved target into a load error, and the smoke check loads `MultiLineEditBox`, so a game update breaks it loudly, not silently.
- Each wrapped `text` call scans the page with the tag regex, in the book editor only.
- MixinExtras is now a compile dependency (`compileOnly`, provided at runtime by Fabric Loader 0.20.1); see [[Module Layout]].

## Alternatives considered

- **Hide tags fully in the editor too.** Not chosen: invisible characters can be half-deleted with Backspace, which breaks a tag without the player seeing it.
- **Strip tags out of the editable text and append them again on save.** Not chosen: it fights Stendhal's own page handling, which assigns the page field and copies page content.

## References

- Version: 1.3.0.3, `CHANGELOG.md` ("Image tags no longer cover the picture")
- Repository files: `src/main/java/io/github/jumperonjava/imaginebook/util/ImageTags.java`, `mixin/MultiLineEditBoxMixin.java`, `mixin/BookScreenMixin.java`, `src/test/java/io/github/jumperonjava/imaginebook/util/ImageTagsTest.java`
- Related notes: [[Module Layout]], [[Stendhal Compatibility]], [[v1.3.0.3 Tags faded, controls beside the book]]
