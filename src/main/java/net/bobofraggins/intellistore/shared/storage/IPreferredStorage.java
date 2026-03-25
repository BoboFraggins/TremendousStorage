package net.bobofraggins.intellistore.shared.storage;

/**
 * Implemented by {@link net.neoforged.neoforge.items.IItemHandler} wrappers that can
 * report whether they already hold a given item type.
 *
 * <p>Used by {@link net.bobofraggins.intellistore.storage.networkinterface.NiItemHandler}
 * to do preferred-first insertion within each priority bucket: items are routed to a
 * handler that already stores that type before spilling into handlers that don't, preventing
 * fragmentation across containers of the same priority.
 */
public interface IPreferredStorage {
    /**
     * Returns {@code true} if this handler already stores items matching {@code key} and
     * would accept more of the same type.
     */
    boolean isPreferredFor(StorageKey key);
}
