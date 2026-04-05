package net.bobofraggins.intellistore.shared.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.bobofraggins.intellistore.shared.storage.StorageTier;
import net.bobofraggins.intellistore.storage.enderfolder.EnderFolderItem;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * Adds to the end city treasure chest:
 * - Uncommon (20%): a linked pair of Ender Folders (Diamond 80%, Emerald 20%)
 * - Rare (5%): a Lazurite Paxel with 2-3 non-curse enchantments
 */
public class EndCityModifier extends LootModifier {

    public static final MapCodec<EndCityModifier> CODEC =
            RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, EndCityModifier::new));

    /** Enchantment pool entries: resource path → max level. */
    private static final List<Map.Entry<String, Integer>> ENCHANT_POOL = List.of(
            Map.entry("efficiency", 3),
            Map.entry("fortune", 2),
            Map.entry("silk_touch", 1),
            Map.entry("unbreaking", 2),
            Map.entry("sharpness", 3),
            Map.entry("mending", 1));

    public EndCityModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        RandomSource rng = context.getRandom();

        // Linked ender folder pair (20% chance)
        if (rng.nextFloat() < 0.20f) {
            StorageTier tier = rng.nextFloat() < 0.80f ? StorageTier.DIAMOND : StorageTier.EMERALD;
            ItemStack folder1 = new ItemStack(Registration.ENDER_FOLDER.get());
            folder1.set(
                    Registration.FOLDER_CONTENTS.get(),
                    net.bobofraggins.intellistore.storage.manillafolder.FolderContents.EMPTY.withTier(tier));
            ItemStack folder2 = new ItemStack(Registration.ENDER_FOLDER.get());
            folder2.set(
                    Registration.FOLDER_CONTENTS.get(),
                    net.bobofraggins.intellistore.storage.manillafolder.FolderContents.EMPTY.withTier(tier));
            long linkId = rng.nextLong();
            EnderFolderItem.setLinkId(folder1, linkId);
            EnderFolderItem.setLinkId(folder2, linkId);
            generatedLoot.add(folder1);
            generatedLoot.add(folder2);
        }

        // Enchanted lazurite paxel (5% chance)
        if (rng.nextFloat() < 0.05f) {
            ItemStack paxel = new ItemStack(Registration.LAZURITE_PAXEL.get());
            applyRandomEnchants(paxel, rng, context.getLevel().registryAccess());
            generatedLoot.add(paxel);
        }

        return generatedLoot;
    }

    private static void applyRandomEnchants(ItemStack item, RandomSource rng, HolderLookup.Provider registries) {
        HolderLookup.RegistryLookup<Enchantment> lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);

        // Shuffle pool using Fisher-Yates
        List<Map.Entry<String, Integer>> pool = new ArrayList<>(ENCHANT_POOL);
        for (int i = pool.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            Map.Entry<String, Integer> tmp = pool.get(i);
            pool.set(i, pool.get(j));
            pool.set(j, tmp);
        }

        int target = 2 + rng.nextInt(2); // 2 or 3
        boolean fortuneSelected = false;
        boolean silkTouchSelected = false;
        ItemEnchantments.Mutable enchants = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        int added = 0;

        for (Map.Entry<String, Integer> entry : pool) {
            if (added >= target) break;
            String path = entry.getKey();
            // Fortune and Silk Touch are mutually exclusive
            if (path.equals("fortune") && silkTouchSelected) continue;
            if (path.equals("silk_touch") && fortuneSelected) continue;

            ResourceKey<Enchantment> key =
                    ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.withDefaultNamespace(path));
            lookup.get(key).ifPresent((Holder<Enchantment> holder) -> {
                int level = 1 + rng.nextInt(entry.getValue());
                enchants.set(holder, level);
            });

            if (path.equals("fortune")) fortuneSelected = true;
            if (path.equals("silk_touch")) silkTouchSelected = true;
            added++;
        }

        item.set(DataComponents.ENCHANTMENTS, enchants.toImmutable());
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
