package net.bobofraggins.tremendousstorage.shared.util;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Singleton bridge between the optional JEI search bar and TremendousStorage's inventory panes.
 *
 * <p>When JEI is present, the JEI plugin sets a provider that returns the current filter text.
 * When JEI is absent (or before it initialises), the provider returns an empty string so all
 * items are shown normally.
 *
 * <p>Only {@code TremendousStorageJeiPlugin} should call {@link #setProvider}; all other code uses
 * {@link #getFilter} and {@link #matches}.
 */
public final class SearchSync {

    private static Supplier<String> provider = () -> "";

    private SearchSync() {}

    /** Called by the JEI plugin when the JEI runtime becomes available or is torn down. */
    public static void setProvider(Supplier<String> p) {
        provider = (p != null) ? p : () -> "";
    }

    /**
     * Returns the current JEI filter text, lowercased and stripped, or {@code ""} if JEI is not
     * present or has no active filter.
     */
    public static String getFilter() {
        return provider.get().toLowerCase(Locale.ROOT).strip();
    }

    /**
     * Returns {@code true} when {@code stack} should be shown given {@code filter}.
     *
     * <ul>
     *   <li>Empty filter → always matches.
     *   <li>{@code @text} → matches when the item's mod namespace contains {@code text}.
     *   <li>{@code #text} → matches when any tooltip line (after the name) contains {@code text}.
     *   <li>{@code $text} → matches when any of the item's tags contains {@code text}.
     *   <li>Otherwise → matches when the item's display name contains {@code filter} (both
     *       already lowercased).
     * </ul>
     */
    public static boolean matches(ItemStack stack, String filter) {
        if (filter.isEmpty()) return true;
        if (filter.startsWith("@")) {
            String query = filter.substring(1);
            String ns = BuiltInRegistries.ITEM
                    .getKey(stack.getItem())
                    .getNamespace()
                    .toLowerCase(Locale.ROOT);
            return ns.contains(query);
        }
        if (filter.startsWith("#")) {
            String query = filter.substring(1);
            List<Component> lines = stack.getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.Default.NORMAL);
            // Skip index 0 (item name); check remaining tooltip lines.
            for (int i = 1; i < lines.size(); i++) {
                if (lines.get(i).getString().toLowerCase(Locale.ROOT).contains(query)) return true;
            }
            return false;
        }
        if (filter.startsWith("$")) {
            String query = filter.substring(1);
            return stack.getTags()
                    .anyMatch(tag ->
                            tag.location().toString().toLowerCase(Locale.ROOT).contains(query));
        }
        return stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(filter);
    }
}
