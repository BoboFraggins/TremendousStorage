package net.bobofraggins.tremendousstorage.external.jade;

import net.bobofraggins.tremendousstorage.storage.chest.ChestBlock;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlockEntity;
import net.bobofraggins.tremendousstorage.storage.filingcabinet.FilingCabinetBlock;
import net.bobofraggins.tremendousstorage.storage.filingcabinet.FilingCabinetBlockEntity;
import net.bobofraggins.tremendousstorage.storage.tank.TankBlock;
import net.bobofraggins.tremendousstorage.storage.tank.TankBlockEntity;
import net.bobofraggins.tremendousstorage.storage.tube.TubeBlock;
import net.bobofraggins.tremendousstorage.storage.tube.TubeBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/** Jade plugin for TremendousStorage blocks. */
@WailaPlugin
public class TubeJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration reg) {
        reg.registerBlockDataProvider(TubeJadeDataProvider.INSTANCE, TubeBlockEntity.class);
        reg.registerBlockDataProvider(StorageTierJadeDataProvider.INSTANCE, ChestBlockEntity.class);
        reg.registerBlockDataProvider(StorageTierJadeDataProvider.INSTANCE, TankBlockEntity.class);
        reg.registerBlockDataProvider(StorageTierJadeDataProvider.INSTANCE, FilingCabinetBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration reg) {
        reg.registerBlockComponent(TubeJadeComponentProvider.INSTANCE, TubeBlock.class);
        reg.registerBlockComponent(StorageTierJadeComponentProvider.INSTANCE, ChestBlock.class);
        reg.registerBlockComponent(StorageTierJadeComponentProvider.INSTANCE, TankBlock.class);
        reg.registerBlockComponent(StorageTierJadeComponentProvider.INSTANCE, FilingCabinetBlock.class);
    }
}
