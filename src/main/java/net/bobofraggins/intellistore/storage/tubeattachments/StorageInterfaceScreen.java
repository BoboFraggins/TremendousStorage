package net.bobofraggins.intellistore.storage.tubeattachments;

import net.bobofraggins.intellistore.shared.network.SetStorageInterfacePriorityPacket;
import net.bobofraggins.intellistore.shared.priority.Priority;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Priority screen for a Storage Interface attachment on a Tube face.
 *
 * <p>Background panel: 176 × 50 pixels. Shows the attachment title and a
 * [▼] [Current Priority Name] [▲] row, matching the style of
 * {@link net.bobofraggins.intellistore.shared.ui.PriorityScreen}.
 */
public class StorageInterfaceScreen extends AbstractContainerScreen<StorageInterfaceMenu> {

    private static final ResourceLocation BG_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    private static final int BG_WIDTH = 176;
    private static final int BG_HEIGHT = 50;

    private static final int LABEL_Y = 20;
    private static final int ROW_Y = 31;
    private static final int BTN_W = 20;
    private static final int BTN_H = 14;
    private static final int LBL_W = 56;
    private static final int GAP = 2;
    private static final int ROW_W = BTN_W + GAP + LBL_W + GAP + BTN_W;

    private Button downBtn;
    private Button upBtn;

    public StorageInterfaceScreen(StorageInterfaceMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = BG_WIDTH;
        this.imageHeight = BG_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        int rowX = leftPos + (BG_WIDTH - ROW_W) / 2;
        int btnY = topPos + ROW_Y;

        // ▼ — decrease priority
        downBtn = addRenderableWidget(Button.builder(Component.literal("▼"), btn -> {
                    int next = Math.max(0, menu.getPriority() - 1);
                    PacketDistributor.sendToServer(
                            new SetStorageInterfacePriorityPacket(menu.getPos(), menu.getFaceIndex(), next));
                })
                .bounds(rowX, btnY, BTN_W, BTN_H)
                .build());

        // ▲ — increase priority
        upBtn = addRenderableWidget(Button.builder(Component.literal("▲"), btn -> {
                    int next = Math.min(Priority.VALUES.length - 1, menu.getPriority() + 1);
                    PacketDistributor.sendToServer(
                            new SetStorageInterfacePriorityPacket(menu.getPos(), menu.getFaceIndex(), next));
                })
                .bounds(rowX + BTN_W + GAP + LBL_W + GAP, btnY, BTN_W, BTN_H)
                .build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        int selected = menu.getPriority();
        if (downBtn != null) downBtn.active = (selected > 0);
        if (upBtn != null) upBtn.active = (selected < Priority.VALUES.length - 1);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;

        // Title bar from generic_54
        graphics.blit(BG_TEXTURE, x, y, 0, 0, BG_WIDTH, 17);

        // Gray panel body
        graphics.fill(x, y + 17, x + BG_WIDTH, y + BG_HEIGHT, 0xFFC6C6C6);

        // Border lines
        graphics.fill(x, y + 17, x + 1, y + BG_HEIGHT, 0xFF555555);
        graphics.fill(x + BG_WIDTH - 1, y + 17, x + BG_WIDTH, y + BG_HEIGHT, 0xFFFFFFFF);
        graphics.fill(x, y + BG_HEIGHT - 1, x + BG_WIDTH, y + BG_HEIGHT, 0xFF555555);

        // "Priority:" label row
        Component priorityLabel = Component.translatable("screen.intellistore.priority_label");
        graphics.drawString(
                font, priorityLabel, x + (BG_WIDTH - font.width(priorityLabel)) / 2, y + LABEL_Y, 0x404040, false);

        // Current priority name in inset label area
        int rowX = x + (BG_WIDTH - ROW_W) / 2;
        int lblX0 = rowX + BTN_W + GAP;
        int lblY = y + ROW_Y;

        graphics.fill(lblX0, lblY, lblX0 + LBL_W, lblY + 1, 0xFF373737);
        graphics.fill(lblX0, lblY + 1, lblX0 + 1, lblY + BTN_H, 0xFF373737);
        graphics.fill(lblX0, lblY + BTN_H, lblX0 + LBL_W + 1, lblY + BTN_H + 1, 0xFFFFFFFF);
        graphics.fill(lblX0 + LBL_W, lblY, lblX0 + LBL_W + 1, lblY + BTN_H, 0xFFFFFFFF);
        graphics.fill(lblX0 + 1, lblY + 1, lblX0 + LBL_W, lblY + BTN_H, 0xFF8B8B8B);

        Priority current = Priority.fromOrdinal(menu.getPriority());
        String name = Component.translatable(current.translationKey()).getString();
        int nameX = lblX0 + (LBL_W - font.width(name)) / 2;
        int nameY = lblY + (BTN_H - 8) / 2;
        graphics.drawString(font, name, nameX, nameY, 0x404040, false);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, (BG_WIDTH - font.width(title)) / 2, 4, 0x404040, false);
    }
}
