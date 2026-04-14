package net.bobofraggins.tremendousstorage.glamping.picnicbasket;

import com.mojang.serialization.MapCodec;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Ender Picnic Basket block.
 *
 * <p>Identical in appearance and behaviour to the regular {@link PicnicBasketBlock} except that
 * it creates an {@link EnderPicnicBasketBlockEntity} whose inventory is shared with its linked
 * partner basket via {@link EnderPicnicBasketStorage}.
 */
public class EnderPicnicBasketBlock extends PicnicBasketBlock {

    @SuppressWarnings("unchecked")
    public static final MapCodec<PicnicBasketBlock> CODEC =
            (MapCodec<PicnicBasketBlock>) (MapCodec<?>) simpleCodec(EnderPicnicBasketBlock::new);

    public EnderPicnicBasketBlock(Properties props) {
        super(props);
    }

    @Override
    public MapCodec<PicnicBasketBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnderPicnicBasketBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(
                type,
                Registration.ENDER_PICNIC_BASKET_BE_TYPE.get(),
                level.isClientSide()
                        ? EnderPicnicBasketBlockEntity::clientTick
                        : EnderPicnicBasketBlockEntity::serverTick);
    }
}
