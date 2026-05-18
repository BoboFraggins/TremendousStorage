package net.bobofraggins.tremendousstorage.glamping.dankfannypack;

import net.bobofraggins.tremendousstorage.shared.network.OpenFannyPackPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** The Dank Fanny Pack — a wearable belt curio that opens a Filing Cabinet UI when used. */
public class DankFannyPackItem extends Item {

    public DankFannyPackItem(Item.Properties properties) {
        super(properties);
    }

    // -------------------------------------------------------------------------
    // Right-click to open UI
    // -------------------------------------------------------------------------

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            int slotIndex =
                    hand == InteractionHand.MAIN_HAND ? sp.getInventory().getSelectedSlot() : 40;
            openFannyPackUi(sp, OpenFannyPackPacket.SLOT_INVENTORY, slotIndex, "");
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    // -------------------------------------------------------------------------
    // Open UI
    // -------------------------------------------------------------------------

    public static void openFannyPackUi(ServerPlayer player, int slotType, int slotIndex, String slotId) {
        if (getFannyPackStack(player, slotType, slotIndex, slotId).isEmpty()) return;
        player.openMenu(
                new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.translatable("item.tremendousstorage.dank_fanny_pack");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player p) {
                        return new DankFannyPackMenu(syncId, inv, slotType, slotIndex, slotId);
                    }
                },
                buf -> {
                    buf.writeInt(slotType);
                    buf.writeInt(slotIndex);
                    buf.writeUtf(slotId);
                });
    }

    // -------------------------------------------------------------------------
    // Slot utilities
    // -------------------------------------------------------------------------

    public static ItemStack getFannyPackStack(Player player, int slotType, int slotIndex, String slotId) {
        if (slotType == OpenFannyPackPacket.SLOT_INVENTORY) {
            ItemStack stack = player.getInventory().getItem(slotIndex);
            return stack.getItem() instanceof DankFannyPackItem ? stack : ItemStack.EMPTY;
        }
        return getFromCurios(player, slotIndex, slotId);
    }

    public static void setFannyPackStack(Player player, ItemStack stack, int slotType, int slotIndex, String slotId) {
        if (slotType == OpenFannyPackPacket.SLOT_INVENTORY) {
            player.getInventory().setItem(slotIndex, stack);
        } else {
            setInCurios(player, stack, slotIndex, slotId);
        }
    }

    private static ItemStack getFromCurios(Player player, int slotIndex, String slotId) {
        try {
            var inv = player.getCapability(top.theillusivec4.curios.api.CuriosCapability.INVENTORY);
            if (inv == null) return ItemStack.EMPTY;
            var entry = inv.getCurios().get(slotId);
            if (entry == null) return ItemStack.EMPTY;
            ItemStack stack = entry.getStacks().getStackInSlot(slotIndex);
            return stack.getItem() instanceof DankFannyPackItem ? stack : ItemStack.EMPTY;
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
}
