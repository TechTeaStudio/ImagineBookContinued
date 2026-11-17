package io.github.jumperonjava.imaginebook;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the Stendhal compatibility fix as source text.
 *
 * <p>Stendhal injects at the head of {@code BookEditScreen.init()} and cancels it. ImagineBook used to
 * add its widgets at the tail of that same method, so with Stendhal installed they were never created
 * and opening a book and quill crashed ("removeButton is null"). The widgets must be added from
 * {@code ScreenInitMixin}, after {@code Screen} has called {@code init()}.
 */
class StendhalCompatSourceTest {
    private static final Path MIXINS = Path.of("src/main/java/io/github/jumperonjava/imaginebook/mixin");

    private static String read(String file) throws IOException {
        return Files.readString(MIXINS.resolve(file));
    }

    @Test
    void editorDoesNotInjectIntoBookEditScreenInit() throws IOException {
        String editor = read("NewBookEditScreenMixin.java");
        assertFalse(editor.matches("(?s).*@Inject\\(\\s*method\\s*=\\s*\"init.*"),
                "Stendhal cancels BookEditScreen.init(); add widgets from ScreenInitMixin instead");
        assertTrue(editor.contains("implements BookEditScreenHooks"));
    }

    @Test
    void screenHookCoversFirstOpenAndResize() throws IOException {
        String hook = read("ScreenInitMixin.java");
        assertTrue(hook.contains("\"init(II)V\""), "first open goes through Screen.init(int, int)");
        assertTrue(hook.contains("\"rebuildWidgets()V\""), "resize goes through Screen.rebuildWidgets()");
        assertTrue(hook.contains("shift = At.Shift.AFTER"), "widgets must be added after init() returns");
    }

    @Test
    void screenHookIsRegistered() throws IOException {
        String config = Files.readString(Path.of("src/main/resources/imaginebook.mixins.json"));
        assertTrue(config.contains("\"ScreenInitMixin\""));
    }
}
