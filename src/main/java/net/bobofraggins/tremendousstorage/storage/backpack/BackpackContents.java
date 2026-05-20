package net.bobofraggins.tremendousstorage.storage.backpack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.bobofraggins.tremendousstorage.shared.config.SortMode;
import net.bobofraggins.tremendousstorage.shared.priority.Priority;
import net.bobofraggins.tremendousstorage.shared.storage.StorageKey;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * DataComponent stored on every Tremendous Backpack ItemStack.
 *
 * <p>Mirrors the storage model of {@code ChestBlockEntity}: items are identified
 * by type+component-patch key, stored with long counts, in insertion order.
 * Instances are immutable; mutation returns new instances via {@link #withInserted} and
 * {@link #withExtracted}.
 */
public final class BackpackContents {

    /** One stored item type with its count. */
    public record Entry(ItemStack type, long count) {

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        ItemStack.OPTIONAL_CODEC.fieldOf("type").forGetter(Entry::type),
                        Codec.LONG.fieldOf("count").forGetter(Entry::count))
                .apply(instance, Entry::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ItemStack.OPTIONAL_STREAM_CODEC, Entry::type, ByteBufCodecs.VAR_LONG, Entry::count, Entry::new);
    }

    public static DataComponentType<BackpackContents> type() {
        return net.bobofraggins.tremendousstorage.shared.register.Registration.TREMENDOUS_BACKPACK_CONTENTS.get();
    }

    public static final BackpackContents EMPTY =
            new BackpackContents(List.of(), StorageTier.WOOD, Priority.LOW, SortMode.AMOUNT, false);

    public static final Codec<BackpackContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Entry.CODEC.listOf().optionalFieldOf("entries", List.of()).forGetter(c -> c.entries),
                    Codec.STRING.optionalFieldOf("tier", "wood").forGetter(c -> c.tier.getId()),
                    Codec.INT.optionalFieldOf("priority", 0).forGetter(c -> c.priority.ordinal()),
                    Codec.STRING.optionalFieldOf("sort_mode", "AMOUNT").forGetter(c -> c.sortMode.name()),
                    Codec.BOOL.optionalFieldOf("crafting_upgrade", false).forGetter(c -> c.hasCraftingUpgrade))
            .apply(
                    instance,
                    (entries, tier, priority, sortMode, craftingUpgrade) -> new BackpackContents(
                            entries,
                            StorageTier.fromId(tier),
                            Priority.fromOrdinal(priority),
                            parseSortMode(sortMode),
                            craftingUpgrade)));

    public static final StreamCodec<RegistryFriendlyByteBuf, BackpackContents> STREAM_CODEC = new StreamCodec<>() {
        private final StreamCodec<RegistryFriendlyByteBuf, List<Entry>> listCodec =
                Entry.STREAM_CODEC.apply(ByteBufCodecs.list());

        @Override
        public BackpackContents decode(RegistryFriendlyByteBuf buf) {
            List<Entry> entries = listCodec.decode(buf);
            StorageTier tier = StorageTier.fromId(buf.readUtf());
            Priority priority = Priority.fromOrdinal(buf.readVarInt());
            SortMode sortMode = parseSortMode(buf.readUtf());
            boolean craftingUpgrade = buf.readBoolean();
            return new BackpackContents(entries, tier, priority, sortMode, craftingUpgrade);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, BackpackContents value) {
            listCodec.encode(buf, value.entries);
            buf.writeUtf(value.tier.getId());
            buf.writeVarInt(value.priority.ordinal());
            buf.writeUtf(value.sortMode.name());
            buf.writeBoolean(value.hasCraftingUpgrade);
        }
    };

    private final List<Entry> entries;
    private final StorageTier tier;
    private final Priority priority;
    private final SortMode sortMode;
    private final boolean hasCraftingUpgrade;

    public BackpackContents(
            List<Entry> entries, StorageTier tier, Priority priority, SortMode sortMode, boolean hasCraftingUpgrade) {
        this.entries = List.copyOf(entries);
        this.tier = tier;
        this.priority = priority;
        this.sortMode = sortMode;
        this.hasCraftingUpgrade = hasCraftingUpgrade;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public List<Entry> entries() {
        return entries;
    }

    public StorageTier tier() {
        return tier;
    }

    public Priority priority() {
        return priority;
    }

    public SortMode sortMode() {
        return sortMode;
    }

    public boolean hasCraftingUpgrade() {
        return hasCraftingUpgrade;
    }

    public long getCapacity() {
        return tier.getCapacity();
    }

    public long totalCount() {
        long total = 0;
        for (Entry e : entries) total += e.count();
        return total;
    }

    public int typeCount() {
        return entries.size();
    }

    public ItemStack getType(int index) {
        if (index < 0 || index >= entries.size()) return ItemStack.EMPTY;
        return entries.get(index).type().copyWithCount(1);
    }

    public long getCount(int index) {
        if (index < 0 || index >= entries.size()) return 0L;
        return entries.get(index).count();
    }

    // -------------------------------------------------------------------------
    // Mutation — returns new instances
    // -------------------------------------------------------------------------

    /**
     * Inserts up to {@code amount} items of the given type.
     *
     * @return two-element array: [remainder (Long), newContents] where remainder is the amount
     *     that could NOT be inserted.
     */
    public Object[] withInserted(ItemStack type, long amount) {
        if (amount <= 0 || type.isEmpty()) return new Object[] {amount, this};
        long space = getCapacity() - totalCount();
        if (space <= 0) return new Object[] {amount, this};
        long toInsert = Math.min(amount, space);
        StorageKey key = StorageKey.of(type);
        List<Entry> mutable = new ArrayList<>(entries);
        boolean found = false;
        for (int i = 0; i < mutable.size(); i++) {
            if (StorageKey.of(mutable.get(i).type()).equals(key)) {
                mutable.set(i, new Entry(mutable.get(i).type(), mutable.get(i).count() + toInsert));
                found = true;
                break;
            }
        }
        if (!found) mutable.add(new Entry(type.copyWithCount(1), toInsert));
        return new Object[] {
            amount - toInsert, new BackpackContents(mutable, tier, priority, sortMode, hasCraftingUpgrade)
        };
    }

    /**
     * Extracts up to {@code amount} items from the entry at {@code index}.
     *
     * @return two-element array: [extracted ItemStack, newContents] — stack is EMPTY if nothing
     *     was extracted.
     */
    public Object[] withExtracted(int index, long amount) {
        if (index < 0 || index >= entries.size()) return new Object[] {ItemStack.EMPTY, this};
        Entry e = entries.get(index);
        long toExtract = Math.min(amount, Math.min(e.type().getMaxStackSize(), e.count()));
        if (toExtract <= 0) return new Object[] {ItemStack.EMPTY, this};
        List<Entry> mutable = new ArrayList<>(entries);
        long remaining = e.count() - toExtract;
        if (remaining == 0) {
            int lastIdx = mutable.size() - 1;
            Entry last = mutable.remove(lastIdx);
            if (index != lastIdx) mutable.set(index, last);
        } else {
            mutable.set(index, new Entry(e.type(), remaining));
        }
        ItemStack extracted = e.type().copyWithCount((int) toExtract);
        return new Object[] {extracted, new BackpackContents(mutable, tier, priority, sortMode, hasCraftingUpgrade)};
    }

    public BackpackContents withTier(StorageTier newTier) {
        return new BackpackContents(entries, newTier, priority, sortMode, hasCraftingUpgrade);
    }

    public BackpackContents withPriority(Priority newPriority) {
        return new BackpackContents(entries, tier, newPriority, sortMode, hasCraftingUpgrade);
    }

    public BackpackContents withSortMode(SortMode newSortMode) {
        return new BackpackContents(entries, tier, priority, newSortMode, hasCraftingUpgrade);
    }

    public BackpackContents withCraftingUpgrade() {
        return new BackpackContents(entries, tier, priority, sortMode, true);
    }

    // -------------------------------------------------------------------------
    // equals / hashCode (required for DataComponent change detection)
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BackpackContents other)) return false;
        return tier == other.tier
                && priority == other.priority
                && sortMode == other.sortMode
                && hasCraftingUpgrade == other.hasCraftingUpgrade
                && entries.size() == other.entries.size();
    }

    @Override
    public int hashCode() {
        int r = tier.hashCode();
        r = 31 * r + priority.hashCode();
        r = 31 * r + sortMode.hashCode();
        r = 31 * r + Boolean.hashCode(hasCraftingUpgrade);
        r = 31 * r + entries.size();
        return r;
    }

    private static SortMode parseSortMode(String s) {
        try {
            return SortMode.valueOf(s);
        } catch (IllegalArgumentException e) {
            return SortMode.AMOUNT;
        }
    }
}
