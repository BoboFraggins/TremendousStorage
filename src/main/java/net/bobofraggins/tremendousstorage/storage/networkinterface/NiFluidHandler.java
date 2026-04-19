package net.bobofraggins.tremendousstorage.storage.networkinterface;

import java.util.List;
import net.bobofraggins.tremendousstorage.storage.tank.TankBlockEntity;
import net.bobofraggins.tremendousstorage.storage.tank.TankFluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Aggregated {@link IFluidHandler} for the Network Interface.
 *
 * <p>Exposes all fluid tanks reachable in the network as a flat list of tank slots.
 *
 * <p>Fill order: tanks already holding the requested fluid are filled first (to consolidate),
 * then unlocked (empty) tanks.
 *
 * <p>Drain: {@link #drain(FluidStack, FluidAction)} accumulates across all tanks holding the
 * same fluid. {@link #drain(int, FluidAction)} returns fluid from the first non-empty tank.
 */
public class NiFluidHandler implements IFluidHandler {

    private final List<TankBlockEntity> tanks;

    public NiFluidHandler(List<TankBlockEntity> tanks) {
        this.tanks = tanks;
    }

    private TankFluidHandler handler(int index) {
        return new TankFluidHandler(tanks.get(index));
    }

    @Override
    public int getTanks() {
        return tanks.size();
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        if (tank < 0 || tank >= tanks.size()) return FluidStack.EMPTY;
        return handler(tank).getFluidInTank(0);
    }

    @Override
    public int getTankCapacity(int tank) {
        if (tank < 0 || tank >= tanks.size()) return 0;
        return handler(tank).getTankCapacity(0);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        if (tank < 0 || tank >= tanks.size()) return false;
        return handler(tank).isFluidValid(0, stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return 0;
        int remaining = resource.getAmount();
        // First pass: top up tanks already holding this fluid type
        for (TankBlockEntity tank : tanks) {
            FluidStack stored = tank.getStoredFluid();
            if (stored.isEmpty() || !FluidStack.isSameFluidSameComponents(stored, resource)) continue;
            int filled = new TankFluidHandler(tank).fill(resource.copyWithAmount(remaining), action);
            remaining -= filled;
            if (remaining <= 0) return resource.getAmount();
        }
        // Second pass: fill empty (unlocked) tanks
        for (TankBlockEntity tank : tanks) {
            if (!tank.getStoredFluid().isEmpty()) continue;
            int filled = new TankFluidHandler(tank).fill(resource.copyWithAmount(remaining), action);
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
        for (TankBlockEntity tank : tanks) {
            if (remaining <= 0) break;
            FluidStack drained = new TankFluidHandler(tank).drain(resource.copyWithAmount(remaining), action);
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
        for (TankBlockEntity tank : tanks) {
            FluidStack drained = new TankFluidHandler(tank).drain(maxDrain, action);
            if (!drained.isEmpty()) return drained;
        }
        return FluidStack.EMPTY;
    }
}
