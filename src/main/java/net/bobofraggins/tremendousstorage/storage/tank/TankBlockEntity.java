package net.bobofraggins.tremendousstorage.storage.tank;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.shared.ui.TankSettingsMenu;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.bobofraggins.tremendousstorage.storage.networkinterface.NiLink;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Stores a single fluid type. Capacity starts at {@value #BASE_CAPACITY} mB (16 buckets) at
 * {@link StorageTier#WOOD} and multiplies by 4 with each tier upgrade.
 *
 * <p>The tank is unlocked (storedFluid is EMPTY) when fresh and locks to the first fluid
 * inserted. It stays locked at amount 0 after drain — use Whiteout Tape in the crafting grid
 * to unlock it again.
 *
 * <p>All interaction is via the {@link TankFluidHandler} IFluidHandler capability.
 * The stored fluid type ({@code storedFluid} with amount=1) is held as a type key; the
 * actual quantity is tracked separately as a {@code long} to support large capacities.
 */
public class TankBlockEntity extends BlockEntity implements MenuProvider {

    /** Capacity at {@link StorageTier#WOOD} (16 buckets). Each tier multiplies by 4. */
    public static final long BASE_CAPACITY = 16_000L;

    private final NiLink niLink = new NiLink();

    /**
     * Two-slot container for the fill/drain pane (slot 0 = input, slot 1 = output).
     * Changes mark the BE dirty so the contents survive world save.
     */
    public final SimpleContainer transferContainer = new SimpleContainer(2);

    /** Type key — always has amount=1. EMPTY means unlocked. */
    private FluidStack storedFluid = FluidStack.EMPTY;

    private long amount = 0L;
    private boolean voidExcess = false;
    private StorageTier tier = StorageTier.WOOD;

    public TankBlockEntity(BlockPos pos, BlockState state) {
        this(Registration.TANK_BE_TYPE.get(), pos, state);
    }

    protected TankBlockEntity(
            net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        transferContainer.addListener(c -> super.setChanged());
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public boolean isLocked() {
        return !storedFluid.isEmpty();
    }

    /** Returns the stored fluid type (amount=1), or EMPTY if unlocked. */
    public FluidStack getStoredFluid() {
        return storedFluid;
    }

    public long getAmount() {
        return amount;
    }

    public long getCapacity() {
        return tier.getScaledCapacity(BASE_CAPACITY);
    }

    public StorageTier getTier() {
        return tier;
    }

    public void setTier(StorageTier tier) {
        this.tier = tier;
        setChanged();
    }

    public boolean isVoidExcess() {
        return voidExcess;
    }

    public void setVoidExcess(boolean voidExcess) {
        this.voidExcess = voidExcess;
        setChanged();
    }

    // -------------------------------------------------------------------------
    // Mutation
    // -------------------------------------------------------------------------

    /**
     * Attempts to insert up to {@code requested} mB of the given fluid.
     *
     * @param fluid     the fluid type + components to insert (amount ignored for type check)
     * @param requested how many mB to insert
     * @param simulate  if true, don't modify state
     * @return how many mB were actually inserted
     */
    public long insert(FluidStack fluid, long requested, boolean simulate) {
        if (fluid.isEmpty() || requested <= 0) return 0;

        if (!storedFluid.isEmpty() && !FluidStack.isSameFluidSameComponents(storedFluid, fluid)) {
            return 0; // locked to a different fluid
        }

        long space = getCapacity() - amount;
        long toInsert = Math.min(requested, space);
        if (toInsert <= 0) return 0;

        if (!simulate) {
            if (storedFluid.isEmpty()) {
                storedFluid = fluid.copyWithAmount(1);
            }
            amount += toInsert;
            notifyFluidChanged();
        }
        return toInsert;
    }

    /**
     * Extracts up to {@code requested} mB.
     *
     * @param simulate if true, don't modify state
     * @return the extracted FluidStack (may be smaller than requested), or EMPTY
     */
    public FluidStack extract(long requested, boolean simulate) {
        if (storedFluid.isEmpty() || amount == 0 || requested <= 0) return FluidStack.EMPTY;

        long toExtract = Math.min(requested, Math.min(Integer.MAX_VALUE, amount));
        if (toExtract <= 0) return FluidStack.EMPTY;

        FluidStack result = storedFluid.copyWithAmount((int) toExtract);
        if (!simulate) {
            amount -= toExtract;
            notifyFluidChanged();
        }
        return result;
    }

    /** Clears the stored fluid type. Only valid when amount == 0. */
    public void clearFluid() {
        storedFluid = FluidStack.EMPTY;
        amount = 0;
        notifyFluidChanged();
    }

    // -------------------------------------------------------------------------
    // setChanged / notifyFluidChanged
    // -------------------------------------------------------------------------

    /**
     * Lightweight notification for fluid-content mutations (insert/extract/clear).
     *
     * <p>Saves NBT, notifies the NI that contents changed, and syncs to clients.
     * Does NOT invalidate capabilities or the NI topology cache.
     */
    private void notifyFluidChanged() {
        super.setChanged();
        if (level instanceof ServerLevel sl) {
            niLink.notifyChanged(sl, worldPosition, getBlockState());
        }
    }

    @Override
    public void setChanged() {
        niLink.invalidate();
        super.setChanged();
        if (level != null) {
            level.invalidateCapabilities(worldPosition);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // -------------------------------------------------------------------------
    // Server tick — fluid transfer pane
    // -------------------------------------------------------------------------

    public static void serverTick(
            net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState state,
            TankBlockEntity be) {
        be.tickFluidTransfer();
    }

    private void tickFluidTransfer() {
        ItemStack input = transferContainer.getItem(0);
        if (input.isEmpty()) return;
        if (!transferContainer.getItem(1).isEmpty()) return; // output blocked

        IFluidHandlerItem handler = input.copy().getCapability(Capabilities.FluidHandler.ITEM);
        if (handler == null) return;

        FluidStack contained = handler.getFluidInTank(0);

        if (!contained.isEmpty()) {
            // Item has fluid → insert into tank (must match locked type)
            if (!storedFluid.isEmpty() && !FluidStack.isSameFluidSameComponents(storedFluid, contained)) return;
            int fluidAmt = contained.getAmount();
            long space = getCapacity() - amount;
            // Hold until tank can accept the full amount (unless it voids excess)
            if (space < fluidAmt && !voidExcess) return;
            handler.drain(fluidAmt, IFluidHandler.FluidAction.EXECUTE);
            insert(contained, fluidAmt, false);
            transferContainer.setItem(0, ItemStack.EMPTY);
            transferContainer.setItem(1, handler.getContainer());
        } else {
            // Item is empty → fill from tank
            if (storedFluid.isEmpty() || amount == 0) return;
            int tryFill = (int) Math.min(amount, Integer.MAX_VALUE);
            int canFill = handler.fill(storedFluid.copyWithAmount(tryFill), IFluidHandler.FluidAction.SIMULATE);
            if (canFill <= 0) return; // item doesn't accept this fluid type
            // Hold until tank has enough to fill the container completely
            if (amount < canFill) return;
            extract(canFill, false);
            handler.fill(storedFluid.copyWithAmount(canFill), IFluidHandler.FluidAction.EXECUTE);
            transferContainer.setItem(0, ItemStack.EMPTY);
            transferContainer.setItem(1, handler.getContainer());
        }
    }

    // -------------------------------------------------------------------------
    // MenuProvider
    // -------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.tremendousstorage.tank");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        ContainerData data = new ContainerData() {
            @Override
            public int get(int index) {
                return index == 0 ? (voidExcess ? 1 : 0) : 0;
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return 1;
            }
        };
        return new TankSettingsMenu(id, inv, worldPosition, data, transferContainer);
    }

    // -------------------------------------------------------------------------
    // NBT
    // -------------------------------------------------------------------------

    private static final String TAG_FLUID = "StoredFluid";
    private static final String TAG_AMOUNT = "Amount";
    private static final String TAG_VOID_EXCESS = "VoidExcess";
    private static final String TAG_TIER = "Tier";
    private static final String TAG_TRANSFER = "Transfer";

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!storedFluid.isEmpty()) {
            tag.put(TAG_FLUID, storedFluid.save(registries));
        }
        tag.putLong(TAG_AMOUNT, amount);
        tag.putBoolean(TAG_VOID_EXCESS, voidExcess);
        tag.putString(TAG_TIER, tier.getId());
        net.minecraft.nbt.CompoundTag transferTag = new net.minecraft.nbt.CompoundTag();
        ItemStack in = transferContainer.getItem(0);
        ItemStack out = transferContainer.getItem(1);
        if (!in.isEmpty()) transferTag.put("Input", in.save(registries));
        if (!out.isEmpty()) transferTag.put("Output", out.save(registries));
        tag.put(TAG_TRANSFER, transferTag);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        voidExcess = tag.getBoolean(TAG_VOID_EXCESS);
        tier = StorageTier.fromId(tag.getString(TAG_TIER));
        if (tag.contains(TAG_FLUID)) {
            storedFluid = FluidStack.parseOptional(registries, tag.getCompound(TAG_FLUID));
            if (!storedFluid.isEmpty()) {
                storedFluid = storedFluid.copyWithAmount(1);
            }
        } else {
            storedFluid = FluidStack.EMPTY;
        }
        amount = tag.getLong(TAG_AMOUNT);
        if (tag.contains(TAG_TRANSFER)) {
            net.minecraft.nbt.CompoundTag t = tag.getCompound(TAG_TRANSFER);
            transferContainer.setItem(0, t.contains("Input")
                    ? ItemStack.parseOptional(registries, t.getCompound("Input")) : ItemStack.EMPTY);
            transferContainer.setItem(1, t.contains("Output")
                    ? ItemStack.parseOptional(registries, t.getCompound("Output")) : ItemStack.EMPTY);
        }
    }

    // -------------------------------------------------------------------------
    // Item component sync
    // -------------------------------------------------------------------------

    /**
     * Populates the {@link Registration#TANK_CONTENTS} component on the dropped/saved item
     * so the fluid state is preserved without duplicating it in {@code block_entity_data}.
     */
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(Registration.TANK_CONTENTS.get(), new TankContents(storedFluid, amount));
    }

    /**
     * Restores fluid state from {@link Registration#TANK_CONTENTS} when the block item is
     * placed back in the world. Falls back gracefully for old items that only have
     * {@code block_entity_data} (fluid is then loaded by {@link #loadAdditional}).
     */
    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        TankContents contents = input.get(Registration.TANK_CONTENTS);
        if (contents != null) {
            storedFluid = contents.storedFluid().isEmpty()
                    ? net.neoforged.neoforge.fluids.FluidStack.EMPTY
                    : contents.storedFluid().copyWithAmount(1);
            amount = contents.amount();
        }
    }

    /** Removes fluid fields from the NBT tag since they are stored in the component instead. */
    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove(TAG_FLUID);
        tag.remove(TAG_AMOUNT);
    }

    // -------------------------------------------------------------------------
    // Client sync
    // -------------------------------------------------------------------------

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
