package net.bobofraggins.tremendousstorage.storage.networkinterface;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.storage.storageupgrade.StorageUpgradeItem;
import net.bobofraggins.tremendousstorage.storage.tube.NetworkConnector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** A single-block-tall Network Interface. */
public class NetworkInterfaceBlock extends BaseEntityBlock implements NetworkConnector {

    public static final Property<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<StorageTier> TIER_PROP = EnumProperty.create("tier", StorageTier.class);

    public static final MapCodec<NetworkInterfaceBlock> CODEC = simpleCodec(NetworkInterfaceBlock::new);

    @Override
    public MapCodec<NetworkInterfaceBlock> codec() {
        return CODEC;
    }

    public NetworkInterfaceBlock(Properties props) {
        super(props);
        registerDefaultState(
                stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TIER_PROP, StorageTier.WOOD));
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, TIER_PROP);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    // -------------------------------------------------------------------------
    // Block entity
    // -------------------------------------------------------------------------

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NetworkInterfaceBlockEntity(pos, state);
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(
                type, Registration.NETWORK_INTERFACE_BE_TYPE.get(), (lvl, pos, st, be) -> be.serverTick());
    }

    // -------------------------------------------------------------------------
    // Drops — persist tier (and energy) to dropped item
    // -------------------------------------------------------------------------

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof NetworkInterfaceBlockEntity ni) {
            for (ItemStack drop : drops) {
                if (drop.getItem() instanceof net.minecraft.world.item.BlockItem) {
                    TagValueOutput _beOut = TagValueOutput.createWithContext(
                            ProblemReporter.DISCARDING, params.getLevel().registryAccess());
                    ni.saveCustomOnly(_beOut);
                    BlockItem.setBlockEntityData(drop, ni.getType(), _beOut);
                }
            }
        }
        return drops;
    }

    // -------------------------------------------------------------------------
    // Neighbour change — invalidate scan cache
    // -------------------------------------------------------------------------

    @Override
    public void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            net.minecraft.world.level.block.Block neighborBlock,
            @javax.annotation.Nullable net.minecraft.world.level.redstone.Orientation orientation,
            boolean movedByPiston) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof NetworkInterfaceBlockEntity ni) {
            ni.setChanged();
        }
    }

    // -------------------------------------------------------------------------
    // Interaction
    // -------------------------------------------------------------------------

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (stack.getItem() instanceof StorageUpgradeItem) return InteractionResult.FAIL;
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (player.isShiftKeyDown()) {
            // Sneak + right-click: deposit all player inventory items into the network
            if (!(level.getBlockEntity(pos) instanceof NetworkInterfaceBlockEntity ni)) {
                return InteractionResult.FAIL;
            }
            IItemHandler network = ni.getItemHandler();
            if (network == null) return InteractionResult.FAIL;
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                net.minecraft.world.item.ItemStack held = player.getInventory().getItem(slot);
                if (held.isEmpty()) continue;
                net.minecraft.world.item.ItemStack remainder = ItemHandlerHelper.insertItem(network, held, false);
                player.getInventory().setItem(slot, remainder);
            }
            player.getInventory().setChanged();
            player.sendOverlayMessage(Component.translatable("action.tremendousstorage.deposit_complete"));
            return InteractionResult.SUCCESS;
        }

        if (level.getBlockEntity(pos) instanceof MenuProvider mp) {
            player.openMenu(mp, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }
}
