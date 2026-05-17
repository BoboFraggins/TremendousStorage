package net.bobofraggins.tremendousstorage.storage.tank;

import net.bobofraggins.tremendousstorage.shared.storage.IKeyCounterContributor;
import net.bobofraggins.tremendousstorage.shared.storage.KeyCounter;
import net.bobofraggins.tremendousstorage.shared.storage.StorageKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Exposes a {@link ResourceHandler}{@code <FluidResource>}'s contents as a virtual
 * {@link ResourceHandler}{@code <ItemResource>} slot containing filled buckets, so the SAT can
 * display and extract fluid as bucket-form items.
 *
 * <p>The adapter presents at most one slot. The slot is absent ({@link #size()} == 0) when the
 * handler is empty. When present, the stack is the appropriate filled bucket with count == 1.
 *
 * <p>Insertion is always refused — fluids are inserted via the normal fluid capability, not
 * through the SAT item path.
 */
public class TankItemAdapter implements ResourceHandler<ItemResource>, IKeyCounterContributor {

    private final ResourceHandler<FluidResource> fluidHandler;

    public TankItemAdapter(ResourceHandler<FluidResource> fluidHandler) {
        this.fluidHandler = fluidHandler;
    }

    // -------------------------------------------------------------------------
    // ResourceHandler<ItemResource>
    // -------------------------------------------------------------------------

    @Override
    public int size() {
        FluidResource res = fluidHandler.getResource(0);
        if (res.isEmpty()) return 0;
        Item bucket = res.toStack(1).getFluid().getBucket();
        return bucket == Items.AIR ? 0 : 1;
    }

    @Override
    public ItemResource getResource(int index) {
        if (index != 0) return ItemResource.EMPTY;
        FluidResource res = fluidHandler.getResource(0);
        if (res.isEmpty()) return ItemResource.EMPTY;
        Item bucket = res.toStack(1).getFluid().getBucket();
        if (bucket == Items.AIR) return ItemResource.EMPTY;
        return ItemResource.of(new ItemStack(bucket));
    }

    @Override
    public long getAmountAsLong(int index) {
        if (index != 0) return 0;
        long fluidAmount = fluidHandler.getAmountAsLong(0);
        return fluidAmount / 1000;
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        return Integer.MAX_VALUE; // virtual slot — actual limit is tank capacity
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return false; // insertion via SAT is not supported for fluid tanks
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext tx) {
        return 0; // insertion via SAT is not supported for fluid tanks
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext tx) {
        if (index != 0 || resource.isEmpty() || amount <= 0) return 0;
        FluidResource fluidRes = fluidHandler.getResource(0);
        if (fluidRes.isEmpty()) return 0;
        FluidStack fluidStack = fluidRes.toStack(1);
        Item bucketItem = fluidStack.getFluid().getBucket();
        if (bucketItem == Items.AIR) return 0;
        if (!ItemStack.isSameItemSameComponents(new ItemStack(bucketItem), resource.toStack(1))) return 0;

        long available = fluidHandler.getAmountAsLong(0) / 1000;
        if (available <= 0) return 0;
        int toExtract = (int) Math.min(amount, available);
        int mbToExtract = toExtract * 1000;

        int drained = fluidHandler.extract(0, fluidRes, mbToExtract, tx);
        return drained / 1000;
    }

    // -------------------------------------------------------------------------
    // IKeyCounterContributor
    // -------------------------------------------------------------------------

    @Override
    public void contributeToKeyCounter(KeyCounter kc) {
        FluidResource res = fluidHandler.getResource(0);
        if (res.isEmpty()) return;
        Item bucketItem = res.toStack(1).getFluid().getBucket();
        if (bucketItem == Items.AIR) return;
        kc.add(StorageKey.of(new ItemStack(bucketItem)), fluidHandler.getAmountAsLong(0));
    }
}
