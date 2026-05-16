package net.bobofraggins.tremendousstorage.external.exnihilosequentia;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ExNihiloSequentiaIntegration {

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TremendousStorage.MODID);

    public static final DeferredHolder<Item, Item> LAZURITE_PIECES =
            ITEMS.register("lazurite_pieces", () -> new Item(new Item.Properties()));

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(ExNihiloSequentiaIntegration::addCreativeTabItems);
    }

    private static void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        ResourceKey<CreativeModeTab> tab = ResourceKey.create(
                Registries.CREATIVE_MODE_TAB,
                Identifier.fromNamespaceAndPath(TremendousStorage.MODID, "tremendousstorage"));
        if (event.getTabKey().equals(tab)) {
            event.accept(LAZURITE_PIECES.get());
        }
    }
}
