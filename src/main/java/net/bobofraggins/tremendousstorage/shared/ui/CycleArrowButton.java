package net.bobofraggins.tremendousstorage.shared.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * A small clickable button that renders as a pointing triangle (▲ or ▼) using fill commands.
 *
 * <p>The triangle is 7 pixels wide and 4 pixels tall, centered in the button's bounds.
 * Color shifts from gray to white on hover.
 */
public class CycleArrowButton extends AbstractWidget {

    private final boolean pointsUp;
    private final Runnable onPress;

    public CycleArrowButton(int x, int y, int width, int height, boolean pointsUp, Runnable onPress) {
        super(x, y, width, height, Component.empty());
        this.pointsUp = pointsUp;
        this.onPress = onPress;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int color = isMouseOver(mouseX, mouseY) ? 0xFFFFFFFF : 0xFFAAAAAA;
        int cx = getX() + width / 2;
        int cy = getY() + height / 2;

        if (pointsUp) {
            int tip = cy - 2;
            graphics.fill(cx, tip, cx + 1, tip + 1, color);
            graphics.fill(cx - 1, tip + 1, cx + 2, tip + 2, color);
            graphics.fill(cx - 2, tip + 2, cx + 3, tip + 3, color);
            graphics.fill(cx - 3, tip + 3, cx + 4, tip + 4, color);
        } else {
            int tip = cy + 1;
            graphics.fill(cx - 3, tip - 4, cx + 4, tip - 3, color);
            graphics.fill(cx - 2, tip - 3, cx + 3, tip - 2, color);
            graphics.fill(cx - 1, tip - 2, cx + 2, tip - 1, color);
            graphics.fill(cx, tip - 1, cx + 1, tip, color);
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean consumed) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        if (button == 0 && isActive() && isMouseOver(mouseX, mouseY)) {
            onPress.run();
            return true;
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
        defaultButtonNarrationText(out);
    }
}
