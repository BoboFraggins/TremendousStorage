package net.bobofraggins.intellistore.storage.tubeattachments;

import java.util.List;
import net.bobofraggins.intellistore.shared.network.SetImportExportFilterPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Filter screen for an Import Interface attachment.
 *
 * <p>Layout (176 × 116 px):
 * <ul>
 *   <li>Title centred at y=6
 *   <li>"Filter:" label at y=20
 *   <li>3×3 grid of 18×18 ghost slots at x=29, y=30 (centred in 176px)
 *   <li>"Mode: Accept" / "Mode: Reject" toggle button, 120×20 centred at y=88
 * </ul>
 */
public class ImportInterfaceScreen extends AbstractContainerScreen<ImportInterfaceMenu> {

    private static final int BG_WIDTH = 176;
    private static final int BG_HEIGHT = 116;

    /** Top-left of the 3×3 ghost grid (relative to leftPos/topPos). */
    private static final int GRID_X = (BG_WIDTH - 3 * 18) / 2; // 29

    private static final int GRID_Y = 30;

    private static final int SLOT_SIZE = 18;
    private static final int MODE_BTN_Y = 88;
    private static final int MODE_BTN_W = 120;
    private static final int MODE_BTN_H = 20;

    private Button modeButton;

    public ImportInterfaceScreen(ImportInterfaceMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = BG_WIDTH;
        this.imageHeight = BG_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        int btnX = leftPos + (BG_WIDTH - MODE_BTN_W) / 2;
        int btnY = topPos + MODE_BTN_Y;
        modeButton = addRenderableWidget(Button.builder(modeLabel(), btn -> {
                    PacketDistributor.sendToServer(
                            new SetImportExportFilterPacket(menu.getPos(), menu.getFaceIndex(), -1, ItemStack.EMPTY));
                })
                .bounds(btnX, btnY, MODE_BTN_W, MODE_BTN_H)
                .build());
    }

    private Component modeLabel() {
        return menu.isRejectMode()
                ? Component.translatable("screen.intellistore.filter_reject")
                : Component.translatable("screen.intellistore.filter_accept");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (modeButton != null) modeButton.setMessage(modeLabel());
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
        graphics.drawString(
                font, filterLabel, leftPos + (BG_WIDTH - font.width(filterLabel)) / 2, topPos + 20, 0xAAAAAA, false);

        // Draw ghost slot backgrounds and item icons
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int sx = leftPos + GRID_X + col * SLOT_SIZE;
                int sy = topPos + GRID_Y + row * SLOT_SIZE;
                int slotIndex = row * 3 + col;

                // Dark recessed slot background
                graphics.fill(sx, sy, sx + SLOT_SIZE, sy + SLOT_SIZE, 0xFF303030);
                graphics.fill(sx + 1, sy + 1, sx + SLOT_SIZE - 1, sy + SLOT_SIZE - 1, 0xFF1A1A1A);

                // Item icon (ghost — rendered but not interactive as a real Slot)
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

    // -------------------------------------------------------------------------
    // Mouse interaction
    // -------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check ghost slot clicks before delegating to widgets/super
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
            // Clear this slot
            menu.setFilterSlot(slotIndex, ItemStack.EMPTY);
            PacketDistributor.sendToServer(
                    new SetImportExportFilterPacket(menu.getPos(), menu.getFaceIndex(), slotIndex, ItemStack.EMPTY));
        } else {
            // Set this slot to the carried item type (count 1, ghost)
            ItemStack ghost = carried.copyWithCount(1);
            menu.setFilterSlot(slotIndex, ghost);
            PacketDistributor.sendToServer(
                    new SetImportExportFilterPacket(menu.getPos(), menu.getFaceIndex(), slotIndex, ghost));
        }
    }

    // -------------------------------------------------------------------------
    // Ghost slot screen coordinates (used by JEI ghost ingredient handler)
    // -------------------------------------------------------------------------

    /** Returns the screen-absolute x of the left edge of the given ghost slot (0-8). */
    public int getGhostSlotX(int slotIndex) {
        return leftPos + GRID_X + (slotIndex % 3) * SLOT_SIZE + 1;
    }

    /** Returns the screen-absolute y of the top edge of the given ghost slot (0-8). */
    public int getGhostSlotY(int slotIndex) {
        return topPos + GRID_Y + (slotIndex / 3) * SLOT_SIZE + 1;
    }

    /** Inner size of each ghost slot cell (for JEI target rectangles). */
    public static int getGhostSlotInnerSize() {
        return SLOT_SIZE - 2;
    }

    // -------------------------------------------------------------------------
    // Sync from server
    // -------------------------------------------------------------------------

    public void applySync(List<ItemStack> slots, boolean rejectMode) {
        menu.applySync(slots, rejectMode);
        if (modeButton != null) modeButton.setMessage(modeLabel());
    }
}
