package net.bobofraggins.tremendousstorage.shared.ui;

import net.bobofraggins.tremendousstorage.shared.network.SetPullerSidesPacket;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlockEntity;
import net.bobofraggins.tremendousstorage.storage.filingcabinet.FilingCabinetBlockEntity;
import net.bobofraggins.tremendousstorage.storage.tank.TankBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Config drawer pane for the Puller Upgrade.
 *
 * <p>Renders a 3×3 grid of six 18×18 toggle buttons arranged as:
 * <pre>
 *   . T .
 *   L F R
 *   . B X
 * </pre>
 * where T=Top, B=Bottom, L=Left, R=Right, F=Front, X=Back. Each button shows
 * the block adjacent on that face as an item icon, and toggles that face on/off.
 *
 * <p>The sides bitmask: bit 0=TOP, 1=BOTTOM, 2=LEFT, 3=RIGHT, 4=FRONT, 5=BACK.
 */
public class PullerSidesPane implements IDialogPane {

    private static final int BIT_TOP = 0;
    private static final int BIT_BOTTOM = 1;
    private static final int BIT_LEFT = 2;
    private static final int BIT_RIGHT = 3;
    private static final int BIT_FRONT = 4;
    private static final int BIT_BACK = 5;

    private static final int BUTTON_SIZE = 22;
    private static final int BUTTON_MARGIN = 1;
    private static final int GRID_W = 3 * BUTTON_SIZE + 2 * BUTTON_MARGIN; // 68
    private static final int LEFT_MARGIN = (ConfigDrawer.WIDTH - GRID_W) / 2; // 21
    private static final int HEADER_Y = 5;
    private static final int BUTTONS_Y = 17; // HEADER_Y + font_height(8) + gap(4)
    private static final int PANE_HEIGHT = BUTTONS_Y + GRID_W + 5; // 90

    private static final int COLOR_LIGHT = 0xFFFFFFFF;
    private static final int COLOR_DARK = 0xFF555555;

    private static final ResourceLocation TEX_ON =
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "widget/puller_button_on");
    private static final ResourceLocation TEX_OFF =
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "widget/puller_button_off");

    /** {col, row, bit} for each active button in the 3×3 grid. */
    private static final int[][] BUTTONS = {
        {1, 0, BIT_TOP},
        {0, 1, BIT_LEFT},
        {1, 1, BIT_FRONT},
        {2, 1, BIT_RIGHT},
        {1, 2, BIT_BOTTOM},
        {2, 2, BIT_BACK},
    };

    private final BlockPos pos;

    public PullerSidesPane(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public int preferredWidth() {
        return ConfigDrawer.WIDTH;
    }

    @Override
    public int preferredHeight() {
        return PANE_HEIGHT;
    }

    @Override
    public void render(GuiGraphics g, Font font, int width, int localMouseX, int localMouseY, float partialTick) {
        Component header = Component.translatable("screen.tremendousstorage.puller_upgrade_label")
                .withStyle(ChatFormatting.BOLD);
        g.drawString(font, header, (width - font.width(header)) / 2, HEADER_Y, 0x404040, false);

        int sidesMask = getSidesMask();
        Direction facing = getFacing();

        int stride = BUTTON_SIZE + BUTTON_MARGIN;
        for (int[] btn : BUTTONS) {
            int col = btn[0], row = btn[1], bit = btn[2];
            int bx = LEFT_MARGIN + col * stride;
            int by = BUTTONS_Y + row * stride;
            boolean on = (sidesMask & (1 << bit)) != 0;

            // Raised bevel: light top/left, dark bottom/right
            g.fill(bx, by, bx + BUTTON_SIZE, by + 1, COLOR_LIGHT);
            g.fill(bx, by + 1, bx + 1, by + BUTTON_SIZE, COLOR_LIGHT);
            g.fill(bx, by + BUTTON_SIZE, bx + BUTTON_SIZE + 1, by + BUTTON_SIZE + 1, COLOR_DARK);
            g.fill(bx + BUTTON_SIZE, by, bx + BUTTON_SIZE + 1, by + BUTTON_SIZE, COLOR_DARK);
            g.blitSprite(on ? TEX_ON : TEX_OFF, bx + 1, by + 1, BUTTON_SIZE - 2, BUTTON_SIZE - 2);

            ItemStack adjacent = getAdjacentItem(facing, bit);
            if (!adjacent.isEmpty()) {
                g.renderItem(adjacent, bx + 3, by + 3);
            }
        }
    }

    @Override
    public boolean mouseClicked(double localX, double localY, int button) {
        if (button != 0) return false;
        int stride = BUTTON_SIZE + BUTTON_MARGIN;
        for (int[] btn : BUTTONS) {
            int col = btn[0], row = btn[1], bit = btn[2];
            int bx = LEFT_MARGIN + col * stride;
            int by = BUTTONS_Y + row * stride;
            if (localX >= bx && localX < bx + BUTTON_SIZE && localY >= by && localY < by + BUTTON_SIZE) {
                int updated = getSidesMask() ^ (1 << bit);
                PacketDistributor.sendToServer(new SetPullerSidesPacket(pos, updated));
                return true;
            }
        }
        return false;
    }

    private int getSidesMask() {
        var level = Minecraft.getInstance().level;
        if (level == null) return 0;
        var be = level.getBlockEntity(pos);
        if (be instanceof ChestBlockEntity chest) return chest.getPullerSides();
        if (be instanceof FilingCabinetBlockEntity fc) return fc.getPullerSides();
        if (be instanceof TankBlockEntity tank) return tank.getPullerSides();
        return 0;
    }

    private Direction getFacing() {
        var level = Minecraft.getInstance().level;
        if (level == null) return Direction.NORTH;
        var state = level.getBlockState(pos);
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        return Direction.NORTH;
    }

    private ItemStack getAdjacentItem(Direction facing, int bit) {
        Direction world =
                switch (bit) {
                    case BIT_TOP -> Direction.UP;
                    case BIT_BOTTOM -> Direction.DOWN;
                    case BIT_FRONT -> facing;
                    case BIT_BACK -> facing.getOpposite();
                    case BIT_LEFT -> facing.getCounterClockWise();
                    case BIT_RIGHT -> facing.getClockWise();
                    default -> Direction.NORTH;
                };
        var level = Minecraft.getInstance().level;
        if (level == null) return ItemStack.EMPTY;
        var state = level.getBlockState(pos.relative(world));
        if (state.isAir()) return ItemStack.EMPTY;
        var item = state.getBlock().asItem();
        if (item == Items.AIR) return ItemStack.EMPTY;
        return new ItemStack(item);
    }
}
