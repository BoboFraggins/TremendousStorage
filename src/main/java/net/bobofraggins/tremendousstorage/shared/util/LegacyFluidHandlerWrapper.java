package net.bobofraggins.tremendousstorage.shared.util;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * Wraps a legacy {@link IFluidHandler} as a {@link ResourceHandler}{@code <FluidResource>}.
 *
 * <p>Transaction semantics are NOT supported: operations execute immediately.
 */
@Deprecated
public class LegacyFluidHandlerWrapper implements ResourceHandler<FluidResource> {

    private final IFluidHandler handler;

    /**
     * Returns the modified container item after drain/fill operations, if the underlying
     * handler is an {@link IFluidHandlerItem}. Returns {@code null} otherwise.
     */
    @Nullable
    public ItemStack getContainer() {
        return handler instanceof IFluidHandlerItem item ? item.getContainer() : null;
    }

    public LegacyFluidHandlerWrapper(IFluidHandler handler) {
        this.handler = handler;
    }

    @Override
    public int size() {
        return handler.getTanks();
    }

    @Override
    public FluidResource getResource(int index) {
        return FluidResource.of(handler.getFluidInTank(index));
    }

    @Override
    public long getAmountAsLong(int index) {
        return handler.getFluidInTank(index).getAmount();
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        return handler.getTankCapacity(index);
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return handler.isFluidValid(index, resource.toStack(1));
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext tx) {
        if (resource.isEmpty() || amount <= 0) return 0;
        FluidStack inTank = handler.getFluidInTank(index);
        if (!inTank.isEmpty() && !FluidStack.isSameFluidSameComponents(inTank, resource.toStack(1))) return 0;
        return handler.fill(resource.toStack(amount), IFluidHandler.FluidAction.EXECUTE);
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext tx) {
        if (resource.isEmpty() || amount <= 0) return 0;
        FluidStack inTank = handler.getFluidInTank(index);
        if (!FluidStack.isSameFluidSameComponents(inTank, resource.toStack(1))) return 0;
        FluidStack drained = handler.drain(resource.toStack(amount), IFluidHandler.FluidAction.EXECUTE);
        return drained.getAmount();
    }
}
