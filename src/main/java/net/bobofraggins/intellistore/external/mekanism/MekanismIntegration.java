package net.bobofraggins.intellistore.external.mekanism;

import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalBuilder;
import net.bobofraggins.intellistore.IntelliStore;
import net.minecraft.core.Holder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MekanismIntegration {

    private static final int LAZURITE_TINT = 0x2E558A;

    private static final DeferredRegister<Chemical> CHEMICALS =
            DeferredRegister.create(MekanismAPI.CHEMICAL_REGISTRY_NAME, IntelliStore.MODID);

    public static final Holder<Chemical> DIRTY_LAZURITE = CHEMICALS.register(
            "dirty_lazurite", () -> new Chemical(ChemicalBuilder.dirtySlurry().tint(LAZURITE_TINT)));

    public static final Holder<Chemical> CLEAN_LAZURITE = CHEMICALS.register(
            "clean_lazurite", () -> new Chemical(ChemicalBuilder.cleanSlurry().tint(LAZURITE_TINT)));

    public static void register(IEventBus modBus) {
        CHEMICALS.register(modBus);
    }
}
