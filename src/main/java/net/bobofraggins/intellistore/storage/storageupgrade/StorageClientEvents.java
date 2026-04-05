package net.bobofraggins.intellistore.storage.storageupgrade;

import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.bobofraggins.intellistore.shared.storage.StorageTier;
import net.bobofraggins.intellistore.storage.tremendouschest.TremendousChestBlockEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * Client-only event subscriber that registers block and item color handlers for tiered storage
 * blocks (Tremendous Chest).
 *
 * <p>The block has a tint-index-0 overlay layer in its model. The color returned here
 * determines the accent square color shown on the block's sides and front.
 */
@EventBusSubscriber(modid = IntelliStore.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class StorageClientEvents {

    private StorageClientEvents() {}

    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register(
                (state, level, pos, tintIndex) -> {
                    if (tintIndex != 0 || level == null || pos == null) return -1;
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof TremendousChestBlockEntity bulk) {
                        return bulk.getTier().getColor();
                    }
                    return -1;
                },
                Registration.TREMENDOUS_CHEST.get());
    }

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        // BlockItem color for Tremendous Chest (reads tier from NBT component)
        event.register(
                (stack, tintIndex) -> tintIndex == 0 ? tierColorFromStack(stack) : -1,
                Registration.TREMENDOUS_CHEST_ITEM.get());
    }

    private static int tierColorFromStack(net.minecraft.world.item.ItemStack stack) {
        var customData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (customData != null) {
            CompoundTag tag = customData.getUnsafe();
            if (tag.contains("Tier")) {
                return StorageTier.fromId(tag.getString("Tier")).getColor();
            }
        }
        return StorageTier.WOOD.getColor();
    }
}
