package net.bobofraggins.intellistore.shared.ui;

import java.util.function.Supplier;
import net.bobofraggins.intellistore.shared.config.SortMode;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Config-drawer pane that displays the current sort mode and advances it on click.
 *
 * <p>Layout adapts to the {@code width} passed to {@link #render}, matching the style of
 * {@link PriorityPane}. Constructed with a supplier that reads the current mode (from the
 * block entity) and a {@link Runnable} that the caller uses to cycle the mode and send the
 * appropriate network packet.
 */
public class SortPane implements IDialogPane {

    private static final int PANE_HEIGHT = 28;
    private static final int LABEL_Y = 3;
    private static final int BTN_Y = 14;
    private static final int BTN_H = 14;
    /** Button width — slightly narrower than the drawer so there is padding on each side. */
    private static final int BTN_W = 90;

    private static final int COLOR_BTN = 0xFFC6C6C6;
    private static final int COLOR_BTN_HOVER = 0xFFB0B0B0;

    private final Supplier<SortMode> modeSupplier;
    private final Runnable onCycle;

    public SortPane(Supplier<SortMode> modeSupplier, Runnable onCycle) {
        this.modeSupplier = modeSupplier;
        this.onCycle = onCycle;
    }

    // -------------------------------------------------------------------------
    // IDialogPane
    // -------------------------------------------------------------------------

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
        SortMode mode = modeSupplier.get();
        int btnX = (width - BTN_W) / 2;

        Component label = Component.translatable("screen.intellistore.sort_label");
        graphics.drawString(font, label, (width - font.width(label)) / 2, LABEL_Y, 0x404040, false);

        boolean hovered = isInButton(localMouseX, localMouseY, btnX);
        graphics.fill(btnX, BTN_Y, btnX + BTN_W, BTN_Y + BTN_H, hovered ? COLOR_BTN_HOVER : COLOR_BTN);

        String btnText = mode.displayName() + " \u25b6";
        int textX = btnX + (BTN_W - font.width(btnText)) / 2;
        int textY = BTN_Y + (BTN_H - font.lineHeight) / 2;
        graphics.drawString(font, btnText, textX, textY, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(double localX, double localY, int button) {
        if (button != 0) return false;
        int btnX = (preferredWidth() - BTN_W) / 2;
        if (isInButton(localX, localY, btnX)) {
            onCycle.run();
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static boolean isInButton(double lx, double ly, int btnX) {
        return lx >= btnX && lx < btnX + BTN_W && ly >= BTN_Y && ly < BTN_Y + BTN_H;
    }
}
