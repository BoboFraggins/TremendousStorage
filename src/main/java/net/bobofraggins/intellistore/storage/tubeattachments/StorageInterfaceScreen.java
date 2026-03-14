package net.bobofraggins.intellistore.storage.tubeattachments;

import net.bobofraggins.intellistore.shared.network.SetStorageInterfacePriorityPacket;
import net.bobofraggins.intellistore.shared.priority.Priority;
import net.bobofraggins.intellistore.shared.ui.ConfigDrawer;
import net.bobofraggins.intellistore.shared.ui.PriorityPane;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Priority screen for a Storage Interface attachment on a Tube face.
 *
 * <p>Background panel: 176 x 17 pixels (title bar only). The priority control lives in the
 * slide-out config drawer opened by the "≡" button in the top-left of the title bar.
 */
public class StorageInterfaceScreen extends AbstractContainerScreen<StorageInterfaceMenu> {

    private static final ResourceLocation BG_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    private static final int BG_WIDTH = 176;
    private static final int BG_HEIGHT = 17;

    private final ConfigDrawer configDrawer;

    public StorageInterfaceScreen(StorageInterfaceMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = BG_WIDTH;
        this.imageHeight = BG_HEIGHT;
        configDrawer = new ConfigDrawer(new StorageInterfacePriorityPane(menu));
    }

    @Override
    protected void init() {
        super.init();
        configDrawer.init(leftPos, topPos, BG_HEIGHT);

        addRenderableWidget(Button.builder(Component.literal("\u2261"), btn -> configDrawer.toggle())
                .bounds(leftPos + 3, topPos + 2, 20, 13)
                .build());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (configDrawer.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        configDrawer.render(graphics, font, mouseX, mouseY, partialTick);
        // Title bar only
        graphics.blit(BG_TEXTURE, leftPos, topPos, 0, 0, BG_WIDTH, BG_HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, (BG_WIDTH - font.width(title)) / 2, 4, 0x404040, false);
    }

    // -------------------------------------------------------------------------
    // Inner class: priority pane that sends StorageInterface-specific packets
    // -------------------------------------------------------------------------

    private static final class StorageInterfacePriorityPane extends PriorityPane {

        private final StorageInterfaceMenu menu;

        StorageInterfacePriorityPane(StorageInterfaceMenu menu) {
            super(menu::getPriority, menu.getPos());
            this.menu = menu;
        }

        @Override
        public boolean mouseClicked(double localX, double localY, int button) {
            if (button != 0) return false;
            int selected = menu.getPriority();
            // Mirror the layout from PriorityPane: ROW_W=100, centred in WIDTH=110
            int rowX = (preferredWidth() - 100) / 2;
            int downBtnX = rowX;
            int upBtnX = rowX + 20 + 2 + 56 + 2;
            int rowY = 14;
            int btnH = 14;

            if (localX >= downBtnX && localX < downBtnX + 20 && localY >= rowY && localY < rowY + btnH) {
                if (selected > 0)
                    PacketDistributor.sendToServer(
                            new SetStorageInterfacePriorityPacket(menu.getPos(), menu.getFaceIndex(), selected - 1));
                return true;
            }
            if (localX >= upBtnX && localX < upBtnX + 20 && localY >= rowY && localY < rowY + btnH) {
                if (selected < Priority.VALUES.length - 1)
                    PacketDistributor.sendToServer(
                            new SetStorageInterfacePriorityPacket(menu.getPos(), menu.getFaceIndex(), selected + 1));
                return true;
            }
            return false;
        }
    }
}
