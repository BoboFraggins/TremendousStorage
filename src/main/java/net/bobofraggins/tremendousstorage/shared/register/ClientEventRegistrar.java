package net.bobofraggins.tremendousstorage.shared.register;

import net.bobofraggins.tremendousstorage.canvas.CanvasClientEvents;
import net.bobofraggins.tremendousstorage.external.mobgrindinutils.XpFluidClientEvents;
import net.bobofraggins.tremendousstorage.shared.input.QuickStackClientEvents;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackClientEvents;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackClientTickHandler;
import net.bobofraggins.tremendousstorage.storage.chest.ChestClientEvents;
import net.bobofraggins.tremendousstorage.storage.filingcabinet.FilingCabinetClientEvents;
import net.bobofraggins.tremendousstorage.storage.items.PositiveVibesClientEvents;
import net.bobofraggins.tremendousstorage.storage.networkinterface.NetworkInterfaceClientEvents;
import net.bobofraggins.tremendousstorage.storage.personalaccessterminal.PersonalAccessTerminalClientEvents;
import net.bobofraggins.tremendousstorage.storage.personalaccessterminal.PersonalAccessTerminalClientTickHandler;
import net.bobofraggins.tremendousstorage.storage.recyclingbin.RecyclingBinClientEvents;
import net.bobofraggins.tremendousstorage.storage.storageupgrade.StorageClientEvents;
import net.bobofraggins.tremendousstorage.storage.tank.TankClientEvents;
import net.bobofraggins.tremendousstorage.storage.tube.TubeClientEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Registers all client-only event listener classes with their respective event buses.
 *
 * <p>This class is only ever referenced and loaded on the client side — it is called from
 * {@link net.bobofraggins.tremendousstorage.TremendousStorage} behind an
 * {@code FMLEnvironment.dist.isClient()} guard so the server JVM never loads it.
 */
public final class ClientEventRegistrar {

    private ClientEventRegistrar() {}

    /**
     * Registers all client-only event listeners.
     *
     * @param modBus the mod event bus ({@code IEventBus} from the {@code @Mod} constructor)
     */
    public static void register(IEventBus modBus) {
        // ── Mod-bus client events (registration, renderer setup, etc.) ──────────
        modBus.register(ChestClientEvents.class);
        modBus.register(BackpackClientEvents.class);
        modBus.register(FilingCabinetClientEvents.class);
        modBus.register(TankClientEvents.class);
        modBus.register(TubeClientEvents.class);
        modBus.register(NetworkInterfaceClientEvents.class);
        modBus.register(StorageClientEvents.class);
        modBus.register(RecyclingBinClientEvents.class);
        modBus.register(QuickStackClientEvents.class);
        modBus.register(PersonalAccessTerminalClientEvents.class);
        modBus.register(PositiveVibesClientEvents.class);
        modBus.register(CanvasClientEvents.class);
        modBus.register(XpFluidClientEvents.class);

        // ── Game-bus client events (tick handlers, input, etc.) ─────────────────
        NeoForge.EVENT_BUS.register(BackpackClientTickHandler.class);
        NeoForge.EVENT_BUS.register(PersonalAccessTerminalClientTickHandler.class);
    }
}
