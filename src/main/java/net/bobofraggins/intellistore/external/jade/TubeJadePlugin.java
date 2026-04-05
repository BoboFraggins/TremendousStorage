package net.bobofraggins.intellistore.external.jade;

import net.bobofraggins.intellistore.storage.tremendouschest.TremendousChestBlock;
import net.bobofraggins.intellistore.storage.tremendouschest.TremendousChestBlockEntity;
import net.bobofraggins.intellistore.storage.tremendoustank.TremendousTankBlock;
import net.bobofraggins.intellistore.storage.tremendoustank.TremendousTankBlockEntity;
import net.bobofraggins.intellistore.storage.tube.TubeBlock;
import net.bobofraggins.intellistore.storage.tube.TubeBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/** Jade plugin for IntelliStore blocks. */
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
