package net.bobofraggins.tremendousstorage.storage.barrel;

import net.bobofraggins.tremendousstorage.shared.network.SetPriorityPacket;
import net.bobofraggins.tremendousstorage.shared.network.SetVoidExcessPacket;
import net.bobofraggins.tremendousstorage.shared.ui.ConfigDrawer;
import net.bobofraggins.tremendousstorage.shared.ui.Dialog;
import net.bobofraggins.tremendousstorage.shared.ui.PlayerInventoryPane;
import net.bobofraggins.tremendousstorage.shared.ui.PriorityPane;
import net.bobofraggins.tremendousstorage.shared.ui.VoidExcessPane;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class BarrelScreen extends AbstractContainerScreen<BarrelMenu> {

    private final Dialog dialog;
    private final ConfigDrawer configDrawer;

    public BarrelScreen(BarrelMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);

        dialog = new Dialog(Dialog.blankPane(PlayerInventoryPane.WIDTH, 10), new PlayerInventoryPane());
        imageWidth = dialog.totalWidth();
        imageHeight = dialog.totalHeight();

        configDrawer = new ConfigDrawer(
                new VoidExcessPane(
                        menu::isVoidExcess,
                        () -> PacketDistributor.sendToServer(
                                new SetVoidExcessPacket(menu.getPos(), !menu.isVoidExcess()))),
                new PriorityPane(
                        () -> menu.getPriority().ordinal(),
                        p -> PacketDistributor.sendToServer(new SetPriorityPacket(menu.getPos(), p))));
    }

    @Override
    protected void init() {
        super.init();
        dialog.init(leftPos, topPos);
        configDrawer.init(leftPos, topPos, imageHeight);
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mx, int my) {
        configDrawer.render(gfx, font, mx, my, partialTick);
        configDrawer.renderTab(gfx, mx, my);
        dialog.render(gfx, font, title, mx, my, partialTick);
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mx, int my) {
        // Title is drawn by Dialog. Draw only the player inventory label.
        gfx.drawString(font, Component.translatable("container.inventory"), BarrelMenu.INV_LEFT, 17, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics gfx, int mx, int my, float partialTick) {
        renderBackground(gfx, mx, my, partialTick);
        super.render(gfx, mx, my, partialTick);
        renderTooltip(gfx, mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (configDrawer.mouseClicked(mx, my, button)) return true;
        return super.mouseClicked(mx, my, button);
    }
}
