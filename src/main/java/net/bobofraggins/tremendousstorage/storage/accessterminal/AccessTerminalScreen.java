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

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public AccessTerminalScreen(AccessTerminalMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        networkPane = new NetworkInventoryPane(menu);
        if (menu.hasCraftingUpgrade()) {
            dialog = new Dialog(networkPane, new CraftingGridPane(), new PlayerInventoryPane());
        } else {
            dialog = new Dialog(networkPane, new PlayerInventoryPane());
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
        searchBox =
                new EditBox(font, leftPos + imageWidth - 5 - 2 - searchW, topPos + 4, searchW, 12, Component.empty());
        searchBox.setMaxLength(64);
        searchBox.setHint(Component.literal("Search..."));
        searchBox.setResponder(text -> {
            networkPane.setFilter(text.toLowerCase(Locale.ROOT).strip());
            SearchSync.pushToJei(text);
        });
        addRenderableWidget(searchBox);

        // Config drawer toggle button — sits in the tab that protrudes from the dialog's left edge.
        addRenderableWidget(new PressableIconButton(
                leftPos - ConfigDrawer.TAB_W + 4,
                topPos + 3,
                16,
                16,
                ResourceLocation.fromNamespaceAndPath("tremendousstorage", "widget/button_config"),
                ResourceLocation.fromNamespaceAndPath("tremendousstorage", "widget/button_config_focused"),
                configDrawer::toggle));

        // Quick stack button above the player inventory, right-aligned.
        // Pane index: network(0), [crafting(1),] playerInv(last)
        int playerInvPaneIndex = menu.hasCraftingUpgrade() ? 2 : 1;
        addRenderableWidget(new PressableIconButton(
                leftPos + dialog.totalWidth() - 26,
                dialog.getPaneAbsY(playerInvPaneIndex) - 20,
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
        int bx = searchBox.getX() - 2;
        int by = searchBox.getY() - 2;
        int bw = searchBox.getWidth() + 4;
        int bh = searchBox.getHeight() + 4;
        int c = 0xFF373737;
        graphics.fill(bx, by, bx + bw, by + 2, c);
        graphics.fill(bx, by + bh - 2, bx + bw, by + bh, c);
        graphics.fill(bx, by + 2, bx + 2, by + bh - 2, c);
        graphics.fill(bx + bw - 2, by + 2, bx + bw, by + bh - 2, c);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Title is drawn by Dialog; suppress the default label rendering.
    }
}
