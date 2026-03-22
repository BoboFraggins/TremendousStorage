package net.bobofraggins.intellistore.storage.tubeattachments;

import net.bobofraggins.intellistore.shared.network.SetStorageInterfacePriorityPacket;
import net.bobofraggins.intellistore.shared.ui.Dialog;
import net.bobofraggins.intellistore.shared.ui.PriorityPane;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Priority screen for a Storage Interface attachment on a Tube face.
 *
 * <p>Shows the attachment title and a priority control using the standard {@link Dialog} background
 * and {@link PriorityPane}.
 */
public class StorageInterfaceScreen extends AbstractContainerScreen<StorageInterfaceMenu> {

    private final Dialog dialog;

    public StorageInterfaceScreen(StorageInterfaceMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        PriorityPane priorityPane = new PriorityPane(
                menu::getPriority,
                p -> PacketDistributor.sendToServer(
                        new SetStorageInterfacePriorityPacket(menu.getPos(), menu.getFaceIndex(), p)));
        dialog = new Dialog(priorityPane);
        this.imageWidth = dialog.totalWidth();
        this.imageHeight = dialog.totalHeight();
    }

    @Override
    protected void init() {
        super.init();
        dialog.init(leftPos, topPos);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        dialog.render(graphics, font, title, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (dialog.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Title is drawn by Dialog.
    }
}
