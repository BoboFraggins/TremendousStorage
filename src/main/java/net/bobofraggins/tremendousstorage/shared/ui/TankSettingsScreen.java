package net.bobofraggins.tremendousstorage.shared.ui;

import net.bobofraggins.tremendousstorage.shared.network.ClearTankContentsPacket;
import net.bobofraggins.tremendousstorage.shared.network.SetVoidExcessPacket;
import net.bobofraggins.tremendousstorage.storage.tank.ClearTankPane;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Settings screen for the Tank.
 *
 * <p>Layout (176 × 168 px):
 * <pre>
 *   ┌─ Dialog title bar (17 px) ───────────┐
 *   │  Title                               │
 *   ├──────────────────────────────────────┤  y=17
 *   │            [Input]                   │  y=20
 *   │              ↓                       │
 *   │            [Output]                  │  y=62
 *   ├──────────────────────────────────────┤  y=83
 *   │  Player inventory (3 rows + hotbar)  │
 *   └──────────────────────────────────────┘  y=168
 * </pre>
 *
 * <p>Void Excess and Clear Contents live in the slide-out {@link ConfigDrawer}.
 */
public class TankSettingsScreen extends AbstractContainerScreen<TankSettingsMenu> {

    private static final ResourceLocation GHOST_BUCKET =
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "textures/gui/ghost/bucket.png");
    private static final ResourceLocation GHOST_BOTTLE =
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "textures/gui/ghost/glass_bottle.png");
    private static final ResourceLocation GHOST_SYRINGE =
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "textures/gui/ghost/experience_syringe.png");

    /** Height of the settings pane (fluid slots + arrow). 17 + 66 + 80 + 5 = 168 total. */
    private static final int SETTINGS_PANE_H = 66;

    /** Y of the separator line between settings and player inventory (screen-relative). */
    private static final int SEPARATOR_DY = Dialog.TITLE_H + SETTINGS_PANE_H; // 83

    private final Dialog dialog;
    private final ConfigDrawer configDrawer;

    public TankSettingsScreen(TankSettingsMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);

        dialog = new Dialog(
                Dialog.blankPane(PlayerInventoryPane.WIDTH, SETTINGS_PANE_H),
                new PlayerInventoryPane(TankSettingsMenu.INV_START_X));
        this.imageWidth = dialog.totalWidth();
        this.imageHeight = dialog.totalHeight();

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
        dialog.init(leftPos, topPos);
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
        dialog.render(g, font, title, mouseX, mouseY, partialTick);
        configDrawer.renderTab(g, mouseX, mouseY);

        int x = leftPos;
        int y = topPos;

        // Separator between settings area and player inventory
        g.fill(x, y + SEPARATOR_DY, x + imageWidth, y + SEPARATOR_DY + 1, 0xFF555555);

        // Fluid-transfer slot backgrounds (centered)
        drawSlot(g, x + TankSettingsMenu.FLUID_IN_X, y + TankSettingsMenu.FLUID_IN_Y);
        drawSlot(g, x + TankSettingsMenu.FLUID_OUT_X, y + TankSettingsMenu.FLUID_OUT_Y);

        // Ghost item hint in input slot when empty — cycles bucket → bottle → syringe
        if (menu.getSlot(0).getItem().isEmpty()) {
            int phase = (int) ((System.currentTimeMillis() / 1500) % 3);
            ResourceLocation ghostTex =
                    switch (phase) {
                        case 0 -> GHOST_BUCKET;
                        case 1 -> GHOST_BOTTLE;
                        default -> GHOST_SYRINGE;
                    };
            int gx = x + TankSettingsMenu.FLUID_IN_X;
            int gy = y + TankSettingsMenu.FLUID_IN_Y;
            g.blit(ghostTex, gx, gy, 0, 0, 16, 16, 16, 16);
        }

        // Down-arrow between the two slots
        drawDownArrow(g, x + TankSettingsMenu.FLUID_IN_X, y + TankSettingsMenu.FLUID_IN_Y + 16);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Title is drawn by Dialog.
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
