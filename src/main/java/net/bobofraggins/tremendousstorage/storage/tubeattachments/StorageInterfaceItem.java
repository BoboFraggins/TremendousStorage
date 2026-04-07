package net.bobofraggins.tremendousstorage.storage.tubeattachments;

import net.minecraft.world.item.Item;

/**
 * The Storage Interface item. Consumed when placed on a tube face (right-click).
 * Recovered when the attachment is punched off or the tube is broken.
 */
public class StorageInterfaceItem extends Item {

    public StorageInterfaceItem() {
        super(new Item.Properties());
    }
}
