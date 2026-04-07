package net.bobofraggins.tremendousstorage.storage.tubeattachments;

import net.minecraft.world.item.Item;

/** Consumable attachment item that, when installed on a tube face, imports items from the
 *  adjacent external inventory into the tube network. */
public class ImportInterfaceItem extends Item {
    public ImportInterfaceItem() {
        super(new Item.Properties());
    }
}
