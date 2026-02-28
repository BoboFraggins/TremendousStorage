package net.bobofraggins.intellistore.external.arsnouveau;

import com.hollingsworth.arsnouveau.api.source.ISourceCap;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import net.bobofraggins.intellistore.IntelliStore;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers all Source Tank content (block, item, block entity, data component, capability).
 *
 * <p>This class is only instantiated when Ars Nouveau is present (guarded in
 * {@link net.bobofraggins.intellistore.shared.register.Registration#register}), so all Ars Nouveau
 * API imports are safe here.
 */
public final class SourceTankRegistration {

    private SourceTankRegistration() {}

    // -------------------------------------------------------------------------
    // Deferred registers (use the same mod-id as the main mod)
    // -------------------------------------------------------------------------

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(IntelliStore.MODID);

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(IntelliStore.MODID);

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, IntelliStore.MODID);

    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, IntelliStore.MODID);

    // -------------------------------------------------------------------------
    // Registered objects (public so other classes can reference them)
    // -------------------------------------------------------------------------

    public static DeferredBlock<SourceTankBlock> SOURCE_TANK;
    public static DeferredHolder<Item, BlockItem> SOURCE_TANK_ITEM;
    public static DeferredHolder<BlockEntityType<?>, BlockEntityType<SourceTankBlockEntity>> SOURCE_TANK_BE_TYPE;
    public static DeferredHolder<DataComponentType<?>, DataComponentType<SourceTankContents>> SOURCE_TANK_CONTENTS;

    // -------------------------------------------------------------------------
    // Entry point
    // -------------------------------------------------------------------------

    /**
     * Called from {@code Registration.register()} only when Ars Nouveau is loaded.
     * Registers all deferred objects and subscribes to capability + renderer events.
     */
    public static void register(IEventBus modBus) {
        SOURCE_TANK = BLOCKS.register(
                "source_tank",
                () -> new SourceTankBlock(BlockBehaviour.Properties.of()
                        .strength(3.0f, 1000.0f)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.GLASS)
                        .noOcclusion()));

        SOURCE_TANK_ITEM = ITEMS.registerSimpleBlockItem("source_tank", SOURCE_TANK);

        SOURCE_TANK_BE_TYPE = BLOCK_ENTITY_TYPES.register(
                "source_tank", () -> BlockEntityType.Builder.of(SourceTankBlockEntity::new, SOURCE_TANK.get())
                        .build(null));

        SOURCE_TANK_CONTENTS =
                DATA_COMPONENTS.register("source_tank_contents", () -> DataComponentType.<SourceTankContents>builder()
                        .persistent(SourceTankContents.CODEC)
                        .networkSynchronized(SourceTankContents.STREAM_CODEC)
                        .build());

        // Register deferred registers onto the mod bus
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        DATA_COMPONENTS.register(modBus);

        // Subscribe to capability and renderer events
        modBus.addListener(SourceTankRegistration::registerCapabilities);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(SourceTankRegistration::registerRenderers);
        }
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        BlockCapability<ISourceCap, Direction> sourceCap = CapabilityRegistry.SOURCE_CAPABILITY;
        event.registerBlockEntity(
                sourceCap, SOURCE_TANK_BE_TYPE.get(), (be, side) -> new SourceTankSourceCapHandler(be));
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(SOURCE_TANK_BE_TYPE.get(), SourceTankRenderer::new);
    }
}
