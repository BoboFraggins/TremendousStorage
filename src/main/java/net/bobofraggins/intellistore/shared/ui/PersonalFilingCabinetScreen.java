package net.bobofraggins.intellistore.shared.ui;

import net.bobofraggins.intellistore.shared.network.SetPfcVoidExcessPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Screen for the Personal Filing Cabinet item.
 *
 * <p>Layout (176 × 162 px):
 * <ul>
 *   <li>Title centred at y=6
 *   <li>"Void Excess: ON/OFF" toggle button (120×16) centred at y=24
 *   <li>2 rows × 4 columns of folder slots starting at x=29, y=44 (18×18 each)
 *   <li>Player inventory 3×9 starting at y=82
 *   <li>Player hotbar at y=140
 * </ul>
 */
public class PersonalFilingCabinetScreen extends AbstractContainerScreen<PersonalFilingCabinetMenu> {

    private static final int BG_WIDTH = 176;
    private static final int BG_HEIGHT = 170;

    private static final int BTN_W = 120;
    private static final int BTN_H = 14;
    private static final int BTN_Y = 18;

    private Button voidExcessButton;

    public PersonalFilingCabinetScreen(PersonalFilingCabinetMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = BG_WIDTH;
        this.imageHeight = BG_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        int btnX = leftPos + (BG_WIDTH - BTN_W) / 2;
        int btnY = topPos + BTN_Y;
        voidExcessButton = addRenderableWidget(Button.builder(
                        voidExcessLabel(),
                        btn -> {
                            boolean newValue = !menu.isVoidExcess();
                            menu.setVoidExcess(newValue);
                            PacketDistributor.sendToServer(
                                    new SetPfcVoidExcessPacket(menu.getPfcSlot(), newValue));
                        })
                .bounds(btnX, btnY, BTN_W, BTN_H)
                .build());
    }

    private Component voidExcessLabel() {
        return menu.isVoidExcess()
                ? Component.translatable("screen.intellistore.void_excess_on")
                : Component.translatable("screen.intellistore.void_excess_off");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (voidExcessButton != null) voidExcessButton.setMessage(voidExcessLabel());
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Dark background
        graphics.fill(leftPos, topPos, leftPos + BG_WIDTH, topPos + BG_HEIGHT, 0xC0101010);

        // Slot backgrounds for the 8 folder slots (2 rows × 4 cols)
        // Coordinates must match the Slot positions in PersonalFilingCabinetMenu (x=29, y=44)
        for (int i = 0; i < PersonalFilingCabinetMenu.FOLDER_SLOTS; i++) {
            int col = i % 4;
            int row = i / 4;
            int sx = leftPos + 29 + col * 18;
            int sy = topPos + 44 + row * 18;
            graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF303030);
            graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF1A1A1A);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, (BG_WIDTH - font.width(title)) / 2, 6, 0xFFFFFF, false);
        // Hide the default inventory label
    }
}
