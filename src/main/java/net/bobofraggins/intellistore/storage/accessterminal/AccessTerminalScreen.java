package net.bobofraggins.intellistore.storage.accessterminal;

import java.util.List;
import net.bobofraggins.intellistore.shared.config.SortMode;
import net.bobofraggins.intellistore.shared.input.QuickStackClientEvents;
import net.bobofraggins.intellistore.shared.network.QuickStackPacket;
import net.bobofraggins.intellistore.shared.network.RequestSatContentsPacket;
import net.bobofraggins.intellistore.shared.network.SatContentsPacket;
import net.bobofraggins.intellistore.shared.network.SetSortModePacket;
import net.bobofraggins.intellistore.shared.ui.ConfigDrawer;
import net.bobofraggins.intellistore.shared.ui.Dialog;
import net.bobofraggins.intellistore.shared.ui.PlayerInventoryPane;
import net.bobofraggins.intellistore.shared.ui.PressableIconButton;
import net.bobofraggins.intellistore.shared.ui.SortPane;
import net.bobofraggins.intellistore.shared.util.SearchSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Screen for the Storage Access Terminal.
 *
 * <p>Rendering is split into three {@link net.bobofraggins.intellistore.shared.ui.IDialogPane}
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

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public AccessTerminalScreen(AccessTerminalMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        networkPane = new NetworkInventoryPane(menu);
        dialog = new Dialog(networkPane, new CraftingGridPane(), new PlayerInventoryPane());
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

        // Config drawer toggle button in the title bar, left-aligned.
        addRenderableWidget(new PressableIconButton(
                leftPos + 8,
                topPos + 6,
                16,
                16,
                ResourceLocation.fromNamespaceAndPath("intellistore", "widget/button_config"),
                ResourceLocation.fromNamespaceAndPath("intellistore", "widget/button_config_focused"),
                configDrawer::toggle));

        // Quick stack button above the player inventory, right-aligned.
        addRenderableWidget(new PressableIconButton(
                leftPos + dialog.totalWidth() - 26,
                dialog.getPaneAbsY(2) - 20,
                16,
                16,
                ResourceLocation.fromNamespaceAndPath("intellistore", "widget/button_quick_stack"),
                ResourceLocation.fromNamespaceAndPath("intellistore", "widget/button_quick_stack_focused"),
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
        networkPane.setFilter(SearchSync.getFilter());
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
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Title is drawn by Dialog; suppress the default label rendering.
    }
}
