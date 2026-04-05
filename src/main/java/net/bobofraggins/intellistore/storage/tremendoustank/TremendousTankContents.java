package net.bobofraggins.intellistore.storage.tremendoustank;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Data component storing the fluid type and amount inside a Tremendous Tank block item.
 *
 * <p>Stored on the block item so that the loot table {@code copy_components} function preserves
 * state when the tank is broken, and so Whiteout Tape can clear it in the crafting grid.
 *
 * <p>The {@code storedFluid} uses {@link FluidStack#OPTIONAL_CODEC} — EMPTY encodes as absent
 * so an unlocked tank serializes cleanly. {@code amount} is stored as a separate long.
 */
public record TremendousTankContents(FluidStack storedFluid, long amount) {

    public static final TremendousTankContents EMPTY = new TremendousTankContents(FluidStack.EMPTY, 0);

    public static final Codec<TremendousTankContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    FluidStack.OPTIONAL_CODEC.fieldOf("fluid").forGetter(TremendousTankContents::storedFluid),
                    Codec.LONG.optionalFieldOf("amount", 0L).forGetter(TremendousTankContents::amount))
            .apply(instance, TremendousTankContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TremendousTankContents> STREAM_CODEC =
            StreamCodec.composite(
                    FluidStack.OPTIONAL_STREAM_CODEC,
                    TremendousTankContents::storedFluid,
                    ByteBufCodecs.VAR_LONG,
                    TremendousTankContents::amount,
                    TremendousTankContents::new);

    /** True if the tank has no fluid and no lock. */
    public boolean isEmpty() {
        return storedFluid.isEmpty() || amount == 0;
    }

    /** True if the tank has been locked to a fluid type (even if amount == 0). */
    public boolean isLocked() {
        return !storedFluid.isEmpty();
    }
}
