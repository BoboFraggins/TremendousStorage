package net.bobofraggins.tremendousstorage.storage.accessterminal;

import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.shared.config.SortMode;
import net.bobofraggins.tremendousstorage.shared.input.QuickStackClientEvents;
import net.bobofraggins.tremendousstorage.shared.network.ClearCraftingGridPacket;
import net.bobofraggins.tremendousstorage.shared.network.CycleRecipePacket;
import net.bobofraggins.tremendousstorage.shared.network.QuickStackPacket;
import net.bobofraggins.tremendousstorage.shared.network.RequestSatContentsPacket;
import net.bobofraggins.tremendousstorage.shared.network.SatContentsPacket;
import net.bobofraggins.tremendousstorage.shared.network.SetSortModePacket;
import net.bobofraggins.tremendousstorage.shared.ui.ConfigDrawer;
import net.bobofraggins.tremendousstorage.shared.ui.CraftingGridPane;
import net.bobofraggins.tremendousstorage.shared.ui.CycleArrowButton;
import net.bobofraggins.tremendousstorage.shared.ui.Dialog;
import net.bobofraggins.tremendousstorage.shared.ui.PlayerInventoryPane;
import net.bobofraggins.tremendousstorage.shared.ui.PressableIconButton;
import net.bobofraggins.tremendousstorage.shared.ui.SearchBoxWidget;
import net.bobofraggins.tremendousstorage.shared.ui.SortPane;
import net.bobofraggins.tremendousstorage.shared.util.SearchSync;
import net.minecraft.client.Minecraft;
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

    @Nullable
    private final CraftingGridPane craftingGridPane;

    private SearchBoxWidget searchBox;

    @Nullable
    private CycleArrowButton upArrow;

    @Nullable
    private CycleArrowButton downArrow;

    @Nullable
    private Slot shiftDragSlot;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public AccessTerminalScreen(AccessTerminalMenu menu, Inventory inv, Component title) {
        NetworkInventoryPane networkPane_ = new NetworkInventoryPane(menu);
        Dialog dialog_;
        CraftingGridPane craftingGridPane_ = null;
        if (menu.hasCraftingUpgrade()) {
            craftingGridPane_ =
                    new CraftingGridPane(AccessTerminalLayout.CRAFTING_GRID_X, AccessTerminalLayout.CRAFTING_RESULT_X);
            dialog_ = new Dialog(
                    networkPane_, craftingGridPane_, new PlayerInventoryPane(AccessTerminalLayout.PLAYER_INV_X));
        } else {
            dialog_ = new Dialog(
                    networkPane_,
                    Dialog.blankPane(PlayerInventoryPane.WIDTH, 20),
                    new PlayerInventoryPane(AccessTerminalLayout.PLAYER_INV_X));
        }
        super(menu, inv, title, dialog_.totalWidth(), dialog_.totalHeight());
        networkPane = networkPane_;
        dialog = dialog_;
        craftingGridPane = craftingGridPane_;
        configDrawer = new ConfigDrawer(new SortPane(networkPane_::getSortMode, newMode -> {
            networkPane_.setSortMode(newMode);
            ClientPacketDistributor.sendToServer(new SetSortModePacket(menu.getSatPos(), newMode));
        }));
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
        searchBox = new SearchBoxWidget(font, leftPos, topPos, imageWidth);
        searchBox.getEditBox().setResponder(text -> {
            networkPane.setFilter(text.toLowerCase(Locale.ROOT).strip());
            SearchSync.pushToJei(text);
        });
        addRenderableWidget(searchBox.getEditBox());
        searchBox.initFilter(SearchSync.getRawFilter());

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
                Identifier.fromNamespaceAndPath("tremendousstorage", "widget/button_quick_stack"),
                Identifier.fromNamespaceAndPath("tremendousstorage", "widget/button_quick_stack_focused"),
                () -> ClientPacketDistributor.sendToServer(new QuickStackPacket(menu.getNiPos(), true))));

        if (craftingGridPane != null) {
            int btnX = leftPos + craftingGridPane.clearButtonLocalX();
            int btnY = dialog.getPaneAbsY(1);
            addRenderableWidget(new PressableIconButton(
                    btnX,
                    btnY,
                    CraftingGridPane.BUTTON_SIZE,
                    CraftingGridPane.BUTTON_SIZE,
                    Identifier.fromNamespaceAndPath("tremendousstorage", "widget/button_clear"),
                    Identifier.fromNamespaceAndPath("tremendousstorage", "widget/button_clear_focused"),
                    () -> {
                        boolean shift = com.mojang.blaze3d.platform.InputConstants.isKeyDown(
                                        net.minecraft.client.Minecraft.getInstance()
                                                .getWindow(),
                                        com.mojang.blaze3d.platform.InputConstants.KEY_LSHIFT)
                                || com.mojang.blaze3d.platform.InputConstants.isKeyDown(
                                        net.minecraft.client.Minecraft.getInstance()
                                                .getWindow(),
                                        com.mojang.blaze3d.platform.InputConstants.KEY_RSHIFT);
                        ClientPacketDistributor.sendToServer(new ClearCraftingGridPacket(!shift));
                    }));
        }

        if (menu.hasCraftingUpgrade()) {
            // Up/down arrows above and below the crafting arrow for recipe cycling.
            // Arrow occupies local y [ARROW_LOCAL_Y .. ARROW_LOCAL_Y + ARROW_H] within pane 1.
            int craftingPaneAbsY = dialog.getPaneAbsY(1);
            int arrowLocalY = CraftingGridPane.ARROW_LOCAL_Y; // 13
            int arrowH = AccessTerminalLayout.ARROW_H; // 22
            int paneH = CraftingGridPane.HEIGHT; // 58
            int btnW = 9, btnH = 6;
            int btnX = leftPos + AccessTerminalLayout.ARROW_X + (AccessTerminalLayout.ARROW_W - btnW) / 2;
            int upBtnY = craftingPaneAbsY + (arrowLocalY - btnH) / 2;
            int downBtnY = craftingPaneAbsY + arrowLocalY + arrowH + (paneH - arrowLocalY - arrowH - btnH) / 2;

            upArrow = addRenderableWidget(new CycleArrowButton(
                    btnX,
                    upBtnY,
                    btnW,
                    btnH,
                    true,
                    () -> ClientPacketDistributor.sendToServer(new CycleRecipePacket(menu.getSatPos(), -1))));
            downArrow = addRenderableWidget(new CycleArrowButton(
                    btnX,
                    downBtnY,
                    btnW,
                    btnH,
                    false,
                    () -> ClientPacketDistributor.sendToServer(new CycleRecipePacket(menu.getSatPos(), +1))));
        }

        if (menu.hasNetwork()) {
            ClientPacketDistributor.sendToServer(new RequestSatContentsPacket(menu.getNiPos()));
        }
        SatContentsPacket.PENDING_STACKS = null;
        SatContentsPacket.PENDING_COUNTS = List.of();
        SatContentsPacket.PENDING_FLUID_INDICES = java.util.Set.of();
        SatContentsPacket.PENDING_CAPACITY = 0L;
    }

    // -------------------------------------------------------------------------
    // Tick
    // -------------------------------------------------------------------------

    @Override
    protected void containerTick() {
        super.containerTick();
        if (upArrow != null) {
            boolean show = menu.getMatchingRecipeCount() > 1;
            upArrow.visible = show;
            downArrow.visible = show;
        }
        // Sync JEI → EditBox when JEI is available and its filter differs from ours.
        if (SearchSync.isJeiAvailable()) {
            String jeiRaw = SearchSync.getRawFilter();
            if (!jeiRaw.equals(searchBox.getValue())) {
                searchBox.setValue(jeiRaw);
            }
        }
        List<ItemStack> pending = SatContentsPacket.PENDING_STACKS;
        if (pending != null && pending != networkPane.getStacks()) {
            networkPane.setContents(pending, SatContentsPacket.PENDING_COUNTS);
            networkPane.setFluidIndices(SatContentsPacket.PENDING_FLUID_INDICES);
            networkPane.setCapacity(SatContentsPacket.PENDING_CAPACITY);
        }
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private void cycleSortMode() {
        SortMode next = networkPane.getSortMode().next();
        networkPane.setSortMode(next);
        ClientPacketDistributor.sendToServer(new SetSortModePacket(menu.getSatPos(), next));
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key();
        int scanCode = event.scancode();
        int modifiers = event.modifiers();

        if (searchBox.getEditBox().isFocused()) {
            if (keyCode == 256) {
                searchBox.getEditBox().setFocused(false);
                return super.keyPressed(event);
            }
            searchBox.getEditBox().keyPressed(event);
            return true;
        }
        if (QuickStackClientEvents.QUICK_STACK != null
                && QuickStackClientEvents.QUICK_STACK.matches(event)
                && menu.hasNetwork()) {
            ClientPacketDistributor.sendToServer(new QuickStackPacket(menu.getNiPos(), true));
            return true;
        }
        if (QuickStackClientEvents.CYCLE_SORT != null && QuickStackClientEvents.CYCLE_SORT.matches(event)) {
            cycleSortMode();
            return true;
        }
        if (menu.hasCraftingUpgrade()) {
            if (QuickStackClientEvents.CLEAR_GRID_TO_STORAGE != null
                    && QuickStackClientEvents.CLEAR_GRID_TO_STORAGE.matches(event)) {
                ClientPacketDistributor.sendToServer(new ClearCraftingGridPacket(true));
                return true;
            }
            if (QuickStackClientEvents.CLEAR_GRID_TO_INVENTORY != null
                    && QuickStackClientEvents.CLEAR_GRID_TO_INVENTORY.matches(event)) {
                ClientPacketDistributor.sendToServer(new ClearCraftingGridPacket(false));
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean consumed) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        shiftDragSlot = null;
        if (QuickStackClientEvents.quickStackMatchesMouse(button) && menu.hasNetwork()) {
            ClientPacketDistributor.sendToServer(new QuickStackPacket(menu.getNiPos(), true));
            return true;
        }
        if (QuickStackClientEvents.cycleSortMatchesMouse(button)) {
            cycleSortMode();
            return true;
        }
        if (configDrawer.mouseClicked(mouseX, mouseY, button)) return true;
        if (dialog.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(event, consumed);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (dialog.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        if (dialog.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
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
        if (dialog.mouseReleased(mouseX, mouseY, button)) return true;
        return super.mouseReleased(event);
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractBackground(graphics, mouseX, mouseY, partialTick);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        // Tooltip for hovered network slot
        int networkPaneAbsY = dialog.getPaneAbsY(0);
        ItemStack hovered = networkPane.getHoveredStack(mouseX - leftPos, mouseY - networkPaneAbsY);
        if (hovered != null) {
            graphics.setTooltipForNextFrame(font, hovered, mouseX, mouseY);
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        configDrawer.render(graphics, font, mouseX, mouseY, partialTick);
        configDrawer.renderTab(graphics, mouseX, mouseY);
        dialog.render(graphics, font, title, mouseX, mouseY, partialTick);
        searchBox.render(graphics, font);
        super.extractContents(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Title is drawn by Dialog; suppress the default label rendering.
    }
}
