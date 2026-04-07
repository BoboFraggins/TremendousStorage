package net.bobofraggins.tremendousstorage.shared.ui;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * A dialog window that stacks {@link IDialogPane} instances vertically below a title bar.
 *
 * <p>The dialog owns its own background (9-slice title bar + gray fill + border lines) and routes
 * mouse events to whichever pane contains the cursor.
 *
 * <p>Background is rendered using nine separate 1-pixel textures: four corners, four edges, and a
 * center. Corner textures are blitted directly; the top edge tiles horizontally to fill any dialog
 * width; body fill and single-pixel borders use {@code fill()} (equivalent to tiling a 1×1 solid
 * pixel).
 *
 * <p>Usage:
 *
 * <pre>{@code
 * // Create in Screen constructor (so imageWidth/imageHeight are available):
 * dialog = new Dialog(paneA, paneB, paneC);
 * imageWidth  = dialog.totalWidth();
 * imageHeight = dialog.totalHeight();
 *
 * // In Screen.init():
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

    // ── 9-slice textures ─────────────────────────────────────────────────────
    // Each texture contains only the actual border decoration pixels from generic_54.png —
    // no interior gray fill is stored in the texture data.
    // Corners: 5×5. Top/bottom edges: 1×5 (tile H). Left/right edges: 5×1 (tile V).
    // The interior body fill is rendered via fill() using COLOR_BODY.

    private static final ResourceLocation TEX_CORNER_TL =
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "textures/gui/dialog_corner_tl.png");
    private static final ResourceLocation TEX_CORNER_TR =
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "textures/gui/dialog_corner_tr.png");
    private static final ResourceLocation TEX_CORNER_BL =
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "textures/gui/dialog_corner_bl.png");
    private static final ResourceLocation TEX_CORNER_BR =
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "textures/gui/dialog_corner_br.png");
    private static final ResourceLocation TEX_EDGE_TOP =
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "textures/gui/dialog_edge_top.png");
    private static final ResourceLocation TEX_EDGE_BOTTOM =
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "textures/gui/dialog_edge_bottom.png");
    private static final ResourceLocation TEX_EDGE_LEFT =
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "textures/gui/dialog_edge_left.png");
    private static final ResourceLocation TEX_EDGE_RIGHT =
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "textures/gui/dialog_edge_right.png");

    /** Height of the title bar area; panes are positioned below this. */
    public static final int TITLE_H = 17;

    /** Size of each corner square and the fixed dimension of each edge texture. */
    private static final int CORNER = 5;

    /**
     * Padding added below the last pane so its content doesn't sit directly under the bottom
     * corner border. Equal to {@link #CORNER} so the bottom frame is flush with the pane end.
     */
    public static final int BOTTOM_PADDING = CORNER;

    // Body fill colour — matches the center.png 1×1 pixel.
    private static final int COLOR_BODY = 0xFFC6C6C6;

    private final int width;
    private final List<IDialogPane> panes;

    /** Index of the pane that last consumed a mouseClicked; -1 if none. Used to route drags. */
    private int activePaneIndex = -1;

    /** Y offset of each pane relative to the body start (i.e. relative to {@code y + TITLE_H}). */
    private final int[] paneYOffsets;

    private final int bodyHeight;

    /** Screen-space origin, set by {@link #init}. */
    private int x;

    private int y;

    /**
     * Returns a no-op pane with the given fixed dimensions and no rendered content.
     *
     * <p>Useful for screens that manage their own content rendering but still want Dialog to
     * draw the background frame at the correct size.
     */
    public static IDialogPane blankPane(int width, int height) {
        return new IDialogPane() {
            @Override
            public int preferredWidth() {
                return width;
            }

            @Override
            public int preferredHeight() {
                return height;
            }

            @Override
            public void render(
                    GuiGraphics graphics, Font font, int w, int localMouseX, int localMouseY, float partialTick) {}
        };
    }

    /**
     * Creates a dialog whose dimensions are derived from the panes.
     * Width = {@code max(pane.preferredWidth())} over all panes.
     * Height = {@link #TITLE_H} + sum of pane heights + {@link #BOTTOM_PADDING}.
     */
    public Dialog(IDialogPane... panes) {
        this.panes = List.of(panes);
        this.paneYOffsets = new int[panes.length];
        int totalH = 0;
        int maxW = 0;
        for (int i = 0; i < panes.length; i++) {
            paneYOffsets[i] = totalH;
            totalH += panes[i].preferredHeight();
            maxW = Math.max(maxW, panes[i].preferredWidth());
        }
        this.bodyHeight = totalH;
        this.width = maxW;
    }

    /** Call from {@code Screen.init()} with the screen's {@code leftPos} and {@code topPos}. */
    public void init(int screenX, int screenY) {
        this.x = screenX;
        this.y = screenY;
    }

    /** Total width of the dialog, equal to the widest pane's {@code preferredWidth()}. */
    public int totalWidth() {
        return width;
    }

    /** Total height: title bar + pane heights + bottom padding. */
    public int totalHeight() {
        return TITLE_H + bodyHeight + BOTTOM_PADDING;
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
        int bodyTop = y + CORNER;
        int bodyBot = y + totalH; // exclusive bottom of content area
        int midW = width - 2 * CORNER; // width of middle strip (between corners)
        int midH = totalH - 2 * CORNER; // height of middle strip (between corners)

        // ── Top corners + top edge ────────────────────────────────────────────
        graphics.blit(TEX_CORNER_TL, x, y, 0, 0, CORNER, CORNER, CORNER, CORNER);
        for (int px = 0; px < midW; px++) {
            graphics.blit(TEX_EDGE_TOP, x + CORNER + px, y, 0, 0, 1, CORNER, 1, CORNER);
        }
        graphics.blit(TEX_CORNER_TR, x + width - CORNER, y, 0, 0, CORNER, CORNER, CORNER, CORNER);

        // ── Side edges + center fill ──────────────────────────────────────────
        for (int py = 0; py < midH; py++) {
            graphics.blit(TEX_EDGE_LEFT, x, bodyTop + py, 0, 0, CORNER, 1, CORNER, 1);
            graphics.blit(TEX_EDGE_RIGHT, x + width - CORNER, bodyTop + py, 0, 0, CORNER, 1, CORNER, 1);
        }
        graphics.fill(x + CORNER, bodyTop, x + width - CORNER, bodyBot - CORNER, COLOR_BODY);

        // ── Bottom edge + bottom corners ──────────────────────────────────────
        graphics.blit(TEX_CORNER_BL, x, bodyBot - CORNER, 0, 0, CORNER, CORNER, CORNER, CORNER);
        for (int px = 0; px < midW; px++) {
            graphics.blit(TEX_EDGE_BOTTOM, x + CORNER + px, bodyBot - CORNER, 0, 0, 1, CORNER, 1, CORNER);
        }
        graphics.blit(TEX_CORNER_BR, x + width - CORNER, bodyBot - CORNER, 0, 0, CORNER, CORNER, CORNER, CORNER);

        // ── Title centred in title bar ────────────────────────────────────────
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
        activePaneIndex = -1;
        for (int i = 0; i < panes.size(); i++) {
            int paneAbsY = getPaneAbsY(i);
            int paneH = panes.get(i).preferredHeight();
            if (mouseY >= paneAbsY && mouseY < paneAbsY + paneH) {
                if (panes.get(i).mouseClicked(mouseX - x, mouseY - paneAbsY, button)) {
                    activePaneIndex = i;
                    return true;
                }
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

    /**
     * Routes a mouse drag to the pane that consumed the last {@link #mouseClicked}.
     * Returns true if consumed.
     */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (activePaneIndex >= 0) {
            int paneAbsY = getPaneAbsY(activePaneIndex);
            return panes.get(activePaneIndex).mouseDragged(mouseX - x, mouseY - paneAbsY, button, dragX, dragY);
        }
        return false;
    }

    /**
     * Routes a mouse release to the pane that consumed the last {@link #mouseClicked}.
     * Returns true if consumed.
     */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (activePaneIndex >= 0) {
            int paneAbsY = getPaneAbsY(activePaneIndex);
            boolean result = panes.get(activePaneIndex).mouseReleased(mouseX - x, mouseY - paneAbsY, button);
            activePaneIndex = -1;
            return result;
        }
        return false;
    }
}
