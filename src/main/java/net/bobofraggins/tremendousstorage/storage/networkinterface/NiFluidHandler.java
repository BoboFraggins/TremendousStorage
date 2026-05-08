package net.bobofraggins.tremendousstorage.storage.networkinterface;

import java.util.List;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Aggregated {@link IFluidHandler} for the Network Interface.
 *
 * <p>Exposes all fluid handlers reachable in the network as a flat list of tank slots.
 *
 * <p>Fill order: tanks already holding the requested fluid are filled first (to consolidate),
 * then unlocked (empty) tanks.
 *
 * <p>Drain: {@link #drain(FluidStack, FluidAction)} accumulates across all tanks holding the
 * same fluid. {@link #drain(int, FluidAction)} returns fluid from the first non-empty tank.
 */
public class NiFluidHandler implements IFluidHandler {

    private final List<IFluidHandler> tanks;

    public NiFluidHandler(List<IFluidHandler> tanks) {
        this.tanks = tanks;
    }

    @Override
    public int getTanks() {
        return tanks.size();
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        if (tank < 0 || tank >= tanks.size()) return FluidStack.EMPTY;
        return tanks.get(tank).getFluidInTank(0);
    }

    @Override
    public int getTankCapacity(int tank) {
        if (tank < 0 || tank >= tanks.size()) return 0;
        return tanks.get(tank).getTankCapacity(0);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        if (tank < 0 || tank >= tanks.size()) return false;
        return tanks.get(tank).isFluidValid(0, stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return 0;
        int remaining = resource.getAmount();
        // First pass: top up tanks already holding this fluid type
        for (IFluidHandler handler : tanks) {
            FluidStack stored = handler.getFluidInTank(0);
            if (stored.isEmpty() || !FluidStack.isSameFluidSameComponents(stored, resource)) continue;
            int filled = handler.fill(resource.copyWithAmount(remaining), action);
            remaining -= filled;
            if (remaining <= 0) return resource.getAmount();
        }
        // Second pass: fill empty (unlocked) tanks
        for (IFluidHandler handler : tanks) {
            if (!handler.getFluidInTank(0).isEmpty()) continue;
            int filled = handler.fill(resource.copyWithAmount(remaining), action);
            remaining -= filled;
            if (remaining <= 0) return resource.getAmount();
        }
        return resource.getAmount() - remaining;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return FluidStack.EMPTY;
        int remaining = resource.getAmount();
        int totalDrained = 0;
        FluidStack type = FluidStack.EMPTY;
        for (IFluidHandler handler : tanks) {
            if (remaining <= 0) break;
            FluidStack drained = handler.drain(resource.copyWithAmount(remaining), action);
            if (drained.isEmpty()) continue;
            if (type.isEmpty()) type = drained.copyWithAmount(0);
            totalDrained += drained.getAmount();
            remaining -= drained.getAmount();
        }
        return totalDrained == 0 ? FluidStack.EMPTY : type.copyWithAmount(totalDrained);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0) return FluidStack.EMPTY;
        for (IFluidHandler handler : tanks) {
            FluidStack drained = handler.drain(maxDrain, action);
            if (!drained.isEmpty()) return drained;
        }
        return FluidStack.EMPTY;
    }
}
