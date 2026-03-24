package net.bobofraggins.intellistore.external.create;

import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import java.util.List;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Fan processing type that activates when the airflow passes through Healing Salve fluid.
 *
 * <p>Transforms Zombie Brains into Brains. Tints the fan's airflow particles teal-blue,
 * sampled from the {@code healing_salve_flow.png} texture (avg colour #048ECF).
 * Living entities caught in the flow receive a brief Regeneration I effect.
 */
public class HealingSalveFanProcessingType implements FanProcessingType {

    /** Average colour sampled from {@code healing_salve_flow.png}: R=4, G=142, B=207. */
    private static final Vector3f PARTICLE_COLOR = new Vector3f(4 / 255f, 142 / 255f, 207 / 255f);

    /** Packed RGB used to tint the fan's air-flow particle stream. */
    private static final int AIR_FLOW_COLOR = 0x048ECF;

    @Override
    public boolean isValidAt(Level level, BlockPos pos) {
        return level.getFluidState(pos).getFluidType() == Registration.HEALING_SALVE_TYPE.get();
    }

    @Override
    public int getPriority() {
        return 400;
    }

    @Override
    public boolean canProcess(ItemStack stack, Level level) {
        return stack.is(Registration.ZOMBIE_BRAIN.get());
    }

    @Override
    public List<ItemStack> process(ItemStack stack, Level level) {
        if (!stack.is(Registration.ZOMBIE_BRAIN.get())) return null;
        return List.of(new ItemStack(Registration.BRAIN.get()));
    }

    @Override
    public void spawnProcessingParticles(Level level, Vec3 pos) {
        if (level.random.nextInt(8) != 0) return;
        DustParticleOptions particle = new DustParticleOptions(PARTICLE_COLOR, 1f);
        level.addParticle(
                particle,
                pos.x + (level.random.nextFloat() - 0.5f) * 0.5f,
                pos.y + 0.1f,
                pos.z + (level.random.nextFloat() - 0.5f) * 0.5f,
                0,
                0,
                0);
    }

    @Override
    public void morphAirFlow(AirFlowParticleAccess access, RandomSource random) {
        access.setColor(AIR_FLOW_COLOR);
    }

    @Override
    public void affectEntity(Entity entity, Level level) {
        if (entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, false, false));
        }
    }
}
