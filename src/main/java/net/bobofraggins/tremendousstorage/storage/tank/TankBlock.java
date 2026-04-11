package net.bobofraggins.tremendousstorage.storage.tank;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.tube.NetworkConnector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * The Tank block — stores up to {@link TankBlockEntity#CAPACITY} mB of a single fluid type.
 *
 * <p>The front face (the direction the player faces when placing) shows a 12×12 transparent
 * window. When the tank contains a fluid, the fluid's texture is rendered there by
 * {@link TankRenderer}.
 *
 * <p>Right-clicking with a fluid container item (e.g. a bucket) fills or drains the tank.
 * All other fluid movement is via the {@link TankFluidHandler} IFluidHandler capability.
 */
public class TankBlock extends BaseEntityBlock implements NetworkConnector {

    public static final MapCodec<TankBlock> CODEC = simpleCodec(TankBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public TankBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    public MapCodec<TankBlock> codec() {
        return CODEC;
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
        return new TankBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // MODEL renders the block model (base slab + glass jar); BESR handles fluid fill + stubs.
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof TankBlockEntity tank) {
            for (ItemStack drop : drops) {
                if (drop.getItem() instanceof BlockItem) {
                    tank.saveToItem(drop, params.getLevel().registryAccess());
                }
            }
        }
        return drops;
    }

    private static final int BOTTLE_MB = 250;

    /**
     * Right-click with a fluid container item (bucket or glass bottle) to fill/drain the tank.
     *
     * <p>Buckets and modded fluid containers are handled by
     * {@link FluidUtil#interactWithFluidHandler} (1000 mB per interaction).
     *
     * <p>Glass bottles and water bottles are handled manually (250 mB per interaction):
     * <ul>
     *   <li>Empty glass bottle + tank has ≥ 250 mB water → give water bottle, drain tank.
     *   <li>Water bottle + tank accepts water and has ≥ 250 mB free → give empty bottle, fill tank.
     * </ul>
     */
    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;

        // --- Bottle handling (250 mB per bottle) ---
        if (stack.is(Items.GLASS_BOTTLE)) {
            if (!(level.getBlockEntity(pos) instanceof TankBlockEntity be))
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            FluidStack simulated = be.extract(BOTTLE_MB, true);
            if (simulated.getAmount() >= BOTTLE_MB) {
                ItemStack result;
                if (simulated.getFluid() == Fluids.WATER) {
                    result = PotionContents.createItemStack(Items.POTION, Potions.WATER);
                } else if (simulated.getFluid() == Registration.HEALING_SALVE_SOURCE.get()) {
                    result = new ItemStack(Registration.POSITIVE_VIBES_BOTTLE.get());
                } else {
                    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                }
                be.extract(BOTTLE_MB, false);
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, result));
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (stack.is(Items.POTION)) {
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents != null && contents.is(Potions.WATER)) {
                if (!(level.getBlockEntity(pos) instanceof TankBlockEntity be))
                    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                FluidStack water = new FluidStack(Fluids.WATER, BOTTLE_MB);
                long inserted = be.insert(water, BOTTLE_MB, true);
                if (inserted >= BOTTLE_MB) {
                    be.insert(water, BOTTLE_MB, false);
                    player.setItemInHand(
                            hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
                    level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
                    return ItemInteractionResult.SUCCESS;
                }
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // --- Bucket / modded fluid container handling (via capability) ---
        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, state, null, null);
        if (handler == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        // Determine sound direction before the interaction mutates the item stack.
        // Empty bucket / empty tank item → we are filling it (BUCKET_FILL sound).
        // Filled bucket / non-empty tank item → we are emptying it (BUCKET_EMPTY sound).
        boolean filling;
        if (stack.is(Items.BUCKET)) {
            filling = true;
        } else if (stack.getItem() instanceof TankItem) {
            TankContents tankContents =
                    stack.getOrDefault(Registration.TANK_CONTENTS.get(), TankContents.EMPTY);
            filling = tankContents.amount() == 0;
        } else {
            filling = false;
        }
        boolean success = FluidUtil.interactWithFluidHandler(player, hand, handler);
        if (success && level.getBlockEntity(pos) instanceof TankBlockEntity be) {
            FluidStack stored = be.getStoredFluid();
            if (!stored.isEmpty()) {
                SoundEvent sound =
                        stored.getFluidType().getSound(filling ? SoundActions.BUCKET_FILL : SoundActions.BUCKET_EMPTY);
                if (sound == null) sound = filling ? SoundEvents.BUCKET_FILL : SoundEvents.BUCKET_EMPTY;
                level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
        }
        return success ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** Right-click with empty hand → open tank settings screen. */
    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof TankBlockEntity be)) return InteractionResult.FAIL;
        player.openMenu(be, buf -> buf.writeBlockPos(pos));
        return InteractionResult.SUCCESS;
    }
}
