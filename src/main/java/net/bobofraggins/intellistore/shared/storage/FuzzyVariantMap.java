package net.bobofraggins.intellistore.shared.storage;

import it.unimi.dsi.fastutil.objects.Object2LongAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import java.util.Comparator;
import net.minecraft.core.component.DataComponents;

/**
 * {@link VariantCounter} backed by an {@link Object2LongAVLTreeMap} sorted by damage value
 * ascending.
 *
 * <p>Used for damageable items ({@code item.getMaxDamage() > 0}). The damage-ascending order
 * allows {@link #findFuzzy(int, int)} to efficiently return all entries whose damage falls in a
 * given range (useful for extracting "any sword with ≤ 50% durability").
 *
 * <p>The comparator is: damage ascending, then {@link StorageKey#hashCode()} as tiebreaker for a
 * stable total order. Two distinct keys with the same damage and the same hash (astronomically
 * rare) would collide; this is acceptable in practice.
 */
final class FuzzyVariantMap implements VariantCounter {

    private static final Comparator<StorageKey> DAMAGE_ORDER =
            Comparator.comparingInt(FuzzyVariantMap::getDamage).thenComparingInt(StorageKey::hashCode);

    private final Object2LongAVLTreeMap<StorageKey> tree = new Object2LongAVLTreeMap<>(DAMAGE_ORDER);

    @Override
    public long getAmount(StorageKey key) {
        return tree.getLong(key);
    }

    @Override
    public void add(StorageKey key, long delta) {
        long newVal = tree.getLong(key) + delta;
        if (newVal <= 0) {
            tree.removeLong(key);
        } else {
            tree.put(key, newVal);
        }
    }

    @Override
    public void remove(StorageKey key) {
        tree.removeLong(key);
    }

    @Override
    public void clear() {
        tree.clear();
    }

    @Override
    public Iterable<Object2LongMap.Entry<StorageKey>> entries() {
        return tree.object2LongEntrySet();
    }

    @Override
    public boolean isEmpty() {
        return tree.isEmpty();
    }

    /**
     * Returns all entries whose damage value falls in [{@code minDamage}, {@code maxDamage}]
     * (inclusive). The tree's damage-ascending order means these entries are contiguous, though
     * the current implementation scans the full entry set for simplicity.
     */
    public Iterable<Object2LongMap.Entry<StorageKey>> findFuzzy(int minDamage, int maxDamage) {
        return () -> tree.object2LongEntrySet().stream()
                .filter(e -> {
                    int d = getDamage(e.getKey());
                    return d >= minDamage && d <= maxDamage;
                })
                .iterator();
    }

    /**
     * Extracts the damage value from a {@link StorageKey}'s component patch.
     *
     * <p>Returns {@code 0} when the damage component is absent (fresh / undamaged item).
     */
    static int getDamage(StorageKey key) {
        var patch = key.components.get(DataComponents.DAMAGE);
        return (patch != null && patch.isPresent()) ? patch.get() : 0;
    }
}
