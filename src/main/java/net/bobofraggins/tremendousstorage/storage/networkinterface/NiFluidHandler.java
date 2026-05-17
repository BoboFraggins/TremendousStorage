package net.bobofraggins.tremendousstorage.storage.networkinterface;

import java.util.List;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Aggregated {@link ResourceHandler}{@code <FluidResource>} for the Network Interface.
 *
 * <p>Exposes all fluid handlers reachable in the network as a flat list of tank slots.
 *
 * <p>Insert order: tanks already holding the requested fluid are filled first (to consolidate),
 * then unlocked (empty) tanks.
 *
 * <p>Extract: accumulates across all tanks holding the same fluid.
 */
public class NiFluidHandler implements ResourceHandler<FluidResource> {

    private final List<ResourceHandler<FluidResource>> tanks;

    public NiFluidHandler(List<ResourceHandler<FluidResource>> tanks) {
        this.tanks = tanks;
    }

    @Override
    public int size() {
        return tanks.size();
    }

    @Override
    public FluidResource getResource(int index) {
        if (index < 0 || index >= tanks.size()) return FluidResource.EMPTY;
        return tanks.get(index).getResource(0);
    }

    @Override
    public long getAmountAsLong(int index) {
        if (index < 0 || index >= tanks.size()) return 0;
        return tanks.get(index).getAmountAsLong(0);
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        if (index < 0 || index >= tanks.size()) return 0;
        return tanks.get(index).getCapacityAsLong(0, resource);
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        if (index < 0 || index >= tanks.size()) return false;
        return tanks.get(index).isValid(0, resource);
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext tx) {
        if (resource.isEmpty() || amount <= 0) return 0;
        int remaining = amount;
        // First pass: top up tanks already holding this fluid type
        for (ResourceHandler<FluidResource> handler : tanks) {
            if (handler.getResource(0).isEmpty()) continue;
            if (!resource.equals(handler.getResource(0))) continue;
            int filled = handler.insert(0, resource, remaining, tx);
            remaining -= filled;
            if (remaining <= 0) return amount;
        }
        // Second pass: fill empty (unlocked) tanks
        for (ResourceHandler<FluidResource> handler : tanks) {
            if (!handler.getResource(0).isEmpty()) continue;
            int filled = handler.insert(0, resource, remaining, tx);
            remaining -= filled;
            if (remaining <= 0) return amount;
        }
        return amount - remaining;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext tx) {
        if (resource.isEmpty() || amount <= 0) return 0;
        int remaining = amount;
        int totalExtracted = 0;
        for (ResourceHandler<FluidResource> handler : tanks) {
            if (remaining <= 0) break;
            if (!resource.equals(handler.getResource(0))) continue;
            int extracted = handler.extract(0, resource, remaining, tx);
            totalExtracted += extracted;
            remaining -= extracted;
        }
        return totalExtracted;
    }
}
