package net.bobofraggins.tremendousstorage.storage.tubeattachments;

import java.util.List;
import net.bobofraggins.tremendousstorage.shared.network.SetImportExportFilterPacket;
import net.bobofraggins.tremendousstorage.shared.ui.Dialog;
import net.bobofraggins.tremendousstorage.shared.ui.PlayerInventoryPane;
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
 * <p>Layout uses the standard {@link Dialog} frame with a blank filter pane (176×102) and a
 * {@link PlayerInventoryPane} below it.
 */
public class ImportInterfaceScreen extends AbstractContainerScreen<ImportInterfaceMenu> {

    /** Width of the filter pane / dialog — also PlayerInventoryPane.WIDTH. */
    private static final int FILTER_PANE_W = 176;

    /** Height of the filter pane (label + 3×3 grid + mode button + padding). */
    private static final int FILTER_PANE_H = 102;

    /** Left edge of the 3×3 ghost grid, relative to leftPos. */
    private static final int GRID_X = (FILTER_PANE_W - 3 * 18) / 2; // 61

    /** Top edge of the 3×3 ghost grid in screen-absolute Y = topPos + GRID_SCREEN_DY. */
    private static final int GRID_SCREEN_DY = Dialog.TITLE_H + 16; // 33

    private static final int SLOT_SIZE = 18;

    /** "Filter:" label screen-absolute Y = topPos + LABEL_SCREEN_DY. */
    private static final int LABEL_SCREEN_DY = Dialog.TITLE_H + 4; // 21

    /** Mode button screen-absolute Y = topPos + BTN_SCREEN_DY. */
    private static final int BTN_SCREEN_DY = Dialog.TITLE_H + 76; // 93

    private static final int MODE_BTN_W = 120;
    private static final int MODE_BTN_H = 20;

    private final Dialog dialog;
    private Button modeButton;

    public ImportInterfaceScreen(ImportInterfaceMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        dialog = new Dialog(
                Dialog.blankPane(FILTER_PANE_W, FILTER_PANE_H), new PlayerInventoryPane(ImportInterfaceMenu.INV_X));
        this.imageWidth = dialog.totalWidth();
        this.imageHeight = dialog.totalHeight();
    }

    @Override
    protected void init() {
        super.init();
        dialog.init(leftPos, topPos);
        int btnX = leftPos + (FILTER_PANE_W - MODE_BTN_W) / 2;
        int btnY = topPos + BTN_SCREEN_DY;
        modeButton = addRenderableWidget(Button.builder(modeLabel(), btn -> {
                    PacketDistributor.sendToServer(
                            new SetImportExportFilterPacket(menu.getPos(), menu.getFaceIndex(), -1, ItemStack.EMPTY));
                })
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
        dialog.render(graphics, font, title, mouseX, mouseY, partialTick);

        // "Filter:" label
        Component filterLabel = Component.translatable("screen.tremendousstorage.filter_label");
        graphics.drawString(
                font,
                filterLabel,
                leftPos + (FILTER_PANE_W - font.width(filterLabel)) / 2,
                topPos + LABEL_SCREEN_DY,
                0xAAAAAA,
                false);

        // Ghost slot backgrounds and item icons
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

    // -------------------------------------------------------------------------
    // Mouse interaction
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Ghost slot screen coordinates (used by JEI ghost ingredient handler)
    // -------------------------------------------------------------------------

    /** Returns the screen-absolute x of the left edge of the given ghost slot (0-8). */
    public int getGhostSlotX(int slotIndex) {
        return leftPos + GRID_X + (slotIndex % 3) * SLOT_SIZE + 1;
    }

    /** Returns the screen-absolute y of the top edge of the given ghost slot (0-8). */
    public int getGhostSlotY(int slotIndex) {
        return topPos + GRID_SCREEN_DY + (slotIndex / 3) * SLOT_SIZE + 1;
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
