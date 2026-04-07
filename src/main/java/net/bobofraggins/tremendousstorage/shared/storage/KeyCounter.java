package net.bobofraggins.tremendousstorage.shared.storage;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;

/**
 * Two-level aggregate counter: {@code Item} (identity) → {@link VariantCounter}.
 *
 * <p>The outer map uses {@link Reference2ObjectOpenHashMap} so that the {@link Item} key is
 * looked up by {@code ==} identity — no {@code hashCode()} computation at the outer level,
 * since {@code Item} instances are singletons.
 *
 * <p>For each {@code Item}, the inner {@link VariantCounter} is:
 * <ul>
 *   <li>{@link FuzzyVariantMap} when {@code item.getMaxDamage() > 0} — sorted by damage
 *       ascending so that {@link FuzzyVariantMap#findFuzzy(int, int)} range queries are
 *       possible.
 *   <li>{@link UnorderedVariantMap} otherwise — pure O(1) hash map.
 * </ul>
 */
public final class KeyCounter {

    private final Reference2ObjectOpenHashMap<Item, VariantCounter> outerMap = new Reference2ObjectOpenHashMap<>();

    /**
     * Adds {@code amount} to the stored count for {@code key}.
     *
     * <p>Creates the inner {@link VariantCounter} lazily on first use.
     */
    public void add(StorageKey key, long amount) {
        VariantCounter vc = outerMap.get(key.item);
        if (vc == null) {
            vc = newVariantCounter(key.item);
            outerMap.put(key.item, vc);
        }
        vc.add(key, amount);
    }

    /**
     * Subtracts {@code amount} from the stored count for {@code key}.
     *
     * <p>Removes the inner {@link VariantCounter} entirely if it becomes empty.
     */
    public void remove(StorageKey key, long amount) {
        VariantCounter vc = outerMap.get(key.item);
        if (vc == null) return;
        vc.add(key, -amount);
        if (vc.isEmpty()) outerMap.remove(key.item);
    }

    /** Returns the stored amount for {@code key}, or {@code 0} if absent. */
    public long getAmount(StorageKey key) {
        VariantCounter vc = outerMap.get(key.item);
        return vc == null ? 0 : vc.getAmount(key);
    }

    /**
     * Returns the {@link VariantCounter} for {@code item}, or {@code null} if no entries exist
     * for that item. Callers can cast to {@link FuzzyVariantMap} when
     * {@code item.getMaxDamage() > 0} to access {@link FuzzyVariantMap#findFuzzy(int, int)}.
     */
    @Nullable
    public VariantCounter getVariants(Item item) {
        return outerMap.get(item);
    }

    /** Removes all entries. */
    public void clear() {
        outerMap.clear();
    }

    /** Returns all entries across all items (order is unspecified). */
    public Iterable<Object2LongMap.Entry<StorageKey>> allEntries() {
        return () -> outerMap.values().stream()
                .flatMap(vc -> StreamSupport.stream(vc.entries().spliterator(), false))
                .iterator();
    }

    private static VariantCounter newVariantCounter(Item item) {
        return item.components().has(DataComponents.MAX_DAMAGE) ? new FuzzyVariantMap() : new UnorderedVariantMap();
    }
}
