package net.bobofraggins.intellistore.manillafolder;

import java.util.List;
import net.bobofraggins.intellistore.util.CountFormat;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A Manila Folder stores a large quantity of a single item type.
 *
 * <p>The folder is unlocked until the first item is placed inside via the crafting grid
 * ({@link FolderStorageRecipe}). After that it is locked to that item type. Items can be
 * extracted via the crafting grid ({@link FolderExtractRecipe}), and two same-type folders can
 * be merged together ({@link FolderMergeRecipe}).
 *
 * <p>All storage state lives in the {@link FolderContents} data component on the ItemStack.
 */
public class ManillaFolderItem extends Item {

    private final FolderTier tier;

    public ManillaFolderItem(FolderTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public FolderTier getTier() {
        return tier;
    }

    // -------------------------------------------------------------------------
    // Convenience accessors for folder state
    // -------------------------------------------------------------------------

    public static FolderContents getContents(ItemStack stack) {
        return stack.getOrDefault(FolderContents.type(), FolderContents.EMPTY);
    }

    public static ItemStack setContents(ItemStack stack, FolderContents contents) {
        ItemStack copy = stack.copy();
        copy.set(FolderContents.type(), contents);
        return copy;
    }

    public long getCapacity() {
        return tier.getDefaultCapacity();
    }

    // -------------------------------------------------------------------------
    // Tooltip
    // -------------------------------------------------------------------------

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        FolderContents contents = getContents(stack);
        if (contents.isEmpty()) {
            lines.add(Component.translatable("item.intellistore.manila_folder.empty"));
        } else {
            ItemStack stored = contents.storedItem().get();
            lines.add(Component.translatable(
                    "item.intellistore.manila_folder.contents",
                    CountFormat.format(contents.count()),
                    stored.getHoverName()));
        }
        lines.add(Component.translatable(
                "item.intellistore.manila_folder.capacity",
                CountFormat.format(tier.getDefaultCapacity())));
    }
}
