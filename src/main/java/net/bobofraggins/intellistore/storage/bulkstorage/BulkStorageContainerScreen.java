package net.bobofraggins.intellistore.storage.bulkstorage;

import java.util.ArrayList;
import java.util.List;
import net.bobofraggins.intellistore.shared.network.LocalStorageInteractPacket;
import net.bobofraggins.intellistore.shared.ui.Dialog;
import net.bobofraggins.intellistore.shared.ui.LocalInventoryPane;
import net.bobofraggins.intellistore.shared.ui.PlayerInventoryPane;
import net.bobofraggins.intellistore.shared.ui.PriorityPane;
import net.bobofraggins.intellistore.shared.util.SearchSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Screen for the Bulk Storage Container.
 *
 * <p>Displays the block's stored items in a scrollable grid (same layout as the Access Terminal
 * network inventory), a priority control, and the player inventory.
 */
public class BulkStorageContainerScreen extends AbstractContainerScreen<BulkStorageContainerMenu> {

    private final LocalInventoryPane inventoryPane;
    private final Dialog dialog;

    public BulkStorageContainerScreen(BulkStorageContainerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        inventoryPane = new LocalInventoryPane();
        dialog = new Dialog(
                new PriorityPane(menu::getPriority, menu.getPos()), inventoryPane, new PlayerInventoryPane());
        this.imageWidth = dialog.totalWidth();
        this.imageHeight = dialog.totalHeight();
    }

    @Override
    protected void init() {
        super.init();
        dialog.init(leftPos, topPos);
        inventoryPane.setClickHandler((idx, amount, toCursor) -> PacketDistributor.sendToServer(
                new LocalStorageInteractPacket(menu.getPos(), true, idx, amount, toCursor)));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        inventoryPane.setFilter(SearchSync.getFilter());
        refreshInventory();
    }

    private void refreshInventory() {
        BlockEntity be = Minecraft.getInstance().level.getBlockEntity(menu.getPos());
        if (!(be instanceof BulkStorageContainerBlockEntity bulk)) {
            inventoryPane.setContents(List.of(), List.of());
            return;
        }
        int n = bulk.typeCount();
        List<ItemStack> stacks = new ArrayList<>(n);
        List<Long> counts = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            stacks.add(bulk.getType(i));
            counts.add(bulk.getCount(i));
        }
        inventoryPane.setContents(stacks, counts);
    }

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

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        int paneAbsY = dialog.getPaneAbsY(1);
        ItemStack hovered = inventoryPane.getHoveredStack(mouseX - leftPos, mouseY - paneAbsY);
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
        // Title is drawn by Dialog.
    }
}
