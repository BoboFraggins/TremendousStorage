package net.bobofraggins.tremendousstorage.external.mobgrindinutils;

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

/**
 * Handles XP fluid integration.
 *
 * <p>When Mob Grinding Utils is present its {@code mob_grinding_utils:fluid_xp} fluid is used
 * directly. The XP Juice fallback fluid is always registered; it activates when MGU is absent or
 * does not expose {@code fluid_xp}. Client-side rendering is handled by {@link XpFluidClientEvents}.
 */
public final class MobGrindingUtilsIntegration {

    private static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, TremendousStorage.MODID);

    private static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, TremendousStorage.MODID);

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TremendousStorage.MODID);

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TremendousStorage.MODID);

    public static final DeferredHolder<FluidType, FluidType> XP_FLUID_TYPE = FLUID_TYPES.register(
            "xp_juice",
            () -> new FluidType(
                    FluidType.Properties.create().density(900).viscosity(1500).temperature(300)));

    public static final DeferredHolder<Fluid, XpFluid.Source> XP_FLUID_SOURCE =
            FLUIDS.register("xp_juice", XpFluid.Source::new);

    public static final DeferredHolder<Fluid, XpFluid.Flowing> XP_FLUID_FLOWING =
            FLUIDS.register("xp_juice_flowing", XpFluid.Flowing::new);

    public static final DeferredBlock<LiquidBlock> XP_FLUID_BLOCK = BLOCKS.register(
            "xp_juice",
            () -> new LiquidBlock(
                    XP_FLUID_SOURCE.get(),
                    BlockBehaviour.Properties.of()
                            .noCollission()
                            .strength(100f)
                            .noLootTable()
                            .liquid()
                            .replaceable()
                            .pushReaction(PushReaction.DESTROY)));

    public static final DeferredHolder<Item, BucketItem> XP_FLUID_BUCKET = ITEMS.register(
            "xp_juice_bucket",
            () -> new BucketItem(
                    XP_FLUID_SOURCE.get(), new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));

    /** Shared properties object referenced by {@link XpFluid}. */
    public static final BaseFlowingFluid.Properties XP_FLUID_PROPS = new BaseFlowingFluid.Properties(
                    XP_FLUID_TYPE, () -> XP_FLUID_SOURCE.get(), () -> XP_FLUID_FLOWING.get())
            .bucket(() -> XP_FLUID_BUCKET.get())
            .block(() -> XP_FLUID_BLOCK.get())
            .slopeFindDistance(2)
            .levelDecreasePerBlock(1)
            .tickRate(20);

    public static Fluid getXpFluid() {
        return XP_FLUID_SOURCE.get();
    }

    /** Called unconditionally from {@link TremendousStorage} so XP Juice is always available. */
    public static void register(IEventBus modBus) {
        FLUID_TYPES.register(modBus);
        FLUIDS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(MobGrindingUtilsIntegration::addCreativeTabItems);
    }

    private static void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        ResourceKey<CreativeModeTab> tab = ResourceKey.create(
                Registries.CREATIVE_MODE_TAB,
                ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "tremendousstorage"));
        if (event.getTabKey().equals(tab)) {
            event.accept(XP_FLUID_BUCKET.get());
        }
    }
}
