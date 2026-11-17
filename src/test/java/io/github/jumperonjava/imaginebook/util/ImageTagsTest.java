package io.github.jumperonjava.imaginebook.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageTagsTest {
    private static final String TAG = "[https://i.imgur.com/MK3eW3A.jpg,40,25,100,100,0]";

    @Test
    void findsTagsAsInTheBookFormat() {
        String page = "Hi " + TAG + " there [res:minecraft:textures/item/apple.png,1,2]";
        var spans = ImageTags.find(page);
        assertEquals(2, spans.size());
        assertEquals(TAG, page.substring(spans.get(0).start(), spans.get(0).end()));
        assertEquals("[res:minecraft:textures/item/apple.png,1,2]", page.substring(spans.get(1).start(), spans.get(1).end()));
    }

    @Test
    void plainBracketsAreNotTags() {
        assertTrue(ImageTags.find("[note] and [a,b] and [x,1]").isEmpty());
    }

    @Test
    void stripRemovesOnlyTags() {
        assertEquals("Hi  there", ImageTags.strip("Hi " + TAG + " there"));
        assertEquals("no tags", ImageTags.strip("no tags"));
    }

    @Test
    void piecesSplitALineAtTagBorders() {
        String page = "ab" + TAG + "cd";
        var tags = ImageTags.find(page);
        // a wrapped line that starts in the middle of the tag and ends after it
        int offset = 10;
        String line = page.substring(offset);
        var pieces = ImageTags.pieces(line, offset, tags);
        assertEquals(List.of(new ImageTags.Piece(page.substring(10, 2 + TAG.length()), true),
                new ImageTags.Piece("cd", false)), pieces);
    }

    @Test
    void pieceAfterATagKeepsTheFormattingBeforeIt() {
        String page = "§cred §l" + TAG + "bold";
        var pieces = ImageTags.pieces(page, 0, ImageTags.find(page));
        assertEquals(3, pieces.size());
        assertEquals("§cred §l", pieces.get(0).text());
        assertTrue(pieces.get(1).tag());
        assertEquals("§c§lbold", pieces.get(2).text());
    }

    @Test
    void lineWithoutTagsIsOnePiece() {
        var pieces = ImageTags.pieces("plain", 0, List.of());
        assertEquals(List.of(new ImageTags.Piece("plain", false)), pieces);
    }

    @Test
    void activeFormattingFollowsMinecraftRules() {
        assertEquals("§c§l", ImageTags.activeFormatting("§cred §lbold"));
        assertEquals("§c", ImageTags.activeFormatting("§l§c"), "a colour resets formats");
        assertEquals("", ImageTags.activeFormatting("§c§r"), "reset clears everything");
        assertEquals("", ImageTags.activeFormatting("plain"));
    }

    @Test
    void fadedKeepsColourAndLowersAlpha() {
        int faded = ImageTags.faded(0xFF102030);
        assertEquals(0x102030, faded & 0xFFFFFF);
        assertTrue((faded >>> 24) < 0x40);
    }

    @Test
    void componentWithoutTagsIsReturnedAsIs() {
        Component page = Component.literal("no images");
        assertSame(page, ImageTags.strip(page));
    }

    @Test
    void componentStripKeepsStyles() {
        Component page = Component.literal("red ").withStyle(ChatFormatting.RED)
                .append(Component.literal("x" + TAG + "y").withStyle(ChatFormatting.BOLD));
        Component stripped = ImageTags.strip(page);
        assertEquals("red xy", stripped.getString());
        List<String> bold = new ArrayList<>();
        stripped.visit((style, text) -> {
            if (style.isBold()) bold.add(text);
            return Optional.empty();
        }, Style.EMPTY);
        assertEquals(List.of("x", "y"), bold);
    }
}
