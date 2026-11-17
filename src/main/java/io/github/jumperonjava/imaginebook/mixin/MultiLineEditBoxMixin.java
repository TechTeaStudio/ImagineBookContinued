package io.github.jumperonjava.imaginebook.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.jumperonjava.imaginebook.util.ImageTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/**
 * Fades image tags in the book editor's text box so the picture under them stays readable. They stay in
 * the text, faint, so the cursor and Backspace still show where a tag is; the reader hides them fully.
 *
 * MultiLineEditBox.extractContents (26.3) walks the lines and draws each with GuiGraphicsExtractor.text
 * in three places: ordinal 0 the part before the cursor, ordinal 1 the part after it, ordinal 2 a line
 * without the cursor. Before those calls it always asks the line's StringView for beginIndex() and
 * endIndex(); those values are recorded here, because StringView is package-private and a handler
 * cannot name it. defaultRequire 1 fails loudly if an update moves any of this, and the Mixin smoke
 * check loads this class.
 */
@Mixin(MultiLineEditBox.class)
public abstract class MultiLineEditBoxMixin {
    private static final String TEXT = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V";

    @Unique private int imaginebook$lineBegin;
    @Unique private int imaginebook$lineEnd;

    @ModifyExpressionValue(method = "extractContents", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/MultilineTextField$StringView;beginIndex()I"))
    private int imaginebook$lineBegin(int begin) {
        imaginebook$lineBegin = begin;
        return begin;
    }

    @ModifyExpressionValue(method = "extractContents", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/MultilineTextField$StringView;endIndex()I"))
    private int imaginebook$lineEnd(int end) {
        imaginebook$lineEnd = end;
        return end;
    }

    @WrapOperation(method = "extractContents", at = @At(value = "INVOKE", target = TEXT, ordinal = 0))
    private void imaginebook$beforeCursor(GuiGraphicsExtractor graphics, Font font, String part, int x, int y, int color,
                                          boolean shadow, Operation<Void> original) {
        imaginebook$draw(graphics, font, part, imaginebook$lineBegin, x, y, color, shadow, original);
    }

    @WrapOperation(method = "extractContents", at = @At(value = "INVOKE", target = TEXT, ordinal = 1))
    private void imaginebook$afterCursor(GuiGraphicsExtractor graphics, Font font, String part, int x, int y, int color,
                                         boolean shadow, Operation<Void> original) {
        imaginebook$draw(graphics, font, part, imaginebook$lineEnd - part.length(), x, y, color, shadow, original);
    }

    @WrapOperation(method = "extractContents", at = @At(value = "INVOKE", target = TEXT, ordinal = 2))
    private void imaginebook$wholeLine(GuiGraphicsExtractor graphics, Font font, String part, int x, int y, int color,
                                       boolean shadow, Operation<Void> original) {
        imaginebook$draw(graphics, font, part, imaginebook$lineBegin, x, y, color, shadow, original);
    }

    @Unique
    private void imaginebook$draw(GuiGraphicsExtractor graphics, Font font, String part, int offset, int x, int y, int color,
                                  boolean shadow, Operation<Void> original) {
        // Only the book editor: other screens' text boxes (signs, chat with Stendhal) keep their text as is.
        List<ImageTags.Span> tags = Minecraft.getInstance().gui.screen() instanceof BookEditScreen
                ? ImageTags.find(((MultiLineEditBox) (Object) this).getValue()) : List.of();
        if (tags.isEmpty()) {
            original.call(graphics, font, part, x, y, color, shadow);
            return;
        }
        int drawX = x;
        for (ImageTags.Piece piece : ImageTags.pieces(part, offset, tags)) {
            original.call(graphics, font, piece.text(), drawX, y, piece.tag() ? ImageTags.faded(color) : color, shadow && !piece.tag());
            drawX += font.width(piece.text());
        }
    }
}
