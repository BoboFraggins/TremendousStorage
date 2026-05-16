package net.bobofraggins.tremendousstorage.storage.networkinterface;

import java.util.List;
import net.bobofraggins.tremendousstorage.shared.input.QuickStackClientEvents;
import net.bobofraggins.tremendousstorage.shared.network.NetworkContentsPacket;
import net.bobofraggins.tremendousstorage.shared.network.QuickStackPacket;
import net.bobofraggins.tremendousstorage.shared.network.RequestNetworkContentsPacket;
import net.bobofraggins.tremendousstorage.shared.ui.Dialog;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Screen for the Network Interface block.
 *
 * <p>Layout (176 × 220 px):
 * <ul>
 *   <li>Title row + validity indicator
 *   <li>Scrollable list of attached TremendousStorage blocks (translation keys → display names)
 * </ul>
 *
 * <p>When the screen opens it sends a {@link RequestNetworkContentsPacket} to the server;
 * the response populates {@link NetworkContentsPacket#PENDING} which is polled each tick.
 */
public class NetworkInterfaceScreen extends AbstractContainerScreen<NetworkInterfaceMenu> {

    private static final int BG_WIDTH = 176;
    private static final int BG_HEIGHT = 220;

    /** Number of list rows visible at once. */
    private static final int VISIBLE_ROWS = 12;

    private static final int ROW_HEIGHT = 12;
    private static final int LIST_Y_START = 30;

    private static final int SCROLLBAR_W = 14;
    private static final int SCROLLBAR_TRACK_X = BG_WIDTH - SCROLLBAR_W - 4; // 158
    private static final int SCROLLER_W = 12;
    private static final int SCROLLER_H = 15;

    private static final Identifier SCROLLER = Identifier.withDefaultNamespace("container/creative_inventory/scroller");
    private static final Identifier SCROLLER_DISABLED =
            Identifier.withDefaultNamespace("container/creative_inventory/scroller_disabled");

    private final Dialog dialog;

    private List<String> entries = List.of();
    private int scrollOffset = 0;
    private int lastScanDirtyCounter = -1;
    private boolean draggingScrollbar = false;

    public NetworkInterfaceScreen(NetworkInterfaceMenu menu, Inventory inv, Component title) {
        Dialog dialog_ = new Dialog(Dialog.blankPane(BG_WIDTH, BG_HEIGHT - Dialog.TITLE_H - Dialog.BOTTOM_PADDING));
        super(menu, inv, title, dialog_.totalWidth(), dialog_.totalHeight());
        dialog = dialog_;
    }

    @Override
    protected void init() {
        super.init();
        dialog.init(leftPos, topPos);
        // Request block list from server
        ClientPacketDistributor.sendToServer(new RequestNetworkContentsPacket(menu.getPos()));
        // Clear stale data from a previous screen open
        NetworkContentsPacket.PENDING = List.of();
        entries = List.of();
        scrollOffset = 0;
        lastScanDirtyCounter = menu.getScanDirtyCounter();
    }

    // -------------------------------------------------------------------------
    // Tick — poll for server response
    // -------------------------------------------------------------------------

    @Override
    protected void containerTick() {
        super.containerTick();
        int counter = menu.getScanDirtyCounter();
        if (counter != lastScanDirtyCounter) {
            lastScanDirtyCounter = counter;
            ClientPacketDistributor.sendToServer(new RequestNetworkContentsPacket(menu.getPos()));
            NetworkContentsPacket.PENDING = List.of();
        }
        List<String> pending = NetworkContentsPacket.PENDING;
        if (!pending.isEmpty() && pending != entries) {
            entries = pending;
            // Clamp scroll after receiving new data
            clampScroll();
        }
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key();
        int scanCode = event.scancode();
        int modifiers = event.modifiers();

        if (QuickStackClientEvents.QUICK_STACK != null
                && QuickStackClientEvents.QUICK_STACK.matches(event)
                && menu.isNetworkValid()) {
            ClientPacketDistributor.sendToServer(new QuickStackPacket(menu.getPos(), true));
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean consumed) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        if (button == 0 && isInScrollbar(mouseX, mouseY)) {
            draggingScrollbar = true;
            scrollToY(mouseY);
            return true;
        }
        return super.mouseClicked(event, consumed);
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        if (draggingScrollbar) {
            scrollToY(mouseY);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        if (draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollOffset -= (int) Math.signum(scrollY);
        clampScroll();
        return true;
    }

    private boolean isInScrollbar(double absX, double absY) {
        int trackX = leftPos + SCROLLBAR_TRACK_X;
        int trackY = topPos + LIST_Y_START;
        return absX >= trackX
                && absX < trackX + SCROLLBAR_W
                && absY >= trackY
                && absY < trackY + VISIBLE_ROWS * ROW_HEIGHT;
    }

    private void scrollToY(double absY) {
        int trackY = topPos + LIST_Y_START;
        int trackH = VISIBLE_ROWS * ROW_HEIGHT;
        int maxScroll = Math.max(0, entries.size() - VISIBLE_ROWS);
        if (maxScroll == 0) return;
        float frac = ((float) absY - trackY - 1 - SCROLLER_H / 2.0f) / (trackH - 2 - SCROLLER_H);
        frac = Math.max(0f, Math.min(1f, frac));
        scrollOffset = Math.round(frac * maxScroll);
        clampScroll();
    }

    private void clampScroll() {
        int maxScroll = Math.max(0, entries.size() - VISIBLE_ROWS);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractBackground(graphics, mouseX, mouseY, partialTick);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = leftPos, y = topPos;

        dialog.render(graphics, font, title, mouseX, mouseY, partialTick);

        // Validity indicator line below title bar
        boolean valid = menu.isNetworkValid();
        Component statusText = Component.translatable(
                valid
                        ? "screen.tremendousstorage.network_interface.valid"
                        : "screen.tremendousstorage.network_interface.invalid");
        int statusColor = valid ? 0x006600 : 0xAA0000;
        graphics.text(font, statusText, x + (BG_WIDTH - font.width(statusText)) / 2, y + 18, statusColor, false);

        // List area separator
        graphics.fill(x + 4, y + LIST_Y_START - 2, x + BG_WIDTH - 4, y + LIST_Y_START - 1, 0x80555555);

        // Draw list rows (scissored to avoid rendering under the scrollbar track)
        int listAreaBottom = y + LIST_Y_START + VISIBLE_ROWS * ROW_HEIGHT;
        graphics.enableScissor(x + 4, y + LIST_Y_START, x + SCROLLBAR_TRACK_X - 1, listAreaBottom);

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int idx = i + scrollOffset;
            if (idx >= entries.size()) break;

            String key = entries.get(idx);
            // Strip optional " (N)" count suffix before translating, then re-append
            String translatable = key;
            String countSuffix = "";
            int parenIdx = key.lastIndexOf(" (");
            if (parenIdx > 0 && key.endsWith(")")) {
                translatable = key.substring(0, parenIdx);
                countSuffix = key.substring(parenIdx);
            }
            String displayStr = translatable.contains(".")
                    ? Component.translatable(translatable).getString() + countSuffix
                    : key;

            int rowY = y + LIST_Y_START + i * ROW_HEIGHT;
            graphics.text(font, displayStr, x + 6, rowY, 0x404040, false);
        }

        graphics.disableScissor();

        // Empty state
        if (entries.isEmpty()) {
            String emptyMsg = "No blocks connected";
            graphics.text(
                    font, emptyMsg, x + (BG_WIDTH - font.width(emptyMsg)) / 2, y + LIST_Y_START + 4, 0x808080, false);
        }

        // Scrollbar track: sunken 3D bevel matching the network inventory pane
        int trackX = x + SCROLLBAR_TRACK_X;
        int trackY = y + LIST_Y_START;
        int trackH = VISIBLE_ROWS * ROW_HEIGHT;
        graphics.fill(trackX, trackY, trackX + SCROLLBAR_W, trackY + trackH, 0xFF8B8B8B);
        graphics.fill(trackX, trackY, trackX + SCROLLBAR_W, trackY + 1, 0xFF555555);
        graphics.fill(trackX, trackY, trackX + 1, trackY + trackH, 0xFF555555);
        graphics.fill(trackX, trackY + trackH - 1, trackX + SCROLLBAR_W, trackY + trackH, 0xFFFFFFFF);
        graphics.fill(trackX + SCROLLBAR_W - 1, trackY, trackX + SCROLLBAR_W, trackY + trackH, 0xFFFFFFFF);

        // Scrollbar thumb
        boolean canScroll = entries.size() > VISIBLE_ROWS;
        int thumbX = trackX + 1;
        int thumbY;
        if (!canScroll) {
            thumbY = trackY + 1;
        } else {
            int maxScroll = entries.size() - VISIBLE_ROWS;
            float frac = scrollOffset / (float) maxScroll;
            thumbY = trackY + 1 + (int) ((trackH - 2 - SCROLLER_H) * frac);
        }
        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                canScroll ? SCROLLER : SCROLLER_DISABLED,
                thumbX,
                thumbY,
                SCROLLER_W,
                SCROLLER_H);
        super.extractContents(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Title is drawn by Dialog.
    }
}
