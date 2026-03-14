package net.bobofraggins.intellistore.external.mekanism;

import mekanism.api.chemical.IChemicalHandler;
import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.shared.storage.StorageTier;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
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
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers all Gas Tank content (block, item, block entity, data component, capability).
 *
 * <p>This class is only instantiated when Mekanism is present (guarded in
 * {@link net.bobofraggins.intellistore.shared.register.Registration#register}), so all Mekanism
 * API imports are safe here.
 */
public final class GasTankRegistration {

    private GasTankRegistration() {}

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

    public static DeferredBlock<GasTankBlock> GAS_TANK;
    public static DeferredHolder<Item, BlockItem> GAS_TANK_ITEM;
    public static DeferredHolder<BlockEntityType<?>, BlockEntityType<GasTankBlockEntity>> GAS_TANK_BE_TYPE;
    public static DeferredHolder<DataComponentType<?>, DataComponentType<GasTankContents>> GAS_TANK_CONTENTS;

    /**
     * The Mekanism {@code IChemicalHandler} block capability, looked up by resource location.
     * Both Mekanism and our code call {@code BlockCapability.createSided()} with the same
     * name + type, so they get the same capability object (NeoForge deduplicates by name).
     */
    static final BlockCapability<IChemicalHandler, Direction> CHEMICAL_BLOCK_CAP = BlockCapability.createSided(
            ResourceLocation.fromNamespaceAndPath("mekanism", "chemical_handler"), IChemicalHandler.class);

    // -------------------------------------------------------------------------
    // Entry point
    // -------------------------------------------------------------------------

    /**
     * Called from {@code Registration.register()} only when Mekanism is loaded.
     * Registers all deferred objects and subscribes to capability + renderer events.
     */
    public static void register(IEventBus modBus) {
        // Create registrations
        GAS_TANK = BLOCKS.register(
                "gas_tank",
                () -> new GasTankBlock(BlockBehaviour.Properties.of()
                        .strength(3.0f, 1000.0f)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.GLASS)
                        .noOcclusion()));

        GAS_TANK_ITEM = ITEMS.registerSimpleBlockItem("gas_tank", GAS_TANK);

        GAS_TANK_BE_TYPE = BLOCK_ENTITY_TYPES.register(
                "gas_tank", () -> BlockEntityType.Builder.of(GasTankBlockEntity::new, GAS_TANK.get())
                        .build(null));

        GAS_TANK_CONTENTS =
                DATA_COMPONENTS.register("gas_tank_contents", () -> DataComponentType.<GasTankContents>builder()
                        .persistent(GasTankContents.CODEC)
                        .networkSynchronized(GasTankContents.STREAM_CODEC)
                        .build());

        // Register deferred registers onto the mod bus
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        DATA_COMPONENTS.register(modBus);

        // Subscribe to capability and renderer events
        modBus.addListener(GasTankRegistration::registerCapabilities);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(GasTankRegistration::registerRenderers);
            modBus.addListener(GasTankRegistration::registerBlockColors);
        }
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                CHEMICAL_BLOCK_CAP, GAS_TANK_BE_TYPE.get(), (be, side) -> new GasTankChemicalHandler(be));
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(GAS_TANK_BE_TYPE.get(), GasTankRenderer::new);
    }

    private static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register(
                (state, level, pos, tintIndex) -> {
                    if (tintIndex != 0 || level == null || pos == null) return -1;
                    if (level.getBlockEntity(pos) instanceof GasTankBlockEntity be) {
                        return be.getTier().getColor();
                    }
                    return StorageTier.PAPER.getColor();
                },
                GAS_TANK.get());
    }
}
