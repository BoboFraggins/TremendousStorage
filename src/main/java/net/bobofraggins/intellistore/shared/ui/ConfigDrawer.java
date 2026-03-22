package net.bobofraggins.intellistore.shared.ui;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Animated slide-out config panel that appears to the left of the main dialog.
 *
 * <p>Contains {@link IDialogPane} instances stacked vertically. It is toggled by a button in the
 * dialog title bar and slides in/out with an ease-out cubic animation.
 *
 * <p>Call {@link #render} in {@code renderBg()} <em>before</em> rendering the main dialog so the
 * drawer appears behind the dialog's left border.
 */
public class ConfigDrawer {

    /** Width of the drawer in pixels. */
    public static final int WIDTH = 110;

    private static final long ANIM_MS = 200L;
    private static final int CORNER = 5;
    private static final int CONTENT_PAD = 5;
    private static final int COLOR_BODY = 0xFFC6C6C6;

    // 9-slice textures shared with Dialog (left/top/bottom only — right side abuts the dialog)
    private static final ResourceLocation TEX_CORNER_TL =
            ResourceLocation.fromNamespaceAndPath("intellistore", "textures/gui/dialog_corner_tl.png");
    private static final ResourceLocation TEX_CORNER_BL =
            ResourceLocation.fromNamespaceAndPath("intellistore", "textures/gui/dialog_corner_bl.png");
    private static final ResourceLocation TEX_EDGE_TOP =
            ResourceLocation.fromNamespaceAndPath("intellistore", "textures/gui/dialog_edge_top.png");
    private static final ResourceLocation TEX_EDGE_BOTTOM =
            ResourceLocation.fromNamespaceAndPath("intellistore", "textures/gui/dialog_edge_bottom.png");
    private static final ResourceLocation TEX_EDGE_LEFT =
            ResourceLocation.fromNamespaceAndPath("intellistore", "textures/gui/dialog_edge_left.png");

    private final List<IDialogPane> panes;
    private final int[] paneYOffsets;

    private boolean open = false;
    private float animFrom = 0f;
    private long animStartMs = -1L;

    /** Left edge of the main dialog in screen space (= right edge of this drawer when fully open). */
    private int dialogX;

    /** Top edge of the main dialog in screen space. */
    private int dialogY;

    /** Height of the main dialog in pixels. */
    private int dialogH;

    public ConfigDrawer(IDialogPane... panes) {
        this.panes = List.of(panes);
        this.paneYOffsets = new int[panes.length];
        int y = CONTENT_PAD;
        for (int i = 0; i < panes.length; i++) {
            paneYOffsets[i] = y;
            y += panes[i].preferredHeight() + CONTENT_PAD;
        }
    }

    /**
     * Call from {@code Screen.init()} with the dialog's screen-space origin and height. Safe to
     * call multiple times on window resize; preserves open/animation state.
     */
    public void init(int dialogX, int dialogY, int dialogH) {
        this.dialogX = dialogX;
        this.dialogY = dialogY;
        this.dialogH = dialogH;
    }

    /** Toggles the drawer open or closed, starting a smooth animation from the current progress. */
    public void toggle() {
        animFrom = getProgress(System.currentTimeMillis());
        open = !open;
        animStartMs = System.currentTimeMillis();
    }

    /**
     * Renders the drawer. Must be called <em>before</em> the main dialog so the drawer appears
     * behind the dialog's left border.
     */
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        float p = getProgress(System.currentTimeMillis());
        if (p <= 0.001f) return;

        int drawerX = dialogX - WIDTH;
        int drawerTop = dialogY + 10;
        int drawerH = dialogH - 20;

        // Scissor to the currently-visible portion (slides open from right to left).
        // Right boundary extends 1 px into the dialog border to eliminate the seam.
        int visibleLeft = dialogX - Math.round(WIDTH * p);
        graphics.enableScissor(visibleLeft, drawerTop, dialogX + 1, drawerTop + drawerH);

        // Body fill — extend 1 px right to cover the dialog's left border pixel
        graphics.fill(drawerX + CORNER, drawerTop + CORNER, dialogX + 1, drawerTop + drawerH - CORNER, COLOR_BODY);

        // Top-left corner + top edge
        graphics.blit(TEX_CORNER_TL, drawerX, drawerTop, 0, 0, CORNER, CORNER, CORNER, CORNER);
        int edgeW = WIDTH - CORNER;
        for (int px = 0; px < edgeW; px++) {
            graphics.blit(TEX_EDGE_TOP, drawerX + CORNER + px, drawerTop, 0, 0, 1, CORNER, 1, CORNER);
        }

        // Left edge
        int midH = drawerH - 2 * CORNER;
        for (int py = 0; py < midH; py++) {
            graphics.blit(TEX_EDGE_LEFT, drawerX, drawerTop + CORNER + py, 0, 0, CORNER, 1, CORNER, 1);
        }

        // Bottom-left corner + bottom edge
        graphics.blit(TEX_CORNER_BL, drawerX, drawerTop + drawerH - CORNER, 0, 0, CORNER, CORNER, CORNER, CORNER);
        for (int px = 0; px < edgeW; px++) {
            graphics.blit(
                    TEX_EDGE_BOTTOM, drawerX + CORNER + px, drawerTop + drawerH - CORNER, 0, 0, 1, CORNER, 1, CORNER);
        }

        // Pane content — offset by top inset
        for (int i = 0; i < panes.size(); i++) {
            int paneAbsY = dialogY + 10 + paneYOffsets[i];
            int localMouseX = mouseX - drawerX;
            int localMouseY = mouseY - paneAbsY;
            graphics.pose().pushPose();
            graphics.pose().translate(drawerX, paneAbsY, 0);
            panes.get(i).render(graphics, font, WIDTH, localMouseX, localMouseY, partialTick);
            graphics.pose().popPose();
        }

        graphics.disableScissor();
    }

    /**
     * Routes a mouse click into the drawer. Only accepts clicks when the drawer is fully open.
     *
     * @return {@code true} if consumed
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (getProgress(System.currentTimeMillis()) < 0.99f) return false;

        int drawerX = dialogX - WIDTH;
        if (mouseX < drawerX || mouseX >= dialogX) return false;
        if (mouseY < dialogY + 10 || mouseY >= dialogY + dialogH - 10) return false;

        for (int i = 0; i < panes.size(); i++) {
            int paneAbsY = dialogY + 10 + paneYOffsets[i];
            int paneH = panes.get(i).preferredHeight();
            if (mouseY >= paneAbsY && mouseY < paneAbsY + paneH) {
                return panes.get(i).mouseClicked(mouseX - drawerX, mouseY - paneAbsY, button);
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private float getProgress(long now) {
        if (animStartMs < 0) return open ? 1f : 0f;
        float t = Math.min((now - animStartMs) / (float) ANIM_MS, 1f);
        // Ease-out cubic: decelerates as it approaches the target
        float inv = 1f - t;
        float eased = 1f - inv * inv * inv;
        return open ? animFrom + (1f - animFrom) * eased : animFrom * (1f - eased);
    }
}
