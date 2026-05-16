package net.bobofraggins.tremendousstorage.shared.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * A 16×16 icon button that shows a normal sprite at rest and a pressed sprite while the mouse
 * button is held down. No hover/focus state change — the button only reacts visually to a click.
 */
public class PressableIconButton extends AbstractWidget {

    private final Identifier normalSprite;
    private final Identifier pressedSprite;
    private final Runnable onPress;
    private boolean pressing = false;

    public PressableIconButton(
            int x, int y, int width, int height, Identifier normal, Identifier pressed, Runnable onPress) {
        super(x, y, width, height, Component.empty());
        this.normalSprite = normal;
        this.pressedSprite = pressed;
        this.onPress = onPress;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED, pressing ? pressedSprite : normalSprite, getX(), getY(), width, height);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean consumed) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        if (button == 0 && isActive() && isMouseOver(mouseX, mouseY)) {
            pressing = true;
            onPress.run();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        if (button == 0) pressing = false;
        return super.mouseReleased(event);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }
}
