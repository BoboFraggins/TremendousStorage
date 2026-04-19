package net.bobofraggins.tremendousstorage.storage.filingcabinet;

import java.util.Optional;
import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.shared.priority.Priority;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.util.PullerUtil;
import net.bobofraggins.tremendousstorage.storage.accessterminal.AccessTerminalBFS;
import net.bobofraggins.tremendousstorage.storage.manillafolder.FolderContents;
import net.bobofraggins.tremendousstorage.storage.manillafolder.ManillaFolderItem;
import net.bobofraggins.tremendousstorage.storage.networkinterface.NetworkListable;
import net.bobofraggins.tremendousstorage.storage.networkinterface.NiCacheHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;

/** Stores up to {@value #SLOT_COUNT} Manila Folder stacks. */
public class FilingCabinetBlockEntity extends BlockEntity
        implements net.minecraft.world.Container, MenuProvider, NiCacheHolder, NetworkListable {

    public static final int SLOT_COUNT = 8;

    private final NonNullList<ItemStack> folders = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private Priority priority = Priority.HIGH;
    private boolean voidExcess = false;
    private boolean hasMagnetUpgrade = false;
    private boolean hasPullerUpgrade = false;
    private int pullerSides = 0;
    private int pullerTickCounter = 0;

    private static final int PULL_TICKS = 4;
    private static final int PULL_AMOUNT = 4;

    @Nullable
    private BlockPos cachedNiPos = null;

    // -------------------------------------------------------------------------
    // Drawer animation state (client-side only)
    // -------------------------------------------------------------------------

    /** Number of players with this block's menu open (synced from server via block event). */
    public int openCount = 0;

    /** Drawer open fraction last tick (0 = closed, 1 = fully open). */
    public float prevDrawerOffset = 0f;

    /** Drawer open fraction this tick. */
    public float drawerOffset = 0f;

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
                    SoundEvents.BARREL_OPEN,
                    SoundSource.BLOCKS,
                    0.5f,
                    level.random.nextFloat() * 0.1f + 0.9f);
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) {
            level.playSound(
                    null,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    SoundEvents.BARREL_CLOSE,
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
            return player.containerMenu instanceof FilingCabinetMenu m
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, FilingCabinetBlockEntity be) {
        be.openersCounter.recheckOpeners(level, pos, state);
        if (be.hasPullerUpgrade && be.pullerSides != 0 && ++be.pullerTickCounter >= PULL_TICKS) {
            be.pullerTickCounter = 0;
            be.tickPuller(level, pos, state);
        }
        if (be.hasMagnetUpgrade) {
            AABB searchArea = new AABB(pos).inflate(3);
            for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, searchArea)) {
                if (entity.isRemoved()) continue;
                ItemStack stack = entity.getItem();
                if (stack.isEmpty()) continue;
                ItemStack leftover = be.magnetAbsorb(stack);
                if (leftover.isEmpty()) {
                    entity.discard();
                } else if (leftover.getCount() < stack.getCount()) {
                    entity.setItem(leftover);
                }
            }
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, FilingCabinetBlockEntity be) {
        be.prevDrawerOffset = be.drawerOffset;
        if (be.openCount > 0 && be.drawerOffset < 1f) {
            be.drawerOffset = Math.min(1f, be.drawerOffset + 0.1f);
        } else if (be.openCount == 0 && be.drawerOffset > 0f) {
            be.drawerOffset = Math.max(0f, be.drawerOffset - 0.1f);
        }
    }

    public FilingCabinetBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.FILING_CABINET_BE_TYPE.get(), pos, state);
    }

    // -------------------------------------------------------------------------
    // Capability invalidation
    // -------------------------------------------------------------------------

    @Override
    public void invalidateNiCache() {
        cachedNiPos = null;
    }

    @Override
    @Nullable
    public BlockPos getOrFindNiPos(ServerLevel level) {
        if (cachedNiPos != null
                && !(level.getBlockEntity(cachedNiPos)
                        instanceof
                        net.bobofraggins.tremendousstorage.storage.networkinterface.NetworkInterfaceBlockEntity)) {
            cachedNiPos = null;
        }
        if (cachedNiPos == null) cachedNiPos = AccessTerminalBFS.findNI(level, worldPosition);
        return cachedNiPos;
    }

    @Override
    public void setChanged() {
        invalidateNiCache();
        super.setChanged();
        if (level != null) {
            level.invalidateCapabilities(worldPosition);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * Updates a folder slot's stack and notifies the NI of the content change, without
     * triggering a full topology invalidation.
     *
     * <p>Called by {@link FilingCabinetItemHandler} after inserting into or extracting from a
     * folder's embedded inventory. Placing or removing a folder itself uses
     * {@link #setFolder(int, ItemStack)} which calls the full {@link #setChanged()}.
     */
    void notifyFolderContentsChanged(int slot, ItemStack updated) {
        folders.set(slot, updated);
        super.setChanged();
        if (level instanceof ServerLevel sl) {
            notifyNiContentsChanged(sl);
        }
    }

    // -------------------------------------------------------------------------
    // Container interface
    // -------------------------------------------------------------------------

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        return folders.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return folders.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(folders, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(folders, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        folders.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return net.minecraft.world.Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        folders.replaceAll(s -> ItemStack.EMPTY);
        setChanged();
    }

    // -------------------------------------------------------------------------
    // Custom accessors
    // -------------------------------------------------------------------------

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority p) {
        this.priority = p;
        setChanged();
    }

    public boolean isVoidExcess() {
        return voidExcess;
    }

    public void setVoidExcess(boolean voidExcess) {
        this.voidExcess = voidExcess;
        setChanged();
    }

    public ItemStack getFolder(int slot) {
        return folders.get(slot);
    }

    public void setFolder(int slot, ItemStack stack) {
        folders.set(slot, stack);
        setChanged();
    }

    public boolean hasMagnetUpgrade() {
        return hasMagnetUpgrade;
    }

    public void setMagnetUpgrade(boolean value) {
        hasMagnetUpgrade = value;
        setChanged();
    }

    public boolean hasPullerUpgrade() {
        return hasPullerUpgrade;
    }

    public void setPullerUpgrade(boolean value) {
        hasPullerUpgrade = value;
        setChanged();
    }

    public int getPullerSides() {
        return pullerSides;
    }

    public void setPullerSides(int mask) {
        pullerSides = mask;
        setChanged();
    }

    // -------------------------------------------------------------------------
    // Puller logic
    // -------------------------------------------------------------------------

    private void tickPuller(Level level, BlockPos pos, BlockState state) {
        PullerUtil.tickPuller(level, pos, state, pullerSides, this::pullFromHandler);
    }

    private void pullFromHandler(IItemHandler handler) {
        for (int s = 0; s < handler.getSlots(); s++) {
            ItemStack simulated = handler.extractItem(s, PULL_AMOUNT, true);
            if (simulated.isEmpty()) continue;
            ItemStack remainder = pullerAbsorb(simulated, PULL_AMOUNT, true);
            int canInsert = simulated.getCount() - remainder.getCount();
            if (canInsert <= 0) continue;
            ItemStack extracted = handler.extractItem(s, canInsert, false);
            if (!extracted.isEmpty()) pullerAbsorb(extracted, canInsert, false);
            break;
        }
    }

    /**
     * Like {@link #magnetAbsorb} but also accepts items into unlocked (empty) folders,
     * locking them to that item type on first use.
     *
     * @param simulate if true, no state is mutated
     */
    public ItemStack pullerAbsorb(ItemStack incoming, int maxAmount, boolean simulate) {
        long remaining = Math.min(incoming.getCount(), maxAmount);
        for (int slot = 0; slot < SLOT_COUNT && remaining > 0; slot++) {
            ItemStack folder = folders.get(slot);
            if (folder.isEmpty() || !(folder.getItem() instanceof ManillaFolderItem)) continue;
            FolderContents contents = ManillaFolderItem.getContents(folder);

            if (contents.isEmpty()) {
                // Lock the folder to this item type and insert
                FolderContents locked = new FolderContents(Optional.of(incoming.copyWithCount(1)), 0L, contents.tier());
                FolderContents.InsertResult result = locked.insert(remaining, locked.getCapacity());
                long absorbed = remaining - result.remainder();
                if (absorbed > 0 && !simulate) {
                    notifyFolderContentsChanged(
                            slot, ManillaFolderItem.setContents(folder.copyWithCount(1), result.updated()));
                }
                remaining = result.remainder();
            } else if (contents.accepts(incoming)) {
                long capacity = ManillaFolderItem.getCapacity(folder);
                FolderContents.InsertResult result = contents.insert(remaining, capacity);
                long absorbed = remaining - result.remainder();
                if (absorbed > 0 && !simulate) {
                    notifyFolderContentsChanged(
                            slot, ManillaFolderItem.setContents(folder.copyWithCount(1), result.updated()));
                }
                remaining = result.remainder();
            }
        }
        return remaining == 0 ? ItemStack.EMPTY : incoming.copyWithCount((int) remaining);
    }

    /**
     * Attempts to absorb as many items from {@code incoming} as possible into folders that are
     * already locked to that item type. Returns the leftover stack (empty if fully absorbed).
     */
    public ItemStack magnetAbsorb(ItemStack incoming) {
        long remaining = incoming.getCount();
        for (int slot = 0; slot < SLOT_COUNT && remaining > 0; slot++) {
            ItemStack folder = folders.get(slot);
            if (folder.isEmpty() || !(folder.getItem() instanceof ManillaFolderItem)) continue;
            FolderContents contents = ManillaFolderItem.getContents(folder);
            if (contents.isEmpty()) continue; // unlocked — not "already in inventory"
            if (!contents.accepts(incoming)) continue;
            long capacity = ManillaFolderItem.getCapacity(folder);
            FolderContents.InsertResult result = contents.insert(remaining, capacity);
            long absorbed = remaining - result.remainder();
            if (absorbed > 0) {
                notifyFolderContentsChanged(
                        slot, ManillaFolderItem.setContents(folder.copyWithCount(1), result.updated()));
                remaining = result.remainder();
            }
        }
        if (voidExcess) return ItemStack.EMPTY;
        return remaining == 0 ? ItemStack.EMPTY : incoming.copyWithCount((int) remaining);
    }

    // -------------------------------------------------------------------------
    // MenuProvider
    // -------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.tremendousstorage.filing_cabinet");
    }

    @Override
    public String getNetworkName() {
        StringBuilder sb = new StringBuilder();
        if (hasMagnetUpgrade) sb.append("Magnet");
        if (hasPullerUpgrade) {
            if (!sb.isEmpty()) sb.append('/');
            sb.append("Puller");
        }
        String base =
                Component.translatable("block.tremendousstorage.filing_cabinet").getString();
        return sb.isEmpty() ? base : base + " (" + sb + ")";
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new FilingCabinetMenu(id, inv, worldPosition, this);
    }

    // -------------------------------------------------------------------------
    // NBT serialization
    // -------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, folders, registries);
        tag.putInt("Priority", priority.ordinal());
        tag.putBoolean("VoidExcess", voidExcess);
        if (hasMagnetUpgrade) tag.putBoolean("MagnetUpgrade", true);
        if (hasPullerUpgrade) {
            tag.putBoolean("PullerUpgrade", true);
            tag.putInt("PullerSides", pullerSides);
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, folders, registries);
        priority = Priority.fromOrdinal(tag.getInt("Priority"));
        voidExcess = tag.getBoolean("VoidExcess");
        hasMagnetUpgrade = tag.getBoolean("MagnetUpgrade");
        hasPullerUpgrade = tag.getBoolean("PullerUpgrade");
        pullerSides = tag.getInt("PullerSides");
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
