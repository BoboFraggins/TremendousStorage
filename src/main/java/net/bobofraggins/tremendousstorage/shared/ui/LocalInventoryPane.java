package net.bobofraggins.tremendousstorage.shared.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.shared.config.TremendousStorageClientConfig;
import net.bobofraggins.tremendousstorage.shared.util.CountFormat;
import net.bobofraggins.tremendousstorage.shared.util.SearchSync;
import net.bobofraggins.tremendousstorage.storage.accessterminal.AccessTerminalLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Dialog pane that renders a scrollable item grid backed by a local block inventory.
 *
 * <p>Uses the same visual layout as the network inventory grid in the Access Terminal: 9 columns,
 * 4 visible rows, 18 px slots, and a 6 px scrollbar flush against the right edge of the grid.
 *
 * <p>Grid interactions (click to extract, click with carried item to insert) are routed to a
 * {@link GridClickHandler} supplied by the hosting screen.
 *
 * <p>When a JEI search filter is active (via {@link SearchSync}), the grid shows only matching
 * items. Click indices reported to {@link GridClickHandler} are always the <em>original</em>
 * indices in the unfiltered list so that server-side extraction stays correct.
 */
public class LocalInventoryPane implements IDialogPane {

    /**
     * Callback for grid click interactions.
     *
     * <ul>
     *   <li>{@code idx == -1}: player clicked the grid while holding an item — insert from cursor.
     *   <li>{@code idx >= 0}: player clicked an occupied grid slot — extract {@code amount} items;
     *       {@code toCursor = true} places them on the cursor, {@code false} sends them directly
     *       to the player's inventory (shift-click behaviour). The index is always relative to the
     *       <em>unfiltered</em> list regardless of any active search filter.
     * </ul>
     */
    @FunctionalInterface
    public interface GridClickHandler {
        void onClick(int idx, int amount, boolean toCursor);
    }

    private static final Identifier BG_TEXTURE =
            Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");

    private static final Identifier SCROLLER = Identifier.withDefaultNamespace("container/creative_inventory/scroller");
    private static final Identifier SCROLLER_DISABLED =
            Identifier.withDefaultNamespace("container/creative_inventory/scroller_disabled");

    private static final int GRID_X = AccessTerminalLayout.NETWORK_X;
    private static final int GRID_Y = AccessTerminalLayout.NETWORK_Y - AccessTerminalLayout.TITLE_H; // 1
    private static final int SCROLLBAR_X = AccessTerminalLayout.SCROLLBAR_X;

    // GRID_Y(1) + rows*SLOT_SIZE + bottom_gap(4) — computed dynamically from config

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /** Full unfiltered contents supplied by the hosting screen. */
    private List<ItemStack> allStacks = List.of();

    private List<Long> allCounts = List.of();

    /** Filtered view rendered in the grid. May be the same object as allStacks when no filter. */
    private List<ItemStack> displayStacks = List.of();

    private List<Long> displayCounts = List.of();

    /**
     * Maps displayed index → allStacks index. {@code null} means identity (no filter active).
     */
    @Nullable
    private int[] toOriginal = null;

    /**
     * Maps allStacks index → original server-side index. {@code null} when the caller did not
     * pre-sort the list (allStacks indices equal server indices directly).
     */
    @Nullable
    private int[] baseToOriginal = null;

    private String appliedFilter = "";
    private int scrollOffset = 0;
    private boolean draggingScrollbar = false;

    /**
     * Original server-side indices already extracted in the current shift-drag session.
     * Cleared on mouseReleased and at the start of each new mouseClicked.
     */
    private final Set<Integer> shiftDragVisited = new HashSet<>();

    @Nullable
    private GridClickHandler clickHandler;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Replaces the full item list and re-applies the current filter. */
    public void setContents(List<ItemStack> stacks, List<Long> counts) {
        this.allStacks = stacks;
        this.allCounts = counts;
        this.baseToOriginal = null;
        applyFilter();
    }

    /**
     * Replaces the full item list with an explicit sort-order mapping and re-applies the filter.
     *
     * @param sortedToOriginal maps each position in {@code stacks} to the corresponding
     *     server-side index in the block entity's unsorted inventory.
     */
    public void setContents(List<ItemStack> stacks, List<Long> counts, int[] sortedToOriginal) {
        this.allStacks = stacks;
        this.allCounts = counts;
        this.baseToOriginal = sortedToOriginal;
        applyFilter();
    }

    /**
     * Updates the active search filter and rebuilds the display list.
     *
     * <p>The filter string should be the raw output of {@link SearchSync#getFilter()} — already
     * lowercased and stripped. This method is a no-op when the filter has not changed.
     */
    public void setFilter(String filter) {
        if (filter.equals(appliedFilter)) return;
        appliedFilter = filter;
        applyFilter();
    }

    public void setClickHandler(GridClickHandler handler) {
        this.clickHandler = handler;
    }

    /**
     * Returns the item stack under the given pane-local mouse position, or {@code null} if the
     * cursor is outside the grid or no item is there. Uses the current filtered view.
     */
    @Nullable
    public ItemStack getHoveredStack(double localX, double localY) {
        if (!isInGrid(localX, localY)) return null;
        int col = (int) ((localX - GRID_X) / AccessTerminalLayout.SLOT_SIZE);
        int row = (int) ((localY - gridStartY()) / AccessTerminalLayout.SLOT_SIZE);
        int idx = (row + scrollOffset) * AccessTerminalLayout.NETWORK_COLS + col;
        return (idx >= 0 && idx < displayStacks.size()) ? displayStacks.get(idx) : null;
    }

    // -------------------------------------------------------------------------
    // IDialogPane
    // -------------------------------------------------------------------------

    @Override
    public int preferredWidth() {
        return AccessTerminalLayout.BG_WIDTH;
    }

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
        shiftDragVisited.clear();
        if (isInScrollbar(localX, localY)) {
            draggingScrollbar = true;
            scrollToY(localY);
            return true;
        }
        if (!isInGrid(localX, localY) || clickHandler == null) return false;

        var mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        ItemStack carried = mc.player.containerMenu.getCarried();

        if (!carried.isEmpty()) {
            if (button == 0) clickHandler.onClick(-1, 0, false);
            return true;
        }

        int col = (int) ((localX - GRID_X) / AccessTerminalLayout.SLOT_SIZE);
        int row = (int) ((localY - gridStartY()) / AccessTerminalLayout.SLOT_SIZE);
        int displayedIdx = (row + scrollOffset) * AccessTerminalLayout.NETWORK_COLS + col;

        if (displayedIdx >= 0 && displayedIdx < displayStacks.size()) {
            long count = displayCounts.get(displayedIdx);
            int maxStack = displayStacks.get(displayedIdx).getMaxStackSize();
            // Translate display index → allStacks index → server-side index
            int allStacksIdx = (toOriginal != null) ? toOriginal[displayedIdx] : displayedIdx;
            int originalIdx = (baseToOriginal != null) ? baseToOriginal[allStacksIdx] : allStacksIdx;
            if (com.mojang.blaze3d.platform.InputConstants.isKeyDown(
                            net.minecraft.client.Minecraft.getInstance().getWindow(),
                            com.mojang.blaze3d.platform.InputConstants.KEY_LSHIFT)
                    || com.mojang.blaze3d.platform.InputConstants.isKeyDown(
                            net.minecraft.client.Minecraft.getInstance().getWindow(),
                            com.mojang.blaze3d.platform.InputConstants.KEY_RSHIFT)) {
                clickHandler.onClick(originalIdx, (int) Math.min(count, maxStack), false);
                shiftDragVisited.add(originalIdx);
            } else {
                int amount = (button == 1) ? (int) Math.max(1, (count + 1) / 2) : (int) Math.min(count, maxStack);
                clickHandler.onClick(originalIdx, amount, true);
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
        var mc = Minecraft.getInstance();
        if (button == 0
                        && com.mojang.blaze3d.platform.InputConstants.isKeyDown(
                                net.minecraft.client.Minecraft.getInstance().getWindow(),
                                com.mojang.blaze3d.platform.InputConstants.KEY_LSHIFT)
                || com.mojang.blaze3d.platform.InputConstants.isKeyDown(
                                net.minecraft.client.Minecraft.getInstance().getWindow(),
                                com.mojang.blaze3d.platform.InputConstants.KEY_RSHIFT)
                        && clickHandler != null
                        && mc.player != null
                        && mc.player.containerMenu.getCarried().isEmpty()) {
            if (isInGrid(localX, localY)) {
                int col = (int) ((localX - GRID_X) / AccessTerminalLayout.SLOT_SIZE);
                int row = (int) ((localY - gridStartY()) / AccessTerminalLayout.SLOT_SIZE);
                int displayedIdx = (row + scrollOffset) * AccessTerminalLayout.NETWORK_COLS + col;
                if (displayedIdx >= 0 && displayedIdx < displayStacks.size()) {
                    long count = displayCounts.get(displayedIdx);
                    int maxStack = displayStacks.get(displayedIdx).getMaxStackSize();
                    int allStacksIdx = (toOriginal != null) ? toOriginal[displayedIdx] : displayedIdx;
                    int originalIdx = (baseToOriginal != null) ? baseToOriginal[allStacksIdx] : allStacksIdx;
                    if (count > 0 && shiftDragVisited.add(originalIdx)) {
                        clickHandler.onClick(originalIdx, (int) Math.min(count, maxStack), false);
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double localX, double localY, int button) {
        shiftDragVisited.clear();
        if (draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private int visibleRows() {
        return TremendousStorageClientConfig.getVisibleRows();
    }

    private int gridStartY() {
        return GRID_Y;
    }

    private void applyFilter() {
        if (appliedFilter.isEmpty()) {
            displayStacks = allStacks;
            displayCounts = allCounts;
            toOriginal = null;
        } else {
            List<ItemStack> fs = new ArrayList<>();
            List<Long> fc = new ArrayList<>();
            List<Integer> map = new ArrayList<>();
            for (int i = 0; i < allStacks.size(); i++) {
                if (SearchSync.matches(allStacks.get(i), appliedFilter)) {
                    fs.add(allStacks.get(i));
                    fc.add(allCounts.get(i));
                    map.add(i);
                }
            }
            displayStacks = fs;
            displayCounts = fc;
            toOriginal = map.stream().mapToInt(i -> i).toArray();
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
                && localX < AccessTerminalLayout.BG_WIDTH
                && localY >= startY
                && localY < startY + barH;
    }

    /** Maps a pane-local Y coordinate inside the scrollbar track to a scroll offset. */
    private void scrollToY(double localY) {
        int rows = visibleRows();
        int barH = rows * AccessTerminalLayout.SLOT_SIZE;
        int totalRows = Math.max(
                (displayStacks.size() + AccessTerminalLayout.NETWORK_COLS - 1) / AccessTerminalLayout.NETWORK_COLS, 1);
        int maxScroll = Math.max(0, totalRows - rows);
        if (maxScroll == 0) return;
        float scrollFraction = ((float) localY - gridStartY() - 1 - AccessTerminalLayout.SCROLLER_H / 2.0f)
                / (barH - 2 - AccessTerminalLayout.SCROLLER_H);
        scrollFraction = Math.max(0f, Math.min(1f, scrollFraction));
        scrollOffset = Math.round(scrollFraction * maxScroll);
        clampScroll();
    }

    private void clampScroll() {
        int totalRows =
                (displayStacks.size() + AccessTerminalLayout.NETWORK_COLS - 1) / AccessTerminalLayout.NETWORK_COLS;
        int maxScroll = Math.max(0, totalRows - visibleRows());
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    private void drawGrid(GuiGraphics graphics, Font font) {
        int rows = visibleRows();
        int startY = gridStartY();

        for (int row = 0; row < rows; row++) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    BG_TEXTURE,
                    GRID_X,
                    startY + row * AccessTerminalLayout.SLOT_SIZE,
                    7,
                    17,
                    AccessTerminalLayout.NETWORK_W,
                    AccessTerminalLayout.SLOT_SIZE,
                    256,
                    256);
        }

        if (displayStacks.isEmpty()) return;

        int firstIdx = scrollOffset * AccessTerminalLayout.NETWORK_COLS;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < AccessTerminalLayout.NETWORK_COLS; col++) {
                int idx = firstIdx + row * AccessTerminalLayout.NETWORK_COLS + col;
                if (idx >= displayStacks.size()) return;

                ItemStack stack = displayStacks.get(idx);
                long count = displayCounts.get(idx);
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

        // Track background with inner bevel
        int trackX = AccessTerminalLayout.SCROLLBAR_TRACK_X;
        int trackW = AccessTerminalLayout.SCROLLBAR_W;
        graphics.fill(trackX, barY, trackX + trackW, barY + barH, 0xFF8B8B8B);
        graphics.fill(trackX, barY, trackX + trackW, barY + 1, 0xFF555555);
        graphics.fill(trackX, barY, trackX + 1, barY + barH, 0xFF555555);
        graphics.fill(trackX, barY + barH - 1, trackX + trackW, barY + barH, 0xFFFFFFFF);
        graphics.fill(trackX + trackW - 1, barY, trackX + trackW, barY + barH, 0xFFFFFFFF);

        int totalRows = Math.max(
                (displayStacks.size() + AccessTerminalLayout.NETWORK_COLS - 1) / AccessTerminalLayout.NETWORK_COLS, 1);
        boolean canScroll = totalRows > rows;

        int thumbX = trackX + 1;
        int thumbY;
        if (!canScroll) {
            thumbY = barY + 1;
        } else {
            int maxScroll = totalRows - rows;
            float scrollFraction = scrollOffset / (float) maxScroll;
            thumbY = barY + 1 + (int) ((barH - 2 - AccessTerminalLayout.SCROLLER_H) * scrollFraction);
        }

        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                canScroll ? SCROLLER : SCROLLER_DISABLED,
                thumbX,
                thumbY,
                AccessTerminalLayout.SCROLLER_W,
                AccessTerminalLayout.SCROLLER_H);
    }
}
