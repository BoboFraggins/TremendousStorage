package net.bobofraggins.tremendousstorage.storage.enderchest;

import com.mojang.serialization.MapCodec;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.bobofraggins.tremendousstorage.storage.tremendouschest.TremendousChestBlock;

/**
 * The Ender Tremendous Chest block.
 *
 * <p>Identical in appearance and behaviour to the regular {@link TremendousChestBlock} except
 * that it creates an {@link EnderTremendousChestBlockEntity} whose inventory is shared with its
 * linked partner chest via {@link EnderChestStorage}.
 */
public class EnderTremendousChestBlock extends TremendousChestBlock {

    @SuppressWarnings("unchecked")
    public static final MapCodec<TremendousChestBlock> CODEC =
            (MapCodec<TremendousChestBlock>) (MapCodec<?>) simpleCodec(EnderTremendousChestBlock::new);

    public EnderTremendousChestBlock(Properties props) {
        super(props);
    }

    @Override
    public MapCodec<TremendousChestBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnderTremendousChestBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(
                type,
                Registration.ENDER_TREMENDOUS_CHEST_BE_TYPE.get(),
                level.isClientSide()
                        ? EnderTremendousChestBlockEntity::clientTick
                        : EnderTremendousChestBlockEntity::serverTick);
    }
}
