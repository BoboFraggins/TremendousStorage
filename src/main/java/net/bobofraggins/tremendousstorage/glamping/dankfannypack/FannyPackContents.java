package net.bobofraggins.tremendousstorage.glamping.dankfannypack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.bobofraggins.tremendousstorage.shared.ui.AbstractFilingCabinetMenu;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Data component stored on every Dank Fanny Pack item stack.
 *
 * <p>Holds the 8 Manila Folder slots (via {@link ItemContainerContents}) plus void-excess and
 * priority settings. Instances are immutable; mutation returns new instances via the {@code with*}
 * helpers.
 */
public record FannyPackContents(ItemContainerContents slots, boolean voidExcess, int priority) {

    public static final FannyPackContents EMPTY = new FannyPackContents(ItemContainerContents.EMPTY, false, 0);

    public static final Codec<FannyPackContents> CODEC = RecordCodecBuilder.create(i -> i.group(
                    ItemContainerContents.CODEC
                            .optionalFieldOf("slots", ItemContainerContents.EMPTY)
                            .forGetter(FannyPackContents::slots),
                    Codec.BOOL.optionalFieldOf("void_excess", false).forGetter(FannyPackContents::voidExcess),
                    Codec.INT.optionalFieldOf("priority", 0).forGetter(FannyPackContents::priority))
            .apply(i, FannyPackContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FannyPackContents> STREAM_CODEC = StreamCodec.composite(
            ItemContainerContents.STREAM_CODEC,
            FannyPackContents::slots,
            ByteBufCodecs.BOOL,
            FannyPackContents::voidExcess,
            ByteBufCodecs.VAR_INT,
            FannyPackContents::priority,
            FannyPackContents::new);

    /** Returns the 8 folder slots as a fixed-size list, with empty slots filled by EMPTY. */
    public NonNullList<ItemStack> toItemList() {
        NonNullList<ItemStack> list = NonNullList.withSize(AbstractFilingCabinetMenu.FOLDER_SLOTS, ItemStack.EMPTY);
        slots.copyInto(list);
        return list;
    }

    public FannyPackContents withSlots(NonNullList<ItemStack> items) {
        return new FannyPackContents(ItemContainerContents.fromItems(items), voidExcess, priority);
    }

    public FannyPackContents withVoidExcess(boolean voidExcess) {
        return new FannyPackContents(slots, voidExcess, priority);
    }

    public FannyPackContents withPriority(int priority) {
        return new FannyPackContents(slots, voidExcess, priority);
    }
}
