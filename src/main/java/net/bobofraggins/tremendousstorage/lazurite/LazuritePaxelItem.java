package net.bobofraggins.tremendousstorage.lazurite;

import java.util.Set;
import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

public class LazuritePaxelItem extends net.minecraft.world.item.Item {

    private static final Set<ItemAbility> PAXEL_ACTIONS =
            Set.copyOf(com.google.common.collect.ImmutableSet.<ItemAbility>builder()
                    .addAll(ItemAbilities.DEFAULT_AXE_ACTIONS)
                    .addAll(ItemAbilities.DEFAULT_SHOVEL_ACTIONS)
                    .addAll(ItemAbilities.DEFAULT_HOE_ACTIONS)
                    .build());

    public LazuritePaxelItem(Properties properties) {
        super(LazuriteTier.PAXEL.applyToolProperties(
                properties,
                BlockTags.create(ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "mineable/paxel")),
                3.0f,
                -3.0f,
                LazuriteTier.PAXEL.speed()));
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility action) {
        return PAXEL_ACTIONS.contains(action);
    }

    /** Tries axe interactions first (strip/scrape/wax-off), then shovel (flatten/douse campfire), then hoe (till). */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        BlockState state = level.getBlockState(pos);

        // Axe: strip, scrape, wax-off
        BlockState result = tryAxe(state, context, level, pos, player);

        // Shovel: flatten path or douse campfire
        if (result == null) {
            if (context.getClickedFace() == Direction.DOWN) {
                return InteractionResult.PASS;
            }
            result = state.getToolModifiedState(context, ItemAbilities.SHOVEL_FLATTEN, false);
            if (result != null && level.isEmptyBlock(pos.above())) {
                level.playSound(player, pos, SoundEvents.SHOVEL_FLATTEN, SoundSource.BLOCKS, 1.0f, 1.0f);
            } else {
                result = state.getToolModifiedState(context, ItemAbilities.SHOVEL_DOUSE, false);
                if (result != null && !level.isClientSide) {
                    level.levelEvent(null, LevelEvent.SOUND_EXTINGUISH_FIRE, pos, 0);
                }
            }
        }

        // Hoe: till
        if (result == null) {
            result = state.getToolModifiedState(context, ItemAbilities.HOE_TILL, false);
        }

        if (result == null) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            ItemStack stack = context.getItemInHand();
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(serverPlayer, pos, stack);
            }
            level.setBlock(pos, result, Block.UPDATE_ALL_IMMEDIATE);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, result));
            if (player != null) {
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.getHand()));
            }
        }
        return InteractionResult.SUCCESS;
    }

    private BlockState tryAxe(BlockState state, UseOnContext context, Level level, BlockPos pos, Player player) {
        BlockState result = state.getToolModifiedState(context, ItemAbilities.AXE_STRIP, false);
        if (result != null) {
            level.playSound(player, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0f, 1.0f);
            return result;
        }
        result = state.getToolModifiedState(context, ItemAbilities.AXE_SCRAPE, false);
        if (result != null) {
            level.playSound(player, pos, SoundEvents.AXE_SCRAPE, SoundSource.BLOCKS, 1.0f, 1.0f);
            level.levelEvent(player, LevelEvent.PARTICLES_SCRAPE, pos, 0);
            return result;
        }
        result = state.getToolModifiedState(context, ItemAbilities.AXE_WAX_OFF, false);
        if (result != null) {
            level.playSound(player, pos, SoundEvents.AXE_WAX_OFF, SoundSource.BLOCKS, 1.0f, 1.0f);
            level.levelEvent(player, LevelEvent.PARTICLES_WAX_OFF, pos, 0);
            return result;
        }
        return null;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // intentionally empty - mark as valid for durability consumption
    }

    /** Sword: consume 1 durability per hit (not 2 like a digger tool). */
    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
    }
}
