package net.bobofraggins.tremendousstorage.storage.tubeattachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * Data component carried by an Import or Export Interface item.
 *
 * <p>Stores 9 ghost-item filter slots. When the interface is punched off or dropped with the
 * tube, the current filter is serialized back onto the item so that re-placing it restores
 * the previous config.
 *
 * <p>{@code slots} always has exactly 9 entries; empty entries are {@link ItemStack#EMPTY}.
 */
public record InterfaceFilterContents(List<ItemStack> slots) {

    public static final int SLOT_COUNT = 9;

    public static final InterfaceFilterContents EMPTY = new InterfaceFilterContents(emptySlots());

    private static List<ItemStack> emptySlots() {
        List<ItemStack> list = new ArrayList<>(SLOT_COUNT);
        for (int i = 0; i < SLOT_COUNT; i++) list.add(ItemStack.EMPTY);
        return list;
    }

    public boolean isEmpty() {
        for (ItemStack stack : slots) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    public static final Codec<InterfaceFilterContents> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(ItemStack.OPTIONAL_CODEC
                            .listOf(SLOT_COUNT, SLOT_COUNT)
                            .fieldOf("slots")
                            .forGetter(InterfaceFilterContents::slots))
                    .apply(instance, InterfaceFilterContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, InterfaceFilterContents> STREAM_CODEC =
            StreamCodec.composite(
                    ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(SLOT_COUNT)),
                    InterfaceFilterContents::slots,
                    InterfaceFilterContents::new);
}
