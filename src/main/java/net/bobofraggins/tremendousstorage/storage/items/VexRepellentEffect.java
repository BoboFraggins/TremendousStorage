package net.bobofraggins.tremendousstorage.storage.items;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Vex Repellent effect: uses the Positive Vibes colour (#048ECF) for its
 * ambient particles and periodically propels nearby Vexes away from the bearer.
 */
public class VexRepellentEffect extends MobEffect {

    /** Average colour sampled from {@code positive_vibes_flow.png}: R=4, G=142, B=207. */
    public VexRepellentEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x048ECF);
    }
}
