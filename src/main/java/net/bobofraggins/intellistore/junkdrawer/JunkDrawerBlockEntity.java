package net.bobofraggins.intellistore.junkdrawer;

import net.bobofraggins.intellistore.register.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stores a single item type in quantities up to {@value #CAPACITY}.
 *
 * <p>Unlike Manila Folders, the Junk Drawer accepts any item — including damageable tools,
 * items with NBT data, and anything else folders reject. There is no player-facing UI; all
 * interaction is via hoppers, pipes, or automation mods through the {@link JunkDrawerItemHandler}
 * IItemHandler capability.
 *
 * <p>The drawer is unlocked (empty storedItem) when empty and locks to the first item type
 * inserted. It stays locked at count 0 after drain (same behaviour as Manila Folders) so that
 * automation pulling from the drawer doesn't accidentally re-purpose it mid-chain. Use a Whiteout
 * Tape in the crafting grid to unlock it again.
 */
public class JunkDrawerBlockEntity extends BlockEntity {

    public static final long CAPACITY = 32_768L;

    private ItemStack storedType = ItemStack.EMPTY; // count is always 1; used as the type key
    private long count = 0L;

    public JunkDrawerBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.JUNK_DRAWER_BE_TYPE.get(), pos, state);
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public boolean isEmpty() {
        return storedType.isEmpty() || count == 0;
    }

    /** Returns the item type stored, with count 1, or EMPTY if unlocked. */
    public ItemStack getStoredType() {
        return storedType;
    }

    public long getCount() {
        return count;
    }

    /**
     * Attempts to insert {@code amount} items of the given type.
     *
     * @return number of items that could NOT be inserted (the remainder).
     */
    public long insert(ItemStack type, long amount) {
        if (amount <= 0) return 0;

        if (storedType.isEmpty()) {
            // Unlock → lock to this type
            storedType = type.copyWithCount(1);
        } else if (!ItemStack.isSameItemSameComponents(storedType, type)) {
            return amount; // wrong type
        }

        long space = CAPACITY - count;
        long toInsert = Math.min(amount, space);
        count += toInsert;
        setChanged();
        return amount - toInsert;
    }

    /**
     * Extracts up to {@code amount} items.
     *
     * @return the extracted stack (may be smaller than requested).
     */
    public ItemStack extract(long amount) {
        if (storedType.isEmpty() || count == 0) return ItemStack.EMPTY;

        long toExtract = Math.min(amount, Math.min(storedType.getMaxStackSize(), count));
        if (toExtract <= 0) return ItemStack.EMPTY;

        count -= toExtract;
        setChanged();
        return storedType.copyWithCount((int) toExtract);
    }

    /**
     * Clears the stored item type, resetting the drawer to unlocked state.
     * Only valid when count == 0.
     */
    public void clearType() {
        storedType = ItemStack.EMPTY;
        count = 0;
        setChanged();
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

    private static final String TAG_STORED_TYPE = "StoredType";
    private static final String TAG_COUNT = "Count";

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!storedType.isEmpty()) {
            tag.put(TAG_STORED_TYPE, storedType.save(registries));
        }
        tag.putLong(TAG_COUNT, count);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TAG_STORED_TYPE)) {
            storedType = ItemStack.parse(registries, tag.getCompound(TAG_STORED_TYPE))
                    .orElse(ItemStack.EMPTY);
            if (!storedType.isEmpty()) {
                storedType = storedType.copyWithCount(1);
            }
        } else {
            storedType = ItemStack.EMPTY;
        }
        count = tag.getLong(TAG_COUNT);
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

    // -------------------------------------------------------------------------
    // Server tick (no-op — kept for symmetry with Filing Cabinet)
    // -------------------------------------------------------------------------

    public static void serverTick(
            Level level, BlockPos pos, BlockState state, JunkDrawerBlockEntity be) {
        // intentionally empty
    }
}
