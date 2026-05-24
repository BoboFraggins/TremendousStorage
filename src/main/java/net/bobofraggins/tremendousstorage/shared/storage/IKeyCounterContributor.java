package net.bobofraggins.tremendousstorage.shared.storage;

/**
 * Implemented by {@link net.neoforged.neoforge.items.IItemHandler} wrappers that can provide
 * accurate item counts directly to a {@link KeyCounter}, bypassing the per-slot stack-size cap
 * imposed by {@link net.neoforged.neoforge.items.IItemHandler#getStackInSlot}.
 */
public interface IKeyCounterContributor {
    void contributeToKeyCounter(KeyCounter kc);

    /**
     * Returns the total item capacity contributed by this storage for the inventory counter
     * display. Returns {@code 0} by default; only block entities with a meaningful item capacity
     * (e.g. {@link net.bobofraggins.tremendousstorage.storage.chest.ChestItemHandler}) override
     * this.
     */
    default long getItemCapacity() {
        return 0;
    }
}
