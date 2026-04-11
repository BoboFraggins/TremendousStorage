package net.bobofraggins.tremendousstorage.shared.ui;

import net.bobofraggins.tremendousstorage.shared.network.ClearTankContentsPacket;
import net.bobofraggins.tremendousstorage.shared.network.SetVoidExcessPacket;
import net.bobofraggins.tremendousstorage.storage.tank.TankBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Settings screen for the Tank.
 *
 * <p>Layout (176 × 168 px):
 * <pre>
 *   ┌─ 0 ──────────────────────────── 176 ─┐
 *   │  Title                              14│  title bar
 *   ├──────────────────────────────────────┤
 *   │  [Input]  │  Void Excess            │
 *   │    ↓      │  [toggle]               │  settings (y=14–82)
 *   │  [Output] │  [Clear Contents]       │
 *   ├──────────────────────────────────────┤  y=82
 *   │  Player inventory (3 rows)          │
 *   │  Hotbar                             │
 *   └──────────────────────────────────────┘  168
 * </pre>
 */
public class TankSettingsScreen extends AbstractContainerScreen<TankSettingsMenu> {

    private static final int BG_WIDTH  = 176;
    private static final int BG_HEIGHT = 168;

    // Right-pane button geometry
    private static final int BTN_X = 92;
    private static final int BTN_W = 78;
    private static final int BTN_H = 20;
    private static final int VOID_BTN_Y  = 30;
    private static final int CLEAR_BTN_Y = 54;

    public TankSettingsScreen(TankSettingsMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = BG_WIDTH;
        this.imageHeight = BG_HEIGHT;
        this.inventoryLabelY = BG_HEIGHT - 94; // positions "Inventory" label
    }

    @Override
    protected void init() {
        super.init();

        addRenderableWidget(Button.builder(voidExcessLabel(), btn -> {
                    boolean newValue = !menu.isVoidExcess();
                    PacketDistributor.sendToServer(new SetVoidExcessPacket(menu.getPos(), newValue));
                    btn.setMessage(voidExcessLabel(!menu.isVoidExcess()));
                })
                .bounds(leftPos + BTN_X, topPos + VOID_BTN_Y, BTN_W, BTN_H)
                .build());

        BlockEntity be = minecraft.level != null ? minecraft.level.getBlockEntity(menu.getPos()) : null;
        if (be instanceof TankBlockEntity) {
            addRenderableWidget(Button.builder(
                            Component.translatable("screen.tremendousstorage.clear_tank_contents"),
                            btn -> PacketDistributor.sendToServer(new ClearTankContentsPacket(menu.getPos())))
                    .bounds(leftPos + BTN_X, topPos + CLEAR_BTN_Y, BTN_W, BTN_H)
                    .build());
        }
    }

    private Component voidExcessLabel() {
        return voidExcessLabel(menu.isVoidExcess());
    }

    private Component voidExcessLabel(boolean value) {
        return Component.translatable(
                value ? "screen.tremendousstorage.void_excess_on" : "screen.tremendousstorage.void_excess_off");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (!renderables.isEmpty() && renderables.get(0) instanceof Button btn) {
            btn.setMessage(voidExcessLabel());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        // Full background
        g.fill(x, y, x + BG_WIDTH, y + BG_HEIGHT, 0xFFC6C6C6);

        // Title-bar separator
        g.fill(x, y + 14, x + BG_WIDTH, y + 15, 0xFF555555);

        // Settings / player-inventory separator
        g.fill(x, y + 82, x + BG_WIDTH, y + 83, 0xFF555555);

        // Left-pane divider (between fluid transfer and settings buttons)
        g.fill(x + 88, y + 14, x + 89, y + 82, 0xFF555555);

        // Fluid-transfer slot backgrounds
        drawSlot(g, x + TankSettingsMenu.FLUID_IN_X,  y + TankSettingsMenu.FLUID_IN_Y);
        drawSlot(g, x + TankSettingsMenu.FLUID_OUT_X, y + TankSettingsMenu.FLUID_OUT_Y);

        // Down-arrow between the two slots
        drawDownArrow(g, x + TankSettingsMenu.FLUID_IN_X, y + TankSettingsMenu.FLUID_IN_Y + 16);

        // "Fill / Drain" label above the input slot
        Component fillLabel = Component.translatable("screen.tremendousstorage.fluid_transfer");
        g.drawString(font, fillLabel,
                x + 44 - font.width(fillLabel) / 2, y + 17, 0x404040, false);

        // Void-excess label above the toggle button
        Component voidLabel = Component.translatable("screen.tremendousstorage.void_excess_label");
        g.drawString(font, voidLabel,
                x + BTN_X + (BTN_W - font.width(voidLabel)) / 2, y + 20, 0x404040, false);

        // Player-inventory slot backgrounds
        drawSlotGrid(g, x, y, 8, 90, 9, 3);
        drawSlotGrid(g, x, y, 8, 148, 9, 1);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, (BG_WIDTH - font.width(title)) / 2, 4, 0x404040, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    // -------------------------------------------------------------------------
    // Drawing helpers
    // -------------------------------------------------------------------------

    /** Draws a single 16×16 slot inset at screen position (sx, sy). */
    private static void drawSlot(GuiGraphics g, int sx, int sy) {
        g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF373737);
        g.fill(sx, sy, sx + 16, sy + 16, 0xFF8B8B8B);
    }

    /** Draws a grid of slot insets starting at (startX, startY) with the given col/row counts. */
    private static void drawSlotGrid(GuiGraphics g, int ox, int oy, int startX, int startY, int cols, int rows) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                drawSlot(g, ox + startX + col * 18, oy + startY + row * 18);
            }
        }
    }

    /**
     * Draws a down-pointing arrow in the gap below a slot.
     *
     * @param gapX  left edge of the slot (arrow is centred horizontally within the 16 px slot)
     * @param gapY  top of the gap area (= bottom edge of the slot above)
     */
    private static void drawDownArrow(GuiGraphics g, int gapX, int gapY) {
        int cx = gapX + 8; // centre of 16 px slot
        int top = gapY + 3;
        // Stem: 2 px wide
        g.fill(cx - 1, top,     cx + 1, top + 9,  0xFF555555);
        // Arrowhead: 3 rows widening to a point
        g.fill(cx - 4, top + 9, cx + 4, top + 11, 0xFF555555); // 8 px
        g.fill(cx - 2, top + 11,cx + 2, top + 13, 0xFF555555); // 4 px
        g.fill(cx - 1, top + 13,cx + 1, top + 15, 0xFF555555); // 2 px
    }
}
