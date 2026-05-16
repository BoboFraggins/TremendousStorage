package net.bobofraggins.tremendousstorage.shared.ui;

import net.bobofraggins.tremendousstorage.shared.network.ClearTankContentsPacket;
import net.bobofraggins.tremendousstorage.shared.network.SetVoidExcessPacket;
import net.bobofraggins.tremendousstorage.storage.tank.ClearTankPane;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

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

    private static final Identifier GHOST_BUCKET =
            Identifier.fromNamespaceAndPath("tremendousstorage", "textures/gui/ghost/bucket.png");
    private static final Identifier GHOST_BOTTLE =
            Identifier.fromNamespaceAndPath("tremendousstorage", "textures/gui/ghost/glass_bottle.png");
    private static final Identifier GHOST_SYRINGE =
            Identifier.fromNamespaceAndPath("tremendousstorage", "textures/gui/ghost/experience_syringe.png");

    /** Height of the settings pane (fluid slots + arrow + 5 px margin). 17 + 71 + 80 + 5 = 173 total. */
    private static final int SETTINGS_PANE_H = 71;

    private final Dialog dialog;
    private final ConfigDrawer configDrawer;

    public TankSettingsScreen(TankSettingsMenu menu, Inventory inv, Component title) {
        Dialog dialog_ = new Dialog(
                Dialog.blankPane(PlayerInventoryPane.WIDTH, SETTINGS_PANE_H),
                new PlayerInventoryPane(TankSettingsMenu.INV_START_X));
        super(menu, inv, title, dialog_.totalWidth(), dialog_.totalHeight());
        dialog = dialog_;

        VoidExcessPane voidPane = new VoidExcessPane(
                menu::isVoidExcess,
                () -> ClientPacketDistributor.sendToServer(
                        new SetVoidExcessPacket(menu.getPos(), !menu.isVoidExcess())));
        ClearTankPane clearPane = new ClearTankPane(
                () -> ClientPacketDistributor.sendToServer(new ClearTankContentsPacket(menu.getPos())));
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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractBackground(graphics, mouseX, mouseY, partialTick);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        // Drawer renders behind the main panel
        configDrawer.render(g, font, mouseX, mouseY, partialTick);
        configDrawer.renderTab(g, mouseX, mouseY);
        dialog.render(g, font, title, mouseX, mouseY, partialTick);

        int x = leftPos;
        int y = topPos;

        // Fluid-transfer slot backgrounds (centered)
        drawSlot(g, x + TankSettingsMenu.FLUID_IN_X, y + TankSettingsMenu.FLUID_IN_Y);
        drawSlot(g, x + TankSettingsMenu.FLUID_OUT_X, y + TankSettingsMenu.FLUID_OUT_Y);

        // Ghost item hint in input slot when empty — cycles bucket → bottle → syringe
        if (menu.getSlot(0).getItem().isEmpty()) {
            int phase = (int) ((System.currentTimeMillis() / 1500) % 3);
            Identifier ghostTex =
                    switch (phase) {
                        case 0 -> GHOST_BUCKET;
                        case 1 -> GHOST_BOTTLE;
                        default -> GHOST_SYRINGE;
                    };
            int gx = x + TankSettingsMenu.FLUID_IN_X;
            int gy = y + TankSettingsMenu.FLUID_IN_Y;
            g.blit(RenderPipelines.GUI_TEXTURED, ghostTex, gx, gy, 0, 0, 16, 16, 16, 16);
        }

        // Down-arrow between the two slots
        drawDownArrow(g, x + TankSettingsMenu.FLUID_IN_X, y + TankSettingsMenu.FLUID_IN_Y + 18);
        super.extractContents(g, mouseX, mouseY, partialTick);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        // Title is drawn by Dialog.
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean consumed) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        if (configDrawer.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(event, consumed);
    }

    // -------------------------------------------------------------------------
    // Drawing helpers
    // -------------------------------------------------------------------------

    /** Draws a single slot with the standard bevel style. */
    private static void drawSlot(GuiGraphicsExtractor g, int sx, int sy) {
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
    private static void drawDownArrow(GuiGraphicsExtractor g, int gapX, int gapY) {
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
