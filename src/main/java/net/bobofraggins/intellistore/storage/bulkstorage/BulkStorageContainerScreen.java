package net.bobofraggins.intellistore.storage.bulkstorage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.bobofraggins.intellistore.shared.input.QuickStackClientEvents;
import net.bobofraggins.intellistore.shared.network.LocalStorageInteractPacket;
import net.bobofraggins.intellistore.shared.network.QuickStackPacket;
import net.bobofraggins.intellistore.shared.ui.ConfigDrawer;
import net.bobofraggins.intellistore.shared.ui.Dialog;
import net.bobofraggins.intellistore.shared.ui.LocalInventoryPane;
import net.bobofraggins.intellistore.shared.ui.PlayerInventoryPane;
import net.bobofraggins.intellistore.shared.ui.PriorityPane;
import net.bobofraggins.intellistore.shared.util.SearchSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Screen for the Bulk Storage Container.
 *
 * <p>Displays the block's stored items in a scrollable grid, a priority control (in the slide-out
 * config drawer), and the player inventory.
 */
public class BulkStorageContainerScreen extends AbstractContainerScreen<BulkStorageContainerMenu> {

    private final LocalInventoryPane inventoryPane;
    private final Dialog dialog;
    private final ConfigDrawer configDrawer;

    public BulkStorageContainerScreen(BulkStorageContainerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        inventoryPane = new LocalInventoryPane();
        dialog = new Dialog(inventoryPane, Dialog.blankPane(PlayerInventoryPane.WIDTH, 20), new PlayerInventoryPane());
        this.imageWidth = dialog.totalWidth();
        this.imageHeight = dialog.totalHeight();
        configDrawer = new ConfigDrawer(new PriorityPane(menu::getPriority, menu.getPos()));
    }

    @Override
    protected void init() {
        super.init();
        dialog.init(leftPos, topPos);
        configDrawer.init(leftPos, topPos, imageHeight);
        inventoryPane.setClickHandler((idx, amount, toCursor) -> PacketDistributor.sendToServer(
                new LocalStorageInteractPacket(menu.getPos(), true, idx, amount, toCursor)));

        addRenderableWidget(new ImageButton(
                leftPos + 3,
                topPos + 1,
                16,
                16,
                new WidgetSprites(
                        ResourceLocation.fromNamespaceAndPath("intellistore", "widget/button_config"),
                        ResourceLocation.fromNamespaceAndPath("intellistore", "widget/button_config_focused")),
                btn -> configDrawer.toggle()));

        addRenderableWidget(new ImageButton(
                leftPos + dialog.totalWidth() - 26,
                dialog.getPaneAbsY(2) - 20,
                16,
                16,
                new WidgetSprites(
                        ResourceLocation.fromNamespaceAndPath("intellistore", "widget/button_quick_stack"),
                        ResourceLocation.fromNamespaceAndPath("intellistore", "widget/button_quick_stack_focused")),
                btn -> PacketDistributor.sendToServer(new QuickStackPacket(menu.getPos(), false))));
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
        // Sort alphabetically by display name to match Access Terminal order
        List<Integer> order = new ArrayList<>(n);
        for (int i = 0; i < n; i++) order.add(i);
        order.sort(Comparator.comparing(i -> stacks.get(i).getHoverName().getString()));
        List<ItemStack> sorted = new ArrayList<>(n);
        List<Long> sortedCounts = new ArrayList<>(n);
        for (int i : order) {
            sorted.add(stacks.get(i));
            sortedCounts.add(counts.get(i));
        }
        inventoryPane.setContents(sorted, sortedCounts);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (QuickStackClientEvents.QUICK_STACK != null
                && QuickStackClientEvents.QUICK_STACK.matches(keyCode, scanCode)) {
            PacketDistributor.sendToServer(new QuickStackPacket(menu.getPos(), false));
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        int paneAbsY = dialog.getPaneAbsY(0);
        ItemStack hovered = inventoryPane.getHoveredStack(mouseX - leftPos, mouseY - paneAbsY);
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
        // Title is drawn by Dialog.
    }
}
