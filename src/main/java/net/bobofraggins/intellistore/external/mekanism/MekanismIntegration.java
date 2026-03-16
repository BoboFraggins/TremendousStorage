package net.bobofraggins.intellistore.external.mekanism;

import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalBuilder;
import net.bobofraggins.intellistore.IntelliStore;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MekanismIntegration {

    private static final int LAZURITE_TINT = 0x2E558A;

    private static final DeferredRegister<Chemical> CHEMICALS =
            DeferredRegister.create(MekanismAPI.CHEMICAL_REGISTRY_NAME, IntelliStore.MODID);

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(IntelliStore.MODID);

    public static final Holder<Chemical> DIRTY_LAZURITE = CHEMICALS.register(
            "dirty_lazurite", () -> new Chemical(ChemicalBuilder.dirtySlurry().tint(LAZURITE_TINT)));

    public static final Holder<Chemical> CLEAN_LAZURITE = CHEMICALS.register(
            "clean_lazurite", () -> new Chemical(ChemicalBuilder.cleanSlurry().tint(LAZURITE_TINT)));

    public static final DeferredHolder<Item, Item> LAZURITE_DUST =
            ITEMS.register("lazurite_dust", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> LAZURITE_DIRTY_DUST =
            ITEMS.register("lazurite_dirty_dust", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> LAZURITE_CLUMP =
            ITEMS.register("lazurite_clump", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> LAZURITE_SHARD =
            ITEMS.register("lazurite_shard", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> LAZURITE_CRYSTAL =
            ITEMS.register("lazurite_crystal", () -> new Item(new Item.Properties()));

    public static void register(IEventBus modBus) {
        CHEMICALS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(MekanismIntegration::addCreativeTabItems);
    }

    private static void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        ResourceKey<CreativeModeTab> tab = ResourceKey.create(
                Registries.CREATIVE_MODE_TAB,
                ResourceLocation.fromNamespaceAndPath(IntelliStore.MODID, "intellistore"));
        if (event.getTabKey().equals(tab)) {
            event.accept(LAZURITE_DUST.get());
            event.accept(LAZURITE_DIRTY_DUST.get());
            event.accept(LAZURITE_CLUMP.get());
            event.accept(LAZURITE_SHARD.get());
            event.accept(LAZURITE_CRYSTAL.get());
        }
    }
}
