package net.bobofraggins.tremendousstorage.storage.chest;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.shared.config.SortMode;
import net.bobofraggins.tremendousstorage.shared.priority.Priority;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.KeyCounter;
import net.bobofraggins.tremendousstorage.shared.storage.StorageKey;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.storage.networkinterface.NiCacheHolder;
import net.bobofraggins.tremendousstorage.storage.networkinterface.NiLink;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Stores items in a shared pool across any number of distinct types.
 *
 * <p>Accepts any item (including unstackable items, damageable items, and items with custom
 * component data). The only limit is the total item count across all types, which is determined by
 * the block's {@link StorageTier} (starts at Wood = 4,096; upgradeable to Netherite = 4,194,304).
 *
 * <p>Tier is persisted in NBT and retained when the block is broken and replaced.
 *
 * <p>No player-facing UI — all interaction is via the {@link ChestItemHandler}
 * {@code IItemHandler} capability.
 */
public class ChestBlockEntity extends BlockEntity implements MenuProvider, NiCacheHolder {

    // O(1) lookup by key; orderedKeys provides stable index-based access
    private final Object2LongOpenHashMap<StorageKey> items = new Object2LongOpenHashMap<>();
    private final List<StorageKey> orderedKeys = new ArrayList<>();
    private long cachedTotalCount = 0;
    private Priority priority = Priority.LOW;
    private SortMode sortMode = SortMode.AMOUNT;
    private StorageTier tier = StorageTier.WOOD;
    private boolean hasCraftingUpgrade = false;
    private boolean hasMagnetUpgrade = false;
    private boolean hasPullerUpgrade = false;
    private int pullerSides = 0;
    private int pullerTickCounter = 0;

    private static final int PULL_TICKS  = 4;
    private static final int PULL_AMOUNT = 4;

    public long getCapacity() {
        return tier.getCapacity();
    }

    private final NiLink niLink = new NiLink();

    // -------------------------------------------------------------------------
    // Lid animation state (client-side only)
    // -------------------------------------------------------------------------

    /** Number of players with this block's menu open (synced from server via block event). */
    public int openCount = 0;

    /** Animation progress last tick (0 = closed, 1 = fully open). */
    public float prevLidAngle = 0f;

    /** Animation progress this tick. */
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
                    level.random.nextFloat() * 0.1f + 0.9f);
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
                    level.random.nextFloat() * 0.1f + 0.9f);
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int oldCount, int newCount) {
            level.blockEvent(pos, state.getBlock(), 1, newCount);
        }

        @Override
        protected boolean isOwnContainer(Player player) {
            return player.containerMenu instanceof ChestMenu m
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

    /** Called from {@link ChestBlock#onRemove} to force-close all openers. */
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChestBlockEntity be) {
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
                StorageKey key = StorageKey.of(stack);
                if (!be.contains(key)) continue;
                long remainder = be.insert(stack, stack.getCount(), false);
                if (remainder <= 0) {
                    entity.discard();
                } else {
                    entity.setItem(stack.copyWithCount((int) remainder));
                }
            }
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, ChestBlockEntity be) {
        be.prevLidAngle = be.lidAngle;
        if (be.openCount > 0 && be.lidAngle < 1f) {
            be.lidAngle = Math.min(1f, be.lidAngle + 0.1f);
        } else if (be.openCount == 0 && be.lidAngle > 0f) {
            be.lidAngle = Math.max(0f, be.lidAngle - 0.1f);
        }
    }

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public ChestBlockEntity(BlockPos pos, BlockState state) {
        this(Registration.TREMENDOUS_CHEST_BE_TYPE.get(), pos, state);
    }

    protected ChestBlockEntity(
            net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // -------------------------------------------------------------------------
    // Item filter
    // -------------------------------------------------------------------------

    /**
     * Returns true if the stack qualifies for storage in the Tremendous Chest.
     *
     * <p>Accepts any non-empty item regardless of damage, stack size, or component data.
     */
    public static boolean accepts(ItemStack stack) {
        return !stack.isEmpty();
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /** Total number of items stored across all types. */
    public long totalCount() {
        return cachedTotalCount;
    }

    public boolean isEmpty() {
        return orderedKeys.isEmpty();
    }

    public boolean isFull() {
        return cachedTotalCount >= getCapacity();
    }

    public StorageTier getTier() {
        return tier;
    }

    public void setTier(StorageTier tier) {
        this.tier = tier;
        setChanged();
    }

    /** Number of distinct item types currently stored. */
    public int typeCount() {
        return orderedKeys.size();
    }

    /** Returns the type key at {@code index} (count == 1), or {@link ItemStack#EMPTY}. */
    public ItemStack getType(int index) {
        if (index < 0 || index >= orderedKeys.size()) return ItemStack.EMPTY;
        return orderedKeys.get(index).toDisplayStack();
    }

    /** Returns the stored count for the type at {@code index}, or 0. */
    public long getCount(int index) {
        if (index < 0 || index >= orderedKeys.size()) return 0;
        return items.getLong(orderedKeys.get(index));
    }

    // -------------------------------------------------------------------------
    // Insert / extract
    // -------------------------------------------------------------------------

    /**
     * Inserts up to {@code amount} items of the given type.
     *
     * @return number of items that could NOT be inserted (the remainder).
     */
    public long insert(ItemStack type, long amount, boolean simulate) {
        if (amount <= 0 || !accepts(type)) return amount;

        long space = getCapacity() - cachedTotalCount;
        if (space <= 0) return amount;

        long toInsert = Math.min(amount, space);

        if (!simulate) {
            StorageKey key = StorageKey.of(type);
            if (items.containsKey(key)) {
                items.addTo(key, toInsert);
            } else {
                items.put(key, toInsert);
                orderedKeys.add(key);
            }
            cachedTotalCount += toInsert;
            notifyItemsChanged();
        }

        return amount - toInsert;
    }

    /**
     * Extracts up to {@code amount} items of the given type from the entry at {@code index}.
     *
     * @return the extracted stack (may be smaller than requested), or EMPTY if nothing available.
     */
    public ItemStack extract(int index, long amount, boolean simulate) {
        if (index < 0 || index >= orderedKeys.size()) return ItemStack.EMPTY;

        StorageKey key = orderedKeys.get(index);
        long stored = items.getLong(key);
        if (stored == 0) return ItemStack.EMPTY;

        ItemStack typeStack = key.toDisplayStack();
        long toExtract = Math.min(amount, Math.min(typeStack.getMaxStackSize(), stored));
        if (toExtract <= 0) return ItemStack.EMPTY;

        if (!simulate) {
            long remaining = stored - toExtract;
            if (remaining == 0) {
                // Swap-with-last for O(1) removal — order is not contractually guaranteed
                int lastIdx = orderedKeys.size() - 1;
                StorageKey last = orderedKeys.remove(lastIdx);
                if (index != lastIdx) {
                    orderedKeys.set(index, last);
                }
                items.removeLong(key);
            } else {
                items.put(key, remaining);
            }
            cachedTotalCount -= toExtract;
            notifyItemsChanged();
        }

        return typeStack.copyWithCount((int) toExtract);
    }

    // -------------------------------------------------------------------------
    // KeyCounter contribution
    // -------------------------------------------------------------------------

    /** Adds all stored items with their actual counts to the given KeyCounter. */
    public void populateKeyCounter(KeyCounter kc) {
        for (StorageKey key : orderedKeys) {
            long count = items.getLong(key);
            if (count > 0) kc.add(key, count);
        }
    }

    /** Returns true if this container already holds items of the given key. */
    public boolean contains(StorageKey key) {
        return items.containsKey(key);
    }

    // -------------------------------------------------------------------------
    // Priority
    // -------------------------------------------------------------------------

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority p) {
        this.priority = p;
        setChanged();
    }

    public SortMode getSortMode() {
        return sortMode;
    }

    public void setSortMode(SortMode mode) {
        this.sortMode = mode;
        setChanged();
    }

    // -------------------------------------------------------------------------
    // MenuProvider
    // -------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        Component base = Component.translatable("block.tremendousstorage.chest");
        return base.copy().append(Component.literal(buildSuffix(false)));
    }

    /** Builds the " (Tier/Ender/Magnetized)" suffix string. Pass {@code ender=true} for ender variants. */
    protected String buildSuffix(boolean ender) {
        StringBuilder sb = new StringBuilder();
        if (tier != StorageTier.WOOD) {
            sb.append(Character.toUpperCase(tier.getId().charAt(0))).append(tier.getId().substring(1));
        }
        if (ender) {
            if (!sb.isEmpty()) sb.append('/');
            sb.append("Ender");
        }
        if (hasMagnetUpgrade) {
            if (!sb.isEmpty()) sb.append('/');
            sb.append("Magnetized");
        }
        return sb.isEmpty() ? "" : " (" + sb + ")";
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        ContainerData data = new ContainerData() {
            @Override
            public int get(int index) {
                return index == 0 ? priority.ordinal() : 0;
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return 1;
            }
        };
        return new ChestMenu(id, inv, worldPosition, data, hasCraftingUpgrade, hasPullerUpgrade);
    }

    public boolean hasCraftingUpgrade() {
        return hasCraftingUpgrade;
    }

    public void setCraftingUpgrade(boolean value) {
        hasCraftingUpgrade = value;
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
        Direction facing = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;
        for (int bit = 0; bit < 6; bit++) {
            if ((pullerSides & (1 << bit)) == 0) continue;
            Direction worldDir = bitToWorldDir(bit, facing);
            BlockPos adjacentPos = pos.relative(worldDir);
            IItemHandler cap = level.getCapability(
                    Capabilities.ItemHandler.BLOCK, adjacentPos, worldDir.getOpposite());
            if (cap == null) continue;
            pullFromHandler(cap);
        }
    }

    private void pullFromHandler(IItemHandler handler) {
        for (int s = 0; s < handler.getSlots(); s++) {
            ItemStack simulated = handler.extractItem(s, PULL_AMOUNT, true);
            if (simulated.isEmpty()) continue;
            long remainder = insert(simulated, simulated.getCount(), true);
            int canInsert = (int) (simulated.getCount() - remainder);
            if (canInsert <= 0) continue;
            ItemStack extracted = handler.extractItem(s, canInsert, false);
            if (!extracted.isEmpty()) insert(extracted, extracted.getCount(), false);
            break;
        }
    }

    private static Direction bitToWorldDir(int bit, Direction facing) {
        return switch (bit) {
            case 0  -> Direction.UP;
            case 1  -> Direction.DOWN;
            case 2  -> facing.getCounterClockWise();
            case 3  -> facing.getClockWise();
            case 4  -> facing;
            default -> facing.getOpposite();
        };
    }

    // -------------------------------------------------------------------------
    // NiCacheHolder + setChanged
    // -------------------------------------------------------------------------

    /**
     * Lightweight notification for item-content mutations (insert/extract).
     *
     * <p>Persists NBT and tells the NI the contents changed, but does NOT invalidate the
     * topology cache or fire {@code level.invalidateCapabilities} — the handler identity is
     * unchanged and no BFS re-scan is needed.
     */
    protected void notifyItemsChanged() {
        super.setChanged();
        if (level instanceof ServerLevel sl) {
            niLink.notifyChanged(sl, worldPosition, getBlockState());
        }
    }

    // -------------------------------------------------------------------------
    // Protected inventory helpers (used by ender subclasses to sync with storage)
    // -------------------------------------------------------------------------

    /**
     * Serialises only the item inventory (the "Types" list) to a tag, without tier/priority/etc.
     * Used by ender subclasses to persist inventory to shared storage.
     */
    protected ListTag saveTypes(HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (StorageKey key : orderedKeys) {
            CompoundTag entry = new CompoundTag();
            entry.put(TAG_TYPE, key.toDisplayStack().save(registries));
            entry.putLong(TAG_COUNT, items.getLong(key));
            list.add(entry);
        }
        return list;
    }

    /**
     * Clears the current inventory and deserialises it from a "Types" list tag.
     * Used by ender subclasses to restore inventory from shared storage.
     */
    protected void loadTypes(ListTag list, HolderLookup.Provider registries) {
        items.clear();
        orderedKeys.clear();
        cachedTotalCount = 0;
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            long count = entry.getLong(TAG_COUNT);
            ItemStack.parse(registries, entry.getCompound(TAG_TYPE)).ifPresent(stack -> {
                StorageKey key = StorageKey.of(stack);
                items.put(key, count);
                orderedKeys.add(key);
                cachedTotalCount += count;
            });
        }
    }

    @Override
    public void invalidateNiCache() {
        niLink.invalidate();
    }

    @Override
    @Nullable
    public BlockPos getOrFindNiPos(ServerLevel level) {
        return niLink.getOrFindNiPos(level, worldPosition);
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

    // -------------------------------------------------------------------------
    // NBT serialization
    // -------------------------------------------------------------------------

    private static final String TAG_TYPES = "Types";
    private static final String TAG_TYPE = "Type";
    private static final String TAG_COUNT = "Count";

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (StorageKey key : orderedKeys) {
            CompoundTag entry = new CompoundTag();
            entry.put(TAG_TYPE, key.toDisplayStack().save(registries));
            entry.putLong(TAG_COUNT, items.getLong(key));
            list.add(entry);
        }
        tag.put(TAG_TYPES, list);
        tag.putInt("Priority", priority.ordinal());
        tag.putString("SortMode", sortMode.name());
        tag.putString("Tier", tier.getId());
        if (hasCraftingUpgrade) tag.putBoolean("CraftingUpgrade", true);
        if (hasMagnetUpgrade) tag.putBoolean("MagnetUpgrade", true);
        if (hasPullerUpgrade) {
            tag.putBoolean("PullerUpgrade", true);
            tag.putInt("PullerSides", pullerSides);
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        orderedKeys.clear();
        cachedTotalCount = 0;
        ListTag list = tag.getList(TAG_TYPES, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            long count = entry.getLong(TAG_COUNT);
            ItemStack.parse(registries, entry.getCompound(TAG_TYPE)).ifPresent(stack -> {
                StorageKey key = StorageKey.of(stack);
                items.put(key, count);
                orderedKeys.add(key);
                cachedTotalCount += count;
            });
        }
        priority = Priority.fromOrdinal(tag.getInt("Priority"));
        try {
            sortMode = SortMode.valueOf(tag.getString("SortMode"));
        } catch (IllegalArgumentException e) {
            sortMode = SortMode.AMOUNT;
        }
        tier = StorageTier.fromId(tag.getString("Tier"));
        hasCraftingUpgrade = tag.getBoolean("CraftingUpgrade");
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

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        super.onDataPacket(net, pkt, registries);
        // Re-render the chunk section so IBlockColor picks up the new tier color immediately.
        if (level != null && level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
