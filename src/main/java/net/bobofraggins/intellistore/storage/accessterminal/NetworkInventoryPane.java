package net.bobofraggins.intellistore.storage.accessterminal;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.bobofraggins.intellistore.shared.network.SatExtractPacket;
import net.bobofraggins.intellistore.shared.network.SatInsertPacket;
import net.bobofraggins.intellistore.shared.ui.IDialogPane;
import net.bobofraggins.intellistore.shared.util.CountFormat;
import net.bobofraggins.intellistore.shared.util.SearchSync;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Dialog pane that renders the scrollable network item grid and its scrollbar.
 *
 * <p>Local coordinate origin (0, 0) sits at {@code topPos + Dialog.TITLE_H}.
 * The grid itself is inset by 1 px at the top ({@link AccessTerminalLayout#NETWORK_Y} −
 * {@link AccessTerminalLayout#TITLE_H} = 1).
 *
 * <p>When a JEI search filter is active (via {@link SearchSync}), the grid shows only matching
 * items. Clicks use the displayed item's identity (not a positional index), so filtering does not
 * affect extraction correctness.
 */
public class NetworkInventoryPane implements IDialogPane {

    private static final ResourceLocation BG_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    // Pane-local coordinates derived from AccessTerminalLayout
    /** Local Y of the grid top edge (= NETWORK_Y - TITLE_H = 1). */
    private static final int GRID_Y = AccessTerminalLayout.NETWORK_Y - AccessTerminalLayout.TITLE_H;

    private static final int GRID_X = AccessTerminalLayout.NETWORK_X;
    private static final int SCROLLBAR_X = AccessTerminalLayout.SCROLLBAR_X;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final AccessTerminalMenu menu;

    /** Full unfiltered list received from the server. */
    private List<ItemStack> allStacks = List.of();

    private List<Long> allCounts = List.of();

    /** Filtered view rendered in the grid. May be the same object as allStacks when no filter. */
    private List<ItemStack> stacks = List.of();

    private List<Long> counts = List.of();

    private String appliedFilter = "";
    private int scrollOffset = 0;

    /** False until the first server response arrives. */
    private boolean hasContents = false;

    public NetworkInventoryPane(AccessTerminalMenu menu) {
        this.menu = menu;
    }

    // -------------------------------------------------------------------------
    // IDialogPane
    // -------------------------------------------------------------------------

    @Override
    public int preferredWidth() {
        return AccessTerminalLayout.BG_WIDTH;
    }

    /** Spans from {@code TITLE_H} to {@code CRAFTING_Y} (exclusive). */
    @Override
    public int preferredHeight() {
        return AccessTerminalLayout.CRAFTING_Y - AccessTerminalLayout.TITLE_H;
    }

    @Override
    public void render(
            GuiGraphics graphics, Font font, int width, int localMouseX, int localMouseY, float partialTick) {
        drawGrid(graphics, font);
        drawScrollbar(graphics);
    }

    @Override
    public boolean mouseClicked(double localX, double localY, int button) {
        if (!isInGrid(localX, localY)) return false;

        ItemStack carried = menu.getCarried();
        if (!carried.isEmpty()) {
            if (button == 0 && menu.hasNetwork()) {
                PacketDistributor.sendToServer(new SatInsertPacket(menu.getNiPos(), -1));
            }
            return true;
        }

        int col = (int) ((localX - GRID_X) / AccessTerminalLayout.SLOT_SIZE);
        int row = (int) ((localY - GRID_Y) / AccessTerminalLayout.SLOT_SIZE);
        int idx = (row + scrollOffset) * AccessTerminalLayout.NETWORK_COLS + col;

        if (hasContents && idx >= 0 && idx < stacks.size() && menu.hasNetwork()) {
            ItemStack target = stacks.get(idx);
            long totalCount = counts.get(idx);
            int maxStack = target.getMaxStackSize();
            if (Screen.hasShiftDown()) {
                int amount = (int) Math.min(totalCount, maxStack);
                PacketDistributor.sendToServer(
                        new SatExtractPacket(menu.getNiPos(), target.copyWithCount(1), amount, false));
            } else {
                int amount = (button == 1) ? 1 : (int) Math.min(totalCount, maxStack);
                PacketDistributor.sendToServer(
                        new SatExtractPacket(menu.getNiPos(), target.copyWithCount(1), amount, true));
            }
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double localX, double localY, double dx, double dy) {
        if (isInGrid(localX, localY)) {
            scrollOffset -= (int) Math.signum(dy);
            clampScroll();
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Stores the full server-provided list and applies the current filter. */
    public void setContents(List<ItemStack> stacks, List<Long> counts) {
        this.allStacks = stacks;
        this.allCounts = counts;
        this.hasContents = true;
        applyFilter();
    }

    /**
     * Updates the active search filter and rebuilds the display list.
     *
     * <p>No-op when the filter has not changed since the last call.
     */
    public void setFilter(String filter) {
        if (filter.equals(appliedFilter)) return;
        appliedFilter = filter;
        applyFilter();
    }

    public void reset() {
        this.allStacks = List.of();
        this.allCounts = List.of();
        this.stacks = List.of();
        this.counts = List.of();
        this.hasContents = false;
        this.scrollOffset = 0;
    }

    /**
     * Returns the full unfiltered stack list — used by {@link AccessTerminalScreen} to detect when
     * new server data has arrived.
     */
    public List<ItemStack> getStacks() {
        return allStacks;
    }

    /**
     * Returns the network item stack under the given pane-local mouse position, or {@code null}
     * if the cursor is outside the grid or no item is there.
     */
    @Nullable
    public ItemStack getHoveredStack(double localX, double localY) {
        if (!hasContents || !isInGrid(localX, localY)) return null;
        int col = (int) ((localX - GRID_X) / AccessTerminalLayout.SLOT_SIZE);
        int row = (int) ((localY - GRID_Y) / AccessTerminalLayout.SLOT_SIZE);
        int idx = (row + scrollOffset) * AccessTerminalLayout.NETWORK_COLS + col;
        return (idx >= 0 && idx < stacks.size()) ? stacks.get(idx) : null;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void applyFilter() {
        if (appliedFilter.isEmpty()) {
            stacks = allStacks;
            counts = allCounts;
        } else {
            List<ItemStack> fs = new ArrayList<>();
            List<Long> fc = new ArrayList<>();
            for (int i = 0; i < allStacks.size(); i++) {
                if (SearchSync.matches(allStacks.get(i), appliedFilter)) {
                    fs.add(allStacks.get(i));
                    fc.add(allCounts.get(i));
                }
            }
            stacks = fs;
            counts = fc;
        }
        clampScroll();
    }

    private boolean isInGrid(double localX, double localY) {
        return localX >= GRID_X
                && localX < GRID_X + AccessTerminalLayout.NETWORK_W
                && localY >= GRID_Y
                && localY < GRID_Y + AccessTerminalLayout.NETWORK_H;
    }

    private void clampScroll() {
        int totalRows = (stacks.size() + AccessTerminalLayout.NETWORK_COLS - 1) / AccessTerminalLayout.NETWORK_COLS;
        int maxScroll = Math.max(0, totalRows - AccessTerminalLayout.NETWORK_VISIBLE_ROWS);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    private void drawGrid(GuiGraphics graphics, Font font) {
        // Slot outlines from the chest-row strip in generic_54.png
        for (int row = 0; row < AccessTerminalLayout.NETWORK_VISIBLE_ROWS; row++) {
            graphics.blit(
                    BG_TEXTURE,
                    GRID_X,
                    GRID_Y + row * AccessTerminalLayout.SLOT_SIZE,
                    7,
                    17 + row * AccessTerminalLayout.SLOT_SIZE,
                    AccessTerminalLayout.NETWORK_W,
                    AccessTerminalLayout.SLOT_SIZE);
        }

        if (!hasContents) return;

        int firstIdx = scrollOffset * AccessTerminalLayout.NETWORK_COLS;
        for (int row = 0; row < AccessTerminalLayout.NETWORK_VISIBLE_ROWS; row++) {
            for (int col = 0; col < AccessTerminalLayout.NETWORK_COLS; col++) {
                int idx = firstIdx + row * AccessTerminalLayout.NETWORK_COLS + col;
                if (idx >= stacks.size()) return;

                ItemStack stack = stacks.get(idx);
                long count = counts.get(idx);
                int sx = GRID_X + col * AccessTerminalLayout.SLOT_SIZE + 1;
                int sy = GRID_Y + row * AccessTerminalLayout.SLOT_SIZE + 1;

                graphics.renderItem(stack, sx, sy);
                String countStr = count > 1 ? CountFormat.format(count) : null;
                graphics.renderItemDecorations(font, stack, sx, sy, countStr);
            }
        }
    }

    private void drawScrollbar(GuiGraphics graphics) {
        int barY = GRID_Y;
        int barH = AccessTerminalLayout.NETWORK_H;

        // Track
        graphics.fill(SCROLLBAR_X, barY, SCROLLBAR_X + AccessTerminalLayout.SCROLLBAR_W, barY + barH, 0x40000000);

        // Thumb
        int totalRows = !hasContents
                ? 1
                : Math.max(
                        (stacks.size() + AccessTerminalLayout.NETWORK_COLS - 1) / AccessTerminalLayout.NETWORK_COLS, 1);
        int thumbH = Math.max(8, barH * AccessTerminalLayout.NETWORK_VISIBLE_ROWS / totalRows);
        int maxScroll = Math.max(1, totalRows - AccessTerminalLayout.NETWORK_VISIBLE_ROWS);
        int thumbY = barY + (barH - thumbH) * scrollOffset / maxScroll;
        if (totalRows <= AccessTerminalLayout.NETWORK_VISIBLE_ROWS) {
            thumbY = barY;
            thumbH = barH;
        }
        graphics.fill(SCROLLBAR_X, thumbY, SCROLLBAR_X + AccessTerminalLayout.SCROLLBAR_W, thumbY + thumbH, 0xC0FFFFFF);
    }
}
