package net.bobofraggins.intellistore.shared.register;

import com.blakebr0.mysticalagriculture.api.MysticalAgricultureAPI;
import java.util.EnumMap;
import java.util.Map;
import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.external.arsnouveau.SourceTankRegistration;
import net.bobofraggins.intellistore.external.exdeorum.ExDeorumIntegration;
import net.bobofraggins.intellistore.external.exnihilosequentia.ExNihiloSequentiaIntegration;
import net.bobofraggins.intellistore.external.mekanism.GasTankRegistration;
import net.bobofraggins.intellistore.lazurite.LazuriteBarsBlock;
import net.bobofraggins.intellistore.lazurite.LazuriteOreBlock;
import net.bobofraggins.intellistore.lazurite.LazuritePaxelItem;
import net.bobofraggins.intellistore.lazurite.LazuriteTier;
import net.bobofraggins.intellistore.power.stirlingengine.StirlingEngineBlock;
import net.bobofraggins.intellistore.power.stirlingengine.StirlingEngineBlockEntity;
import net.bobofraggins.intellistore.power.stirlingengine.StirlingEngineEnergyHandler;
import net.bobofraggins.intellistore.shared.config.IntelliStoreConfig;
import net.bobofraggins.intellistore.shared.loot.LootModifiers;
import net.bobofraggins.intellistore.shared.storage.StorageTier;
import net.bobofraggins.intellistore.shared.storage.TieredBlockItem;
import net.bobofraggins.intellistore.shared.ui.PriorityControl;
import net.bobofraggins.intellistore.shared.ui.TankSettingsMenu;
import net.bobofraggins.intellistore.storage.accessterminal.AccessTerminalBlock;
import net.bobofraggins.intellistore.storage.accessterminal.AccessTerminalBlockEntity;
import net.bobofraggins.intellistore.storage.accessterminal.AccessTerminalMenu;
import net.bobofraggins.intellistore.storage.bulkstorage.BulkStorageContainerBlock;
import net.bobofraggins.intellistore.storage.bulkstorage.BulkStorageContainerBlockEntity;
import net.bobofraggins.intellistore.storage.bulkstorage.BulkStorageContainerItemHandler;
import net.bobofraggins.intellistore.storage.bulkstorage.BulkStorageContainerMenu;
import net.bobofraggins.intellistore.storage.enderfolder.EnderFolderItem;
import net.bobofraggins.intellistore.storage.enderfolder.EnderFolderRecipe;
import net.bobofraggins.intellistore.storage.filingcabinet.FilingCabinetBlock;
import net.bobofraggins.intellistore.storage.filingcabinet.FilingCabinetBlockEntity;
import net.bobofraggins.intellistore.storage.filingcabinet.FilingCabinetItemHandler;
import net.bobofraggins.intellistore.storage.filingcabinet.FilingCabinetMenu;
import net.bobofraggins.intellistore.storage.fluidtank.FluidTankBlock;
import net.bobofraggins.intellistore.storage.fluidtank.FluidTankBlockEntity;
import net.bobofraggins.intellistore.storage.fluidtank.FluidTankContents;
import net.bobofraggins.intellistore.storage.fluidtank.FluidTankFluidHandler;
import net.bobofraggins.intellistore.storage.items.BrainItem;
import net.bobofraggins.intellistore.storage.items.HealingSalveBlock;
import net.bobofraggins.intellistore.storage.items.HealingSalveCauldronBlock;
import net.bobofraggins.intellistore.storage.items.HealingSalveFluid;
import net.bobofraggins.intellistore.storage.items.HealingSalveInteractions;
import net.bobofraggins.intellistore.storage.items.ZombieBrainItem;
import net.bobofraggins.intellistore.storage.junkdrawer.JunkDrawerBlock;
import net.bobofraggins.intellistore.storage.junkdrawer.JunkDrawerBlockEntity;
import net.bobofraggins.intellistore.storage.junkdrawer.JunkDrawerItemHandler;
import net.bobofraggins.intellistore.storage.junkdrawer.JunkDrawerMenu;
import net.bobofraggins.intellistore.storage.manillafolder.FolderContents;
import net.bobofraggins.intellistore.storage.manillafolder.FolderExtractRecipe;
import net.bobofraggins.intellistore.storage.manillafolder.FolderMergeRecipe;
import net.bobofraggins.intellistore.storage.manillafolder.FolderStorageRecipe;
import net.bobofraggins.intellistore.storage.manillafolder.FolderTier;
import net.bobofraggins.intellistore.storage.manillafolder.FolderUpgradeRecipe;
import net.bobofraggins.intellistore.storage.manillafolder.ManillaFolderItem;
import net.bobofraggins.intellistore.storage.networkinterface.NetworkInterfaceBlock;
import net.bobofraggins.intellistore.storage.networkinterface.NetworkInterfaceBlockEntity;
import net.bobofraggins.intellistore.storage.networkinterface.NetworkInterfaceMenu;
import net.bobofraggins.intellistore.storage.networkinterface.NiEnergyHandler;
import net.bobofraggins.intellistore.storage.personalaccessterminal.PersonalAccessTerminalItem;
import net.bobofraggins.intellistore.storage.personalfilingcabinet.PersonalFilingCabinetContents;
import net.bobofraggins.intellistore.storage.personalfilingcabinet.PersonalFilingCabinetEvents;
import net.bobofraggins.intellistore.storage.personalfilingcabinet.PersonalFilingCabinetItem;
import net.bobofraggins.intellistore.storage.personalfilingcabinet.PersonalFilingCabinetMenu;
import net.bobofraggins.intellistore.storage.storageupgrade.StorageBlockUpgradeRecipe;
import net.bobofraggins.intellistore.storage.storageupgrade.StorageUpgradeItem;
import net.bobofraggins.intellistore.storage.tube.TubeBlock;
import net.bobofraggins.intellistore.storage.tube.TubeBlockEntity;
import net.bobofraggins.intellistore.storage.tube.TubeEnergyHandler;
import net.bobofraggins.intellistore.storage.tubeattachments.BreakerInterfaceItem;
import net.bobofraggins.intellistore.storage.tubeattachments.BreakerInterfaceMenu;
import net.bobofraggins.intellistore.storage.tubeattachments.ExportInterfaceItem;
import net.bobofraggins.intellistore.storage.tubeattachments.ExportInterfaceMenu;
import net.bobofraggins.intellistore.storage.tubeattachments.ImportInterfaceItem;
import net.bobofraggins.intellistore.storage.tubeattachments.ImportInterfaceMenu;
import net.bobofraggins.intellistore.storage.tubeattachments.InterfaceFilterContents;
import net.bobofraggins.intellistore.storage.tubeattachments.PlacerInterfaceItem;
import net.bobofraggins.intellistore.storage.tubeattachments.PlacerInterfaceMenu;
import net.bobofraggins.intellistore.storage.tubeattachments.StorageInterfaceItem;
import net.bobofraggins.intellistore.storage.tubeattachments.StorageInterfaceMenu;
import net.bobofraggins.intellistore.storage.whiteout.FolderTapeRecipe;
import net.bobofraggins.intellistore.storage.whiteout.WhiteoutTapeItem;
import net.bobofraggins.intellistore.storage.wirelesshub.WirelessHubBlock;
import net.bobofraggins.intellistore.storage.wirelesshub.WirelessHubBlockEntity;
import net.bobofraggins.intellistore.storage.wirelesshub.WirelessHubMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
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

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(IntelliStore.MODID);

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(IntelliStore.MODID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, IntelliStore.MODID);

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE, IntelliStore.MODID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, IntelliStore.MODID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, IntelliStore.MODID);

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, IntelliStore.MODID);

    public static final DeferredRegister<net.minecraft.world.level.material.Fluid> FLUID_REGISTER =
            DeferredRegister.create(Registries.FLUID, IntelliStore.MODID);

    public static final DeferredRegister<FluidType> FLUID_TYPE_REGISTER =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, IntelliStore.MODID);

    // -------------------------------------------------------------------------
    // Data components
    // -------------------------------------------------------------------------

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FolderContents>> FOLDER_CONTENTS =
            DATA_COMPONENTS.register("folder_contents", () -> DataComponentType.<FolderContents>builder()
                    .persistent(FolderContents.CODEC)
                    .networkSynchronized(FolderContents.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FluidTankContents>> FLUID_TANK_CONTENTS =
            DATA_COMPONENTS.register("fluid_tank_contents", () -> DataComponentType.<FluidTankContents>builder()
                    .persistent(FluidTankContents.CODEC)
                    .networkSynchronized(FluidTankContents.STREAM_CODEC)
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

    /**
     * Data component storing the folder contents and void-excess flag on a Personal Filing Cabinet item.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PersonalFilingCabinetContents>>
            PFC_CONTENTS = DATA_COMPONENTS.register(
                    "pfc_contents", () -> DataComponentType.<PersonalFilingCabinetContents>builder()
                            .persistent(PersonalFilingCabinetContents.CODEC)
                            .networkSynchronized(PersonalFilingCabinetContents.STREAM_CODEC)
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

    public static final DeferredBlock<JunkDrawerBlock> JUNK_DRAWER = BLOCKS.register(
            "junk_drawer",
            () -> new JunkDrawerBlock(BlockBehaviour.Properties.of()
                    .strength(5.0f, 1000.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));

    public static final DeferredHolder<Item, BlockItem> JUNK_DRAWER_ITEM =
            ITEMS.register("junk_drawer", () -> new TieredBlockItem(JUNK_DRAWER.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<JunkDrawerBlockEntity>> JUNK_DRAWER_BE_TYPE =
            BLOCK_ENTITY_TYPES.register(
                    "junk_drawer", () -> BlockEntityType.Builder.of(JunkDrawerBlockEntity::new, JUNK_DRAWER.get())
                            .build(null));

    public static final DeferredBlock<BulkStorageContainerBlock> BULK_STORAGE_CONTAINER = BLOCKS.register(
            "bulk_storage_container",
            () -> new BulkStorageContainerBlock(BlockBehaviour.Properties.of()
                    .strength(5.0f, 1000.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));

    public static final DeferredHolder<Item, BlockItem> BULK_STORAGE_CONTAINER_ITEM = ITEMS.register(
            "bulk_storage_container", () -> new TieredBlockItem(BULK_STORAGE_CONTAINER.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BulkStorageContainerBlockEntity>>
            BULK_STORAGE_CONTAINER_BE_TYPE =
                    BLOCK_ENTITY_TYPES.register("bulk_storage_container", () -> BlockEntityType.Builder.of(
                                    BulkStorageContainerBlockEntity::new, BULK_STORAGE_CONTAINER.get())
                            .build(null));

    // -------------------------------------------------------------------------
    // Storage upgrade items
    // -------------------------------------------------------------------------

    public static final DeferredHolder<Item, StorageUpgradeItem> PAPER_TO_COPPER_STORAGE_UPGRADE = ITEMS.register(
            "paper_to_copper_storage_upgrade",
            () -> new StorageUpgradeItem(StorageTier.PAPER, StorageTier.COPPER, new Item.Properties()));

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
        PAPER_TO_COPPER_STORAGE_UPGRADE,
        COPPER_TO_IRON_STORAGE_UPGRADE,
        IRON_TO_GOLD_STORAGE_UPGRADE,
        GOLD_TO_DIAMOND_STORAGE_UPGRADE,
        DIAMOND_TO_EMERALD_STORAGE_UPGRADE,
        EMERALD_TO_NETHERITE_STORAGE_UPGRADE
    };

    public static final DeferredBlock<FluidTankBlock> FLUID_TANK = BLOCKS.register(
            "fluid_tank",
            () -> new FluidTankBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f, 1000.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.GLASS)));

    public static final DeferredHolder<Item, BlockItem> FLUID_TANK_ITEM =
            ITEMS.register("fluid_tank", () -> new TieredBlockItem(FLUID_TANK.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidTankBlockEntity>> FLUID_TANK_BE_TYPE =
            BLOCK_ENTITY_TYPES.register(
                    "fluid_tank", () -> BlockEntityType.Builder.of(FluidTankBlockEntity::new, FLUID_TANK.get())
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
    // Healing Salve fluid type + fluids + fluid block + cauldron + items
    // -------------------------------------------------------------------------

    public static final DeferredHolder<FluidType, FluidType> HEALING_SALVE_TYPE = FLUID_TYPE_REGISTER.register(
            "healing_salve",
            () -> new FluidType(
                    FluidType.Properties.create().density(2000).viscosity(6000).temperature(320)));

    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, HealingSalveFluid.Source>
            HEALING_SALVE_SOURCE = FLUID_REGISTER.register("healing_salve", HealingSalveFluid.Source::new);

    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, HealingSalveFluid.Flowing>
            HEALING_SALVE_FLOWING = FLUID_REGISTER.register("healing_salve_flowing", HealingSalveFluid.Flowing::new);

    public static final DeferredBlock<HealingSalveBlock> HEALING_SALVE_BLOCK = BLOCKS.register(
            "healing_salve",
            () -> new HealingSalveBlock(
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

    public static final DeferredHolder<Item, BucketItem> HEALING_SALVE_BUCKET = ITEMS.register(
            "healing_salve_bucket",
            () -> new BucketItem(
                    HEALING_SALVE_SOURCE.get(),
                    new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));

    /** Shared properties object for the Healing Salve Source + Flowing fluids. */
    public static final BaseFlowingFluid.Properties HEALING_SALVE_FLUID_PROPS = new BaseFlowingFluid.Properties(
                    HEALING_SALVE_TYPE, () -> HEALING_SALVE_SOURCE.get(), () -> HEALING_SALVE_FLOWING.get())
            .bucket(() -> HEALING_SALVE_BUCKET.get())
            .block(() -> HEALING_SALVE_BLOCK.get())
            .slopeFindDistance(2)
            .levelDecreasePerBlock(1)
            .tickRate(30);

    public static final DeferredBlock<HealingSalveCauldronBlock> HEALING_SALVE_CAULDRON = BLOCKS.register(
            "healing_salve_cauldron",
            () -> new HealingSalveCauldronBlock(BlockBehaviour.Properties.of()
                    .strength(2f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredHolder<Item, BlockItem> HEALING_SALVE_CAULDRON_ITEM =
            ITEMS.registerSimpleBlockItem("healing_salve_cauldron", HEALING_SALVE_CAULDRON);

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

    public static final DeferredHolder<Item, BlockItem> NETWORK_INTERFACE_ITEM =
            ITEMS.registerSimpleBlockItem("network_interface", NETWORK_INTERFACE);

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
    // Stirling Engine block + block entity
    // -------------------------------------------------------------------------

    public static final DeferredBlock<StirlingEngineBlock> STIRLING_ENGINE = BLOCKS.register(
            "stirling_engine",
            () -> new StirlingEngineBlock(BlockBehaviour.Properties.of()
                    .strength(2.5f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredHolder<Item, BlockItem> STIRLING_ENGINE_ITEM =
            ITEMS.registerSimpleBlockItem("stirling_engine", STIRLING_ENGINE);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StirlingEngineBlockEntity>>
            STIRLING_ENGINE_BE_TYPE = BLOCK_ENTITY_TYPES.register("stirling_engine", () -> BlockEntityType.Builder.of(
                    StirlingEngineBlockEntity::new, STIRLING_ENGINE.get())
            .build(null));

    // -------------------------------------------------------------------------
    // Tubes (16 colored variants + shared block entity type)
    // -------------------------------------------------------------------------

    public static final Map<DyeColor, DeferredBlock<TubeBlock>> TUBES = new EnumMap<>(DyeColor.class);
    public static final Map<DyeColor, DeferredHolder<Item, BlockItem>> TUBE_ITEMS = new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor color : DyeColor.values()) {
            String name = color.getName() + "_tube";
            DeferredBlock<TubeBlock> block = BLOCKS.register(
                    name,
                    () -> new TubeBlock(
                            color,
                            BlockBehaviour.Properties.of()
                                    .strength(1.5f, 6.0f)
                                    .requiresCorrectToolForDrops()
                                    .sound(SoundType.METAL)
                                    .noOcclusion()));
            TUBES.put(color, block);
            TUBE_ITEMS.put(color, ITEMS.registerSimpleBlockItem(name, block));
        }
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TubeBlockEntity>> TUBE_BE_TYPE =
            BLOCK_ENTITY_TYPES.register("tube", () -> {
                TubeBlock[] blocks =
                        TUBES.values().stream().map(DeferredBlock::get).toArray(TubeBlock[]::new);
                return BlockEntityType.Builder.of(TubeBlockEntity::new, blocks).build(null);
            });

    // -------------------------------------------------------------------------
    // Menu types
    // -------------------------------------------------------------------------

    public static final DeferredHolder<MenuType<?>, MenuType<FilingCabinetMenu>> FILING_CABINET_MENU =
            MENU_TYPES.register("filing_cabinet", () -> IMenuTypeExtension.create(FilingCabinetMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<PriorityControl>> PRIORITY_MENU =
            MENU_TYPES.register("priority", () -> IMenuTypeExtension.create(PriorityControl::new));

    public static final DeferredHolder<MenuType<?>, MenuType<BulkStorageContainerMenu>> BULK_STORAGE_CONTAINER_MENU =
            MENU_TYPES.register(
                    "bulk_storage_container", () -> IMenuTypeExtension.create(BulkStorageContainerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<JunkDrawerMenu>> JUNK_DRAWER_MENU =
            MENU_TYPES.register("junk_drawer", () -> IMenuTypeExtension.create(JunkDrawerMenu::new));

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

    public static final DeferredHolder<MenuType<?>, MenuType<PlacerInterfaceMenu>> PLACER_INTERFACE_MENU =
            MENU_TYPES.register("placer_interface", () -> IMenuTypeExtension.create(PlacerInterfaceMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<BreakerInterfaceMenu>> BREAKER_INTERFACE_MENU =
            MENU_TYPES.register("breaker_interface", () -> IMenuTypeExtension.create(BreakerInterfaceMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<PersonalFilingCabinetMenu>> PERSONAL_FILING_CABINET_MENU =
            MENU_TYPES.register(
                    "personal_filing_cabinet", () -> IMenuTypeExtension.create(PersonalFilingCabinetMenu::new));

    // -------------------------------------------------------------------------
    // Items — storage interface / import interface / export interface
    // -------------------------------------------------------------------------

    public static final DeferredHolder<Item, StorageInterfaceItem> STORAGE_INTERFACE =
            ITEMS.register("storage_interface", StorageInterfaceItem::new);

    public static final DeferredHolder<Item, ImportInterfaceItem> IMPORT_INTERFACE =
            ITEMS.register("import_interface", ImportInterfaceItem::new);

    public static final DeferredHolder<Item, ExportInterfaceItem> EXPORT_INTERFACE =
            ITEMS.register("export_interface", ExportInterfaceItem::new);

    public static final DeferredHolder<Item, PlacerInterfaceItem> PLACER_INTERFACE =
            ITEMS.register("placer_interface", PlacerInterfaceItem::new);

    public static final DeferredHolder<Item, BreakerInterfaceItem> BREAKER_INTERFACE =
            ITEMS.register("breaker_interface", BreakerInterfaceItem::new);

    public static final DeferredHolder<Item, PersonalFilingCabinetItem> PERSONAL_FILING_CABINET =
            ITEMS.register("personal_filing_cabinet", PersonalFilingCabinetItem::new);

    // -------------------------------------------------------------------------
    // Items — whiteout tape
    // -------------------------------------------------------------------------

    public static final DeferredHolder<Item, WhiteoutTapeItem> WHITEOUT_TAPE =
            ITEMS.register("whiteout_tape", WhiteoutTapeItem::new);

    // -------------------------------------------------------------------------
    // Items — one entry per tier
    // -------------------------------------------------------------------------

    public static final Map<FolderTier, DeferredHolder<Item, ManillaFolderItem>> MANILA_FOLDERS =
            new EnumMap<>(FolderTier.class);

    static {
        for (FolderTier tier : FolderTier.values()) {
            MANILA_FOLDERS.put(
                    tier,
                    ITEMS.register(
                            tier.getId() + "_manila_folder", () -> new ManillaFolderItem(tier, new Item.Properties())));
        }
    }

    // -------------------------------------------------------------------------
    // Items — Ender Folders (one per tier)
    // -------------------------------------------------------------------------

    public static final Map<FolderTier, DeferredHolder<Item, EnderFolderItem>> ENDER_FOLDERS =
            new EnumMap<>(FolderTier.class);

    static {
        for (FolderTier tier : FolderTier.values()) {
            ENDER_FOLDERS.put(
                    tier,
                    ITEMS.register(
                            tier.getId() + "_ender_folder",
                            () -> new EnderFolderItem(tier, new Item.Properties().stacksTo(1))));
        }
    }

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

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<EnderFolderRecipe>> ENDER_FOLDER_RECIPE =
            RECIPE_SERIALIZERS.register(
                    "ender_folder", () -> new SimpleCraftingRecipeSerializer<>(EnderFolderRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FolderUpgradeRecipe>>
            FOLDER_UPGRADE_RECIPE = RECIPE_SERIALIZERS.register(
                    "folder_upgrade", () -> new SimpleCraftingRecipeSerializer<>(FolderUpgradeRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StorageBlockUpgradeRecipe>>
            STORAGE_BLOCK_UPGRADE_RECIPE = RECIPE_SERIALIZERS.register(
                    "storage_block_upgrade",
                    () -> new SimpleCraftingRecipeSerializer<>(StorageBlockUpgradeRecipe::new));

    // -------------------------------------------------------------------------
    // Creative tab
    // -------------------------------------------------------------------------

    /** Invisible item whose sole purpose is to provide the creative tab icon texture. */
    public static final DeferredHolder<Item, Item> CREATIVE_TAB_ICON =
            ITEMS.register("creative_tab", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> INTELLISTORE_TAB =
            CREATIVE_MODE_TABS.register("intellistore", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.intellistore"))
                    .icon(() -> FILING_CABINET_ITEM.get().getDefaultInstance())
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
                        output.accept(JUNK_DRAWER_ITEM.get());
                        output.accept(BULK_STORAGE_CONTAINER_ITEM.get());
                        for (DeferredHolder<Item, StorageUpgradeItem> upgrade : STORAGE_UPGRADES) {
                            output.accept(upgrade.get());
                        }
                        output.accept(FLUID_TANK_ITEM.get());
                        if (ModList.get().isLoaded("mekanism")) {
                            output.accept(GasTankRegistration.GAS_TANK_ITEM.get());
                        }
                        if (ModList.get().isLoaded("ars_nouveau")) {
                            output.accept(SourceTankRegistration.SOURCE_TANK_ITEM.get());
                        }
                        if (ModList.get().isLoaded("mysticalagriculture")) {
                            var lazurite = MysticalAgricultureAPI.getCropRegistry()
                                    .getCropById(ResourceLocation.fromNamespaceAndPath(IntelliStore.MODID, "lazurite"));
                            if (lazurite != null) {
                                output.accept(lazurite.getSeedsItem());
                                output.accept(lazurite.getEssenceItem());
                            }
                        }
                        output.accept(NETWORK_INTERFACE_ITEM.get());
                        output.accept(STORAGE_ACCESS_TERMINAL_ITEM.get());
                        output.accept(WIRELESS_HUB_ITEM.get());
                        output.accept(WIRELESS_SAT.get());
                        if (IntelliStoreConfig.STIRLING_ENGINE_ENABLED.get()) {
                            output.accept(STIRLING_ENGINE_ITEM.get());
                        }
                        output.accept(STORAGE_INTERFACE.get());
                        output.accept(IMPORT_INTERFACE.get());
                        output.accept(EXPORT_INTERFACE.get());
                        output.accept(PLACER_INTERFACE.get());
                        output.accept(BREAKER_INTERFACE.get());
                        output.accept(ZOMBIE_BRAIN.get());
                        output.accept(BRAIN.get());
                        output.accept(HEALING_SALVE_BUCKET.get());
                        for (DyeColor color : DyeColor.values()) {
                            output.accept(TUBE_ITEMS.get(color).get());
                        }
                        for (FolderTier tier : FolderTier.values()) {
                            output.accept(MANILA_FOLDERS.get(tier).get());
                        }
                        for (FolderTier tier : FolderTier.values()) {
                            output.accept(ENDER_FOLDERS.get(tier).get());
                        }
                        output.accept(PERSONAL_FILING_CABINET.get());
                        output.accept(WHITEOUT_TAPE.get());
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
        modEventBus.addListener(Registration::registerCapabilities);
        modEventBus.addListener(Registration::onCommonSetup);
        NeoForge.EVENT_BUS.register(PersonalFilingCabinetEvents.class);
        if (ModList.get().isLoaded("exdeorum")) {
            ExDeorumIntegration.register(modEventBus);
        }
        if (ModList.get().isLoaded("exnihilosequentia")) {
            ExNihiloSequentiaIntegration.register(modEventBus);
        }
        if (ModList.get().isLoaded("mekanism")) {
            GasTankRegistration.register(modEventBus);
        }
        if (ModList.get().isLoaded("ars_nouveau")) {
            SourceTankRegistration.register(modEventBus);
        }
        LootModifiers.register(modEventBus);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(HealingSalveInteractions::register);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                FILING_CABINET_BE_TYPE.get(),
                (be, side) -> new FilingCabinetItemHandler(be));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK, JUNK_DRAWER_BE_TYPE.get(), (be, side) -> new JunkDrawerItemHandler(be));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                BULK_STORAGE_CONTAINER_BE_TYPE.get(),
                (be, side) -> new BulkStorageContainerItemHandler(be));
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK, FLUID_TANK_BE_TYPE.get(), (be, side) -> new FluidTankFluidHandler(be));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK, TUBE_BE_TYPE.get(), (be, side) -> be.getNetworkView());
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK, NETWORK_INTERFACE_BE_TYPE.get(), (be, side) -> be.getItemHandler());
        // Energy capabilities
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                NETWORK_INTERFACE_BE_TYPE.get(),
                (be, side) -> new NiEnergyHandler(be));
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK, TUBE_BE_TYPE.get(), (be, side) -> new TubeEnergyHandler(be));
        if (IntelliStoreConfig.STIRLING_ENGINE_ENABLED.get()) {
            event.registerBlockEntity(
                    Capabilities.EnergyStorage.BLOCK,
                    STIRLING_ENGINE_BE_TYPE.get(),
                    (be, side) -> new StirlingEngineEnergyHandler(be));
        }
    }
}
