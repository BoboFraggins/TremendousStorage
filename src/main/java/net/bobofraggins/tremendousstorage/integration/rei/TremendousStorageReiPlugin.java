package net.bobofraggins.tremendousstorage.integration.rei;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.forge.REIPluginClient;
import net.bobofraggins.tremendousstorage.storage.accessterminal.AccessTerminalMenu;

/**
 * REI plugin entry point for TremendousStorage.
 *
 * <p>This class is loaded exclusively by REI's own class scanner via {@link REIPluginClient}.
 * It is never referenced from TremendousStorage's own startup code, so it causes no
 * {@link ClassNotFoundException} when REI is absent.
 */
@REIPluginClient
public class TremendousStorageReiPlugin implements REIClientPlugin {

    @Override
    public void registerTransferHandlers(TransferHandlerRegistry registry) {
        registry.register(new TerminalReiTransferHandler(AccessTerminalMenu.class));
    }
}
