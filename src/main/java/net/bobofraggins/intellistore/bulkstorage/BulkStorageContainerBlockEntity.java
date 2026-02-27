package net.bobofraggins.intellistore.bulkstorage;

import java.util.ArrayList;
import java.util.List;
import net.bobofraggins.intellistore.priority.Priority;
import net.bobofraggins.intellistore.register.Registration;
import net.bobofraggins.intellistore.ui.PriorityMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stores up to {@value #CAPACITY} items in a shared pool across any number of distinct types.
 *
 * <p>The Bulk Storage Container is the complement of the Junk Drawer: it accepts <em>only</em>
 * items that Manila Folders accept — non-damageable items with default component data (plain
 * stackable items). Items with enchantments, custom names, or other non-default data are refused.
 *
 * <p>Multiple distinct item types may be stored simultaneously. The total item count across
 * all types is bounded by {@value #CAPACITY}. There is no locking — any qualifying item may be
 * freely added or removed at any time.
 *
 * <p>Internally, items are stored as a list of {@code (type, count)} entries. Each type
 * occupies exactly one entry; the count may be up to the remaining pool capacity. Items of the
 * same type are merged into a single entry. When a type's count reaches zero its entry is removed.
 *
 * <p>No player-facing UI — all interaction is via the {@link BulkStorageContainerItemHandler}
 * {@code IItemHandler} capability.
 */
public class BulkStorageContainerBlockEntity extends BlockEntity implements MenuProvider {

    public static final long CAPACITY = 32_768L;

    // Each entry: type key (count==1) + stored count
    private final List<ItemStack> types = new ArrayList<>();
    private final List<Long> counts = new ArrayList<>();
    private Priority priority = Priority.LOW;

    public BulkStorageContainerBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.BULK_STORAGE_CONTAINER_BE_TYPE.get(), pos, state);
    }

    // -------------------------------------------------------------------------
    // Item filter
    // -------------------------------------------------------------------------

    /**
     * Returns true if the stack qualifies for storage in the Bulk Storage Container.
     *
     * <p>Accepts non-damageable items whose component data matches the item's defaults — the
     * same items that Manila Folders accept, and the precise complement of what the Junk Drawer
     * accepts.
     */
    public static boolean accepts(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return !stack.isDamageableItem()
                && stack.getComponents().equals(stack.getItem().components());
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /** Total number of items stored across all types. */
    public long totalCount() {
        long total = 0;
        for (long c : counts) total += c;
        return total;
    }

    public boolean isEmpty() {
        return types.isEmpty();
    }

    public boolean isFull() {
        return totalCount() >= CAPACITY;
    }

    /** Number of distinct item types currently stored. */
    public int typeCount() {
        return types.size();
    }

    /** Returns the type key at {@code index} (count == 1), or {@link ItemStack#EMPTY}. */
    public ItemStack getType(int index) {
        if (index < 0 || index >= types.size()) return ItemStack.EMPTY;
        return types.get(index);
    }

    /** Returns the stored count for the type at {@code index}, or 0. */
    public long getCount(int index) {
        if (index < 0 || index >= counts.size()) return 0;
        return counts.get(index);
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

        long space = CAPACITY - totalCount();
        if (space <= 0) return amount;

        long toInsert = Math.min(amount, space);

        if (!simulate) {
            // Find existing entry for this type
            int idx = findType(type);
            if (idx >= 0) {
                counts.set(idx, counts.get(idx) + toInsert);
            } else {
                types.add(type.copyWithCount(1));
                counts.add(toInsert);
            }
            setChanged();
        }

        return amount - toInsert;
    }

    /**
     * Extracts up to {@code amount} items of the given type from the entry at {@code index}.
     *
     * @return the extracted stack (may be smaller than requested), or EMPTY if nothing available.
     */
    public ItemStack extract(int index, long amount, boolean simulate) {
        if (index < 0 || index >= types.size()) return ItemStack.EMPTY;

        ItemStack type = types.get(index);
        long stored = counts.get(index);
        if (stored == 0) return ItemStack.EMPTY;

        long toExtract = Math.min(amount, Math.min(type.getMaxStackSize(), stored));
        if (toExtract <= 0) return ItemStack.EMPTY;

        if (!simulate) {
            long remaining = stored - toExtract;
            if (remaining == 0) {
                types.remove(index);
                counts.remove(index);
            } else {
                counts.set(index, remaining);
            }
            setChanged();
        }

        return type.copyWithCount((int) toExtract);
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

    // -------------------------------------------------------------------------
    // MenuProvider
    // -------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.intellistore.bulk_storage_container");
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
        return new PriorityMenu(id, inv, worldPosition, data);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Returns the index of an existing entry matching {@code type}, or -1. */
    int findType(ItemStack type) {
        for (int i = 0; i < types.size(); i++) {
            if (ItemStack.isSameItemSameComponents(types.get(i), type)) return i;
        }
        return -1;
    }

    // -------------------------------------------------------------------------
    // setChanged
    // -------------------------------------------------------------------------

    @Override
    public void setChanged() {
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
        for (int i = 0; i < types.size(); i++) {
            CompoundTag entry = new CompoundTag();
            entry.put(TAG_TYPE, types.get(i).save(registries));
            entry.putLong(TAG_COUNT, counts.get(i));
            list.add(entry);
        }
        tag.put(TAG_TYPES, list);
        tag.putInt("Priority", priority.ordinal());
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        types.clear();
        counts.clear();
        ListTag list = tag.getList(TAG_TYPES, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            ItemStack.parse(registries, entry.getCompound(TAG_TYPE)).ifPresent(stack -> {
                types.add(stack.copyWithCount(1));
                counts.add(entry.getLong(TAG_COUNT));
            });
        }
        priority = Priority.fromOrdinal(tag.getInt("Priority"));
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
