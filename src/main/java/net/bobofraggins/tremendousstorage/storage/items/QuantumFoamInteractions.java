package net.bobofraggins.tremendousstorage.storage.items;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Registers all {@link CauldronInteraction} entries for Quantum Foam.
 *
 * <p>Three interactions are registered:
 * <ol>
 *   <li>Empty cauldron + Quantum Foam Bucket → Quantum Foam Cauldron
 *   <li>Quantum Foam Cauldron + empty bucket → Quantum Foam Bucket (empties cauldron)
 *   <li>Quantum Foam Cauldron + Glistering Melon Slice → Positive Vibes Cauldron
 * </ol>
 *
 * <p>Call {@link #register()} from {@code FMLCommonSetupEvent} via {@code enqueueWork}.
 */
public final class QuantumFoamInteractions {

    /**
     * Interaction map for the Quantum Foam cauldron block.
     * Passed to {@link QuantumFoamCauldronBlock}'s constructor.
     */
    public static final CauldronInteraction.InteractionMap CAULDRON_INTERACTIONS =
            CauldronInteraction.newInteractionMap("quantum_foam");

    private QuantumFoamInteractions() {}

    /** Registers all cauldron interactions. Call once during common setup. */
    public static void register() {

        // 1. Empty cauldron + Quantum Foam Bucket → Quantum Foam Cauldron
        CauldronInteraction.EMPTY
                .map()
                .put(Registration.QUANTUM_FOAM_BUCKET.get(), (state, level, pos, player, hand, stack) -> {
                    if (!level.isClientSide) {
                        stack.shrink(1);
                        player.addItem(new ItemStack(Items.BUCKET));
                        level.setBlockAndUpdate(
                                pos, Registration.QUANTUM_FOAM_CAULDRON.get().defaultBlockState());
                        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide);
                });

        // 2. Quantum Foam Cauldron + empty bucket → Quantum Foam Bucket
        CAULDRON_INTERACTIONS.map().put(Items.BUCKET, (state, level, pos, player, hand, stack) -> {
            if (!level.isClientSide) {
                stack.shrink(1);
                player.addItem(new ItemStack(Registration.QUANTUM_FOAM_BUCKET.get()));
                level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
                level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        });

        // 3. Quantum Foam Cauldron + Glistering Melon Slice → Positive Vibes Cauldron
        CAULDRON_INTERACTIONS.map().put(Items.GLISTERING_MELON_SLICE, (state, level, pos, player, hand, stack) -> {
            if (!level.isClientSide) {
                stack.shrink(1);
                level.setBlockAndUpdate(
                        pos, Registration.POSITIVE_VIBES_CAULDRON.get().defaultBlockState());
                level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        });
    }
}
