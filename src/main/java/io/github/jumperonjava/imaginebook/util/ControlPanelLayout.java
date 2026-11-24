package io.github.jumperonjava.imaginebook.util;

import java.util.List;
import java.util.Optional;

/**
 * Where the image controls go on the book editor. They used to fill two rows along the bottom edge,
 * right under Stendhal's formatting toolbar (y from height - 50, centred), and covered it. Now they form
 * a panel in the first free spot beside the book, clear of every widget already on the screen and of
 * that bottom band; only a screen with no room on either side gets the old bottom rows.
 */
public final class ControlPanelLayout {
    public static final int ROW = 20;
    public static final int GAP = 4;
    public static final int MARGIN = 4;
    public static final int ROWS = 6;
    public static final int HEIGHT = ROWS * ROW + (ROWS - 1) * GAP;
    public static final int MAX_WIDTH = 200;
    public static final int MIN_WIDTH = 160;
    private static final int BOOK_WIDTH = 192;
    /** Stendhal's toolbar and symbol buttons start at height - 50. */
    private static final int BOTTOM_BAND = 54;

    public record Rect(int x, int y, int w, int h) {
        public boolean intersects(Rect o) {
            return x < o.x + o.w && o.x < x + w && y < o.y + o.h && o.y < y + h;
        }
    }

    public record Slot(int x, int y, int w) {}

    public record Slots(Slot add, Slot remove, Slot url,
                        Slot xButton, Slot xField, Slot yButton, Slot yField,
                        Slot widthButton, Slot widthField, Slot heightButton, Slot heightField,
                        Slot spinButton, Slot spinField, Slot hide) {}

    private ControlPanelLayout() {}

    public static Slots layout(List<Rect> taken, int screenWidth, int screenHeight) {
        return place(taken, screenWidth, screenHeight)
                .map(r -> panel(r.x(), r.y(), r.w()))
                .orElseGet(() -> bottomRows(screenHeight));
    }

    /** The first free panel spot, left of the book first, then right of it, scanning from the top. */
    public static Optional<Rect> place(List<Rect> taken, int screenWidth, int screenHeight) {
        int bookLeft = (screenWidth - BOOK_WIDTH) / 2;
        int width = Math.min(MAX_WIDTH, bookLeft - 2 * MARGIN);
        if (width < MIN_WIDTH) return Optional.empty();
        int lastY = screenHeight - BOTTOM_BAND - HEIGHT;
        for (int x : new int[]{MARGIN, screenWidth - MARGIN - width}) {
            for (int y = MARGIN; y <= lastY; y++) {
                Rect panel = new Rect(x, y, width, HEIGHT);
                Rect padded = new Rect(x - 2, y - 2, width + 4, HEIGHT + 4);
                if (taken.stream().noneMatch(padded::intersects)) return Optional.of(panel);
            }
        }
        return Optional.empty();
    }

    /**
     * Add and Remove on top, the link under them, then position, size and rotation, each a small
     * button (scroll over it to nudge the value) next to its field, and the hidden-link toggle last.
     */
    public static Slots panel(int x, int y, int width) {
        int half = (width - GAP) / 2;
        int right = x + half + GAP;
        int field = half - ROW - 2;
        int[] row = new int[ROWS];
        for (int i = 0; i < ROWS; i++) row[i] = y + i * (ROW + GAP);
        return new Slots(
                new Slot(x, row[0], half), new Slot(right, row[0], half), new Slot(x, row[1], width),
                new Slot(x, row[2], ROW), new Slot(x + ROW + 2, row[2], field),
                new Slot(right, row[2], ROW), new Slot(right + ROW + 2, row[2], field),
                new Slot(x, row[3], ROW), new Slot(x + ROW + 2, row[3], field),
                new Slot(right, row[3], ROW), new Slot(right + ROW + 2, row[3], field),
                new Slot(x, row[4], ROW), new Slot(x + ROW + 2, row[4], field),
                new Slot(x, row[5], width));
    }

    /** The original two rows along the bottom edge, for screens too narrow for a side panel. */
    public static Slots bottomRows(int screenHeight) {
        int column = 96;
        int top = screenHeight - 50 + GAP;
        int second = top + ROW + GAP;
        int[] col = new int[5];
        for (int i = 0; i < 5; i++) col[i] = GAP + i * (column + GAP);
        int field = column - 24;
        return new Slots(
                new Slot(col[0], second, column), new Slot(col[1], second, column), new Slot(col[0], top, 2 * column + GAP),
                new Slot(col[2], top, ROW), new Slot(col[2] + 24, top, field),
                new Slot(col[2], second, ROW), new Slot(col[2] + 24, second, field),
                new Slot(col[3], top, ROW), new Slot(col[3] + 24, top, field),
                new Slot(col[3], second, ROW), new Slot(col[3] + 24, second, field),
                new Slot(col[4], second, ROW), new Slot(col[4] + 24, second, field),
                new Slot(col[4], top, column));  // the free top cell of the last column
    }
}
