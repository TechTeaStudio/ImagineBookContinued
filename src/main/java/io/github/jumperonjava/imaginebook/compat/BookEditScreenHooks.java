package io.github.jumperonjava.imaginebook.compat;

/**
 * Implemented by the book editor mixin, called by {@code ScreenInitMixin} after any screen has
 * run its {@code init()}.
 *
 * <p>The editor widgets are not added at the tail of {@code BookEditScreen.init()} on purpose:
 * Stendhal injects at the head of that method and cancels it to build its own editor, so a tail
 * injection never runs and the screen crashed on the first frame ({@code removeButton} was null).
 * Hooking the caller in {@code Screen} works whoever built the screen.
 */
public interface BookEditScreenHooks {
    void imaginebook$afterInit();
}
