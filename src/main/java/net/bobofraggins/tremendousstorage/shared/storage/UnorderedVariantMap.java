package net.bobofraggins.tremendousstorage.shared.storage;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

/**
 * {@link VariantCounter} backed by an {@link Object2LongOpenHashMap}.
 *
 * <p>Used for all non-damageable items where there is no need for damage-range queries.
 * All operations are O(1) amortised.
 */
final class UnorderedVariantMap implements VariantCounter {

    private final Object2LongOpenHashMap<StorageKey> map = new Object2LongOpenHashMap<>();

    @Override
    public long getAmount(StorageKey key) {
        return map.getLong(key);
    }

    @Override
    public void add(StorageKey key, long delta) {
        long newVal = map.getLong(key) + delta;
        if (newVal <= 0) {
            map.removeLong(key);
        } else {
            map.put(key, newVal);
        }
    }

    @Override
    public void remove(StorageKey key) {
        map.removeLong(key);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Iterable<Object2LongMap.Entry<StorageKey>> entries() {
        return map.object2LongEntrySet();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }
}
