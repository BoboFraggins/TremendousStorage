package net.bobofraggins.intellistore.whiteout;

import net.minecraft.world.item.Item;

/**
 * Whiteout Tape — a limited-use crafting ingredient that unlocks a locked-but-empty
 * Manila Folder, resetting it to accept any item type again.
 *
 * <p>Each use costs one point of durability. At 32 durability the tape has 32 uses;
 * when it would reach max damage it is consumed entirely instead of returning a broken item.
 */
public class WhiteoutTapeItem extends Item {

    public static final int MAX_DURABILITY = 32;

    public WhiteoutTapeItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .durability(MAX_DURABILITY));
    }
}
