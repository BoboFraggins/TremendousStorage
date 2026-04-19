package net.bobofraggins.tremendousstorage.shared.ui;

import net.bobofraggins.tremendousstorage.shared.network.ClearTankContentsPacket;
import net.bobofraggins.tremendousstorage.shared.network.SetVoidExcessPacket;
import net.bobofraggins.tremendousstorage.storage.tank.ClearTankPane;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Settings screen for the Tank.
 *
 * <p>Layout (176 × 168 px):
 * <pre>
 *   ┌─ 0 ──────────────────────────── 176 ─┐
 *   │  Title bar                          14│
 *   ├──────────────────────────────────────┤
 *   │            [Input]                  │  y=20
 *   │              ↓                      │
 *   │            [Output]                 │  y=62  (slots centered at x=80)
 *   ├──────────────────────────────────────┤  y=82
 *   │  Player inventory (3 rows + hotbar) │
 *   └──────────────────────────────────────┘  168
 * </pre>
 *
 * <p>Void Excess and Clear Contents live in the slide-out {@link ConfigDrawer}.
 */
public class TankSettingsScreen extends AbstractContainerScreen<TankSettingsMenu> {

    private static final int BG_WIDTH = 176;
    private static final int BG_HEIGHT = 168;
    private static final int TITLE_BAR_H = 14;
    private static final int SETTINGS_H = 82;
    private static final int INV_PANE_Y = 90;

    private final ConfigDrawer configDrawer;
    private final PlayerInventoryPane playerInvPane;

    public TankSettingsScreen(TankSettingsMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = BG_WIDTH;
        this.imageHeight = BG_HEIGHT;

        playerInvPane = new PlayerInventoryPane(TankSettingsMenu.INV_START_X);

        VoidExcessPane voidPane = new VoidExcessPane(
                menu::isVoidExcess,
                () -> PacketDistributor.sendToServer(new SetVoidExcessPacket(menu.getPos(), !menu.isVoidExcess())));
        ClearTankPane clearPane =
                new ClearTankPane(() -> PacketDistributor.sendToServer(new ClearTankContentsPacket(menu.getPos())));
        if (menu.hasPullerUpgrade()) {
            configDrawer = new ConfigDrawer(voidPane, clearPane, new PullerSidesPane(menu.getPos()));
        } else {
            configDrawer = new ConfigDrawer(voidPane, clearPane);
        }
    }

    @Override
    protected void init() {
        super.init();
        configDrawer.init(leftPos, topPos, imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        // Drawer renders behind the main panel
        configDrawer.render(g, font, mouseX, mouseY, partialTick);

        int x = leftPos;
        int y = topPos;

        // Full background
        g.fill(x, y, x + BG_WIDTH, y + BG_HEIGHT, 0xFFC6C6C6);

        // Title-bar separator
        g.fill(x, y + TITLE_BAR_H, x + BG_WIDTH, y + TITLE_BAR_H + 1, 0xFF555555);

        // Settings / player-inventory separator
        g.fill(x, y + SETTINGS_H, x + BG_WIDTH, y + SETTINGS_H + 1, 0xFF555555);

        // Fluid-transfer slot backgrounds (centered)
        drawSlot(g, x + TankSettingsMenu.FLUID_IN_X, y + TankSettingsMenu.FLUID_IN_Y);
        drawSlot(g, x + TankSettingsMenu.FLUID_OUT_X, y + TankSettingsMenu.FLUID_OUT_Y);

        // Down-arrow between the two slots
        drawDownArrow(g, x + TankSettingsMenu.FLUID_IN_X, y + TankSettingsMenu.FLUID_IN_Y + 16);

        // Player-inventory slot backgrounds
        g.pose().pushPose();
        g.pose().translate(x, y + INV_PANE_Y, 0);
        playerInvPane.render(g, font, BG_WIDTH, mouseX - x, mouseY - (y + INV_PANE_Y), partialTick);
        g.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, (BG_WIDTH - font.width(title)) / 2, 4, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (configDrawer.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // -------------------------------------------------------------------------
    // Drawing helpers
    // -------------------------------------------------------------------------

    /** Draws a single slot with the standard bevel style. */
    private static void drawSlot(GuiGraphics g, int sx, int sy) {
        g.fill(sx, sy, sx + 16, sy + 1, 0xFF373737); // top
        g.fill(sx, sy + 1, sx + 1, sy + 16, 0xFF373737); // left
        g.fill(sx, sy + 16, sx + 17, sy + 17, 0xFFFFFFFF); // bottom
        g.fill(sx + 16, sy, sx + 17, sy + 16, 0xFFFFFFFF); // right
        g.fill(sx + 1, sy + 1, sx + 16, sy + 16, 0xFF8B8B8B); // interior
    }

    /**
     * Draws a down-pointing arrow in the gap below a slot.
     *
     * @param gapX left edge of the slot (arrow is centred horizontally within the 16 px slot)
     * @param gapY top of the gap area (= bottom edge of the slot above)
     */
    private static void drawDownArrow(GuiGraphics g, int gapX, int gapY) {
        int cx = gapX + 8; // centre of 16 px slot
        int top = gapY + 3;
        // Stem: 2 px wide
        g.fill(cx - 1, top, cx + 1, top + 9, 0xFF555555);
        // Arrowhead: 3 rows narrowing to a point
        g.fill(cx - 4, top + 9, cx + 4, top + 11, 0xFF555555); // 8 px
        g.fill(cx - 2, top + 11, cx + 2, top + 13, 0xFF555555); // 4 px
        g.fill(cx - 1, top + 13, cx + 1, top + 15, 0xFF555555); // 2 px
    }
}
