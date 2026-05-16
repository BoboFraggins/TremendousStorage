package net.bobofraggins.tremendousstorage.storage.tubeattachments;

import net.bobofraggins.tremendousstorage.shared.network.SetStorageInterfacePriorityPacket;
import net.bobofraggins.tremendousstorage.shared.ui.Dialog;
import net.bobofraggins.tremendousstorage.shared.ui.PlayerInventoryPane;
import net.bobofraggins.tremendousstorage.shared.ui.PriorityPane;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Priority screen for a Storage Interface attachment on a Tube face.
 *
 * <p>Shows the attachment title and a priority control using the standard {@link Dialog} background
 * and {@link PriorityPane}.
 */
public class StorageInterfaceScreen extends AbstractContainerScreen<StorageInterfaceMenu> {

    private final Dialog dialog;

    public StorageInterfaceScreen(StorageInterfaceMenu menu, Inventory inv, Component title) {
        PriorityPane priorityPane = new PriorityPane(
                menu::getPriority,
                p -> ClientPacketDistributor.sendToServer(
                        new SetStorageInterfacePriorityPacket(menu.getPos(), menu.getFaceIndex(), p)));
        Dialog dialog_ = new Dialog(priorityPane, new PlayerInventoryPane());
        super(menu, inv, title, dialog_.totalWidth(), dialog_.totalHeight());
        dialog = dialog_;
    }

    @Override
    protected void init() {
        super.init();
        dialog.init(leftPos, topPos);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractBackground(graphics, mouseX, mouseY, partialTick);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        dialog.render(graphics, font, title, mouseX, mouseY, partialTick);
        super.extractContents(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean consumed) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        if (dialog.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(event, consumed);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Title is drawn by Dialog.
    }
}
