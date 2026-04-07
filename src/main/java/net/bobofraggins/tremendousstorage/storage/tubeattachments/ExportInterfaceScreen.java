package net.bobofraggins.tremendousstorage.storage.tubeattachments;

import java.util.List;
import net.bobofraggins.tremendousstorage.shared.network.SetImportExportFilterPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Filter screen for an Export Interface attachment.
 *
 * <p>Visually and functionally identical to {@link ImportInterfaceScreen};
 * uses {@link ExportInterfaceMenu} instead.
 */
public class ExportInterfaceScreen extends AbstractContainerScreen<ExportInterfaceMenu> {

    private static final int BG_WIDTH = 176;
    private static final int BG_HEIGHT = 116;

    private static final int GRID_X = (BG_WIDTH - 3 * 18) / 2;
    private static final int GRID_Y = 30;
    private static final int SLOT_SIZE = 18;
    private static final int MODE_BTN_Y = 88;
    private static final int MODE_BTN_W = 120;
    private static final int MODE_BTN_H = 20;

    private Button modeButton;

    public ExportInterfaceScreen(ExportInterfaceMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = BG_WIDTH;
        this.imageHeight = BG_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        int btnX = leftPos + (BG_WIDTH - MODE_BTN_W) / 2;
        int btnY = topPos + MODE_BTN_Y;
        modeButton = addRenderableWidget(Button.builder(
                        modeLabel(),
                        btn -> PacketDistributor.sendToServer(new SetImportExportFilterPacket(
                                menu.getPos(), menu.getFaceIndex(), -1, ItemStack.EMPTY)))
                .bounds(btnX, btnY, MODE_BTN_W, MODE_BTN_H)
                .build());
    }

    private Component modeLabel() {
        return menu.isRejectMode()
                ? Component.translatable("screen.tremendousstorage.filter_reject")
                : Component.translatable("screen.tremendousstorage.filter_accept");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (modeButton != null) modeButton.setMessage(modeLabel());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + BG_WIDTH, topPos + BG_HEIGHT, 0xC0101010);

        Component filterLabel = Component.translatable("screen.tremendousstorage.filter_label");
        graphics.drawString(
                font, filterLabel, leftPos + (BG_WIDTH - font.width(filterLabel)) / 2, topPos + 20, 0xAAAAAA, false);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int sx = leftPos + GRID_X + col * SLOT_SIZE;
                int sy = topPos + GRID_Y + row * SLOT_SIZE;
                int slotIndex = row * 3 + col;

                graphics.fill(sx, sy, sx + SLOT_SIZE, sy + SLOT_SIZE, 0xFF303030);
                graphics.fill(sx + 1, sy + 1, sx + SLOT_SIZE - 1, sy + SLOT_SIZE - 1, 0xFF1A1A1A);

                ItemStack stack = menu.getFilterSlot(slotIndex);
                if (!stack.isEmpty()) {
                    graphics.renderItem(stack, sx + 1, sy + 1);
                }
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, (BG_WIDTH - font.width(title)) / 2, 6, 0xFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int sx = leftPos + GRID_X + col * SLOT_SIZE + 1;
                int sy = topPos + GRID_Y + row * SLOT_SIZE + 1;
                if (mouseX >= sx && mouseX < sx + SLOT_SIZE - 2 && mouseY >= sy && mouseY < sy + SLOT_SIZE - 2) {
                    int slotIndex = row * 3 + col;
                    handleGhostSlotClick(slotIndex);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleGhostSlotClick(int slotIndex) {
        ItemStack carried = minecraft.player.containerMenu.getCarried();
        if (carried.isEmpty()) {
            menu.setFilterSlot(slotIndex, ItemStack.EMPTY);
            PacketDistributor.sendToServer(
                    new SetImportExportFilterPacket(menu.getPos(), menu.getFaceIndex(), slotIndex, ItemStack.EMPTY));
        } else {
            ItemStack ghost = carried.copyWithCount(1);
            menu.setFilterSlot(slotIndex, ghost);
            PacketDistributor.sendToServer(
                    new SetImportExportFilterPacket(menu.getPos(), menu.getFaceIndex(), slotIndex, ghost));
        }
    }

    public int getGhostSlotX(int slotIndex) {
        return leftPos + GRID_X + (slotIndex % 3) * SLOT_SIZE + 1;
    }

    public int getGhostSlotY(int slotIndex) {
        return topPos + GRID_Y + (slotIndex / 3) * SLOT_SIZE + 1;
    }

    public static int getGhostSlotInnerSize() {
        return SLOT_SIZE - 2;
    }

    public void applySync(List<ItemStack> slots, boolean rejectMode) {
        menu.applySync(slots, rejectMode);
        if (modeButton != null) modeButton.setMessage(modeLabel());
    }
}
