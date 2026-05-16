package net.bobofraggins.tremendousstorage.shared.util;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Singleton bridge between the optional JEI search bar and TremendousStorage's inventory panes.
 *
 * <p>When JEI is present, the JEI plugin registers both a provider (JEI→TS) and a setter
 * (TS→JEI) so the two search bars stay in sync.  When JEI is absent, the TS EditBox is the
 * sole filter source and the JEI methods are no-ops.
 *
 * <p>Only {@code TremendousStorageJeiPlugin} should call {@link #setProvider} /
 * {@link #setJeiSetter}; all other code uses {@link #getFilter}, {@link #matches},
 * {@link #getRawFilter}, and {@link #pushToJei}.
 */
public final class SearchSync {

    private static Supplier<String> provider = () -> "";
    private static Consumer<String> jeiSetter = null;
    private static boolean jeiAvailable = false;

    private SearchSync() {}

    /** Called by the JEI plugin when the JEI runtime becomes available or is torn down. */
    public static void setProvider(Supplier<String> p) {
        provider = (p != null) ? p : () -> "";
        jeiAvailable = (p != null);
    }

    /** Called by the JEI plugin to register the setter that pushes text into the JEI search bar. */
    public static void setJeiSetter(Consumer<String> setter) {
        jeiSetter = setter;
    }

    /** Returns {@code true} when JEI is loaded and its search provider is active. */
    public static boolean isJeiAvailable() {
        return jeiAvailable;
    }

    /**
     * Returns the current JEI filter text exactly as typed (no lowercase / strip), or {@code ""}
     * when JEI is absent.  Use this to drive a TS search {@link net.minecraft.client.gui.components.EditBox}
     * so that it stays in sync with the JEI search bar.
     */
    public static String getRawFilter() {
        return provider.get();
    }

    /**
     * Pushes {@code text} into the JEI ingredient filter.  No-op when JEI is absent.
     * Call this from the TS search EditBox responder so typing in TS also updates JEI.
     */
    public static void pushToJei(String text) {
        if (jeiSetter != null) {
            jeiSetter.accept(text);
        }
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
            return stack.getItem().builtInRegistryHolder().tags().anyMatch(tag -> tag.location()
                    .toString()
                    .toLowerCase(Locale.ROOT)
                    .contains(query));
        }
        return stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(filter);
    }
}
