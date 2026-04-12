package net.bobofraggins.tremendousstorage.storage.endertank;

import com.mojang.serialization.MapCodec;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.tank.TankBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class EnderTankBlock extends TankBlock {

    @SuppressWarnings("unchecked")
    public static final MapCodec<TankBlock> CODEC =
            (MapCodec<TankBlock>) (MapCodec<?>) simpleCodec(EnderTankBlock::new);

    @Override
    public MapCodec<TankBlock> codec() {
        return CODEC;
    }

    public EnderTankBlock(Properties props) {
        super(props);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnderTankBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, Registration.ENDER_TANK_BE_TYPE.get(), EnderTankBlockEntity::serverTick);
    }
}
