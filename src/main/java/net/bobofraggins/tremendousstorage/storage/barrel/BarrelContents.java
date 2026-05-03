package net.bobofraggins.tremendousstorage.storage.barrel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * Data component storing the locked item type and quantity on a Barrel block item.
 *
 * <p>{@code storedItem} uses {@link ItemStack#OPTIONAL_CODEC} — empty encodes as absent so an
 * unlocked barrel serialises cleanly. {@code count} is a separate long.
 */
public record BarrelContents(Optional<ItemStack> storedItem, long count) {

    public static final BarrelContents EMPTY = new BarrelContents(Optional.empty(), 0L);

    public static final Codec<BarrelContents> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                    ItemStack.OPTIONAL_CODEC.optionalFieldOf("item").forGetter(BarrelContents::storedItem),
                    Codec.LONG.optionalFieldOf("count", 0L).forGetter(BarrelContents::count))
            .apply(inst, BarrelContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BarrelContents> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs::optional),
            BarrelContents::storedItem,
            ByteBufCodecs.VAR_LONG,
            BarrelContents::count,
            BarrelContents::new);

    public boolean isEmpty() {
        return storedItem.isEmpty() || count == 0;
    }

    public boolean isLocked() {
        return storedItem.isPresent();
    }

    public boolean accepts(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!isLocked()) return true;
        return ItemStack.isSameItemSameComponents(storedItem.get(), stack);
    }
}
