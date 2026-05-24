package net.bobofraggins.tremendousstorage.shared.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * Styled search box matching the Access Terminal's recessed-bevel look.
 *
 * <p>Usage:
 * <pre>{@code
 * // In init():
 * searchBox = new SearchBoxWidget(font, leftPos, topPos, imageWidth);
 * searchBox.getEditBox().setResponder(text -> { ... });
 * addRenderableWidget(searchBox.getEditBox());
 * searchBox.initFilter(SearchSync.getRawFilter());
 *
 * // In renderBg():
 * searchBox.render(graphics, font);
 * }</pre>
 */
public class SearchBoxWidget {

    public static final int WIDTH = 75;
    public static final int HEIGHT = 12;
    private static final int MARGIN = 3;

    private final int bx;
    private final int by;
    private final EditBox editBox;

    public SearchBoxWidget(Font font, int leftPos, int topPos, int imageWidth) {
        this.bx = leftPos + imageWidth - 5 - 2 - WIDTH;
        this.by = topPos + 4;
        int textH = font.lineHeight;
        this.editBox =
                new EditBox(font, bx + MARGIN, by + (HEIGHT - textH) / 2, WIDTH - MARGIN, textH, Component.empty());
        editBox.setMaxLength(64);
        editBox.setBordered(false);
        editBox.setTextColor(0xFFFFFFFF);
    }

    /** The underlying EditBox — must be registered with the screen via {@code addRenderableWidget}. */
    public EditBox getEditBox() {
        return editBox;
    }

    /**
     * Sets the initial filter value. If non-empty the box is auto-focused and text selected so the
     * user immediately sees that a filter is active and can clear it with backspace.
     */
    public void initFilter(String rawFilter) {
        editBox.setValue(rawFilter);
        if (!rawFilter.isEmpty()) {
            editBox.setFocused(true);
            editBox.setHighlightPos(0);
        }
    }

    public String getValue() {
        return editBox.getValue();
    }

    public void setValue(String value) {
        editBox.setValue(value);
    }

    /** Draws the background fill, recessed bevel, and "Search…" hint text. */
    public void render(GuiGraphicsExtractor graphics, Font font) {
        // Background
        graphics.fill(bx, by, bx + WIDTH, by + HEIGHT, 0xFF8B8B8B);

        // Shadow: top and left edges (recessed look)
        graphics.fill(bx - 1, by - 1, bx + WIDTH + 1, by, 0xFF555555);
        graphics.fill(bx - 1, by, bx, by + HEIGHT + 1, 0xFF555555);

        // Highlight: bottom and right edges
        graphics.fill(bx, by + HEIGHT, bx + WIDTH + 1, by + HEIGHT + 1, 0xFFFFFFFF);
        graphics.fill(bx + WIDTH, by, bx + WIDTH + 1, by + HEIGHT, 0xFFFFFFFF);

        // Hint text
        if (editBox.getValue().isEmpty() && !editBox.isFocused()) {
            String hint = net.minecraft.network.chat.Component.translatable("screen.tremendousstorage.search_hint")
                    .getString();
            graphics.text(font, hint, bx + MARGIN, by + (HEIGHT - font.lineHeight) / 2 + 1, 0xFFAAAAAA, false);
        }
    }
}
