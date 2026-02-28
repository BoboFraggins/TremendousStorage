package net.bobofraggins.intellistore.storage.manillafolder;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
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
 */
public record FolderContents(Optional<ItemStack> storedItem, long count) {

    public static final FolderContents EMPTY = new FolderContents(Optional.empty(), 0L);

    public static final Codec<FolderContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    ItemStack.SINGLE_ITEM_CODEC.optionalFieldOf("stored_item").forGetter(FolderContents::storedItem),
                    Codec.LONG.optionalFieldOf("count", 0L).forGetter(FolderContents::count))
            .apply(instance, FolderContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FolderContents> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(ItemStack.STREAM_CODEC),
            FolderContents::storedItem,
            ByteBufCodecs.VAR_LONG,
            FolderContents::count,
            FolderContents::new);

    /** Returns true if this folder has not yet been locked to an item. */
    public boolean isEmpty() {
        return storedItem.isEmpty();
    }

    /**
     * Returns a new {@link FolderContents} with the given item locked in and count set to 1,
     * or {@link Optional#empty()} if this folder is already locked to a different item.
     */
    public Optional<FolderContents> withInitialItem(ItemStack stack) {
        if (!isEmpty()) return Optional.empty();
        ItemStack single = stack.copyWithCount(1);
        return Optional.of(new FolderContents(Optional.of(single), 1L));
    }

    /**
     * Returns a new {@link FolderContents} after inserting {@code toInsert} items, respecting
     * {@code capacity}. The returned value is a pair of the updated contents and any leftover
     * count that did not fit.
     */
    public InsertResult insert(long toInsert, long capacity) {
        long space = capacity - count;
        long accepted = Math.min(space, toInsert);
        long remainder = toInsert - accepted;
        return new InsertResult(new FolderContents(storedItem, count + accepted), remainder);
    }

    /**
     * Returns a new {@link FolderContents} after extracting up to {@code amount} items, and
     * the actual number extracted.
     */
    public ExtractResult extract(long amount) {
        long extracted = Math.min(count, amount);
        return new ExtractResult(new FolderContents(storedItem, count - extracted), extracted);
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

    /** Returns the type reference; set by {@link net.bobofraggins.intellistore.shared.register.Registration}. */
    public static DataComponentType<FolderContents> type() {
        return net.bobofraggins.intellistore.shared.register.Registration.FOLDER_CONTENTS.get();
    }
}
