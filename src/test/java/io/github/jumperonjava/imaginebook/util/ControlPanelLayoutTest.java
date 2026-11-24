package io.github.jumperonjava.imaginebook.util;

import io.github.jumperonjava.imaginebook.util.ControlPanelLayout.Rect;
import io.github.jumperonjava.imaginebook.util.ControlPanelLayout.Slot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlPanelLayoutTest {
    private static final int W = 929, H = 590;

    /** Stendhal 1.4.9 on 26.3: book actions on the left, page actions on the right, toolbar along the bottom. */
    private static List<Rect> stendhal(int w, int h) {
        List<Rect> taken = new ArrayList<>();
        for (int i = 0; i < 5; i++) taken.add(new Rect(4, 13 + i * 36, 217, 27));
        for (int i = 0; i < 7; i++) taken.add(new Rect(w - 221, 13 + i * 36, 217, 27));
        for (int i = 0; i < 16; i++) {
            taken.add(new Rect(w / 2 - 176 + i * 22, h - 50, 20, 20));
            taken.add(new Rect(w / 2 - 176 + i * 22, h - 25, 20, 20));
        }
        taken.add(new Rect((w - 192) / 2 + 36, 14, 120, 180)); // the page text box
        return taken;
    }

    private static List<Slot> slots(ControlPanelLayout.Slots s) {
        return List.of(s.add(), s.remove(), s.url(), s.xButton(), s.xField(), s.yButton(), s.yField(),
                s.widthButton(), s.widthField(), s.heightButton(), s.heightField(), s.spinButton(), s.spinField(), s.hide());
    }

    private static void assertClear(ControlPanelLayout.Slots layout, List<Rect> taken) {
        for (Slot slot : slots(layout)) {
            Rect rect = new Rect(slot.x(), slot.y(), slot.w(), ControlPanelLayout.ROW);
            for (Rect other : taken) {
                assertFalse(rect.intersects(other), slot + " overlaps " + other);
            }
        }
    }

    @Test
    void withStendhalThePanelGoesLeftBetweenItsColumnAndToolbar() {
        var taken = stendhal(W, H);
        var placed = ControlPanelLayout.place(taken, W, H);
        assertTrue(placed.isPresent());
        Rect panel = placed.get();
        assertTrue(panel.x() + panel.w() <= (W - 192) / 2, "left of the book");
        assertTrue(panel.y() >= 13 + 4 * 36 + 27, "below Stendhal's left column");
        assertTrue(panel.y() + panel.h() <= H - 54, "above Stendhal's toolbar band");
        assertClear(ControlPanelLayout.layout(taken, W, H), taken);
    }

    @Test
    void vanillaScreenGetsTheTopLeftCorner() {
        List<Rect> taken = List.of(new Rect((W - 192) / 2 + 36, 14, 120, 180), new Rect(W / 2 - 100, 196, 98, 20),
                new Rect(W / 2 + 2, 196, 98, 20));
        var panel = ControlPanelLayout.place(taken, W, H).orElseThrow();
        assertEquals(ControlPanelLayout.MARGIN, panel.x());
        assertEquals(ControlPanelLayout.MARGIN, panel.y());
    }

    @Test
    void everySlotStaysInsideThePanel() {
        var layout = ControlPanelLayout.panel(10, 20, 200);
        for (Slot slot : slots(layout)) {
            assertTrue(slot.x() >= 10 && slot.x() + slot.w() <= 210, slot.toString());
            assertTrue(slot.y() >= 20 && slot.y() + ControlPanelLayout.ROW <= 20 + ControlPanelLayout.HEIGHT, slot.toString());
        }
    }

    @Test
    void slotsDoNotOverlapEachOther() {
        var all = slots(ControlPanelLayout.panel(0, 0, 200));
        for (int i = 0; i < all.size(); i++) {
            for (int j = i + 1; j < all.size(); j++) {
                Rect a = new Rect(all.get(i).x(), all.get(i).y(), all.get(i).w(), ControlPanelLayout.ROW);
                Rect b = new Rect(all.get(j).x(), all.get(j).y(), all.get(j).w(), ControlPanelLayout.ROW);
                assertFalse(a.intersects(b), all.get(i) + " and " + all.get(j));
            }
        }
    }

    @Test
    void hiddenLinkToggleIsTheLastFullWidthRow() {
        var layout = ControlPanelLayout.panel(10, 20, 200);
        assertEquals(10, layout.hide().x());
        assertEquals(200, layout.hide().w());
        assertEquals(20 + ControlPanelLayout.HEIGHT - ControlPanelLayout.ROW, layout.hide().y());
        var bottom = ControlPanelLayout.bottomRows(H);
        for (Slot other : List.of(bottom.url(), bottom.xButton(), bottom.widthButton(), bottom.spinButton())) {
            Rect a = new Rect(bottom.hide().x(), bottom.hide().y(), bottom.hide().w(), ControlPanelLayout.ROW);
            assertFalse(a.intersects(new Rect(other.x(), other.y(), other.w(), ControlPanelLayout.ROW)), "bottom rows: " + other);
        }
    }

    @Test
    void tooSmallScreenFallsBackToTheBottomRows() {
        int w = 400, h = 240;
        var taken = stendhal(w, h);
        assertTrue(ControlPanelLayout.place(taken, w, h).isEmpty());
        assertEquals(ControlPanelLayout.bottomRows(h), ControlPanelLayout.layout(taken, w, h));
    }

    @Test
    void sidesNarrowerThanTheMinimumAreNotUsed() {
        int w = 192 + 2 * 150;
        assertTrue(ControlPanelLayout.place(List.of(), w, 600).isEmpty());
    }
}
