package io.github.jumperonjava.imaginebook.resolvers;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PagePreviewTest {
    private static final URI CLOVI_POST = URI.create("https://clovi.ru/post/12207");

    /** The head of https://clovi.ru/post/12207 as served on 2026-11-17. */
    private static final String CLOVI_HTML = """
            <title>мем - Clovi</title>
            <meta property="og:title" content="мем" />
            <meta property="og:type" content="website" />
            <meta property="og:image:secure_url" content="/f/YwUDL2J"/>
            <meta property="og:image" content="/f/YwUDL2J" />
            <meta property="og:image:type" content="image/png">
            """;

    @Test
    void cloviPostLeadsToItsFile() {
        assertEquals(Optional.of(URI.create("https://clovi.ru/f/YwUDL2J")), PagePreview.find(CLOVI_HTML, CLOVI_POST));
    }

    @Test
    void attributesInAnyOrderAndEntitiesDecoded() {
        String html = "<meta content='https://cdn.example.org/a.png?x=1&amp;y=2' property='og:image'>";
        assertEquals(Optional.of(URI.create("https://cdn.example.org/a.png?x=1&y=2")), PagePreview.find(html, CLOVI_POST));
    }

    @Test
    void twitterImageIsTheFallback() {
        String html = "<meta name=\"twitter:image\" content=\"https://example.org/t.jpg\">";
        assertEquals(Optional.of(URI.create("https://example.org/t.jpg")), PagePreview.find(html, CLOVI_POST));
    }

    @Test
    void onlyWebAddressesAreFollowed() {
        assertTrue(PagePreview.find("<meta property=\"og:image\" content=\"javascript:alert(1)\">", CLOVI_POST).isEmpty());
        assertTrue(PagePreview.find("<meta property=\"og:image\" content=\"file:///C:/x.png\">", CLOVI_POST).isEmpty());
        assertTrue(PagePreview.find("<meta property=\"og:image\" content=\"data:image/png;base64,AAAA\">", CLOVI_POST).isEmpty());
    }

    @Test
    void pageWithoutPreviewGivesNothing() {
        assertTrue(PagePreview.find("<html><body>no preview</body></html>", CLOVI_POST).isEmpty());
        assertTrue(PagePreview.find("<meta property=\"og:image\" content=\"\">", CLOVI_POST).isEmpty());
    }

    @Test
    void picturesAreRecognisedByTheirFirstBytes() {
        assertTrue(PagePreview.looksLikeImage(new byte[]{(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10}));
        assertTrue(PagePreview.looksLikeImage(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}));
        assertTrue(PagePreview.looksLikeImage("GIF89a....".getBytes()));
        assertTrue(PagePreview.looksLikeImage("RIFF\0\0\0\0WEBPVP8 ".getBytes()));
        assertTrue(PagePreview.looksLikeImage("BM......".getBytes()));
        assertFalse(PagePreview.looksLikeImage("<!DOCTYPE html><html>".getBytes()));
        assertFalse(PagePreview.looksLikeImage(new byte[0]));
    }

    @Test
    void downloaderFollowsAPagePreviewOnce() throws IOException {
        String downloader = Files.readString(Path.of("src/main/java/io/github/jumperonjava/imaginebook/resolvers/AsyncImageDownloader.java"));
        assertTrue(downloader.contains("PagePreview.looksLikeImage(bytes)"));
        assertTrue(downloader.contains("PagePreview.find("));
        assertTrue(downloader.contains("bytes = readLimited(preview, downloadURI);"), "one hop, same size limit");
    }
}
