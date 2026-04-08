package net.bobofraggins.tremendousstorage.storage.enderbackpack;

import com.mojang.serialization.MapCodec;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.tremendousbackpack.TremendousBackpackBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Ender Tremendous Backpack block.
 *
 * <p>Identical in appearance and behaviour to the regular {@link TremendousBackpackBlock} except
 * that it creates an {@link EnderTremendousBackpackBlockEntity} whose inventory is shared with its
 * linked partner via {@link EnderBackpackStorage}.
 */
public class EnderTremendousBackpackBlock extends TremendousBackpackBlock {

    @SuppressWarnings("unchecked")
    public static final MapCodec<TremendousBackpackBlock> CODEC =
            (MapCodec<TremendousBackpackBlock>) (MapCodec<?>) simpleCodec(EnderTremendousBackpackBlock::new);

    public EnderTremendousBackpackBlock(Properties props) {
        super(props);
    }

    @Override
    public MapCodec<TremendousBackpackBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnderTremendousBackpackBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(
                type,
                Registration.ENDER_TREMENDOUS_BACKPACK_BE_TYPE.get(),
                level.isClientSide()
                        ? EnderTremendousBackpackBlockEntity::clientTick
                        : EnderTremendousBackpackBlockEntity::serverTick);
    }
}
