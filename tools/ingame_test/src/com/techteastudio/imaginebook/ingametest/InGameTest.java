package com.techteastudio.imaginebook.ingametest;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Development-only client mod: drives a real Minecraft 26.3 client through the book editor and exits.
 * Run by tools/ingame_test/run.py with ImagineBook Continued, Stendhal and Fabric API installed in an
 * isolated game directory. Every step prints a line starting with IMAGINEBOOK_TEST.
 */
public final class InGameTest implements ClientModInitializer {
    private static final String TAG = "IMAGINEBOOK_TEST ";

    @Override
    public void onInitializeClient() {
        Thread thread = new Thread(InGameTest::run, "imaginebook-ingame-test");
        thread.setDaemon(true);
        thread.start();
    }

    private static void log(String message) {
        System.out.println(TAG + message);
    }

    private static <T> T onMain(Supplier<T> task) throws Exception {
        return Minecraft.getInstance().submit(task).get(60, TimeUnit.SECONDS);
    }

    private static void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    private static void shot(String name) throws Exception {
        onMain(() -> {
            Screenshot.grab(Minecraft.getInstance(), false);
            return null;
        });
        sleep(1500);
        log("screenshot " + name);
    }

    private static boolean hasWidget(String label) throws Exception {
        return onMain(() -> Minecraft.getInstance().gui.screen().children().stream()
                .anyMatch(c -> c instanceof AbstractWidget w && w.getMessage().getString().equals(label)));
    }

    private static int widgetCount() throws Exception {
        return onMain(() -> Minecraft.getInstance().gui.screen().children().size());
    }

    private static void run() {
        Minecraft mc = Minecraft.getInstance();
        try {
            long deadline = System.currentTimeMillis() + 150_000;
            // BookEditScreen needs a real player (it reads owner.getName()), so the test plays in a world.
            String lastScreen = "";
            while (!onMain(() -> mc.player != null && mc.level != null && mc.gui.screen() == null)) {
                String current = onMain(() -> mc.gui.screen() == null ? "none" : mc.gui.screen().getClass().getName()
                        + " \"" + mc.gui.screen().getTitle().getString() + "\"");
                if (!current.equals(lastScreen)) {
                    log("waiting for the world, screen " + current);
                    lastScreen = current;
                }
                if (System.currentTimeMillis() > deadline) {
                    shot("0_world_never_loaded");
                    throw new IllegalStateException("world never loaded");
                }
                sleep(1000);
            }
            sleep(4000);
            log("in world");

            ItemStack book = new ItemStack(Items.WRITABLE_BOOK);
            WritableBookContent content = new WritableBookContent(List.of(
                    Filterable.passThrough("ImagineBook Continued in-game test\n"
                            + "[res:imaginebook:textures/downloading.png,8,24,60,60,0]"
                            + "[https://i.imgur.com/removed.png,90,90,1000,1000,15]"),
                    Filterable.passThrough("Second page")));
            book.set(DataComponents.WRITABLE_BOOK_CONTENT, content);
            onMain(() -> {
                mc.gui.setScreen(new BookEditScreen(mc.player, book, InteractionHand.MAIN_HAND, content));
                return null;
            });
            sleep(5000);
            log("screen " + onMain(() -> mc.gui.screen().getClass().getName()));
            log("widgets after open: " + widgetCount());
            log("add button after open: " + hasWidget("Add image"));
            shot("1_opened");

            onMain(() -> {
                Screen screen = mc.gui.screen();
                int bookX = (screen.width - 192) / 2;
                screen.mouseClicked(new MouseButtonEvent(bookX + 30, 2 + 24 + 20, new MouseButtonInfo(0, 0)), false);
                return null;
            });
            sleep(1500);
            log("image selected, remove button visible: " + onMain(() -> mc.gui.screen().children().stream()
                    .anyMatch(c -> c instanceof AbstractWidget w && w.visible && w.getMessage().getString().equals("Remove image"))));
            shot("2_image_selected");

            onMain(() -> {
                Screen screen = mc.gui.screen();
                screen.resize(screen.width, screen.height);
                return null;
            });
            sleep(1500);
            boolean addAfterResize = hasWidget("Add image");
            log("widgets after resize: " + widgetCount() + ", add button after resize: " + addAfterResize);
            shot("3_after_resize");

            boolean ok = hasWidget("Add image") && addAfterResize;
            log(ok ? "PASSED" : "FAILED: ImagineBook widgets missing");
            onMain(() -> { mc.gui.setScreen(null); return null; });
            sleep(1000);
        } catch (Throwable failure) {
            failure.printStackTrace();
            log("FAILED " + failure);
        } finally {
            mc.execute(mc::stop);
        }
    }
}
