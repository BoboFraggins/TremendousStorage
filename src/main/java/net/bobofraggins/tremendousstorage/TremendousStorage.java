package net.bobofraggins.tremendousstorage;

import com.mojang.logging.LogUtils;
import net.bobofraggins.tremendousstorage.external.create.CreateIntegration;
import net.bobofraggins.tremendousstorage.external.mekanism.MekanismIntegration;
import net.bobofraggins.tremendousstorage.external.productivemetalworks.ProductiveMetalworksIntegration;
import net.bobofraggins.tremendousstorage.external.structurepoolapi.StructurePoolIntegration;
import net.bobofraggins.tremendousstorage.shared.config.TremendousStorageClientConfig;
import net.bobofraggins.tremendousstorage.shared.config.TremendousStorageConfig;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
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
        if (ModList.get().isLoaded("productivemetalworks")) {
            ProductiveMetalworksIntegration.register(modEventBus);
        }
        if (ModList.get().isLoaded("structure_pool_api")) {
            StructurePoolIntegration.register();
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
