package net.bobofraggins.tremendousstorage.storage.endertank;

import com.mojang.serialization.MapCodec;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.tremendoustank.TremendousTankBlock;
import net.bobofraggins.tremendousstorage.storage.tremendoustank.TremendousTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class EnderTremendousTankBlock extends TremendousTankBlock {

    @SuppressWarnings("unchecked")
    public static final MapCodec<TremendousTankBlock> CODEC =
            (MapCodec<TremendousTankBlock>) (MapCodec<?>) simpleCodec(EnderTremendousTankBlock::new);

    @Override
    public MapCodec<TremendousTankBlock> codec() {
        return CODEC;
    }

    public EnderTremendousTankBlock(Properties props) {
        super(props);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnderTremendousTankBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(
                type,
                Registration.ENDER_TREMENDOUS_TANK_BE_TYPE.get(),
                EnderTremendousTankBlockEntity::serverTick);
    }
}
