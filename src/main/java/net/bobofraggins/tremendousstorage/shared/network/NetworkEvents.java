package net.bobofraggins.tremendousstorage.shared.network;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Registers all server-bound network payloads for TremendousStorage. */
@EventBusSubscriber(modid = TremendousStorage.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class NetworkEvents {

    private NetworkEvents() {}

    @SubscribeEvent
    static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(SetPriorityPacket.TYPE, SetPriorityPacket.STREAM_CODEC, SetPriorityPacket::handle);
        registrar.playToServer(SetVoidExcessPacket.TYPE, SetVoidExcessPacket.STREAM_CODEC, SetVoidExcessPacket::handle);
        registrar.playToServer(
                ClearTankContentsPacket.TYPE, ClearTankContentsPacket.STREAM_CODEC, ClearTankContentsPacket::handle);
        registrar.playToServer(
                SetStorageInterfacePriorityPacket.TYPE,
                SetStorageInterfacePriorityPacket.STREAM_CODEC,
                SetStorageInterfacePriorityPacket::handle);
        registrar.playToServer(
                RequestNetworkContentsPacket.TYPE,
                RequestNetworkContentsPacket.STREAM_CODEC,
                RequestNetworkContentsPacket::handle);
        registrar.playToClient(
                NetworkContentsPacket.TYPE, NetworkContentsPacket.STREAM_CODEC, NetworkContentsPacket::handle);
        registrar.playToServer(
                RequestSatContentsPacket.TYPE, RequestSatContentsPacket.STREAM_CODEC, RequestSatContentsPacket::handle);
        registrar.playToClient(SatContentsPacket.TYPE, SatContentsPacket.STREAM_CODEC, SatContentsPacket::handle);
        registrar.playToServer(SatExtractPacket.TYPE, SatExtractPacket.STREAM_CODEC, SatExtractPacket::handle);
        registrar.playToServer(SatInsertPacket.TYPE, SatInsertPacket.STREAM_CODEC, SatInsertPacket::handle);
        registrar.playToServer(
                OpenPersonalAccessTerminalPacket.TYPE,
                OpenPersonalAccessTerminalPacket.STREAM_CODEC,
                OpenPersonalAccessTerminalPacket::handle);
        registrar.playToServer(
                SetImportExportFilterPacket.TYPE,
                SetImportExportFilterPacket.STREAM_CODEC,
                SetImportExportFilterPacket::handle);
        registrar.playToClient(
                SyncInterfaceFilterPacket.TYPE,
                SyncInterfaceFilterPacket.STREAM_CODEC,
                SyncInterfaceFilterPacket::handle);
        registrar.playToServer(
                LocalStorageInteractPacket.TYPE,
                LocalStorageInteractPacket.STREAM_CODEC,
                LocalStorageInteractPacket::handle);
        registrar.playToServer(QuickStackPacket.TYPE, QuickStackPacket.STREAM_CODEC, QuickStackPacket::handle);
        registrar.playToServer(
                QuickStackFilingCabinetPacket.TYPE,
                QuickStackFilingCabinetPacket.STREAM_CODEC,
                QuickStackFilingCabinetPacket::handle);
        registrar.playToServer(SetSortModePacket.TYPE, SetSortModePacket.STREAM_CODEC, SetSortModePacket::handle);
        registrar.playToServer(
                SatFillCraftingGridPacket.TYPE,
                SatFillCraftingGridPacket.STREAM_CODEC,
                SatFillCraftingGridPacket::handle);
        registrar.playToServer(
                OpenBackpackPacket.TYPE,
                OpenBackpackPacket.STREAM_CODEC,
                OpenBackpackPacket::handle);
        registrar.playToServer(
                BackpackInteractPacket.TYPE,
                BackpackInteractPacket.STREAM_CODEC,
                BackpackInteractPacket::handle);
        registrar.playToServer(
                SetBackpackPriorityPacket.TYPE,
                SetBackpackPriorityPacket.STREAM_CODEC,
                SetBackpackPriorityPacket::handle);
        registrar.playToServer(
                SetBackpackSortModePacket.TYPE,
                SetBackpackSortModePacket.STREAM_CODEC,
                SetBackpackSortModePacket::handle);
    }
}
