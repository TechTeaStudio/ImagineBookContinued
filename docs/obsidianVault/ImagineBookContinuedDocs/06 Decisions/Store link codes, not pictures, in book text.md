---
type: decision
status: active
priority: high
tags: [adr, wire-format, links, servers]
aliases: [Link codes, imgur codes, Coded links]
summary: "Books keep visible image tags, but the link part is a code with no web address in it: imgur:<id> for Imgur pictures, u:<base64url> for any other link. Storing the picture itself in the book was rejected (a book holds about 75 KB, a picture is 100 to 500 KB). Versions before 1.3.1.0 cannot read codes."
related:
  - "[[v1.3.1.0 Link codes instead of web addresses]]"
  - "[[Module Layout]]"
  - "[[Fade tags in the editor, hide them in the reader]]"
created: 2026-11-17
updated: 2026-11-17
---

# Store link codes, not pictures, in book text

## Status

Accepted in v1.3.1.0. In force. Not yet verified in game.

## Context

- On 2026-11-17 the owner asked whether the book could store the picture itself instead of the link, because some servers block web links in book text.
- Since the visible tags of the original mod (commit `28ba12b` "text images", 2025-01-17) an image is a tag in plain page text, `[url,x,y,width%,height%,rotation]`, so a full `https://...` address sits in the book and a server link filter sees it.
- A book and quill holds at most 100 pages of 1024 characters (`WritableBookContent.MAX_PAGES` and `PAGE_EDIT_LENGTH` in Minecraft 26.3).
- The tag is a wire format: other players read it, and so do older versions of the mod.

## Options

### The picture in the book

100 pages of 1024 characters is 102,400 characters. As base64 (3 bytes per 4 characters) that is 76,800 bytes, about 75 KB, with no room left for any text. An Imgur picture is usually 100 to 500 KB, so a whole book holds less than one typical picture. Paper-based servers limit book size further, and players without the mod would see pages of noise.

### The old hidden binary format

Until 2025-01-17 the original ImagineBook stored images out of sight: the page was padded with newlines to exactly 1023 characters (`Imaginebook.LENGTH`), and its last line was base64 of the link bytes plus x, y, width, height and rotation (`ImageSerializer.serializeImageMetadata`). The link was never visible. Against it:

- The images live only while the page is exactly 1023 characters long (`BookScreenMixin` checks `page.length() == Imaginebook.LENGTH`); any edit that changes the length drops every image on that page.
- The padding uses up the page's text room.
- The link inside is only base64 as well, so it hides from a filter no better than a code does.
- Everything since 28ba12b is built on visible tags: the editor, paste, copying a tag between books, the faded and hidden display of 1.3.0.3.

### Link codes in visible tags

Keep the tag and its format, and change only what the link part holds: a code with no scheme, no domain and no comma.

## Decision

Keep visible tags; store the link as a code, never as a web address.

- `util/LinkCodes.encode` turns an Imgur single-picture link (`i.imgur.com/<id>.<ext>` or `imgur.com/<id>`, an id of 5 to 10 letters and digits) into `imgur:<id>`, and any other `http://` or `https://` link into `u:` plus base64url of its UTF-8 bytes, without padding. Anything else (`res:`, an existing code, an empty string) passes through unchanged, so encoding twice gives the same code.
- `LinkCodes.decode` opens `imgur:<id>` as `https://i.imgur.com/<id>.png`. The extension does not matter: Imgur serves a picture under any extension, and `AsyncImageDownloader` decodes downloads by content with `ImageIO` and re-encodes them to PNG. `u:` is decoded back to the exact link; a code that does not decode comes back unchanged and resolves to the error picture.
- `resolvers/CodedLinkResolver` is registered in the resolver map of `Imaginebook` under `imgur` and `u`. It puts the prefix back, decodes, and hands only an `http://` or `https://` result to `UrlResolver`; a `res:` path or a code inside a code gives the error picture.
- The editor (`NewBookEditScreenMixin`) stores `LinkCodes.encode(Imaginebook.fixImgurLink(url))` from the URL field and shows `LinkCodes.decode(...)` in it, so the player always sees and edits the real link.
- Plain `http(s)` links already in books still resolve through `UrlResolver` as before.

## Consequences

### Positive

- Book text holds no scheme, no dot and no comma from a link (`LinkCodesTest.codesDoNotLookLikeLinks`), so a filter that looks for web addresses has nothing to match.
- The tag format and its regex (`util/ImageTags.PATTERN`) are unchanged: base64url uses letters, digits, `-` and `_`, never a comma. The faded and hidden display of 1.3.0.3 works on codes as it did on links.
- Books with plain links keep working; the reader still reads the pre-2025 binary format.
- An Imgur code is shorter than the link it replaces.
- Pinned by `LinkCodesTest` (9 tests): Imgur forms, round trip of other links (a comma and Cyrillic included), idempotence, broken codes, codes that decode to non-web links, tag parsing, and the editor and resolver map wiring.

### Trade-offs accepted

- **Backward compatibility.** ImagineBook versions before 1.3.1.0, the original mod and other 26.x ports included, have no `imgur` or `u` resolver and show the error picture for a code. A book written with 1.3.1.0 shows its pictures only to players on 1.3.1.0 or later. Older books still open everywhere they did before.
- **Codes are wire format.** Never rename `imgur:` or `u:`, and never change the base64url alphabet.
- A `u:` code is about a third longer than its link: a 255-character ASCII link, the most the URL field takes, becomes a 342-character code out of the page's 1024.
- Players without the mod see a code in the tag and cannot copy the address from the text.
- A filter that blocks the word `imgur` would still catch Imgur codes. No real server filter has been tried yet.

## References

- Version: 1.3.1.0, `CHANGELOG.md` ("Books no longer contain web addresses")
- History: commit `28ba12b` "text images" (2025-01-17), the switch from the hidden binary format to visible tags
- Repository files: `src/main/java/io/github/jumperonjava/imaginebook/util/LinkCodes.java`, `resolvers/CodedLinkResolver.java`, `Imaginebook.java`, `mixin/NewBookEditScreenMixin.java`, `ImageSerializer.java`, `src/test/java/io/github/jumperonjava/imaginebook/util/LinkCodesTest.java`
- Related notes: [[v1.3.1.0 Link codes instead of web addresses]], [[Module Layout]], [[Fade tags in the editor, hide them in the reader]]
