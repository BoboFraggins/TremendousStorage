package net.bobofraggins.intellistore.storage.accessterminal;

import java.util.List;
import net.bobofraggins.intellistore.shared.network.RequestSatContentsPacket;
import net.bobofraggins.intellistore.shared.network.SatContentsPacket;
import net.bobofraggins.intellistore.shared.ui.Dialog;
import net.bobofraggins.intellistore.shared.ui.PlayerInventoryPane;
import net.bobofraggins.intellistore.shared.util.SearchSync;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
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

    private NetworkInventoryPane networkPane;
    private Dialog dialog;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public AccessTerminalScreen(AccessTerminalMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        networkPane = new NetworkInventoryPane(menu);
        dialog = new Dialog(networkPane, new CraftingGridPane(), new PlayerInventoryPane());
        this.imageWidth = dialog.totalWidth();
        this.imageHeight = dialog.totalHeight();
    }

    // -------------------------------------------------------------------------
    // Init
    // -------------------------------------------------------------------------

    @Override
    protected void init() {
        super.init();
        dialog.init(leftPos, topPos);

        if (menu.hasNetwork()) {
            PacketDistributor.sendToServer(new RequestSatContentsPacket(menu.getNiPos()));
        }
        SatContentsPacket.PENDING_STACKS = null;
        SatContentsPacket.PENDING_COUNTS = List.of();
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
        }
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (dialog.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (dialog.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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
        dialog.render(graphics, font, title, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Title is drawn by Dialog; suppress the default label rendering.
    }
}
