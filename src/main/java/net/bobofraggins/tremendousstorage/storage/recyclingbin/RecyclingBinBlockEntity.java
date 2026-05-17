package net.bobofraggins.tremendousstorage.storage.recyclingbin;

import net.bobofraggins.tremendousstorage.shared.register.BETypeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

public class RecyclingBinBlockEntity extends BlockEntity implements MenuProvider {

    public static final int FLUID_CAPACITY_MB = 10_000;
    private static final int MB_PER_ITEM = 10;

    private int vibesAmount = 0;

    /**
     * Two-slot container for the fill/drain pane (slot 0 = input, slot 1 = output).
     * Changes mark the BE dirty so the contents survive world save.
     */
    public final SimpleContainer transferContainer = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            RecyclingBinBlockEntity.this.setChanged();
        }
    };

    // -------------------------------------------------------------------------
    // Fluid storage
    // -------------------------------------------------------------------------

    /** Adds up to {@code mB} millibuckets of Positive Vibes, capped at capacity. */
    public void addVibes(int mB) {
        vibesAmount = Math.min(FLUID_CAPACITY_MB, vibesAmount + mB);
        setChanged();
    }

    public int getVibesAmount() {
        return vibesAmount;
    }

    /**
     * Extracts up to {@code maxDrain} mB. Returns the amount actually drained.
     * Pass {@code simulate = true} to preview without modifying state.
     */
    public int extractVibes(int maxDrain, boolean simulate) {
        int drained = Math.min(vibesAmount, maxDrain);
        if (!simulate && drained > 0) {
            vibesAmount -= drained;
            setChanged();
        }
        return drained;
    }

    /**
     * Called when an item is destroyed (from either the menu slot or the item handler).
     * Adds {@value #MB_PER_ITEM} mB per item in the stack; silently does nothing if full.
     */
    public void onItemsDestroyed(int count) {
        addVibes(count * MB_PER_ITEM);
    }

    // -------------------------------------------------------------------------
    // Client sync
    // -------------------------------------------------------------------------

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // -------------------------------------------------------------------------
    // NBT persistence
    // -------------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Vibes", vibesAmount);
        ValueOutput xfer = output.child("Transfer");
        ItemStack in = transferContainer.getItem(0);
        ItemStack out = transferContainer.getItem(1);
        if (!in.isEmpty()) xfer.store("Input", ItemStack.OPTIONAL_CODEC, in);
        if (!out.isEmpty()) xfer.store("Output", ItemStack.OPTIONAL_CODEC, out);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        vibesAmount = input.getIntOr("Vibes", 0);
        ValueInput xfer = input.childOrEmpty("Transfer");
        transferContainer.setItem(
                0, xfer.read("Input", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        transferContainer.setItem(
                1, xfer.read("Output", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
    }

    // -------------------------------------------------------------------------
    // Item component sync (break → item → place)
    // -------------------------------------------------------------------------

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putInt("Vibes", vibesAmount);
        components.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(getType(), tag));
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter input) {
        super.applyImplicitComponents(input);
        var data = input.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data != null) {
            net.minecraft.nbt.CompoundTag tag = data.copyTagWithoutId();
            if (tag.contains("Vibes")) vibesAmount = tag.getIntOr("Vibes", 0);
        }
    }

    // -------------------------------------------------------------------------
    // Lid animation state (client-side)
    // -------------------------------------------------------------------------

    public int openCount = 0;
    public float prevLidAngle = 0f;
    public float lidAngle = 0f;

    // -------------------------------------------------------------------------
    // ContainerOpenersCounter
    // -------------------------------------------------------------------------

    private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState state) {
            level.playSound(
                    null,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    SoundEvents.CHEST_OPEN,
                    SoundSource.BLOCKS,
                    0.5f,
                    level.getRandom().nextFloat() * 0.1f + 0.9f);
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) {
            level.playSound(
                    null,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    SoundEvents.CHEST_CLOSE,
                    SoundSource.BLOCKS,
                    0.5f,
                    level.getRandom().nextFloat() * 0.1f + 0.9f);
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int oldCount, int newCount) {
            level.blockEvent(pos, state.getBlock(), 1, newCount);
        }

        @Override
        public boolean isOwnContainer(Player player) {
            return player.containerMenu instanceof RecyclingBinMenu m
                    && m.getPos().equals(worldPosition);
        }
    };

    public void startOpen(Player player) {
        if (!isRemoved() && !player.isSpectator()) {
            openersCounter.incrementOpeners(player, getLevel(), getBlockPos(), getBlockState(), 64.0);
        }
    }

    public void stopOpen(Player player) {
        if (!isRemoved() && !player.isSpectator()) {
            openersCounter.decrementOpeners(player, getLevel(), getBlockPos(), getBlockState());
        }
    }

    public void recheckOpeners(Level level, BlockPos pos, BlockState state) {
        openersCounter.recheckOpeners(level, pos, state);
    }

    // -------------------------------------------------------------------------
    // Block event (server → client open-count sync)
    // -------------------------------------------------------------------------

    @Override
    public boolean triggerEvent(int id, int type) {
        if (id == 1) {
            openCount = type;
            return true;
        }
        return super.triggerEvent(id, type);
    }

    // -------------------------------------------------------------------------
    // Tickers
    // -------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, RecyclingBinBlockEntity be) {
        be.openersCounter.recheckOpeners(level, pos, state);
        be.tickFluidTransfer();
    }

    private void tickFluidTransfer() {
        ItemStack input = transferContainer.getItem(0);
        if (input.isEmpty()) return;
        if (!transferContainer.getItem(1).isEmpty()) return; // output slot occupied

        ItemStack inputCopy = input.copy();
        var rawCap = inputCopy.getCapability(Capabilities.Fluid.ITEM, ItemAccess.forStack(inputCopy));
        if (!(rawCap instanceof net.bobofraggins.tremendousstorage.storage.tank.TankItemFluidHandler cap)) return;

        FluidResource containedRes = cap.getResource(0);
        long containedAmt = cap.getAmountAsLong(0);

        if (!containedRes.isEmpty() && containedAmt > 0) {
            // Item has fluid → insert Positive Vibes into the recycling bin
            FluidStack contained = containedRes.toStack(1);
            if (contained.getFluid() != BETypeHelper.getFluid("positive_vibes")) return; // wrong fluid
            int amt = (int) Math.min(containedAmt, Integer.MAX_VALUE);
            int space = FLUID_CAPACITY_MB - vibesAmount;
            if (space < amt) return; // hold until there is room for the full amount
            cap.extract(0, containedRes, amt, null);
            addVibes(amt);
            ItemStack modified = cap.getContainer();
            transferContainer.setItem(0, ItemStack.EMPTY);
            transferContainer.setItem(1, modified != null ? modified : ItemStack.EMPTY);
        } else {
            // Item is empty → fill it with Positive Vibes from the recycling bin
            if (vibesAmount == 0) return;
            FluidResource vibesResource = FluidResource.of(BETypeHelper.getFluid("positive_vibes"));
            if (!cap.isValid(0, vibesResource)) return; // item doesn't accept Positive Vibes
            long capacity = cap.getCapacityAsLong(0, vibesResource);
            int canFill = (int) Math.min(vibesAmount, capacity);
            if (canFill <= 0) return;
            // Hold until the bin has enough to fill the container completely
            if (vibesAmount < canFill) return;
            extractVibes(canFill, false);
            cap.insert(0, vibesResource, canFill, null);
            ItemStack modified = cap.getContainer();
            transferContainer.setItem(0, ItemStack.EMPTY);
            transferContainer.setItem(1, modified != null ? modified : ItemStack.EMPTY);
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, RecyclingBinBlockEntity be) {
        be.prevLidAngle = be.lidAngle;
        if (be.openCount > 0 && be.lidAngle < 1f) {
            be.lidAngle = Math.min(1f, be.lidAngle + 0.1f);
        } else if (be.openCount == 0 && be.lidAngle > 0f) {
            be.lidAngle = Math.max(0f, be.lidAngle - 0.1f);
        }
    }

    // -------------------------------------------------------------------------
    // Constructor + MenuProvider
    // -------------------------------------------------------------------------

    public RecyclingBinBlockEntity(BlockPos pos, BlockState state) {
        super(BETypeHelper.get("recycling_bin"), pos, state);
        // setChanged() hook wired via anonymous SimpleContainer subclass above
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.tremendousstorage.recycling_bin");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new RecyclingBinMenu(id, inv, worldPosition, this, transferContainer);
    }
}
