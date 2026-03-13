package net.bobofraggins.intellistore.shared.ui;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * A dialog window that stacks {@link IDialogPane} instances vertically below a title bar.
 *
 * <p>The dialog owns its own background (vanilla title bar + gray fill + border lines) and routes
 * mouse events to whichever pane contains the cursor.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * // Create once in Screen.init():
 * dialog = new Dialog(width, paneA, paneB, paneC);
 * dialog.init(leftPos, topPos);
 *
 * // In renderBg():
 * dialog.render(graphics, font, title, mouseX, mouseY, partialTick);
 *
 * // In mouseClicked/mouseScrolled:
 * if (dialog.mouseClicked(mouseX, mouseY, button)) return true;
 * }</pre>
 */
public class Dialog {

    private static final ResourceLocation BG_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    /** Height of the vanilla title bar strip. */
    public static final int TITLE_H = 17;

    private static final int COLOR_BODY = 0xFFC6C6C6;
    private static final int COLOR_BORDER_DARK = 0xFF555555;
    private static final int COLOR_BORDER_LIGHT = 0xFFFFFFFF;

    private final int width;
    private final List<IDialogPane> panes;

    /** Y offset of each pane relative to the body start (i.e. relative to {@code y + TITLE_H}). */
    private final int[] paneYOffsets;

    private final int bodyHeight;

    /** Screen-space origin, set by {@link #init}. */
    private int x;

    private int y;

    public Dialog(int width, IDialogPane... panes) {
        this.width = width;
        this.panes = List.of(panes);
        this.paneYOffsets = new int[panes.length];
        int total = 0;
        for (int i = 0; i < panes.length; i++) {
            paneYOffsets[i] = total;
            total += panes[i].preferredHeight();
        }
        this.bodyHeight = total;
    }

    /** Call from {@code Screen.init()} with the screen's {@code leftPos} and {@code topPos}. */
    public void init(int screenX, int screenY) {
        this.x = screenX;
        this.y = screenY;
    }

    /** Total height of the dialog: title bar plus the sum of all pane preferred heights. */
    public int totalHeight() {
        return TITLE_H + bodyHeight;
    }

    /** Absolute screen-space X of the pane at the given index. */
    public int getPaneAbsX(int index) {
        return x;
    }

    /** Absolute screen-space Y of the pane at the given index. */
    public int getPaneAbsY(int index) {
        return y + TITLE_H + paneYOffsets[index];
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    public void render(GuiGraphics graphics, Font font, Component title, int mouseX, int mouseY, float partialTick) {
        int totalH = totalHeight();

        // Title bar from vanilla container texture
        graphics.blit(BG_TEXTURE, x, y, 0, 0, width, TITLE_H);

        // Gray body fill
        graphics.fill(x, y + TITLE_H, x + width, y + totalH, COLOR_BODY);

        // Borders: dark left + bottom, light right
        graphics.fill(x, y + TITLE_H, x + 1, y + totalH, COLOR_BORDER_DARK);
        graphics.fill(x + width - 1, y + TITLE_H, x + width, y + totalH, COLOR_BORDER_LIGHT);
        graphics.fill(x, y + totalH - 1, x + width, y + totalH, COLOR_BORDER_DARK);

        // Title centred in title bar
        graphics.drawString(font, title, x + (width - font.width(title)) / 2, y + 5, 0x404040, false);

        // Render each pane translated to its local origin
        for (int i = 0; i < panes.size(); i++) {
            IDialogPane pane = panes.get(i);
            int paneAbsY = getPaneAbsY(i);
            int localMouseX = mouseX - x;
            int localMouseY = mouseY - paneAbsY;
            graphics.pose().pushPose();
            graphics.pose().translate(x, paneAbsY, 0);
            pane.render(graphics, font, width, localMouseX, localMouseY, partialTick);
            graphics.pose().popPose();
        }
    }

    // -------------------------------------------------------------------------
    // Input routing
    // -------------------------------------------------------------------------

    /** Routes a mouse click to the pane under the cursor. Returns true if consumed. */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < panes.size(); i++) {
            int paneAbsY = getPaneAbsY(i);
            int paneH = panes.get(i).preferredHeight();
            if (mouseY >= paneAbsY && mouseY < paneAbsY + paneH) {
                return panes.get(i).mouseClicked(mouseX - x, mouseY - paneAbsY, button);
            }
        }
        return false;
    }

    /** Routes a mouse scroll to the pane under the cursor. Returns true if consumed. */
    public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
        for (int i = 0; i < panes.size(); i++) {
            int paneAbsY = getPaneAbsY(i);
            int paneH = panes.get(i).preferredHeight();
            if (mouseY >= paneAbsY && mouseY < paneAbsY + paneH) {
                return panes.get(i).mouseScrolled(mouseX - x, mouseY - paneAbsY, dx, dy);
            }
        }
        return false;
    }
}
