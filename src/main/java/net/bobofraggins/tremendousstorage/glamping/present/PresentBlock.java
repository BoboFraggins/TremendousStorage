package net.bobofraggins.tremendousstorage.glamping.present;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PresentBlock extends BaseEntityBlock {

    public static final MapCodec<PresentBlock> CODEC = simpleCodec(PresentBlock::new);
    public static final Property<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    @Override
    public MapCodec<PresentBlock> codec() {
        return CODEC;
    }

    public PresentBlock(Properties props) {
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
        return new PresentBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state) {
        return Shapes.empty();
    }

    // -------------------------------------------------------------------------
    // Unwrapping — shift-right-click to restore wrapped block in place
    // -------------------------------------------------------------------------

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isCrouching()) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (!(level.getBlockEntity(pos) instanceof PresentBlockEntity present)) return InteractionResult.PASS;
        if (!present.hasWrappedBlock()) return InteractionResult.PASS;

        BlockState wrapped = present.getWrappedState();
        CompoundTag entityData = present.getWrappedEntityData();

        level.setBlock(pos, wrapped, Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);

        if (entityData != null) {
            BlockEntity newBe = level.getBlockEntity(pos);
            if (newBe != null) {
                newBe.loadCustomOnly(
                        TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), entityData));
                newBe.setChanged();
            }
        }

        Block.popResource(level, pos, new ItemStack(Registration.PRESENT_ITEM.get()));
        return InteractionResult.SUCCESS;
    }

    // -------------------------------------------------------------------------
    // Drops — carry wrapped data on the item when broken
    // -------------------------------------------------------------------------

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        ItemStack drop = new ItemStack(Registration.PRESENT_ITEM.get());
        if (be instanceof PresentBlockEntity present && present.hasWrappedBlock()) {
            CompoundTag wrapper = new CompoundTag();
            wrapper.put(PresentBlockItem.WRAPPED_STATE_KEY, NbtUtils.writeBlockState(present.getWrappedState()));
            CompoundTag entityData = present.getWrappedEntityData();
            if (entityData != null) {
                wrapper.put(PresentBlockItem.WRAPPED_ENTITY_KEY, entityData);
            }
            drop.set(DataComponents.CUSTOM_DATA, CustomData.of(wrapper));
        }
        return List.of(drop);
    }

    // -------------------------------------------------------------------------
    // Middle-click — give item with wrapped data
    // -------------------------------------------------------------------------

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean creative) {
        ItemStack stack = super.getCloneItemStack(level, pos, state, creative);
        if (level.getBlockEntity(pos) instanceof PresentBlockEntity present && present.hasWrappedBlock()) {
            CompoundTag wrapper = new CompoundTag();
            wrapper.put(PresentBlockItem.WRAPPED_STATE_KEY, NbtUtils.writeBlockState(present.getWrappedState()));
            CompoundTag entityData = present.getWrappedEntityData();
            if (entityData != null) {
                wrapper.put(PresentBlockItem.WRAPPED_ENTITY_KEY, entityData);
            }
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(wrapper));
        }
        return stack;
    }

    // -------------------------------------------------------------------------
    // Always drop item even when destroyed by explosion
    // -------------------------------------------------------------------------

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return true;
    }
}
