package net.bobofraggins.intellistore.arsnouveau;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Data component storing the source amount inside a Source Tank block item.
 *
 * <p>Unlike the Fluid Tank or Gas Tank, source has no "type" — it is a plain integer.
 * The tank is always "locked" to source; it simply stores or is empty.
 */
public record SourceTankContents(int amount) {

    public static final SourceTankContents EMPTY = new SourceTankContents(0);

    public static final Codec<SourceTankContents> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(Codec.INT.optionalFieldOf("amount", 0).forGetter(SourceTankContents::amount))
                    .apply(instance, SourceTankContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SourceTankContents> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, SourceTankContents::amount, SourceTankContents::new);

    public boolean isEmpty() {
        return amount <= 0;
    }
}
