package net.bobofraggins.intellistore.shared.storage;

import it.unimi.dsi.fastutil.objects.Object2LongMap;

/**
 * A mutable counter mapping {@link StorageKey} → {@code long} amount.
 *
 * <p>Two implementations exist:
 * <ul>
 *   <li>{@link UnorderedVariantMap} — backed by an {@code Object2LongOpenHashMap}; used for all
 *       non-damageable items where damage-range queries are never needed.
 *   <li>{@link FuzzyVariantMap} — backed by an {@code Object2LongAVLTreeMap} sorted by damage
 *       value ascending; used for damageable items to support efficient range queries via
 *       {@link FuzzyVariantMap#findFuzzy(int, int)}.
 * </ul>
 */
public interface VariantCounter {

    /** Returns the stored amount for {@code key}, or {@code 0} if absent. */
    long getAmount(StorageKey key);

    /**
     * Adds {@code delta} to the stored amount for {@code key}.
     *
     * <p>If the resulting amount is ≤ 0 the entry is removed entirely.
     */
    void add(StorageKey key, long delta);

    /** Removes the entry for {@code key} entirely (regardless of its current amount). */
    void remove(StorageKey key);

    /** Removes all entries. */
    void clear();

    /** Returns all entries in this counter (order is implementation-defined). */
    Iterable<Object2LongMap.Entry<StorageKey>> entries();

    /** Returns {@code true} if no entries are present. */
    boolean isEmpty();
}
