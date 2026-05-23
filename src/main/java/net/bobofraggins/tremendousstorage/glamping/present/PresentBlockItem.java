package net.bobofraggins.tremendousstorage.glamping.present;

import java.util.Optional;
import java.util.function.Consumer;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;

public class PresentBlockItem extends BlockItem {

    static final String WRAPPED_STATE_KEY = "WrappedState";
    static final String WRAPPED_ENTITY_KEY = "WrappedEntity";

    public PresentBlockItem(Block block, Item.Properties props) {
        super(block, props);
    }

    // -------------------------------------------------------------------------
    // Wrapping — right-click on a block (runs before block interaction)
    // -------------------------------------------------------------------------

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        if (hasWrappedBlock(stack)) return InteractionResult.PASS;

        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = ctx.getPlayer();

        if (!canWrap(state, level, pos)) return InteractionResult.PASS;

        if (!level.isClientSide()) {
            CompoundTag entityData = captureBlockEntity(level, pos);

            Direction facing = ctx.getHorizontalDirection().getOpposite();
            level.setBlock(
                    pos,
                    Registration.PRESENT.get().defaultBlockState().setValue(PresentBlock.FACING, facing),
                    Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);

            if (level.getBlockEntity(pos) instanceof PresentBlockEntity present) {
                present.setWrappedBlock(state, entityData);
            }

            if (player != null && !player.isCreative()) {
                stack.shrink(1);
            }
        }

        return InteractionResult.SUCCESS;
    }

    // -------------------------------------------------------------------------
    // Placement — transfer wrapped data from item to block entity
    // -------------------------------------------------------------------------

    @Override
    protected boolean placeBlock(BlockPlaceContext ctx, BlockState state) {
        boolean placed = super.placeBlock(ctx, state);
        if (placed && !ctx.getLevel().isClientSide()) {
            CustomData customData = ctx.getItemInHand().get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                CompoundTag wrapper = customData.copyTag();
                if (wrapper.contains(WRAPPED_STATE_KEY)) {
                    Level level = ctx.getLevel();
                    BlockPos pos = ctx.getClickedPos();
                    BlockState wrappedState = NbtUtils.readBlockState(
                            level.holderLookup(Registries.BLOCK), wrapper.getCompoundOrEmpty(WRAPPED_STATE_KEY));
                    CompoundTag entityData = wrapper.contains(WRAPPED_ENTITY_KEY)
                            ? wrapper.getCompoundOrEmpty(WRAPPED_ENTITY_KEY)
                            : null;
                    if (level.getBlockEntity(pos) instanceof PresentBlockEntity present) {
                        present.setWrappedBlock(wrappedState, entityData);
                    }
                }
            }
        }
        return placed;
    }

    // -------------------------------------------------------------------------
    // Display — item name includes wrapped block type
    // -------------------------------------------------------------------------

    @Override
    public Component getName(ItemStack stack) {
        Component base = super.getName(stack);
        if (!hasWrappedBlock(stack)) return base;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return base;
        Optional<String> blockId =
                data.copyTag().getCompoundOrEmpty(WRAPPED_STATE_KEY).getString("Name");
        return blockId.flatMap(id -> Optional.ofNullable(Identifier.tryParse(id)))
                .map(rl -> BuiltInRegistries.BLOCK.getValue(rl))
                .map(block -> {
                    net.minecraft.world.item.Item item = block.asItem();
                    Component wrappedName = item == net.minecraft.world.item.Items.AIR
                            ? block.getName()
                            : item.getName(new ItemStack(item));
                    return (Component) Component.empty()
                            .append(base)
                            .append(" (")
                            .append(wrappedName)
                            .append(")");
                })
                .orElse(base);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay lines,
            Consumer<Component> tooltipAdder,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, tooltipAdder, flag);
        if (hasWrappedBlock(stack)) {
            tooltipAdder.accept(Component.translatable("item.tremendousstorage.present.tooltip_unwrap"));
        } else {
            tooltipAdder.accept(Component.translatable("item.tremendousstorage.present.tooltip_wrap"));
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    public static boolean hasWrappedBlock(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(WRAPPED_STATE_KEY);
    }

    private static boolean canWrap(BlockState state, Level level, BlockPos pos) {
        if (state.isAir()) return false;
        if (state.getBlock() instanceof PresentBlock) return false;
        if (state.getDestroySpeed(level, pos) < 0) return false;
        return true;
    }

    private static CompoundTag captureBlockEntity(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return null;
        TagValueOutput beOut = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        be.saveCustomOnly(beOut);
        CompoundTag saved = beOut.buildResult();
        return saved.isEmpty() ? null : saved;
    }
}
