package net.bobofraggins.tremendousstorage.shared.loot;

import com.mojang.serialization.MapCodec;
import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class LootModifiers {

    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> GLM_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, TremendousStorage.MODID);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<VillageCartographerModifier>>
            VILLAGE_CARTOGRAPHER_FOLDERS =
                    GLM_SERIALIZERS.register("village_cartographer_folders", () -> VillageCartographerModifier.CODEC);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<VillageWeaponsmithModifier>>
            VILLAGE_WEAPONSMITH_SWORD =
                    GLM_SERIALIZERS.register("village_weaponsmith_sword", () -> VillageWeaponsmithModifier.CODEC);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<VillageToolsmithModifier>>
            VILLAGE_TOOLSMITH_TOOL =
                    GLM_SERIALIZERS.register("village_toolsmith_tool", () -> VillageToolsmithModifier.CODEC);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<EndCityModifier>> END_CITY =
            GLM_SERIALIZERS.register("end_city", () -> EndCityModifier.CODEC);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<AnyChestModifier>>
            ANY_CHEST_SECRETS = GLM_SERIALIZERS.register("any_chest_secrets", () -> AnyChestModifier.CODEC);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<SpawnBonusChestModifier>>
            SPAWN_BONUS_CHEST = GLM_SERIALIZERS.register("spawn_bonus_chest", () -> SpawnBonusChestModifier.CODEC);

    public static void register(IEventBus modBus) {
        GLM_SERIALIZERS.register(modBus);
    }
}
