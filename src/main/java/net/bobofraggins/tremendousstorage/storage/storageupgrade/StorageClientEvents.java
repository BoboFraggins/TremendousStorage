package net.bobofraggins.tremendousstorage.storage.storageupgrade;

import java.util.List;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlockEntity;
import net.bobofraggins.tremendousstorage.storage.manillafolder.FolderItemDecorator;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
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
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(
                List.of(new BlockTintSource() {
                    @Override
                    public int color(net.minecraft.world.level.block.state.BlockState state) {
                        return -1;
                    }

                    @Override
                    public int colorInWorld(
                            net.minecraft.world.level.block.state.BlockState state,
                            BlockAndTintGetter level,
                            BlockPos pos) {
                        BlockEntity be = level.getBlockEntity(pos);
                        if (be instanceof ChestBlockEntity bulk) {
                            return bulk.getTier().getColor();
                        }
                        return -1;
                    }
                }),
                Registration.TREMENDOUS_CHEST.get(),
                Registration.ENDER_TREMENDOUS_CHEST.get());
    }

    @SubscribeEvent
    public static void onRegisterItemTintSources(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("tremendousstorage", "storage_tier"),
                net.bobofraggins.tremendousstorage.shared.storage.StorageTierTintSource.MAP_CODEC);
    }

    @SubscribeEvent
    public static void onRegisterItemDecorations(RegisterItemDecorationsEvent event) {
        FolderItemDecorator decorator = new FolderItemDecorator();
        event.register(Registration.MANILA_FOLDER.get(), decorator);
        event.register(Registration.ENDER_FOLDER.get(), decorator);
    }
}
