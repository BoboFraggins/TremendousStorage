package net.bobofraggins.intellistore.shared.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Dialog pane that renders the player inventory and hotbar slot backgrounds.
 *
 * <p>Local coordinate origin (0, 0) sits at the top of the player inventory rows.
 * Vanilla {@link net.minecraft.client.gui.screens.inventory.AbstractContainerScreen} renders
 * the actual item stacks on top of these backgrounds.
 *
 * <p>The slot outline strip is blitted from {@code generic_54.png} at local y = −7 to match the
 * exact positioning used by the vanilla container texture.
 *
 * <p>Can be used inside a {@link Dialog} or called directly from a screen's {@code renderBg}
 * by translating the pose stack to the player inventory origin first.
 */
public class PlayerInventoryPane implements IDialogPane {

    private static final ResourceLocation BG_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    /** Standard width: vanilla player inventory texture width. */
    public static final int WIDTH = 176;

    /** Standard height: 3 inventory rows + gap + hotbar + bottom padding. */
    public static final int HEIGHT = 80;

    @Override
    public int preferredWidth() {
        return WIDTH;
    }

    @Override
    public int preferredHeight() {
        return HEIGHT;
    }

    @Override
    public void render(
            GuiGraphics graphics, Font font, int width, int localMouseX, int localMouseY, float partialTick) {
        // Blit the player inventory slot outlines from generic_54.png.
        // Source y=126 starts 7 px above the actual slot rows; offset dest by -7 so the
        // slot outlines land at local y=0.
        // Width is fixed at 176 — the vanilla player inventory texture is exactly that wide.
        graphics.blit(BG_TEXTURE, 0, -7, 0, 126, 176, 96);
    }
}
