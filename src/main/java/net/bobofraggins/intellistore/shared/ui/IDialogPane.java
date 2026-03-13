package net.bobofraggins.intellistore.shared.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A self-contained rendered panel within a {@link Dialog}.
 *
 * <p>All rendering and mouse coordinates are <em>pane-local</em>: (0, 0) is the top-left corner
 * of the pane. The {@link Dialog} translates the {@link GuiGraphics} pose stack to the pane's
 * screen-space origin before calling {@link #render}, so implementations can treat (0, 0) as
 * their own origin without knowing where they sit on screen.
 */
public interface IDialogPane {

    /** Preferred height of this pane in pixels. */
    int preferredHeight();

    /**
     * Render this pane.
     *
     * @param graphics    rendering context, pose stack already translated to pane origin
     * @param font        font for text rendering
     * @param width       available width of the pane in pixels
     * @param localMouseX mouse X relative to pane origin
     * @param localMouseY mouse Y relative to pane origin
     * @param partialTick partial tick for animations
     */
    void render(GuiGraphics graphics, Font font, int width, int localMouseX, int localMouseY, float partialTick);

    /**
     * Handle a mouse click in pane-local coordinates.
     *
     * @return true if this pane consumed the event
     */
    default boolean mouseClicked(double localX, double localY, int button) {
        return false;
    }

    /**
     * Handle a mouse scroll in pane-local coordinates.
     *
     * @return true if this pane consumed the event
     */
    default boolean mouseScrolled(double localX, double localY, double dx, double dy) {
        return false;
    }
}
