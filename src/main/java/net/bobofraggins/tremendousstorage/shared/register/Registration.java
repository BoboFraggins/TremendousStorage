package net.bobofraggins.tremendousstorage.shared.register;

import com.blakebr0.mysticalagriculture.api.MysticalAgricultureAPI;
import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.experiencesyringe.ExperienceSyringeItem;
import net.bobofraggins.tremendousstorage.external.exdeorum.ExDeorumIntegration;
import net.bobofraggins.tremendousstorage.external.exnihilosequentia.ExNihiloSequentiaIntegration;
import net.bobofraggins.tremendousstorage.glamping.GlampingRegistration;
import net.bobofraggins.tremendousstorage.glamping.dankfannypack.DankFannyPackItem;
import net.bobofraggins.tremendousstorage.glamping.dankfannypack.DankFannyPackMenu;
import net.bobofraggins.tremendousstorage.glamping.dankfannypack.FannyPackContents;
import net.bobofraggins.tremendousstorage.glamping.magichat.MagicHatBlock;
import net.bobofraggins.tremendousstorage.glamping.magichat.MagicHatItem;
import net.bobofraggins.tremendousstorage.glamping.picnicbasket.EnderPicnicBasketBlock;
import net.bobofraggins.tremendousstorage.glamping.picnicbasket.EnderPicnicBasketBlockEntity;
import net.bobofraggins.tremendousstorage.glamping.picnicbasket.EnderPicnicBasketCraftingRecipe;
import net.bobofraggins.tremendousstorage.guidebook.TremendousStorageGuideItem;
import net.bobofraggins.tremendousstorage.lazurite.LazuriteBarsBlock;
import net.bobofraggins.tremendousstorage.lazurite.LazuriteOreBlock;
import net.bobofraggins.tremendousstorage.lazurite.LazuritePaxelItem;
import net.bobofraggins.tremendousstorage.lazurite.LazuriteRepairRecipe;
import net.bobofraggins.tremendousstorage.lazurite.LazuriteTier;
import net.bobofraggins.tremendousstorage.power.stirlingengine.StirlingEngineBlock;
import net.bobofraggins.tremendousstorage.power.stirlingengine.StirlingEngineBlockEntity;
import net.bobofraggins.tremendousstorage.power.stirlingengine.StirlingEngineEnergyHandler;
import net.bobofraggins.tremendousstorage.shared.loot.LootModifiers;
import net.bobofraggins.tremendousstorage.shared.storage.EnderTieredBlockItem;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.shared.storage.TieredBlockItem;
import net.bobofraggins.tremendousstorage.shared.ui.PriorityControl;
import net.bobofraggins.tremendousstorage.shared.ui.TankSettingsMenu;
import net.bobofraggins.tremendousstorage.storage.accessterminal.AccessTerminalBlock;
import net.bobofraggins.tremendousstorage.storage.accessterminal.AccessTerminalBlockEntity;
import net.bobofraggins.tremendousstorage.storage.accessterminal.AccessTerminalMenu;
import net.bobofraggins.tremendousstorage.storage.armorycabinet.ArmoryCabinetBlock;
import net.bobofraggins.tremendousstorage.storage.armorycabinet.ArmoryCabinetBlockEntity;
import net.bobofraggins.tremendousstorage.storage.armorycabinet.ArmoryCabinetMenu;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackBlock;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackBlockEntity;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackContents;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackItem;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackMenu;
import net.bobofraggins.tremendousstorage.storage.barrel.BarrelBlock;
import net.bobofraggins.tremendousstorage.storage.barrel.BarrelBlockEntity;
import net.bobofraggins.tremendousstorage.storage.barrel.BarrelContents;
import net.bobofraggins.tremendousstorage.storage.barrel.BarrelMenu;
import net.bobofraggins.tremendousstorage.storage.baseupgrade.BaseUpgradeItem;
import net.bobofraggins.tremendousstorage.storage.baseupgrade.CraftingUpgradeItem;
import net.bobofraggins.tremendousstorage.storage.baseupgrade.EnderStorageUpgradeItem;
import net.bobofraggins.tremendousstorage.storage.baseupgrade.HaarpUpgradeItem;
import net.bobofraggins.tremendousstorage.storage.baseupgrade.InterdimensionalUpgradeItem;
import net.bobofraggins.tremendousstorage.storage.baseupgrade.MagnetUpgradeItem;
import net.bobofraggins.tremendousstorage.storage.baseupgrade.PullerUpgradeItem;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlock;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlockEntity;
import net.bobofraggins.tremendousstorage.storage.chest.ChestItemHandler;
import net.bobofraggins.tremendousstorage.storage.chest.ChestMenu;
import net.bobofraggins.tremendousstorage.storage.enderbackpack.EnderBackpackBlock;
import net.bobofraggins.tremendousstorage.storage.enderbackpack.EnderBackpackBlockEntity;
import net.bobofraggins.tremendousstorage.storage.enderbackpack.EnderBackpackCraftingRecipe;
import net.bobofraggins.tremendousstorage.storage.enderbackpack.EnderBackpackItem;
import net.bobofraggins.tremendousstorage.storage.enderbackpack.EnderBackpackMenu;
import net.bobofraggins.tremendousstorage.storage.enderchest.EnderChestBlock;
import net.bobofraggins.tremendousstorage.storage.enderchest.EnderChestBlockEntity;
import net.bobofraggins.tremendousstorage.storage.enderchest.EnderChestCraftingRecipe;
import net.bobofraggins.tremendousstorage.storage.enderfolder.EnderFolderCraftingRecipe;
import net.bobofraggins.tremendousstorage.storage.enderfolder.EnderFolderItem;
import net.bobofraggins.tremendousstorage.storage.endertank.EnderTankBlock;
import net.bobofraggins.tremendousstorage.storage.endertank.EnderTankBlockEntity;
import net.bobofraggins.tremendousstorage.storage.endertank.EnderTankCraftingRecipe;
import net.bobofraggins.tremendousstorage.storage.endertank.EnderTankItem;
import net.bobofraggins.tremendousstorage.storage.filingcabinet.FilingCabinetBlock;
import net.bobofraggins.tremendousstorage.storage.filingcabinet.FilingCabinetBlockEntity;
import net.bobofraggins.tremendousstorage.storage.filingcabinet.FilingCabinetItemHandler;
import net.bobofraggins.tremendousstorage.storage.filingcabinet.FilingCabinetMenu;
import net.bobofraggins.tremendousstorage.storage.honey.HoneyBlock;
import net.bobofraggins.tremendousstorage.storage.honey.HoneyFluid;
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
import net.bobofraggins.tremendousstorage.storage.networkinterface.NiEnergyHandler;
import net.bobofraggins.tremendousstorage.storage.personalaccessterminal.PersonalAccessTerminalItem;
import net.bobofraggins.tremendousstorage.storage.recyclingbin.RecyclingBinBlock;
import net.bobofraggins.tremendousstorage.storage.recyclingbin.RecyclingBinBlockEntity;
import net.bobofraggins.tremendousstorage.storage.recyclingbin.RecyclingBinFluidHandler;
import net.bobofraggins.tremendousstorage.storage.recyclingbin.RecyclingBinItemHandler;
import net.bobofraggins.tremendousstorage.storage.recyclingbin.RecyclingBinMenu;
import net.bobofraggins.tremendousstorage.storage.storageupgrade.StorageUpgradeCraftingRecipe;
import net.bobofraggins.tremendousstorage.storage.storageupgrade.StorageUpgradeItem;
import net.bobofraggins.tremendousstorage.storage.tank.TankBlock;
import net.bobofraggins.tremendousstorage.storage.tank.TankBlockEntity;
import net.bobofraggins.tremendousstorage.storage.tank.TankContents;
import net.bobofraggins.tremendousstorage.storage.tank.TankFluidHandler;
import net.bobofraggins.tremendousstorage.storage.tank.TankItem;
import net.bobofraggins.tremendousstorage.storage.tank.TankItemFluidHandler;
import net.bobofraggins.tremendousstorage.storage.tube.TubeBlock;
import net.bobofraggins.tremendousstorage.storage.tube.TubeBlockEntity;
import net.bobofraggins.tremendousstorage.storage.tube.TubeEnergyHandler;
import net.bobofraggins.tremendousstorage.storage.tubeattachments.ExportInterfaceItem;
import net.bobofraggins.tremendousstorage.storage.tubeattachments.ExportInterfaceMenu;
import net.bobofraggins.tremendousstorage.storage.tubeattachments.ImportInterfaceItem;
import net.bobofraggins.tremendousstorage.storage.tubeattachments.ImportInterfaceMenu;
import net.bobofraggins.tremendousstorage.storage.tubeattachments.InterfaceFilterContents;
import net.bobofraggins.tremendousstorage.storage.tubeattachments.StorageInterfaceItem;
import net.bobofraggins.tremendousstorage.storage.tubeattachments.StorageInterfaceMenu;
import net.bobofraggins.tremendousstorage.storage.whiteout.FolderTapeRecipe;
import net.bobofraggins.tremendousstorage.storage.whiteout.WhiteoutTapeItem;
import net.bobofraggins.tremendousstorage.storage.wirelesshub.WirelessHubBlock;
import net.bobofraggins.tremendousstorage.storage.wirelesshub.WirelessHubBlockEntity;
import net.bobofraggins.tremendousstorage.storage.wirelesshub.WirelessHubMenu;
import net.bobofraggins.tremendousstorage.storage.xpjuice.XpJuiceFluid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.food.FoodProperties;
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

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<TankContents>> TANK_CONTENTS =
            DATA_COMPONENTS.register("tank_contents", () -> DataComponentType.<TankContents>builder()
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
     * Data component storing the dimension {@link net.minecraft.resources.ResourceLocation} of the
     * Wireless Hub that linked a Wireless SAT. Used to look up the hub/NI in the correct server
     * level when the player is in a different dimension.
     */
    public static final DeferredHolder<
                    DataComponentType<?>, DataComponentType<net.minecraft.resources.ResourceLocation>>
            WIRELESS_HUB_DIMENSION = DATA_COMPONENTS.register(
                    "wireless_hub_dimension",
                    () -> DataComponentType.<net.minecraft.resources.ResourceLocation>builder()
                            .persistent(net.minecraft.resources.ResourceLocation.CODEC)
                            .networkSynchronized(net.minecraft.resources.ResourceLocation.STREAM_CODEC)
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
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> EXPERIENCE_SYRINGE_STORED_XP =
            DATA_COMPONENTS.register("experience_syringe_stored_xp", () -> DataComponentType.<Integer>builder()
                    .persistent(com.mojang.serialization.Codec.intRange(0, ExperienceSyringeItem.CAPACITY))
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT)
                    .build());

    /** Data component storing the 8 folder slots and settings on a Dank Fanny Pack item. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FannyPackContents>> FANNY_PACK_CONTENTS =
            DATA_COMPONENTS.register("fanny_pack_contents", () -> DataComponentType.<FannyPackContents>builder()
                    .persistent(FannyPackContents.CODEC)
                    .networkSynchronized(FannyPackContents.STREAM_CODEC)
                    .build());

    /** Data component storing the locked item type and quantity on a Barrel block item. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BarrelContents>> BARREL_CONTENTS =
            DATA_COMPONENTS.register("barrel_contents", () -> DataComponentType.<BarrelContents>builder()
                    .persistent(BarrelContents.CODEC)
                    .networkSynchronized(BarrelContents.STREAM_CODEC)
                    .build());

    /** Data component storing all inventory and settings on a Tremendous Backpack item. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BackpackContents>>
            TREMENDOUS_BACKPACK_CONTENTS =
                    DATA_COMPONENTS.register("backpack_contents", () -> DataComponentType.<BackpackContents>builder()
                            .persistent(BackpackContents.CODEC)
                            .networkSynchronized(BackpackContents.STREAM_CODEC)
                            .build());

    // -------------------------------------------------------------------------
    // Blocks + block entities
    // -------------------------------------------------------------------------

    public static final DeferredBlock<ArmoryCabinetBlock> ARMORY_CABINET = BLOCKS.register(
            "armory_cabinet",
            () -> new ArmoryCabinetBlock(BlockBehaviour.Properties.of()
                    .strength(5.0f, 1000.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredHolder<Item, BlockItem> ARMORY_CABINET_ITEM =
            ITEMS.register("armory_cabinet", () -> new BlockItem(ARMORY_CABINET.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArmoryCabinetBlockEntity>>
            ARMORY_CABINET_BE_TYPE = BLOCK_ENTITY_TYPES.register("armory_cabinet", () -> BlockEntityType.Builder.of(
                    ArmoryCabinetBlockEntity::new, ARMORY_CABINET.get())
            .build(null));

    public static final DeferredBlock<FilingCabinetBlock> FILING_CABINET = BLOCKS.register(
            "filing_cabinet",
            () -> new FilingCabinetBlock(BlockBehaviour.Properties.of()
                    .strength(5.0f, 1000.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.WOOD)));

    public static final DeferredHolder<Item, BlockItem> FILING_CABINET_ITEM =
            ITEMS.register("filing_cabinet", () -> new TieredBlockItem(FILING_CABINET.get(), new Item.Properties()));

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

    public static final DeferredHolder<Item, BlockItem> TREMENDOUS_CHEST_ITEM =
            ITEMS.register("chest", () -> new TieredBlockItem(TREMENDOUS_CHEST.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChestBlockEntity>> TREMENDOUS_CHEST_BE_TYPE =
            BLOCK_ENTITY_TYPES.register(
                    "chest", () -> BlockEntityType.Builder.of(ChestBlockEntity::new, TREMENDOUS_CHEST.get())
                            .build(null));

    public static final DeferredBlock<EnderChestBlock> ENDER_TREMENDOUS_CHEST = BLOCKS.register(
            "ender_chest",
            () -> new EnderChestBlock(BlockBehaviour.Properties.of()
                    .strength(5.0f, 1000.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredHolder<Item, BlockItem> ENDER_TREMENDOUS_CHEST_ITEM = ITEMS.register(
            "ender_chest", () -> new EnderTieredBlockItem(ENDER_TREMENDOUS_CHEST.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnderChestBlockEntity>>
            ENDER_TREMENDOUS_CHEST_BE_TYPE =
                    BLOCK_ENTITY_TYPES.register("ender_chest", () -> BlockEntityType.Builder.of(
                                    EnderChestBlockEntity::new, ENDER_TREMENDOUS_CHEST.get())
                            .build(null));

    public static final DeferredBlock<BackpackBlock> TREMENDOUS_BACKPACK_BLOCK = BLOCKS.register(
            "backpack",
            () -> new BackpackBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f, 1000.0f)
                    .sound(SoundType.WOOL)
                    .noOcclusion()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BackpackBlockEntity>>
            TREMENDOUS_BACKPACK_BE_TYPE = BLOCK_ENTITY_TYPES.register("backpack", () -> BlockEntityType.Builder.of(
                    BackpackBlockEntity::new, TREMENDOUS_BACKPACK_BLOCK.get())
            .build(null));

    public static final DeferredBlock<EnderBackpackBlock> ENDER_TREMENDOUS_BACKPACK_BLOCK = BLOCKS.register(
            "ender_backpack",
            () -> new EnderBackpackBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f, 1000.0f)
                    .sound(SoundType.WOOL)
                    .noOcclusion()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnderBackpackBlockEntity>>
            ENDER_TREMENDOUS_BACKPACK_BE_TYPE =
                    BLOCK_ENTITY_TYPES.register("ender_backpack", () -> BlockEntityType.Builder.of(
                                    EnderBackpackBlockEntity::new, ENDER_TREMENDOUS_BACKPACK_BLOCK.get())
                            .build(null));

    public static final DeferredBlock<net.bobofraggins.tremendousstorage.glamping.picnicbasket.PicnicBasketBlock>
            PICNIC_BASKET_BLOCK = BLOCKS.register(
                    "picnic_basket",
                    () -> new net.bobofraggins.tremendousstorage.glamping.picnicbasket.PicnicBasketBlock(
                            BlockBehaviour.Properties.of()
                                    .strength(1.0f, 10.0f)
                                    .sound(SoundType.WOOD)
                                    .noOcclusion()));

    public static final DeferredHolder<
                    BlockEntityType<?>,
                    BlockEntityType<net.bobofraggins.tremendousstorage.glamping.picnicbasket.PicnicBasketBlockEntity>>
            PICNIC_BASKET_BE_TYPE = BLOCK_ENTITY_TYPES.register("picnic_basket", () -> BlockEntityType.Builder.of(
                    net.bobofraggins.tremendousstorage.glamping.picnicbasket.PicnicBasketBlockEntity::new,
                    PICNIC_BASKET_BLOCK.get())
            .build(null));

    // -------------------------------------------------------------------------
    // Barrel block + block entity
    // -------------------------------------------------------------------------

    public static final DeferredBlock<BarrelBlock> BARREL = BLOCKS.register(
            "barrel",
            () -> new BarrelBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f, 1000.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final DeferredHolder<Item, BlockItem> BARREL_ITEM =
            ITEMS.register("barrel", () -> new TieredBlockItem(BARREL.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BarrelBlockEntity>> BARREL_BE_TYPE =
            BLOCK_ENTITY_TYPES.register("barrel", () -> BlockEntityType.Builder.of(BarrelBlockEntity::new, BARREL.get())
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
                    .sound(SoundType.GLASS)
                    .noOcclusion()));

    public static final DeferredHolder<Item, BlockItem> TANK_ITEM =
            ITEMS.register("tank", () -> new TankItem(TANK.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TankBlockEntity>> TANK_BE_TYPE =
            BLOCK_ENTITY_TYPES.register("tank", () -> BlockEntityType.Builder.of(TankBlockEntity::new, TANK.get())
                    .build(null));

    public static final DeferredBlock<EnderTankBlock> ENDER_TANK = BLOCKS.register(
            "ender_tank",
            () -> new EnderTankBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f, 1000.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.GLASS)
                    .noOcclusion()));

    public static final DeferredHolder<Item, BlockItem> ENDER_TANK_ITEM =
            ITEMS.register("ender_tank", () -> new EnderTankItem(ENDER_TANK.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnderTankBlockEntity>> ENDER_TANK_BE_TYPE =
            BLOCK_ENTITY_TYPES.register(
                    "ender_tank", () -> BlockEntityType.Builder.of(EnderTankBlockEntity::new, ENDER_TANK.get())
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
            RECYCLING_BIN_BE_TYPE = BLOCK_ENTITY_TYPES.register(
                    "recycling_bin", () -> BlockEntityType.Builder.of(RecyclingBinBlockEntity::new, RECYCLING_BIN.get())
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
    // Honey fluid type + fluids + fluid block + bucket
    // -------------------------------------------------------------------------

    public static final DeferredHolder<FluidType, FluidType> HONEY_TYPE = FLUID_TYPE_REGISTER.register(
            "honey",
            () -> new FluidType(
                    FluidType.Properties.create().density(1400).viscosity(4000).temperature(300)));

    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, HoneyFluid.Source> HONEY_SOURCE =
            FLUID_REGISTER.register("honey", HoneyFluid.Source::new);

    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, HoneyFluid.Flowing> HONEY_FLOWING =
            FLUID_REGISTER.register("honey_flowing", HoneyFluid.Flowing::new);

    public static final DeferredBlock<HoneyBlock> HONEY_FLUID_BLOCK = BLOCKS.register(
            "honey_fluid",
            () -> new HoneyBlock(
                    HONEY_SOURCE.get(),
                    BlockBehaviour.Properties.of()
                            .noCollission()
                            .strength(100f)
                            .noLootTable()
                            .liquid()
                            .replaceable()
                            .pushReaction(PushReaction.DESTROY)));

    public static final DeferredHolder<Item, BucketItem> HONEY_FLUID_BUCKET = ITEMS.register(
            "honey_fluid_bucket",
            () -> new BucketItem(
                    HONEY_SOURCE.get(), new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));

    /** Shared properties object for the Honey Source + Flowing fluids. */
    public static final BaseFlowingFluid.Properties HONEY_FLUID_PROPS = new BaseFlowingFluid.Properties(
                    HONEY_TYPE, () -> HONEY_SOURCE.get(), () -> HONEY_FLOWING.get())
            .bucket(() -> HONEY_FLUID_BUCKET.get())
            .block(() -> HONEY_FLUID_BLOCK.get())
            .slopeFindDistance(2)
            .levelDecreasePerBlock(2)
            .tickRate(40);

    // -------------------------------------------------------------------------
    // XP Juice fluid type + fluids + fluid block + bucket
    // -------------------------------------------------------------------------

    public static final DeferredHolder<FluidType, FluidType> XP_JUICE_TYPE = FLUID_TYPE_REGISTER.register(
            "xp_juice",
            () -> new FluidType(
                    FluidType.Properties.create().density(900).viscosity(1500).temperature(300)));

    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, XpJuiceFluid.Source> XP_JUICE_SOURCE =
            FLUID_REGISTER.register("xp_juice", XpJuiceFluid.Source::new);

    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, XpJuiceFluid.Flowing>
            XP_JUICE_FLOWING = FLUID_REGISTER.register("xp_juice_flowing", XpJuiceFluid.Flowing::new);

    public static final DeferredBlock<net.minecraft.world.level.block.LiquidBlock> XP_JUICE_BLOCK = BLOCKS.register(
            "xp_juice",
            () -> new net.minecraft.world.level.block.LiquidBlock(
                    XP_JUICE_SOURCE.get(),
                    BlockBehaviour.Properties.of()
                            .noCollission()
                            .strength(100f)
                            .noLootTable()
                            .liquid()
                            .replaceable()
                            .pushReaction(PushReaction.DESTROY)));

    public static final DeferredHolder<Item, BucketItem> XP_JUICE_BUCKET = ITEMS.register(
            "xp_juice_bucket",
            () -> new BucketItem(
                    XP_JUICE_SOURCE.get(), new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));

    public static final BaseFlowingFluid.Properties XP_JUICE_FLUID_PROPS = new BaseFlowingFluid.Properties(
                    XP_JUICE_TYPE, () -> XP_JUICE_SOURCE.get(), () -> XP_JUICE_FLOWING.get())
            .bucket(() -> XP_JUICE_BUCKET.get())
            .block(() -> XP_JUICE_BLOCK.get())
            .slopeFindDistance(2)
            .levelDecreasePerBlock(1)
            .tickRate(20);

    // -------------------------------------------------------------------------
    // Positive Vibes fluid type + fluids + fluid block + cauldron + items
    // -------------------------------------------------------------------------

    public static final DeferredHolder<FluidType, FluidType> POSITIVE_VIBES_TYPE = FLUID_TYPE_REGISTER.register(
            "positive_vibes",
            () -> new FluidType(
                    FluidType.Properties.create().density(2000).viscosity(6000).temperature(320)));

    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, PositiveVibesFluid.Source>
            POSITIVE_VIBES_SOURCE = FLUID_REGISTER.register("positive_vibes", PositiveVibesFluid.Source::new);

    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, PositiveVibesFluid.Flowing>
            POSITIVE_VIBES_FLOWING = FLUID_REGISTER.register("positive_vibes_flowing", PositiveVibesFluid.Flowing::new);

    public static final DeferredBlock<PositiveVibesBlock> POSITIVE_VIBES_BLOCK = BLOCKS.register(
            "positive_vibes",
            () -> new PositiveVibesBlock(
                    POSITIVE_VIBES_SOURCE.get(),
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

    private static final FoodProperties SNACK_FOOD =
            new FoodProperties.Builder().nutrition(2).saturationModifier(0.25f).build();

    public static final DeferredHolder<Item, Item> GRAHAM_CRACKER =
            ITEMS.register("graham_cracker", () -> new Item(new Item.Properties().food(SNACK_FOOD)));

    public static final DeferredHolder<Item, Item> CHOCOLATE_BAR =
            ITEMS.register("chocolate_bar", () -> new Item(new Item.Properties().food(SNACK_FOOD)));

    public static final DeferredHolder<Item, Item> MARSHMALLOW =
            ITEMS.register("marshmallow", () -> new Item(new Item.Properties().food(SNACK_FOOD)));

    public static final DeferredHolder<Item, Item> TOASTED_MARSHMALLOW =
            ITEMS.register("toasted_marshmallow", () -> new Item(new Item.Properties().food(SNACK_FOOD)));

    public static final DeferredHolder<Item, Item> SMORE = ITEMS.register(
            "smore",
            () -> new Item(new Item.Properties()
                    .food(new FoodProperties.Builder()
                            .nutrition(10)
                            .saturationModifier(0.25f)
                            .build())));

    public static final DeferredHolder<Item, Item> CANVAS =
            ITEMS.register("canvas", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, BlockItem> PICNIC_BASKET_ITEM = ITEMS.register(
            "picnic_basket",
            () -> new net.bobofraggins.tremendousstorage.glamping.picnicbasket.PicnicBasketBlockItem(
                    PICNIC_BASKET_BLOCK.get(), "item.tremendousstorage.picnic_basket.tooltip"));

    public static final DeferredBlock<EnderPicnicBasketBlock> ENDER_PICNIC_BASKET_BLOCK = BLOCKS.register(
            "ender_picnic_basket",
            () -> new EnderPicnicBasketBlock(BlockBehaviour.Properties.of()
                    .strength(1.0f, 10.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final DeferredHolder<Item, BlockItem> ENDER_PICNIC_BASKET_ITEM = ITEMS.register(
            "ender_picnic_basket",
            () -> new net.bobofraggins.tremendousstorage.glamping.picnicbasket.PicnicBasketBlockItem(
                    ENDER_PICNIC_BASKET_BLOCK.get(), "item.tremendousstorage.ender_picnic_basket.tooltip"));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnderPicnicBasketBlockEntity>>
            ENDER_PICNIC_BASKET_BE_TYPE =
                    BLOCK_ENTITY_TYPES.register("ender_picnic_basket", () -> BlockEntityType.Builder.of(
                                    EnderPicnicBasketBlockEntity::new, ENDER_PICNIC_BASKET_BLOCK.get())
                            .build(null));

    // -------------------------------------------------------------------------
    // Dank Fanny Pack
    // -------------------------------------------------------------------------

    public static final DeferredHolder<Item, DankFannyPackItem> DANK_FANNY_PACK =
            ITEMS.register("dank_fanny_pack", DankFannyPackItem::new);

    // -------------------------------------------------------------------------
    // Magic Hat
    // -------------------------------------------------------------------------

    public static final DeferredBlock<MagicHatBlock> MAGIC_HAT_BLOCK = BLOCKS.register(
            "magic_hat",
            () -> new MagicHatBlock(
                    BlockBehaviour.Properties.of().strength(0.5f).noOcclusion().sound(SoundType.WOOL)));

    public static final DeferredHolder<Item, MagicHatItem> MAGIC_HAT_ITEM =
            ITEMS.register("magic_hat", () -> new MagicHatItem(MAGIC_HAT_BLOCK.get()));

    public static DeferredHolder<Item, TremendousStorageGuideItem> TREMENDOUS_STORAGE_GUIDE = null;

    public static final DeferredHolder<Item, BackpackItem> TREMENDOUS_BACKPACK =
            ITEMS.register("backpack", () -> new BackpackItem(TREMENDOUS_BACKPACK_BLOCK.get()));

    public static final DeferredHolder<Item, EnderBackpackItem> ENDER_TREMENDOUS_BACKPACK_ITEM =
            ITEMS.register("ender_backpack", () -> new EnderBackpackItem(ENDER_TREMENDOUS_BACKPACK_BLOCK.get()));

    public static final DeferredHolder<Item, Item> POSITIVE_VIBES_BOTTLE =
            ITEMS.register("positive_vibes_bottle", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, VexRepellentPotionItem> VEX_REPELLENT_POTION =
            ITEMS.register("vex_repellent_potion", () -> new VexRepellentPotionItem(1 * 60 * 20));

    public static final DeferredHolder<Item, VexRepellentPotionItem> VEX_REPELLENT_POTION_EXTENDED =
            ITEMS.register("vex_repellent_potion_extended", () -> new VexRepellentPotionItem(3 * 60 * 20));

    public static final DeferredHolder<Item, VexRepellentPotionItem> VEX_REPELLENT_POTION_LONG =
            ITEMS.register("vex_repellent_potion_long", () -> new VexRepellentPotionItem(8 * 60 * 20));

    public static final DeferredHolder<Item, BucketItem> POSITIVE_VIBES_BUCKET = ITEMS.register(
            "positive_vibes_bucket",
            () -> new BucketItem(
                    POSITIVE_VIBES_SOURCE.get(),
                    new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));

    /** Shared properties object for the Positive Vibes Source + Flowing fluids. */
    public static final BaseFlowingFluid.Properties POSITIVE_VIBES_FLUID_PROPS = new BaseFlowingFluid.Properties(
                    POSITIVE_VIBES_TYPE, () -> POSITIVE_VIBES_SOURCE.get(), () -> POSITIVE_VIBES_FLOWING.get())
            .bucket(() -> POSITIVE_VIBES_BUCKET.get())
            .block(() -> POSITIVE_VIBES_BLOCK.get())
            .slopeFindDistance(2)
            .levelDecreasePerBlock(1)
            .tickRate(30);

    public static final DeferredBlock<PositiveVibesCauldronBlock> POSITIVE_VIBES_CAULDRON = BLOCKS.register(
            "positive_vibes_cauldron",
            () -> new PositiveVibesCauldronBlock(BlockBehaviour.Properties.of()
                    .strength(2f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredHolder<Item, BlockItem> POSITIVE_VIBES_CAULDRON_ITEM =
            ITEMS.registerSimpleBlockItem("positive_vibes_cauldron", POSITIVE_VIBES_CAULDRON);

    // -------------------------------------------------------------------------
    // Stirling Engine block + block entity
    // -------------------------------------------------------------------------

    public static final DeferredBlock<StirlingEngineBlock> STIRLING_ENGINE = BLOCKS.register(
            "stirling_engine",
            () -> new StirlingEngineBlock(BlockBehaviour.Properties.of()
                    .strength(3f, 1000f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(state -> 7)));

    public static final DeferredHolder<Item, BlockItem> STIRLING_ENGINE_ITEM = ITEMS.register(
            "stirling_engine", () -> new TieredBlockItem(STIRLING_ENGINE.get(), new Item.Properties().fireResistant()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StirlingEngineBlockEntity>>
            STIRLING_ENGINE_BE_TYPE = BLOCK_ENTITY_TYPES.register("stirling_engine", () -> BlockEntityType.Builder.of(
                    StirlingEngineBlockEntity::new, STIRLING_ENGINE.get())
            .build(null));

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

    public static final DeferredHolder<Item, BlockItem> STORAGE_ACCESS_TERMINAL_ITEM = ITEMS.register(
            "storage_access_terminal", () -> new TieredBlockItem(STORAGE_ACCESS_TERMINAL.get(), new Item.Properties()));

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
            ITEMS.register("wireless_hub", () -> new TieredBlockItem(WIRELESS_HUB.get(), new Item.Properties()));

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

    public static final DeferredHolder<MenuType<?>, MenuType<BarrelMenu>> BARREL_MENU =
            MENU_TYPES.register("barrel", () -> IMenuTypeExtension.create(BarrelMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ArmoryCabinetMenu>> ARMORY_CABINET_MENU =
            MENU_TYPES.register("armory_cabinet", () -> IMenuTypeExtension.create(ArmoryCabinetMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<FilingCabinetMenu>> FILING_CABINET_MENU =
            MENU_TYPES.register("filing_cabinet", () -> IMenuTypeExtension.create(FilingCabinetMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<DankFannyPackMenu>> DANK_FANNY_PACK_MENU =
            MENU_TYPES.register("dank_fanny_pack", () -> IMenuTypeExtension.create(DankFannyPackMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<PriorityControl>> PRIORITY_MENU =
            MENU_TYPES.register("priority", () -> IMenuTypeExtension.create(PriorityControl::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ChestMenu>> TREMENDOUS_CHEST_MENU =
            MENU_TYPES.register("chest", () -> IMenuTypeExtension.create(ChestMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<StorageInterfaceMenu>> STORAGE_INTERFACE_MENU =
            MENU_TYPES.register("storage_interface", () -> IMenuTypeExtension.create(StorageInterfaceMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<NetworkInterfaceMenu>> NETWORK_INTERFACE_MENU =
            MENU_TYPES.register("network_interface", () -> IMenuTypeExtension.create(NetworkInterfaceMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<AccessTerminalMenu>> STORAGE_ACCESS_TERMINAL_MENU =
            MENU_TYPES.register(
                    "storage_access_terminal", () -> IMenuTypeExtension.create(AccessTerminalMenu::fromNetwork));

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

    public static final DeferredHolder<MenuType<?>, MenuType<EnderBackpackMenu>> ENDER_TREMENDOUS_BACKPACK_MENU =
            MENU_TYPES.register("ender_backpack", () -> IMenuTypeExtension.create(EnderBackpackMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<RecyclingBinMenu>> RECYCLING_BIN_MENU =
            MENU_TYPES.register("recycling_bin", () -> IMenuTypeExtension.create(RecyclingBinMenu::new));

    public static final DeferredHolder<
                    MenuType<?>,
                    MenuType<net.bobofraggins.tremendousstorage.glamping.picnicbasket.PicnicBasketItemMenu>>
            PICNIC_BASKET_ITEM_MENU = MENU_TYPES.register(
                    "picnic_basket_item",
                    () -> IMenuTypeExtension.create(
                            net.bobofraggins.tremendousstorage.glamping.picnicbasket.PicnicBasketItemMenu::new));

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

    public static final DeferredHolder<Item, CraftingUpgradeItem> CRAFTING_UPGRADE =
            ITEMS.register("crafting_upgrade", CraftingUpgradeItem::new);

    public static final DeferredHolder<Item, EnderStorageUpgradeItem> ENDER_STORAGE_UPGRADE =
            ITEMS.register("ender_storage_upgrade", EnderStorageUpgradeItem::new);

    public static final DeferredHolder<Item, MagnetUpgradeItem> MAGNET_UPGRADE =
            ITEMS.register("magnet_upgrade", MagnetUpgradeItem::new);

    public static final DeferredHolder<Item, HaarpUpgradeItem> HAARP_UPGRADE =
            ITEMS.register("haarp_upgrade", HaarpUpgradeItem::new);

    public static final DeferredHolder<Item, PullerUpgradeItem> PULLER_UPGRADE =
            ITEMS.register("puller_upgrade", PullerUpgradeItem::new);

    public static final DeferredHolder<Item, InterdimensionalUpgradeItem> INTERDIMENSIONAL_UPGRADE =
            ITEMS.register("interdimensional_upgrade", InterdimensionalUpgradeItem::new);

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

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<LazuriteRepairRecipe>>
            LAZURITE_REPAIR_RECIPE = RECIPE_SERIALIZERS.register(
                    "lazurite_repair", () -> new SimpleCraftingRecipeSerializer<>(LazuriteRepairRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<EnderChestCraftingRecipe>>
            ENDER_CHEST_CRAFTING_RECIPE =
                    RECIPE_SERIALIZERS.register("ender_chest_crafting", () -> new RecipeSerializer<>() {
                        @Override
                        public com.mojang.serialization.MapCodec<EnderChestCraftingRecipe> codec() {
                            return EnderChestCraftingRecipe.CODEC;
                        }

                        @Override
                        public net.minecraft.network.codec.StreamCodec<
                                        net.minecraft.network.RegistryFriendlyByteBuf, EnderChestCraftingRecipe>
                                streamCodec() {
                            return EnderChestCraftingRecipe.STREAM_CODEC;
                        }
                    });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<EnderBackpackCraftingRecipe>>
            ENDER_BACKPACK_CRAFTING_RECIPE =
                    RECIPE_SERIALIZERS.register("ender_backpack_crafting", () -> new RecipeSerializer<>() {
                        @Override
                        public com.mojang.serialization.MapCodec<EnderBackpackCraftingRecipe> codec() {
                            return EnderBackpackCraftingRecipe.CODEC;
                        }

                        @Override
                        public net.minecraft.network.codec.StreamCodec<
                                        net.minecraft.network.RegistryFriendlyByteBuf, EnderBackpackCraftingRecipe>
                                streamCodec() {
                            return EnderBackpackCraftingRecipe.STREAM_CODEC;
                        }
                    });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<EnderFolderCraftingRecipe>>
            ENDER_FOLDER_CRAFTING_RECIPE =
                    RECIPE_SERIALIZERS.register("ender_folder_crafting", () -> new RecipeSerializer<>() {
                        @Override
                        public com.mojang.serialization.MapCodec<EnderFolderCraftingRecipe> codec() {
                            return EnderFolderCraftingRecipe.CODEC;
                        }

                        @Override
                        public net.minecraft.network.codec.StreamCodec<
                                        net.minecraft.network.RegistryFriendlyByteBuf, EnderFolderCraftingRecipe>
                                streamCodec() {
                            return EnderFolderCraftingRecipe.STREAM_CODEC;
                        }
                    });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<EnderTankCraftingRecipe>>
            ENDER_TANK_CRAFTING_RECIPE =
                    RECIPE_SERIALIZERS.register("ender_tank_crafting", () -> new RecipeSerializer<>() {
                        @Override
                        public com.mojang.serialization.MapCodec<EnderTankCraftingRecipe> codec() {
                            return EnderTankCraftingRecipe.CODEC;
                        }

                        @Override
                        public net.minecraft.network.codec.StreamCodec<
                                        net.minecraft.network.RegistryFriendlyByteBuf, EnderTankCraftingRecipe>
                                streamCodec() {
                            return EnderTankCraftingRecipe.STREAM_CODEC;
                        }
                    });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<EnderPicnicBasketCraftingRecipe>>
            ENDER_PICNIC_BASKET_CRAFTING_RECIPE =
                    RECIPE_SERIALIZERS.register("ender_picnic_basket_crafting", () -> new RecipeSerializer<>() {
                        @Override
                        public com.mojang.serialization.MapCodec<EnderPicnicBasketCraftingRecipe> codec() {
                            return EnderPicnicBasketCraftingRecipe.CODEC;
                        }

                        @Override
                        public net.minecraft.network.codec.StreamCodec<
                                        net.minecraft.network.RegistryFriendlyByteBuf, EnderPicnicBasketCraftingRecipe>
                                streamCodec() {
                            return EnderPicnicBasketCraftingRecipe.STREAM_CODEC;
                        }
                    });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StorageUpgradeCraftingRecipe>>
            STORAGE_UPGRADE_CRAFTING_RECIPE =
                    RECIPE_SERIALIZERS.register("storage_upgrade_crafting", () -> new RecipeSerializer<>() {
                        @Override
                        public com.mojang.serialization.MapCodec<StorageUpgradeCraftingRecipe> codec() {
                            return StorageUpgradeCraftingRecipe.CODEC;
                        }

                        @Override
                        public net.minecraft.network.codec.StreamCodec<
                                        net.minecraft.network.RegistryFriendlyByteBuf, StorageUpgradeCraftingRecipe>
                                streamCodec() {
                            return StorageUpgradeCraftingRecipe.STREAM_CODEC;
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
                        if (TREMENDOUS_STORAGE_GUIDE != null) output.accept(TREMENDOUS_STORAGE_GUIDE.get());
                        output.accept(CANVAS.get());
                        output.accept(CANVAS_BLOCK_ITEM.get());
                        output.accept(GlampingRegistration.TENT_ITEM.get());
                        output.accept(DANK_FANNY_PACK.get());
                        output.accept(TREMENDOUS_BACKPACK.get());
                        output.accept(MAGIC_HAT_ITEM.get());
                        output.accept(PICNIC_BASKET_ITEM.get());
                        output.accept(LAZURITE_ORE_ITEM.get());
                        output.accept(LAZURITE_DEEPSLATE_ORE_ITEM.get());
                        output.accept(RAW_LAZURITE.get());
                        output.accept(LAZURITE_INGOT.get());
                        output.accept(LAZURITE_NUGGET.get());
                        output.accept(LAZURITE_BLOCK_ITEM.get());
                        output.accept(TREMENDOUS_CHEST_ITEM.get());
                        output.accept(TANK_ITEM.get());
                        output.accept(ARMORY_CABINET_ITEM.get());
                        output.accept(FILING_CABINET_ITEM.get());
                        output.accept(BARREL_ITEM.get());

                        output.accept(MANILA_FOLDER.get());
                        output.accept(WHITEOUT_TAPE.get());
                        output.accept(STIRLING_ENGINE_ITEM.get());
                        output.accept(ZOMBIE_BRAIN.get());
                        output.accept(BRAIN.get());
                        output.accept(NETWORK_INTERFACE_ITEM.get());
                        output.accept(STORAGE_ACCESS_TERMINAL_ITEM.get());
                        output.accept(WIRELESS_HUB_ITEM.get());

                        output.accept(TUBE_ITEM.get());
                        output.accept(STORAGE_INTERFACE.get());

                        // TODO: Disable Importer/Exporter until Filters are implemented
                        // output.accept(IMPORT_INTERFACE.get());
                        // output.accept(EXPORT_INTERFACE.get());

                        output.accept(WIRELESS_SAT.get());
                        output.accept(EXPERIENCE_SYRINGE.get());
                        output.accept(RECYCLING_BIN_ITEM.get());
                        output.accept(BASE_UPGRADE.get());
                        output.accept(CRAFTING_UPGRADE.get());
                        output.accept(ENDER_STORAGE_UPGRADE.get());
                        output.accept(ENDER_TREMENDOUS_BACKPACK_ITEM.get());
                        output.accept(ENDER_PICNIC_BASKET_ITEM.get());
                        output.accept(ENDER_TREMENDOUS_CHEST_ITEM.get());
                        for (DeferredHolder<Item, StorageUpgradeItem> upgrade : STORAGE_UPGRADES) {
                            output.accept(upgrade.get());
                        }
                        output.accept(ENDER_TANK_ITEM.get());
                        output.accept(ENDER_FOLDER.get());
                        output.accept(MAGNET_UPGRADE.get());
                        output.accept(HAARP_UPGRADE.get());
                        output.accept(PULLER_UPGRADE.get());
                        output.accept(INTERDIMENSIONAL_UPGRADE.get());

                        output.accept(LAZURITE_PICKAXE.get());
                        output.accept(LAZURITE_AXE.get());
                        output.accept(LAZURITE_SHOVEL.get());
                        output.accept(LAZURITE_SWORD.get());
                        output.accept(LAZURITE_HOE.get());
                        output.accept(LAZURITE_PAXEL.get());
                        output.accept(LAZURITE_BARS_ITEM.get());

                        output.accept(GRAHAM_CRACKER.get());
                        output.accept(CHOCOLATE_BAR.get());
                        output.accept(MARSHMALLOW.get());
                        output.accept(TOASTED_MARSHMALLOW.get());
                        output.accept(SMORE.get());
                        output.accept(HONEY_FLUID_BUCKET.get());
                        output.accept(XP_JUICE_BUCKET.get());
                        output.accept(POSITIVE_VIBES_BUCKET.get());
                        output.accept(POSITIVE_VIBES_BOTTLE.get());
                        output.accept(VEX_REPELLENT_POTION.get());
                        output.accept(VEX_REPELLENT_POTION_EXTENDED.get());
                        output.accept(VEX_REPELLENT_POTION_LONG.get());

                        if (ModList.get().isLoaded("mysticalagriculture")) {
                            var lazurite = MysticalAgricultureAPI.getCropRegistry()
                                    .getCropById(
                                            ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "lazurite"));
                            if (lazurite != null) {
                                output.accept(lazurite.getSeedsItem());
                                output.accept(lazurite.getEssenceItem());
                            }
                        }
                    })
                    .build());

    // -------------------------------------------------------------------------
    // Registration helper called from the mod constructor
    // -------------------------------------------------------------------------

    public static void register(IEventBus modEventBus) {
        if (ModList.get().isLoaded("patchouli")) {
            TREMENDOUS_STORAGE_GUIDE = ITEMS.register("tremendous_storage_guide", TremendousStorageGuideItem::new);
        }
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

        if (ModList.get().isLoaded("curios")) {
            event.enqueueWork(() -> {
                try {
                    top.theillusivec4.curios.api.CuriosApi.registerCurio(
                            MAGIC_HAT_ITEM.get(),
                            net.bobofraggins.tremendousstorage.glamping.magichat.MagicHatCurioIntegration.INSTANCE);
                } catch (NoClassDefFoundError ignored) {
                }
            });
        }
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                BARREL_BE_TYPE.get(),
                (be, side) -> new net.bobofraggins.tremendousstorage.storage.barrel.BarrelItemHandler(be));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ARMORY_CABINET_BE_TYPE.get(),
                (be, side) -> new net.bobofraggins.tremendousstorage.storage.chest.ChestItemHandler(be));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                FILING_CABINET_BE_TYPE.get(),
                (be, side) -> new FilingCabinetItemHandler(be));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK, TREMENDOUS_CHEST_BE_TYPE.get(), (be, side) -> new ChestItemHandler(be));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                TREMENDOUS_BACKPACK_BE_TYPE.get(),
                (be, side) -> new ChestItemHandler(be));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK, PICNIC_BASKET_BE_TYPE.get(), (be, side) -> new ChestItemHandler(be));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ENDER_PICNIC_BASKET_BE_TYPE.get(),
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
                Capabilities.FluidHandler.BLOCK, TANK_BE_TYPE.get(), (be, side) -> new TankFluidHandler(be));
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK, ENDER_TANK_BE_TYPE.get(), (be, side) -> new TankFluidHandler(be));
        event.registerItem(
                Capabilities.FluidHandler.ITEM, (stack, ctx) -> new TankItemFluidHandler(stack), TANK_ITEM.get());
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK, TUBE_BE_TYPE.get(), (be, side) -> be.getNetworkView());
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK, TUBE_BE_TYPE.get(), (be, side) -> new TubeEnergyHandler(be));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK, NETWORK_INTERFACE_BE_TYPE.get(), (be, side) -> be.getItemHandler());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK, NETWORK_INTERFACE_BE_TYPE.get(), (be, side) -> be.getNiFluidHandler());
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                NETWORK_INTERFACE_BE_TYPE.get(),
                (be, side) -> new NiEnergyHandler(be));
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                STIRLING_ENGINE_BE_TYPE.get(),
                (be, side) -> new StirlingEngineEnergyHandler(be));
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
