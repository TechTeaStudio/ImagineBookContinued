package io.github.jumperonjava.imaginebook.util;

import io.github.jumperonjava.imaginebook.ImageSerializer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkCodesTest {
    private static final List<String> LINKS = List.of(
            "https://i.imgur.com/MK3eW3A.jpg",
            "https://cdn.example.org/pics/castle.png?size=large&v=2",
            "http://example.com/a,b.png",
            "https://пример.рф/картинка.png",
            "https://clovi.ru/f/YwUDL2J",
            "https://clovi.ru/post/12207");

    @Test
    void cloviLinksBecomeShortCodes() {
        assertEquals("clovi:YwUDL2J", LinkCodes.encode("https://clovi.ru/f/YwUDL2J"));
        assertEquals("clovi:post/12207", LinkCodes.encode("https://clovi.ru/post/12207"));
        assertEquals("clovi:post/12207", LinkCodes.encode("http://www.clovi.ru/post/12207/"));
    }

    @Test
    void imgurAlbumsAndGalleriesStayPages() {
        // fixImgurLink used to turn every imgur.com/<rest> into i.imgur.com/<rest>.png, a dead address for albums.
        assertEquals("https://i.imgur.com/MK3eW3A.png", LinkCodes.directImgurLink("https://imgur.com/MK3eW3A"));
        for (String page : List.of("https://imgur.com/a/AbCdE12", "https://imgur.com/gallery/some-title-AbCdE12")) {
            assertEquals(page, LinkCodes.directImgurLink(page));
            String code = LinkCodes.encode(LinkCodes.directImgurLink(page));
            assertTrue(code.startsWith("u:"), code);
            assertEquals(page, LinkCodes.decode(code), "the page link survives, so its preview picture is used");
        }
        assertEquals("https://example.org/x.png", LinkCodes.directImgurLink("https://example.org/x.png"));
    }

    @Test
    void editorFixesImgurLinksThroughLinkCodes() throws IOException {
        String mod = Files.readString(Path.of("src/main/java/io/github/jumperonjava/imaginebook/Imaginebook.java"));
        assertTrue(mod.contains("return LinkCodes.directImgurLink(link);"));
    }

    @Test
    void cloviCodesOpenTheirAddresses() {
        assertEquals("https://clovi.ru/f/YwUDL2J", LinkCodes.decode("clovi:YwUDL2J"));
        assertEquals("https://clovi.ru/post/12207", LinkCodes.decode("clovi:post/12207"));
        assertEquals("clovi:../x", LinkCodes.decode("clovi:../x"), "anything else is not a clovi address");
    }

    @Test
    void imgurLinksBecomeShortCodes() {
        assertEquals("imgur:MK3eW3A", LinkCodes.encode("https://i.imgur.com/MK3eW3A.jpg"));
        assertEquals("imgur:MK3eW3A", LinkCodes.encode("https://i.imgur.com/MK3eW3A.png"));
        assertEquals("imgur:MK3eW3A", LinkCodes.encode("https://imgur.com/MK3eW3A"));
        assertEquals("imgur:MK3eW3A", LinkCodes.encode("  http://i.imgur.com/MK3eW3A.webp "));
    }

    @Test
    void imgurCodeOpensAsADirectImage() {
        assertEquals("https://i.imgur.com/MK3eW3A.png", LinkCodes.decode("imgur:MK3eW3A"));
    }

    @Test
    void otherLinksAreEncodedAndComeBackExactly() {
        for (String link : LINKS.subList(1, 4)) {
            String code = LinkCodes.encode(link);
            assertTrue(code.startsWith("u:"), code);
            assertEquals(link, LinkCodes.decode(code));
        }
    }

    @Test
    void codesDoNotLookLikeLinks() {
        // Server link filters look for a scheme or a domain; a code has neither, and no comma to break the tag.
        for (String link : LINKS) {
            String code = LinkCodes.encode(link);
            assertFalse(code.contains("://"), code);
            assertFalse(code.contains("."), code);
            assertFalse(code.contains(","), code);
            assertFalse(code.toLowerCase().contains("http"), code);
        }
    }

    @Test
    void encodingIsIdempotentAndLeavesOtherSourcesAlone() {
        for (String link : LINKS) {
            String code = LinkCodes.encode(link);
            assertEquals(code, LinkCodes.encode(code));
        }
        assertEquals("res:minecraft:textures/item/apple.png", LinkCodes.encode("res:minecraft:textures/item/apple.png"));
        assertEquals("", LinkCodes.encode(""));
    }

    @Test
    void brokenCodeIsLeftAsIs() {
        assertEquals("u:%%%", LinkCodes.decode("u:%%%"));
        assertEquals("res:minecraft:x.png", LinkCodes.decode("res:minecraft:x.png"));
    }

    @Test
    void decodedCodeMustBeAWebLink() {
        String nested = "u:" + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("res:minecraft:x.png".getBytes());
        assertFalse(LinkCodes.isWebLink(LinkCodes.decode(nested)));
        assertTrue(LinkCodes.isWebLink(LinkCodes.decode(LinkCodes.encode(LINKS.get(1)))));
    }

    @Test
    void codedTagsAreReadAsImages() {
        String page = "Hi [" + LinkCodes.encode(LINKS.get(1)) + ",4,5,50,50,0] and [imgur:MK3eW3A,1,2]";
        var images = ImageSerializer.parseSafeModeImages(page);
        assertEquals(2, images.size());
        assertEquals(LinkCodes.encode(LINKS.get(1)), images.get(0).getUrl());
        assertEquals("imgur:MK3eW3A", images.get(1).getUrl());
        assertEquals(2, ImageTags.find(page).size());
    }

    @Test
    void editorStoresCodesAndShowsLinks() throws IOException {
        String editor = Files.readString(Path.of("src/main/java/io/github/jumperonjava/imaginebook/mixin/NewBookEditScreenMixin.java"));
        assertTrue(editor.contains("imageData.setUrl(LinkCodes.encode(Imaginebook.fixImgurLink(url)))"));
        assertTrue(editor.contains("urlField.setValue(LinkCodes.decode(currentPageImage.getUrl()))"));
        String mod = Files.readString(Path.of("src/main/java/io/github/jumperonjava/imaginebook/Imaginebook.java"));
        assertTrue(mod.contains("\"imgur\", CodedLinkResolver.IMGUR"));
        assertTrue(mod.contains("\"u\", CodedLinkResolver.CODED"));
        assertTrue(mod.contains("\"clovi\", CodedLinkResolver.CLOVI"));
    }
}
