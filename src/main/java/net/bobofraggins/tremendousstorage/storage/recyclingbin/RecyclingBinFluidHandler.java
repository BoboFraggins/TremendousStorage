package net.bobofraggins.tremendousstorage.storage.recyclingbin;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Exposes the Recycling Bin's Quantum Foam fluid tank as an {@link IFluidHandler}.
 * Accepts insert and extract of Quantum Foam only; capacity is fixed at
 * {@value RecyclingBinBlockEntity#FLUID_CAPACITY_MB} mB.
 */
public class RecyclingBinFluidHandler implements IFluidHandler {

    private final RecyclingBinBlockEntity be;

    public RecyclingBinFluidHandler(RecyclingBinBlockEntity be) {
        this.be = be;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        int amount = be.getVibesAmount();
        if (amount == 0) return FluidStack.EMPTY;
        return new FluidStack(Registration.QUANTUM_FOAM_SOURCE.get(), amount);
    }

    @Override
    public int getTankCapacity(int tank) {
        return RecyclingBinBlockEntity.FLUID_CAPACITY_MB;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return !stack.isEmpty() && stack.getFluid() == Registration.QUANTUM_FOAM_SOURCE.get();
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (!isFluidValid(0, resource)) return 0;
        int space = RecyclingBinBlockEntity.FLUID_CAPACITY_MB - be.getVibesAmount();
        int toFill = Math.min(space, resource.getAmount());
        if (toFill <= 0) return 0;
        if (!action.simulate()) {
            be.addVibes(toFill);
        }
        return toFill;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (!isFluidValid(0, resource)) return FluidStack.EMPTY;
        return drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        int drained = be.extractVibes(maxDrain, action.simulate());
        if (drained == 0) return FluidStack.EMPTY;
        return new FluidStack(Registration.QUANTUM_FOAM_SOURCE.get(), drained);
    }
}
