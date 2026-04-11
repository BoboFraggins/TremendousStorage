package net.bobofraggins.tremendousstorage.storage.enderbackpack;

import com.mojang.serialization.MapCodec;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Ender Tremendous Backpack block.
 *
 * <p>Identical in appearance and behaviour to the regular {@link BackpackBlock} except
 * that it creates an {@link EnderBackpackBlockEntity} whose inventory is shared with its
 * linked partner via {@link EnderBackpackStorage}.
 */
public class EnderBackpackBlock extends BackpackBlock {

    @SuppressWarnings("unchecked")
    public static final MapCodec<BackpackBlock> CODEC =
            (MapCodec<BackpackBlock>) (MapCodec<?>) simpleCodec(EnderBackpackBlock::new);

    public EnderBackpackBlock(Properties props) {
        super(props);
    }

    @Override
    public MapCodec<BackpackBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnderBackpackBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(
                type,
                Registration.ENDER_TREMENDOUS_BACKPACK_BE_TYPE.get(),
                level.isClientSide()
                        ? EnderBackpackBlockEntity::clientTick
                        : EnderBackpackBlockEntity::serverTick);
    }
}
