package net.bobofraggins.tremendousstorage.storage.manillafolder;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * Data component stored on every Manila Folder ItemStack.
 *
 * <p>{@code storedItem} is the single item type this folder is locked to.
 * Empty means the folder is unlocked and will accept any item on first insert.
 * {@code count} is the number of items stored, which may exceed a normal stack size.
 * {@code tier} determines the folder's capacity.
 */
public record FolderContents(Optional<ItemStack> storedItem, long count, StorageTier tier) {

    public static final FolderContents EMPTY = new FolderContents(Optional.empty(), 0L, StorageTier.WOOD);

    public static final Codec<FolderContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    ItemStack.CODEC.optionalFieldOf("stored_item").forGetter(FolderContents::storedItem),
                    Codec.LONG.optionalFieldOf("count", 0L).forGetter(FolderContents::count),
                    StorageTier.CODEC.optionalFieldOf("tier", StorageTier.WOOD).forGetter(FolderContents::tier))
            .apply(instance, FolderContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FolderContents> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(ItemStack.STREAM_CODEC),
            FolderContents::storedItem,
            ByteBufCodecs.VAR_LONG,
            FolderContents::count,
            ByteBufCodecs.STRING_UTF8.map(StorageTier::fromId, StorageTier::getId),
            FolderContents::tier,
            FolderContents::new);

    /** Returns true if this folder has not yet been locked to an item. */
    public boolean isEmpty() {
        return storedItem.isEmpty();
    }

    /** Returns the capacity for this folder based on its tier. */
    public long getCapacity() {
        return tier.getCapacity();
    }

    /** Returns a copy of this FolderContents with a different tier. */
    public FolderContents withTier(StorageTier newTier) {
        return new FolderContents(storedItem, count, newTier);
    }

    /**
     * Returns a new {@link FolderContents} with the given item locked in and count set to 1,
     * or {@link Optional#empty()} if this folder is already locked to a different item.
     */
    public Optional<FolderContents> withInitialItem(ItemStack stack) {
        if (!isEmpty()) return Optional.empty();
        ItemStack single = stack.copyWithCount(1);
        return Optional.of(new FolderContents(Optional.of(single), 1L, tier));
    }

    /**
     * Returns a new {@link FolderContents} after inserting {@code toInsert} items, respecting
     * the tier capacity. The returned value is a pair of the updated contents and any leftover
     * count that did not fit.
     */
    public InsertResult insert(long toInsert, long capacity) {
        long space = capacity - count;
        long accepted = Math.min(space, toInsert);
        long remainder = toInsert - accepted;
        return new InsertResult(new FolderContents(storedItem, count + accepted, tier), remainder);
    }

    /**
     * Returns a new {@link FolderContents} after extracting up to {@code amount} items, and
     * the actual number extracted.
     */
    public ExtractResult extract(long amount) {
        long extracted = Math.min(count, amount);
        return new ExtractResult(new FolderContents(storedItem, count - extracted, tier), extracted);
    }

    public record InsertResult(FolderContents updated, long remainder) {}

    public record ExtractResult(FolderContents updated, long extracted) {}

    /**
     * Checks whether this folder is compatible with the given stack (same item, ignoring count).
     */
    public boolean accepts(ItemStack stack) {
        return storedItem
                .map(stored -> ItemStack.isSameItemSameComponents(stored, stack))
                .orElse(true);
    }

    /** Returns the type reference; set by {@link net.bobofraggins.tremendousstorage.shared.register.Registration}. */
    public static DataComponentType<FolderContents> type() {
        return net.bobofraggins.tremendousstorage.shared.register.Registration.FOLDER_CONTENTS.get();
    }
}
