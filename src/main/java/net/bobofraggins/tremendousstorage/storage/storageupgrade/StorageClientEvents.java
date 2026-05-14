package net.bobofraggins.tremendousstorage.storage.storageupgrade;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlockEntity;
import net.bobofraggins.tremendousstorage.storage.manillafolder.FolderItemDecorator;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;

/**
 * Client-only event subscriber that registers block and item color handlers for tiered storage
 * blocks (Tremendous Chest).
 *
 * <p>The block has a tint-index-0 overlay layer in its model. The color returned here
 * determines the accent square color shown on the block's sides and front.
 */
public final class StorageClientEvents {

    private StorageClientEvents() {}

    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register(
                (state, level, pos, tintIndex) -> {
                    if (tintIndex != 0 || level == null || pos == null) return -1;
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof ChestBlockEntity bulk) {
                        return bulk.getTier().getColor();
                    }
                    return -1;
                },
                Registration.TREMENDOUS_CHEST.get(),
                Registration.ENDER_TREMENDOUS_CHEST.get());
    }

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        // BlockItem color for Tremendous Chest / Ender Tremendous Chest (reads tier from NBT component)
        event.register(
                (stack, tintIndex) -> tintIndex == 0 ? tierColorFromStack(stack) : -1,
                Registration.TREMENDOUS_CHEST_ITEM.get(),
                Registration.ENDER_TREMENDOUS_CHEST_ITEM.get());
    }

    @SubscribeEvent
    public static void onRegisterItemDecorations(RegisterItemDecorationsEvent event) {
        FolderItemDecorator decorator = new FolderItemDecorator();
        event.register(Registration.MANILA_FOLDER.get(), decorator);
        event.register(Registration.ENDER_FOLDER.get(), decorator);
    }

    private static int tierColorFromStack(net.minecraft.world.item.ItemStack stack) {
        var customData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("Tier")) {
                return StorageTier.fromId(tag.getString("Tier")).getColor();
            }
        }
        return StorageTier.WOOD.getColor();
    }
}
