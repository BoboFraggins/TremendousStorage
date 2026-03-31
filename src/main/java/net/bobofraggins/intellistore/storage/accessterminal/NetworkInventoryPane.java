package net.bobofraggins.intellistore.storage.accessterminal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.bobofraggins.intellistore.shared.config.IntelliStoreClientConfig;
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
import net.minecraft.world.item.Items;
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

    /** Parallel to allStacks: true for entries backed by a fluid tank. */
    private List<Boolean> allIsFluid = List.of();

    /** Filtered view rendered in the grid. May be the same object as allStacks when no filter. */
    private List<ItemStack> stacks = List.of();

    private List<Long> counts = List.of();

    /** Filtered view of allIsFluid, parallel to stacks. */
    private List<Boolean> isFluid = List.of();

    private String appliedFilter = "";
    private int scrollOffset = 0;
    private boolean draggingScrollbar = false;

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

    /** GRID_Y(1) + rows*SLOT_SIZE + bottom_gap(4) — driven by client config. */
    @Override
    public int preferredHeight() {
        return IntelliStoreClientConfig.getVisibleRows() * AccessTerminalLayout.SLOT_SIZE + 5;
    }

    @Override
    public void render(
            GuiGraphics graphics, Font font, int width, int localMouseX, int localMouseY, float partialTick) {
        drawGrid(graphics, font);
        drawScrollbar(graphics);
    }

    @Override
    public boolean mouseClicked(double localX, double localY, int button) {
        if (isInScrollbar(localX, localY)) {
            draggingScrollbar = true;
            scrollToY(localY);
            return true;
        }
        if (!isInGrid(localX, localY)) return false;

        ItemStack carried = menu.getCarried();
        if (!carried.isEmpty()) {
            if (button == 0 && menu.hasNetwork()) {
                PacketDistributor.sendToServer(new SatInsertPacket(menu.getNiPos(), -1));
            }
            return true;
        }

        int col = (int) ((localX - GRID_X) / AccessTerminalLayout.SLOT_SIZE);
        int row = (int) ((localY - gridStartY()) / AccessTerminalLayout.SLOT_SIZE);
        int idx = (row + scrollOffset) * AccessTerminalLayout.NETWORK_COLS + col;

        if (hasContents && idx >= 0 && idx < stacks.size() && menu.hasNetwork()) {
            ItemStack target = stacks.get(idx);
            long totalCount = counts.get(idx);
            int maxStack = target.getMaxStackSize();

            boolean isFluidSlot = !isFluid.isEmpty() && idx < isFluid.size() && isFluid.get(idx);
            if (isFluidSlot) {
                // Fluid slot: require an empty bucket in the cursor
                ItemStack cursorStack = menu.getCarried();
                if (cursorStack.isEmpty() || !cursorStack.is(Items.BUCKET)) {
                    return true; // block but consume the event
                }
                // Always extract 1 bucket per click for fluid slots
                PacketDistributor.sendToServer(new SatExtractPacket(menu.getNiPos(), target.copyWithCount(1), 1, true));
            } else {
                if (Screen.hasShiftDown()) {
                    int amount = (int) Math.min(totalCount, maxStack);
                    PacketDistributor.sendToServer(
                            new SatExtractPacket(menu.getNiPos(), target.copyWithCount(1), amount, false));
                } else {
                    int amount = (button == 1)
                            ? (int) Math.max(1, (totalCount + 1) / 2)
                            : (int) Math.min(totalCount, maxStack);
                    PacketDistributor.sendToServer(
                            new SatExtractPacket(menu.getNiPos(), target.copyWithCount(1), amount, true));
                }
            }
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double localX, double localY, double dx, double dy) {
        if (isInGrid(localX, localY) || isInScrollbar(localX, localY)) {
            scrollOffset -= (int) Math.signum(dy);
            clampScroll();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double localX, double localY, int button, double dragX, double dragY) {
        if (draggingScrollbar) {
            scrollToY(localY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double localX, double localY, int button) {
        if (draggingScrollbar) {
            draggingScrollbar = false;
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
        this.allIsFluid = Collections.nCopies(stacks.size(), false);
        this.hasContents = true;
        applyFilter();
    }

    /**
     * Updates the fluid-backed entry flags, parallel to the allStacks list.
     * Call this after {@link #setContents} whenever a new packet arrives.
     */
    public void setFluidIndices(Set<Integer> indices) {
        List<Boolean> fluid = new ArrayList<>(allStacks.size());
        for (int i = 0; i < allStacks.size(); i++) {
            fluid.add(indices.contains(i));
        }
        this.allIsFluid = List.copyOf(fluid);
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
        this.allIsFluid = List.of();
        this.stacks = List.of();
        this.counts = List.of();
        this.isFluid = List.of();
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
        int row = (int) ((localY - gridStartY()) / AccessTerminalLayout.SLOT_SIZE);
        int idx = (row + scrollOffset) * AccessTerminalLayout.NETWORK_COLS + col;
        return (idx >= 0 && idx < stacks.size()) ? stacks.get(idx) : null;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Returns one fewer row when a filter is active (to make room for the filter label). */
    private int visibleRows() {
        int base = IntelliStoreClientConfig.getVisibleRows();
        return appliedFilter.isEmpty() ? base : base - 1;
    }

    /** Top Y of the item grid — shifts down by one slot when a filter label occupies the top row. */
    private int gridStartY() {
        return appliedFilter.isEmpty() ? GRID_Y : GRID_Y + AccessTerminalLayout.SLOT_SIZE;
    }

    private void applyFilter() {
        if (appliedFilter.isEmpty()) {
            stacks = allStacks;
            counts = allCounts;
            isFluid = allIsFluid;
        } else {
            List<ItemStack> fs = new ArrayList<>();
            List<Long> fc = new ArrayList<>();
            List<Boolean> ff = new ArrayList<>();
            for (int i = 0; i < allStacks.size(); i++) {
                if (SearchSync.matches(allStacks.get(i), appliedFilter)) {
                    fs.add(allStacks.get(i));
                    fc.add(allCounts.get(i));
                    ff.add(allIsFluid.isEmpty() ? false : allIsFluid.get(i));
                }
            }
            stacks = fs;
            counts = fc;
            isFluid = ff;
        }
        clampScroll();
    }

    private boolean isInGrid(double localX, double localY) {
        int startY = gridStartY();
        return localX >= GRID_X
                && localX < GRID_X + AccessTerminalLayout.NETWORK_W
                && localY >= startY
                && localY < startY + visibleRows() * AccessTerminalLayout.SLOT_SIZE;
    }

    private boolean isInScrollbar(double localX, double localY) {
        int startY = gridStartY();
        int barH = visibleRows() * AccessTerminalLayout.SLOT_SIZE;
        return localX >= SCROLLBAR_X
                && localX < SCROLLBAR_X + AccessTerminalLayout.SCROLLBAR_W
                && localY >= startY
                && localY < startY + barH;
    }

    private void scrollToY(double localY) {
        int rows = visibleRows();
        int barH = rows * AccessTerminalLayout.SLOT_SIZE;
        int totalRows = Math.max(
                (stacks.size() + AccessTerminalLayout.NETWORK_COLS - 1) / AccessTerminalLayout.NETWORK_COLS, 1);
        int maxScroll = Math.max(0, totalRows - rows);
        if (maxScroll == 0) return;
        double ratio = (localY - gridStartY()) / (double) barH;
        scrollOffset = (int) Math.round(ratio * maxScroll);
        clampScroll();
    }

    private void clampScroll() {
        int totalRows = (stacks.size() + AccessTerminalLayout.NETWORK_COLS - 1) / AccessTerminalLayout.NETWORK_COLS;
        int maxScroll = Math.max(0, totalRows - visibleRows());
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    private void drawGrid(GuiGraphics graphics, Font font) {
        int rows = visibleRows();
        int startY = gridStartY();

        // Filter label at the top row when a search filter is active
        if (!appliedFilter.isEmpty()) {
            graphics.fill(
                    GRID_X,
                    GRID_Y,
                    GRID_X + AccessTerminalLayout.NETWORK_W,
                    GRID_Y + AccessTerminalLayout.SLOT_SIZE,
                    0xFF1C1C1C);
            String label = "Filtered: " + appliedFilter;
            int maxW = AccessTerminalLayout.NETWORK_W - 4;
            if (font.width(label) > maxW) {
                label = font.plainSubstrByWidth(label, maxW - font.width("...")) + "...";
            }
            int labelY = GRID_Y + (AccessTerminalLayout.SLOT_SIZE - font.lineHeight) / 2;
            graphics.drawString(font, label, GRID_X + 2, labelY, 0xFFCCCCCC, false);
        }

        // Slot outlines from the chest-row strip in generic_54.png
        for (int row = 0; row < rows; row++) {
            graphics.blit(
                    BG_TEXTURE,
                    GRID_X,
                    startY + row * AccessTerminalLayout.SLOT_SIZE,
                    7,
                    17,
                    AccessTerminalLayout.NETWORK_W,
                    AccessTerminalLayout.SLOT_SIZE);
        }

        if (!hasContents) return;

        int firstIdx = scrollOffset * AccessTerminalLayout.NETWORK_COLS;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < AccessTerminalLayout.NETWORK_COLS; col++) {
                int idx = firstIdx + row * AccessTerminalLayout.NETWORK_COLS + col;
                if (idx >= stacks.size()) return;

                ItemStack stack = stacks.get(idx);
                long count = counts.get(idx);
                int sx = GRID_X + col * AccessTerminalLayout.SLOT_SIZE + 1;
                int sy = startY + row * AccessTerminalLayout.SLOT_SIZE + 1;

                graphics.renderItem(stack, sx, sy);
                String countStr = count > 1 ? CountFormat.format(count) : null;
                graphics.renderItemDecorations(font, stack, sx, sy, countStr);
            }
        }
    }

    private void drawScrollbar(GuiGraphics graphics) {
        int rows = visibleRows();
        int barY = gridStartY();
        int barH = rows * AccessTerminalLayout.SLOT_SIZE;

        // Left border
        graphics.fill(SCROLLBAR_X - 1, barY, SCROLLBAR_X, barY + barH, 0xFF555555);

        // Track
        graphics.fill(SCROLLBAR_X, barY, SCROLLBAR_X + AccessTerminalLayout.SCROLLBAR_W, barY + barH, 0x40000000);

        // Thumb
        int totalRows = !hasContents
                ? 1
                : Math.max(
                        (stacks.size() + AccessTerminalLayout.NETWORK_COLS - 1) / AccessTerminalLayout.NETWORK_COLS, 1);
        int thumbH = Math.max(8, barH * rows / totalRows);
        int maxScroll = Math.max(1, totalRows - rows);
        int thumbY = barY + (barH - thumbH) * scrollOffset / maxScroll;
        if (totalRows <= rows) {
            thumbY = barY;
            thumbH = barH;
        }
        graphics.fill(SCROLLBAR_X, thumbY, SCROLLBAR_X + AccessTerminalLayout.SCROLLBAR_W, thumbY + thumbH, 0xC0FFFFFF);
    }
}
