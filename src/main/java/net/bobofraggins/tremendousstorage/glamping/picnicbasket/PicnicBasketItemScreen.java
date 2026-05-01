package net.bobofraggins.tremendousstorage.glamping.picnicbasket;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.shared.ui.ConfigDrawer;
import net.bobofraggins.tremendousstorage.shared.ui.Dialog;
import net.bobofraggins.tremendousstorage.shared.ui.LocalInventoryPane;
import net.bobofraggins.tremendousstorage.shared.ui.PlayerInventoryPane;
import net.bobofraggins.tremendousstorage.shared.ui.SearchBoxWidget;
import net.bobofraggins.tremendousstorage.shared.util.SearchSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Screen for the Picnic Basket item-form UI (opened via keybind).
 *
 * <p>Reads stored food items from the basket item's {@link DataComponents#BLOCK_ENTITY_DATA}
 * component (kept in sync by Minecraft's slot tracking). The config drawer exposes the
 * Auto-feed When Hungry toggle.
 */
public class PicnicBasketItemScreen extends AbstractContainerScreen<PicnicBasketItemMenu> {

    private final LocalInventoryPane inventoryPane;
    private final Dialog dialog;
    private final ConfigDrawer configDrawer;
    private SearchBoxWidget searchBox;

    @Nullable
    private Slot shiftDragSlot;

    public PicnicBasketItemScreen(PicnicBasketItemMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        inventoryPane = new LocalInventoryPane();
        dialog = new Dialog(
                Dialog.blankPane(PlayerInventoryPane.WIDTH, 7),
                inventoryPane,
                Dialog.blankPane(PlayerInventoryPane.WIDTH, 20),
                new PlayerInventoryPane());
        this.imageWidth = dialog.totalWidth();
        this.imageHeight = dialog.totalHeight();
        configDrawer = new ConfigDrawer(new AutoFeedPane(
                () -> {
                    ItemStack basket = getCurrentBasketStack();
                    return basket.isEmpty() || PicnicBasketItemUtils.readAutoFeed(basket);
                },
                () -> {
                    ItemStack basket = getCurrentBasketStack();
                    if (basket.isEmpty()) return;
                    boolean current = PicnicBasketItemUtils.readAutoFeed(basket);
                    PacketDistributor.sendToServer(new PicnicBasketItemAutoFeedPacket(
                            menu.getSlotType(), menu.getSlotIndex(), menu.getSlotId(), !current));
                }));
    }

    @Override
    protected void init() {
        super.init();
        dialog.init(leftPos, topPos);
        configDrawer.init(leftPos, topPos, imageHeight);
        inventoryPane.setClickHandler(
                (idx, amount, toCursor) -> PacketDistributor.sendToServer(new PicnicBasketItemInteractPacket(
                        menu.getSlotType(), menu.getSlotIndex(), menu.getSlotId(), idx, amount, toCursor)));

        searchBox = new SearchBoxWidget(font, leftPos, topPos, imageWidth);
        searchBox.getEditBox().setResponder(text -> {
            inventoryPane.setFilter(text.toLowerCase(Locale.ROOT).strip());
            SearchSync.pushToJei(text);
        });
        addRenderableWidget(searchBox.getEditBox());
        searchBox.initFilter(SearchSync.getRawFilter());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (SearchSync.isJeiAvailable()) {
            String jeiRaw = SearchSync.getRawFilter();
            if (!jeiRaw.equals(searchBox.getValue())) {
                searchBox.setValue(jeiRaw);
            }
        }
        refreshInventory();
    }

    private void refreshInventory() {
        ItemStack basketStack = getCurrentBasketStack();
        if (basketStack.isEmpty()) {
            inventoryPane.setContents(List.of(), List.of());
            return;
        }
        CustomData data = basketStack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) {
            inventoryPane.setContents(List.of(), List.of());
            return;
        }
        var registries = Minecraft.getInstance().level.registryAccess();
        CompoundTag beTag = data.copyTag();
        ListTag types = beTag.getList("Types", Tag.TAG_COMPOUND);
        int n = types.size();
        List<ItemStack> stacks = new ArrayList<>(n);
        List<Long> counts = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            CompoundTag entry = types.getCompound(i);
            ItemStack stack = ItemStack.parseOptional(registries, entry.getCompound("Type"));
            if (!stack.isEmpty()) {
                stacks.add(stack);
                counts.add(entry.getLong("Count"));
            }
        }
        inventoryPane.setContents(stacks, counts);
    }

    private ItemStack getCurrentBasketStack() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return ItemStack.EMPTY;
        if (menu.getSlotType() == PicnicBasketItemUtils.SLOT_CURIOS) {
            try {
                var inv = mc.player.getCapability(top.theillusivec4.curios.api.CuriosCapability.INVENTORY);
                if (inv != null) {
                    var entry = inv.getCurios().get(menu.getSlotId());
                    if (entry != null) {
                        return entry.getStacks().getStackInSlot(menu.getSlotIndex());
                    }
                }
            } catch (NoClassDefFoundError | Exception ignored) {
            }
            return ItemStack.EMPTY;
        }
        return mc.player.getInventory().getItem(menu.getSlotIndex());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox.getEditBox().isFocused()) {
            if (keyCode == 256) {
                searchBox.getEditBox().setFocused(false);
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
            searchBox.getEditBox().keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        shiftDragSlot = null;
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
        if (button == 0 && Screen.hasShiftDown() && menu.getCarried().isEmpty()) {
            Slot slot = hoveredSlot;
            if (slot != null && slot != shiftDragSlot && slot.hasItem()) {
                shiftDragSlot = slot;
                slotClicked(slot, slot.index, 0, ClickType.QUICK_MOVE);
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        shiftDragSlot = null;
        if (dialog.mouseReleased(mouseX, mouseY, button)) return true;
        return super.mouseReleased(mouseX, mouseY, button);
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
        configDrawer.render(graphics, font, mouseX, mouseY, partialTick);
        configDrawer.renderTab(graphics, mouseX, mouseY);
        dialog.render(graphics, font, title, mouseX, mouseY, partialTick);
        searchBox.render(graphics, font);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Title is drawn by Dialog.
    }
}
