package net.bobofraggins.tremendousstorage.storage.chest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.glamping.picnicbasket.AutoFeedPane;
import net.bobofraggins.tremendousstorage.glamping.picnicbasket.EnderPicnicBasketBlockEntity;
import net.bobofraggins.tremendousstorage.glamping.picnicbasket.PicnicBasketBlockEntity;
import net.bobofraggins.tremendousstorage.glamping.picnicbasket.SetAutoFeedPacket;
import net.bobofraggins.tremendousstorage.shared.config.SortMode;
import net.bobofraggins.tremendousstorage.shared.input.QuickStackClientEvents;
import net.bobofraggins.tremendousstorage.shared.network.LocalStorageInteractPacket;
import net.bobofraggins.tremendousstorage.shared.network.QuickStackPacket;
import net.bobofraggins.tremendousstorage.shared.network.SetPriorityPacket;
import net.bobofraggins.tremendousstorage.shared.network.SetSortModePacket;
import net.bobofraggins.tremendousstorage.shared.ui.ConfigDrawer;
import net.bobofraggins.tremendousstorage.shared.ui.CraftingGridPane;
import net.bobofraggins.tremendousstorage.shared.ui.Dialog;
import net.bobofraggins.tremendousstorage.shared.ui.IDialogPane;
import net.bobofraggins.tremendousstorage.shared.ui.LocalInventoryPane;
import net.bobofraggins.tremendousstorage.shared.ui.PlayerInventoryPane;
import net.bobofraggins.tremendousstorage.shared.ui.PressableIconButton;
import net.bobofraggins.tremendousstorage.shared.ui.PriorityPane;
import net.bobofraggins.tremendousstorage.shared.ui.PullerSidesPane;
import net.bobofraggins.tremendousstorage.shared.ui.SearchBoxWidget;
import net.bobofraggins.tremendousstorage.shared.ui.SortPane;
import net.bobofraggins.tremendousstorage.shared.util.SearchSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Screen for the Tremendous Chest.
 *
 * <p>Displays the block's stored items in a scrollable grid, a priority control and sort control
 * (in the slide-out config drawer), and the player inventory.
 */
public class ChestScreen extends AbstractContainerScreen<ChestMenu> {

    private final LocalInventoryPane inventoryPane;
    private final Dialog dialog;
    private final ConfigDrawer configDrawer;
    private SearchBoxWidget searchBox;

    @Nullable
    private Slot shiftDragSlot;

    /** Client-side sort mode; updated optimistically on cycle for snappy UI. */
    private SortMode sortMode = SortMode.AMOUNT;

    public ChestScreen(ChestMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        inventoryPane = new LocalInventoryPane();
        if (menu.hasCraftingUpgrade()) {
            dialog = new Dialog(
                    Dialog.blankPane(PlayerInventoryPane.WIDTH, 7),
                    inventoryPane,
                    Dialog.blankPane(PlayerInventoryPane.WIDTH, 20),
                    new CraftingGridPane(),
                    new PlayerInventoryPane());
        } else {
            dialog = new Dialog(
                    Dialog.blankPane(PlayerInventoryPane.WIDTH, 7),
                    inventoryPane,
                    Dialog.blankPane(PlayerInventoryPane.WIDTH, 20),
                    new PlayerInventoryPane());
        }
        this.imageWidth = dialog.totalWidth();
        this.imageHeight = dialog.totalHeight();
        List<IDialogPane> drawerPanes = new java.util.ArrayList<>();
        drawerPanes.add(new PriorityPane(
                menu::getPriority, p -> PacketDistributor.sendToServer(new SetPriorityPacket(menu.getPos(), p))));
        drawerPanes.add(new SortPane(() -> sortMode, () -> {
            sortMode = sortMode.next();
            PacketDistributor.sendToServer(new SetSortModePacket(menu.getPos(), sortMode));
        }));
        if (menu.hasPullerUpgrade()) {
            drawerPanes.add(new PullerSidesPane(menu.getPos()));
        }
        if (Minecraft.getInstance().level != null) {
            BlockEntity picnicBe = Minecraft.getInstance().level.getBlockEntity(menu.getPos());
            if (picnicBe instanceof PicnicBasketBlockEntity || picnicBe instanceof EnderPicnicBasketBlockEntity) {
                drawerPanes.add(new AutoFeedPane(
                        () -> {
                            BlockEntity b = Minecraft.getInstance().level.getBlockEntity(menu.getPos());
                            if (b instanceof PicnicBasketBlockEntity pb) return pb.isAutoFeed();
                            if (b instanceof EnderPicnicBasketBlockEntity eb) return eb.isAutoFeed();
                            return true;
                        },
                        () -> {
                            BlockEntity b = Minecraft.getInstance().level.getBlockEntity(menu.getPos());
                            boolean current = true;
                            if (b instanceof PicnicBasketBlockEntity pb) current = pb.isAutoFeed();
                            else if (b instanceof EnderPicnicBasketBlockEntity eb) current = eb.isAutoFeed();
                            PacketDistributor.sendToServer(new SetAutoFeedPacket(menu.getPos(), !current));
                        }));
            }
        }
        configDrawer = new ConfigDrawer(drawerPanes.toArray(IDialogPane[]::new));
    }

    @Override
    protected void init() {
        super.init();
        dialog.init(leftPos, topPos);
        configDrawer.init(leftPos, topPos, imageHeight);
        inventoryPane.setClickHandler((idx, amount, toCursor) -> PacketDistributor.sendToServer(
                new LocalStorageInteractPacket(menu.getPos(), true, idx, amount, toCursor)));

        // Restore persisted sort mode from the client-side block entity.
        if (Minecraft.getInstance().level != null) {
            BlockEntity be = Minecraft.getInstance().level.getBlockEntity(menu.getPos());
            if (be instanceof ChestBlockEntity bulk) {
                sortMode = bulk.getSortMode();
            }
        }

        // Search box — right-aligned in the title bar.
        searchBox = new SearchBoxWidget(font, leftPos, topPos, imageWidth);
        searchBox.getEditBox().setResponder(text -> {
            inventoryPane.setFilter(text.toLowerCase(Locale.ROOT).strip());
            SearchSync.pushToJei(text);
        });
        addRenderableWidget(searchBox.getEditBox());
        searchBox.initFilter(SearchSync.getRawFilter());

        addRenderableWidget(new PressableIconButton(
                leftPos + dialog.totalWidth() - 26,
                dialog.getPaneAbsY(3) - 20,
                16,
                16,
                ResourceLocation.fromNamespaceAndPath("tremendousstorage", "widget/button_quick_stack"),
                ResourceLocation.fromNamespaceAndPath("tremendousstorage", "widget/button_quick_stack_focused"),
                () -> PacketDistributor.sendToServer(new QuickStackPacket(menu.getPos(), false))));
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
        BlockEntity be = Minecraft.getInstance().level.getBlockEntity(menu.getPos());
        if (!(be instanceof ChestBlockEntity bulk)) {
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

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox.getEditBox().isFocused()) {
            if (keyCode == 256) {
                searchBox.getEditBox().setFocused(false);
                return true;
            }
            searchBox.getEditBox().keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        if (QuickStackClientEvents.QUICK_STACK != null
                && QuickStackClientEvents.QUICK_STACK.matches(keyCode, scanCode)) {
            PacketDistributor.sendToServer(new QuickStackPacket(menu.getPos(), false));
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
        dialog.render(graphics, font, title, mouseX, mouseY, partialTick);
        searchBox.render(graphics, font);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Title is drawn by Dialog.
    }
}
