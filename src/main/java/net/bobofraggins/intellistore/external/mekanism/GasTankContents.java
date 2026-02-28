package net.bobofraggins.intellistore.external.mekanism;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Data component storing the chemical type and amount inside a Gas Tank block item.
 *
 * <p>Stored on the block item so that the loot table {@code copy_components} function preserves
 * state when the tank is broken.
 *
 * <p>Uses {@link ChemicalStack#OPTIONAL_CODEC} — EMPTY encodes as absent so an unlocked tank
 * serializes cleanly. {@code amount} is stored as a separate long.
 */
public record GasTankContents(ChemicalStack storedChemical, long amount) {

    public static final GasTankContents EMPTY = new GasTankContents(ChemicalStack.EMPTY, 0);

    public static final Codec<GasTankContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    ChemicalStack.OPTIONAL_CODEC.fieldOf("chemical").forGetter(GasTankContents::storedChemical),
                    Codec.LONG.optionalFieldOf("amount", 0L).forGetter(GasTankContents::amount))
            .apply(instance, GasTankContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, GasTankContents> STREAM_CODEC = StreamCodec.composite(
            ChemicalStack.OPTIONAL_STREAM_CODEC,
            GasTankContents::storedChemical,
            ByteBufCodecs.VAR_LONG,
            GasTankContents::amount,
            GasTankContents::new);

    /** True if the tank has no chemical and no lock. */
    public boolean isEmpty() {
        return storedChemical.isEmpty() || amount == 0;
    }

    /** True if the tank has been locked to a chemical type (even if amount == 0). */
    public boolean isLocked() {
        return !storedChemical.isEmpty();
    }
}
