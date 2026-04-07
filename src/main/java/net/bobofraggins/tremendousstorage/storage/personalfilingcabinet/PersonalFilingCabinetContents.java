package net.bobofraggins.tremendousstorage.storage.personalfilingcabinet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * Data component stored on every Personal Filing Cabinet ItemStack.
 *
 * <p>{@code folders} holds exactly 8 slots (some may be {@link ItemStack#EMPTY}).
 * Each non-empty slot must be a Manila Folder item.
 * {@code voidExcess} controls whether overflow from auto-pickup is silently discarded (default true).
 */
public record PersonalFilingCabinetContents(List<ItemStack> folders, boolean voidExcess) {

    public static final int FOLDER_SLOTS = 8;

    public static final PersonalFilingCabinetContents EMPTY;

    static {
        List<ItemStack> emptyFolders = new ArrayList<>(FOLDER_SLOTS);
        for (int i = 0; i < FOLDER_SLOTS; i++) emptyFolders.add(ItemStack.EMPTY);
        EMPTY = new PersonalFilingCabinetContents(List.copyOf(emptyFolders), true);
    }

    public static final Codec<PersonalFilingCabinetContents> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                            ItemStack.OPTIONAL_CODEC
                                    .listOf()
                                    .fieldOf("folders")
                                    .forGetter(PersonalFilingCabinetContents::folders),
                            Codec.BOOL
                                    .optionalFieldOf("void_excess", true)
                                    .forGetter(PersonalFilingCabinetContents::voidExcess))
                    .apply(instance, PersonalFilingCabinetContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PersonalFilingCabinetContents> STREAM_CODEC =
            StreamCodec.composite(
                    ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()),
                    PersonalFilingCabinetContents::folders,
                    ByteBufCodecs.BOOL,
                    PersonalFilingCabinetContents::voidExcess,
                    PersonalFilingCabinetContents::new);

    /** Returns the type reference; set by Registration. */
    public static DataComponentType<PersonalFilingCabinetContents> type() {
        return net.bobofraggins.tremendousstorage.shared.register.Registration.PFC_CONTENTS.get();
    }
}
