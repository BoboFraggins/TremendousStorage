package net.bobofraggins.intellistore.shared.ui;

import net.bobofraggins.intellistore.shared.input.QuickStackClientEvents;
import net.bobofraggins.intellistore.shared.network.QuickStackFilingCabinetPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Shared screen base for
 * {@link net.bobofraggins.intellistore.storage.filingcabinet.FilingCabinetScreen} and
 * {@link net.bobofraggins.intellistore.storage.personalfilingcabinet.PersonalFilingCabinetScreen}.
 *
 * <p>Handles the slide-out config drawer (toggled by a "≡" button in the title bar), folder and
 * extraction slot backgrounds, ghost rendering for locked-but-empty extraction slots, title label,
 * and the standard render pipeline.
 *
 * <p>Subclasses supply the background height, player inventory y, and a {@link ConfigDrawer}
 * populated with whatever config controls they expose (e.g. void excess, priority).
 */
public abstract class AbstractFilingCabinetScreen<M extends AbstractFilingCabinetMenu>
        extends AbstractContainerScreen<M> {

    protected static final int BG_WIDTH = 176;

    private static final PlayerInventoryPane PLAYER_INV_PANE = new PlayerInventoryPane();

    /** Y position of the player inventory rows (relative to topPos), set by subclass. */
    private final int playerInvY;

    private final ConfigDrawer configDrawer;
    private Dialog dialog;

    protected AbstractFilingCabinetScreen(
            M menu, Inventory inv, Component title, int bgHeight, int playerInvY, ConfigDrawer configDrawer) {
        super(menu, inv, title);
        this.playerInvY = playerInvY;
        this.configDrawer = configDrawer;
        dialog = new Dialog(Dialog.blankPane(BG_WIDTH, bgHeight - Dialog.TITLE_H - Dialog.BOTTOM_PADDING));
        this.imageWidth = dialog.totalWidth();
        this.imageHeight = dialog.totalHeight();
    }

    @Override
    protected void init() {
        super.init();
        dialog.init(leftPos, topPos);
        configDrawer.init(leftPos, topPos, imageHeight);

        // Config toggle button in the top-left of the title bar
        addRenderableWidget(new ImageButton(
                leftPos + 3,
                topPos + 1,
                16,
                16,
                new WidgetSprites(
                        ResourceLocation.fromNamespaceAndPath("intellistore", "widget/button_config"),
                        ResourceLocation.fromNamespaceAndPath("intellistore", "widget/button_config_focused")),
                btn -> configDrawer.toggle()));

        // Quick stack button above the player inventory, right-aligned
        addRenderableWidget(new ImageButton(
                leftPos + BG_WIDTH - 26,
                topPos + playerInvY - 20,
                16,
                16,
                new WidgetSprites(
                        ResourceLocation.fromNamespaceAndPath("intellistore", "widget/button_quick_stack"),
                        ResourceLocation.fromNamespaceAndPath("intellistore", "widget/button_quick_stack_focused")),
                btn -> PacketDistributor.sendToServer(new QuickStackFilingCabinetPacket())));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (configDrawer.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (QuickStackClientEvents.QUICK_STACK != null
                && QuickStackClientEvents.QUICK_STACK.matches(keyCode, scanCode)) {
            PacketDistributor.sendToServer(new QuickStackFilingCabinetPacket());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
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

        // Drawer renders first so the dialog's left border appears on top of it
        configDrawer.render(graphics, font, mouseX, mouseY, partialTick);
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
