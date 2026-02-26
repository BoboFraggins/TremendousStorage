package net.bobofraggins.intellistore.filingcabinet;

import com.mojang.serialization.MapCodec;
import net.bobofraggins.intellistore.manillafolder.ManillaFolderItem;
import net.bobofraggins.intellistore.register.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Filing Cabinet block. Holds up to 8 Manila Folders.
 *
 * <p>Interaction model:
 * <ul>
 *   <li>Right-click with a Manila Folder in hand → insert into the first empty slot.
 *   <li>Right-click with empty hand → toggle open/closed.
 *   <li>Sneak + right-click with empty hand (while open) → extract the topmost folder.
 * </ul>
 *
 * <p>Breaking the block drops the cabinet item with its folder inventory intact (saved via the
 * loot table's {@code copy_components} function). No folders are spilled into the world.
 */
public class FilingCabinetBlock extends BaseEntityBlock {

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
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return createTickerHelper(
                    type, Registration.FILING_CABINET_BE_TYPE.get(), FilingCabinetBlockEntity::clientTick);
        }
        return createTickerHelper(
                type, Registration.FILING_CABINET_BE_TYPE.get(), FilingCabinetBlockEntity::serverTick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // BaseEntityBlock defaults to INVISIBLE; override so the JSON model renders.
        return RenderShape.MODEL;
    }

    // -------------------------------------------------------------------------
    // Interaction — item in hand
    // -------------------------------------------------------------------------

    /**
     * Right-click with a Manila Folder: insert into the first empty slot.
     * All other items fall through to the default block interaction.
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

        if (!(stack.getItem() instanceof ManillaFolderItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;

        FilingCabinetBlockEntity be = getBlockEntity(level, pos);
        if (be == null) return ItemInteractionResult.FAIL;

        int slot = be.firstEmptySlot();
        if (slot < 0) {
            player.displayClientMessage(Component.translatable("block.intellistore.filing_cabinet.full"), true);
            return ItemInteractionResult.FAIL;
        }

        be.setFolder(slot, stack.copyWithCount(1));
        if (!player.isCreative()) stack.shrink(1);
        return ItemInteractionResult.SUCCESS;
    }

    // -------------------------------------------------------------------------
    // Interaction — empty hand
    // -------------------------------------------------------------------------

    /**
     * Right-click with empty hand:
     * <ul>
     *   <li>If sneaking and the cabinet is open → extract the topmost folder.
     *   <li>Otherwise → toggle open/closed.
     * </ul>
     */
    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (level.isClientSide()) return InteractionResult.SUCCESS;

        FilingCabinetBlockEntity be = getBlockEntity(level, pos);
        if (be == null) return InteractionResult.FAIL;

        if (player.isShiftKeyDown() && be.isOpen()) {
            int slot = be.lastOccupiedSlot();
            if (slot < 0) return InteractionResult.CONSUME; // open but empty

            ItemStack folder = be.getFolder(slot);
            be.setFolder(slot, ItemStack.EMPTY);
            if (!player.getInventory().add(folder)) {
                player.drop(folder, false);
            }
            return InteractionResult.SUCCESS;
        }

        be.setOpen(!be.isOpen());
        return InteractionResult.SUCCESS;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static FilingCabinetBlockEntity getBlockEntity(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof FilingCabinetBlockEntity be ? be : null;
    }
}
