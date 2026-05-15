package net.bobofraggins.tremendousstorage.power.stirlingengine;

import com.mojang.serialization.MapCodec;
import java.util.List;
import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.storage.tube.NetworkConnector;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

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
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof StirlingEngineBlockEntity be) {
            be.setChanged();
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof StirlingEngineBlockEntity engine) {
            for (ItemStack drop : drops) {
                if (drop.getItem() instanceof net.minecraft.world.item.BlockItem) {
                    TagValueOutput _beOut = TagValueOutput.createWithContext(
                            ProblemReporter.DISCARDING, params.getLevel().registryAccess());
                    engine.saveCustomOnly(_beOut);
                    BlockItem.setBlockEntityData(drop, engine.getType(), _beOut);
                }
            }
        }
        return drops;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StirlingEngineBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return createTickerHelper(
                    type,
                    net.bobofraggins.tremendousstorage.shared.register.Registration.STIRLING_ENGINE_BE_TYPE.get(),
                    (lvl, pos, st, be) -> be.clientTick());
        }
        return createTickerHelper(
                type,
                net.bobofraggins.tremendousstorage.shared.register.Registration.STIRLING_ENGINE_BE_TYPE.get(),
                (lvl, pos, st, be) -> be.serverTick());
    }
}
