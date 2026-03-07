package net.bobofraggins.intellistore.external.jade;

import net.bobofraggins.intellistore.storage.tube.TubeBlock;
import net.bobofraggins.intellistore.storage.tube.TubeBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade plugin for IntelliStore tubes.
 *
 * <p>When the player looks at a tube face that has an attachment installed, the Jade HUD
 * replaces "Tube" with the attachment's name (e.g. "Storage Interface") and shows
 * its item icon.
 */
@WailaPlugin
public class TubeJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration reg) {
        reg.registerBlockDataProvider(TubeJadeDataProvider.INSTANCE, TubeBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration reg) {
        reg.registerBlockComponent(TubeJadeComponentProvider.INSTANCE, TubeBlock.class);
    }
}
