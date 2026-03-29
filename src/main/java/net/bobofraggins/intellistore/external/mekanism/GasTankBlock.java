package net.bobofraggins.intellistore.external.mekanism;

import com.mojang.serialization.MapCodec;
import java.util.List;
import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import net.bobofraggins.intellistore.storage.tube.NetworkConnector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
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
import net.neoforged.neoforge.capabilities.ItemCapability;

/**
 * The Gas Tank block — stores up to {@link GasTankBlockEntity#CAPACITY} mB of a single
 * Mekanism chemical type.
 *
 * <p>Right-clicking with a Mekanism portable gas tank item (or any item exposing
 * {@code IChemicalHandler} via item capability) fills or drains the Gas Tank.
 * All other chemical movement is via the {@link GasTankChemicalHandler} capability,
 * consumed directly by Mekanism pressure tubes and Pipez gas pipes.
 */
public class GasTankBlock extends BaseEntityBlock implements NetworkConnector {

    public static final MapCodec<GasTankBlock> CODEC = simpleCodec(GasTankBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    /**
     * Item capability for IChemicalHandler — looked up by the same resource location that
     * Mekanism uses when building its MultiTypeCapability. This is safe because
     * ItemCapability.createVoid() is idempotent for the same name + type.
     */
    @SuppressWarnings("unchecked")
    private static final ItemCapability<IChemicalHandler, Void> CHEMICAL_ITEM_CAP =
            (ItemCapability<IChemicalHandler, Void>) ItemCapability.createVoid(
                    ResourceLocation.fromNamespaceAndPath("mekanism", "chemical_handler"), IChemicalHandler.class);

    public GasTankBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public MapCodec<GasTankBlock> codec() {
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
        return new GasTankBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // MODEL renders the block model (base slab + glass jar); BESR handles chemical fill + stubs.
        return RenderShape.MODEL;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof GasTankBlockEntity tank) {
            for (ItemStack drop : drops) {
                if (drop.getItem() instanceof BlockItem) {
                    tank.saveToItem(drop, params.getLevel().registryAccess());
                }
            }
        }
        return drops;
    }

    /**
     * Right-click with a Mekanism portable gas tank (or any IChemicalHandler item) to
     * fill or drain the Gas Tank block. Tries to fill first; if the block tank is full or
     * wrong type, tries to drain from the block into the item.
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
        if (!(level.getBlockEntity(pos) instanceof GasTankBlockEntity be))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        IChemicalHandler itemHandler = stack.getCapability(CHEMICAL_ITEM_CAP);
        if (itemHandler == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        // Try to fill block tank from item
        for (int i = 0; i < itemHandler.getChemicalTanks(); i++) {
            ChemicalStack inItem = itemHandler.getChemicalInTank(i);
            if (inItem.isEmpty()) continue;

            long simInserted = be.insert(inItem, inItem.getAmount(), true);
            if (simInserted > 0) {
                ChemicalStack remainder = itemHandler.extractChemical(i, simInserted, Action.SIMULATE);
                long toTransfer = simInserted - (remainder.isEmpty() ? 0 : simInserted - remainder.getAmount());
                // remainder from extractChemical is what could NOT be extracted — we want what can
                ChemicalStack extracted = itemHandler.extractChemical(i, simInserted, Action.SIMULATE);
                long canExtract = extracted.isEmpty() ? 0 : extracted.getAmount();
                if (canExtract <= 0) continue;

                long actualInsert = be.insert(inItem.copyWithAmount(canExtract), canExtract, true);
                if (actualInsert <= 0) continue;

                // Execute
                ChemicalStack actualExtracted = itemHandler.extractChemical(i, actualInsert, Action.EXECUTE);
                be.insert(actualExtracted, actualExtracted.getAmount(), false);
                return ItemInteractionResult.SUCCESS;
            }
        }

        // Try to drain block tank into item
        ChemicalStack inBlock = be.getStoredChemical();
        if (!inBlock.isEmpty() && be.getAmount() > 0) {
            ChemicalStack toOffer = inBlock.copyWithAmount(be.getAmount());
            for (int i = 0; i < itemHandler.getChemicalTanks(); i++) {
                ChemicalStack remainder = itemHandler.insertChemical(i, toOffer, Action.SIMULATE);
                long canInsert = toOffer.getAmount() - (remainder.isEmpty() ? 0 : remainder.getAmount());
                if (canInsert <= 0) continue;

                ChemicalStack extracted = be.extract(canInsert, true);
                if (extracted.isEmpty()) continue;

                ChemicalStack rem2 = itemHandler.insertChemical(i, extracted, Action.EXECUTE);
                long actualTransferred = extracted.getAmount() - (rem2.isEmpty() ? 0 : rem2.getAmount());
                if (actualTransferred > 0) {
                    be.extract(actualTransferred, false);
                    return ItemInteractionResult.SUCCESS;
                }
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** Right-click with empty hand → open tank settings screen. */
    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof GasTankBlockEntity be)) return InteractionResult.FAIL;
        player.openMenu(be, buf -> buf.writeBlockPos(pos));
        return InteractionResult.SUCCESS;
    }
}
