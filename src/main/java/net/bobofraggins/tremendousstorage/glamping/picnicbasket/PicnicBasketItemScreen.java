package net.bobofraggins.tremendousstorage.glamping.picnicbasket;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.shared.config.SortMode;
import net.bobofraggins.tremendousstorage.shared.ui.ConfigDrawer;
import net.bobofraggins.tremendousstorage.shared.ui.Dialog;
import net.bobofraggins.tremendousstorage.shared.ui.LocalInventoryPane;
import net.bobofraggins.tremendousstorage.shared.ui.PlayerInventoryPane;
import net.bobofraggins.tremendousstorage.shared.ui.SearchBoxWidget;
import net.bobofraggins.tremendousstorage.shared.ui.SortPane;
import net.bobofraggins.tremendousstorage.shared.util.SearchSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

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

    private SortMode sortMode = SortMode.AMOUNT;

    @Nullable
    private Slot shiftDragSlot;

    public PicnicBasketItemScreen(PicnicBasketItemMenu menu, Inventory inv, Component title) {
        LocalInventoryPane inventoryPane_ = new LocalInventoryPane();
        Dialog dialog_ = new Dialog(
                Dialog.blankPane(PlayerInventoryPane.WIDTH, 7),
                inventoryPane_,
                Dialog.blankPane(PlayerInventoryPane.WIDTH, 20),
                new PlayerInventoryPane());
        super(menu, inv, title, dialog_.totalWidth(), dialog_.totalHeight());
        inventoryPane = inventoryPane_;
        dialog = dialog_;
        configDrawer = new ConfigDrawer(
                new AutoFeedPane(
                        () -> {
                            ItemStack basket = getCurrentBasketStack();
                            return basket.isEmpty() || PicnicBasketItemUtils.readAutoFeed(basket);
                        },
                        () -> {
                            ItemStack basket = getCurrentBasketStack();
                            if (basket.isEmpty()) return;
                            boolean current = PicnicBasketItemUtils.readAutoFeed(basket);
                            ClientPacketDistributor.sendToServer(new PicnicBasketItemAutoFeedPacket(
                                    menu.getSlotType(), menu.getSlotIndex(), menu.getSlotId(), !current));
                        }),
                new SortPane(() -> sortMode, newMode -> {
                    sortMode = newMode;
                    refreshInventory();
                }));
    }

    @Override
    protected void init() {
        super.init();
        dialog.init(leftPos, topPos);
        configDrawer.init(leftPos, topPos, imageHeight);
        inventoryPane.setClickHandler(
                (idx, amount, toCursor) -> ClientPacketDistributor.sendToServer(new PicnicBasketItemInteractPacket(
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
        var data = basketStack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) {
            inventoryPane.setContents(List.of(), List.of());
            return;
        }
        var registries = Minecraft.getInstance().level.registryAccess();
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        CompoundTag beTag = data.copyTagWithoutId();
        ListTag types = beTag.getListOrEmpty("Types");
        int n = types.size();
        List<ItemStack> stacks = new ArrayList<>(n);
        List<Long> counts = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            CompoundTag entry = types.getCompoundOrEmpty(i);
            ItemStack stack = ItemStack.OPTIONAL_CODEC
                    .parse(ops, entry.getCompoundOrEmpty("Type"))
                    .result()
                    .orElse(ItemStack.EMPTY);
            if (!stack.isEmpty()) {
                stacks.add(stack);
                counts.add(entry.getLongOr("Count", 0L));
            }
        }
        List<Integer> order = new ArrayList<>(n);
        for (int i = 0; i < n; i++) order.add(i);
        order.sort(buildComparator(stacks, counts));
        List<ItemStack> sorted = new ArrayList<>(n);
        List<Long> sortedCounts = new ArrayList<>(n);
        for (int i : order) {
            sorted.add(stacks.get(i));
            sortedCounts.add(counts.get(i));
        }
        int[] sortedToOriginal = order.stream().mapToInt(i -> i).toArray();
        inventoryPane.setContents(sorted, sortedCounts, sortedToOriginal);
    }

    private Comparator<Integer> buildComparator(List<ItemStack> stacks, List<Long> counts) {
        return switch (sortMode) {
            case NAME -> Comparator.comparing(i -> stacks.get(i).getHoverName().getString());
            case MOD -> Comparator.<Integer, String>comparing(i -> {
                        var key = BuiltInRegistries.ITEM.getKey(stacks.get(i).getItem());
                        return key != null ? key.getNamespace() : "";
                    })
                    .thenComparing(i -> stacks.get(i).getHoverName().getString());
            default -> Comparator.comparingLong((Integer i) -> counts.get(i))
                    .reversed()
                    .thenComparing(i -> stacks.get(i).getHoverName().getString());
        };
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
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean consumed) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        shiftDragSlot = null;
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

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractBackground(graphics, mouseX, mouseY, partialTick);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        int paneAbsY = dialog.getPaneAbsY(1);
        ItemStack hovered = inventoryPane.getHoveredStack(mouseX - leftPos, mouseY - paneAbsY);
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
        // Title is drawn by Dialog.
    }
}
