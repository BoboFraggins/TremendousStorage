package net.bobofraggins.intellistore.storage.accessterminal;

import net.bobofraggins.intellistore.shared.ui.IDialogPane;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Dialog pane that renders the player inventory and hotbar slot backgrounds.
 *
 * <p>Local coordinate origin (0, 0) sits at {@code topPos + AccessTerminalLayout.PLAYER_INV_Y}.
 * Vanilla {@link net.minecraft.client.gui.screens.inventory.AbstractContainerScreen} renders
 * the actual item stacks on top of these backgrounds.
 *
 * <p>The slot outline strip is blitted from {@code generic_54.png} at local y = −7 to match the
 * exact positioning used by the vanilla container texture.
 */
public class PlayerInventoryPane implements IDialogPane {

    private static final ResourceLocation BG_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    @Override
    public int preferredHeight() {
        return AccessTerminalLayout.BG_HEIGHT - AccessTerminalLayout.PLAYER_INV_Y;
    }

    @Override
    public void render(
            GuiGraphics graphics, Font font, int width, int localMouseX, int localMouseY, float partialTick) {
        // Blit the player inventory slot outlines from generic_54.png.
        // Source y=126 starts 7 px above the actual slot rows; offset dest by -7 so the
        // slot outlines land at local y=0 (= PLAYER_INV_Y in screen space).
        graphics.blit(BG_TEXTURE, 0, -7, 0, 126, width, 96);
    }
}
