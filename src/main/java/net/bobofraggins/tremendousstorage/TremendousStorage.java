package net.bobofraggins.tremendousstorage;

import com.mojang.logging.LogUtils;
import net.bobofraggins.tremendousstorage.experiencesyringe.ExperienceSyringeEvents;
import net.bobofraggins.tremendousstorage.external.create.CreateIntegration;
import net.bobofraggins.tremendousstorage.external.mekanism.MekanismIntegration;
import net.bobofraggins.tremendousstorage.external.mobgrindinutils.MobGrindingUtilsIntegration;
import net.bobofraggins.tremendousstorage.external.productivemetalworks.ProductiveMetalworksIntegration;
import net.bobofraggins.tremendousstorage.glamping.GlampingEvents;
import net.bobofraggins.tremendousstorage.glamping.GlampingRegistration;
import net.bobofraggins.tremendousstorage.glamping.dankfannypack.DankFannyPackMagnetHandler;
import net.bobofraggins.tremendousstorage.glamping.picnicbasket.PicnicBasketFeedHandler;
import net.bobofraggins.tremendousstorage.shared.ItemPickupInterceptor;
import net.bobofraggins.tremendousstorage.shared.config.TremendousStorageClientConfig;
import net.bobofraggins.tremendousstorage.shared.config.TremendousStorageConfig;
import net.bobofraggins.tremendousstorage.shared.network.NetworkEvents;
import net.bobofraggins.tremendousstorage.shared.register.ClientEventRegistrar;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.enderfolder.EnderFolderEvents;
import net.bobofraggins.tremendousstorage.storage.items.LazuriteEquipmentHandler;
import net.bobofraggins.tremendousstorage.storage.items.PositiveVibesEffectHandler;
import net.bobofraggins.tremendousstorage.storage.items.VexRepellentBrewingRecipes;
import net.bobofraggins.tremendousstorage.storage.items.VexRepellentEffectHandler;
import net.bobofraggins.tremendousstorage.storage.items.ZombieBrainDropHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(TremendousStorage.MODID)
public class TremendousStorage {
    public static final String MODID = "tremendousstorage";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TremendousStorage(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(
                ModConfig.Type.COMMON, TremendousStorageConfig.SPEC, "tremendousstorage-common.toml");
        modContainer.registerConfig(
                ModConfig.Type.CLIENT, TremendousStorageClientConfig.SPEC, "tremendousstorage-client.toml");
        Registration.register(modEventBus);
        GlampingRegistration.register(modEventBus);

        // ── Mod-bus registrations (both sides) ───────────────────────────────────
        modEventBus.register(NetworkEvents.class);
        NeoForge.EVENT_BUS.register(VexRepellentBrewingRecipes.class);
        modEventBus.register(ExperienceSyringeEvents.class);

        // ── Game-bus registrations (both sides) ──────────────────────────────────
        NeoForge.EVENT_BUS.register(GlampingEvents.class);
        NeoForge.EVENT_BUS.register(EnderFolderEvents.class);
        NeoForge.EVENT_BUS.register(PicnicBasketFeedHandler.class);
        NeoForge.EVENT_BUS.register(LazuriteEquipmentHandler.class);
        NeoForge.EVENT_BUS.register(VexRepellentEffectHandler.class);
        NeoForge.EVENT_BUS.register(ZombieBrainDropHandler.class);
        NeoForge.EVENT_BUS.register(PositiveVibesEffectHandler.class);
        NeoForge.EVENT_BUS.register(DankFannyPackMagnetHandler.class);
        NeoForge.EVENT_BUS.register(ItemPickupInterceptor.class);

        // ── Client-only event registrations ──────────────────────────────────────
        if (FMLEnvironment.dist.isClient()) {
            ClientEventRegistrar.register(modEventBus);
        }

        MobGrindingUtilsIntegration.register(modEventBus);
        if (ModList.get().isLoaded("productivemetalworks")) {
            ProductiveMetalworksIntegration.register(modEventBus);
        }
        if (ModList.get().isLoaded("create")) {
            CreateIntegration.register(modEventBus);
        }
        if (ModList.get().isLoaded("mekanism")) {
            MekanismIntegration.register(modEventBus);
        }
        LOGGER.info("TremendousStorage initialized");
    }
}
