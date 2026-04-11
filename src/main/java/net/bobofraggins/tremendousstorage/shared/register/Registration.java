package net.bobofraggins.tremendousstorage.shared.register;

import com.blakebr0.mysticalagriculture.api.MysticalAgricultureAPI;
import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.external.exdeorum.ExDeorumIntegration;
import net.bobofraggins.tremendousstorage.external.exnihilosequentia.ExNihiloSequentiaIntegration;
import net.bobofraggins.tremendousstorage.experiencesyringe.ExperienceSyringeItem;
import net.bobofraggins.tremendousstorage.lazurite.LazuriteBarsBlock;
import net.bobofraggins.tremendousstorage.lazurite.LazuriteOreBlock;
import net.bobofraggins.tremendousstorage.lazurite.LazuritePaxelItem;
import net.bobofraggins.tremendousstorage.lazurite.LazuriteTier;
import net.bobofraggins.tremendousstorage.shared.loot.LootModifiers;
import net.bobofraggins.tremendousstorage.shared.storage.EnderTieredBlockItem;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.shared.storage.TieredBlockItem;
import net.bobofraggins.tremendousstorage.shared.ui.PriorityControl;
import net.bobofraggins.tremendousstorage.shared.ui.TankSettingsMenu;
import net.bobofraggins.tremendousstorage.storage.accessterminal.AccessTerminalBlock;
import net.bobofraggins.tremendousstorage.storage.accessterminal.AccessTerminalBlockEntity;
import net.bobofraggins.tremendousstorage.storage.accessterminal.AccessTerminalMenu;
import net.bobofraggins.tremendousstorage.storage.baseupgrade.BaseUpgradeItem;
import net.bobofraggins.tremendousstorage.storage.baseupgrade.HaarpUpgradeItem;
import net.bobofraggins.tremendousstorage.storage.baseupgrade.MagnetUpgradeItem;
import net.bobofraggins.tremendousstorage.storage.enderbackpack.EnderBackpackSmithingRecipe;
import net.bobofraggins.tremendousstorage.storage.enderbackpack.EnderBackpackBlock;
import net.bobofraggins.tremendousstorage.storage.enderbackpack.EnderBackpackBlockEntity;
import net.bobofraggins.tremendousstorage.storage.enderbackpack.EnderBackpackItem;
import net.bobofraggins.tremendousstorage.storage.enderbackpack.EnderBackpackMenu;
import net.bobofraggins.tremendousstorage.storage.enderchest.EnderChestSmithingRecipe;
import net.bobofraggins.tremendousstorage.storage.enderchest.EnderChestBlock;
import net.bobofraggins.tremendousstorage.storage.enderchest.EnderChestBlockEntity;
import net.bobofraggins.tremendousstorage.storage.enderfolder.EnderFolderItem;
import net.bobofraggins.tremendousstorage.storage.enderfolder.EnderFolderSmithingRecipe;
import net.bobofraggins.tremendousstorage.storage.filingcabinet.FilingCabinetBlock;
import net.bobofraggins.tremendousstorage.storage.filingcabinet.FilingCabinetBlockEntity;
import net.bobofraggins.tremendousstorage.storage.filingcabinet.FilingCabinetItemHandler;
import net.bobofraggins.tremendousstorage.storage.filingcabinet.FilingCabinetMenu;
import net.bobofraggins.tremendousstorage.storage.items.BrainItem;
import net.bobofraggins.tremendousstorage.storage.items.PositiveVibesBlock;
import net.bobofraggins.tremendousstorage.storage.items.PositiveVibesCauldronBlock;
import net.bobofraggins.tremendousstorage.storage.items.PositiveVibesFluid;
import net.bobofraggins.tremendousstorage.storage.items.PositiveVibesInteractions;
import net.bobofraggins.tremendousstorage.storage.items.VexRepellentEffect;
import net.bobofraggins.tremendousstorage.storage.items.VexRepellentPotionItem;
import net.bobofraggins.tremendousstorage.storage.items.ZombieBrainItem;
import net.bobofraggins.tremendousstorage.storage.manillafolder.FolderContents;
import net.bobofraggins.tremendousstorage.storage.manillafolder.FolderExtractRecipe;
import net.bobofraggins.tremendousstorage.storage.manillafolder.FolderMergeRecipe;
import net.bobofraggins.tremendousstorage.storage.manillafolder.FolderStorageRecipe;
import net.bobofraggins.tremendousstorage.storage.manillafolder.ManillaFolderItem;
import net.bobofraggins.tremendousstorage.storage.networkinterface.NetworkInterfaceBlock;
import net.bobofraggins.tremendousstorage.storage.networkinterface.NetworkInterfaceBlockEntity;
import net.bobofraggins.tremendousstorage.storage.networkinterface.NetworkInterfaceMenu;
import net.bobofraggins.tremendousstorage.storage.personalaccessterminal.PersonalAccessTerminalItem;
import net.bobofraggins.tremendousstorage.storage.storageupgrade.StorageSmithingUpgradeRecipe;
import net.bobofraggins.tremendousstorage.storage.storageupgrade.StorageUpgradeItem;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackBlock;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackBlockEntity;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackContents;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackItem;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackMenu;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlock;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlockEntity;
import net.bobofraggins.tremendousstorage.storage.chest.ChestItemHandler;
import net.bobofraggins.tremendousstorage.storage.chest.ChestMenu;
import net.bobofraggins.tremendousstorage.storage.endertank.EnderTankBlock;
import net.bobofraggins.tremendousstorage.storage.endertank.EnderTankBlockEntity;
import net.bobofraggins.tremendousstorage.storage.endertank.EnderTankItem;
import net.bobofraggins.tremendousstorage.storage.endertank.EnderTankSmithingRecipe;
import net.bobofraggins.tremendousstorage.storage.tank.TankBlock;
import net.bobofraggins.tremendousstorage.storage.tank.TankBlockEntity;
import net.bobofraggins.tremendousstorage.storage.tank.TankContents;
import net.bobofraggins.tremendousstorage.storage.tank.TankFluidHandler;
import net.bobofraggins.tremendousstorage.storage.tank.TankItem;
import net.bobofraggins.tremendousstorage.storage.tank.TankItemFluidHandler;
import net.bobofraggins.tremendousstorage.storage.tube.TubeBlock;
import net.bobofraggins.tremendousstorage.storage.tube.TubeBlockEntity;
import net.bobofraggins.tremendousstorage.storage.tubeattachments.ExportInterfaceItem;
import net.bobofraggins.tremendousstorage.storage.tubeattachments.ExportInterfaceMenu;
import net.bobofraggins.tremendousstorage.storage.tubeattachments.ImportInterfaceItem;
import net.bobofraggins.tremendousstorage.storage.tubeattachments.ImportInterfaceMenu;
import net.bobofraggins.tremendousstorage.storage.tubeattachments.InterfaceFilterContents;
import net.bobofraggins.tremendousstorage.storage.tubeattachments.StorageInterfaceItem;
import net.bobofraggins.tremendousstorage.storage.tubeattachments.StorageInterfaceMenu;
import net.bobofraggins.tremendousstorage.storage.whiteout.FolderTapeRecipe;
import net.bobofraggins.tremendousstorage.storage.whiteout.WhiteoutTapeItem;
import net.bobofraggins.tremendousstorage.storage.recyclingbin.RecyclingBinBlock;
import net.bobofraggins.tremendousstorage.storage.recyclingbin.RecyclingBinBlockEntity;
import net.bobofraggins.tremendousstorage.storage.recyclingbin.RecyclingBinFluidHandler;
import net.bobofraggins.tremendousstorage.storage.recyclingbin.RecyclingBinItemHandler;
import net.bobofraggins.tremendousstorage.storage.recyclingbin.RecyclingBinMenu;
import net.bobofraggins.tremendousstorage.storage.wirelesshub.WirelessHubBlock;
import net.bobofraggins.tremendousstorage.storage.wirelesshub.WirelessHubBlockEntity;
import net.bobofraggins.tremendousstorage.storage.wirelesshub.WirelessHubMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class Registration {

    private Registration() {}

    // -------------------------------------------------------------------------
    // Deferred registers
    // -------------------------------------------------------------------------

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TremendousStorage.MODID);

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TremendousStorage.MODID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TremendousStorage.MODID);

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(
            net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE, TremendousStorage.MODID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, TremendousStorage.MODID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TremendousStorage.MODID);

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, TremendousStorage.MODID);

    public static final DeferredRegister<net.minecraft.world.level.material.Fluid> FLUID_REGISTER =
            DeferredRegister.create(Registries.FLUID, TremendousStorage.MODID);

    public static final DeferredRegister<FluidType> FLUID_TYPE_REGISTER =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, TremendousStorage.MODID);

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, TremendousStorage.MODID);

    public static final DeferredHolder<MobEffect, VexRepellentEffect> VEX_REPELLENT_EFFECT =
            MOB_EFFECTS.register("vex_repellent", VexRepellentEffect::new);

    // -------------------------------------------------------------------------
    // Data components
    // -------------------------------------------------------------------------

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FolderContents>> FOLDER_CONTENTS =
            DATA_COMPONENTS.register("folder_contents", () -> DataComponentType.<FolderContents>builder()
                    .persistent(FolderContents.CODEC)
                    .networkSynchronized(FolderContents.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<TankContents>>
            TANK_CONTENTS = DATA_COMPONENTS.register(
                    "tank_contents", () -> DataComponentType.<TankContents>builder()
                            .persistent(TankContents.CODEC)
                            .networkSynchronized(TankContents.STREAM_CODEC)
                            .build());

    /**
     * Data component storing the linked Network Interface {@link BlockPos} in a Wireless SAT item.
     * Encoded as three varints (x, y, z) over the network.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> WIRELESS_NI_POS =
            DATA_COMPONENTS.register("wireless_ni_pos", () -> DataComponentType.<BlockPos>builder()
                    .persistent(BlockPos.CODEC)
                    .networkSynchronized(BlockPos.STREAM_CODEC)
                    .build());

    /**
     * Data component storing the Wireless Hub {@link BlockPos} that was used to link a Wireless SAT.
     * Used at access-time to verify the hub is still present on the network.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> WIRELESS_HUB_POS =
            DATA_COMPONENTS.register("wireless_hub_pos", () -> DataComponentType.<BlockPos>builder()
                    .persistent(BlockPos.CODEC)
                    .networkSynchronized(BlockPos.STREAM_CODEC)
                    .build());

    /**
     * Presence flag — set on a Wireless SAT item when the Crafting Upgrade has been applied.
     * Stored as a boolean; absent ≡ false.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>>
            WIRELESS_SAT_HAS_CRAFTING_UPGRADE = DATA_COMPONENTS.register(
                    "wireless_sat_has_crafting_upgrade", () -> DataComponentType.<Boolean>builder()
                            .persistent(com.mojang.serialization.Codec.BOOL)
                            .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.BOOL)
                            .build());

    /**
     * Data component storing the filter configuration on Import Interface and Export Interface items.
     * Carried on the item so filter state persists through break and re-attach cycles.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<InterfaceFilterContents>>
            INTERFACE_FILTER = DATA_COMPONENTS.register(
                    "interface_filter", () -> DataComponentType.<InterfaceFilterContents>builder()
                            .persistent(InterfaceFilterContents.CODEC)
                            .networkSynchronized(InterfaceFilterContents.STREAM_CODEC)
                            .build());

    /**
     * Data component storing the priority ordinal (0–4) on a Storage Interface item.
     * Persists the priority through punch-off and re-attach cycles; defaults to NORMAL (ordinal 2)
     * when absent.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> STORAGE_INTERFACE_PRIORITY =
            DATA_COMPONENTS.register("storage_interface_priority", () -> DataComponentType.<Integer>builder()
                    .persistent(com.mojang.serialization.Codec.INT)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.INT)
                    .build());

    /**
     * Data component storing the 64-bit shared link ID on an Ender Folder item.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> ENDER_LINK_ID =
            DATA_COMPONENTS.register("ender_link_id", () -> DataComponentType.<Long>builder()
                    .persistent(com.mojang.serialization.Codec.LONG)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_LONG)
                    .build());

    /** Data component storing the XP points (= mB of XP fluid) held inside an Experience Syringe. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>>
            EXPERIENCE_SYRINGE_STORED_XP = DATA_COMPONENTS.register(
                    "experience_syringe_stored_xp", () -> DataComponentType.<Integer>builder()
                            .persistent(com.mojang.serialization.Codec.intRange(0, ExperienceSyringeItem.CAPACITY))
                            .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT)
                            .build());

    /** Data component storing all inventory and settings on a Tremendous Backpack item. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BackpackContents>>
            TREMENDOUS_BACKPACK_CONTENTS = DATA_COMPONENTS.register(
                    "backpack_contents", () -> DataComponentType.<BackpackContents>builder()
                            .persistent(BackpackContents.CODEC)
                            .networkSynchronized(BackpackContents.STREAM_CODEC)
                            .build());


    // -------------------------------------------------------------------------
    // Blocks + block entities
    // -------------------------------------------------------------------------

    public static final DeferredBlock<FilingCabinetBlock> FILING_CABINET = BLOCKS.register(
            "filing_cabinet",
            () -> new FilingCabinetBlock(BlockBehaviour.Properties.of()
                    .strength(5.0f, 1000.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.WOOD)));

    public static final DeferredHolder<Item, BlockItem> FILING_CABINET_ITEM =
            ITEMS.registerSimpleBlockItem("filing_cabinet", FILING_CABINET);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FilingCabinetBlockEntity>>
            FILING_CABINET_BE_TYPE = BLOCK_ENTITY_TYPES.register("filing_cabinet", () -> BlockEntityType.Builder.of(
                    FilingCabinetBlockEntity::new, FILING_CABINET.get())
            .build(null));

    public static final DeferredBlock<ChestBlock> TREMENDOUS_CHEST = BLOCKS.register(
            "chest",
            () -> new ChestBlock(BlockBehaviour.Properties.of()
                    .strength(5.0f, 1000.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredHolder<Item, BlockItem> TREMENDOUS_CHEST_ITEM = ITEMS.register(
            "chest", () -> new TieredBlockItem(TREMENDOUS_CHEST.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChestBlockEntity>>
            TREMENDOUS_CHEST_BE_TYPE = BLOCK_ENTITY_TYPES.register("chest", () -> BlockEntityType.Builder.of(
                    ChestBlockEntity::new, TREMENDOUS_CHEST.get())
            .build(null));

    public static final DeferredBlock<EnderChestBlock> ENDER_TREMENDOUS_CHEST = BLOCKS.register(
            "ender_chest",
            () -> new EnderChestBlock(BlockBehaviour.Properties.of()
                    .strength(5.0f, 1000.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredHolder<Item, BlockItem> ENDER_TREMENDOUS_CHEST_ITEM = ITEMS.register(
            "ender_chest",
            () -> new EnderTieredBlockItem(ENDER_TREMENDOUS_CHEST.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnderChestBlockEntity>>
            ENDER_TREMENDOUS_CHEST_BE_TYPE = BLOCK_ENTITY_TYPES.register(
                    "ender_chest", () -> BlockEntityType.Builder.of(
                                    EnderChestBlockEntity::new, ENDER_TREMENDOUS_CHEST.get())
                            .build(null));

    public static final DeferredBlock<BackpackBlock> TREMENDOUS_BACKPACK_BLOCK = BLOCKS.register(
            "backpack",
            () -> new BackpackBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f, 1000.0f)
                    .sound(SoundType.WOOL)
                    .noOcclusion()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BackpackBlockEntity>>
            TREMENDOUS_BACKPACK_BE_TYPE =
                    BLOCK_ENTITY_TYPES.register("backpack", () -> BlockEntityType.Builder.of(
                                    BackpackBlockEntity::new, TREMENDOUS_BACKPACK_BLOCK.get())
                            .build(null));

    public static final DeferredBlock<EnderBackpackBlock> ENDER_TREMENDOUS_BACKPACK_BLOCK = BLOCKS.register(
            "ender_backpack",
            () -> new EnderBackpackBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f, 1000.0f)
                    .sound(SoundType.WOOL)
                    .noOcclusion()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnderBackpackBlockEntity>>
            ENDER_TREMENDOUS_BACKPACK_BE_TYPE = BLOCK_ENTITY_TYPES.register(
                    "ender_backpack", () -> BlockEntityType.Builder.of(
                                    EnderBackpackBlockEntity::new, ENDER_TREMENDOUS_BACKPACK_BLOCK.get())
                            .build(null));

    // -------------------------------------------------------------------------
    // Storage upgrade items
    // -------------------------------------------------------------------------

    public static final DeferredHolder<Item, StorageUpgradeItem> WOOD_TO_COPPER_STORAGE_UPGRADE = ITEMS.register(
            "wood_to_copper_storage_upgrade",
            () -> new StorageUpgradeItem(StorageTier.WOOD, StorageTier.COPPER, new Item.Properties()));

    public static final DeferredHolder<Item, StorageUpgradeItem> COPPER_TO_IRON_STORAGE_UPGRADE = ITEMS.register(
            "copper_to_iron_storage_upgrade",
            () -> new StorageUpgradeItem(StorageTier.COPPER, StorageTier.IRON, new Item.Properties()));

    public static final DeferredHolder<Item, StorageUpgradeItem> IRON_TO_GOLD_STORAGE_UPGRADE = ITEMS.register(
            "iron_to_gold_storage_upgrade",
            () -> new StorageUpgradeItem(StorageTier.IRON, StorageTier.GOLD, new Item.Properties()));

    public static final DeferredHolder<Item, StorageUpgradeItem> GOLD_TO_DIAMOND_STORAGE_UPGRADE = ITEMS.register(
            "gold_to_diamond_storage_upgrade",
            () -> new StorageUpgradeItem(StorageTier.GOLD, StorageTier.DIAMOND, new Item.Properties()));

    public static final DeferredHolder<Item, StorageUpgradeItem> DIAMOND_TO_EMERALD_STORAGE_UPGRADE = ITEMS.register(
            "diamond_to_emerald_storage_upgrade",
            () -> new StorageUpgradeItem(StorageTier.DIAMOND, StorageTier.EMERALD, new Item.Properties()));

    public static final DeferredHolder<Item, StorageUpgradeItem> EMERALD_TO_NETHERITE_STORAGE_UPGRADE = ITEMS.register(
            "emerald_to_netherite_storage_upgrade",
            () -> new StorageUpgradeItem(StorageTier.EMERALD, StorageTier.NETHERITE, new Item.Properties()));

    public static final DeferredHolder<Item, StorageUpgradeItem>[] STORAGE_UPGRADES = new DeferredHolder[] {
        WOOD_TO_COPPER_STORAGE_UPGRADE,
        COPPER_TO_IRON_STORAGE_UPGRADE,
        IRON_TO_GOLD_STORAGE_UPGRADE,
        GOLD_TO_DIAMOND_STORAGE_UPGRADE,
        DIAMOND_TO_EMERALD_STORAGE_UPGRADE,
        EMERALD_TO_NETHERITE_STORAGE_UPGRADE
    };

    public static final DeferredBlock<TankBlock> TANK = BLOCKS.register(
            "tank",
            () -> new TankBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f, 1000.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.GLASS)));

    public static final DeferredHolder<Item, BlockItem> TANK_ITEM = ITEMS.register(
            "tank", () -> new TankItem(TANK.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TankBlockEntity>>
            TANK_BE_TYPE = BLOCK_ENTITY_TYPES.register("tank", () -> BlockEntityType.Builder.of(
                    TankBlockEntity::new, TANK.get())
            .build(null));

    public static final DeferredBlock<EnderTankBlock> ENDER_TANK = BLOCKS.register(
            "ender_tank",
            () -> new EnderTankBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f, 1000.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.GLASS)));

    public static final DeferredHolder<Item, BlockItem> ENDER_TANK_ITEM = ITEMS.register(
            "ender_tank",
            () -> new EnderTankItem(ENDER_TANK.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnderTankBlockEntity>>
            ENDER_TANK_BE_TYPE = BLOCK_ENTITY_TYPES.register(
                    "ender_tank", () -> BlockEntityType.Builder.of(
                                    EnderTankBlockEntity::new, ENDER_TANK.get())
                            .build(null));

    // -------------------------------------------------------------------------
    // Recycling Bin
    // -------------------------------------------------------------------------

    public static final DeferredBlock<RecyclingBinBlock> RECYCLING_BIN = BLOCKS.register(
            "recycling_bin",
            () -> new RecyclingBinBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f, 1000.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredHolder<Item, BlockItem> RECYCLING_BIN_ITEM =
            ITEMS.registerSimpleBlockItem("recycling_bin", RECYCLING_BIN);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RecyclingBinBlockEntity>>
            RECYCLING_BIN_BE_TYPE = BLOCK_ENTITY_TYPES.register("recycling_bin", () -> BlockEntityType.Builder.of(
                    RecyclingBinBlockEntity::new, RECYCLING_BIN.get())
            .build(null));

    // -------------------------------------------------------------------------
    // Lazurite bars
    // -------------------------------------------------------------------------

    public static final DeferredBlock<LazuriteBarsBlock> LAZURITE_BARS = BLOCKS.register(
            "lazurite_bars",
            () -> new LazuriteBarsBlock(BlockBehaviour.Properties.of()
                    .strength(5.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredHolder<Item, BlockItem> LAZURITE_BARS_ITEM =
            ITEMS.registerSimpleBlockItem("lazurite_bars", LAZURITE_BARS);

    // -------------------------------------------------------------------------
    // Lazurite ore + ingot
    // -------------------------------------------------------------------------

    public static final DeferredBlock<LazuriteOreBlock> LAZURITE_ORE = BLOCKS.register(
            "lazurite_ore",
            () -> new LazuriteOreBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f, 3.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));

    public static final DeferredHolder<Item, BlockItem> LAZURITE_ORE_ITEM =
            ITEMS.registerSimpleBlockItem("lazurite_ore", LAZURITE_ORE);

    public static final DeferredBlock<LazuriteOreBlock> LAZURITE_DEEPSLATE_ORE = BLOCKS.register(
            "lazurite_deepslate_ore",
            () -> new LazuriteOreBlock(BlockBehaviour.Properties.of()
                    .strength(4.5f, 3.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE)));

    public static final DeferredHolder<Item, BlockItem> LAZURITE_DEEPSLATE_ORE_ITEM =
            ITEMS.registerSimpleBlockItem("lazurite_deepslate_ore", LAZURITE_DEEPSLATE_ORE);

    public static final DeferredHolder<Item, Item> RAW_LAZURITE =
            ITEMS.register("raw_lazurite", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> LAZURITE_INGOT =
            ITEMS.register("lazurite_ingot", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> LAZURITE_NUGGET =
            ITEMS.register("lazurite_nugget", () -> new Item(new Item.Properties()));

    public static final DeferredBlock<net.minecraft.world.level.block.Block> LAZURITE_BLOCK = BLOCKS.register(
            "lazurite_block",
            () -> new net.minecraft.world.level.block.Block(BlockBehaviour.Properties.of()
                    .strength(5.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));

    public static final DeferredHolder<Item, BlockItem> LAZURITE_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem("lazurite_block", LAZURITE_BLOCK);

    // -------------------------------------------------------------------------
    // Canvas block
    // -------------------------------------------------------------------------

    public static final DeferredBlock<net.minecraft.world.level.block.Block> CANVAS_BLOCK = BLOCKS.register(
            "canvas_block",
            () -> new net.minecraft.world.level.block.Block(BlockBehaviour.Properties.of()
                    .strength(0.5f, 0.5f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.WOOL)));

    public static final DeferredHolder<Item, BlockItem> CANVAS_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem("canvas_block", CANVAS_BLOCK);

    // -------------------------------------------------------------------------
    // Lazurite tools
    // -------------------------------------------------------------------------

    public static final DeferredHolder<Item, PickaxeItem> LAZURITE_PICKAXE = ITEMS.register(
            "lazurite_pickaxe",
            () -> new PickaxeItem(
                    LazuriteTier.INSTANCE,
                    new Item.Properties()
                            .attributes(PickaxeItem.createAttributes(LazuriteTier.INSTANCE, 1.0f, -2.8f))));

    public static final DeferredHolder<Item, AxeItem> LAZURITE_AXE = ITEMS.register(
            "lazurite_axe",
            () -> new AxeItem(
                    LazuriteTier.INSTANCE,
                    new Item.Properties().attributes(AxeItem.createAttributes(LazuriteTier.INSTANCE, 4.0f, -3.3f))));

    public static final DeferredHolder<Item, ShovelItem> LAZURITE_SHOVEL = ITEMS.register(
            "lazurite_shovel",
            () -> new ShovelItem(
                    LazuriteTier.INSTANCE,
                    new Item.Properties().attributes(ShovelItem.createAttributes(LazuriteTier.INSTANCE, 1.5f, -3.0f))));

    public static final DeferredHolder<Item, SwordItem> LAZURITE_SWORD = ITEMS.register(
            "lazurite_sword",
            () -> new SwordItem(
                    LazuriteTier.INSTANCE,
                    new Item.Properties().attributes(SwordItem.createAttributes(LazuriteTier.INSTANCE, 3, -2.4f))));

    public static final DeferredHolder<Item, HoeItem> LAZURITE_HOE = ITEMS.register(
            "lazurite_hoe",
            () -> new HoeItem(
                    LazuriteTier.INSTANCE,
                    new Item.Properties().attributes(HoeItem.createAttributes(LazuriteTier.INSTANCE, -2.0f, -1.0f))));

    public static final DeferredHolder<Item, LazuritePaxelItem> LAZURITE_PAXEL =
            ITEMS.register("lazurite_paxel", () -> new LazuritePaxelItem(new Item.Properties()));

    // -------------------------------------------------------------------------
    // Positive Vibes fluid type + fluids + fluid block + cauldron + items
    // -------------------------------------------------------------------------

    public static final DeferredHolder<FluidType, FluidType> HEALING_SALVE_TYPE = FLUID_TYPE_REGISTER.register(
            "positive_vibes",
            () -> new FluidType(
                    FluidType.Properties.create().density(2000).viscosity(6000).temperature(320)));

    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, PositiveVibesFluid.Source>
            HEALING_SALVE_SOURCE = FLUID_REGISTER.register("positive_vibes", PositiveVibesFluid.Source::new);

    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, PositiveVibesFluid.Flowing>
            HEALING_SALVE_FLOWING = FLUID_REGISTER.register("positive_vibes_flowing", PositiveVibesFluid.Flowing::new);

    public static final DeferredBlock<PositiveVibesBlock> HEALING_SALVE_BLOCK = BLOCKS.register(
            "positive_vibes",
            () -> new PositiveVibesBlock(
                    HEALING_SALVE_SOURCE.get(),
                    BlockBehaviour.Properties.of()
                            .noCollission()
                            .strength(100f)
                            .noLootTable()
                            .liquid()
                            .replaceable()
                            .pushReaction(PushReaction.DESTROY)
                            .lightLevel(state -> 8)));

    public static final DeferredHolder<Item, ZombieBrainItem> ZOMBIE_BRAIN =
            ITEMS.register("zombie_brain", ZombieBrainItem::new);

    public static final DeferredHolder<Item, BrainItem> BRAIN = ITEMS.register("brain", BrainItem::new);

    public static final DeferredHolder<Item, Item> CANVAS =
            ITEMS.register("canvas", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, BackpackItem> TREMENDOUS_BACKPACK =
            ITEMS.register("backpack", () -> new BackpackItem(TREMENDOUS_BACKPACK_BLOCK.get()));

    public static final DeferredHolder<Item, EnderBackpackItem> ENDER_TREMENDOUS_BACKPACK_ITEM =
            ITEMS.register("ender_backpack",
                    () -> new EnderBackpackItem(ENDER_TREMENDOUS_BACKPACK_BLOCK.get()));

    public static final DeferredHolder<Item, Item> POSITIVE_VIBES_BOTTLE =
            ITEMS.register("positive_vibes_bottle", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, VexRepellentPotionItem> VEX_REPELLENT_POTION =
            ITEMS.register("vex_repellent_potion", () -> new VexRepellentPotionItem(1 * 60 * 20));

    public static final DeferredHolder<Item, VexRepellentPotionItem> VEX_REPELLENT_POTION_EXTENDED =
            ITEMS.register("vex_repellent_potion_extended", () -> new VexRepellentPotionItem(3 * 60 * 20));

    public static final DeferredHolder<Item, VexRepellentPotionItem> VEX_REPELLENT_POTION_LONG =
            ITEMS.register("vex_repellent_potion_long", () -> new VexRepellentPotionItem(8 * 60 * 20));

    public static final DeferredHolder<Item, BucketItem> HEALING_SALVE_BUCKET = ITEMS.register(
            "positive_vibes_bucket",
            () -> new BucketItem(
                    HEALING_SALVE_SOURCE.get(),
                    new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));

    /** Shared properties object for the Positive Vibes Source + Flowing fluids. */
    public static final BaseFlowingFluid.Properties HEALING_SALVE_FLUID_PROPS = new BaseFlowingFluid.Properties(
                    HEALING_SALVE_TYPE, () -> HEALING_SALVE_SOURCE.get(), () -> HEALING_SALVE_FLOWING.get())
            .bucket(() -> HEALING_SALVE_BUCKET.get())
            .block(() -> HEALING_SALVE_BLOCK.get())
            .slopeFindDistance(2)
            .levelDecreasePerBlock(1)
            .tickRate(30);

    public static final DeferredBlock<PositiveVibesCauldronBlock> HEALING_SALVE_CAULDRON = BLOCKS.register(
            "positive_vibes_cauldron",
            () -> new PositiveVibesCauldronBlock(BlockBehaviour.Properties.of()
                    .strength(2f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredHolder<Item, BlockItem> HEALING_SALVE_CAULDRON_ITEM =
            ITEMS.registerSimpleBlockItem("positive_vibes_cauldron", HEALING_SALVE_CAULDRON);

    // -------------------------------------------------------------------------
    // Network Interface block + block entity
    // -------------------------------------------------------------------------

    public static final DeferredBlock<NetworkInterfaceBlock> NETWORK_INTERFACE = BLOCKS.register(
            "network_interface",
            () -> new NetworkInterfaceBlock(BlockBehaviour.Properties.of()
                    .strength(5f, 1000f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(state -> 8)));

    public static final DeferredHolder<Item, BlockItem> NETWORK_INTERFACE_ITEM = ITEMS.register(
            "network_interface", () -> new TieredBlockItem(NETWORK_INTERFACE.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetworkInterfaceBlockEntity>>
            NETWORK_INTERFACE_BE_TYPE =
                    BLOCK_ENTITY_TYPES.register("network_interface", () -> BlockEntityType.Builder.of(
                                    NetworkInterfaceBlockEntity::new, NETWORK_INTERFACE.get())
                            .build(null));

    // -------------------------------------------------------------------------
    // Storage Access Terminal block
    // -------------------------------------------------------------------------

    public static final DeferredBlock<AccessTerminalBlock> STORAGE_ACCESS_TERMINAL = BLOCKS.register(
            "storage_access_terminal",
            () -> new AccessTerminalBlock(BlockBehaviour.Properties.of()
                    .strength(2.5f, 1000f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final DeferredHolder<Item, BlockItem> STORAGE_ACCESS_TERMINAL_ITEM =
            ITEMS.registerSimpleBlockItem("storage_access_terminal", STORAGE_ACCESS_TERMINAL);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AccessTerminalBlockEntity>>
            STORAGE_ACCESS_TERMINAL_BE_TYPE =
                    BLOCK_ENTITY_TYPES.register("storage_access_terminal", () -> BlockEntityType.Builder.of(
                                    AccessTerminalBlockEntity::new, STORAGE_ACCESS_TERMINAL.get())
                            .build(null));

    // -------------------------------------------------------------------------
    // Wireless Hub block + block entity
    // -------------------------------------------------------------------------

    public static final DeferredBlock<WirelessHubBlock> WIRELESS_HUB = BLOCKS.register(
            "wireless_hub",
            () -> new WirelessHubBlock(BlockBehaviour.Properties.of()
                    .strength(2.5f, 1000f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(state -> 14)));

    public static final DeferredHolder<Item, BlockItem> WIRELESS_HUB_ITEM =
            ITEMS.registerSimpleBlockItem("wireless_hub", WIRELESS_HUB);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WirelessHubBlockEntity>>
            WIRELESS_HUB_BE_TYPE = BLOCK_ENTITY_TYPES.register(
                    "wireless_hub", () -> BlockEntityType.Builder.of(WirelessHubBlockEntity::new, WIRELESS_HUB.get())
                            .build(null));

    // -------------------------------------------------------------------------
    // Wireless SAT item
    // -------------------------------------------------------------------------

    public static final DeferredHolder<Item, PersonalAccessTerminalItem> WIRELESS_SAT =
            ITEMS.register("wireless_sat", PersonalAccessTerminalItem::new);

    // -------------------------------------------------------------------------
    // Tubes
    // -------------------------------------------------------------------------

    public static final DeferredBlock<TubeBlock> TUBE = BLOCKS.register(
            "tube",
            () -> new TubeBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredHolder<Item, BlockItem> TUBE_ITEM = ITEMS.registerSimpleBlockItem("tube", TUBE);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TubeBlockEntity>> TUBE_BE_TYPE =
            BLOCK_ENTITY_TYPES.register("tube", () -> BlockEntityType.Builder.of(TubeBlockEntity::new, TUBE.get())
                    .build(null));

    // -------------------------------------------------------------------------
    // Menu types
    // -------------------------------------------------------------------------

    public static final DeferredHolder<MenuType<?>, MenuType<FilingCabinetMenu>> FILING_CABINET_MENU =
            MENU_TYPES.register("filing_cabinet", () -> IMenuTypeExtension.create(FilingCabinetMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<PriorityControl>> PRIORITY_MENU =
            MENU_TYPES.register("priority", () -> IMenuTypeExtension.create(PriorityControl::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ChestMenu>> TREMENDOUS_CHEST_MENU =
            MENU_TYPES.register("chest", () -> IMenuTypeExtension.create(ChestMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<StorageInterfaceMenu>> STORAGE_INTERFACE_MENU =
            MENU_TYPES.register("storage_interface", () -> IMenuTypeExtension.create(StorageInterfaceMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<NetworkInterfaceMenu>> NETWORK_INTERFACE_MENU =
            MENU_TYPES.register("network_interface", () -> IMenuTypeExtension.create(NetworkInterfaceMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<AccessTerminalMenu>> STORAGE_ACCESS_TERMINAL_MENU =
            MENU_TYPES.register("storage_access_terminal", () -> IMenuTypeExtension.create(AccessTerminalMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<WirelessHubMenu>> WIRELESS_HUB_MENU =
            MENU_TYPES.register("wireless_hub", () -> IMenuTypeExtension.create(WirelessHubMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<TankSettingsMenu>> TANK_SETTINGS_MENU =
            MENU_TYPES.register("tank_settings", () -> IMenuTypeExtension.create(TankSettingsMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ImportInterfaceMenu>> IMPORT_INTERFACE_MENU =
            MENU_TYPES.register("import_interface", () -> IMenuTypeExtension.create(ImportInterfaceMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ExportInterfaceMenu>> EXPORT_INTERFACE_MENU =
            MENU_TYPES.register("export_interface", () -> IMenuTypeExtension.create(ExportInterfaceMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<BackpackMenu>> TREMENDOUS_BACKPACK_MENU =
            MENU_TYPES.register("backpack", () -> IMenuTypeExtension.create(BackpackMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<EnderBackpackMenu>>
            ENDER_TREMENDOUS_BACKPACK_MENU = MENU_TYPES.register(
                    "ender_backpack",
                    () -> IMenuTypeExtension.create(EnderBackpackMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<RecyclingBinMenu>> RECYCLING_BIN_MENU =
            MENU_TYPES.register("recycling_bin", () -> IMenuTypeExtension.create(RecyclingBinMenu::new));

    // -------------------------------------------------------------------------
    // Items — storage interface / import interface / export interface
    // -------------------------------------------------------------------------

    public static final DeferredHolder<Item, StorageInterfaceItem> STORAGE_INTERFACE =
            ITEMS.register("storage_interface", StorageInterfaceItem::new);

    public static final DeferredHolder<Item, ImportInterfaceItem> IMPORT_INTERFACE =
            ITEMS.register("import_interface", ImportInterfaceItem::new);

    public static final DeferredHolder<Item, ExportInterfaceItem> EXPORT_INTERFACE =
            ITEMS.register("export_interface", ExportInterfaceItem::new);

    // -------------------------------------------------------------------------
    // Items — whiteout tape
    // -------------------------------------------------------------------------

    public static final DeferredHolder<Item, WhiteoutTapeItem> WHITEOUT_TAPE =
            ITEMS.register("whiteout_tape", WhiteoutTapeItem::new);

    public static final DeferredHolder<Item, BaseUpgradeItem> BASE_UPGRADE =
            ITEMS.register("base_upgrade", BaseUpgradeItem::new);

    public static final DeferredHolder<Item, BaseUpgradeItem> CRAFTING_UPGRADE =
            ITEMS.register("crafting_upgrade", BaseUpgradeItem::new);

    public static final DeferredHolder<Item, BaseUpgradeItem> ENDER_STORAGE_UPGRADE =
            ITEMS.register("ender_storage_upgrade", BaseUpgradeItem::new);

    public static final DeferredHolder<Item, MagnetUpgradeItem> MAGNET_UPGRADE =
            ITEMS.register("magnet_upgrade", MagnetUpgradeItem::new);

    public static final DeferredHolder<Item, HaarpUpgradeItem> HAARP_UPGRADE =
            ITEMS.register("haarp_upgrade", HaarpUpgradeItem::new);

    public static final DeferredHolder<Item, ExperienceSyringeItem> EXPERIENCE_SYRINGE =
            ITEMS.register("experience_syringe", ExperienceSyringeItem::new);

    // -------------------------------------------------------------------------
    // Items — Manila Folder and Ender Folder (single items; tier in FolderContents)
    // -------------------------------------------------------------------------

    public static final DeferredHolder<Item, ManillaFolderItem> MANILA_FOLDER =
            ITEMS.register("manila_folder", () -> new ManillaFolderItem(new Item.Properties()));

    public static final DeferredHolder<Item, EnderFolderItem> ENDER_FOLDER =
            ITEMS.register("ender_folder", () -> new EnderFolderItem(new Item.Properties()));

    // -------------------------------------------------------------------------
    // Recipe serializers
    // -------------------------------------------------------------------------

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FolderStorageRecipe>>
            FOLDER_STORAGE_RECIPE = RECIPE_SERIALIZERS.register(
                    "folder_storage", () -> new SimpleCraftingRecipeSerializer<>(FolderStorageRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FolderExtractRecipe>>
            FOLDER_EXTRACT_RECIPE = RECIPE_SERIALIZERS.register(
                    "folder_extract", () -> new SimpleCraftingRecipeSerializer<>(FolderExtractRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FolderMergeRecipe>> FOLDER_MERGE_RECIPE =
            RECIPE_SERIALIZERS.register(
                    "folder_merge", () -> new SimpleCraftingRecipeSerializer<>(FolderMergeRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FolderTapeRecipe>> FOLDER_TAPE_RECIPE =
            RECIPE_SERIALIZERS.register(
                    "folder_tape", () -> new SimpleCraftingRecipeSerializer<>(FolderTapeRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<EnderChestSmithingRecipe>>
            ENDER_CHEST_SMITHING_RECIPE =
                    RECIPE_SERIALIZERS.register("ender_chest_smithing", () -> new RecipeSerializer<>() {
                        @Override
                        public com.mojang.serialization.MapCodec<EnderChestSmithingRecipe> codec() {
                            return EnderChestSmithingRecipe.CODEC;
                        }

                        @Override
                        public net.minecraft.network.codec.StreamCodec<
                                        net.minecraft.network.RegistryFriendlyByteBuf, EnderChestSmithingRecipe>
                                streamCodec() {
                            return EnderChestSmithingRecipe.STREAM_CODEC;
                        }
                    });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<EnderBackpackSmithingRecipe>>
            ENDER_BACKPACK_SMITHING_RECIPE =
                    RECIPE_SERIALIZERS.register("ender_backpack_smithing", () -> new RecipeSerializer<>() {
                        @Override
                        public com.mojang.serialization.MapCodec<EnderBackpackSmithingRecipe> codec() {
                            return EnderBackpackSmithingRecipe.CODEC;
                        }

                        @Override
                        public net.minecraft.network.codec.StreamCodec<
                                        net.minecraft.network.RegistryFriendlyByteBuf, EnderBackpackSmithingRecipe>
                                streamCodec() {
                            return EnderBackpackSmithingRecipe.STREAM_CODEC;
                        }
                    });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<EnderFolderSmithingRecipe>>
            ENDER_FOLDER_SMITHING_RECIPE =
                    RECIPE_SERIALIZERS.register("ender_folder_smithing", () -> new RecipeSerializer<>() {
                        @Override
                        public com.mojang.serialization.MapCodec<EnderFolderSmithingRecipe> codec() {
                            return EnderFolderSmithingRecipe.CODEC;
                        }

                        @Override
                        public net.minecraft.network.codec.StreamCodec<
                                        net.minecraft.network.RegistryFriendlyByteBuf, EnderFolderSmithingRecipe>
                                streamCodec() {
                            return EnderFolderSmithingRecipe.STREAM_CODEC;
                        }
                    });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<EnderTankSmithingRecipe>>
            ENDER_TANK_SMITHING_RECIPE =
                    RECIPE_SERIALIZERS.register("ender_tank_smithing", () -> new RecipeSerializer<>() {
                        @Override
                        public com.mojang.serialization.MapCodec<EnderTankSmithingRecipe> codec() {
                            return EnderTankSmithingRecipe.CODEC;
                        }

                        @Override
                        public net.minecraft.network.codec.StreamCodec<
                                        net.minecraft.network.RegistryFriendlyByteBuf, EnderTankSmithingRecipe>
                                streamCodec() {
                            return EnderTankSmithingRecipe.STREAM_CODEC;
                        }
                    });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StorageSmithingUpgradeRecipe>>
            STORAGE_SMITHING_UPGRADE_RECIPE =
                    RECIPE_SERIALIZERS.register("storage_smithing_upgrade", () -> new RecipeSerializer<>() {
                        @Override
                        public com.mojang.serialization.MapCodec<StorageSmithingUpgradeRecipe> codec() {
                            return StorageSmithingUpgradeRecipe.CODEC;
                        }

                        @Override
                        public net.minecraft.network.codec.StreamCodec<
                                        net.minecraft.network.RegistryFriendlyByteBuf, StorageSmithingUpgradeRecipe>
                                streamCodec() {
                            return StorageSmithingUpgradeRecipe.STREAM_CODEC;
                        }
                    });

    // -------------------------------------------------------------------------
    // Creative tab
    // -------------------------------------------------------------------------

    /** Invisible item whose sole purpose is to provide the creative tab icon texture. */
    public static final DeferredHolder<Item, Item> CREATIVE_TAB_ICON =
            ITEMS.register("creative_tab", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TREMENDOUSSTORAGE_TAB =
            CREATIVE_MODE_TABS.register("tremendousstorage", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.tremendousstorage"))
                    .icon(() -> STORAGE_ACCESS_TERMINAL_ITEM.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(LAZURITE_BARS_ITEM.get());
                        output.accept(LAZURITE_ORE_ITEM.get());
                        output.accept(LAZURITE_DEEPSLATE_ORE_ITEM.get());
                        output.accept(RAW_LAZURITE.get());
                        output.accept(LAZURITE_NUGGET.get());
                        output.accept(LAZURITE_INGOT.get());
                        output.accept(LAZURITE_BLOCK_ITEM.get());
                        output.accept(LAZURITE_PICKAXE.get());
                        output.accept(LAZURITE_AXE.get());
                        output.accept(LAZURITE_SHOVEL.get());
                        output.accept(LAZURITE_SWORD.get());
                        output.accept(LAZURITE_HOE.get());
                        output.accept(LAZURITE_PAXEL.get());
                        output.accept(FILING_CABINET_ITEM.get());
                        output.accept(TREMENDOUS_CHEST_ITEM.get());
                        output.accept(ENDER_TREMENDOUS_CHEST_ITEM.get());
                        for (DeferredHolder<Item, StorageUpgradeItem> upgrade : STORAGE_UPGRADES) {
                            output.accept(upgrade.get());
                        }
                        output.accept(TANK_ITEM.get());
                        output.accept(ENDER_TANK_ITEM.get());
                        if (ModList.get().isLoaded("mysticalagriculture")) {
                            var lazurite = MysticalAgricultureAPI.getCropRegistry()
                                    .getCropById(
                                            ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "lazurite"));
                            if (lazurite != null) {
                                output.accept(lazurite.getSeedsItem());
                                output.accept(lazurite.getEssenceItem());
                            }
                        }
                        output.accept(NETWORK_INTERFACE_ITEM.get());
                        output.accept(STORAGE_ACCESS_TERMINAL_ITEM.get());
                        output.accept(WIRELESS_HUB_ITEM.get());
                        output.accept(WIRELESS_SAT.get());
                        output.accept(STORAGE_INTERFACE.get());
                        output.accept(IMPORT_INTERFACE.get());
                        output.accept(EXPORT_INTERFACE.get());
                        output.accept(ZOMBIE_BRAIN.get());
                        output.accept(BRAIN.get());
                        output.accept(CANVAS.get());
                        output.accept(CANVAS_BLOCK_ITEM.get());
                        output.accept(TREMENDOUS_BACKPACK.get());
                        output.accept(ENDER_TREMENDOUS_BACKPACK_ITEM.get());
                        output.accept(HEALING_SALVE_BUCKET.get());
                        output.accept(POSITIVE_VIBES_BOTTLE.get());
                        output.accept(VEX_REPELLENT_POTION.get());
                        output.accept(VEX_REPELLENT_POTION_EXTENDED.get());
                        output.accept(VEX_REPELLENT_POTION_LONG.get());
                        output.accept(TUBE_ITEM.get());
                        output.accept(MANILA_FOLDER.get());
                        output.accept(ENDER_FOLDER.get());
                        output.accept(WHITEOUT_TAPE.get());
                        output.accept(BASE_UPGRADE.get());
                        output.accept(CRAFTING_UPGRADE.get());
                        output.accept(ENDER_STORAGE_UPGRADE.get());
                        output.accept(MAGNET_UPGRADE.get());
                        output.accept(HAARP_UPGRADE.get());
                        output.accept(EXPERIENCE_SYRINGE.get());
                        output.accept(RECYCLING_BIN_ITEM.get());
                    })
                    .build());

    // -------------------------------------------------------------------------
    // Registration helper called from the mod constructor
    // -------------------------------------------------------------------------

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        DATA_COMPONENTS.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        MENU_TYPES.register(modEventBus);
        FLUID_REGISTER.register(modEventBus);
        FLUID_TYPE_REGISTER.register(modEventBus);
        MOB_EFFECTS.register(modEventBus);
        modEventBus.addListener(Registration::registerCapabilities);
        modEventBus.addListener(Registration::onCommonSetup);
        if (ModList.get().isLoaded("exdeorum")) {
            ExDeorumIntegration.register(modEventBus);
        }
        if (ModList.get().isLoaded("exnihilosequentia")) {
            ExNihiloSequentiaIntegration.register(modEventBus);
        }
        LootModifiers.register(modEventBus);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(PositiveVibesInteractions::register);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                FILING_CABINET_BE_TYPE.get(),
                (be, side) -> new FilingCabinetItemHandler(be));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                TREMENDOUS_CHEST_BE_TYPE.get(),
                (be, side) -> new ChestItemHandler(be));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                TREMENDOUS_BACKPACK_BE_TYPE.get(),
                (be, side) -> new ChestItemHandler(be));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ENDER_TREMENDOUS_CHEST_BE_TYPE.get(),
                (be, side) -> new ChestItemHandler(be));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ENDER_TREMENDOUS_BACKPACK_BE_TYPE.get(),
                (be, side) -> new ChestItemHandler(be));
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                TANK_BE_TYPE.get(),
                (be, side) -> new TankFluidHandler(be));
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ENDER_TANK_BE_TYPE.get(),
                (be, side) -> new TankFluidHandler(be));
        event.registerItem(
                Capabilities.FluidHandler.ITEM,
                (stack, ctx) -> new TankItemFluidHandler(stack),
                TANK_ITEM.get());
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK, TUBE_BE_TYPE.get(), (be, side) -> be.getNetworkView());
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK, NETWORK_INTERFACE_BE_TYPE.get(), (be, side) -> be.getItemHandler());
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                RECYCLING_BIN_BE_TYPE.get(),
                (be, side) -> new RecyclingBinItemHandler(be));
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                RECYCLING_BIN_BE_TYPE.get(),
                (be, side) -> new RecyclingBinFluidHandler(be));
    }
}
