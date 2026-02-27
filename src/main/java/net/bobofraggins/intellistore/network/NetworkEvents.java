package net.bobofraggins.intellistore.network;

import net.bobofraggins.intellistore.IntelliStore;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Registers all server-bound network payloads for IntelliStore. */
@EventBusSubscriber(modid = IntelliStore.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class NetworkEvents {

    private NetworkEvents() {}

    @SubscribeEvent
    static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                SetPriorityPacket.TYPE,
                SetPriorityPacket.STREAM_CODEC,
                SetPriorityPacket::handle);
        registrar.playToServer(
                ToggleFilingCabinetPacket.TYPE,
                ToggleFilingCabinetPacket.STREAM_CODEC,
                ToggleFilingCabinetPacket::handle);
        registrar.playToServer(
                SetStorageInterfacePriorityPacket.TYPE,
                SetStorageInterfacePriorityPacket.STREAM_CODEC,
                SetStorageInterfacePriorityPacket::handle);
        registrar.playToServer(
                RequestNetworkContentsPacket.TYPE,
                RequestNetworkContentsPacket.STREAM_CODEC,
                RequestNetworkContentsPacket::handle);
        registrar.playToClient(
                NetworkContentsPacket.TYPE,
                NetworkContentsPacket.STREAM_CODEC,
                NetworkContentsPacket::handle);
    }
}
