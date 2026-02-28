package net.bobofraggins.intellistore.ui;

import net.bobofraggins.intellistore.network.SetImportExportFilterPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Filter screen for a Placer Interface attachment.
 *
 * <p>Layout (176 × 80 px):
 * <ul>
 *   <li>Title centred at y=6
 *   <li>"Filter:" label centred at y=24
 *   <li>Single 18×18 ghost slot centred at y=38
 * </ul>
 */
public class PlacerInterfaceScreen extends AbstractContainerScreen<PlacerInterfaceMenu> {

    private static final int BG_WIDTH  = 176;
    private static final int BG_HEIGHT = 80;

    private static final int SLOT_SIZE = 18;
    /** Centre the single slot horizontally. */
    private static final int SLOT_X = (BG_WIDTH - SLOT_SIZE) / 2; // 79
    private static final int SLOT_Y = 38;

    public PlacerInterfaceScreen(PlacerInterfaceMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = BG_WIDTH;
        this.imageHeight = BG_HEIGHT;
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + BG_WIDTH, topPos + BG_HEIGHT, 0xC0101010);

        // "Filter:" label
        Component filterLabel = Component.translatable("screen.intellistore.filter_label");
        graphics.drawString(font, filterLabel,
                leftPos + (BG_WIDTH - font.width(filterLabel)) / 2, topPos + 24, 0xAAAAAA, false);

        // Ghost slot background
        int sx = leftPos + SLOT_X;
        int sy = topPos  + SLOT_Y;
        graphics.fill(sx, sy, sx + SLOT_SIZE, sy + SLOT_SIZE, 0xFF303030);
        graphics.fill(sx + 1, sy + 1, sx + SLOT_SIZE - 1, sy + SLOT_SIZE - 1, 0xFF1A1A1A);

        // Item icon
        ItemStack stack = menu.getFilterSlot();
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, sx + 1, sy + 1);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, (BG_WIDTH - font.width(title)) / 2, 6, 0xFFFFFF, false);
    }

    // -------------------------------------------------------------------------
    // Mouse interaction
    // -------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int sx = leftPos + SLOT_X + 1;
        int sy = topPos  + SLOT_Y + 1;
        if (mouseX >= sx && mouseX < sx + SLOT_SIZE - 2
                && mouseY >= sy && mouseY < sy + SLOT_SIZE - 2) {
            handleGhostSlotClick();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleGhostSlotClick() {
        ItemStack carried = minecraft.player.containerMenu.getCarried();
        if (carried.isEmpty()) {
            menu.setFilterSlot(ItemStack.EMPTY);
            PacketDistributor.sendToServer(new SetImportExportFilterPacket(
                    menu.getPos(), menu.getFaceIndex(), 0, ItemStack.EMPTY));
        } else {
            ItemStack ghost = carried.copyWithCount(1);
            menu.setFilterSlot(ghost);
            PacketDistributor.sendToServer(new SetImportExportFilterPacket(
                    menu.getPos(), menu.getFaceIndex(), 0, ghost));
        }
    }

    // -------------------------------------------------------------------------
    // Ghost slot screen coordinates (for JEI ghost ingredient handler)
    // -------------------------------------------------------------------------

    public int getGhostSlotX() { return leftPos + SLOT_X + 1; }
    public int getGhostSlotY() { return topPos  + SLOT_Y + 1; }
    public static int getGhostSlotInnerSize() { return SLOT_SIZE - 2; }

    // -------------------------------------------------------------------------
    // Sync from server
    // -------------------------------------------------------------------------

    public void applySync(ItemStack slot) {
        menu.applySync(slot);
    }
}
