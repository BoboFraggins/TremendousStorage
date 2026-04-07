package net.bobofraggins.tremendousstorage.storage.accessterminal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.shared.config.SortMode;
import net.bobofraggins.tremendousstorage.shared.config.TremendousStorageClientConfig;
import net.bobofraggins.tremendousstorage.shared.network.SatExtractPacket;
import net.bobofraggins.tremendousstorage.shared.network.SatInsertPacket;
import net.bobofraggins.tremendousstorage.shared.ui.IDialogPane;
import net.bobofraggins.tremendousstorage.shared.util.CountFormat;
import net.bobofraggins.tremendousstorage.shared.util.SearchSync;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
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

    /**
     * The most recent raw stacks list sent by the server. Used by {@link #setFluidIndices} to
     * remap server-relative fluid indices to our stable display-order indices.
     */
    private List<ItemStack> lastServerStacks = List.of();

    /** Filtered view rendered in the grid. May be the same object as allStacks when no filter. */
    private List<ItemStack> stacks = List.of();

    private List<Long> counts = List.of();

    /** Filtered view of allIsFluid, parallel to stacks. */
    private List<Boolean> isFluid = List.of();

    private SortMode sortMode = SortMode.AMOUNT;
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
        return TremendousStorageClientConfig.getVisibleRows() * AccessTerminalLayout.SLOT_SIZE + 5;
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
            if (totalCount == 0) return true; // ghost entry — item no longer in network
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
                if (button == 2) {
                    // Middle-click: extract exactly one item to cursor
                    PacketDistributor.sendToServer(
                            new SatExtractPacket(menu.getNiPos(), target.copyWithCount(1), 1, true));
                } else if (Screen.hasShiftDown()) {
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

    /**
     * Merges a new server-provided list into the display list while preserving display order
     * (Ghost Mode). On the first call the server list is adopted directly. On subsequent calls
     * counts are updated in-place by item identity; items that vanish from the network stay
     * visible at count 0 so the grid does not reorder while the player is interacting with it.
     * Genuinely new items are appended to the end.
     */
    public void setContents(List<ItemStack> newStacks, List<Long> newCounts) {
        this.hasContents = true;
        this.lastServerStacks = newStacks;

        if (allStacks.isEmpty()) {
            this.allStacks = new ArrayList<>(newStacks);
            this.allCounts = new ArrayList<>(newCounts);
            this.allIsFluid = new ArrayList<>(Collections.nCopies(newStacks.size(), false));
            sortDisplayList();
            applyFilter();
            return;
        }

        // Update counts in-place; items absent from the server list become ghost entries (count 0).
        boolean[] serverMatched = new boolean[newStacks.size()];
        for (int i = 0; i < allStacks.size(); i++) {
            Long newCount = null;
            for (int j = 0; j < newStacks.size(); j++) {
                if (!serverMatched[j] && ItemStack.isSameItemSameComponents(allStacks.get(i), newStacks.get(j))) {
                    newCount = newCounts.get(j);
                    serverMatched[j] = true;
                    break;
                }
            }
            allCounts.set(i, newCount != null ? newCount : 0L);
        }

        // Append items that are new to the network (not seen before).
        for (int j = 0; j < newStacks.size(); j++) {
            if (!serverMatched[j]) {
                allStacks.add(newStacks.get(j));
                allCounts.add(newCounts.get(j));
                allIsFluid.add(false); // setFluidIndices will correct this
            }
        }

        applyFilter();
    }

    /**
     * Updates the fluid-backed entry flags.
     *
     * <p>The incoming {@code indices} reference positions in the server's list
     * ({@link #lastServerStacks}), which may differ from our stable display order. Items are
     * matched by identity so the flags stay correct regardless of order changes.
     *
     * <p>Call this after {@link #setContents} whenever a new packet arrives.
     */
    public void setFluidIndices(Set<Integer> indices) {
        List<Boolean> fluid = new ArrayList<>(allStacks.size());
        for (int i = 0; i < allStacks.size(); i++) {
            boolean isFluidItem = false;
            for (int serverIdx : indices) {
                if (serverIdx < lastServerStacks.size()
                        && ItemStack.isSameItemSameComponents(allStacks.get(i), lastServerStacks.get(serverIdx))) {
                    isFluidItem = true;
                    break;
                }
            }
            fluid.add(isFluidItem);
        }
        this.allIsFluid = fluid;
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
        this.lastServerStacks = List.of();
        this.stacks = List.of();
        this.counts = List.of();
        this.isFluid = List.of();
        this.hasContents = false;
        this.scrollOffset = 0;
    }

    /**
     * Returns the last raw server list — used by {@link AccessTerminalScreen} to detect when new
     * server data has arrived via reference-equality comparison against {@code PENDING_STACKS}.
     */
    public List<ItemStack> getStacks() {
        return lastServerStacks;
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

    public SortMode getSortMode() {
        return sortMode;
    }

    /**
     * Updates the sort mode, re-sorts the display list, and rebuilds the filtered view.
     * Does nothing if the mode is unchanged.
     */
    public void setSortMode(SortMode mode) {
        if (mode == sortMode) return;
        sortMode = mode;
        sortDisplayList();
        applyFilter();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Sorts {@link #allStacks}, {@link #allCounts}, and {@link #allIsFluid} together according
     * to the current {@link SortMode}. Ghost entries (count 0) always sort last in AMOUNT mode;
     * in NAME and MOD modes they follow normal ordering.
     */
    private void sortDisplayList() {
        int n = allStacks.size();
        if (n == 0) return;

        Integer[] indices = new Integer[n];
        for (int i = 0; i < n; i++) indices[i] = i;
        Arrays.sort(indices, buildComparator());

        List<ItemStack> sortedStacks = new ArrayList<>(n);
        List<Long> sortedCounts = new ArrayList<>(n);
        List<Boolean> sortedFluid = new ArrayList<>(n);
        for (int i : indices) {
            sortedStacks.add(allStacks.get(i));
            sortedCounts.add(allCounts.get(i));
            sortedFluid.add(i < allIsFluid.size() ? allIsFluid.get(i) : false);
        }
        allStacks = sortedStacks;
        allCounts = sortedCounts;
        allIsFluid = sortedFluid;
    }

    private Comparator<Integer> buildComparator() {
        return switch (sortMode) {
            case AMOUNT -> (a, b) -> Long.compare(allCounts.get(b), allCounts.get(a));
            case NAME -> (a, b) -> allStacks
                    .get(a)
                    .getHoverName()
                    .getString()
                    .compareToIgnoreCase(allStacks.get(b).getHoverName().getString());
            case MOD -> (a, b) -> {
                int cmp = modId(allStacks.get(a)).compareToIgnoreCase(modId(allStacks.get(b)));
                return cmp != 0
                        ? cmp
                        : allStacks
                                .get(a)
                                .getHoverName()
                                .getString()
                                .compareToIgnoreCase(
                                        allStacks.get(b).getHoverName().getString());
            };
        };
    }

    private static String modId(ItemStack stack) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key != null ? key.getNamespace() : "";
    }

    /** Returns one fewer row when a filter is active (to make room for the filter label). */
    private int visibleRows() {
        int base = TremendousStorageClientConfig.getVisibleRows();
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
                if (count == 0) {
                    // Ghost entry: item is no longer in the network. Show a red "0".
                    graphics.pose().pushPose();
                    graphics.pose().translate(0, 0, 200);
                    String zeroStr = "0";
                    graphics.drawString(font, zeroStr, sx + 19 - font.width(zeroStr), sy + 9, 0xFFFF5555, true);
                    graphics.pose().popPose();
                } else {
                    String countStr = count > 1 ? CountFormat.format(count) : null;
                    graphics.renderItemDecorations(font, stack, sx, sy, countStr);
                }
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
