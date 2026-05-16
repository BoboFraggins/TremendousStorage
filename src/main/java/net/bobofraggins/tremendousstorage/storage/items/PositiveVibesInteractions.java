package net.bobofraggins.tremendousstorage.storage.items;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Registers all {@link CauldronInteraction} entries for Positive Vibes.
 *
 * <p>Three interactions are registered:
 * <ol>
 *   <li>Water cauldron + Glistering Melon Slice → Positive Vibes cauldron (consumes melon)
 *   <li>Positive Vibes cauldron + empty bucket → Positive Vibes Bucket (empties cauldron)
 *   <li>Positive Vibes cauldron + Zombie Brain → Brain replaces item in hand, cauldron emptied
 * </ol>
 *
 * <p>Call {@link #register()} from {@code FMLCommonSetupEvent} via {@code enqueueWork}.
 */
public final class PositiveVibesInteractions {

    /**
     * Interaction map for the Positive Vibes cauldron block.
     * Passed to {@link PositiveVibesCauldronBlock}'s constructor.
     */
    public static final CauldronInteraction.InteractionMap CAULDRON_INTERACTIONS =
            CauldronInteraction.newInteractionMap("positive_vibes");

    private PositiveVibesInteractions() {}

    /** Registers all cauldron interactions. Call once during common setup. */
    public static void register() {

        // 1. Empty cauldron + Positive Vibes Bucket → Positive Vibes cauldron
        CauldronInteraction.EMPTY
                .map()
                .put(Registration.POSITIVE_VIBES_BUCKET.get(), (state, level, pos, player, hand, stack) -> {
                    if (!level.isClientSide()) {
                        stack.shrink(1);
                        player.addItem(new ItemStack(Items.BUCKET));
                        level.setBlockAndUpdate(
                                pos, Registration.POSITIVE_VIBES_CAULDRON.get().defaultBlockState());
                        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
                    }
                    return InteractionResult.SUCCESS;
                });

        // 2. Positive Vibes cauldron + empty bucket → Positive Vibes Bucket
        CAULDRON_INTERACTIONS.map().put(Items.BUCKET, (state, level, pos, player, hand, stack) -> {
            if (!level.isClientSide()) {
                stack.shrink(1);
                player.addItem(new ItemStack(Registration.POSITIVE_VIBES_BUCKET.get()));
                level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
                level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            }
            return InteractionResult.SUCCESS;
        });

        // 3. Positive Vibes cauldron + Zombie Brain → Brain in hand, cauldron emptied
        CAULDRON_INTERACTIONS.map().put(Registration.ZOMBIE_BRAIN.get(), (state, level, pos, player, hand, stack) -> {
            if (!level.isClientSide()) {
                stack.shrink(1);
                player.setItemInHand(hand, new ItemStack(Registration.BRAIN.get()));
                level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
                level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            }
            return InteractionResult.SUCCESS;
        });
    }
}
