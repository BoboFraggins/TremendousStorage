package net.bobofraggins.intellistore.storage.junkdrawer;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.bobofraggins.intellistore.storage.tube.NetworkConnector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Junk Drawer block — stores up to 32,768 individual items, one per slot.
 *
 * <p>Accepts only items that Manila Folders reject: damageable items (tools, armour, weapons)
 * and items with non-default component data (enchanted books, named items, potions, etc.).
 * There is no locking — any qualifying item may be freely added or removed at any time.
 *
 * <p>There is no player-facing UI. All item movement is via hoppers, pipes, or any mod that
 * reads the {@link net.neoforged.neoforge.items.IItemHandler} capability.
 */
public class JunkDrawerBlock extends BaseEntityBlock implements NetworkConnector {

    public static final MapCodec<JunkDrawerBlock> CODEC = simpleCodec(JunkDrawerBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    @Override
    public MapCodec<JunkDrawerBlock> codec() {
        return CODEC;
    }

    public JunkDrawerBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new JunkDrawerBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // Use the standard JSON cube model.
        return RenderShape.MODEL;
    }

    @Override
    public void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            BlockPos neighborPos,
            boolean movedByPiston) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof JunkDrawerBlockEntity be) {
            be.setChanged();
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof JunkDrawerBlockEntity junk) {
            for (ItemStack drop : drops) {
                if (drop.getItem() instanceof BlockItem) {
                    junk.saveToItem(drop, params.getLevel().registryAccess());
                }
            }
        }
        return drops;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof MenuProvider mp) {
            player.openMenu(mp, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }
}
