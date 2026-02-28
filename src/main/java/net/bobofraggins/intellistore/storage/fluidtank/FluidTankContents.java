package net.bobofraggins.intellistore.storage.fluidtank;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Data component storing the fluid type and amount inside a Fluid Tank block item.
 *
 * <p>Stored on the block item so that the loot table {@code copy_components} function preserves
 * state when the tank is broken, and so Whiteout Tape can clear it in the crafting grid.
 *
 * <p>The {@code storedFluid} uses {@link FluidStack#OPTIONAL_CODEC} — EMPTY encodes as absent
 * so an unlocked tank serializes cleanly. {@code amount} is stored as a separate long.
 */
public record FluidTankContents(FluidStack storedFluid, long amount) {

    public static final FluidTankContents EMPTY = new FluidTankContents(FluidStack.EMPTY, 0);

    public static final Codec<FluidTankContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    FluidStack.OPTIONAL_CODEC.fieldOf("fluid").forGetter(FluidTankContents::storedFluid),
                    Codec.LONG.optionalFieldOf("amount", 0L).forGetter(FluidTankContents::amount))
            .apply(instance, FluidTankContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidTankContents> STREAM_CODEC = StreamCodec.composite(
            FluidStack.OPTIONAL_STREAM_CODEC,
            FluidTankContents::storedFluid,
            ByteBufCodecs.VAR_LONG,
            FluidTankContents::amount,
            FluidTankContents::new);

    /** True if the tank has no fluid and no lock. */
    public boolean isEmpty() {
        return storedFluid.isEmpty() || amount == 0;
    }

    /** True if the tank has been locked to a fluid type (even if amount == 0). */
    public boolean isLocked() {
        return !storedFluid.isEmpty();
    }
}
