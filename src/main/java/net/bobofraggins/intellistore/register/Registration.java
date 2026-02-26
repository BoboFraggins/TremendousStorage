package net.bobofraggins.intellistore.register;

import java.util.EnumMap;
import java.util.Map;
import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.filingcabinet.FilingCabinetBlock;
import net.bobofraggins.intellistore.filingcabinet.FilingCabinetBlockEntity;
import net.bobofraggins.intellistore.filingcabinet.FilingCabinetItemHandler;
import net.bobofraggins.intellistore.manillafolder.FolderContents;
import net.bobofraggins.intellistore.manillafolder.FolderExtractRecipe;
import net.bobofraggins.intellistore.manillafolder.FolderMergeRecipe;
import net.bobofraggins.intellistore.manillafolder.FolderStorageRecipe;
import net.bobofraggins.intellistore.manillafolder.FolderTier;
import net.bobofraggins.intellistore.manillafolder.ManillaFolderItem;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

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

    // -------------------------------------------------------------------------
    // Data components
    // -------------------------------------------------------------------------

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FolderContents>> FOLDER_CONTENTS =
            DATA_COMPONENTS.register("folder_contents", () -> DataComponentType.<FolderContents>builder()
                    .persistent(FolderContents.CODEC)
                    .networkSynchronized(FolderContents.STREAM_CODEC)
                    .build());

    // -------------------------------------------------------------------------
    // Blocks + block entities
    // -------------------------------------------------------------------------

    public static final DeferredBlock<FilingCabinetBlock> FILING_CABINET =
            BLOCKS.register(
                    "filing_cabinet",
                    () -> new FilingCabinetBlock(BlockBehaviour.Properties.of()
                            .strength(5.0f, 1000.0f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.WOOD)));

    public static final DeferredHolder<Item, BlockItem> FILING_CABINET_ITEM =
            ITEMS.registerSimpleBlockItem("filing_cabinet", FILING_CABINET);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FilingCabinetBlockEntity>>
            FILING_CABINET_BE_TYPE = BLOCK_ENTITY_TYPES.register(
                    "filing_cabinet",
                    () -> BlockEntityType.Builder
                            .of(FilingCabinetBlockEntity::new, FILING_CABINET.get())
                            .build(null));

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

    // -------------------------------------------------------------------------
    // Creative tab
    // -------------------------------------------------------------------------

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> INTELLISTORE_TAB =
            CREATIVE_MODE_TABS.register("intellistore", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.intellistore"))
                    .icon(() -> MANILA_FOLDERS.get(FolderTier.IRON).get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(FILING_CABINET_ITEM.get());
                        for (FolderTier tier : FolderTier.values()) {
                            output.accept(MANILA_FOLDERS.get(tier).get());
                        }
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
        modEventBus.addListener(Registration::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                FILING_CABINET_BE_TYPE.get(),
                (be, side) -> new FilingCabinetItemHandler(be));
    }
}
