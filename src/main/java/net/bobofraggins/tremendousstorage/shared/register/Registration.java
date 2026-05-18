package net.bobofraggins.tremendousstorage.shared.register;

// import com.blakebr0.mysticalagriculture.api.MysticalAgricultureAPI; // disabled - not available on 1.21.4
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
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.shared.storage.TieredBlockItem;
import net.bobofraggins.tremendousstorage.shared.ui.PriorityControl;
import net.bobofraggins.tremendousstorage.shared.ui.TankSettingsMenu;
import net.bobofraggins.tremendousstorage.storage.accessterminal.AccessTerminalBlock;
import net.bobofraggins.tremendousstorage.storage.accessterminal.AccessTerminalBlockEntity;
import net.bobofraggins.tremendousstorage.storage.accessterminal.AccessTerminalMenu;
import net.bobofraggins.tremendousstorage.storage.armorycabinet.ArmoryCabinetBlock;
import net.bobofraggins.tremendousstorage.storage.armorycabinet.ArmoryCabinetBlockEntity;
import net.bobofraggins.tremendousstorage.storage.armorycabinet.ArmoryCabinetItem;
import net.bobofraggins.tremendousstorage.storage.armorycabinet.ArmoryCabinetMenu;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackBlock;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackBlockEntity;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackContents;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackItem;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackMenu;
import net.bobofraggins.tremendousstorage.storage.barrel.BarrelBlock;
import net.bobofraggins.tremendousstorage.storage.barrel.BarrelBlockEntity;
import net.bobofraggins.tremendousstorage.storage.barrel.BarrelContents;
import net.bobofraggins.tremendousstorage.storage.barrel.BarrelItem;
import net.bobofraggins.tremendousstorage.storage.barrel.BarrelMenu;
import net.bobofraggins.tremendousstorage.storage.barrel.CompactingBarrelItemHandler;
import net.bobofraggins.tremendousstorage.storage.baseupgrade.BaseUpgradeItem;
import net.bobofraggins.tremendousstorage.storage.baseupgrade.CompactingUpgradeItem;
import net.bobofraggins.tremendousstorage.storage.baseupgrade.CraftingUpgradeItem;
import net.bobofraggins.tremendousstorage.storage.baseupgrade.EnderStorageUpgradeItem;
import net.bobofraggins.tremendousstorage.storage.baseupgrade.HaarpUpgradeItem;
import net.bobofraggins.tremendousstorage.storage.baseupgrade.InterdimensionalUpgradeItem;
import net.bobofraggins.tremendousstorage.storage.baseupgrade.MagnetUpgradeItem;
import net.bobofraggins.tremendousstorage.storage.baseupgrade.PullerUpgradeItem;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlock;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlockEntity;
import net.bobofraggins.tremendousstorage.storage.chest.ChestItem;
import net.bobofraggins.tremendousstorage.storage.chest.ChestItemHandler;
import net.bobofraggins.tremendousstorage.storage.chest.ChestMenu;
import net.bobofraggins.tremendousstorage.storage.enderbackpack.EnderBackpackBlock;
import net.bobofraggins.tremendousstorage.storage.enderbackpack.EnderBackpackBlockEntity;
import net.bobofraggins.tremendousstorage.storage.enderbackpack.EnderBackpackCraftingRecipe;
import net.bobofraggins.tremendousstorage.storage.enderbackpack.EnderBackpackItem;
import net.bobofraggins.tremendousstorage.storage.enderbackpack.EnderBackpackMenu;
import net.bobofraggins.tremendousstorage.storage.enderbarrel.EnderBarrelBlock;
import net.bobofraggins.tremendousstorage.storage.enderbarrel.EnderBarrelBlockEntity;
import net.bobofraggins.tremendousstorage.storage.enderbarrel.EnderBarrelCraftingRecipe;
import net.bobofraggins.tremendousstorage.storage.enderbarrel.EnderBarrelItem;
import net.bobofraggins.tremendousstorage.storage.enderchest.EnderChestBlock;
import net.bobofraggins.tremendousstorage.storage.enderchest.EnderChestBlockEntity;
import net.bobofraggins.tremendousstorage.storage.enderchest.EnderChestCraftingRecipe;
import net.bobofraggins.tremendousstorage.storage.enderchest.EnderChestItem;
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
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.crafting.RecipeSerializer;
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
     * Data component storing the dimension {@link net.minecraft.resources.Identifier} of the
     * Wireless Hub that linked a Wireless SAT. Used to look up the hub/NI in the correct server
     * level when the player is in a different dimension.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.resources.Identifier>>
            WIRELESS_HUB_DIMENSION = DATA_COMPONENTS.register(
                    "wireless_hub_dimension", () -> DataComponentType.<net.minecraft.resources.Identifier>builder()
                            .persistent(net.minecraft.resources.Identifier.CODEC)
                            .networkSynchronized(net.minecraft.resources.Identifier.STREAM_CODEC)
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

    public static final DeferredBlock<ArmoryCabinetBlock> ARMORY_CABINET =
            BLOCKS.registerBlock("armory_cabinet", ArmoryCabinetBlock::new, () -> BlockBehaviour.Properties.of()
                    .strength(5.0f, 1000.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion());

    public static final DeferredHolder<Item, BlockItem> ARMORY_CABINET_ITEM = ITEMS.registerItem(
            "armory_cabinet", props -> new ArmoryCabinetItem(ARMORY_CABINET.get(), props), Item.Properties::new);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArmoryCabinetBlockEntity>>
            ARMORY_CABINET_BE_TYPE = BLOCK_ENTITY_TYPES.register(
                    "armory_cabinet", () -> new BlockEntityType<>(ArmoryCabinetBlockEntity::new, ARMORY_CABINET.get()));

    public static final DeferredBlock<FilingCabinetBlock> FILING_CABINET =
            BLOCKS.registerBlock("filing_cabinet", FilingCabinetBlock::new, () -> BlockBehaviour.Properties.of()
                    .strength(5.0f, 1000.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.WOOD));

    public static final DeferredHolder<Item, BlockItem> FILING_CABINET_ITEM = ITEMS.registerItem(
            "filing_cabinet", props -> new TieredBlockItem(FILING_CABINET.get(), props), Item.Properties::new);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FilingCabinetBlockEntity>>
            FILING_CABINET_BE_TYPE = BLOCK_ENTITY_TYPES.register(
                    "filing_cabinet", () -> new BlockEntityType<>(FilingCabinetBlockEntity::new, FILING_CABINET.get()));

    public static final DeferredBlock<ChestBlock> TREMENDOUS_CHEST =
            BLOCKS.registerBlock("chest", ChestBlock::new, () -> BlockBehaviour.Properties.of()
                    .strength(5.0f, 1000.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion());

    public static final DeferredHolder<Item, BlockItem> TREMENDOUS_CHEST_ITEM =
            ITEMS.registerItem("chest", props -> new ChestItem(TREMENDOUS_CHEST.get(), props), Item.Properties::new);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChestBlockEntity>> TREMENDOUS_CHEST_BE_TYPE =
            BLOCK_ENTITY_TYPES.register(
                    "chest", () -> new BlockEntityType<>(ChestBlockEntity::new, TREMENDOUS_CHEST.get()));

    public static final DeferredBlock<EnderChestBlock> ENDER_TREMENDOUS_CHEST =
            BLOCKS.registerBlock("ender_chest", EnderChestBlock::new, () -> BlockBehaviour.Properties.of()
                    .strength(5.0f, 1000.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion());

    public static final DeferredHolder<Item, BlockItem> ENDER_TREMENDOUS_CHEST_ITEM = ITEMS.registerItem(
            "ender_chest", props -> new EnderChestItem(ENDER_TREMENDOUS_CHEST.get(), props), Item.Properties::new);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnderChestBlockEntity>>
            ENDER_TREMENDOUS_CHEST_BE_TYPE = BLOCK_ENTITY_TYPES.register(
                    "ender_chest",
                    () -> new BlockEntityType<>(EnderChestBlockEntity::new, ENDER_TREMENDOUS_CHEST.get()));

    public static final DeferredBlock<BackpackBlock> TREMENDOUS_BACKPACK_BLOCK =
            BLOCKS.registerBlock("backpack", BackpackBlock::new, () -> BlockBehaviour.Properties.of()
                    .strength(2.0f, 1000.0f)
                    .sound(SoundType.WOOL)
                    .noOcclusion());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BackpackBlockEntity>>
            TREMENDOUS_BACKPACK_BE_TYPE = BLOCK_ENTITY_TYPES.register(
                    "backpack", () -> new BlockEntityType<>(BackpackBlockEntity::new, TREMENDOUS_BACKPACK_BLOCK.get()));

    public static final DeferredBlock<EnderBackpackBlock> ENDER_TREMENDOUS_BACKPACK_BLOCK =
            BLOCKS.registerBlock("ender_backpack", EnderBackpackBlock::new, () -> BlockBehaviour.Properties.of()
                    .strength(2.0f, 1000.0f)
                    .sound(SoundType.WOOL)
                    .noOcclusion());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnderBackpackBlockEntity>>
            ENDER_TREMENDOUS_BACKPACK_BE_TYPE = BLOCK_ENTITY_TYPES.register(
                    "ender_backpack",
                    () -> new BlockEntityType<>(EnderBackpackBlockEntity::new, ENDER_TREMENDOUS_BACKPACK_BLOCK.get()));

    public static final DeferredBlock<net.bobofraggins.tremendousstorage.glamping.picnicbasket.PicnicBasketBlock>
            PICNIC_BASKET_BLOCK = BLOCKS.registerBlock(
                    "picnic_basket",
                    net.bobofraggins.tremendousstorage.glamping.picnicbasket.PicnicBasketBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .strength(1.0f, 10.0f)
                            .sound(SoundType.WOOD)
                            .noOcclusion());

    public static final DeferredHolder<
                    BlockEntityType<?>,
                    BlockEntityType<net.bobofraggins.tremendousstorage.glamping.picnicbasket.PicnicBasketBlockEntity>>
            PICNIC_BASKET_BE_TYPE = BLOCK_ENTITY_TYPES.register(
                    "picnic_basket",
                    () -> new BlockEntityType<>(
                            net.bobofraggins.tremendousstorage.glamping.picnicbasket.PicnicBasketBlockEntity::new,
                            PICNIC_BASKET_BLOCK.get()));

    // -------------------------------------------------------------------------
    // Barrel block + block entity
    // -------------------------------------------------------------------------

    public static final DeferredBlock<BarrelBlock> BARREL =
            BLOCKS.registerBlock("barrel", BarrelBlock::new, () -> BlockBehaviour.Properties.of()
                    .strength(3.0f, 1000.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion());

    public static final DeferredHolder<Item, BlockItem> BARREL_ITEM =
            ITEMS.registerItem("barrel", props -> new BarrelItem(BARREL.get(), props), Item.Properties::new);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BarrelBlockEntity>> BARREL_BE_TYPE =
            BLOCK_ENTITY_TYPES.register("barrel", () -> new BlockEntityType<>(BarrelBlockEntity::new, BARREL.get()));

    public static final DeferredBlock<EnderBarrelBlock> ENDER_BARREL =
            BLOCKS.registerBlock("ender_barrel", EnderBarrelBlock::new, () -> BlockBehaviour.Properties.of()
                    .strength(3.0f, 1000.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion());

    public static final DeferredHolder<Item, BlockItem> ENDER_BARREL_ITEM = ITEMS.registerItem(
            "ender_barrel", props -> new EnderBarrelItem(ENDER_BARREL.get(), props), Item.Properties::new);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnderBarrelBlockEntity>>
            ENDER_BARREL_BE_TYPE = BLOCK_ENTITY_TYPES.register(
                    "ender_barrel", () -> new BlockEntityType<>(EnderBarrelBlockEntity::new, ENDER_BARREL.get()));

    // -------------------------------------------------------------------------
    // Storage upgrade items
    // -------------------------------------------------------------------------

    public static final DeferredHolder<Item, StorageUpgradeItem> WOOD_TO_COPPER_STORAGE_UPGRADE = ITEMS.registerItem(
            "wood_to_copper_storage_upgrade",
            props -> new StorageUpgradeItem(StorageTier.WOOD, StorageTier.COPPER, props),
            Item.Properties::new);

    public static final DeferredHolder<Item, StorageUpgradeItem> COPPER_TO_IRON_STORAGE_UPGRADE = ITEMS.registerItem(
            "copper_to_iron_storage_upgrade",
            props -> new StorageUpgradeItem(StorageTier.COPPER, StorageTier.IRON, props),
            Item.Properties::new);

    public static final DeferredHolder<Item, StorageUpgradeItem> IRON_TO_GOLD_STORAGE_UPGRADE = ITEMS.registerItem(
            "iron_to_gold_storage_upgrade",
            props -> new StorageUpgradeItem(StorageTier.IRON, StorageTier.GOLD, props),
            Item.Properties::new);

    public static final DeferredHolder<Item, StorageUpgradeItem> GOLD_TO_DIAMOND_STORAGE_UPGRADE = ITEMS.registerItem(
            "gold_to_diamond_storage_upgrade",
            props -> new StorageUpgradeItem(StorageTier.GOLD, StorageTier.DIAMOND, props),
            Item.Properties::new);

    public static final DeferredHolder<Item, StorageUpgradeItem> DIAMOND_TO_EMERALD_STORAGE_UPGRADE =
            ITEMS.registerItem(
                    "diamond_to_emerald_storage_upgrade",
                    props -> new StorageUpgradeItem(StorageTier.DIAMOND, StorageTier.EMERALD, props),
                    Item.Properties::new);

    public static final DeferredHolder<Item, StorageUpgradeItem> EMERALD_TO_NETHERITE_STORAGE_UPGRADE =
            ITEMS.registerItem(
                    "emerald_to_netherite_storage_upgrade",
                    props -> new StorageUpgradeItem(StorageTier.EMERALD, StorageTier.NETHERITE, props),
                    Item.Properties::new);

    public static final DeferredHolder<Item, StorageUpgradeItem> NETHERITE_TO_NETHER_STAR_STORAGE_UPGRADE =
            ITEMS.registerItem(
                    "netherite_to_nether_star_storage_upgrade",
                    props -> new StorageUpgradeItem(StorageTier.NETHERITE, StorageTier.NETHER_STAR, true, props),
                    Item.Properties::new);

    @SuppressWarnings("unchecked")
    public static final DeferredHolder<Item, StorageUpgradeItem>[] STORAGE_UPGRADES = new DeferredHolder[] {
        WOOD_TO_COPPER_STORAGE_UPGRADE,
        COPPER_TO_IRON_STORAGE_UPGRADE,
        IRON_TO_GOLD_STORAGE_UPGRADE,
        GOLD_TO_DIAMOND_STORAGE_UPGRADE,
        DIAMOND_TO_EMERALD_STORAGE_UPGRADE,
        EMERALD_TO_NETHERITE_STORAGE_UPGRADE,
        NETHERITE_TO_NETHER_STAR_STORAGE_UPGRADE
    };

    public static final DeferredBlock<TankBlock> TANK =
            BLOCKS.registerBlock("tank", TankBlock::new, () -> BlockBehaviour.Properties.of()
                    .strength(3.0f, 1000.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.GLASS)
                    .noOcclusion());

    public static final DeferredHolder<Item, BlockItem> TANK_ITEM =
            ITEMS.registerItem("tank", props -> new TankItem(TANK.get(), props), Item.Properties::new);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TankBlockEntity>> TANK_BE_TYPE =
            BLOCK_ENTITY_TYPES.register("tank", () -> new BlockEntityType<>(TankBlockEntity::new, TANK.get()));

    public static final DeferredBlock<EnderTankBlock> ENDER_TANK =
            BLOCKS.registerBlock("ender_tank", EnderTankBlock::new, () -> BlockBehaviour.Properties.of()
                    .strength(3.0f, 1000.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.GLASS)
                    .noOcclusion());

    public static final DeferredHolder<Item, BlockItem> ENDER_TANK_ITEM =
            ITEMS.registerItem("ender_tank", props -> new EnderTankItem(ENDER_TANK.get(), props), Item.Properties::new);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnderTankBlockEntity>> ENDER_TANK_BE_TYPE =
            BLOCK_ENTITY_TYPES.register(
                    "ender_tank", () -> new BlockEntityType<>(EnderTankBlockEntity::new, ENDER_TANK.get()));

    // -------------------------------------------------------------------------
    // Recycling Bin
    // -------------------------------------------------------------------------

    public static final DeferredBlock<RecyclingBinBlock> RECYCLING_BIN =
            BLOCKS.registerBlock("recycling_bin", RecyclingBinBlock::new, () -> BlockBehaviour.Properties.of()
                    .strength(3.0f, 1000.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion());

    public static final DeferredHolder<Item, BlockItem> RECYCLING_BIN_ITEM =
            ITEMS.registerSimpleBlockItem("recycling_bin", RECYCLING_BIN);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RecyclingBinBlockEntity>>
            RECYCLING_BIN_BE_TYPE = BLOCK_ENTITY_TYPES.register(
                    "recycling_bin", () -> new BlockEntityType<>(RecyclingBinBlockEntity::new, RECYCLING_BIN.get()));

    // -------------------------------------------------------------------------
    // Lazurite bars
    // -------------------------------------------------------------------------

    public static final DeferredBlock<LazuriteBarsBlock> LAZURITE_BARS =
            BLOCKS.registerBlock("lazurite_bars", LazuriteBarsBlock::new, () -> BlockBehaviour.Properties.of()
                    .strength(5.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion());

    public static final DeferredHolder<Item, BlockItem> LAZURITE_BARS_ITEM =
            ITEMS.registerSimpleBlockItem("lazurite_bars", LAZURITE_BARS);

    // -------------------------------------------------------------------------
    // Lazurite ore + ingot
    // -------------------------------------------------------------------------

    public static final DeferredBlock<LazuriteOreBlock> LAZURITE_ORE =
            BLOCKS.registerBlock("lazurite_ore", LazuriteOreBlock::new, () -> BlockBehaviour.Properties.of()
                    .strength(3.0f, 3.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE));

    public static final DeferredHolder<Item, BlockItem> LAZURITE_ORE_ITEM =
            ITEMS.registerSimpleBlockItem("lazurite_ore", LAZURITE_ORE);

    public static final DeferredBlock<LazuriteOreBlock> LAZURITE_DEEPSLATE_ORE =
            BLOCKS.registerBlock("lazurite_deepslate_ore", LazuriteOreBlock::new, () -> BlockBehaviour.Properties.of()
                    .strength(4.5f, 3.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE));

    public static final DeferredHolder<Item, BlockItem> LAZURITE_DEEPSLATE_ORE_ITEM =
            ITEMS.registerSimpleBlockItem("lazurite_deepslate_ore", LAZURITE_DEEPSLATE_ORE);

    public static final DeferredHolder<Item, Item> RAW_LAZURITE = ITEMS.registerItem("raw_lazurite", Item::new);

    public static final DeferredHolder<Item, Item> LAZURITE_INGOT = ITEMS.registerItem("lazurite_ingot", Item::new);

    public static final DeferredHolder<Item, Item> LAZURITE_NUGGET = ITEMS.registerItem("lazurite_nugget", Item::new);

    public static final DeferredBlock<net.minecraft.world.level.block.Block> LAZURITE_BLOCK = BLOCKS.registerBlock(
            "lazurite_block", net.minecraft.world.level.block.Block::new, () -> BlockBehaviour.Properties.of()
                    .strength(5.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL));

    public static final DeferredHolder<Item, BlockItem> LAZURITE_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem("lazurite_block", LAZURITE_BLOCK);

    // -------------------------------------------------------------------------
    // Canvas block
    // -------------------------------------------------------------------------

    public static final DeferredBlock<net.minecraft.world.level.block.Block> CANVAS_BLOCK = BLOCKS.registerBlock(
            "canvas_block",
            net.minecraft.world.level.block.Block::new,
            () -> BlockBehaviour.Properties.of().strength(0.5f, 0.5f).sound(SoundType.WOOL));

    public static final DeferredHolder<Item, BlockItem> CANVAS_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem("canvas_block", CANVAS_BLOCK);

    // -------------------------------------------------------------------------
    // Lazurite tools
    // -------------------------------------------------------------------------

    public static final DeferredHolder<Item, Item> LAZURITE_PICKAXE = ITEMS.registerItem(
            "lazurite_pickaxe",
            props -> new Item(LazuriteTier.INSTANCE.applyToolProperties(
                    props,
                    net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE,
                    1.0f,
                    -2.8f,
                    LazuriteTier.INSTANCE.speed())),
            Item.Properties::new);

    public static final DeferredHolder<Item, AxeItem> LAZURITE_AXE = ITEMS.registerItem(
            "lazurite_axe", props -> new AxeItem(LazuriteTier.INSTANCE, 4.0f, -3.3f, props), Item.Properties::new);

    public static final DeferredHolder<Item, ShovelItem> LAZURITE_SHOVEL = ITEMS.registerItem(
            "lazurite_shovel",
            props -> new ShovelItem(LazuriteTier.INSTANCE, 1.5f, -3.0f, props),
            Item.Properties::new);

    public static final DeferredHolder<Item, Item> LAZURITE_SWORD = ITEMS.registerItem(
            "lazurite_sword",
            props -> new Item(LazuriteTier.INSTANCE.applySwordProperties(props, 3.0f, -2.4f)),
            Item.Properties::new);

    public static final DeferredHolder<Item, HoeItem> LAZURITE_HOE = ITEMS.registerItem(
            "lazurite_hoe", props -> new HoeItem(LazuriteTier.INSTANCE, -2.0f, -1.0f, props), Item.Properties::new);

    public static final DeferredHolder<Item, LazuritePaxelItem> LAZURITE_PAXEL =
            ITEMS.registerItem("lazurite_paxel", LazuritePaxelItem::new);

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

    public static final DeferredBlock<HoneyBlock> HONEY_FLUID_BLOCK = BLOCKS.registerBlock(
            "honey_fluid", props -> new HoneyBlock(HONEY_SOURCE.get(), props), () -> BlockBehaviour.Properties.of()
                    .noCollision()
                    .strength(100f)
                    .noLootTable()
                    .liquid()
                    .replaceable()
                    .pushReaction(PushReaction.DESTROY));

    public static final DeferredHolder<Item, BucketItem> HONEY_FLUID_BUCKET = ITEMS.registerItem(
            "honey_fluid_bucket",
            props -> new BucketItem(HONEY_SOURCE.get(), props),
            () -> new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET));

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

    public static final DeferredBlock<net.minecraft.world.level.block.LiquidBlock> XP_JUICE_BLOCK =
            BLOCKS.registerBlock(
                    "xp_juice",
                    props -> new net.minecraft.world.level.block.LiquidBlock(XP_JUICE_SOURCE.get(), props),
                    () -> BlockBehaviour.Properties.of()
                            .noCollision()
                            .strength(100f)
                            .noLootTable()
                            .liquid()
                            .replaceable()
                            .pushReaction(PushReaction.DESTROY));

    public static final DeferredHolder<Item, BucketItem> XP_JUICE_BUCKET = ITEMS.registerItem(
            "xp_juice_bucket",
            props -> new BucketItem(XP_JUICE_SOURCE.get(), props),
            () -> new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET));

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

    public static final DeferredBlock<PositiveVibesBlock> POSITIVE_VIBES_BLOCK = BLOCKS.registerBlock(
            "positive_vibes",
            props -> new PositiveVibesBlock(POSITIVE_VIBES_SOURCE.get(), props),
            () -> BlockBehaviour.Properties.of()
                    .noCollision()
                    .strength(100f)
                    .noLootTable()
                    .liquid()
                    .replaceable()
                    .pushReaction(PushReaction.DESTROY)
                    .lightLevel(state -> 8));

    public static final DeferredHolder<Item, ZombieBrainItem> ZOMBIE_BRAIN =
            ITEMS.registerItem("zombie_brain", ZombieBrainItem::new);

    public static final DeferredHolder<Item, BrainItem> BRAIN = ITEMS.registerItem("brain", BrainItem::new);

    private static final FoodProperties SNACK_FOOD =
            new FoodProperties.Builder().nutrition(2).saturationModifier(0.25f).build();

    public static final DeferredHolder<Item, Item> GRAHAM_CRACKER =
            ITEMS.registerItem("graham_cracker", Item::new, () -> new Item.Properties().food(SNACK_FOOD));

    public static final DeferredHolder<Item, Item> CHOCOLATE_BAR =
            ITEMS.registerItem("chocolate_bar", Item::new, () -> new Item.Properties().food(SNACK_FOOD));

    public static final DeferredHolder<Item, Item> MARSHMALLOW =
            ITEMS.registerItem("marshmallow", Item::new, () -> new Item.Properties().food(SNACK_FOOD));

    public static final DeferredHolder<Item, Item> TOASTED_MARSHMALLOW =
            ITEMS.registerItem("toasted_marshmallow", Item::new, () -> new Item.Properties().food(SNACK_FOOD));

    public static final DeferredHolder<Item, Item> SMORE =
            ITEMS.registerItem("smore", Item::new, () -> new Item.Properties()
                    .food(new FoodProperties.Builder()
                            .nutrition(10)
                            .saturationModifier(0.25f)
                            .build()));

    public static final DeferredHolder<Item, Item> CANVAS = ITEMS.registerItem("canvas", Item::new);

    public static final DeferredHolder<Item, BlockItem> PICNIC_BASKET_ITEM = ITEMS.registerItem(
            "picnic_basket",
            props -> new net.bobofraggins.tremendousstorage.glamping.picnicbasket.PicnicBasketBlockItem(
                    PICNIC_BASKET_BLOCK.get(), "item.tremendousstorage.picnic_basket.tooltip", props),
            Item.Properties::new);

    public static final DeferredBlock<EnderPicnicBasketBlock> ENDER_PICNIC_BASKET_BLOCK = BLOCKS.registerBlock(
            "ender_picnic_basket", EnderPicnicBasketBlock::new, () -> BlockBehaviour.Properties.of()
                    .strength(1.0f, 10.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion());

    public static final DeferredHolder<Item, BlockItem> ENDER_PICNIC_BASKET_ITEM = ITEMS.registerItem(
            "ender_picnic_basket",
            props -> new net.bobofraggins.tremendousstorage.glamping.picnicbasket.PicnicBasketBlockItem(
                    ENDER_PICNIC_BASKET_BLOCK.get(), "item.tremendousstorage.ender_picnic_basket.tooltip", props),
            Item.Properties::new);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnderPicnicBasketBlockEntity>>
            ENDER_PICNIC_BASKET_BE_TYPE = BLOCK_ENTITY_TYPES.register(
                    "ender_picnic_basket",
                    () -> new BlockEntityType<>(EnderPicnicBasketBlockEntity::new, ENDER_PICNIC_BASKET_BLOCK.get()));

    // -------------------------------------------------------------------------
    // Dank Fanny Pack
    // -------------------------------------------------------------------------

    public static final DeferredHolder<Item, DankFannyPackItem> DANK_FANNY_PACK =
            ITEMS.registerItem("dank_fanny_pack", DankFannyPackItem::new, () -> new Item.Properties().stacksTo(1));

    // -------------------------------------------------------------------------
    // Magic Hat
    // -------------------------------------------------------------------------

    public static final DeferredBlock<MagicHatBlock> MAGIC_HAT_BLOCK = BLOCKS.registerBlock(
            "magic_hat",
            MagicHatBlock::new,
            () -> BlockBehaviour.Properties.of().strength(0.5f).noOcclusion().sound(SoundType.WOOL));

    public static final DeferredHolder<Item, MagicHatItem> MAGIC_HAT_ITEM = ITEMS.registerItem(
            "magic_hat", props -> new MagicHatItem(MAGIC_HAT_BLOCK.get(), props), () -> new Item.Properties()
                    .stacksTo(1)
                    .attributes(net.bobofraggins.tremendousstorage.glamping.magichat.MagicHatItem.DEFAULT_MODIFIERS));

    public static DeferredHolder<Item, TremendousStorageGuideItem> TREMENDOUS_STORAGE_GUIDE = null;

    public static final DeferredHolder<Item, BackpackItem> TREMENDOUS_BACKPACK = ITEMS.registerItem(
            "backpack", props -> new BackpackItem(TREMENDOUS_BACKPACK_BLOCK.get(), props), () -> new Item.Properties()
                    .stacksTo(1));

    public static final DeferredHolder<Item, EnderBackpackItem> ENDER_TREMENDOUS_BACKPACK_ITEM = ITEMS.registerItem(
            "ender_backpack",
            props -> new EnderBackpackItem(ENDER_TREMENDOUS_BACKPACK_BLOCK.get(), props),
            () -> new Item.Properties().stacksTo(1));

    public static final DeferredHolder<Item, Item> POSITIVE_VIBES_BOTTLE =
            ITEMS.registerItem("positive_vibes_bottle", Item::new);

    public static final DeferredHolder<Item, VexRepellentPotionItem> VEX_REPELLENT_POTION = ITEMS.registerItem(
            "vex_repellent_potion", props -> new VexRepellentPotionItem(1 * 60 * 20, props), () -> new Item.Properties()
                    .stacksTo(1));

    public static final DeferredHolder<Item, VexRepellentPotionItem> VEX_REPELLENT_POTION_EXTENDED = ITEMS.registerItem(
            "vex_repellent_potion_extended",
            props -> new VexRepellentPotionItem(3 * 60 * 20, props),
            () -> new Item.Properties().stacksTo(1));

    public static final DeferredHolder<Item, VexRepellentPotionItem> VEX_REPELLENT_POTION_LONG = ITEMS.registerItem(
            "vex_repellent_potion_long",
            props -> new VexRepellentPotionItem(8 * 60 * 20, props),
            () -> new Item.Properties().stacksTo(1));

    public static final DeferredHolder<Item, BucketItem> POSITIVE_VIBES_BUCKET = ITEMS.registerItem(
            "positive_vibes_bucket",
            props -> new BucketItem(POSITIVE_VIBES_SOURCE.get(), props),
            () -> new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET));

    /** Shared properties object for the Positive Vibes Source + Flowing fluids. */
    public static final BaseFlowingFluid.Properties POSITIVE_VIBES_FLUID_PROPS = new BaseFlowingFluid.Properties(
                    POSITIVE_VIBES_TYPE, () -> POSITIVE_VIBES_SOURCE.get(), () -> POSITIVE_VIBES_FLOWING.get())
            .bucket(() -> POSITIVE_VIBES_BUCKET.get())
            .block(() -> POSITIVE_VIBES_BLOCK.get())
            .slopeFindDistance(2)
            .levelDecreasePerBlock(1)
            .tickRate(30);

    public static final DeferredBlock<PositiveVibesCauldronBlock> POSITIVE_VIBES_CAULDRON = BLOCKS.registerBlock(
            "positive_vibes_cauldron", PositiveVibesCauldronBlock::new, () -> BlockBehaviour.Properties.of()
                    .strength(2f)
                    .sound(SoundType.METAL)
                    .noOcclusion());

    public static final DeferredHolder<Item, BlockItem> POSITIVE_VIBES_CAULDRON_ITEM =
            ITEMS.registerSimpleBlockItem("positive_vibes_cauldron", POSITIVE_VIBES_CAULDRON);

    // -------------------------------------------------------------------------
    // Stirling Engine block + block entity
    // -------------------------------------------------------------------------

    public static final DeferredBlock<StirlingEngineBlock> STIRLING_ENGINE =
            BLOCKS.registerBlock("stirling_engine", StirlingEngineBlock::new, () -> BlockBehaviour.Properties.of()
                    .strength(3f, 1000f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(state -> 7));

    public static final DeferredHolder<Item, BlockItem> STIRLING_ENGINE_ITEM = ITEMS.registerItem(
            "stirling_engine", props -> new TieredBlockItem(STIRLING_ENGINE.get(), props), () -> new Item.Properties()
                    .fireResistant());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StirlingEngineBlockEntity>>
            STIRLING_ENGINE_BE_TYPE = BLOCK_ENTITY_TYPES.register(
                    "stirling_engine",
                    () -> new BlockEntityType<>(StirlingEngineBlockEntity::new, STIRLING_ENGINE.get()));

    // -------------------------------------------------------------------------
    // Network Interface block + block entity
    // -------------------------------------------------------------------------

    public static final DeferredBlock<NetworkInterfaceBlock> NETWORK_INTERFACE =
            BLOCKS.registerBlock("network_interface", NetworkInterfaceBlock::new, () -> BlockBehaviour.Properties.of()
                    .strength(5f, 1000f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(state -> 8));

    public static final DeferredHolder<Item, BlockItem> NETWORK_INTERFACE_ITEM = ITEMS.registerItem(
            "network_interface", props -> new TieredBlockItem(NETWORK_INTERFACE.get(), props), Item.Properties::new);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetworkInterfaceBlockEntity>>
            NETWORK_INTERFACE_BE_TYPE = BLOCK_ENTITY_TYPES.register(
                    "network_interface",
                    () -> new BlockEntityType<>(NetworkInterfaceBlockEntity::new, NETWORK_INTERFACE.get()));

    // -------------------------------------------------------------------------
    // Storage Access Terminal block
    // -------------------------------------------------------------------------

    public static final DeferredBlock<AccessTerminalBlock> STORAGE_ACCESS_TERMINAL = BLOCKS.registerBlock(
            "storage_access_terminal", AccessTerminalBlock::new, () -> BlockBehaviour.Properties.of()
                    .strength(2.5f, 1000f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.WOOD)
                    .noOcclusion());

    public static final DeferredHolder<Item, BlockItem> STORAGE_ACCESS_TERMINAL_ITEM = ITEMS.registerItem(
            "storage_access_terminal",
            props -> new TieredBlockItem(STORAGE_ACCESS_TERMINAL.get(), props),
            Item.Properties::new);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AccessTerminalBlockEntity>>
            STORAGE_ACCESS_TERMINAL_BE_TYPE = BLOCK_ENTITY_TYPES.register(
                    "storage_access_terminal",
                    () -> new BlockEntityType<>(AccessTerminalBlockEntity::new, STORAGE_ACCESS_TERMINAL.get()));

    // -------------------------------------------------------------------------
    // Wireless Hub block + block entity
    // -------------------------------------------------------------------------

    public static final DeferredBlock<WirelessHubBlock> WIRELESS_HUB =
            BLOCKS.registerBlock("wireless_hub", WirelessHubBlock::new, () -> BlockBehaviour.Properties.of()
                    .strength(2.5f, 1000f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(state -> 14));

    public static final DeferredHolder<Item, BlockItem> WIRELESS_HUB_ITEM = ITEMS.registerItem(
            "wireless_hub", props -> new TieredBlockItem(WIRELESS_HUB.get(), props), Item.Properties::new);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WirelessHubBlockEntity>>
            WIRELESS_HUB_BE_TYPE = BLOCK_ENTITY_TYPES.register(
                    "wireless_hub", () -> new BlockEntityType<>(WirelessHubBlockEntity::new, WIRELESS_HUB.get()));

    // -------------------------------------------------------------------------
    // Wireless SAT item
    // -------------------------------------------------------------------------

    public static final DeferredHolder<Item, PersonalAccessTerminalItem> WIRELESS_SAT = ITEMS.registerItem(
            "wireless_sat", PersonalAccessTerminalItem::new, () -> new Item.Properties().stacksTo(1));

    // -------------------------------------------------------------------------
    // Tubes
    // -------------------------------------------------------------------------

    public static final DeferredBlock<TubeBlock> TUBE =
            BLOCKS.registerBlock("tube", TubeBlock::new, () -> BlockBehaviour.Properties.of()
                    .strength(1.5f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion());

    public static final DeferredHolder<Item, BlockItem> TUBE_ITEM = ITEMS.registerSimpleBlockItem("tube", TUBE);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TubeBlockEntity>> TUBE_BE_TYPE =
            BLOCK_ENTITY_TYPES.register("tube", () -> new BlockEntityType<>(TubeBlockEntity::new, TUBE.get()));

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
            ITEMS.registerItem("storage_interface", StorageInterfaceItem::new);

    public static final DeferredHolder<Item, ImportInterfaceItem> IMPORT_INTERFACE =
            ITEMS.registerItem("import_interface", ImportInterfaceItem::new);

    public static final DeferredHolder<Item, ExportInterfaceItem> EXPORT_INTERFACE =
            ITEMS.registerItem("export_interface", ExportInterfaceItem::new);

    // -------------------------------------------------------------------------
    // Items — whiteout tape
    // -------------------------------------------------------------------------

    public static final DeferredHolder<Item, WhiteoutTapeItem> WHITEOUT_TAPE =
            ITEMS.registerItem("whiteout_tape", WhiteoutTapeItem::new, () -> new Item.Properties()
                    .stacksTo(1)
                    .durability(WhiteoutTapeItem.MAX_DURABILITY));

    public static final DeferredHolder<Item, BaseUpgradeItem> BASE_UPGRADE =
            ITEMS.registerItem("base_upgrade", BaseUpgradeItem::new);

    public static final DeferredHolder<Item, CraftingUpgradeItem> CRAFTING_UPGRADE =
            ITEMS.registerItem("crafting_upgrade", CraftingUpgradeItem::new);

    public static final DeferredHolder<Item, EnderStorageUpgradeItem> ENDER_STORAGE_UPGRADE =
            ITEMS.registerItem("ender_storage_upgrade", EnderStorageUpgradeItem::new);

    public static final DeferredHolder<Item, MagnetUpgradeItem> MAGNET_UPGRADE =
            ITEMS.registerItem("magnet_upgrade", MagnetUpgradeItem::new);

    public static final DeferredHolder<Item, HaarpUpgradeItem> HAARP_UPGRADE =
            ITEMS.registerItem("haarp_upgrade", HaarpUpgradeItem::new);

    public static final DeferredHolder<Item, PullerUpgradeItem> PULLER_UPGRADE =
            ITEMS.registerItem("puller_upgrade", PullerUpgradeItem::new);

    public static final DeferredHolder<Item, InterdimensionalUpgradeItem> INTERDIMENSIONAL_UPGRADE =
            ITEMS.registerItem("interdimensional_upgrade", InterdimensionalUpgradeItem::new);

    public static final DeferredHolder<Item, CompactingUpgradeItem> COMPACTING_UPGRADE =
            ITEMS.registerItem("compacting_upgrade", CompactingUpgradeItem::new);

    public static final DeferredHolder<Item, ExperienceSyringeItem> EXPERIENCE_SYRINGE = ITEMS.registerItem(
            "experience_syringe", ExperienceSyringeItem::new, () -> new Item.Properties().stacksTo(1));

    // -------------------------------------------------------------------------
    // Items — Manila Folder and Ender Folder (single items; tier in FolderContents)
    // -------------------------------------------------------------------------

    public static final DeferredHolder<Item, ManillaFolderItem> MANILA_FOLDER =
            ITEMS.registerItem("manila_folder", ManillaFolderItem::new);

    public static final DeferredHolder<Item, EnderFolderItem> ENDER_FOLDER =
            ITEMS.registerItem("ender_folder", EnderFolderItem::new);

    // -------------------------------------------------------------------------
    // Recipe serializers
    // -------------------------------------------------------------------------

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FolderStorageRecipe>>
            FOLDER_STORAGE_RECIPE = RECIPE_SERIALIZERS.register(
                    "folder_storage", () -> singletonRecipeSerializer(new FolderStorageRecipe()));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FolderExtractRecipe>>
            FOLDER_EXTRACT_RECIPE = RECIPE_SERIALIZERS.register(
                    "folder_extract", () -> singletonRecipeSerializer(new FolderExtractRecipe()));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FolderMergeRecipe>> FOLDER_MERGE_RECIPE =
            RECIPE_SERIALIZERS.register("folder_merge", () -> singletonRecipeSerializer(new FolderMergeRecipe()));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FolderTapeRecipe>> FOLDER_TAPE_RECIPE =
            RECIPE_SERIALIZERS.register("folder_tape", () -> singletonRecipeSerializer(new FolderTapeRecipe()));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<LazuriteRepairRecipe>>
            LAZURITE_REPAIR_RECIPE = RECIPE_SERIALIZERS.register(
                    "lazurite_repair", () -> singletonRecipeSerializer(new LazuriteRepairRecipe()));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<EnderChestCraftingRecipe>>
            ENDER_CHEST_CRAFTING_RECIPE = RECIPE_SERIALIZERS.register(
                    "ender_chest_crafting",
                    () -> new RecipeSerializer<>(
                            EnderChestCraftingRecipe.CODEC, EnderChestCraftingRecipe.STREAM_CODEC));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<EnderBackpackCraftingRecipe>>
            ENDER_BACKPACK_CRAFTING_RECIPE = RECIPE_SERIALIZERS.register(
                    "ender_backpack_crafting",
                    () -> new RecipeSerializer<>(
                            EnderBackpackCraftingRecipe.CODEC, EnderBackpackCraftingRecipe.STREAM_CODEC));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<EnderFolderCraftingRecipe>>
            ENDER_FOLDER_CRAFTING_RECIPE = RECIPE_SERIALIZERS.register(
                    "ender_folder_crafting",
                    () -> new RecipeSerializer<>(
                            EnderFolderCraftingRecipe.CODEC, EnderFolderCraftingRecipe.STREAM_CODEC));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<EnderTankCraftingRecipe>>
            ENDER_TANK_CRAFTING_RECIPE = RECIPE_SERIALIZERS.register(
                    "ender_tank_crafting",
                    () -> new RecipeSerializer<>(EnderTankCraftingRecipe.CODEC, EnderTankCraftingRecipe.STREAM_CODEC));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<EnderPicnicBasketCraftingRecipe>>
            ENDER_PICNIC_BASKET_CRAFTING_RECIPE = RECIPE_SERIALIZERS.register(
                    "ender_picnic_basket_crafting",
                    () -> new RecipeSerializer<>(
                            EnderPicnicBasketCraftingRecipe.CODEC, EnderPicnicBasketCraftingRecipe.STREAM_CODEC));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<EnderBarrelCraftingRecipe>>
            ENDER_BARREL_CRAFTING_RECIPE = RECIPE_SERIALIZERS.register(
                    "ender_barrel_crafting",
                    () -> new RecipeSerializer<>(
                            EnderBarrelCraftingRecipe.CODEC, EnderBarrelCraftingRecipe.STREAM_CODEC));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StorageUpgradeCraftingRecipe>>
            STORAGE_UPGRADE_CRAFTING_RECIPE = RECIPE_SERIALIZERS.register(
                    "storage_upgrade_crafting",
                    () -> new RecipeSerializer<>(
                            StorageUpgradeCraftingRecipe.CODEC, StorageUpgradeCraftingRecipe.STREAM_CODEC));

    // -------------------------------------------------------------------------
    // Creative tab
    // -------------------------------------------------------------------------

    /** Invisible item whose sole purpose is to provide the creative tab icon texture. */
    public static final DeferredHolder<Item, Item> CREATIVE_TAB_ICON = ITEMS.registerItem("creative_tab", Item::new);

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
                        output.accept(BARREL_ITEM.get());
                        output.accept(TANK_ITEM.get());
                        output.accept(ARMORY_CABINET_ITEM.get());
                        output.accept(FILING_CABINET_ITEM.get());

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
                        for (DeferredHolder<Item, StorageUpgradeItem> upgrade : STORAGE_UPGRADES) {
                            StorageUpgradeItem item = upgrade.get();
                            if (item.isStorageOnly()
                                    && !net.bobofraggins.tremendousstorage.shared.config.TremendousStorageConfig
                                            .INCLUDE_NETHER_STAR_TIER_UPGRADE
                                            .get()) continue;
                            output.accept(item);
                        }
                        output.accept(CRAFTING_UPGRADE.get());
                        output.accept(ENDER_STORAGE_UPGRADE.get());
                        output.accept(ENDER_TREMENDOUS_BACKPACK_ITEM.get());
                        output.accept(ENDER_PICNIC_BASKET_ITEM.get());
                        output.accept(ENDER_BARREL_ITEM.get());
                        output.accept(ENDER_TREMENDOUS_CHEST_ITEM.get());
                        output.accept(ENDER_TANK_ITEM.get());
                        output.accept(ENDER_FOLDER.get());
                        output.accept(MAGNET_UPGRADE.get());
                        output.accept(HAARP_UPGRADE.get());
                        output.accept(PULLER_UPGRADE.get());
                        output.accept(INTERDIMENSIONAL_UPGRADE.get());
                        output.accept(COMPACTING_UPGRADE.get());

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

                        // if (ModList.get().isLoaded("mysticalagriculture")) { // disabled - not available on 1.21.4
                        //     var lazurite = MysticalAgricultureAPI.getCropRegistry()
                        //             .getCropById(
                        //                     Identifier.fromNamespaceAndPath(TremendousStorage.MODID,
                        // "lazurite"));
                        //     if (lazurite != null) {
                        //         output.accept(lazurite.getSeedsItem());
                        //         output.accept(lazurite.getEssenceItem());
                        //     }
                        // }
                    })
                    .build());

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Creates a RecipeSerializer for a singleton (no-data) recipe type.
     *
     * <p>StreamCodec.unit requires reference equality when encoding — if the MapCodec creates a
     * new instance during datapack load, encoding fails. This helper shares one instance between
     * both codecs so the same object is always used for equality checks.
     */
    private static <R extends net.minecraft.world.item.crafting.Recipe<?>>
            RecipeSerializer<R> singletonRecipeSerializer(R instance) {
        return new RecipeSerializer<>(
                com.mojang.serialization.MapCodec.unit(instance),
                net.minecraft.network.codec.StreamCodec.unit(instance));
    }

    // -------------------------------------------------------------------------
    // Registration helper called from the mod constructor
    // -------------------------------------------------------------------------

    public static void register(IEventBus modEventBus) {
        if (ModList.get().isLoaded("patchouli")) {
            TREMENDOUS_STORAGE_GUIDE = ITEMS.registerItem(
                    "tremendous_storage_guide", TremendousStorageGuideItem::new, () -> new Item.Properties()
                            .stacksTo(1));
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
                Capabilities.Item.BLOCK, BARREL_BE_TYPE.get(), (be, side) -> new CompactingBarrelItemHandler(be));
        event.registerBlockEntity(
                Capabilities.Item.BLOCK, ENDER_BARREL_BE_TYPE.get(), (be, side) -> new CompactingBarrelItemHandler(be));
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ARMORY_CABINET_BE_TYPE.get(),
                (be, side) -> new net.bobofraggins.tremendousstorage.storage.chest.ChestItemHandler(be));
        event.registerBlockEntity(
                Capabilities.Item.BLOCK, FILING_CABINET_BE_TYPE.get(), (be, side) -> new FilingCabinetItemHandler(be));
        event.registerBlockEntity(
                Capabilities.Item.BLOCK, TREMENDOUS_CHEST_BE_TYPE.get(), (be, side) -> new ChestItemHandler(be));
        event.registerBlockEntity(
                Capabilities.Item.BLOCK, TREMENDOUS_BACKPACK_BE_TYPE.get(), (be, side) -> new ChestItemHandler(be));
        event.registerBlockEntity(
                Capabilities.Item.BLOCK, PICNIC_BASKET_BE_TYPE.get(), (be, side) -> new ChestItemHandler(be));
        event.registerBlockEntity(
                Capabilities.Item.BLOCK, ENDER_PICNIC_BASKET_BE_TYPE.get(), (be, side) -> new ChestItemHandler(be));
        event.registerBlockEntity(
                Capabilities.Item.BLOCK, ENDER_TREMENDOUS_CHEST_BE_TYPE.get(), (be, side) -> new ChestItemHandler(be));
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ENDER_TREMENDOUS_BACKPACK_BE_TYPE.get(),
                (be, side) -> new ChestItemHandler(be));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, TANK_BE_TYPE.get(), (be, side) -> new TankFluidHandler(be));
        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK, ENDER_TANK_BE_TYPE.get(), (be, side) -> new TankFluidHandler(be));
        event.registerItem(Capabilities.Fluid.ITEM, (stack, ctx) -> new TankItemFluidHandler(stack), TANK_ITEM.get());
        event.registerBlockEntity(Capabilities.Item.BLOCK, TUBE_BE_TYPE.get(), (be, side) -> be.getNetworkView());
        event.registerBlockEntity(
                Capabilities.Energy.BLOCK, TUBE_BE_TYPE.get(), (be, side) -> new TubeEnergyHandler(be));
        event.registerBlockEntity(
                Capabilities.Item.BLOCK, NETWORK_INTERFACE_BE_TYPE.get(), (be, side) -> be.getItemHandler());
        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK, NETWORK_INTERFACE_BE_TYPE.get(), (be, side) -> be.getNiFluidHandler());
        event.registerBlockEntity(
                Capabilities.Energy.BLOCK, NETWORK_INTERFACE_BE_TYPE.get(), (be, side) -> new NiEnergyHandler(be));
        event.registerBlockEntity(
                Capabilities.Energy.BLOCK,
                STIRLING_ENGINE_BE_TYPE.get(),
                (be, side) -> new StirlingEngineEnergyHandler(be));
        event.registerBlockEntity(
                Capabilities.Item.BLOCK, RECYCLING_BIN_BE_TYPE.get(), (be, side) -> new RecyclingBinItemHandler(be));
        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK, RECYCLING_BIN_BE_TYPE.get(), (be, side) -> new RecyclingBinFluidHandler(be));
    }
}
