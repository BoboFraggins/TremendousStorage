package net.bobofraggins.tremendousstorage.shared.storage;

import java.util.Objects;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Immutable key identifying a specific item type + component combination.
 *
 * <p>Used as a map key in the storage data structures. Pre-computes its hash at construction so
 * that repeated O(1) map lookups never re-hash the {@link DataComponentPatch}.
 *
 * <p>All instances are created through the factory methods — the constructor is private.
 */
public final class StorageKey {

    /** Item singleton; identity comparison (==) is valid because Item instances are singletons. */
    public final Item item;

    /** Component delta from defaults (copied from the source stack). */
    final DataComponentPatch components;

    /** Pre-computed hash — never re-computed after construction. */
    private final int hashCode;

    private StorageKey(Item item, DataComponentPatch components) {
        this.item = item;
        this.components = components;
        this.hashCode = Objects.hash(item, components);
    }

    /**
     * Creates a key from a non-empty stack, capturing its item type and component patch.
     *
     * @throws IllegalArgumentException if the stack is empty
     */
    public static StorageKey of(ItemStack stack) {
        if (stack.isEmpty()) throw new IllegalArgumentException("Cannot create StorageKey from empty stack");
        return new StorageKey(stack.getItem(), stack.getComponentsPatch());
    }

    /**
     * Creates a key that matches by item type only, ignoring all component data.
     *
     * <p>Used for fuzzy / range queries (e.g. "any item of this type").
     *
     * @throws IllegalArgumentException if the stack is empty
     */
    public static StorageKey ofFuzzy(ItemStack stack) {
        if (stack.isEmpty()) throw new IllegalArgumentException("Cannot create StorageKey from empty stack");
        return new StorageKey(stack.getItem(), DataComponentPatch.EMPTY);
    }

    /** Returns true if {@code other} has the same item type, ignoring component data. */
    public boolean isPrimarilyEqual(StorageKey other) {
        return this.item == other.item;
    }

    /** Reconstructs a display stack (count = 1) with this key's item and components applied. */
    public ItemStack toDisplayStack() {
        return new ItemStack(BuiltInRegistries.ITEM.wrapAsHolder(item), 1, components);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof StorageKey other)) return false;
        // Fast path: hash mismatch means unequal before touching DataComponentPatch
        if (this.hashCode != other.hashCode) return false;
        if (this.item != other.item) return false;
        return this.components.equals(other.components);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return "StorageKey[" + item + ", " + components + "]";
    }
}
