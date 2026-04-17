package net.bobofraggins.tremendousstorage.storage.accessterminal;

import java.util.List;
import java.util.Locale;
import net.bobofraggins.tremendousstorage.shared.config.SortMode;
import net.bobofraggins.tremendousstorage.shared.input.QuickStackClientEvents;
import net.bobofraggins.tremendousstorage.shared.network.QuickStackPacket;
import net.bobofraggins.tremendousstorage.shared.network.RequestSatContentsPacket;
import net.bobofraggins.tremendousstorage.shared.network.SatContentsPacket;
import net.bobofraggins.tremendousstorage.shared.network.SetSortModePacket;
import net.bobofraggins.tremendousstorage.shared.ui.ConfigDrawer;
import net.bobofraggins.tremendousstorage.shared.ui.CraftingGridPane;
import net.bobofraggins.tremendousstorage.shared.ui.Dialog;
import net.bobofraggins.tremendousstorage.shared.ui.PlayerInventoryPane;
import net.bobofraggins.tremendousstorage.shared.ui.PressableIconButton;
import net.bobofraggins.tremendousstorage.shared.ui.SortPane;
import net.bobofraggins.tremendousstorage.shared.util.SearchSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Screen for the Storage Access Terminal.
 *
 * <p>Rendering is split into three {@link net.bobofraggins.tremendousstorage.shared.ui.IDialogPane}
 * instances managed by a {@link Dialog}:
 *
 * <ul>
 *   <li>{@link NetworkInventoryPane} — scrollable network item grid
 *   <li>{@link CraftingGridPane} — 3×3 crafting grid, arrow, and result slot backgrounds
 *   <li>{@link PlayerInventoryPane} — player inventory and hotbar slot backgrounds
 * </ul>
 *
 * <p>All pixel coordinates originate from {@link AccessTerminalLayout}.
 */
public class AccessTerminalScreen extends AbstractContainerScreen<AccessTerminalMenu> {

    // -------------------------------------------------------------------------
    // Panes + dialog
    // -------------------------------------------------------------------------

    private final NetworkInventoryPane networkPane;
    private final Dialog dialog;
    private final ConfigDrawer configDrawer;
    private EditBox searchBox;

    // Visual bounding box of the search field (used for border drawing and hint text).
    private int searchBx, searchBy, searchBw, searchBh;
    private static final int SEARCH_MARGIN = 3;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public AccessTerminalScreen(AccessTerminalMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        networkPane = new NetworkInventoryPane(menu);
        if (menu.hasCraftingUpgrade()) {
            dialog = new Dialog(
                    networkPane,
                    new CraftingGridPane(AccessTerminalLayout.CRAFTING_GRID_X, AccessTerminalLayout.CRAFTING_RESULT_X),
                    new PlayerInventoryPane(AccessTerminalLayout.PLAYER_INV_X));
        } else {
            dialog = new Dialog(
                    networkPane,
                    Dialog.blankPane(PlayerInventoryPane.WIDTH, 20),
                    new PlayerInventoryPane(AccessTerminalLayout.PLAYER_INV_X));
        }
        this.imageWidth = dialog.totalWidth();
        this.imageHeight = dialog.totalHeight();
        configDrawer = new ConfigDrawer(new SortPane(networkPane::getSortMode, this::cycleSortMode));
    }

    // -------------------------------------------------------------------------
    // Init
    // -------------------------------------------------------------------------

    @Override
    protected void init() {
        super.init();
        dialog.init(leftPos, topPos);
        configDrawer.init(leftPos, topPos, imageHeight);

        // Restore persisted sort mode from the client-side block entity.
        if (Minecraft.getInstance().level != null) {
            var be = Minecraft.getInstance().level.getBlockEntity(menu.getSatPos());
            if (be instanceof AccessTerminalBlockEntity at) {
                networkPane.setSortMode(at.getSortMode());
            }
        }

        // Search box — right-aligned in the title bar.
        int searchW = 75;
        searchBx = leftPos + imageWidth - 5 - 2 - searchW;
        searchBy = topPos + 4;
        searchBw = searchW;
        searchBh = 12;
        int textH = font.lineHeight;
        searchBox = new EditBox(
                font,
                searchBx + SEARCH_MARGIN,
                searchBy + (searchBh - textH) / 2,
                searchBw - SEARCH_MARGIN,
                textH,
                Component.empty());
        searchBox.setMaxLength(64);
        searchBox.setBordered(false);
        searchBox.setTextColor(0xFFFFFFFF);
        searchBox.setResponder(text -> {
            networkPane.setFilter(text.toLowerCase(Locale.ROOT).strip());
            SearchSync.pushToJei(text);
        });
        addRenderableWidget(searchBox);
        String existingFilter = SearchSync.getRawFilter();
        searchBox.setValue(existingFilter);
        if (!existingFilter.isEmpty()) {
            // Select all so the user immediately sees there is an active filter and can clear it.
            searchBox.setFocused(true);
            searchBox.setHighlightPos(0);
        }

        // Quick stack button: 4 px above and horizontally centered on the top-right player inv slot.
        int buttonX = leftPos
                + AccessTerminalLayout.PLAYER_INV_X
                + 8 * AccessTerminalLayout.SLOT_SIZE
                + (AccessTerminalLayout.SLOT_SIZE - 16) / 2;
        int buttonY = dialog.getPaneAbsY(2) - 20;
        addRenderableWidget(new PressableIconButton(
                buttonX,
                buttonY,
                16,
                16,
                ResourceLocation.fromNamespaceAndPath("tremendousstorage", "widget/button_quick_stack"),
                ResourceLocation.fromNamespaceAndPath("tremendousstorage", "widget/button_quick_stack_focused"),
                () -> PacketDistributor.sendToServer(new QuickStackPacket(menu.getNiPos(), true))));

        if (menu.hasNetwork()) {
            PacketDistributor.sendToServer(new RequestSatContentsPacket(menu.getNiPos()));
        }
        SatContentsPacket.PENDING_STACKS = null;
        SatContentsPacket.PENDING_COUNTS = List.of();
        SatContentsPacket.PENDING_FLUID_INDICES = java.util.Set.of();
    }

    // -------------------------------------------------------------------------
    // Tick
    // -------------------------------------------------------------------------

    @Override
    protected void containerTick() {
        super.containerTick();
        // Sync JEI → EditBox when JEI is available and its filter differs from ours.
        if (SearchSync.isJeiAvailable()) {
            String jeiRaw = SearchSync.getRawFilter();
            if (!jeiRaw.equals(searchBox.getValue())) {
                searchBox.setValue(jeiRaw); // triggers responder → updates pane + pushes back to JEI
            }
        }
        List<ItemStack> pending = SatContentsPacket.PENDING_STACKS;
        if (pending != null && pending != networkPane.getStacks()) {
            networkPane.setContents(pending, SatContentsPacket.PENDING_COUNTS);
            networkPane.setFluidIndices(SatContentsPacket.PENDING_FLUID_INDICES);
        }
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private void cycleSortMode() {
        SortMode next = networkPane.getSortMode().next();
        networkPane.setSortMode(next);
        PacketDistributor.sendToServer(new SetSortModePacket(menu.getSatPos(), next));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox.isFocused()) {
            if (keyCode == 256) { // Escape: unfocus the search box without closing the screen
                searchBox.setFocused(false);
                return true;
            }
            searchBox.keyPressed(keyCode, scanCode, modifiers);
            return true; // consume all keys while search box is focused
        }
        if (QuickStackClientEvents.QUICK_STACK != null
                && QuickStackClientEvents.QUICK_STACK.matches(keyCode, scanCode)
                && menu.hasNetwork()) {
            PacketDistributor.sendToServer(new QuickStackPacket(menu.getNiPos(), true));
            return true;
        }
        if (QuickStackClientEvents.CYCLE_SORT != null && QuickStackClientEvents.CYCLE_SORT.matches(keyCode, scanCode)) {
            cycleSortMode();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (configDrawer.mouseClicked(mouseX, mouseY, button)) return true;
        if (dialog.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (dialog.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dialog.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dialog.mouseReleased(mouseX, mouseY, button)) return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        // Tooltip for hovered network slot
        int networkPaneAbsY = dialog.getPaneAbsY(0);
        ItemStack hovered = networkPane.getHoveredStack(mouseX - leftPos, mouseY - networkPaneAbsY);
        if (hovered != null) {
            graphics.renderTooltip(font, hovered, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        configDrawer.render(graphics, font, mouseX, mouseY, partialTick);
        dialog.render(graphics, font, title, mouseX, mouseY, partialTick);
        drawSearchBorder(graphics);
    }

    private void drawSearchBorder(GuiGraphics graphics) {
        // Background
        graphics.fill(searchBx, searchBy, searchBx + searchBw, searchBy + searchBh, 0xFF8B8B8B);

        // Shadow: top and left edges (recessed look)
        graphics.fill(searchBx - 1, searchBy - 1, searchBx + searchBw + 1, searchBy, 0xFF555555);
        graphics.fill(searchBx - 1, searchBy, searchBx, searchBy + searchBh + 1, 0xFF555555);

        // Highlight: bottom and right edges
        graphics.fill(searchBx, searchBy + searchBh, searchBx + searchBw + 1, searchBy + searchBh + 1, 0xFFFFFFFF);
        graphics.fill(searchBx + searchBw, searchBy, searchBx + searchBw + 1, searchBy + searchBh, 0xFFFFFFFF);

        // Hint text — drawn manually so it respects the margin and vertical centering
        if (searchBox.getValue().isEmpty() && !searchBox.isFocused()) {
            int hintX = searchBx + SEARCH_MARGIN;
            int hintY = searchBy + (searchBh - font.lineHeight) / 2 + 1;
            graphics.drawString(font, "Search...", hintX, hintY, 0xFFAAAAAA, false);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Title is drawn by Dialog; suppress the default label rendering.
    }
}
