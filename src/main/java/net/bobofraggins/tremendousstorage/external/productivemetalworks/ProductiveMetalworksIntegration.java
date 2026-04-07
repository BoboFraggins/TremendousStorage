package net.bobofraggins.tremendousstorage.external.productivemetalworks;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ProductiveMetalworksIntegration {

    private static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, TremendousStorage.MODID);

    private static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, TremendousStorage.MODID);

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TremendousStorage.MODID);

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TremendousStorage.MODID);

    public static final DeferredHolder<FluidType, FluidType> MOLTEN_LAZURITE_TYPE = FLUID_TYPES.register(
            "molten_lazurite",
            () -> new FluidType(
                    FluidType.Properties.create().density(3000).viscosity(6000).temperature(1300)));

    public static final DeferredHolder<Fluid, MoltenLazuriteFluid.Source> MOLTEN_LAZURITE_SOURCE =
            FLUIDS.register("molten_lazurite", MoltenLazuriteFluid.Source::new);

    public static final DeferredHolder<Fluid, MoltenLazuriteFluid.Flowing> MOLTEN_LAZURITE_FLOWING =
            FLUIDS.register("molten_lazurite_flowing", MoltenLazuriteFluid.Flowing::new);

    public static final DeferredBlock<LiquidBlock> MOLTEN_LAZURITE_BLOCK = BLOCKS.register(
            "molten_lazurite",
            () -> new LiquidBlock(
                    MOLTEN_LAZURITE_SOURCE.get(),
                    BlockBehaviour.Properties.of()
                            .noCollission()
                            .strength(100f)
                            .noLootTable()
                            .liquid()
                            .replaceable()
                            .pushReaction(PushReaction.DESTROY)
                            .lightLevel(state -> 15)));

    public static final DeferredHolder<Item, BucketItem> MOLTEN_LAZURITE_BUCKET = ITEMS.register(
            "molten_lazurite_bucket",
            () -> new BucketItem(
                    MOLTEN_LAZURITE_SOURCE.get(),
                    new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));

    /** Shared properties object for the Molten Lazurite Source + Flowing fluids. */
    public static final BaseFlowingFluid.Properties MOLTEN_LAZURITE_FLUID_PROPS = new BaseFlowingFluid.Properties(
                    MOLTEN_LAZURITE_TYPE, () -> MOLTEN_LAZURITE_SOURCE.get(), () -> MOLTEN_LAZURITE_FLOWING.get())
            .bucket(() -> MOLTEN_LAZURITE_BUCKET.get())
            .block(() -> MOLTEN_LAZURITE_BLOCK.get())
            .slopeFindDistance(2)
            .levelDecreasePerBlock(1)
            .tickRate(20);

    public static void register(IEventBus modBus) {
        FLUID_TYPES.register(modBus);
        FLUIDS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(ProductiveMetalworksIntegration::addCreativeTabItems);
    }

    private static void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        ResourceKey<CreativeModeTab> tab = ResourceKey.create(
                Registries.CREATIVE_MODE_TAB,
                ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "tremendousstorage"));
        if (event.getTabKey().equals(tab)) {
            event.accept(MOLTEN_LAZURITE_BUCKET.get());
        }
    }
}
