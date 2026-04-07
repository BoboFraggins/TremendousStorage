package net.bobofraggins.tremendousstorage.external.jade;

import net.bobofraggins.tremendousstorage.storage.tremendouschest.TremendousChestBlock;
import net.bobofraggins.tremendousstorage.storage.tremendouschest.TremendousChestBlockEntity;
import net.bobofraggins.tremendousstorage.storage.tremendoustank.TremendousTankBlock;
import net.bobofraggins.tremendousstorage.storage.tremendoustank.TremendousTankBlockEntity;
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
        reg.registerBlockDataProvider(StorageTierJadeDataProvider.INSTANCE, TremendousChestBlockEntity.class);
        reg.registerBlockDataProvider(StorageTierJadeDataProvider.INSTANCE, TremendousTankBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration reg) {
        reg.registerBlockComponent(TubeJadeComponentProvider.INSTANCE, TubeBlock.class);
        reg.registerBlockComponent(StorageTierJadeComponentProvider.INSTANCE, TremendousChestBlock.class);
        reg.registerBlockComponent(StorageTierJadeComponentProvider.INSTANCE, TremendousTankBlock.class);
    }
}
