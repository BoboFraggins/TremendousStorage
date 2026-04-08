package net.bobofraggins.tremendousstorage.storage.recyclingbin;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;

public class RecyclingBinBlockEntity extends BlockEntity implements MenuProvider {

    public static final int FLUID_CAPACITY_MB = 10_000;
    private static final int MB_PER_ITEM = 10;

    private int vibesAmount = 0;

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
    // NBT persistence
    // -------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Vibes", vibesAmount);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        vibesAmount = tag.getInt("Vibes");
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
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.CHEST_OPEN,
                    SoundSource.BLOCKS,
                    0.5f,
                    level.random.nextFloat() * 0.1f + 0.9f);
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) {
            level.playSound(
                    null,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.CHEST_CLOSE,
                    SoundSource.BLOCKS,
                    0.5f,
                    level.random.nextFloat() * 0.1f + 0.9f);
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int oldCount, int newCount) {
            level.blockEvent(pos, state.getBlock(), 1, newCount);
        }

        @Override
        protected boolean isOwnContainer(Player player) {
            return player.containerMenu instanceof RecyclingBinMenu m
                    && m.getPos().equals(worldPosition);
        }
    };

    public void startOpen(Player player) {
        if (!isRemoved() && !player.isSpectator()) {
            openersCounter.incrementOpeners(player, getLevel(), getBlockPos(), getBlockState());
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
        super(Registration.RECYCLING_BIN_BE_TYPE.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.tremendousstorage.recycling_bin");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new RecyclingBinMenu(id, inv, worldPosition, this);
    }
}
