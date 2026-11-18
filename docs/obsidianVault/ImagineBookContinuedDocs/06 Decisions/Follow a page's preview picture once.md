---
type: decision
status: active
priority: high
tags: [adr, downloader, links]
aliases: [Page previews, og:image follow]
summary: "When a download is not a picture by its first bytes, the downloader follows the page's preview picture (og:image, og:image:secure_url, og:image:url, twitter:image) once, http and https only, under the same 8 MB limit and timeouts, and caches it under the page's address. Rejected: resolving post links at paste time in the editor."
related:
  - "[[v1.3.2.0 clovi.ru and page links]]"
  - "[[Store link codes, not pictures, in book text]]"
  - "[[Module Layout]]"
created: 2026-11-17
updated: 2026-11-18
---

# Follow a page's preview picture once

## Status

Accepted in v1.3.2.0. In force. Checked live on 2026-11-17: with the mod's own classes against a clovi post, and `PagePreview` against a single-picture Imgur page. No Imgur album or gallery page was fetched. Checked in game by the owner with 1.3.2.0 on 2026-11-18, who reported that it works; it reaches Modrinth with 1.3.2.1.

## Context

- On 2026-11-17 the owner asked for clovi.ru support with a post link, https://clovi.ru/post/12207. A post is an HTML page of 7.5 KB. The picture is at the address the page names in `og:image`, here the relative `/f/YwUDL2J`, which serves a 29.5 KB JPEG.
- Players copy the address the browser shows, and on many sites that is a page, not the picture: a clovi post, an Imgur page. Until 1.3.2.0 such a link always gave the error picture, because `AsyncImageDownloader` handed the HTML to `ImageIO`, which cannot decode it.
- The book stores a code, not the address ([[Store link codes, not pictures, in book text]]), and every reader downloads the picture on their own client. Whatever turns a page into a picture has to work for every reader.

## Decision

The downloader, not the editor, turns a page into its picture: a download that is not a picture is followed to the page's preview picture, once.

- `resolvers/PagePreview.looksLikeImage` decides by the first bytes: PNG, JPEG, GIF, WebP or BMP is a picture, anything else is treated as a page.
- `PagePreview.find` reads the page's `<meta>` tags and takes the first `http` or `https` address among `og:image`, then `og:image:secure_url`, `og:image:url` and `twitter:image`, a relative address resolved against the page.
- `AsyncImageDownloader.downloadThread` opens that address with the same 10 s connect and 15 s read timeouts and User-Agent, and reads it through `readLimited` under the same 8 MB limit. The result is not checked for a page again: one hop.
- The picture is converted to PNG and cached under the page's address (`<game dir>/imaginebook/<host>/<path hash>.cache`). Failures and the size limit count against the page's address, so `RetryPolicy` and the size error apply as before.
- The rule is generic. It applies to every link part that resolves to a web address: a `clovi:post/` code, a `u:` code and a plain `http(s)` link.

## Consequences

### Positive

- clovi posts work without clovi-specific download code: `clovi:post/<number>` only decodes to the post's address.
- Other sites that publish a preview picture work the same way. For Imgur, 1.3.2.0 also stopped the editor from rewriting album and gallery links into dead `i.imgur.com/<rest>.png` addresses: `LinkCodes.directImgurLink` converts only a single-picture page, so albums and galleries stay page links and reach this step ([[v1.3.2.0 clovi.ru and page links#Imgur albums and galleries]]).
- Verified for Imgur only on a single-picture page: https://imgur.com/MK3eW3A names `twitter:image` https://i.imgur.com/MK3eW3Ah.jpg, which `PagePreview` finds. Whether album and gallery pages carry a preview tag is unverified; it is an open check in the 1.3.2.0 summary.
- The wire format does not change: the book stores the code of the link the player entered, and books already written are unaffected.
- A link straight to a picture costs nothing extra: the check reads bytes already downloaded.

### Trade-offs accepted

- **One extra small request per page per reader until cached.** Each reader's client fetches the page (7.5 KB for a clovi post) before the picture, the first time only; after that the cached file is used, across restarts too.
- **Only `http` and `https`.** A preview naming `javascript:`, `file:`, `data:` or another scheme is skipped and the next key is tried; when none is left the result is the error picture.
- **One hop.** If the preview address serves another page, the download fails with the error picture. No chains through pages and no loops.
- A page over 8 MB is refused as a picture would be, before any preview is looked for.
- Meta tags are found with a regex, not an HTML parser: enough for the head of a served page, blind to a preview added by JavaScript.
- The picture shown is the one the site chose for link previews.
- Pinned by `PagePreviewTest` (7 tests): the real clovi post head, attribute order and entities, the `twitter:image` fallback, refused schemes, pages without a preview, picture signatures, and a source check that the downloader reads the preview once through `readLimited`.

## Alternatives considered

- **Resolve the post at paste time in the editor.** When the player enters a clovi post link, fetch the page in the editor and store the file code `clovi:<id>` instead. Not chosen: the URL field would need asynchronous UI state (waiting, failed, retry) it does not have, and when the fetch fails the book still ends up with an unresolved post link that readers must handle anyway. It would also cover only links entered in the URL field, not page links typed into the page text.

## References

- Version: 1.3.2.0, `CHANGELOG.md` ("Links to a web page instead of a picture now work")
- Repository files: `src/main/java/io/github/jumperonjava/imaginebook/resolvers/PagePreview.java`, `resolvers/AsyncImageDownloader.java` (`downloadThread`, `readLimited`), `util/LinkCodes.java` (`directImgurLink`); test `src/test/java/io/github/jumperonjava/imaginebook/resolvers/PagePreviewTest.java`
- Related notes: [[v1.3.2.0 clovi.ru and page links]], [[Store link codes, not pictures, in book text]], [[Module Layout]]
