package net.bobofraggins.tremendousstorage.storage.items;

import java.util.Set;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/**
 * Gives mobs that naturally spawn wielding a vanilla sword, axe, or shovel a chance to instead
 * carry the Lazurite equivalent, or a Lazurite Paxel.
 *
 * <p>Probabilities (applied after vanilla equipment is placed):
 *
 * <ul>
 *   <li>10 % — Lazurite Paxel replaces whatever sword/axe/shovel the mob was given.
 *   <li>10 % — Lazurite equivalent of the vanilla tool (sword→sword, axe→axe, shovel→shovel).
 *   <li>80 % — Vanilla tool is kept unchanged.
 * </ul>
 *
 * <p>Enchantment chance matches vanilla mob enchantment logic: 25 % × regional-difficulty
 * multiplier, with a level of 5 + multiplier × rand(18).
 *
 * <p>Only applies to mobs that spawned naturally or from spawners — not to egg/command spawns.
 */
public final class LazuriteEquipmentHandler {

    private LazuriteEquipmentHandler() {}

    private static final float PAXEL_CHANCE = 0.10f;
    private static final float LAZURITE_TOOL_CHANCE = 0.10f;

    private static final Set<Item> SWORDS = Set.of(
            Items.WOODEN_SWORD,
            Items.STONE_SWORD,
            Items.IRON_SWORD,
            Items.GOLDEN_SWORD,
            Items.DIAMOND_SWORD,
            Items.NETHERITE_SWORD);

    private static final Set<Item> AXES = Set.of(
            Items.WOODEN_AXE,
            Items.STONE_AXE,
            Items.IRON_AXE,
            Items.GOLDEN_AXE,
            Items.DIAMOND_AXE,
            Items.NETHERITE_AXE);

    private static final Set<Item> SHOVELS = Set.of(
            Items.WOODEN_SHOVEL,
            Items.STONE_SHOVEL,
            Items.IRON_SHOVEL,
            Items.GOLDEN_SHOVEL,
            Items.DIAMOND_SHOVEL,
            Items.NETHERITE_SHOVEL);

    @SubscribeEvent
    static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        // Only natural / spawner spawns — skip commands, eggs, etc.
        EntitySpawnReason spawnType = event.getSpawnType();
        if (spawnType == EntitySpawnReason.COMMAND
                || spawnType == EntitySpawnReason.SPAWN_ITEM_USE
                || spawnType == EntitySpawnReason.BUCKET
                || spawnType == EntitySpawnReason.DISPENSER) return;

        Mob mob = event.getEntity();
        ItemStack mainhand = mob.getMainHandItem();
        if (mainhand.isEmpty()) return;

        Item held = mainhand.getItem();
        boolean isSword = SWORDS.contains(held);
        boolean isAxe = AXES.contains(held);
        boolean isShovel = SHOVELS.contains(held);
        if (!isSword && !isAxe && !isShovel) return;

        RandomSource random = mob.getRandom();
        float roll = random.nextFloat();

        Item replacement;
        if (roll < PAXEL_CHANCE) {
            replacement = Registration.LAZURITE_PAXEL.get();
        } else if (roll < PAXEL_CHANCE + LAZURITE_TOOL_CHANCE) {
            if (isSword) replacement = Registration.LAZURITE_SWORD.get();
            else if (isAxe) replacement = Registration.LAZURITE_AXE.get();
            else replacement = Registration.LAZURITE_SHOVEL.get();
        } else {
            return; // Keep vanilla item
        }

        ItemStack newStack = new ItemStack(replacement);

        // Re-roll enchantment at vanilla mob difficulty rate (25% × specialMultiplier).
        DifficultyInstance difficulty = event.getDifficulty();
        ServerLevelAccessor serverLevel = event.getLevel();
        if (difficulty.getDifficulty() != Difficulty.PEACEFUL) {
            float specialMultiplier = difficulty.getSpecialMultiplier();
            if (specialMultiplier > 0 && random.nextFloat() < 0.25F * specialMultiplier) {
                int enchantLevel = (int) (5.0F + specialMultiplier * random.nextInt(18));
                HolderLookup.RegistryLookup<net.minecraft.world.item.enchantment.Enchantment> enchantRegistry =
                        serverLevel.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                newStack = EnchantmentHelper.enchantItem(
                        random,
                        newStack,
                        enchantLevel,
                        serverLevel.registryAccess(),
                        enchantRegistry.get(EnchantmentTags.IN_ENCHANTING_TABLE));
            }
        }

        mob.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, newStack);
        mob.setDropChance(EquipmentSlot.MAINHAND, 0.085f);
    }
}
