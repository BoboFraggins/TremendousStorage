package net.bobofraggins.intellistore.storage.tremendousbackpack;

import javax.annotation.Nullable;
import net.bobofraggins.intellistore.shared.network.OpenTremendousBackpackPacket;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The Tremendous Backpack — a wearable, upgradeable bulk-storage item.
 *
 * <p>Right-click opens a bulk-storage UI identical to the Bulk Storage Container. The 'B' keybind
 * (handled by {@link TremendousBackpackClientTickHandler}) opens the UI from inventory, hotbar, or
 * a Curios slot. Integrates with the Curios API via the {@code back} curio tag.
 *
 * <p>All stored items and settings are persisted as a {@link TremendousBackpackContents} data
 * component on the ItemStack.
 */
public class TremendousBackpackItem extends Item {

    public TremendousBackpackItem() {
        super(new Item.Properties().stacksTo(1));
    }

    // -------------------------------------------------------------------------
    // Right-click to open UI
    // -------------------------------------------------------------------------

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.success(stack);

        int slotIndex = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 40;
        openBackpackUi((ServerPlayer) player, OpenTremendousBackpackPacket.SLOT_INVENTORY, slotIndex, "");
        return InteractionResultHolder.success(stack);
    }

    // -------------------------------------------------------------------------
    // Open UI (called from right-click and from packet handler)
    // -------------------------------------------------------------------------

    /**
     * Opens the Tremendous Backpack UI for the given player using the indicated slot location.
     * Validates that the slot still holds a TremendousBackpackItem before opening.
     */
    public static void openBackpackUi(ServerPlayer player, int slotType, int slotIndex, String slotId) {
        ItemStack backpackStack = getBackpackStack(player, slotType, slotIndex, slotId);
        if (backpackStack.isEmpty()) return;

        TremendousBackpackContents contents = backpackStack.getOrDefault(
                Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), TremendousBackpackContents.EMPTY);
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

        player.openMenu(new Provider(slotType, slotIndex, slotId, data), buf -> {
            buf.writeInt(slotType);
            buf.writeInt(slotIndex);
            buf.writeUtf(slotId);
        });
    }

    // -------------------------------------------------------------------------
    // Slot utilities (used by menus and packet handlers)
    // -------------------------------------------------------------------------

    /**
     * Retrieves the Tremendous Backpack ItemStack from the given slot.
     * Returns {@link ItemStack#EMPTY} if the slot doesn't hold a TremendousBackpackItem.
     */
    public static ItemStack getBackpackStack(Player player, int slotType, int slotIndex, String slotId) {
        if (slotType == OpenTremendousBackpackPacket.SLOT_INVENTORY) {
            ItemStack stack = player.getInventory().getItem(slotIndex);
            return stack.getItem() instanceof TremendousBackpackItem ? stack : ItemStack.EMPTY;
        } else {
            return getFromCurios(player, slotIndex, slotId);
        }
    }

    /**
     * Writes an updated Tremendous Backpack ItemStack back to the given slot.
     */
    public static void setBackpackStack(Player player, ItemStack stack, int slotType, int slotIndex, String slotId) {
        if (slotType == OpenTremendousBackpackPacket.SLOT_INVENTORY) {
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
            return stack.getItem() instanceof TremendousBackpackItem ? stack : ItemStack.EMPTY;
        } catch (NoClassDefFoundError | Exception ignored) {
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
        } catch (NoClassDefFoundError | Exception ignored) {
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

        public Provider(int slotType, int slotIndex, String slotId, ContainerData data) {
            this.slotType = slotType;
            this.slotIndex = slotIndex;
            this.slotId = slotId;
            this.data = data;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("item.intellistore.tremendous_backpack");
        }

        @Override
        public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
            return new TremendousBackpackMenu(syncId, inv, slotType, slotIndex, slotId, data);
        }
    }
}
