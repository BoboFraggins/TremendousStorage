package net.bobofraggins.intellistore.shared.priority;

/** Five-level priority used by storage blocks to determine insert/extract order. */
public enum Priority {
    LOWEST,
    LOW,
    NORMAL,
    HIGH,
    HIGHEST;

    public static final Priority[] VALUES = values();

    /** Returns the Priority for the given ordinal, clamped to [0, 4]. */
    public static Priority fromOrdinal(int i) {
        return VALUES[Math.clamp(i, 0, VALUES.length - 1)];
    }

    /** Translation key for display in UI. */
    public String translationKey() {
        return "priority.intellistore." + name().toLowerCase();
    }
}
