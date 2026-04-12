package net.bobofraggins.tremendousstorage.storage.wirelesshub;

import java.util.function.IntSupplier;
import net.bobofraggins.tremendousstorage.shared.network.SetHaarpModePacket;
import net.bobofraggins.tremendousstorage.shared.ui.IDialogPane;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Dialog pane for the HAARP weather-control section of the Wireless Hub UI.
 *
 * <p>Renders a "Weather Mode" header and four radio buttons (Off / No Rain / Rain / Thunderstorm).
 * Clicking a row sends {@link SetHaarpModePacket} to the server.
 */
public class HaarpWeatherPane implements IDialogPane {

    private static final int PANE_HEIGHT = 68;
    private static final int HEADER_Y = 5;
    private static final int FIRST_ROW_Y = 18;
    private static final int ROW_STRIDE = 11;
    private static final int RADIO_SIZE = 7;
    private static final int RADIO_LEFT = 20;
    private static final int LABEL_LEFT = 32;

    /** Active colour for the selected radio indicator. */
    private static final int COLOR_ACTIVE = 0xFF00AA00;
    /** Inactive colour for unselected radio indicators. */
    private static final int COLOR_INACTIVE = 0xFF737373;

    private static final WeatherMode[] MODES = WeatherMode.values();
    private static final String[] MODE_KEYS = {
        "screen.tremendousstorage.haarp.off",
        "screen.tremendousstorage.haarp.no_rain",
        "screen.tremendousstorage.haarp.rain",
        "screen.tremendousstorage.haarp.thunderstorm"
    };

    /** Supplies the current mode ordinal (synced from ContainerData). */
    private final IntSupplier modeSupplier;

    private final BlockPosSupplier hubPosSupplier;

    @FunctionalInterface
    public interface BlockPosSupplier {
        net.minecraft.core.BlockPos get();
    }

    public HaarpWeatherPane(IntSupplier modeSupplier, BlockPosSupplier hubPosSupplier) {
        this.modeSupplier = modeSupplier;
        this.hubPosSupplier = hubPosSupplier;
    }

    @Override
    public int preferredWidth() {
        return 176;
    }

    @Override
    public int preferredHeight() {
        return PANE_HEIGHT;
    }

    @Override
    public void render(GuiGraphics g, Font font, int width, int localMouseX, int localMouseY, float partialTick) {
        int currentOrdinal = modeSupplier.getAsInt();

        // Header
        Component header = Component.translatable("screen.tremendousstorage.haarp.weather_mode");
        g.drawString(font, header, (width - font.width(header)) / 2, HEADER_Y, 0x404040, false);

        // Radio rows
        for (int i = 0; i < MODES.length; i++) {
            int rowY = FIRST_ROW_Y + i * ROW_STRIDE;
            boolean selected = (i == currentOrdinal);

            // Radio indicator box
            int color = selected ? COLOR_ACTIVE : COLOR_INACTIVE;
            g.fill(RADIO_LEFT, rowY, RADIO_LEFT + RADIO_SIZE, rowY + RADIO_SIZE, color);
            if (!selected) {
                // Hollow centre
                g.fill(RADIO_LEFT + 2, rowY + 2, RADIO_LEFT + RADIO_SIZE - 2, rowY + RADIO_SIZE - 2, 0xFFC6C6C6);
            }

            // Label
            Component label = Component.translatable(MODE_KEYS[i]);
            g.drawString(font, label, LABEL_LEFT, rowY, 0x404040, false);
        }
    }

    @Override
    public boolean mouseClicked(double localX, double localY, int button) {
        if (button != 0) return false;
        for (int i = 0; i < MODES.length; i++) {
            int rowY = FIRST_ROW_Y + i * ROW_STRIDE;
            if (localY >= rowY && localY < rowY + ROW_STRIDE) {
                PacketDistributor.sendToServer(new SetHaarpModePacket(hubPosSupplier.get(), i));
                return true;
            }
        }
        return false;
    }
}
