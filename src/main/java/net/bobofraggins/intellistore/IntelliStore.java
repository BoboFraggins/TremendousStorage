package net.bobofraggins.intellistore;

import com.mojang.logging.LogUtils;
import net.bobofraggins.intellistore.external.create.CreateIntegration;
import net.bobofraggins.intellistore.external.productivemetalworks.ProductiveMetalworksIntegration;
import net.bobofraggins.intellistore.external.structurepoolapi.StructurePoolIntegration;
import net.bobofraggins.intellistore.shared.config.IntelliStoreClientConfig;
import net.bobofraggins.intellistore.shared.config.IntelliStoreConfig;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(IntelliStore.MODID)
public class IntelliStore {
    public static final String MODID = "intellistore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public IntelliStore(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, IntelliStoreConfig.SPEC, "intellistore-common.toml");
        modContainer.registerConfig(ModConfig.Type.CLIENT, IntelliStoreClientConfig.SPEC, "intellistore-client.toml");
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
        LOGGER.info("IntelliStore initialized");
    }
}
