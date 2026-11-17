---
type: note
status: active
priority: high
tags: [architecture, mixin, fabric]
aliases: [Package layout, Source layout]
summary: "What each package and class under io.github.jumperonjava.imaginebook does: entry point, the tag pattern, wire format and link codes (clovi included), tag parsing and display, image resolvers, the downloader and its page-preview step, the viewer, editor and text box Mixins, new-picture fitting, the control panel layout, utilities."
related:
  - "[[Stendhal Compatibility]]"
  - "[[Fade tags in the editor, hide them in the reader]]"
  - "[[Store link codes, not pictures, in book text]]"
  - "[[Follow a page's preview picture once]]"
  - "[[v1.3.1.1 Bracket text and fitted pictures]]"
  - "[[v1.3.2.0 clovi.ru and page links]]"
  - "[[Build and Test]]"
  - "[[Single Fabric 26.3 target]]"
created: 2026-11-17
updated: 2026-11-17
---

# Module Layout

## Summary

All code lives in one Fabric source set under `src/main/java/io/github/jumperonjava/imaginebook`, the package name kept from upstream. The mod reads image tags from book page text, resolves each tag to a texture (downloaded, cached or from a resource pack) and draws it through Mixins into the vanilla book viewer and book editor. Eight Mixins are registered in `src/main/resources/imaginebook.mixins.json`, all client-side, with `defaultRequire: 1` so an injection that does not apply is an error. Since 1.3.0.3 the tag text is hidden in the viewer and faded in the editor, so it no longer covers the picture. Since 1.3.1.0 the link in a tag is stored as a code, so book text holds no web address. Since 1.3.1.1 bracketed text such as `[1]` is never taken for a tag, and a new picture starts on the page's text area, fitted to it. Since 1.3.2.0 clovi.ru links have their own codes, and a link to a web page shows the page's preview picture.

## Context

The image format is plain page text, so a book stays valid for players without the mod. A tag is `[link,x,y,width%,height%,rotation]`; `x` and `y` are required, one size number sets both width and height, and the link part chooses a resolver by its prefix up to the first colon. `ImageData.getImage()` splits the link at the first colon and hands the rest to the resolver registered for the prefix in `Imaginebook`; an unknown prefix gives the error image. What counts as a tag is set by one regex, see [[#Tag pattern]].

### Tag pattern

One regex, `util/ImageTags.PATTERN`, defines a tag everywhere: parsing (`ImageSerializer`), hiding in the reader (`BookScreenMixin`), fading in the editor (`MultiLineEditBoxMixin`) and rewriting a tag (`NewBookEditScreenMixin` through `ImageTags.replace`). Since 1.3.1.1:

`\[[^,\[\]\s]*?(,[+-]?\d+(\.\d+)?){2,5}\]`

A link part with no comma, no bracket and no whitespace, then two to five numbers. So `[note]`, `[a,b]`, `[x,1]` and `[1]` stay plain text, and a tag never runs across lines.

From the original mod through 1.3.1.0 the link part excluded only commas: `\[[^,]*?(,[+-]?\d+(\.\d+)?){2,5}\]`. Bracketed text before a tag then became part of it: `[1] and [note]: [https://...,1,2]` was one tag whose link began at `1]`, so a picture after bracketed text on the same line never showed, and a tag could run across lines. Tags written by any version with a real link still parse, and link codes contain none of the excluded characters. See [[v1.3.1.1 Bracket text and fitted pictures]].

### Wire format of the link part

The tag and every link form below are read by other players and by other versions; never rename a prefix. See [[Store link codes, not pictures, in book text]].

| Link part | Written by | Resolves to |
|---|---|---|
| `imgur:<id>` | the editor since 1.3.1.0, for an Imgur single-picture link | `https://i.imgur.com/<id>.png` |
| `clovi:<file id>` | the editor since 1.3.2.0, for a clovi.ru file link (`clovi.ru/f/<id>`) | `https://clovi.ru/f/<id>`, the picture itself |
| `clovi:post/<number>` | the editor since 1.3.2.0, for a clovi.ru post link | `https://clovi.ru/post/<number>`, a page; the downloader follows its preview picture |
| `u:<base64url>` | the editor since 1.3.1.0, for any other web link | the decoded `http(s)` address; anything else gives the error image |
| `https://...`, `http://...` | versions before 1.3.1.0, or a tag typed into the page text | the address as written |
| `res:<namespace>:<path>` | a tag typed by the player | a texture from the game or a resource pack |

Codes contain no scheme, no dot and no comma; `clovi:post/` holds the only slash. Versions before 1.3.1.0 have no `imgur` or `u` resolver and show the error image for a code; versions before 1.3.2.0 have no `clovi` resolver. Any form that resolves to a web page instead of a picture (a `clovi:post/` code, a `u:` code such as an Imgur album or gallery link, a plain link) shows the page's preview picture since 1.3.2.0, see `PagePreview` below and [[Follow a page's preview picture once]]. The older hidden binary format (see `ImageSerializer` below) is read by the viewer only.

## Details

### Root package

| Class | Role |
|---|---|
| `Imaginebook` | Shared state. `init()` creates the cache folder `<game dir>/imaginebook` and registers the render pipeline. Holds the resolver map by prefix: `http` and `https` (`UrlResolver`), `res` (`TextureResolver`), `imgur` and `u` (`CodedLinkResolver`, since 1.3.1.0), `clovi` (`CodedLinkResolver`, since 1.3.2.0); an unknown prefix gives the error image. `fixImgurLink` calls `LinkCodes.directImgurLink` (since 1.3.2.0), which turns only a single-picture page `https://imgur.com/<id>` into `https://i.imgur.com/<id>.png`; the editor passes its result on to `LinkCodes.encode`. Before 1.3.2.0 it rewrote every `https://imgur.com/<rest>`, so an album or gallery link became a dead `i.imgur.com/<rest>.png`. |
| `ImageSerializer` | `parseSafeModeImages(page)` finds tags with `ImageTags.PATTERN` and builds `ImageData`. The binary `serialize`/`deserializeImageMetadata` pair is the legacy format the original mod wrote until commit `28ba12b` (2025-01-17): the page padded with newlines to exactly 1023 characters (`Imaginebook.LENGTH`), its last line Base64 of the link bytes plus x, y, width, height and rotation. Still read by the viewer (`BookScreenMixin`) for old books; the editor neither reads nor writes it. |
| `Config` | Gson file `<config dir>/imaginebook.json`, one field `imgurAccept` (false by default). |

### `image/`

- `ImageData`: URL, `x`, `y`, width and height fractions, rotation. `bookString()` writes the tag back; `renderImage()` translates, rotates around the image centre and scales.
- `Image`: a texture-backed drawable. `BookDrawable`: the drawable interface both images and placeholders implement.

### `resolvers/`

- `Resolver`: `resolve(path)` returns a `BookDrawable`.
- `UrlResolver`: one instance per scheme, all sharing one `AsyncImageDownloader`.
- `TextureResolver`: parses the `res:` path as an `Identifier`; an invalid id gives the error image.
- `CodedLinkResolver`: three instances, `IMGUR`, `CLOVI` and `CODED`, for the `imgur:`, `clovi:` and `u:` link codes (`IMGUR` and `CODED` since 1.3.1.0, `CLOVI` since 1.3.2.0). Puts the prefix back, decodes with `LinkCodes.decode` and hands an `https://` or `http://` result to `UrlResolver.INSTANCE_S` or `INSTANCE`; anything else, a `res:` path, a code inside a code or a malformed clovi code, gives the error image.
- `AsyncImageDownloader`: one daemon thread ("ImagineBook image downloader"). Caches files under `<game dir>/imaginebook/<host>/<path hash>.cache`, decodes with `ImageIO` (WebP through the bundled `webp-imageio`) and re-encodes to PNG. Connect timeout 10 s, read timeout 15 s, a User-Agent header (`open`). Shows placeholder textures while downloading or on error (`downloading.png`, `not_loaded.png`, `size_error.png`, `invalid_url.png`). `readLimited(in, downloadURI)` (since 1.3.2.0) reads any response, picture or page, up to `MAX_SIZE` 8 MB; over it, the address is marked over the limit, `size_error.png` shows, and an `IOException` names the limit (until 1.3.1.1 the log called it 1 MB). Page step in `downloadThread` (since 1.3.2.0): when the bytes are not a picture (`PagePreview.looksLikeImage`), `PagePreview.find` looks for the page's preview address; if there is one, it is opened once and read through `readLimited` under the page's address, and the picture is cached under the page's address. The preview is not checked for a page again, so a second page gives the error image.
- `PagePreview` (since 1.3.2.0): `looksLikeImage(bytes)` is true for PNG, JPEG, GIF, WebP and BMP by their first bytes. `find(html, page)` scans `<meta>` tags with a regex, reads `property` or `name` and `content` in either order and either quote, decodes `&amp;`, `&quot;`, `&#39;`, `&lt;` and `&gt;`, and returns the first `http` or `https` address among `og:image`, `og:image:secure_url`, `og:image:url` and `twitter:image`, in that order, a relative address resolved against the page. Pinned by `PagePreviewTest`; see [[Follow a page's preview picture once]].
- `RetryPolicy`: a failed URL waits 30 s times the number of failures and is given up after 3 failures until the game restarts. Pinned by `RetryPolicyTest`.

### `mixin/`

| Mixin | Target | Role |
|---|---|---|
| `BookScreenMixin` | `BookViewScreen` | The viewer. Parses every page, tags included, on construction and on `setBookAccess`, draws the images at the head of `extractRenderState` so vanilla text and controls draw on top. `imaginebook$hideTags` (`@ModifyExpressionValue` on `BookAccess.getPage(I)` inside `visitText`) hands the viewer the page without its tags, styles kept, so the tag text is not drawn and clicks on styled text match what is shown. |
| `NewBookEditScreenMixin` | `BookEditScreen` | The editor. Parses pages after the constructor, implements `BookEditScreenHooks.imaginebook$afterInit()` which adds the URL field, Add and Remove buttons and the x, y, width, height and rotation controls at the slots `util/ControlPanelLayout` picks, handles paste in `keyPressed` through `BookPaste`, and draws in `extractRenderState`. Every use of its widgets tolerates null. The URL field shows the real link and the book stores its code: the responder writes `LinkCodes.encode(Imaginebook.fixImgurLink(url))` and returns early when the field equals the decoded stored link; `updateFields()` fills the field with `LinkCodes.decode(...)`. A link over 255 characters shows `imaginebook.error.url_too_long`. `mutateImage` rewrites a picture's tag with `ImageTags.replace`, counting image tags only, as the picture list does (since 1.3.1.1; before, it counted every `[..]` on the page). Add image creates a picture at the corner of the page's text area (`ImageFit.PAGE_X`, `PAGE_Y`); entering a link marks it, and `imaginebook$fitLoaded()`, run from the render hook, shrinks it with `ImageFit.percent` once the real picture has loaded (since 1.3.1.1). |
| `MultiLineEditBoxMixin` | `MultiLineEditBox` | Fades image tags in the book editor's text box to about 15 % alpha. Two `@ModifyExpressionValue` hooks in `extractContents` record the line's `StringView.beginIndex()` and `endIndex()` (`StringView` is package-private in 26.3, so a handler cannot take it). Three `@WrapOperation` hooks on `GuiGraphicsExtractor.text` (ordinal 0 the part before the cursor, 1 the part after it, 2 a line without the cursor) draw the line in pieces, tags faded and without shadow. Only while the open screen is a `BookEditScreen`. See [[Fade tags in the editor, hide them in the reader]]. |
| `ScreenInitMixin` | `Screen` | Calls `imaginebook$afterInit()` right after `Screen.init(II)V` and `Screen.rebuildWidgets()V` have called `init()`. See [[Stendhal Compatibility]]. |
| `MinecraftClientMixin` | `net.minecraft.client.gui.Gui` | Despite the name, hooks `setScreen`: keeps the editor open while `Imaginebook.cancelledFinalize` is set. `updateFields()` in the editor never sets the flag at present (its error list is always empty), so this path is dormant. |
| `MultiLineEditBoxAccessor` | `MultiLineEditBox` | Accessor for `textField`. |
| `MultilineTextFieldAccessor` | `MultilineTextField` | Accessors for `selectCursor` and `width`, used by paste. |
| `RenderPipelineAccessor` | `RenderPipelines` | Reads `GUI_TEXTURED_SNIPPET` and invokes `register`. |

### `util/`

- `BookPaste`: plans a paste before the book changes: replaces the selection, splits the result over pages within the character limit and the page width, prefers a word or line boundary, keeps surrogate pairs whole, and refuses (empty result) when the pages run out. Pinned by `BookPasteTest`.
- `ImageTags`: the tag regex ([[#Tag pattern]]) and how tags are shown; the page text always keeps them. `find` returns tag spans; `replace(page, index, replacement)` swaps the index-th image tag, counted as the picture list counts them, and leaves the page unchanged for an index outside the tags (since 1.3.1.1); `strip(Component)` rebuilds a page without its tags, every style kept, and returns the same object when there are none (used by the viewer); `pieces` splits an editor line at tag borders, a text piece after a tag starting with the formatting codes active before it (`activeFormatting`); `faded` sets alpha `0x26`. Pinned by `ImageTagsTest`.
- `LinkCodes`: how a link is written into the book (since 1.3.1.0). `encode` turns an Imgur single-picture link into `imgur:<id>`, a clovi.ru file link `http(s)://[www.]clovi.ru/f/<id>` (4 to 16 letters and digits) into `clovi:<id>` and a post link `http(s)://[www.]clovi.ru/post/<number>` (1 to 12 digits) into `clovi:post/<number>` (both since 1.3.2.0, a trailing slash allowed), and any other `http(s)` link into `u:` plus base64url of its UTF-8 bytes without padding; anything else passes through, so encoding is idempotent. `decode` opens `imgur:<id>` as `https://i.imgur.com/<id>.png` (downloads are decoded by content, so the extension does not matter), `clovi:<id>` as `https://clovi.ru/f/<id>`, `clovi:post/<number>` as `https://clovi.ru/post/<number>` and `u:` as the exact link; a code that does not decode comes back unchanged. `directImgurLink` (since 1.3.2.0, called by `Imaginebook.fixImgurLink` before `encode`) turns a single-picture page `http(s)://imgur.com/<id>` (5 to 10 letters and digits, an optional trailing slash) into `https://i.imgur.com/<id>.png` and returns any other link unchanged, so Imgur albums (`imgur.com/a/...`) and galleries (`imgur.com/gallery/...`) stay page links, stored as `u:` codes and shown through the page-preview step. `isWebLink` checks for `http://` or `https://`. Pinned by `LinkCodesTest`; see [[Store link codes, not pictures, in book text]].
- `ImageFit`: where a new picture starts and how big it may get (since 1.3.1.1). `PAGE_X` 36 and `PAGE_Y` 30 are `BookViewScreen.PAGE_TEXT_X_OFFSET` and `PAGE_TEXT_Y_OFFSET`, `PAGE_WIDTH` 114 and `PAGE_HEIGHT` 126 are `BookEditScreen.TEXT_WIDTH` and `TEXT_HEIGHT`, all from 26.3. `percent(w, h)` gives the largest whole percent, 1 to 100, at which a picture of that pixel size fits the area; 100 % is the picture's own pixel size. Pictures already in books are never refitted. Pinned by `ImageFitTest`.
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
- `src/test/`: JUnit 5 tests: `BookPasteTest`, `RetryPolicyTest`, `PagePreviewTest`, `ImageTagsTest`, `ImageFitTest`, `LinkCodesTest`, `ControlPanelLayoutTest`, `StendhalCompatSourceTest`, `CiWorkflowTest`, `VersionDriftTest`.
- `src/main/resources/assets/imaginebook/`: icon, placeholder textures, `en_us`, `ru_ru` and `uk_ua` language files.

## Links

- [[Stendhal Compatibility]]
- [[Fade tags in the editor, hide them in the reader]]
- [[Store link codes, not pictures, in book text]]
- [[Follow a page's preview picture once]]
- [[v1.3.1.0 Link codes instead of web addresses]]
- [[v1.3.1.1 Bracket text and fitted pictures]]
- [[v1.3.2.0 clovi.ru and page links]]
- [[Build and Test]]
- [[06 Decisions]]
