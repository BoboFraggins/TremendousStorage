package net.bobofraggins.intellistore.storage.fluidtank;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Exposes the Fluid Tank as a single-tank {@link IFluidHandler} for pumps and fluid automation.
 *
 * <p>Insert rules:
 * <ol>
 *   <li>If the tank is unlocked, it locks to the inserted fluid type and fills.
 *   <li>If locked to a matching fluid, fills up to remaining capacity.
 *   <li>If locked to a different fluid, returns 0 (no insertion).
 * </ol>
 *
 * <p>Extract rules:
 * <ol>
 *   <li>If unlocked or amount == 0, returns EMPTY.
 *   <li>Extracts up to {@code min(requested, amount)} mB.
 *   <li>The tank stays locked at amount 0 after full drain.
 * </ol>
 */
public class FluidTankFluidHandler implements IFluidHandler {

    private final FluidTankBlockEntity be;

    public FluidTankFluidHandler(FluidTankBlockEntity be) {
        this.be = be;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        FluidStack type = be.getStoredFluid();
        if (type.isEmpty() || be.getAmount() == 0) return FluidStack.EMPTY;
        long visible = Math.min(Integer.MAX_VALUE, be.getAmount());
        return type.copyWithAmount((int) visible);
    }

    @Override
    public int getTankCapacity(int tank) {
        return (int) Math.min(Integer.MAX_VALUE, be.getCapacity());
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        if (stack.isEmpty()) return false;
        FluidStack stored = be.getStoredFluid();
        if (stored.isEmpty()) return true; // unlocked — accepts anything
        return FluidStack.isSameFluidSameComponents(stored, stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return 0;
        if (be.isVoidExcess()) {
            // Accept anything the tank can match; excess is silently voided
            FluidStack stored = be.getStoredFluid();
            if (!stored.isEmpty() && !FluidStack.isSameFluidSameComponents(stored, resource)) return 0;
            be.insert(resource, resource.getAmount(), action.simulate());
            return resource.getAmount();
        }
        long inserted = be.insert(resource, resource.getAmount(), action.simulate());
        return (int) inserted;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return FluidStack.EMPTY;
        FluidStack stored = be.getStoredFluid();
        if (stored.isEmpty() || !FluidStack.isSameFluidSameComponents(stored, resource)) {
            return FluidStack.EMPTY;
        }
        return be.extract(resource.getAmount(), action.simulate());
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return be.extract(maxDrain, action.simulate());
    }
}
