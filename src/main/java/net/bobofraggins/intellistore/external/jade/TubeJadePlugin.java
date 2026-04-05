package net.bobofraggins.intellistore.external.jade;

import net.bobofraggins.intellistore.storage.bulkstorage.BulkStorageContainerBlock;
import net.bobofraggins.intellistore.storage.bulkstorage.BulkStorageContainerBlockEntity;
import net.bobofraggins.intellistore.storage.fluidtank.FluidTankBlock;
import net.bobofraggins.intellistore.storage.fluidtank.FluidTankBlockEntity;
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
        reg.registerBlockDataProvider(StorageTierJadeDataProvider.INSTANCE, BulkStorageContainerBlockEntity.class);
        reg.registerBlockDataProvider(StorageTierJadeDataProvider.INSTANCE, FluidTankBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration reg) {
        reg.registerBlockComponent(TubeJadeComponentProvider.INSTANCE, TubeBlock.class);
        reg.registerBlockComponent(StorageTierJadeComponentProvider.INSTANCE, BulkStorageContainerBlock.class);
        reg.registerBlockComponent(StorageTierJadeComponentProvider.INSTANCE, FluidTankBlock.class);
    }
}
