package net.bobofraggins.intellistore.shared.util;

import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * Singleton bridge between the optional JEI search bar and IntelliStore's inventory panes.
 *
 * <p>When JEI is present, the JEI plugin sets a provider that returns the current filter text.
 * When JEI is absent (or before it initialises), the provider returns an empty string so all
 * items are shown normally.
 *
 * <p>Only {@code IntelliStoreJeiPlugin} should call {@link #setProvider}; all other code uses
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
     *   <li>{@code @prefix} → matches when the item's mod namespace contains {@code prefix}.
     *   <li>Otherwise → matches when the item's display name contains {@code filter} (both
     *       already lowercased).
     * </ul>
     */
    public static boolean matches(ItemStack stack, String filter) {
        if (filter.isEmpty()) return true;
        if (filter.startsWith("@")) {
            String modQuery = filter.substring(1);
            String ns = BuiltInRegistries.ITEM
                    .getKey(stack.getItem())
                    .getNamespace()
                    .toLowerCase(Locale.ROOT);
            return ns.contains(modQuery);
        }
        return stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(filter);
    }
}
