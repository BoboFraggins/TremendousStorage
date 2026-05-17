package net.bobofraggins.tremendousstorage.experiencesyringe;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Exposes the Experience Syringe as a fluid {@link ResourceHandler}{@code <FluidResource>}.
 *
 * <p>Presents XP storage as the active XP fluid, converting between XP points and mB at the
 * ratio defined in {@link ExperienceSyringeItem} (1 XP = 20 mB). All amounts are rounded to
 * whole XP points so no fluid is silently discarded mid-transfer.
 *
 * <p>The handler works on the passed item stack directly. After insert/extract, retrieve the
 * updated stack via {@link #getContainer()}.
 */
public class ExperienceSyringeFluidHandler implements ResourceHandler<FluidResource> {

    private static final TagKey<Fluid> EXPERIENCE_FLUID_TAG =
            TagKey.create(Registries.FLUID, Identifier.fromNamespaceAndPath("c", "experience"));

    private final ItemStack container;

    public ExperienceSyringeFluidHandler(ItemStack stack) {
        this.container = stack;
    }

    public ItemStack getContainer() {
        return container;
    }

    private int storedXp() {
        return container.getOrDefault(Registration.EXPERIENCE_SYRINGE_STORED_XP, 0);
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public FluidResource getResource(int index) {
        int stored = storedXp();
        if (stored == 0) return FluidResource.EMPTY;
        Fluid xpFluid = Registration.XP_JUICE_SOURCE.get();
        return xpFluid == Fluids.EMPTY ? FluidResource.EMPTY : FluidResource.of(xpFluid);
    }

    @Override
    public long getAmountAsLong(int index) {
        return ExperienceSyringeItem.xpToMb(storedXp());
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        return ExperienceSyringeItem.xpToMb(ExperienceSyringeItem.CAPACITY);
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return !resource.isEmpty() && resource.toStack(1).is(EXPERIENCE_FLUID_TAG);
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext tx) {
        if (!isValid(index, resource) || amount <= 0) return 0;
        int stored = storedXp();
        int spaceXp = ExperienceSyringeItem.CAPACITY - stored;
        if (spaceXp <= 0) return 0;
        int xpToAdd = Math.min(ExperienceSyringeItem.mbToXp(amount), spaceXp);
        if (xpToAdd <= 0) return 0;
        int mbConsumed = ExperienceSyringeItem.xpToMb(xpToAdd);
        container.set(Registration.EXPERIENCE_SYRINGE_STORED_XP, stored + xpToAdd);
        return mbConsumed;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext tx) {
        if (!isValid(index, resource) || amount <= 0) return 0;
        int stored = storedXp();
        if (stored == 0) return 0;
        int xpToDrain = Math.min(ExperienceSyringeItem.mbToXp(amount), stored);
        if (xpToDrain <= 0) return 0;
        int mbDrained = ExperienceSyringeItem.xpToMb(xpToDrain);
        container.set(Registration.EXPERIENCE_SYRINGE_STORED_XP, stored - xpToDrain);
        return mbDrained;
    }
}
