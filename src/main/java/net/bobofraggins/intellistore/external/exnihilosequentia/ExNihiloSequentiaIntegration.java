package net.bobofraggins.intellistore.external.exnihilosequentia;

import net.bobofraggins.intellistore.IntelliStore;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ExNihiloSequentiaIntegration {

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(IntelliStore.MODID);

    public static final DeferredHolder<Item, Item> LAZURITE_PIECES =
            ITEMS.register("lazurite_pieces", () -> new Item(new Item.Properties()));

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(ExNihiloSequentiaIntegration::addCreativeTabItems);
    }

    private static void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        ResourceKey<CreativeModeTab> tab = ResourceKey.create(
                Registries.CREATIVE_MODE_TAB,
                ResourceLocation.fromNamespaceAndPath(IntelliStore.MODID, "intellistore"));
        if (event.getTabKey().equals(tab)) {
            event.accept(LAZURITE_PIECES.get());
        }
    }
}
