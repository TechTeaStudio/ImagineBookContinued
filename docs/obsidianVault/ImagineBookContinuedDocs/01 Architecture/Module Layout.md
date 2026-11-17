---
type: note
status: active
priority: high
tags: [architecture, mixin, fabric]
aliases: [Package layout, Source layout]
summary: "What each package and class under io.github.jumperonjava.imaginebook does: entry point, the tag wire format and link codes, tag parsing and display, image resolvers and downloader, the viewer, editor and text box Mixins, the control panel layout, utilities."
related:
  - "[[Stendhal Compatibility]]"
  - "[[Fade tags in the editor, hide them in the reader]]"
  - "[[Store link codes, not pictures, in book text]]"
  - "[[Build and Test]]"
  - "[[Single Fabric 26.3 target]]"
created: 2026-11-17
updated: 2026-11-17
---

# Module Layout

## Summary

All code lives in one Fabric source set under `src/main/java/io/github/jumperonjava/imaginebook`, the package name kept from upstream. The mod reads image tags from book page text, resolves each tag to a texture (downloaded, cached or from a resource pack) and draws it through Mixins into the vanilla book viewer and book editor. Eight Mixins are registered in `src/main/resources/imaginebook.mixins.json`, all client-side, with `defaultRequire: 1` so an injection that does not apply is an error. Since 1.3.0.3 the tag text is hidden in the viewer and faded in the editor, so it no longer covers the picture. Since 1.3.1.0 the link in a tag is stored as a code, so book text holds no web address.

## Context

The image format is plain page text, so a book stays valid for players without the mod. A tag is `[link,x,y,width%,height%,rotation]`; `x` and `y` are required, one size number sets both width and height, and the link part chooses a resolver by its prefix up to the first colon. `ImageData.getImage()` splits the link at the first colon and hands the rest to the resolver registered for the prefix in `Imaginebook`; an unknown prefix gives the error image. One regex, `util/ImageTags.PATTERN`, defines a tag for both parsing and display: a link without commas, then two to five numbers, so `[note]`, `[a,b]` and `[x,1]` stay plain text.

### Wire format of the link part

The tag and every link form below are read by other players and by other versions; never rename a prefix. See [[Store link codes, not pictures, in book text]].

| Link part | Written by | Resolves to |
|---|---|---|
| `imgur:<id>` | the editor since 1.3.1.0, for an Imgur single-picture link | `https://i.imgur.com/<id>.png` |
| `u:<base64url>` | the editor since 1.3.1.0, for any other web link | the decoded `http(s)` address; anything else gives the error image |
| `https://...`, `http://...` | versions before 1.3.1.0, or a tag typed into the page text | the address as written |
| `res:<namespace>:<path>` | a tag typed by the player | a texture from the game or a resource pack |

Codes contain no scheme, no dot and no comma. Versions before 1.3.1.0 have no `imgur` or `u` resolver and show the error image for a code. The older hidden binary format (see `ImageSerializer` below) is read by the viewer only.

## Details

### Root package

| Class | Role |
|---|---|
| `Imaginebook` | Shared state. `init()` creates the cache folder `<game dir>/imaginebook` and registers the render pipeline. Holds the resolver map by prefix: `http` and `https` (`UrlResolver`), `res` (`TextureResolver`), `imgur` and `u` (`CodedLinkResolver`, since 1.3.1.0); an unknown prefix gives the error image. `fixImgurLink` turns `https://imgur.com/<id>` into `https://i.imgur.com/<id>.png`; the editor passes its result on to `LinkCodes.encode`. |
| `ImageSerializer` | `parseSafeModeImages(page)` finds tags with `ImageTags.PATTERN` and builds `ImageData`. The binary `serialize`/`deserializeImageMetadata` pair is the legacy format the original mod wrote until commit `28ba12b` (2025-01-17): the page padded with newlines to exactly 1023 characters (`Imaginebook.LENGTH`), its last line Base64 of the link bytes plus x, y, width, height and rotation. Still read by the viewer (`BookScreenMixin`) for old books; the editor neither reads nor writes it. |
| `Config` | Gson file `<config dir>/imaginebook.json`, one field `imgurAccept` (false by default). |

### `image/`

- `ImageData`: URL, `x`, `y`, width and height fractions, rotation. `bookString()` writes the tag back; `renderImage()` translates, rotates around the image centre and scales.
- `Image`: a texture-backed drawable. `BookDrawable`: the drawable interface both images and placeholders implement.

### `resolvers/`

- `Resolver`: `resolve(path)` returns a `BookDrawable`.
- `UrlResolver`: one instance per scheme, all sharing one `AsyncImageDownloader`.
- `TextureResolver`: parses the `res:` path as an `Identifier`; an invalid id gives the error image.
- `CodedLinkResolver`: two instances, `IMGUR` and `CODED`, for the `imgur:` and `u:` link codes (since 1.3.1.0). Puts the prefix back, decodes with `LinkCodes.decode` and hands an `https://` or `http://` result to `UrlResolver.INSTANCE_S` or `INSTANCE`; anything else, a `res:` path or a code inside a code, gives the error image.
- `AsyncImageDownloader`: one daemon thread ("ImagineBook image downloader"). Caches files under `<game dir>/imaginebook/<host>/<path hash>.cache`, decodes with `ImageIO` (WebP through the bundled `webp-imageio`) and re-encodes to PNG. Connect timeout 10 s, read timeout 15 s, a User-Agent header. Shows placeholder textures while downloading or on error (`downloading.png`, `not_loaded.png`, `size_error.png`, `invalid_url.png`).
- `RetryPolicy`: a failed URL waits 30 s times the number of failures and is given up after 3 failures until the game restarts. Pinned by `RetryPolicyTest`.

### `mixin/`

| Mixin | Target | Role |
|---|---|---|
| `BookScreenMixin` | `BookViewScreen` | The viewer. Parses every page, tags included, on construction and on `setBookAccess`, draws the images at the head of `extractRenderState` so vanilla text and controls draw on top. `imaginebook$hideTags` (`@ModifyExpressionValue` on `BookAccess.getPage(I)` inside `visitText`) hands the viewer the page without its tags, styles kept, so the tag text is not drawn and clicks on styled text match what is shown. |
| `NewBookEditScreenMixin` | `BookEditScreen` | The editor. Parses pages after the constructor, implements `BookEditScreenHooks.imaginebook$afterInit()` which adds the URL field, Add and Remove buttons and the x, y, width, height and rotation controls at the slots `util/ControlPanelLayout` picks, handles paste in `keyPressed` through `BookPaste`, and draws in `extractRenderState`. Every use of its widgets tolerates null. The URL field shows the real link and the book stores its code: the responder writes `LinkCodes.encode(Imaginebook.fixImgurLink(url))` and returns early when the field equals the decoded stored link; `updateFields()` fills the field with `LinkCodes.decode(...)`. A link over 255 characters shows `imaginebook.error.url_too_long`. |
| `MultiLineEditBoxMixin` | `MultiLineEditBox` | Fades image tags in the book editor's text box to about 15 % alpha. Two `@ModifyExpressionValue` hooks in `extractContents` record the line's `StringView.beginIndex()` and `endIndex()` (`StringView` is package-private in 26.3, so a handler cannot take it). Three `@WrapOperation` hooks on `GuiGraphicsExtractor.text` (ordinal 0 the part before the cursor, 1 the part after it, 2 a line without the cursor) draw the line in pieces, tags faded and without shadow. Only while the open screen is a `BookEditScreen`. See [[Fade tags in the editor, hide them in the reader]]. |
| `ScreenInitMixin` | `Screen` | Calls `imaginebook$afterInit()` right after `Screen.init(II)V` and `Screen.rebuildWidgets()V` have called `init()`. See [[Stendhal Compatibility]]. |
| `MinecraftClientMixin` | `net.minecraft.client.gui.Gui` | Despite the name, hooks `setScreen`: keeps the editor open while `Imaginebook.cancelledFinalize` is set. `updateFields()` in the editor never sets the flag at present (its error list is always empty), so this path is dormant. |
| `MultiLineEditBoxAccessor` | `MultiLineEditBox` | Accessor for `textField`. |
| `MultilineTextFieldAccessor` | `MultilineTextField` | Accessors for `selectCursor` and `width`, used by paste. |
| `RenderPipelineAccessor` | `RenderPipelines` | Reads `GUI_TEXTURED_SNIPPET` and invokes `register`. |

### `util/`

- `BookPaste`: plans a paste before the book changes: replaces the selection, splits the result over pages within the character limit and the page width, prefers a word or line boundary, keeps surrogate pairs whole, and refuses (empty result) when the pages run out. Pinned by `BookPasteTest`.
- `ImageTags`: the tag regex and how tags are shown; the page text always keeps them. `find` returns tag spans; `strip(Component)` rebuilds a page without its tags, every style kept, and returns the same object when there are none (used by the viewer); `pieces` splits an editor line at tag borders, a text piece after a tag starting with the formatting codes active before it (`activeFormatting`); `faded` sets alpha `0x26`. Pinned by `ImageTagsTest`.
- `LinkCodes`: how a link is written into the book (since 1.3.1.0). `encode` turns an Imgur single-picture link into `imgur:<id>` and any other `http(s)` link into `u:` plus base64url of its UTF-8 bytes without padding; anything else passes through, so encoding is idempotent. `decode` opens `imgur:<id>` as `https://i.imgur.com/<id>.png` (downloads are decoded by content, so the extension does not matter) and `u:` as the exact link; a code that does not decode comes back unchanged. `isWebLink` checks for `http://` or `https://`. Pinned by `LinkCodesTest`; see [[Store link codes, not pictures, in book text]].
- `ControlPanelLayout`: where the image controls go on the editor. A 5-row panel up to 200 px wide in the first free spot left of the book, then right, clear of every widget on the screen and of the bottom 54 px where Stendhal's toolbar sits; the old two bottom rows only when no spot fits. Pinned by `ControlPanelLayoutTest`; see [[Stendhal Compatibility]].
- `ImageRenderPipeline`: registers `imaginebook:pipeline/gui_textured_nocull`, the GUI textured pipeline without back-face culling, so mirrored images still draw. The Mixin smoke mod asserts this.
- `VersionFunctions`: thin wrappers over the GUI pose stack and texture drawing.
- `DeletedImageData` (writes an empty tag, used by Remove) and `FileReadWrite` (text file read and write, used by `Config`).

### `compat/` and `platform/`

- `compat/BookEditScreenHooks`: the one-method interface between `ScreenInitMixin` and the editor Mixin.
- `platform/PlatformEntrypoint`: the Fabric `client` entrypoint, calls `Imaginebook.init()`. `platform/PlatformPaths`: game and config folders from `FabricLoader`.

### MixinExtras

`BookScreenMixin` and `MultiLineEditBoxMixin` use MixinExtras injectors (`@ModifyExpressionValue`, `@WrapOperation`). MixinExtras 0.5.5 comes nested in Fabric Loader 0.20.1 (`META-INF/jars/mixinextras-fabric-0.5.5.jar`), which provides it at runtime, so the mod jar does not bundle it. `build.gradle` has it `compileOnly`; `tools/build_local.py` extracts it from the loader jar into `tools/.cache/` for the local build (`MIXINEXTRAS`). Keep both versions in step with the loader.

### Outside `src/main`

- `src/smoke/`: a development-only mod for the windowless Mixin check, never shipped (see [[Build and Test]]).
- `src/test/`: JUnit 5 tests: `BookPasteTest`, `RetryPolicyTest`, `ImageTagsTest`, `LinkCodesTest`, `ControlPanelLayoutTest`, `StendhalCompatSourceTest`, `CiWorkflowTest`, `VersionDriftTest`.
- `src/main/resources/assets/imaginebook/`: icon, placeholder textures, `en_us`, `ru_ru` and `uk_ua` language files.

## Links

- [[Stendhal Compatibility]]
- [[Fade tags in the editor, hide them in the reader]]
- [[Store link codes, not pictures, in book text]]
- [[v1.3.1.0 Link codes instead of web addresses]]
- [[Build and Test]]
- [[06 Decisions]]
