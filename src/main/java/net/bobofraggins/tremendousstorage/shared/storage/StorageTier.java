package net.bobofraggins.tremendousstorage.shared.storage;

import javax.annotation.Nullable;

/** Material tiers for upgradeable storage blocks. */
public enum StorageTier implements net.minecraft.util.StringRepresentable {
    WOOD("wood", 4_096L, 0xC8A855),
    COPPER("copper", 16_384L, 0xC07645),
    IRON("iron", 65_536L, 0xCFCFCF),
    GOLD("gold", 131_072L, 0xFFD83D),
    DIAMOND("diamond", 524_288L, 0x4CE6DF),
    EMERALD("emerald", 1_048_576L, 0x17DD62),
    NETHERITE("netherite", 4_194_304L, 0x443A38);

    private final String id;
    private final long capacity;
    /** ARGB color used for the tinted accent on block/item textures. */
    private final int color;

    StorageTier(String id, long capacity, int color) {
        this.id = id;
        this.capacity = capacity;
        this.color = 0xFF000000 | color;
    }

    public static final com.mojang.serialization.Codec<StorageTier> CODEC =
            com.mojang.serialization.Codec.STRING.xmap(StorageTier::fromId, StorageTier::getId);

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, StorageTier>
            STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of(
                    (buf, tier) -> buf.writeUtf(tier.getId()), buf -> fromId(buf.readUtf()));

    public String getId() {
        return id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public long getCapacity() {
        return capacity;
    }

    /** ARGB color for {@code IBlockColor}/{@code IItemColor} tinting. */
    public int getColor() {
        return color;
    }

    /**
     * Returns {@code base} × 4^{@link #ordinal()} — used by tanks to compute their capacity at
     * this tier. WOOD returns {@code base}, COPPER returns {@code base × 4}, etc.
     */
    public long getScaledCapacity(long base) {
        return base << (ordinal() * 2);
    }

    /**
     * Returns the next tier up, or {@code null} if this is already the highest tier.
     *
     * <p>Used by {@link net.bobofraggins.tremendousstorage.storage.storageupgrade.StorageUpgradeItem} to
     * determine which tier an upgrade produces.
     */
    @Nullable
    public StorageTier next() {
        int ord = ordinal() + 1;
        StorageTier[] vals = values();
        return ord < vals.length ? vals[ord] : null;
    }

    /**
     * Looks up a tier by its {@link #getId() id string}, returning {@link #WOOD} as a safe
     * default if the id is unrecognised.
     */
    public static StorageTier fromId(String id) {
        for (StorageTier tier : values()) {
            if (tier.id.equals(id)) return tier;
        }
        return WOOD;
    }
}
