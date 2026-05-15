package net.bobofraggins.tremendousstorage.shared.ui;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
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

    /** Width of the drawer in pixels (does not include the tab). */
    public static final int WIDTH = 110;

    /** Width of the toggle-button tab that always protrudes from the dialog's left edge. */
    public static final int TAB_W = 24;

    /** Height of the toggle-button tab. Just tall enough for a 16×16 button with 3 px padding. */
    public static final int TAB_H = 22;

    private static final long ANIM_MS = 200L;
    private static final int CORNER = 5;
    private static final int CONTENT_PAD = 5;
    private static final int VERT_PAD = 12;
    private static final int SIDE_PAD = 12;
    private static final int COLOR_BODY = 0xFFC6C6C6;

    private static final ResourceLocation BUTTON_NORMAL =
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "widget/button_config");

    private static final ResourceLocation BUTTON_HOVER =
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "widget/button_config_focused");

    // 9-slice textures shared with Dialog (left/top/bottom only — right side abuts the dialog)
    private static final ResourceLocation TEX_CORNER_TL =
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "textures/gui/dialog_corner_tl.png");
    private static final ResourceLocation TEX_CORNER_BL =
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "textures/gui/dialog_corner_bl.png");
    private static final ResourceLocation TEX_EDGE_TOP =
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "textures/gui/dialog_edge_top.png");
    private static final ResourceLocation TEX_EDGE_BOTTOM =
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "textures/gui/dialog_edge_bottom.png");
    private static final ResourceLocation TEX_EDGE_LEFT =
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "textures/gui/dialog_edge_left.png");

    private final List<IDialogPane> panes;
    private final int[] paneYOffsets;
    /** Total height of the pane content area (padding + pane heights + padding). */
    private final int contentHeight;

    /**
     * When true (the default), the tab renders the config toggle icon and handles its own clicks.
     * Set to false for screens that place the toggle button elsewhere (e.g. inside the dialog).
     */
    private boolean showTabButton = true;

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
        int y = VERT_PAD;
        for (int i = 0; i < panes.length; i++) {
            paneYOffsets[i] = y;
            y += panes[i].preferredHeight();
            if (i < panes.length - 1) y += CONTENT_PAD;
        }
        this.contentHeight = y + VERT_PAD;
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

    /**
     * Disables the built-in tab button rendering and click handling. Call this when the host screen
     * places its own toggle button elsewhere (e.g. inside the main dialog title bar).
     */
    public void withoutTabButton() {
        showTabButton = false;
    }

    /** Toggles the drawer open or closed, starting a smooth animation from the current progress. */
    public void toggle() {
        animFrom = getProgress(System.currentTimeMillis());
        open = !open;
        animStartMs = System.currentTimeMillis();
    }

    /**
     * Renders only the drawer body (not the tab). Call this <em>before</em> the main dialog.
     * Follow with {@link #renderTab} <em>after</em> the main dialog so the tab appears on top.
     */
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        float p = getProgress(System.currentTimeMillis());
        if (p <= 0.001f) {
            return;
        }

        int drawerX = dialogX - TAB_W - WIDTH;
        int drawerTop = dialogY + 15;
        int drawerH = contentHeight + 2 * CORNER;

        // Scissor to the currently-visible portion (slides open from right to left).
        // Right boundary extends 1 px into the dialog to eliminate the seam.
        int visibleLeft = dialogX - TAB_W - Math.round(WIDTH * p);
        graphics.enableScissor(visibleLeft, drawerTop, dialogX + 1, drawerTop + drawerH);

        // Body fill — extends right to the dialog left edge (+1 px to cover seam)
        graphics.fill(drawerX + CORNER, drawerTop + CORNER, dialogX + 1, drawerTop + drawerH - CORNER, COLOR_BODY);

        // Top-left corner + top edge
        graphics.blit(
                RenderPipelines.GUI_TEXTURED, TEX_CORNER_TL, drawerX, drawerTop, 0, 0, CORNER, CORNER, CORNER, CORNER);
        int edgeW = WIDTH + TAB_W - CORNER;
        for (int px = 0; px < edgeW; px++) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEX_EDGE_TOP,
                    drawerX + CORNER + px,
                    drawerTop,
                    0,
                    0,
                    1,
                    CORNER,
                    1,
                    CORNER);
        }

        // Left edge
        int midH = drawerH - 2 * CORNER;
        for (int py = 0; py < midH; py++) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEX_EDGE_LEFT,
                    drawerX,
                    drawerTop + CORNER + py,
                    0,
                    0,
                    CORNER,
                    1,
                    CORNER,
                    1);
        }

        // Bottom-left corner + bottom edge
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEX_CORNER_BL,
                drawerX,
                drawerTop + drawerH - CORNER,
                0,
                0,
                CORNER,
                CORNER,
                CORNER,
                CORNER);
        for (int px = 0; px < edgeW; px++) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEX_EDGE_BOTTOM,
                    drawerX + CORNER + px,
                    drawerTop + drawerH - CORNER,
                    0,
                    0,
                    1,
                    CORNER,
                    1,
                    CORNER);
        }

        // Pane content — offset by top inset
        for (int i = 0; i < panes.size(); i++) {
            int paneAbsY = dialogY + 15 + paneYOffsets[i];
            int localMouseX = mouseX - drawerX - SIDE_PAD;
            int localMouseY = mouseY - paneAbsY;
            graphics.pose().pushMatrix();
            graphics.pose().translate(drawerX + SIDE_PAD, paneAbsY);
            panes.get(i).render(graphics, font, WIDTH, localMouseX, localMouseY, partialTick);
            graphics.pose().popMatrix();
        }

        graphics.disableScissor();
    }

    /**
     * Renders the tab that protrudes from the dialog's left edge. Call this <em>after</em> the
     * main dialog so the tab appears on top of both the drawer body and the dialog background.
     */
    public void renderTab(GuiGraphics graphics, int mouseX, int mouseY) {
        renderTab(graphics, mouseX, mouseY, getProgress(System.currentTimeMillis()));
    }

    private void renderTab(GuiGraphics graphics, int mouseX, int mouseY, float p) {
        // Tab slides left with the drawer: closed → left of dialog; open → left of drawer body.
        // The extra 2 px (animated via p) shifts the tab slightly left of the drawer edge when open.
        int tabX = dialogX - TAB_W + 5 - Math.round((WIDTH + TAB_W + 2) * p);
        int tabY = dialogY + 15;
        int midH = TAB_H - 2 * CORNER;

        // Fill — right edge extends 1 px into the dialog so the seam is fully covered.
        graphics.fill(tabX + CORNER, tabY + CORNER, tabX + TAB_W + 1, tabY + TAB_H - CORNER, COLOR_BODY);

        // Top-left corner
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEX_CORNER_TL, tabX, tabY, 0, 0, CORNER, CORNER, CORNER, CORNER);

        // Top edge
        for (int px = 0; px < TAB_W - CORNER + 1; px++) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEX_EDGE_TOP, tabX + CORNER + px, tabY, 0, 0, 1, CORNER, 1, CORNER);
        }

        // Left edge
        for (int py = 0; py < midH; py++) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEX_EDGE_LEFT, tabX, tabY + CORNER + py, 0, 0, CORNER, 1, CORNER, 1);
        }

        // Bottom-left corner
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEX_CORNER_BL,
                tabX,
                tabY + TAB_H - CORNER,
                0,
                0,
                CORNER,
                CORNER,
                CORNER,
                CORNER);

        // Bottom edge
        for (int px = 0; px < TAB_W - CORNER + 1; px++) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEX_EDGE_BOTTOM,
                    tabX + CORNER + px,
                    tabY + TAB_H - CORNER,
                    0,
                    0,
                    1,
                    CORNER,
                    1,
                    CORNER);
        }
        // No right border — abuts the dialog (closed) or drawer body (open)

        if (showTabButton) {
            boolean hovered = mouseX >= tabX && mouseX < tabX + TAB_W + 1 && mouseY >= tabY && mouseY < tabY + TAB_H;
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED, hovered ? BUTTON_HOVER : BUTTON_NORMAL, tabX + 4, tabY + 3, 16, 16);
        }
    }

    /**
     * Routes a mouse click into the drawer. Also handles clicks on the toggle tab when
     * {@link #showTabButton} is true.
     *
     * @return {@code true} if consumed
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showTabButton) {
            int tabX = dialogX - TAB_W + 5 - Math.round((WIDTH + TAB_W + 2) * getProgress(System.currentTimeMillis()));
            if (mouseX >= tabX
                    && mouseX < tabX + TAB_W + 1
                    && mouseY >= dialogY + 15
                    && mouseY < dialogY + 15 + TAB_H) {
                toggle();
                return true;
            }
        }

        if (getProgress(System.currentTimeMillis()) < 0.99f) return false;

        int drawerX = dialogX - TAB_W - WIDTH;
        int drawerTop = dialogY + 15;
        int drawerH = contentHeight + 2 * CORNER;
        if (mouseX < drawerX || mouseX >= dialogX) return false;
        if (mouseY < drawerTop || mouseY >= drawerTop + drawerH) return false;

        for (int i = 0; i < panes.size(); i++) {
            int paneAbsY = dialogY + 15 + paneYOffsets[i];
            int paneH = panes.get(i).preferredHeight();
            if (mouseY >= paneAbsY && mouseY < paneAbsY + paneH) {
                return panes.get(i).mouseClicked(mouseX - drawerX - SIDE_PAD, mouseY - paneAbsY, button);
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
