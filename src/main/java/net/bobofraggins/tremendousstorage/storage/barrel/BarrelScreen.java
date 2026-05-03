package net.bobofraggins.tremendousstorage.storage.barrel;

import net.bobofraggins.tremendousstorage.shared.priority.Priority;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class BarrelScreen extends AbstractContainerScreen<BarrelMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    private static final int GUI_W = 176;
    private static final int GUI_H = 154;

    // Widget layout (relative to topPos / leftPos)
    private static final int LABEL_X = 8;
    private static final int PRIORITY_Y = 20;
    private static final int VOID_Y = 50;
    private static final int INV_Y = 72; // player inventory top
    private static final int HOTBAR_Y = 130;

    private Button priorityUpBtn;
    private Button priorityDownBtn;
    private Button voidExcessBtn;

    public BarrelScreen(BarrelMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = GUI_W;
        imageHeight = GUI_H;
        inventoryLabelY = INV_Y - 11;
    }

    @Override
    protected void init() {
        super.init();

        int cx = leftPos;
        int cy = topPos;

        priorityUpBtn = addRenderableWidget(Button.builder(Component.literal("▲"), btn -> changePriority(1))
                .pos(cx + GUI_W - 36, cy + PRIORITY_Y)
                .size(14, 14)
                .build());

        priorityDownBtn = addRenderableWidget(Button.builder(Component.literal("▼"), btn -> changePriority(-1))
                .pos(cx + GUI_W - 20, cy + PRIORITY_Y)
                .size(14, 14)
                .build());

        voidExcessBtn = addRenderableWidget(Button.builder(voidLabel(), btn -> toggleVoidExcess())
                .pos(cx + LABEL_X, cy + VOID_Y)
                .size(160, 14)
                .build());
    }

    private void changePriority(int delta) {
        Priority current = menu.getPriority();
        int next = Math.clamp(current.ordinal() + delta, 0, Priority.VALUES.length - 1);
        PacketDistributor.sendToServer(
                new net.bobofraggins.tremendousstorage.shared.network.SetPriorityPacket(menu.getPos(), next));
    }

    private void toggleVoidExcess() {
        PacketDistributor.sendToServer(new net.bobofraggins.tremendousstorage.shared.network.SetVoidExcessPacket(
                menu.getPos(), !menu.isVoidExcess()));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (voidExcessBtn != null) voidExcessBtn.setMessage(voidLabel());
    }

    private Component voidLabel() {
        String suffix = menu.isVoidExcess()
                ? Component.translatable("screen.tremendousstorage.void_excess_on")
                        .getString()
                : Component.translatable("screen.tremendousstorage.void_excess_off")
                        .getString();
        return Component.literal(suffix);
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mx, int my) {
        // Draw player inventory background
        gfx.blit(TEXTURE, leftPos, topPos + INV_Y - 17, 0, 0, imageWidth, 96);
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mx, int my) {
        // Title
        gfx.drawString(font, title, (imageWidth - font.width(title)) / 2, 7, 0x404040, false);

        // Inventory label
        gfx.drawString(font, inventoryLabelText(), LABEL_X, inventoryLabelY, 0x404040, false);

        // Priority label
        Component priorityLabel = Component.translatable("screen.tremendousstorage.priority_label");
        gfx.drawString(font, priorityLabel, LABEL_X, PRIORITY_Y + 3, 0x404040, false);

        Component priorityName = Component.translatable(menu.getPriority().translationKey());
        int nameX = GUI_W - 36 - 4 - font.width(priorityName);
        gfx.drawString(font, priorityName, nameX, PRIORITY_Y + 3, 0x404040, false);
    }

    private Component inventoryLabelText() {
        return Component.translatable("container.inventory");
    }

    @Override
    public void render(GuiGraphics gfx, int mx, int my, float partialTick) {
        renderBackground(gfx, mx, my, partialTick);
        super.render(gfx, mx, my, partialTick);
        renderTooltip(gfx, mx, my);
    }
}
