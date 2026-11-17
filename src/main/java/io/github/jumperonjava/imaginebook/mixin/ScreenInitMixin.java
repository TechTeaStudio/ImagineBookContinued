package io.github.jumperonjava.imaginebook.mixin;

import io.github.jumperonjava.imaginebook.compat.BookEditScreenHooks;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the ImagineBook editor widgets right after a screen's {@code init()} returns.
 *
 * <p>Both callers are covered: {@code init(int, int)} on first open and {@code rebuildWidgets()}
 * on resize, which clears the widgets before calling {@code init()} again. See
 * {@link BookEditScreenHooks} for why this does not target {@code BookEditScreen.init()}.
 */
@Mixin(Screen.class)
public abstract class ScreenInitMixin {
    @Inject(method = {"init(II)V", "rebuildWidgets()V"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;init()V", shift = At.Shift.AFTER))
    private void imaginebook$afterInit(CallbackInfo ci) {
        if (this instanceof BookEditScreenHooks hooks) {
            hooks.imaginebook$afterInit();
        }
    }
}
