package net.bobofraggins.tremendousstorage.shared.ui;

import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.shared.input.QuickStackClientEvents;
import net.bobofraggins.tremendousstorage.shared.network.QuickStackFilingCabinetPacket;
import net.bobofraggins.tremendousstorage.shared.util.CountFormat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Shared screen base for
 * {@link net.bobofraggins.tremendousstorage.storage.filingcabinet.FilingCabinetScreen} and
 * {@link net.bobofraggins.tremendousstorage.storage.personalfilingcabinet.PersonalFilingCabinetScreen}.
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

    private static final PlayerInventoryPane PLAYER_INV_PANE = new PlayerInventoryPane(7);

    /** Y position of the player inventory rows (relative to topPos), set by subclass. */
    private final int playerInvY;

    private final ConfigDrawer configDrawer;
    private Dialog dialog;

    @Nullable
    private Slot shiftDragSlot;

    protected AbstractFilingCabinetScreen(
            M menu, Inventory inv, Component title, int bgHeight, int playerInvY, ConfigDrawer configDrawer) {
        super(menu, inv, title, BG_WIDTH, bgHeight);
        this.playerInvY = playerInvY;
        this.configDrawer = configDrawer;
        dialog = new Dialog(Dialog.blankPane(BG_WIDTH, bgHeight - Dialog.TITLE_H - Dialog.BOTTOM_PADDING));
    }

    @Override
    protected void init() {
        super.init();
        dialog.init(leftPos, topPos);
        configDrawer.init(leftPos, topPos, imageHeight);

        // Quick stack button above the player inventory, right-aligned
        addRenderableWidget(new PressableIconButton(
                leftPos + BG_WIDTH - 26,
                topPos + playerInvY - 20,
                16,
                16,
                Identifier.fromNamespaceAndPath("tremendousstorage", "widget/button_quick_stack"),
                Identifier.fromNamespaceAndPath("tremendousstorage", "widget/button_quick_stack_focused"),
                () -> ClientPacketDistributor.sendToServer(new QuickStackFilingCabinetPacket())));
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean consumed) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        shiftDragSlot = null;
        if (configDrawer.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(event, consumed);
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        if (button == 0
                        && com.mojang.blaze3d.platform.InputConstants.isKeyDown(
                                net.minecraft.client.Minecraft.getInstance().getWindow(),
                                com.mojang.blaze3d.platform.InputConstants.KEY_LSHIFT)
                || com.mojang.blaze3d.platform.InputConstants.isKeyDown(
                                net.minecraft.client.Minecraft.getInstance().getWindow(),
                                com.mojang.blaze3d.platform.InputConstants.KEY_RSHIFT)
                        && menu.getCarried().isEmpty()) {
            Slot slot = hoveredSlot;
            if (slot != null && slot != shiftDragSlot && slot.hasItem()) {
                shiftDragSlot = slot;
                slotClicked(slot, slot.index, 0, ContainerInput.QUICK_MOVE);
            }
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        shiftDragSlot = null;
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key();
        int scanCode = event.scancode();
        int modifiers = event.modifiers();

        if (QuickStackClientEvents.QUICK_STACK != null && QuickStackClientEvents.QUICK_STACK.matches(event)) {
            ClientPacketDistributor.sendToServer(new QuickStackFilingCabinetPacket());
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractBackground(graphics, mouseX, mouseY, partialTick);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = leftPos, y = topPos;

        // Drawer renders first so the dialog's left border appears on top of it
        configDrawer.render(graphics, font, mouseX, mouseY, partialTick);
        configDrawer.renderTab(graphics, mouseX, mouseY);
        dialog.render(graphics, font, title, mouseX, mouseY, partialTick);

        // Folder and extraction slots — two columns of 4 rows each
        for (int row = 0; row < AbstractFilingCabinetMenu.ROWS_PER_COLUMN; row++) {
            int sy = y + AbstractFilingCabinetMenu.FOLDER_Y_START + row * 18;
            drawSlotBackground(graphics, x + AbstractFilingCabinetMenu.FOLDER_X_LEFT, sy, 16, 16);
            drawSlotBackground(graphics, x + AbstractFilingCabinetMenu.EXTRACTION_X_LEFT, sy, 16, 16);
            drawSlotBackground(graphics, x + AbstractFilingCabinetMenu.FOLDER_X_RIGHT, sy, 16, 16);
            drawSlotBackground(graphics, x + AbstractFilingCabinetMenu.EXTRACTION_X_RIGHT, sy, 16, 16);
        }

        // Vertical rule between the two columns
        int ruleTop = y + AbstractFilingCabinetMenu.FOLDER_Y_START;
        int ruleBottom = ruleTop + AbstractFilingCabinetMenu.ROWS_PER_COLUMN * 18;
        graphics.fill(
                x + AbstractFilingCabinetMenu.COLUMN_RULE_X,
                ruleTop,
                x + AbstractFilingCabinetMenu.COLUMN_RULE_X + 2,
                ruleBottom,
                0xFF555555);

        // Player inventory slot backgrounds
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y + playerInvY);
        PLAYER_INV_PANE.render(graphics, font, BG_WIDTH, 0, 0, 0);
        graphics.pose().popMatrix();

        super.extractContents(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * Renders each slot; extraction slots that are locked-but-empty get a dimmed ghost icon,
     * and extraction slots with items always use compact count labels ("1.2k", "64") to match
     * other inventories in the mod.
     */
    @Override
    protected void extractSlot(GuiGraphicsExtractor graphics, Slot slot, int x, int y) {
        if (slot instanceof FolderExtractionSlot extractSlot && extractSlot.isGhost()) {
            ItemStack ghost = extractSlot.getGhostItem();
            if (!ghost.isEmpty()) {
                int sx = slot.x, sy = slot.y;
                graphics.item(ghost, sx, sy);
                graphics.fill(sx, sy, sx + 16, sy + 16, 0x80000000);
            }
            return;
        }

        if (slot instanceof FolderExtractionSlot && slot.hasItem()) {
            int sx = slot.x, sy = slot.y;
            ItemStack stack = slot.getItem();
            graphics.item(stack, sx, sy);
            long count = stack.getCount();
            if (count > 1) renderSizeLabel(graphics, font, sx, sy, CountFormat.format(count), 0xFFFFFFFF);
            if (hoveredSlot == slot) {
                graphics.fillGradient(sx, sy, sx + 16, sy + 16, -2130706433, -2130706433);
            }
            return;
        }

        super.extractSlot(graphics, slot, x, y);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Title is drawn by Dialog.
    }

    /** Renders a count label at 0.666 scale in the bottom-right corner of a 16×16 slot. */
    private static void renderSizeLabel(
            GuiGraphicsExtractor graphics, Font font, float x, float y, String text, int color) {
        float scale = 0.666f;
        float scaleInv = 1f / scale;
        float offset = -1f;

        graphics.pose().pushMatrix();
        graphics.pose().translate(0f, 0f);
        graphics.pose().scale(scale, scale);
        float textX = (x + offset + 16f + 2f - font.width(text) * scale) * scaleInv;
        float textY = (y + offset + 10f) * scaleInv;

        graphics.text(font, text, (int) (textX + 1f), (int) (textY + 1f), 0xFF414141, false);
        graphics.text(font, text, (int) textX, (int) textY, color, false);

        graphics.pose().popMatrix();
    }

    private static void drawSlotBackground(GuiGraphicsExtractor graphics, int sx, int sy, int w, int h) {
        graphics.fill(sx, sy, sx + w, sy + 1, 0xFF373737); // top
        graphics.fill(sx, sy + 1, sx + 1, sy + h, 0xFF373737); // left
        graphics.fill(sx, sy + h, sx + w + 1, sy + h + 1, 0xFFFFFFFF); // bottom
        graphics.fill(sx + w, sy, sx + w + 1, sy + h, 0xFFFFFFFF); // right
        graphics.fill(sx + 1, sy + 1, sx + w, sy + h, 0xFF8B8B8B); // interior
    }
}
