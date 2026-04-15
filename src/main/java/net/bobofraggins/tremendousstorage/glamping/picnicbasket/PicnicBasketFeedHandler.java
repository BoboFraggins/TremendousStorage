package net.bobofraggins.tremendousstorage.glamping.picnicbasket;

import java.util.ArrayList;
import java.util.List;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Auto-feeds the player from a Picnic Basket (regular or Ender) when their hunger is below max.
 *
 * <p>Every {@link #FEED_INTERVAL} ticks, the handler scans the player's main inventory and any
 * Curios charm slot for a basket that has food stored. If found, a random stored food type is
 * chosen, consumed (applying nutrition, saturation, and all food effects), and its count
 * decremented in the basket's {@link DataComponents#BLOCK_ENTITY_DATA}.
 *
 * <p>Works in survival and adventure mode only; skipped for creative/spectator players.
 */
public class PicnicBasketFeedHandler {

    private PicnicBasketFeedHandler() {}

    /** Ticks between feeding attempts. 40 t = 2 s. */
    private static final int FEED_INTERVAL = 40;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (player.isCreative() || player.isSpectator()) return;
        if (!player.getFoodData().needsFood()) return;
        if (player.level().getGameTime() % FEED_INTERVAL != 0) return;

        ItemStack basketStack = findBasket(player);
        if (basketStack == null) return;

        feedFromBasket(basketStack, player, (ServerLevel) player.level());
    }

    // -------------------------------------------------------------------------
    // Find basket
    // -------------------------------------------------------------------------

    private static ItemStack findBasket(Player player) {
        // Main inventory (includes offhand at slot 40)
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isBasket(stack)) return stack;
        }

        // Curios (soft dependency)
        try {
            var curiosInv = player.getCapability(top.theillusivec4.curios.api.CuriosCapability.INVENTORY);
            if (curiosInv != null) {
                for (var entry : curiosInv.getCurios().entrySet()) {
                    var handler = entry.getValue().getStacks();
                    for (int i = 0; i < handler.getSlots(); i++) {
                        ItemStack stack = handler.getStackInSlot(i);
                        if (isBasket(stack)) return stack;
                    }
                }
            }
        } catch (NoClassDefFoundError | Exception ignored) {
            // Curios not installed — skip
        }

        return null;
    }

    private static boolean isBasket(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() == Registration.PICNIC_BASKET_ITEM.get()
                || stack.getItem() == Registration.ENDER_PICNIC_BASKET_ITEM.get();
    }

    // -------------------------------------------------------------------------
    // Feed from basket
    // -------------------------------------------------------------------------

    private static void feedFromBasket(ItemStack basketStack, Player player, ServerLevel level) {
        CustomData existing = basketStack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (existing == null) return;

        CompoundTag beTag = existing.copyTag();
        // Default true: feed unless explicitly disabled
        if (beTag.contains("AutoFeed") && !beTag.getBoolean("AutoFeed")) return;
        ListTag types = beTag.getList("Types", Tag.TAG_COMPOUND);
        if (types.isEmpty()) return;

        HolderLookup.Provider registries = level.registryAccess();

        // Collect indices of non-empty food entries
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < types.size(); i++) {
            if (types.getCompound(i).getLong("Count") > 0) {
                ItemStack stored =
                        ItemStack.parseOptional(registries, types.getCompound(i).getCompound("Type"));
                if (!stored.isEmpty() && stored.has(DataComponents.FOOD)) {
                    candidates.add(i);
                }
            }
        }
        if (candidates.isEmpty()) return;

        int chosenIdx = candidates.get(level.random.nextInt(candidates.size()));
        CompoundTag entry = types.getCompound(chosenIdx);
        ItemStack stored = ItemStack.parseOptional(registries, entry.getCompound("Type"));
        if (stored.isEmpty()) return;

        FoodProperties food = stored.get(DataComponents.FOOD);
        if (food == null) return;

        // Apply nutrition, saturation, and all food effects
        player.getFoodData().eat(food);
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.GENERIC_EAT,
                SoundSource.PLAYERS,
                0.5f,
                level.random.nextFloat() * 0.1f + 0.9f);
        for (FoodProperties.PossibleEffect possibleEffect : food.effects()) {
            if (level.random.nextFloat() < possibleEffect.probability()) {
                player.addEffect(possibleEffect.effect());
            }
        }

        // Decrement stored count; remove entry if exhausted
        long remaining = entry.getLong("Count") - 1;
        if (remaining <= 0) {
            types.remove(chosenIdx);
        } else {
            entry.putLong("Count", remaining);
        }

        beTag.put("Types", types);
        basketStack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(beTag));
    }
}
