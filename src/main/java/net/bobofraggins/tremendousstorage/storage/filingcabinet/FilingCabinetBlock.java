package net.bobofraggins.tremendousstorage.storage.filingcabinet;

import com.mojang.serialization.MapCodec;
import java.util.List;
import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.tube.NetworkConnector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Filing Cabinet block. Holds up to 8 Manila Folders.
 *
 * <p>Right-click (with or without item in hand) → opens the inventory UI.
 *
 * <p>Breaking the block drops the cabinet item with its folder inventory intact (saved via the
 * loot table's {@code copy_components} function). No folders are spilled into the world.
 */
public class FilingCabinetBlock extends BaseEntityBlock implements NetworkConnector {

    public static final MapCodec<FilingCabinetBlock> CODEC = simpleCodec(FilingCabinetBlock::new);

    @Override
    public MapCodec<FilingCabinetBlock> codec() {
        return CODEC;
    }

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public FilingCabinetBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    // -------------------------------------------------------------------------
    // Blockstate
    // -------------------------------------------------------------------------

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    // -------------------------------------------------------------------------
    // Block entity wiring
    // -------------------------------------------------------------------------

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FilingCabinetBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        super.triggerEvent(state, level, pos, id, param);
        BlockEntity be = level.getBlockEntity(pos);
        return be != null && be.triggerEvent(id, param);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(
                type,
                Registration.FILING_CABINET_BE_TYPE.get(),
                level.isClientSide()
                        ? FilingCabinetBlockEntity::clientTick
                        : FilingCabinetBlockEntity::serverTick);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof FilingCabinetBlockEntity be) {
                be.recheckOpeners(level, pos, state);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof FilingCabinetBlockEntity cabinet) {
            for (ItemStack drop : drops) {
                if (drop.getItem() instanceof BlockItem) {
                    cabinet.saveToItem(drop, params.getLevel().registryAccess());
                }
            }
        }
        return drops;
    }

    // -------------------------------------------------------------------------
    // Neighbour change — propagate cache invalidation through the tube network
    // -------------------------------------------------------------------------

    @Override
    public void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            BlockPos neighborPos,
            boolean movedByPiston) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FilingCabinetBlockEntity be) {
            be.setChanged();
        }
    }

    // -------------------------------------------------------------------------
    // Interaction — open UI on any right-click
    // -------------------------------------------------------------------------

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (level.isClientSide()) return InteractionResult.SUCCESS;

        FilingCabinetBlockEntity be = getBlockEntity(level, pos);
        if (be == null) return InteractionResult.FAIL;

        be.startOpen(player);
        player.openMenu(be, buf -> buf.writeBlockPos(pos));
        return InteractionResult.SUCCESS;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static FilingCabinetBlockEntity getBlockEntity(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof FilingCabinetBlockEntity be ? be : null;
    }
}
