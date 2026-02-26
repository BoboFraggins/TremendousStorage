package net.bobofraggins.intellistore.manillafolder;

/** Material tiers for Manila Folders, in ascending order of capacity. */
public enum FolderTier {
    PAPER("paper", 4_096L),
    COPPER("copper", 16_384L),
    IRON("iron", 65_536L),
    GOLD("gold", 131_072L),
    DIAMOND("diamond", 524_288L),
    EMERALD("emerald", 1_048_576L),
    NETHERITE("netherite", 4_294_967_296L);

    private final String id;
    private final long defaultCapacity;

    FolderTier(String id, long defaultCapacity) {
        this.id = id;
        this.defaultCapacity = defaultCapacity;
    }

    public String getId() {
        return id;
    }

    public long getDefaultCapacity() {
        return defaultCapacity;
    }
}
