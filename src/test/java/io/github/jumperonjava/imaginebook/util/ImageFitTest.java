package io.github.jumperonjava.imaginebook.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageFitTest {
    @Test
    void bigPictureShrinksIntoThePageTextArea() {
        assertEquals(11, ImageFit.percent(1000, 1000), "114 / 1000, rounded down");
        assertEquals(50, ImageFit.percent(228, 100), "width decides");
        assertEquals(25, ImageFit.percent(100, 504), "height decides: 126 / 504");
    }

    @Test
    void smallPictureKeepsItsSize() {
        assertEquals(100, ImageFit.percent(64, 64));
        assertEquals(100, ImageFit.percent(114, 126));
    }

    @Test
    void fittedPictureNeverExceedsTheArea() {
        for (int w = 1; w <= 3000; w += 37) {
            for (int h = 1; h <= 3000; h += 53) {
                int p = ImageFit.percent(w, h);
                assertTrue(p >= 1 && p <= 100, w + "x" + h);
                assertTrue(w * p / 100f <= ImageFit.PAGE_WIDTH && h * p / 100f <= ImageFit.PAGE_HEIGHT, w + "x" + h);
            }
        }
    }

    @Test
    void emptySizeIsLeftAlone() {
        assertEquals(100, ImageFit.percent(0, 0));
    }

    @Test
    void pageAreaMatchesMinecraft() {
        // BookViewScreen.PAGE_TEXT_X_OFFSET / PAGE_TEXT_Y_OFFSET and BookEditScreen.TEXT_WIDTH / TEXT_HEIGHT in 26.3.
        assertEquals(36, ImageFit.PAGE_X);
        assertEquals(30, ImageFit.PAGE_Y);
        assertEquals(114, ImageFit.PAGE_WIDTH);
        assertEquals(126, ImageFit.PAGE_HEIGHT);
    }

    @Test
    void editorUsesTheFixes() throws IOException {
        String editor = Files.readString(Path.of("src/main/java/io/github/jumperonjava/imaginebook/mixin/NewBookEditScreenMixin.java"));
        assertTrue(editor.contains("ImageTags.replace(getCurrentPageContent(), id, modifiedImage.bookString())"), "tags found by ImageTags");
        assertFalse(editor.contains("Pattern.compile(\"\\\\[.*?\\\\]\")"), "no lookup by any bracket");
        assertTrue(editor.contains("new ImageData(\"\", (short) ImageFit.PAGE_X, (short) ImageFit.PAGE_Y, 1, 1)"), "new picture starts on the page");
        assertTrue(editor.contains("ImageFit.percent("), "a new picture is fitted once it has loaded");
        String downloader = Files.readString(Path.of("src/main/java/io/github/jumperonjava/imaginebook/resolvers/AsyncImageDownloader.java"));
        assertFalse(downloader.contains("1MB limit"), "the limit is 8 MB");
    }
}
