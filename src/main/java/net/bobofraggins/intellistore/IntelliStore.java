package net.bobofraggins.intellistore;

import com.mojang.logging.LogUtils;
import net.bobofraggins.intellistore.shared.config.IntelliStoreConfig;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(IntelliStore.MODID)
public class IntelliStore {
    public static final String MODID = "intellistore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public IntelliStore(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, IntelliStoreConfig.SPEC, "intellistore-common.toml");
        Registration.register(modEventBus);
        LOGGER.info("IntelliStore initialized");
    }
}
