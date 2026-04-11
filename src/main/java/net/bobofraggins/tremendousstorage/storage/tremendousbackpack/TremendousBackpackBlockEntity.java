package net.bobofraggins.tremendousstorage.storage.tremendousbackpack;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.tremendouschest.TremendousChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for the placed Tremendous Backpack.
 *
 * <p>Extends {@link TremendousChestBlockEntity} so that all storage logic, lid animation,
 * opener counting, sound, and NBT serialization are inherited unchanged. The only differences
 * are the block entity type and display name.
 *
 * <p>Uses {@link net.bobofraggins.tremendousstorage.storage.tremendouschest.TremendousChestMenu} for
 * its UI (the chest screen is identical to what we need).
 */
public class TremendousBackpackBlockEntity extends TremendousChestBlockEntity {

    public TremendousBackpackBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.TREMENDOUS_BACKPACK_BE_TYPE.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        Component base = Component.translatable("block.tremendousstorage.tremendous_backpack");
        return base.copy().append(Component.literal(buildSuffix(false)));
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        ContainerData data = new SimpleContainerData(1);
        return new net.bobofraggins.tremendousstorage.storage.tremendouschest.TremendousChestMenu(
                id, inv, worldPosition, data, hasCraftingUpgrade());
    }

    // -------------------------------------------------------------------------
    // Tickers (delegate to inherited implementation via static method wrappers)
    // -------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, TremendousBackpackBlockEntity be) {
        TremendousChestBlockEntity.serverTick(level, pos, state, be);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TremendousBackpackBlockEntity be) {
        TremendousChestBlockEntity.clientTick(level, pos, state, be);
    }
}
