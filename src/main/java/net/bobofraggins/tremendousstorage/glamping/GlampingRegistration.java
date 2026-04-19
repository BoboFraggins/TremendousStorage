package net.bobofraggins.tremendousstorage.glamping;

import com.mojang.serialization.MapCodec;
import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.glamping.skyblock.SkyBlock;
import net.bobofraggins.tremendousstorage.glamping.skyblock.SkyBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class GlampingRegistration {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TremendousStorage.MODID);

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TremendousStorage.MODID);

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, TremendousStorage.MODID);

    private static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATOR_TYPES =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, TremendousStorage.MODID);

    // -------------------------------------------------------------------------
    // Tent block
    // -------------------------------------------------------------------------

    public static final DeferredBlock<TentBlock> TENT = BLOCKS.register(
            "tent",
            () -> new TentBlock(BlockBehaviour.Properties.of()
                    .strength(0.5f, 0.5f)
                    .sound(SoundType.WOOL)
                    .noOcclusion()));

    public static final DeferredHolder<Item, BlockItem> TENT_ITEM = ITEMS.registerSimpleBlockItem("tent", TENT);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TentBlockEntity>> TENT_BE_TYPE =
            BLOCK_ENTITY_TYPES.register("tent", () -> BlockEntityType.Builder.of(TentBlockEntity::new, TENT.get())
                    .build(null));

    // -------------------------------------------------------------------------
    // Sky block
    // -------------------------------------------------------------------------

    public static final DeferredBlock<SkyBlock> SKY_BLOCK = BLOCKS.register(
            "sky_block",
            () -> new SkyBlock(BlockBehaviour.Properties.of()
                    .strength(0.5f, 0.5f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.GLASS)));

    public static final DeferredHolder<Item, BlockItem> SKY_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem("sky_block", SKY_BLOCK);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SkyBlockEntity>> SKY_BLOCK_BE_TYPE =
            BLOCK_ENTITY_TYPES.register(
                    "sky_block", () -> BlockEntityType.Builder.of(SkyBlockEntity::new, SKY_BLOCK.get())
                            .build(null));

    // -------------------------------------------------------------------------
    // Tent door block
    // -------------------------------------------------------------------------

    public static final DeferredBlock<TentDoorBlock> TENT_DOOR = BLOCKS.register(
            "tent_door",
            () -> new TentDoorBlock(BlockBehaviour.Properties.of()
                    .strength(-1.0f, 3600000.0f)
                    .sound(SoundType.WOOL)
                    .noOcclusion()));

    public static final DeferredHolder<Item, BlockItem> TENT_DOOR_ITEM =
            ITEMS.registerSimpleBlockItem("tent_door", TENT_DOOR);

    // -------------------------------------------------------------------------
    // Chunk generator codec
    // -------------------------------------------------------------------------

    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<GlampingChunkGenerator>>
            GLAMPING_GENERATOR_TYPE = CHUNK_GENERATOR_TYPES.register("glamping", () -> GlampingChunkGenerator.CODEC);

    // -------------------------------------------------------------------------

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        CHUNK_GENERATOR_TYPES.register(modEventBus);
    }
}
