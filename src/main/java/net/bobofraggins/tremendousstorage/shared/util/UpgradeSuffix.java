package net.bobofraggins.tremendousstorage.shared.util;

import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;

/**
 * Fluent builder for the upgrade-label suffix appended to block names.
 *
 * <p>Usage:
 * <pre>{@code
 * String suffix = new UpgradeSuffix()
 *         .tier(tier)
 *         .addIf(hasCraftingUpgrade, "Crafting")
 *         .addIf(hasMagnetUpgrade,   "Magnet")
 *         .toString(); // "" or " (Copper/Crafting/Magnet)"
 * }</pre>
 */
public final class UpgradeSuffix {

    private final StringBuilder sb = new StringBuilder();

    /** Appends the tier display name when above WOOD. */
    public UpgradeSuffix tier(StorageTier tier) {
        if (tier != StorageTier.WOOD) append(tier.getDisplayName());
        return this;
    }

    /** Appends {@code label} when {@code condition} is {@code true}. */
    public UpgradeSuffix addIf(boolean condition, String label) {
        if (condition) append(label);
        return this;
    }

    private void append(String label) {
        if (!sb.isEmpty()) sb.append('/');
        sb.append(label);
    }

    /** Returns {@code ""} when nothing was added, otherwise {@code " (a/b/c)"}. */
    @Override
    public String toString() {
        return sb.isEmpty() ? "" : " (" + sb + ")";
    }
}
