package net.bobofraggins.tremendousstorage.storage.manillafolder;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;

/**
 * Renders the folder's locked item as a small overlay in the bottom-right corner of the
 * folder's item icon. The stored item is drawn at half scale (8×8 screen pixels) so it
 * fits inside the 16×16 slot without obscuring the folder texture.
 */
public class FolderItemDecorator implements IItemDecorator {

    @Override
    public boolean render(GuiGraphics guiGraphics, Font font, ItemStack stack, int xOffset, int yOffset) {
        FolderContents contents = stack.getOrDefault(Registration.FOLDER_CONTENTS.get(), FolderContents.EMPTY);
        if (contents.storedItem().isEmpty()) return false;

        ItemStack display = contents.storedItem().get().copyWithCount(1);

        var pose = guiGraphics.pose();
        pose.pushPose();
        // Translate to bottom-right quadrant of the 16x16 slot and scale to half size
        pose.translate(xOffset + 8, yOffset + 8, 300);
        pose.scale(0.5f, 0.5f, 1.0f);
        guiGraphics.renderItem(display, 0, 0);
        pose.popPose();

        return false;
    }
}
