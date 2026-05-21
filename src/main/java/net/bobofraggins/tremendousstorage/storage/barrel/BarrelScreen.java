package net.bobofraggins.tremendousstorage.storage.barrel;

import net.bobofraggins.tremendousstorage.shared.network.SetPriorityPacket;
import net.bobofraggins.tremendousstorage.shared.network.SetVoidExcessPacket;
import net.bobofraggins.tremendousstorage.shared.ui.ConfigDrawer;
import net.bobofraggins.tremendousstorage.shared.ui.Dialog;
import net.bobofraggins.tremendousstorage.shared.ui.IDialogPane;
import net.bobofraggins.tremendousstorage.shared.ui.PlayerInventoryPane;
import net.bobofraggins.tremendousstorage.shared.ui.PriorityPane;
import net.bobofraggins.tremendousstorage.shared.ui.PullerSidesPane;
import net.bobofraggins.tremendousstorage.shared.ui.VoidExcessPane;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class BarrelScreen extends AbstractContainerScreen<BarrelMenu> {

    private final Dialog dialog;
    private final ConfigDrawer configDrawer;

    public BarrelScreen(BarrelMenu menu, Inventory inv, Component title) {
        Dialog dialog_ = new Dialog(
                Dialog.blankPane(188, 0),
                new VoidExcessPane(
                        menu::isVoidExcess,
                        () -> ClientPacketDistributor.sendToServer(
                                new SetVoidExcessPacket(menu.getPos(), !menu.isVoidExcess()))),
                new PriorityPane(
                        () -> menu.getPriority().ordinal(),
                        p -> ClientPacketDistributor.sendToServer(new SetPriorityPacket(menu.getPos(), p))),
                Dialog.blankPane(0, 10),
                new PlayerInventoryPane());
        super(menu, inv, title, dialog_.totalWidth(), dialog_.totalHeight());
        dialog = dialog_;

        java.util.List<IDialogPane> drawerPanes = new java.util.ArrayList<>();
        if (menu.hasPullerUpgrade()) {
            drawerPanes.add(new PullerSidesPane(menu.getPos()));
        }
        configDrawer = new ConfigDrawer(drawerPanes.toArray(IDialogPane[]::new));
    }

    @Override
    protected void init() {
        super.init();
        dialog.init(leftPos, topPos);
        configDrawer.init(leftPos, topPos, imageHeight);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor gfx, int mx, int my, float partialTick) {
        dialog.render(gfx, font, title, mx, my, partialTick);
        configDrawer.render(gfx, font, mx, my, partialTick);
        configDrawer.renderTab(gfx, mx, my);
        super.extractContents(gfx, mx, my, partialTick);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor gfx, int mx, int my) {
        // Title is drawn by Dialog. Draw only the player inventory label.
        gfx.text(
                font,
                Component.translatable("container.inventory"),
                BarrelMenu.INV_LEFT,
                BarrelMenu.INV_TOP - 10,
                0x404040,
                false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mx, int my, float partialTick) {
        extractBackground(gfx, mx, my, partialTick);
        super.extractRenderState(gfx, mx, my, partialTick);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean consumed) {
        double mx = event.x();
        double my = event.y();
        int button = event.button();

        if (configDrawer.mouseClicked(mx, my, button)) return true;
        if (dialog.mouseClicked(mx, my, button)) return true;
        return super.mouseClicked(event, consumed);
    }
}
