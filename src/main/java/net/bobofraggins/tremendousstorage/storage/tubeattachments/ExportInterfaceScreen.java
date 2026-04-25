package net.bobofraggins.tremendousstorage.storage.tubeattachments;

import java.util.List;
import net.bobofraggins.tremendousstorage.shared.network.SetImportExportFilterPacket;
import net.bobofraggins.tremendousstorage.shared.ui.Dialog;
import net.bobofraggins.tremendousstorage.shared.ui.PlayerInventoryPane;
import net.minecraft.client.gui.GuiGraphics;
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

    private static final int FILTER_PANE_W = 176;
    private static final int FILTER_PANE_H = 76;
    private static final int GRID_X = (FILTER_PANE_W - 3 * 18) / 2; // 61
    private static final int GRID_SCREEN_DY = Dialog.TITLE_H + 16; // 33
    private static final int SLOT_SIZE = 18;
    private static final int LABEL_SCREEN_DY = Dialog.TITLE_H + 4; // 21

    private final Dialog dialog;

    public ExportInterfaceScreen(ExportInterfaceMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        dialog = new Dialog(
                Dialog.blankPane(FILTER_PANE_W, FILTER_PANE_H), new PlayerInventoryPane(ExportInterfaceMenu.INV_X));
        this.imageWidth = dialog.totalWidth();
        this.imageHeight = dialog.totalHeight();
    }

    @Override
    protected void init() {
        super.init();
        dialog.init(leftPos, topPos);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        dialog.render(graphics, font, title, mouseX, mouseY, partialTick);

        Component filterLabel = Component.translatable("screen.tremendousstorage.filter_label");
        graphics.drawString(
                font,
                filterLabel,
                leftPos + (FILTER_PANE_W - font.width(filterLabel)) / 2,
                topPos + LABEL_SCREEN_DY,
                0xAAAAAA,
                false);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int sx = leftPos + GRID_X + col * SLOT_SIZE;
                int sy = topPos + GRID_SCREEN_DY + row * SLOT_SIZE;
                int slotIndex = row * 3 + col;

                drawSlot(graphics, sx, sy);

                ItemStack stack = menu.getFilterSlot(slotIndex);
                if (!stack.isEmpty()) {
                    graphics.renderItem(stack, sx + 1, sy + 1);
                }
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Title is drawn by Dialog.
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int sx = leftPos + GRID_X + col * SLOT_SIZE + 1;
                int sy = topPos + GRID_SCREEN_DY + row * SLOT_SIZE + 1;
                if (mouseX >= sx && mouseX < sx + SLOT_SIZE - 2 && mouseY >= sy && mouseY < sy + SLOT_SIZE - 2) {
                    handleGhostSlotClick(row * 3 + col);
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

    private static void drawSlot(GuiGraphics g, int sx, int sy) {
        g.fill(sx, sy, sx + 16, sy + 1, 0xFF373737); // top
        g.fill(sx, sy + 1, sx + 1, sy + 16, 0xFF373737); // left
        g.fill(sx, sy + 16, sx + 17, sy + 17, 0xFFFFFFFF); // bottom
        g.fill(sx + 16, sy, sx + 17, sy + 16, 0xFFFFFFFF); // right
        g.fill(sx + 1, sy + 1, sx + 16, sy + 16, 0xFF8B8B8B); // interior
    }

    public int getGhostSlotX(int slotIndex) {
        return leftPos + GRID_X + (slotIndex % 3) * SLOT_SIZE + 1;
    }

    public int getGhostSlotY(int slotIndex) {
        return topPos + GRID_SCREEN_DY + (slotIndex / 3) * SLOT_SIZE + 1;
    }

    public static int getGhostSlotInnerSize() {
        return SLOT_SIZE - 2;
    }

    public void applySync(List<ItemStack> slots) {
        menu.applySync(slots);
    }
}
