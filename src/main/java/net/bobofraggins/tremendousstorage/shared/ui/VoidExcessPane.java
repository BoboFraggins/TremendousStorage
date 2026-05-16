package net.bobofraggins.tremendousstorage.shared.ui;

import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Config drawer pane for the void-excess toggle.
 *
 * <p>Renders a single toggle button whose label reflects the current on/off state, read live from
 * the supplied {@link BooleanSupplier}. Clicking fires the {@link Runnable} which should toggle
 * the setting and send the appropriate packet to the server.
 */
public class VoidExcessPane implements IDialogPane {

    private static final int PANE_HEIGHT = 36;
    private static final int LABEL_Y = 3;
    private static final int TOGGLE_Y = 14;
    private static final int TOGGLE_W = 64;
    private static final int TOGGLE_H = 16;

    private static final Identifier TOGGLE_ON =
            Identifier.fromNamespaceAndPath("tremendousstorage", "textures/gui/widget/toggle_on.png");
    private static final Identifier TOGGLE_OFF =
            Identifier.fromNamespaceAndPath("tremendousstorage", "textures/gui/widget/toggle_off.png");

    private final BooleanSupplier stateGetter;
    private final Runnable toggleAction;

    public VoidExcessPane(BooleanSupplier stateGetter, Runnable toggleAction) {
        this.stateGetter = stateGetter;
        this.toggleAction = toggleAction;
    }

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
        Component label = Component.translatable("screen.tremendousstorage.void_excess_label");
        graphics.drawString(font, label, (width - font.width(label)) / 2, LABEL_Y, 0x404040, false);

        int toggleX = (width - TOGGLE_W) / 2;
        Identifier tex = stateGetter.getAsBoolean() ? TOGGLE_ON : TOGGLE_OFF;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED, tex, toggleX, TOGGLE_Y, 0, 0, TOGGLE_W, TOGGLE_H, TOGGLE_W, TOGGLE_H);
    }

    @Override
    public boolean mouseClicked(double localX, double localY, int button) {
        if (button != 0) return false;
        int toggleX = (preferredWidth() - TOGGLE_W) / 2;
        if (localX >= toggleX && localX < toggleX + TOGGLE_W && localY >= TOGGLE_Y && localY < TOGGLE_Y + TOGGLE_H) {
            toggleAction.run();
            return true;
        }
        return false;
    }
}
