package net.bobofraggins.intellistore.storage.networkinterface;

/**
 * A single entry in the Network Interface's attached-block list.
 *
 * @param translationKey the block's translation key (e.g. {@code "block.intellistore.filing_cabinet"})
 * @param count          number of blocks of this type attached to the network
 */
public record AttachedEntry(String translationKey, int count) {

    /** Convenience constructor for a single block (count = 1). */
    public AttachedEntry(String translationKey) {
        this(translationKey, 1);
    }
}
