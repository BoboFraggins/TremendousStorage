package net.bobofraggins.intellistore.storage.items;

import net.bobofraggins.intellistore.shared.register.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Registers all {@link CauldronInteraction} entries for Healing Salve.
 *
 * <p>Three interactions are registered:
 * <ol>
 *   <li>Water cauldron + Glistering Melon Slice → Healing Salve cauldron (consumes melon)
 *   <li>Healing Salve cauldron + empty bucket → Healing Salve Bucket (empties cauldron)
 *   <li>Healing Salve cauldron + Zombie Brain → spawns Brain item entity (cauldron stays full)
 * </ol>
 *
 * <p>Call {@link #register()} from {@code FMLCommonSetupEvent} via {@code enqueueWork}.
 */
public final class HealingSalveInteractions {

    /**
     * Interaction map for the Healing Salve cauldron block.
     * Passed to {@link HealingSalveCauldronBlock}'s constructor.
     */
    public static final CauldronInteraction.InteractionMap CAULDRON_INTERACTIONS =
            CauldronInteraction.newInteractionMap("healing_salve");

    private HealingSalveInteractions() {}

    /** Registers all cauldron interactions. Call once during common setup. */
    public static void register() {

        // 1. Water cauldron + Glistering Melon Slice → Healing Salve cauldron
        CauldronInteraction.WATER.map().put(Items.GLISTERING_MELON_SLICE, (state, level, pos, player, hand, stack) -> {
            if (!level.isClientSide) {
                stack.shrink(1);
                level.setBlockAndUpdate(
                        pos, Registration.HEALING_SALVE_CAULDRON.get().defaultBlockState());
                level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        });

        // 2. Healing Salve cauldron + empty bucket → Healing Salve Bucket
        CAULDRON_INTERACTIONS.map().put(Items.BUCKET, (state, level, pos, player, hand, stack) -> {
            if (!level.isClientSide) {
                stack.shrink(1);
                player.addItem(new ItemStack(Registration.HEALING_SALVE_BUCKET.get()));
                level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
                level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        });

        // 3. Healing Salve cauldron + Zombie Brain → spawns Brain item entity (cauldron stays full)
        CAULDRON_INTERACTIONS.map().put(Registration.ZOMBIE_BRAIN.get(), (state, level, pos, player, hand, stack) -> {
            if (!level.isClientSide) {
                stack.shrink(1);
                BlockPos above = pos.above();
                ItemEntity brainEntity = new ItemEntity(
                        level,
                        above.getX() + 0.5,
                        above.getY() + 0.2,
                        above.getZ() + 0.5,
                        new ItemStack(Registration.BRAIN.get()));
                brainEntity.setDeltaMovement(0, 0.25, 0);
                level.addFreshEntity(brainEntity);
                level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        });
    }
}
