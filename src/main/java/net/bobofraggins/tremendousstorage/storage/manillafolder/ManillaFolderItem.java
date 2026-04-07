package net.bobofraggins.tremendousstorage.storage.manillafolder;

import java.util.List;
import net.bobofraggins.tremendousstorage.shared.util.CountFormat;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A Manila Folder stores a large quantity of a single item type.
 *
 * <p>The folder is unlocked until the first item is placed inside via the crafting grid
 * ({@link FolderStorageRecipe}). After that it is locked to that item type. Items can be
 * extracted via the crafting grid ({@link FolderExtractRecipe}), and two same-tier same-type
 * folders can be merged together ({@link FolderMergeRecipe}).
 *
 * <p>All storage state lives in the {@link FolderContents} data component on the ItemStack,
 * including the tier which determines capacity.
 */
public class ManillaFolderItem extends Item {

    public ManillaFolderItem(Properties properties) {
        super(properties);
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

    public static long getCapacity(ItemStack stack) {
        return getContents(stack).getCapacity();
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return getContents(stack).isEmpty() ? 64 : 1;
    }

    @Override
    public Component getName(ItemStack stack) {
        FolderContents contents = getContents(stack);
        String tierId = contents.tier().getId();
        return Component.translatable(getDescriptionId())
                .append(Component.literal(" ("))
                .append(Component.translatable("tier.tremendousstorage." + tierId))
                .append(Component.literal(")"));
    }

    // -------------------------------------------------------------------------
    // Tooltip
    // -------------------------------------------------------------------------

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        FolderContents contents = getContents(stack);
        if (contents.isEmpty()) {
            lines.add(Component.translatable("item.tremendousstorage.manila_folder.empty"));
        } else {
            ItemStack stored = contents.storedItem().get();
            lines.add(Component.translatable(
                    "item.tremendousstorage.manila_folder.contents",
                    CountFormat.format(contents.count()),
                    stored.getHoverName()));
        }
        lines.add(Component.translatable(
                "item.tremendousstorage.manila_folder.capacity", CountFormat.format(contents.getCapacity())));
    }
}
