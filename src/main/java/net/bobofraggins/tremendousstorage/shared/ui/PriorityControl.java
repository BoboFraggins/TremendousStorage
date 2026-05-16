package net.bobofraggins.tremendousstorage.shared.ui;

import net.bobofraggins.tremendousstorage.shared.network.SetPriorityPacket;
import net.bobofraggins.tremendousstorage.shared.priority.Priority;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Menu for the priority screen shared by storage blocks.
 *
 * <p>Implements {@link IDialogPane} so it can be hosted inside a {@link Dialog}.
 *
 * <p>No item slots. Carries one server→client data value:
 * <ul>
 *   <li>slot 0: priority ordinal (0–4)
 * </ul>
 *
 * <p>Layout (pane-local coordinates, pane height = 33):
 * <ul>
 *   <li>"Priority:" label row at local y = 3
 *   <li>▼ button at (38, 14), 20 × 14
 *   <li>label box at (60, 14), 56 × 14
 *   <li>▲ button at (118, 14), 20 × 14
 * </ul>
 */
public class PriorityControl extends AbstractContainerMenu implements IDialogPane {

    // -------------------------------------------------------------------------
    // Layout constants (pane-local, pane width = 176, pane height = 33)
    // -------------------------------------------------------------------------

    private static final int PANE_HEIGHT = 33;
    private static final int PANE_WIDTH = 176;

    private static final int LABEL_Y = 3; // "Priority:" text row
    private static final int ROW_Y = 14; // arrow + name row
    private static final int BTN_W = 20;
    private static final int BTN_H = 14;
    private static final int LBL_W = 56;
    private static final int GAP = 2;
    private static final int ROW_W = BTN_W + GAP + LBL_W + GAP + BTN_W; // 100
    private static final int ROW_X = (PANE_WIDTH - ROW_W) / 2; // 38

    private static final int DOWN_BTN_X = ROW_X;
    private static final int UP_BTN_X = ROW_X + BTN_W + GAP + LBL_W + GAP;
    private static final int LBL_X = ROW_X + BTN_W + GAP;

    // -------------------------------------------------------------------------
    // Menu state
    // -------------------------------------------------------------------------

    private final BlockPos pos;
    private final ContainerData data;

    /** Server-side constructor. */
    public PriorityControl(int windowId, Inventory inv, BlockPos pos, ContainerData data) {
        super(Registration.PRIORITY_MENU.get(), windowId);
        this.pos = pos;
        this.data = data;
        addDataSlots(data);
    }

    /** Client-side constructor (called by the menu type factory). */
    public PriorityControl(int windowId, Inventory inv, FriendlyByteBuf buf) {
        this(windowId, inv, buf.readBlockPos(), new SimpleContainerData(1));
    }

    public BlockPos getPos() {
        return pos;
    }

    public int getPriority() {
        return data.get(0);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    // -------------------------------------------------------------------------
    // IDialogPane
    // -------------------------------------------------------------------------

    @Override
    public int preferredWidth() {
        return PANE_WIDTH;
    }

    @Override
    public int preferredHeight() {
        return PANE_HEIGHT;
    }

    @Override
    public void render(
            GuiGraphicsExtractor graphics, Font font, int width, int localMouseX, int localMouseY, float partialTick) {
        int selected = getPriority();

        // "Priority:" label, centred
        Component priorityLabel = Component.translatable("screen.tremendousstorage.priority_label");
        graphics.text(font, priorityLabel, (width - font.width(priorityLabel)) / 2, LABEL_Y, 0x404040, false);

        // ▼ button
        boolean downActive = selected > 0;
        drawButton(graphics, font, DOWN_BTN_X, ROW_Y, BTN_W, BTN_H, "▼", downActive);

        // ▲ button
        boolean upActive = selected < Priority.VALUES.length - 1;
        drawButton(graphics, font, UP_BTN_X, ROW_Y, BTN_W, BTN_H, "▲", upActive);

        // Label box (no border, matches dialog background)
        graphics.fill(LBL_X, ROW_Y, LBL_X + LBL_W, ROW_Y + BTN_H, 0xFFC6C6C6);

        // Priority name centred in label box
        Priority current = Priority.fromOrdinal(selected);
        String name = Component.translatable(current.translationKey()).getString();
        int nameX = LBL_X + (LBL_W - font.width(name)) / 2;
        int nameY = ROW_Y + (BTN_H - 8) / 2;
        graphics.text(font, name, nameX, nameY, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(double localX, double localY, int button) {
        if (button != 0) return false;
        int selected = getPriority();

        if (isInButton(localX, localY, DOWN_BTN_X)) {
            if (selected > 0) {
                ClientPacketDistributor.sendToServer(new SetPriorityPacket(pos, selected - 1));
            }
            return true;
        }
        if (isInButton(localX, localY, UP_BTN_X)) {
            if (selected < Priority.VALUES.length - 1) {
                ClientPacketDistributor.sendToServer(new SetPriorityPacket(pos, selected + 1));
            }
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static boolean isInButton(double lx, double ly, int btnX) {
        return lx >= btnX && lx < btnX + BTN_W && ly >= ROW_Y && ly < ROW_Y + BTN_H;
    }

    private static void drawButton(
            GuiGraphicsExtractor graphics, Font font, int x, int y, int w, int h, String label, boolean active) {
        // Vanilla-style button border: dark top/left, light bottom/right, gray fill
        int fillColor = active ? 0xFFC6C6C6 : 0xFF9B9B9B;
        graphics.fill(x, y, x + w, y + 1, 0xFF555555); // top
        graphics.fill(x, y + 1, x + 1, y + h, 0xFF555555); // left
        graphics.fill(x, y + h, x + w, y + h + 1, 0xFFFFFFFF); // bottom
        graphics.fill(x + w, y, x + w + 1, y + h + 1, 0xFFFFFFFF); // right
        graphics.fill(x + 1, y + 1, x + w, y + h, fillColor); // fill

        // Label centred
        int lx = x + (w - font.width(label)) / 2;
        int ly = y + (h - 8) / 2;
        graphics.text(font, label, lx, ly, active ? 0x404040 : 0x707070, false);
    }
}
