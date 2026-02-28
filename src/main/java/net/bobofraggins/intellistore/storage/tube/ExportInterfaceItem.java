package net.bobofraggins.intellistore.storage.tube;

import net.minecraft.world.item.Item;

/** Consumable attachment item that, when installed on a tube face, exports items from the
 *  tube network into the adjacent external inventory. */
public class ExportInterfaceItem extends Item {
    public ExportInterfaceItem() {
        super(new Item.Properties());
    }
}
