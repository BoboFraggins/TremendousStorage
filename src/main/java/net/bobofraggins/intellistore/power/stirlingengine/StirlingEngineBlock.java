package net.bobofraggins.intellistore.power.stirlingengine;

import com.mojang.serialization.MapCodec;
import net.bobofraggins.intellistore.storage.tube.NetworkConnector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** A heat-powered energy generator that converts heat from blocks below into RF. */
public class StirlingEngineBlock extends BaseEntityBlock implements NetworkConnector {

    public static final MapCodec<StirlingEngineBlock> CODEC = simpleCodec(StirlingEngineBlock::new);

    @Override
    public MapCodec<StirlingEngineBlock> codec() {
        return CODEC;
    }

    public StirlingEngineBlock(Properties props) {
        super(props);
    }

    @Override
    public void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            BlockPos neighborPos,
            boolean movedByPiston) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof StirlingEngineBlockEntity be) {
            be.setChanged();
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StirlingEngineBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return createTickerHelper(
                    type,
                    net.bobofraggins.intellistore.shared.register.Registration.STIRLING_ENGINE_BE_TYPE.get(),
                    (lvl, pos, st, be) -> be.clientTick());
        }
        return createTickerHelper(
                type,
                net.bobofraggins.intellistore.shared.register.Registration.STIRLING_ENGINE_BE_TYPE.get(),
                (lvl, pos, st, be) -> be.serverTick());
    }
}
