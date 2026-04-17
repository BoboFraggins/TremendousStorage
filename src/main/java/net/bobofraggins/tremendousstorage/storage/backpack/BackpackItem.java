package net.bobofraggins.tremendousstorage.storage.backpack;

import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.shared.network.OpenBackpackPacket;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.shared.storage.TieredBlockItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * The Tremendous Backpack — a wearable, upgradeable bulk-storage item.
 *
 * <p>Right-click opens a bulk-storage UI identical to the Bulk Storage Container. The 'B' keybind
 * (handled by {@link BackpackClientTickHandler}) opens the UI from inventory, hotbar, or
 * a Curios slot. Integrates with the Curios API via the {@code back} curio tag.
 *
 * <p>All stored items and settings are persisted as a {@link BackpackContents} data
 * component on the ItemStack.
 */
public class BackpackItem extends BlockItem {

    public BackpackItem(Block block) {
        super(block, new Item.Properties().stacksTo(1));
    }

    @Override
    public Component getName(ItemStack stack) {
        Component base = super.getName(stack);
        BackpackContents contents =
                stack.getOrDefault(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), BackpackContents.EMPTY);
        StringBuilder sb = new StringBuilder();
        StorageTier tier = contents.tier();
        if (tier != StorageTier.WOOD) sb.append(TieredBlockItem.capitalize(tier.getId()));
        if (contents.hasCraftingUpgrade()) {
            if (!sb.isEmpty()) sb.append('/');
            sb.append("Crafting");
        }
        // Magnet and Puller are stored in BLOCK_ENTITY_DATA (not BackpackContents)
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data != null) {
            var tag = data.copyTag();
            if (tag.getBoolean("MagnetUpgrade")) {
                if (!sb.isEmpty()) sb.append('/');
                sb.append("Magnet");
            }
            if (tag.getBoolean("PullerUpgrade")) {
                if (!sb.isEmpty()) sb.append('/');
                sb.append("Puller");
            }
        }
        return sb.isEmpty() ? base : Component.empty().append(base).append(" (" + sb + ")");
    }

    // -------------------------------------------------------------------------
    // Right-click to open UI; sneak + right-click on block face to place
    // -------------------------------------------------------------------------

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            // Sneak + right-click on a block face → place the backpack block.
            return super.useOn(ctx);
        }
        // Right-click (no sneak) → open the backpack UI.
        if (!ctx.getLevel().isClientSide() && player instanceof ServerPlayer sp) {
            int slotIndex = ctx.getHand() == InteractionHand.MAIN_HAND ? sp.getInventory().selected : 40;
            openBackpackUi(sp, OpenBackpackPacket.SLOT_INVENTORY, slotIndex, "");
        }
        return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.success(stack);

        int slotIndex = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 40;
        openBackpackUi((ServerPlayer) player, OpenBackpackPacket.SLOT_INVENTORY, slotIndex, "");
        return InteractionResultHolder.success(stack);
    }

    // -------------------------------------------------------------------------
    // Open UI (called from right-click and from packet handler)
    // -------------------------------------------------------------------------

    /**
     * Opens the Tremendous Backpack UI for the given player using the indicated slot location.
     * Validates that the slot still holds a BackpackItem before opening.
     *
     * <p>Delegates to {@link #openUi} so subclasses (e.g. Ender Backpack) can inject
     * pre-open sync logic and swap in a different menu type.
     */
    public static void openBackpackUi(ServerPlayer player, int slotType, int slotIndex, String slotId) {
        ItemStack backpackStack = getBackpackStack(player, slotType, slotIndex, slotId);
        if (backpackStack.isEmpty()) return;
        if (backpackStack.getItem() instanceof BackpackItem item) {
            item.openUi(player, backpackStack, slotType, slotIndex, slotId);
        }
    }

    /**
     * Opens the backpack UI. Override in subclasses to sync storage or use a different menu type.
     */
    protected void openUi(ServerPlayer player, ItemStack backpackStack, int slotType, int slotIndex, String slotId) {
        BackpackContents contents =
                backpackStack.getOrDefault(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), BackpackContents.EMPTY);
        int initialPriority = contents.priority().ordinal();
        int[] priorityHolder = {initialPriority};
        ContainerData data = new ContainerData() {
            @Override
            public int get(int i) {
                return i == 0 ? priorityHolder[0] : 0;
            }

            @Override
            public void set(int i, int v) {
                if (i == 0) priorityHolder[0] = v;
            }

            @Override
            public int getCount() {
                return 1;
            }
        };

        boolean craftingUpgrade = contents.hasCraftingUpgrade();
        player.openMenu(new Provider(slotType, slotIndex, slotId, data, craftingUpgrade), buf -> {
            buf.writeInt(slotType);
            buf.writeInt(slotIndex);
            buf.writeUtf(slotId);
            buf.writeBoolean(craftingUpgrade);
        });
    }

    // -------------------------------------------------------------------------
    // Slot utilities (used by menus and packet handlers)
    // -------------------------------------------------------------------------

    /**
     * Retrieves the Tremendous Backpack ItemStack from the given slot.
     * Returns {@link ItemStack#EMPTY} if the slot doesn't hold a BackpackItem.
     */
    public static ItemStack getBackpackStack(Player player, int slotType, int slotIndex, String slotId) {
        if (slotType == OpenBackpackPacket.SLOT_INVENTORY) {
            ItemStack stack = player.getInventory().getItem(slotIndex);
            return stack.getItem() instanceof BackpackItem ? stack : ItemStack.EMPTY;
        } else {
            return getFromCurios(player, slotIndex, slotId);
        }
    }

    /**
     * Writes an updated Tremendous Backpack ItemStack back to the given slot.
     */
    public static void setBackpackStack(Player player, ItemStack stack, int slotType, int slotIndex, String slotId) {
        if (slotType == OpenBackpackPacket.SLOT_INVENTORY) {
            player.getInventory().setItem(slotIndex, stack);
        } else {
            setInCurios(player, stack, slotIndex, slotId);
        }
    }

    @Nullable
    private static ItemStack getFromCurios(Player player, int slotIndex, String slotId) {
        try {
            var inv = player.getCapability(top.theillusivec4.curios.api.CuriosCapability.INVENTORY);
            if (inv == null) return ItemStack.EMPTY;
            var entry = inv.getCurios().get(slotId);
            if (entry == null) return ItemStack.EMPTY;
            ItemStack stack = entry.getStacks().getStackInSlot(slotIndex);
            return stack.getItem() instanceof BackpackItem ? stack : ItemStack.EMPTY;
        } catch (LinkageError ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static void setInCurios(Player player, ItemStack stack, int slotIndex, String slotId) {
        try {
            var inv = player.getCapability(top.theillusivec4.curios.api.CuriosCapability.INVENTORY);
            if (inv == null) return;
            var entry = inv.getCurios().get(slotId);
            if (entry == null) return;
            entry.getStacks().setStackInSlot(slotIndex, stack);
        } catch (LinkageError ignored) {
        }
    }

    // -------------------------------------------------------------------------
    // MenuProvider
    // -------------------------------------------------------------------------

    public static class Provider implements MenuProvider {

        private final int slotType;
        private final int slotIndex;
        private final String slotId;
        private final ContainerData data;
        private final boolean hasCraftingUpgrade;

        public Provider(int slotType, int slotIndex, String slotId, ContainerData data, boolean hasCraftingUpgrade) {
            this.slotType = slotType;
            this.slotIndex = slotIndex;
            this.slotId = slotId;
            this.data = data;
            this.hasCraftingUpgrade = hasCraftingUpgrade;
        }

        protected int getSlotType() {
            return slotType;
        }

        protected int getSlotIndex() {
            return slotIndex;
        }

        protected String getSlotId() {
            return slotId;
        }

        protected ContainerData getData() {
            return data;
        }

        protected boolean hasCraftingUpgrade() {
            return hasCraftingUpgrade;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("item.tremendousstorage.backpack");
        }

        @Override
        public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
            return new BackpackMenu(syncId, inv, slotType, slotIndex, slotId, data, hasCraftingUpgrade);
        }
    }
}
