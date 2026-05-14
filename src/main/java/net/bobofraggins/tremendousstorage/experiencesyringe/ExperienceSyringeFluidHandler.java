package net.bobofraggins.tremendousstorage.experiencesyringe;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/**
 * Exposes the Experience Syringe as an {@link IFluidHandlerItem}.
 *
 * <p>The syringe stores XP as integer points internally. This handler presents that storage
 * as the active XP fluid (Mob Grinding Utils' fluid_xp when loaded, our own xp_juice
 * otherwise), converting between XP points and mB at the ratio defined in
 * {@link ExperienceSyringeItem} (1 XP = 20 mB, 1 bucket = 50 XP).
 *
 * <p>All fill/drain amounts are rounded to whole XP points so no fluid is ever
 * silently discarded mid-transfer.
 */
public class ExperienceSyringeFluidHandler implements IFluidHandlerItem {

    private static final TagKey<Fluid> EXPERIENCE_FLUID_TAG =
            TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("c", "experience"));

    private final ItemStack container;

    public ExperienceSyringeFluidHandler(ItemStack stack) {
        this.container = stack;
    }

    @Override
    public ItemStack getContainer() {
        return container;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        int stored = container.getOrDefault(Registration.EXPERIENCE_SYRINGE_STORED_XP, 0);
        if (stored == 0) return FluidStack.EMPTY;
        Fluid xpFluid = Registration.XP_JUICE_SOURCE.get();
        if (xpFluid == Fluids.EMPTY) return FluidStack.EMPTY;
        return new FluidStack(xpFluid, ExperienceSyringeItem.xpToMb(stored));
    }

    @Override
    public int getTankCapacity(int tank) {
        return ExperienceSyringeItem.xpToMb(ExperienceSyringeItem.CAPACITY);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return !stack.isEmpty() && stack.is(EXPERIENCE_FLUID_TAG);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (!isFluidValid(0, resource)) return 0;
        int stored = container.getOrDefault(Registration.EXPERIENCE_SYRINGE_STORED_XP, 0);
        int spaceXp = ExperienceSyringeItem.CAPACITY - stored;
        if (spaceXp <= 0) return 0;
        // Round down to whole XP points so no mB are consumed without storing XP.
        int xpToAdd = Math.min(ExperienceSyringeItem.mbToXp(resource.getAmount()), spaceXp);
        if (xpToAdd <= 0) return 0;
        int mbConsumed = ExperienceSyringeItem.xpToMb(xpToAdd);
        if (action.execute()) {
            container.set(Registration.EXPERIENCE_SYRINGE_STORED_XP, stored + xpToAdd);
        }
        return mbConsumed;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (!isFluidValid(0, resource)) return FluidStack.EMPTY;
        return drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        int stored = container.getOrDefault(Registration.EXPERIENCE_SYRINGE_STORED_XP, 0);
        if (stored == 0) return FluidStack.EMPTY;
        int xpToDrain = Math.min(ExperienceSyringeItem.mbToXp(maxDrain), stored);
        if (xpToDrain <= 0) return FluidStack.EMPTY;
        int mbDrained = ExperienceSyringeItem.xpToMb(xpToDrain);
        if (action.execute()) {
            container.set(Registration.EXPERIENCE_SYRINGE_STORED_XP, stored - xpToDrain);
        }
        Fluid xpFluid = Registration.XP_JUICE_SOURCE.get();
        return new FluidStack(xpFluid, mbDrained);
    }
}
