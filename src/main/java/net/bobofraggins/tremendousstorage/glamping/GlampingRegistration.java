package net.bobofraggins.tremendousstorage.glamping;

import com.mojang.serialization.MapCodec;
import net.bobofraggins.tremendousstorage.TremendousStorage;
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
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TremendousStorage.MODID);

    private static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATOR_TYPES =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, TremendousStorage.MODID);

    // -------------------------------------------------------------------------
    // Tent block
    // -------------------------------------------------------------------------

    public static final DeferredBlock<TentBlock> TENT =
            BLOCKS.registerBlock("tent", TentBlock::new, () -> BlockBehaviour.Properties.of()
                    .strength(0.5f, 0.5f)
                    .sound(SoundType.WOOL)
                    .noOcclusion());

    public static final DeferredHolder<Item, BlockItem> TENT_ITEM = ITEMS.registerSimpleBlockItem("tent", TENT);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TentBlockEntity>> TENT_BE_TYPE =
            BLOCK_ENTITY_TYPES.register("tent", () -> new BlockEntityType<>(TentBlockEntity::new, TENT.get()));

    // -------------------------------------------------------------------------
    // Tent door block
    // -------------------------------------------------------------------------

    public static final DeferredBlock<TentDoorBlock> TENT_DOOR =
            BLOCKS.registerBlock("tent_door", TentDoorBlock::new, () -> BlockBehaviour.Properties.of()
                    .strength(-1.0f, 3600000.0f)
                    .sound(SoundType.WOOL)
                    .noOcclusion());

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
