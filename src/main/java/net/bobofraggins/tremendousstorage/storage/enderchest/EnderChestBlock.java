package net.bobofraggins.tremendousstorage.storage.enderchest;

import com.mojang.serialization.MapCodec;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Ender Tremendous Chest block.
 *
 * <p>Identical in appearance and behaviour to the regular {@link ChestBlock} except
 * that it creates an {@link EnderChestBlockEntity} whose inventory is shared with its
 * linked partner chest via {@link EnderChestStorage}.
 */
public class EnderChestBlock extends ChestBlock {

    @SuppressWarnings("unchecked")
    public static final MapCodec<ChestBlock> CODEC =
            (MapCodec<ChestBlock>) (MapCodec<?>) simpleCodec(EnderChestBlock::new);

    public EnderChestBlock(Properties props) {
        super(props);
    }

    @Override
    public MapCodec<ChestBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnderChestBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(
                type,
                Registration.ENDER_TREMENDOUS_CHEST_BE_TYPE.get(),
                level.isClientSide() ? EnderChestBlockEntity::clientTick : EnderChestBlockEntity::serverTick);
    }
}
