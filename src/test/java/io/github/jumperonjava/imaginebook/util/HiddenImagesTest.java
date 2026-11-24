package io.github.jumperonjava.imaginebook.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jumperonjava.imaginebook.ImageSerializer;
import io.github.jumperonjava.imaginebook.image.ImageData;
import net.minecraft.util.StringUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HiddenImagesTest {
    private static ImageData image(String url, float x, float y, float w, float h, float rot) {
        ImageData d = new ImageData(url, (short) 0, (short) 0, w, h);
        d.x = x;
        d.y = y;
        d.rotation = rot;
        return d;
    }

    private static String encode(ImageData d, String caption) {
        return HiddenImages.encode(d, caption).orElseThrow();
    }

    @Test
    void imgurRoundTrip() {
        String text = encode(image("imgur:MK3eW3A", 36, 30, 0.11f, 0.12f, 15), "Картинка");
        List<HiddenImages.Decoded> found = HiddenImages.findAll(text);
        assertEquals(1, found.size());
        HiddenImages.Decoded d = found.get(0);
        assertEquals(0, d.start());
        assertEquals(text.length(), d.end());
        assertEquals("imgur:MK3eW3A", d.image().getUrl());
        assertEquals(36, d.image().x);
        assertEquals(30, d.image().y);
        assertEquals(0.11f, d.image().widthFraction, 1e-6);
        assertEquals(0.12f, d.image().heightFraction, 1e-6);
        assertEquals(15, d.image().rotation);
        assertEquals("Картинка", d.image().caption);
    }

    @Test
    void cloviFileAndPostRoundTrip() {
        for (String url : List.of("clovi:YwUDL2J", "clovi:post/12207")) {
            String text = encode(image(url, -40, 120, 0.5f, 0.5f, 270), "Pic");
            assertEquals(url, HiddenImages.findAll(text).get(0).image().getUrl());
            assertEquals(-40, HiddenImages.findAll(text).get(0).image().x);
            assertEquals(270, HiddenImages.findAll(text).get(0).image().rotation);
        }
    }

    @Test
    void afterTheCaptionOnlyInvisibleCharacters() {
        String text = encode(image("imgur:MK3eW3A", 36, 30, 1, 1, 0), "Картинка");
        assertTrue(text.startsWith("Картинка"));
        for (char c : text.substring("Картинка".length()).toCharArray()) {
            assertTrue(c == HiddenImages.ZERO || c == HiddenImages.ONE, "unexpected char U+" + Integer.toHexString(c));
        }
    }

    @Test
    void bookTextCarriesNoLink() {
        for (String url : List.of("imgur:MK3eW3A", "clovi:YwUDL2J", "clovi:post/12207")) {
            String code = encode(image(url, 36, 30, 1, 1, 0), "");
            for (String part : List.of("imgur", "clovi", "http", ".", ":", "/", "[", "]", ",")) {
                assertFalse(code.contains(part), url + " leaks " + part);
            }
        }
    }

    @Test
    void vanillaServerKeepsEveryCharacter() {
        // ServerGamePacketListenerImpl.handleEditBook drops what StringUtil.isAllowedChatCharacter refuses.
        String text = encode(image("clovi:post/12207", 5, 5, 1, 1, 90), "Картинка");
        text.chars().forEach(c -> assertTrue(StringUtil.isAllowedChatCharacter(c), "server would drop U+" + Integer.toHexString(c)));
    }

    @Test
    void zeroIsZeroWidthInTheVanillaFont() throws IOException {
        try (InputStream in = HiddenImagesTest.class.getClassLoader().getResourceAsStream("assets/minecraft/font/include/space.json")) {
            assertNotNull(in, "vanilla font definition on the classpath");
            JsonObject advances = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonArray("providers").get(0).getAsJsonObject().getAsJsonObject("advances");
            assertEquals(0, advances.get(String.valueOf(HiddenImages.ZERO)).getAsInt(), "players without the mod see nothing");
            assertEquals(4, advances.get(String.valueOf(HiddenImages.ONE)).getAsInt());
        }
    }

    @Test
    void imgurCodeTakesAboutTwoLines() {
        String code = encode(image("imgur:MK3eW3A", 36, 30, 1, 1, 0), "");
        assertTrue(code.length() <= 130, "length " + code.length());
        long spaces = code.chars().filter(c -> c == HiddenImages.ONE).count();
        assertTrue(spaces * 4 <= 3 * 114, spaces + " spaces is more than three book lines");
    }

    @Test
    void oneFlippedBitIsRejected() {
        String text = encode(image("imgur:MK3eW3A", 36, 30, 1, 1, 0), "");
        for (int i = HiddenImages.MARKER.length(); i < text.length(); i += 7) {
            char[] chars = text.toCharArray();
            chars[i] = chars[i] == HiddenImages.ZERO ? HiddenImages.ONE : HiddenImages.ZERO;
            assertTrue(HiddenImages.findAll(new String(chars)).isEmpty(), "bit " + i);
        }
    }

    @Test
    void framesAmongOrdinaryText() {
        String a = encode(image("imgur:AAAAAAA", 1, 2, 1, 1, 0), "One");
        String b = encode(image("clovi:post/9", 3, 4, 1, 1, 0), "Two");
        String page = "Hello " + a + " world\n" + b + "‌ stray  spaces";
        List<HiddenImages.Decoded> found = HiddenImages.findAll(page);
        assertEquals(2, found.size());
        assertEquals("imgur:AAAAAAA", found.get(0).image().getUrl());
        assertEquals(page.indexOf("One"), found.get(0).start());
        assertEquals("clovi:post/9", found.get(1).image().getUrl());
        assertEquals(page.indexOf("Two"), found.get(1).start());
        assertTrue(HiddenImages.findAll("plain text with  spaces‌‌ and a stray mark").isEmpty());
    }

    @Test
    void otherSourcesStayTags() {
        for (String url : List.of("u:aHR0cHM6Ly9leGFtcGxlLm9yZy94LnBuZw", "res:minecraft:textures/item/apple.png", "https://i.imgur.com/MK3eW3A.png", "")) {
            assertFalse(HiddenImages.canHide(url), url);
            ImageData d = image(url, 1, 1, 1, 1, 0);
            assertEquals(d.bookString(), HiddenImages.write(d, true, "Pic"));
        }
    }

    @Test
    void toggleDecidesTheFormat() {
        ImageData d = image("imgur:MK3eW3A", 36, 30, 1, 1, 0);
        assertEquals(d.bookString(), HiddenImages.write(d, false, "Pic"));
        String hidden = HiddenImages.write(d, true, "Pic");
        assertTrue(hidden.startsWith("Pic") && hidden.length() > 50);
        assertEquals("", HiddenImages.write(new DeletedImageData(), true, "Pic"), "a removed picture leaves nothing");
    }

    @Test
    void outOfRangeValuesAreClamped() {
        HiddenImages.Decoded d = HiddenImages.findAll(encode(image("imgur:MK3eW3A", 1000, -900, 20f, 0f, -90), "")).get(0);
        assertEquals(511, d.image().x);
        assertEquals(-512, d.image().y);
        assertEquals(10.23f, d.image().widthFraction, 1e-6);
        assertEquals(0f, d.image().heightFraction, 1e-6);
        assertEquals(270, d.image().rotation);
    }

    @Test
    void existingCaptionIsKeptOnRewrite() {
        ImageData d = HiddenImages.findAll(encode(image("imgur:MK3eW3A", 1, 1, 1, 1, 0), "Мой кот")).get(0).image();
        d.x = 50;
        String again = HiddenImages.write(d, true, "Картинка");
        assertTrue(again.startsWith("Мой кот"));
        assertEquals(50, HiddenImages.findAll(again).get(0).image().x);
    }

    @Test
    void tagsAndHiddenPicturesShareOneOrder() {
        String hidden = encode(image("imgur:BBBBBBB", 3, 4, 1, 1, 0), "Pic");
        String page = "[imgur:AAAAAAA,1,2,100,100,0] text " + hidden + " [clovi:CCCCCCC,5,6]";
        List<ImageData> images = ImageSerializer.parseSafeModeImages(page);
        assertEquals(List.of("imgur:AAAAAAA", "imgur:BBBBBBB", "clovi:CCCCCCC"), images.stream().map(ImageData::getUrl).toList());
        List<ImageTags.Span> spans = ImageTags.find(page);
        assertEquals(3, spans.size());
        assertEquals(hidden, page.substring(spans.get(1).start(), spans.get(1).end()));
        // the editor rewrites picture n by span n; the hidden one is replaced whole, caption included
        assertEquals("[imgur:AAAAAAA,1,2,100,100,0] text X [clovi:CCCCCCC,5,6]", ImageTags.replace(page, 1, "X"));
        assertEquals(" text  ", ImageTags.strip(page));
    }

    @Test
    void editorWritesThroughTheToggle() throws IOException {
        String editor = Files.readString(Path.of("src/main/java/io/github/jumperonjava/imaginebook/mixin/NewBookEditScreenMixin.java"));
        assertTrue(editor.contains("ImageTags.replace(getCurrentPageContent(), id, imaginebook$write(modifiedImage))"));
        assertTrue(editor.contains("HiddenImages.write(image, Config.INSTANCE.data().hideLinks, I18n.get(\"imaginebook.hidden.caption\"))"));
    }
}
