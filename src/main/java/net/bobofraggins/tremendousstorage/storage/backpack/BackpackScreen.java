package net.bobofraggins.tremendousstorage.storage.backpack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.bobofraggins.tremendousstorage.shared.config.SortMode;
import net.bobofraggins.tremendousstorage.shared.network.BackpackInteractPacket;
import net.bobofraggins.tremendousstorage.shared.network.SetBackpackPriorityPacket;
import net.bobofraggins.tremendousstorage.shared.network.SetBackpackSortModePacket;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.ui.ConfigDrawer;
import net.bobofraggins.tremendousstorage.shared.ui.Dialog;
import net.bobofraggins.tremendousstorage.shared.ui.LocalInventoryPane;
import net.bobofraggins.tremendousstorage.shared.ui.PlayerInventoryPane;
import net.bobofraggins.tremendousstorage.shared.ui.PressableIconButton;
import net.bobofraggins.tremendousstorage.shared.ui.PriorityPane;
import net.bobofraggins.tremendousstorage.shared.ui.SortPane;
import net.bobofraggins.tremendousstorage.shared.util.SearchSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

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
    private EditBox searchBox;

    /** Client-side sort mode; updated optimistically on cycle for snappy UI. */
    private SortMode sortMode = SortMode.AMOUNT;

    public BackpackScreen(BackpackMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        inventoryPane = new LocalInventoryPane();
        dialog = new Dialog(
                Dialog.blankPane(PlayerInventoryPane.WIDTH, 7),
                inventoryPane,
                Dialog.blankPane(PlayerInventoryPane.WIDTH, 20),
                new PlayerInventoryPane());
        this.imageWidth = dialog.totalWidth();
        this.imageHeight = dialog.totalHeight();
        configDrawer = new ConfigDrawer(
                new PriorityPane(
                        menu::getPriority,
                        p -> PacketDistributor.sendToServer(new SetBackpackPriorityPacket(
                                menu.getSlotType(), menu.getSlotIndex(), menu.getSlotId(), p))),
                new SortPane(() -> sortMode, () -> {
                    sortMode = sortMode.next();
                    PacketDistributor.sendToServer(new SetBackpackSortModePacket(
                            menu.getSlotType(), menu.getSlotIndex(), menu.getSlotId(), sortMode.ordinal()));
                }));
    }

    @Override
    protected void init() {
        super.init();
        dialog.init(leftPos, topPos);
        configDrawer.init(leftPos, topPos, imageHeight);
        inventoryPane.setClickHandler(
                (idx, amount, toCursor) -> PacketDistributor.sendToServer(new BackpackInteractPacket(
                        menu.getSlotType(), menu.getSlotIndex(), menu.getSlotId(), idx, amount, toCursor)));

        // Read initial sort mode from the backpack's current contents
        BackpackContents contents = getCurrentContents();
        sortMode = contents.sortMode();

        // Search box — right-aligned in the title bar.
        int searchW = 75;
        searchBox =
                new EditBox(font, leftPos + imageWidth - 5 - 2 - searchW, topPos + 3, searchW, 10, Component.empty());
        searchBox.setMaxLength(64);
        searchBox.setHint(Component.literal("Search..."));
        searchBox.setResponder(text -> {
            inventoryPane.setFilter(text.toLowerCase(Locale.ROOT).strip());
            SearchSync.pushToJei(text);
        });
        addRenderableWidget(searchBox);

        addRenderableWidget(new PressableIconButton(
                leftPos + 8,
                topPos + 6,
                16,
                16,
                ResourceLocation.fromNamespaceAndPath("tremendousstorage", "widget/button_config"),
                ResourceLocation.fromNamespaceAndPath("tremendousstorage", "widget/button_config_focused"),
                () -> configDrawer.toggle()));
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox.isFocused()) {
            if (keyCode == 256) {
                searchBox.setFocused(false);
                return true;
            }
            searchBox.keyPressed(keyCode, scanCode, modifiers);
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
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Title is drawn by Dialog.
    }
}
