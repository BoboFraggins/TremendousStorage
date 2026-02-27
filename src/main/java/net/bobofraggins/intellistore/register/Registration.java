package net.bobofraggins.intellistore.register;

import java.util.EnumMap;
import java.util.Map;
import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.bulkstorage.BulkStorageContainerBlock;
import net.bobofraggins.intellistore.bulkstorage.BulkStorageContainerBlockEntity;
import net.bobofraggins.intellistore.bulkstorage.BulkStorageContainerItemHandler;
import net.bobofraggins.intellistore.filingcabinet.FilingCabinetBlock;
import net.bobofraggins.intellistore.filingcabinet.FilingCabinetBlockEntity;
import net.bobofraggins.intellistore.filingcabinet.FilingCabinetItemHandler;
import net.bobofraggins.intellistore.fluidtank.FluidTankBlock;
import net.bobofraggins.intellistore.fluidtank.FluidTankBlockEntity;
import net.bobofraggins.intellistore.fluidtank.FluidTankContents;
import net.bobofraggins.intellistore.fluidtank.FluidTankFluidHandler;
import net.bobofraggins.intellistore.healingsalve.BrainItem;
import net.bobofraggins.intellistore.healingsalve.HealingSalveBlock;
import net.bobofraggins.intellistore.healingsalve.HealingSalveCauldronBlock;
import net.bobofraggins.intellistore.healingsalve.HealingSalveFluid;
import net.bobofraggins.intellistore.healingsalve.HealingSalveInteractions;
import net.bobofraggins.intellistore.healingsalve.ZombieBrainItem;
import net.bobofraggins.intellistore.junkdrawer.JunkDrawerBlock;
import net.bobofraggins.intellistore.junkdrawer.JunkDrawerBlockEntity;
import net.bobofraggins.intellistore.junkdrawer.JunkDrawerItemHandler;
import net.bobofraggins.intellistore.manillafolder.FolderContents;
import net.bobofraggins.intellistore.manillafolder.FolderExtractRecipe;
import net.bobofraggins.intellistore.manillafolder.FolderMergeRecipe;
import net.bobofraggins.intellistore.manillafolder.FolderStorageRecipe;
import net.bobofraggins.intellistore.manillafolder.FolderTier;
import net.bobofraggins.intellistore.manillafolder.ManillaFolderItem;
import net.bobofraggins.intellistore.networkinterface.NetworkInterfaceBlock;
import net.bobofraggins.intellistore.networkinterface.NetworkInterfaceBlockEntity;
import net.bobofraggins.intellistore.storagetransceiver.StorageAccessTerminalBlock;
import net.bobofraggins.intellistore.tube.TubeBlock;
import net.bobofraggins.intellistore.tube.TubeBlockEntity;
import net.bobofraggins.intellistore.ui.FilingCabinetMenu;
import net.bobofraggins.intellistore.ui.NetworkInterfaceMenu;
import net.bobofraggins.intellistore.ui.PriorityMenu;
import net.bobofraggins.intellistore.ui.StorageAccessTerminalMenu;
import net.bobofraggins.intellistore.ui.StorageInterfaceMenu;
import net.bobofraggins.intellistore.whiteout.FolderTapeRecipe;
import net.bobofraggins.intellistore.whiteout.WhiteoutTapeItem;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
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
            ITEMS.registerSimpleBlockItem("junk_drawer", JUNK_DRAWER);

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

    public static final DeferredHolder<Item, BlockItem> BULK_STORAGE_CONTAINER_ITEM =
            ITEMS.registerSimpleBlockItem("bulk_storage_container", BULK_STORAGE_CONTAINER);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BulkStorageContainerBlockEntity>>
            BULK_STORAGE_CONTAINER_BE_TYPE = BLOCK_ENTITY_TYPES.register(
                    "bulk_storage_container",
                    () -> BlockEntityType.Builder.of(
                                    BulkStorageContainerBlockEntity::new, BULK_STORAGE_CONTAINER.get())
                            .build(null));

    public static final DeferredBlock<FluidTankBlock> FLUID_TANK = BLOCKS.register(
            "fluid_tank",
            () -> new FluidTankBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f, 1000.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.GLASS)));

    public static final DeferredHolder<Item, BlockItem> FLUID_TANK_ITEM =
            ITEMS.registerSimpleBlockItem("fluid_tank", FLUID_TANK);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidTankBlockEntity>> FLUID_TANK_BE_TYPE =
            BLOCK_ENTITY_TYPES.register(
                    "fluid_tank", () -> BlockEntityType.Builder.of(FluidTankBlockEntity::new, FLUID_TANK.get())
                            .build(null));

    // -------------------------------------------------------------------------
    // Healing Salve fluid type + fluids + fluid block + cauldron + items
    // -------------------------------------------------------------------------

    public static final DeferredHolder<FluidType, FluidType> HEALING_SALVE_TYPE =
            FLUID_TYPE_REGISTER.register("healing_salve", () ->
                    new FluidType(FluidType.Properties.create()
                            .density(2000)
                            .viscosity(6000)
                            .temperature(320)));

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

    public static final DeferredHolder<Item, BrainItem> BRAIN =
            ITEMS.register("brain", BrainItem::new);

    public static final DeferredHolder<Item, BucketItem> HEALING_SALVE_BUCKET =
            ITEMS.register("healing_salve_bucket",
                    () -> new BucketItem(HEALING_SALVE_SOURCE.get(),
                            new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));

    /** Shared properties object for the Healing Salve Source + Flowing fluids. */
    public static final BaseFlowingFluid.Properties HEALING_SALVE_FLUID_PROPS =
            new BaseFlowingFluid.Properties(
                    HEALING_SALVE_TYPE,
                    () -> HEALING_SALVE_SOURCE.get(),
                    () -> HEALING_SALVE_FLOWING.get())
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
            NETWORK_INTERFACE_BE_TYPE = BLOCK_ENTITY_TYPES.register(
                    "network_interface",
                    () -> BlockEntityType.Builder.of(
                                    NetworkInterfaceBlockEntity::new, NETWORK_INTERFACE.get())
                            .build(null));

    // -------------------------------------------------------------------------
    // Storage Access Terminal block
    // -------------------------------------------------------------------------

    public static final DeferredBlock<StorageAccessTerminalBlock> STORAGE_ACCESS_TERMINAL =
            BLOCKS.register("storage_access_terminal",
                    () -> new StorageAccessTerminalBlock(BlockBehaviour.Properties.of()
                            .strength(2.5f, 1000f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.WOOD)));

    public static final DeferredHolder<Item, BlockItem> STORAGE_ACCESS_TERMINAL_ITEM =
            ITEMS.registerSimpleBlockItem("storage_access_terminal", STORAGE_ACCESS_TERMINAL);

    // -------------------------------------------------------------------------
    // Tubes (16 colored variants + shared block entity type)
    // -------------------------------------------------------------------------

    public static final Map<DyeColor, DeferredBlock<TubeBlock>> TUBES = new EnumMap<>(DyeColor.class);
    public static final Map<DyeColor, DeferredHolder<Item, BlockItem>> TUBE_ITEMS = new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor color : DyeColor.values()) {
            String name = color.getName() + "_tube";
            DeferredBlock<TubeBlock> block = BLOCKS.register(name,
                    () -> new TubeBlock(color, BlockBehaviour.Properties.of()
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
                TubeBlock[] blocks = TUBES.values().stream()
                        .map(DeferredBlock::get)
                        .toArray(TubeBlock[]::new);
                return BlockEntityType.Builder.of(TubeBlockEntity::new, blocks).build(null);
            });

    // -------------------------------------------------------------------------
    // Menu types
    // -------------------------------------------------------------------------

    public static final DeferredHolder<MenuType<?>, MenuType<FilingCabinetMenu>> FILING_CABINET_MENU =
            MENU_TYPES.register("filing_cabinet",
                    () -> IMenuTypeExtension.create(FilingCabinetMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<PriorityMenu>> PRIORITY_MENU =
            MENU_TYPES.register("priority",
                    () -> IMenuTypeExtension.create(PriorityMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<StorageInterfaceMenu>> STORAGE_INTERFACE_MENU =
            MENU_TYPES.register("storage_interface",
                    () -> IMenuTypeExtension.create(StorageInterfaceMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<NetworkInterfaceMenu>> NETWORK_INTERFACE_MENU =
            MENU_TYPES.register("network_interface",
                    () -> IMenuTypeExtension.create(NetworkInterfaceMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<StorageAccessTerminalMenu>>
            STORAGE_ACCESS_TERMINAL_MENU = MENU_TYPES.register("storage_access_terminal",
                    () -> IMenuTypeExtension.create(StorageAccessTerminalMenu::new));

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
                            tier.getId() + "_manila_folder",
                            () -> new ManillaFolderItem(tier, new Item.Properties().stacksTo(1))));
        }
    }

    // -------------------------------------------------------------------------
    // Recipe serializers
    // -------------------------------------------------------------------------

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FolderStorageRecipe>>
            FOLDER_STORAGE_RECIPE = RECIPE_SERIALIZERS.register("folder_storage", () -> new RecipeSerializer<>() {
        @Override
        public com.mojang.serialization.MapCodec<FolderStorageRecipe> codec() {
            return FolderStorageRecipe.CODEC;
        }

        @Override
        public net.minecraft.network.codec.StreamCodec<
                        net.minecraft.network.RegistryFriendlyByteBuf, FolderStorageRecipe>
                streamCodec() {
            return FolderStorageRecipe.STREAM_CODEC;
        }
    });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FolderExtractRecipe>>
            FOLDER_EXTRACT_RECIPE = RECIPE_SERIALIZERS.register("folder_extract", () -> new RecipeSerializer<>() {
        @Override
        public com.mojang.serialization.MapCodec<FolderExtractRecipe> codec() {
            return FolderExtractRecipe.CODEC;
        }

        @Override
        public net.minecraft.network.codec.StreamCodec<
                        net.minecraft.network.RegistryFriendlyByteBuf, FolderExtractRecipe>
                streamCodec() {
            return FolderExtractRecipe.STREAM_CODEC;
        }
    });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FolderMergeRecipe>> FOLDER_MERGE_RECIPE =
            RECIPE_SERIALIZERS.register("folder_merge", () -> new RecipeSerializer<>() {
                @Override
                public com.mojang.serialization.MapCodec<FolderMergeRecipe> codec() {
                    return FolderMergeRecipe.CODEC;
                }

                @Override
                public net.minecraft.network.codec.StreamCodec<
                                net.minecraft.network.RegistryFriendlyByteBuf, FolderMergeRecipe>
                        streamCodec() {
                    return FolderMergeRecipe.STREAM_CODEC;
                }
            });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FolderTapeRecipe>> FOLDER_TAPE_RECIPE =
            RECIPE_SERIALIZERS.register("folder_tape", () -> new RecipeSerializer<>() {
                @Override
                public com.mojang.serialization.MapCodec<FolderTapeRecipe> codec() {
                    return FolderTapeRecipe.CODEC;
                }

                @Override
                public net.minecraft.network.codec.StreamCodec<
                                net.minecraft.network.RegistryFriendlyByteBuf, FolderTapeRecipe>
                        streamCodec() {
                    return FolderTapeRecipe.STREAM_CODEC;
                }
            });

    // -------------------------------------------------------------------------
    // Creative tab
    // -------------------------------------------------------------------------

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> INTELLISTORE_TAB =
            CREATIVE_MODE_TABS.register("intellistore", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.intellistore"))
                    .icon(() -> MANILA_FOLDERS.get(FolderTier.IRON).get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(FILING_CABINET_ITEM.get());
                        output.accept(JUNK_DRAWER_ITEM.get());
                        output.accept(BULK_STORAGE_CONTAINER_ITEM.get());
                        output.accept(FLUID_TANK_ITEM.get());
                        output.accept(NETWORK_INTERFACE_ITEM.get());
                        output.accept(STORAGE_ACCESS_TERMINAL_ITEM.get());
                        output.accept(ZOMBIE_BRAIN.get());
                        output.accept(BRAIN.get());
                        output.accept(HEALING_SALVE_BUCKET.get());
                        for (DyeColor color : DyeColor.values()) {
                            output.accept(TUBE_ITEMS.get(color).get());
                        }
                        for (FolderTier tier : FolderTier.values()) {
                            output.accept(MANILA_FOLDERS.get(tier).get());
                        }
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
                Capabilities.ItemHandler.BLOCK,
                NETWORK_INTERFACE_BE_TYPE.get(),
                (be, side) -> be.getItemHandler());
    }
}
