package net.bobofraggins.tremendousstorage.storage.backpack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.shared.config.SortMode;
import net.bobofraggins.tremendousstorage.shared.input.QuickStackClientEvents;
import net.bobofraggins.tremendousstorage.shared.network.BackpackInteractPacket;
import net.bobofraggins.tremendousstorage.shared.network.BackpackQuickStackPacket;
import net.bobofraggins.tremendousstorage.shared.network.ClearCraftingGridPacket;
import net.bobofraggins.tremendousstorage.shared.network.SetBackpackPriorityPacket;
import net.bobofraggins.tremendousstorage.shared.network.SetBackpackSortModePacket;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.ui.ConfigDrawer;
import net.bobofraggins.tremendousstorage.shared.ui.CraftingGridPane;
import net.bobofraggins.tremendousstorage.shared.ui.Dialog;
import net.bobofraggins.tremendousstorage.shared.ui.LocalInventoryPane;
import net.bobofraggins.tremendousstorage.shared.ui.PlayerInventoryPane;
import net.bobofraggins.tremendousstorage.shared.ui.PressableIconButton;
import net.bobofraggins.tremendousstorage.shared.ui.PriorityPane;
import net.bobofraggins.tremendousstorage.shared.ui.SearchBoxWidget;
import net.bobofraggins.tremendousstorage.shared.ui.SortPane;
import net.bobofraggins.tremendousstorage.shared.util.SearchSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Screen for the Tremendous Backpack.
 *
 * <p>Displays the backpack's stored items in a scrollable grid, priority and sort controls in the
 * slide-out config drawer, and the player inventory. Reads contents from the client-side backpack
 * ItemStack's {@link BackpackContents} data component (synced automatically by
 * Minecraft's slot tracking when the server modifies the item).
 */
public class BackpackScreen extends AbstractContainerScreen<BackpackMenu> {

    private final LocalInventoryPane inventoryPane;
    private final Dialog dialog;
    private final ConfigDrawer configDrawer;

    @Nullable
    private final CraftingGridPane craftingGridPane;

    private SearchBoxWidget searchBox;

    /** Client-side sort mode; updated optimistically on cycle for snappy UI. */
    private SortMode sortMode = SortMode.AMOUNT;

    @Nullable
    private Slot shiftDragSlot;

    public BackpackScreen(BackpackMenu menu, Inventory inv, Component title) {
        LocalInventoryPane inventoryPane_ = new LocalInventoryPane();
        Dialog dialog_;
        CraftingGridPane craftingGridPane_ = null;
        if (menu.hasCraftingUpgrade()) {
            craftingGridPane_ = new CraftingGridPane();
            dialog_ = new Dialog(
                    Dialog.blankPane(PlayerInventoryPane.WIDTH, 7),
                    inventoryPane_,
                    craftingGridPane_,
                    new PlayerInventoryPane());
        } else {
            dialog_ = new Dialog(
                    Dialog.blankPane(PlayerInventoryPane.WIDTH, 7),
                    inventoryPane_,
                    Dialog.blankPane(PlayerInventoryPane.WIDTH, 20),
                    new PlayerInventoryPane());
        }
        super(menu, inv, title, dialog_.totalWidth(), dialog_.totalHeight());
        inventoryPane = inventoryPane_;
        dialog = dialog_;
        craftingGridPane = craftingGridPane_;
        configDrawer = new ConfigDrawer(
                new PriorityPane(
                        menu::getPriority,
                        p -> ClientPacketDistributor.sendToServer(new SetBackpackPriorityPacket(
                                menu.getSlotType(), menu.getSlotIndex(), menu.getSlotId(), p))),
                new SortPane(() -> sortMode, newMode -> {
                    sortMode = newMode;
                    ClientPacketDistributor.sendToServer(new SetBackpackSortModePacket(
                            menu.getSlotType(), menu.getSlotIndex(), menu.getSlotId(), sortMode.ordinal()));
                }));
    }

    @Override
    protected void init() {
        super.init();
        dialog.init(leftPos, topPos);
        configDrawer.init(leftPos, topPos, imageHeight);
        inventoryPane.setClickHandler(
                (idx, amount, toCursor) -> ClientPacketDistributor.sendToServer(new BackpackInteractPacket(
                        menu.getSlotType(), menu.getSlotIndex(), menu.getSlotId(), idx, amount, toCursor)));

        // Read initial sort mode from the backpack's current contents
        BackpackContents contents = getCurrentContents();
        sortMode = contents.sortMode();

        // Search box — right-aligned in the title bar.
        searchBox = new SearchBoxWidget(font, leftPos, topPos, imageWidth);
        searchBox.getEditBox().setResponder(text -> {
            inventoryPane.setFilter(text.toLowerCase(Locale.ROOT).strip());
            SearchSync.pushToJei(text);
        });
        addRenderableWidget(searchBox.getEditBox());
        searchBox.initFilter(SearchSync.getRawFilter());

        // Quick Stack button — above the player inventory pane (pane index 3)
        addRenderableWidget(new PressableIconButton(
                leftPos + dialog.totalWidth() - 26,
                dialog.getPaneAbsY(3) - 20,
                16,
                16,
                Identifier.fromNamespaceAndPath("tremendousstorage", "widget/button_quick_stack"),
                Identifier.fromNamespaceAndPath("tremendousstorage", "widget/button_quick_stack_focused"),
                () -> ClientPacketDistributor.sendToServer(
                        new BackpackQuickStackPacket(menu.getSlotType(), menu.getSlotIndex(), menu.getSlotId()))));

        if (craftingGridPane != null) {
            int btnX = leftPos + craftingGridPane.clearButtonLocalX();
            int btnY = dialog.getPaneAbsY(2);
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
        BackpackContents contents = getCurrentContents();
        int n = contents.typeCount();
        List<ItemStack> stacks = new ArrayList<>(n);
        List<Long> counts = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            stacks.add(contents.getType(i));
            counts.add(contents.getCount(i));
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

    /**
     * Reads the backpack's contents from the client-side player inventory (or Curios slot).
     * Falls back to EMPTY if the item can't be found.
     */
    private BackpackContents getCurrentContents() {
        ItemStack stack = getClientBackpackStack();
        if (stack.isEmpty()) return BackpackContents.EMPTY;
        return stack.getOrDefault(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), BackpackContents.EMPTY);
    }

    private ItemStack getClientBackpackStack() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return ItemStack.EMPTY;

        if (menu.getSlotType() == net.bobofraggins.tremendousstorage.shared.network.OpenBackpackPacket.SLOT_CURIOS) {
            try {
                var inv = mc.player.getCapability(top.theillusivec4.curios.api.CuriosCapability.INVENTORY);
                if (inv != null) {
                    var entry = inv.getCurios().get(menu.getSlotId());
                    if (entry != null) {
                        ItemStack s = entry.getStacks().getStackInSlot(menu.getSlotIndex());
                        if (s.getItem() instanceof BackpackItem) return s;
                    }
                }
            } catch (NoClassDefFoundError | Exception ignored) {
            }
            return ItemStack.EMPTY;
        }

        ItemStack s = mc.player.getInventory().getItem(menu.getSlotIndex());
        return s.getItem() instanceof BackpackItem ? s : ItemStack.EMPTY;
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
        if (QuickStackClientEvents.QUICK_STACK != null && QuickStackClientEvents.QUICK_STACK.matches(event)) {
            ClientPacketDistributor.sendToServer(
                    new BackpackQuickStackPacket(menu.getSlotType(), menu.getSlotIndex(), menu.getSlotId()));
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
        if (QuickStackClientEvents.quickStackMatchesMouse(button)) {
            ClientPacketDistributor.sendToServer(
                    new BackpackQuickStackPacket(menu.getSlotType(), menu.getSlotIndex(), menu.getSlotId()));
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
