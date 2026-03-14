package net.bobofraggins.intellistore.shared.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Shared screen base for
 * {@link net.bobofraggins.intellistore.storage.filingcabinet.FilingCabinetScreen} and
 * {@link net.bobofraggins.intellistore.storage.personalfilingcabinet.PersonalFilingCabinetScreen}.
 *
 * <p>Handles the void-excess toggle button, folder/extraction slot backgrounds, ghost rendering
 * for locked-but-empty extraction slots, title label, and the standard render pipeline. Subclasses
 * supply the background height, player inventory y, and the action to fire when the void-excess
 * button is clicked.
 */
public abstract class AbstractFilingCabinetScreen<M extends AbstractFilingCabinetMenu>
        extends AbstractContainerScreen<M> {

    protected static final int BG_WIDTH = 176;

    private static final PlayerInventoryPane PLAYER_INV_PANE = new PlayerInventoryPane();
    private static final int BTN_W = 120;
    private static final int BTN_H = 14;
    private static final int BTN_Y = 18;

    /** Y position of the player inventory rows (relative to topPos), set by subclass. */
    private final int playerInvY;

    private Dialog dialog;
    private Button voidExcessButton;

    protected AbstractFilingCabinetScreen(M menu, Inventory inv, Component title, int bgHeight, int playerInvY) {
        super(menu, inv, title);
        this.playerInvY = playerInvY;
        dialog = new Dialog(Dialog.blankPane(BG_WIDTH, bgHeight - Dialog.TITLE_H - Dialog.BOTTOM_PADDING));
        this.imageWidth = dialog.totalWidth();
        this.imageHeight = dialog.totalHeight();
    }

    /**
     * Returns the action to run when the void-excess button is clicked.
     * Implementations should toggle {@code menu.isVoidExcess()}, call
     * {@code menu.setVoidExcess()}, and send the appropriate packet to the server.
     */
    protected abstract Button.OnPress voidExcessAction();

    @Override
    protected void init() {
        super.init();
        dialog.init(leftPos, topPos);
        int btnX = leftPos + (BG_WIDTH - BTN_W) / 2;
        int btnY = topPos + BTN_Y;
        voidExcessButton = addRenderableWidget(Button.builder(voidExcessLabel(), voidExcessAction())
                .bounds(btnX, btnY, BTN_W, BTN_H)
                .build());
    }

    private Component voidExcessLabel() {
        return menu.isVoidExcess()
                ? Component.translatable("screen.intellistore.void_excess_on")
                : Component.translatable("screen.intellistore.void_excess_off");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (voidExcessButton != null) voidExcessButton.setMessage(voidExcessLabel());
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

        dialog.render(graphics, font, title, mouseX, mouseY, partialTick);

        // Folder slots and extraction slots — 8 vertical rows, two columns
        for (int i = 0; i < AbstractFilingCabinetMenu.FOLDER_SLOTS; i++) {
            int sy = y + AbstractFilingCabinetMenu.FOLDER_Y_START + i * 18;
            drawSlotBackground(graphics, x + AbstractFilingCabinetMenu.FOLDER_X, sy, 16, 16);
            drawSlotBackground(graphics, x + AbstractFilingCabinetMenu.EXTRACTION_X, sy, 16, 16);
        }

        // Player inventory slot backgrounds
        graphics.pose().pushPose();
        graphics.pose().translate(x, y + playerInvY, 0);
        PLAYER_INV_PANE.render(graphics, font, BG_WIDTH, 0, 0, 0);
        graphics.pose().popPose();
    }

    /**
     * Renders each slot; extraction slots that are locked-but-empty get a dimmed ghost icon
     * instead of a normal (empty) slot render.
     */
    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        if (slot instanceof FolderExtractionSlot extractSlot && extractSlot.isGhost()) {
            ItemStack ghost = extractSlot.getGhostItem();
            if (!ghost.isEmpty()) {
                int sx = leftPos + slot.x;
                int sy = topPos + slot.y;
                graphics.renderItem(ghost, sx, sy);
                // Dark overlay to distinguish ghost from a real item
                graphics.fill(sx, sy, sx + 16, sy + 16, 0x80000000);
            }
        } else {
            super.renderSlot(graphics, slot);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Title is drawn by Dialog.
    }

    private static void drawSlotBackground(GuiGraphics graphics, int sx, int sy, int w, int h) {
        graphics.fill(sx, sy, sx + w, sy + 1, 0xFF373737); // top
        graphics.fill(sx, sy + 1, sx + 1, sy + h, 0xFF373737); // left
        graphics.fill(sx, sy + h, sx + w + 1, sy + h + 1, 0xFFFFFFFF); // bottom
        graphics.fill(sx + w, sy, sx + w + 1, sy + h, 0xFFFFFFFF); // right
        graphics.fill(sx + 1, sy + 1, sx + w, sy + h, 0xFF8B8B8B); // interior
    }
}
