package net.bobofraggins.tremendousstorage.shared.ui;

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.bobofraggins.tremendousstorage.shared.config.SortMode;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Config-drawer pane that displays the current sort mode with left/right navigation buttons.
 *
 * <p>Wraps around (no disabled state). Layout matches {@link PriorityPane}: label row on top,
 * [◄] [value label] [►] row below.
 */
public class SortPane implements IDialogPane {

    private static final int PANE_HEIGHT = 35;
    private static final int LABEL_Y = 3;
    private static final int ROW_Y = 14;
    private static final int BTN_W = 16;
    private static final int BTN_H = 16;
    private static final int LBL_W = 56;
    private static final int GAP = 2;
    private static final int ROW_W = BTN_W + GAP + LBL_W + GAP + BTN_W; // 92

    private static final ResourceLocation BTN_LEFT =
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "textures/gui/widget/button_left.png");
    private static final ResourceLocation BTN_LEFT_FOCUSED =
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "textures/gui/widget/button_left_focused.png");
    private static final ResourceLocation BTN_RIGHT =
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "textures/gui/widget/button_right.png");
    private static final ResourceLocation BTN_RIGHT_FOCUSED =
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "textures/gui/widget/button_right_focused.png");

    private final Supplier<SortMode> getter;
    private final Consumer<SortMode> setter;
    private int lastRenderedWidth = ConfigDrawer.WIDTH;

    public SortPane(Supplier<SortMode> getter, Consumer<SortMode> setter) {
        this.getter = getter;
        this.setter = setter;
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
        lastRenderedWidth = width;
        int rowX = (width - ROW_W) / 2;
        int leftBtnX = rowX;
        int rightBtnX = rowX + BTN_W + GAP + LBL_W + GAP;
        int lblX = rowX + BTN_W + GAP;

        Component label = Component.translatable("screen.tremendousstorage.sort_label");
        graphics.drawString(font, label, (width - font.width(label)) / 2, LABEL_Y, 0x404040, false);

        ResourceLocation leftTex = isInButton(localMouseX, localMouseY, leftBtnX) ? BTN_LEFT_FOCUSED : BTN_LEFT;
        ResourceLocation rightTex = isInButton(localMouseX, localMouseY, rightBtnX) ? BTN_RIGHT_FOCUSED : BTN_RIGHT;
        graphics.blit(RenderType::guiTextured, leftTex, leftBtnX, ROW_Y, 0, 0, BTN_W, BTN_H, BTN_W, BTN_H);
        graphics.blit(RenderType::guiTextured, rightTex, rightBtnX, ROW_Y, 0, 0, BTN_W, BTN_H, BTN_W, BTN_H);

        graphics.fill(lblX, ROW_Y, lblX + LBL_W, ROW_Y + BTN_H, 0xFFC6C6C6);

        String name = getter.get().displayName();
        int nameX = lblX + (LBL_W - font.width(name)) / 2;
        int nameY = ROW_Y + (BTN_H - 8) / 2;
        graphics.drawString(font, name, nameX, nameY, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(double localX, double localY, int button) {
        if (button != 0) return false;
        int rowX = (lastRenderedWidth - ROW_W) / 2;
        int leftBtnX = rowX;
        int rightBtnX = rowX + BTN_W + GAP + LBL_W + GAP;

        if (isInButton(localX, localY, leftBtnX)) {
            setter.accept(getter.get().prev());
            return true;
        }
        if (isInButton(localX, localY, rightBtnX)) {
            setter.accept(getter.get().next());
            return true;
        }
        return false;
    }

    private static boolean isInButton(double lx, double ly, int btnX) {
        return lx >= btnX && lx < btnX + BTN_W && ly >= ROW_Y && ly < ROW_Y + BTN_H;
    }
}
