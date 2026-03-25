package net.bobofraggins.intellistore.shared.storage;

/**
 * Implemented by {@link net.neoforged.neoforge.items.IItemHandler} wrappers that can provide
 * accurate item counts directly to a {@link KeyCounter}, bypassing the per-slot stack-size cap
 * imposed by {@link net.neoforged.neoforge.items.IItemHandler#getStackInSlot}.
 */
public interface IKeyCounterContributor {
    void contributeToKeyCounter(KeyCounter kc);
}
