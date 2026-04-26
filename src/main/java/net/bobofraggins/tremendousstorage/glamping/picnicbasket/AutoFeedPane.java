package net.bobofraggins.tremendousstorage.glamping.picnicbasket;

import java.util.function.BooleanSupplier;
import net.bobofraggins.tremendousstorage.shared.ui.ConfigDrawer;
import net.bobofraggins.tremendousstorage.shared.ui.IDialogPane;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Config drawer pane for the picnic basket's auto-feed toggle.
 *
 * <p>Mirrors {@link net.bobofraggins.tremendousstorage.shared.ui.VoidExcessPane} in layout;
 * the label reads "Auto-feed When Hungry" and the toggle reflects whether the basket will
 * automatically consume food to restore the carrying player's hunger.
 */
public class AutoFeedPane implements IDialogPane {

    private static final int PANE_HEIGHT = 36;
    private static final int LABEL_Y = 3;
    private static final int TOGGLE_Y = 14;
    private static final int TOGGLE_W = 64;
    private static final int TOGGLE_H = 16;

    private static final ResourceLocation TOGGLE_ON =
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "textures/gui/widget/toggle_on.png");
    private static final ResourceLocation TOGGLE_OFF =
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "textures/gui/widget/toggle_off.png");

    private final BooleanSupplier stateGetter;
    private final Runnable toggleAction;

    public AutoFeedPane(BooleanSupplier stateGetter, Runnable toggleAction) {
        this.stateGetter = stateGetter;
        this.toggleAction = toggleAction;
    }

    @Override
    public int preferredWidth() {
        return ConfigDrawer.WIDTH;
    }

    @Override
    public int preferredHeight() {
        return PANE_HEIGHT;
    }

    @Override
    public void render(
            GuiGraphics graphics, Font font, int width, int localMouseX, int localMouseY, float partialTick) {
        Component label = Component.translatable("screen.tremendousstorage.auto_feed_label")
                .withStyle(ChatFormatting.BOLD);
        graphics.drawString(font, label, (width - font.width(label)) / 2, LABEL_Y, 0x404040, false);

        int toggleX = (width - TOGGLE_W) / 2;
        ResourceLocation tex = stateGetter.getAsBoolean() ? TOGGLE_ON : TOGGLE_OFF;
        graphics.blit(tex, toggleX, TOGGLE_Y, 0, 0, TOGGLE_W, TOGGLE_H, TOGGLE_W, TOGGLE_H);
    }

    @Override
    public boolean mouseClicked(double localX, double localY, int button) {
        if (button != 0) return false;
        int toggleX = (preferredWidth() - TOGGLE_W) / 2;
        if (localX >= toggleX && localX < toggleX + TOGGLE_W && localY >= TOGGLE_Y && localY < TOGGLE_Y + TOGGLE_H) {
            toggleAction.run();
            return true;
        }
        return false;
    }
}
