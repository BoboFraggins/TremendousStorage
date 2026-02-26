package net.bobofraggins.intellistore.junkdrawer;

import java.util.ArrayList;
import java.util.List;
import net.bobofraggins.intellistore.register.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stores up to {@value #CAPACITY} individual items, one per slot.
 *
 * <p>The Junk Drawer is the complement of Manila Folders: it accepts <em>only</em> items that
 * folders reject — specifically, items that are damageable (tools, armour, weapons) or that
 * carry non-default component data (enchanted books, named items, potions, etc.). Plain
 * stackable items with no extra data are refused.
 *
 * <p>There is no locking. Any qualifying item may be added or removed at any time. All slots
 * are exposed via the {@link JunkDrawerItemHandler} {@code IItemHandler} capability so that
 * automation mods (Applied Energistics, Refined Storage, hoppers, pipes) can query and manage
 * every item independently.
 *
 * <p>No player-facing UI — all interaction is via automation.
 */
public class JunkDrawerBlockEntity extends BlockEntity {

    public static final int CAPACITY = 32_768;

    private final List<ItemStack> items = new ArrayList<>();

    public JunkDrawerBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.JUNK_DRAWER_BE_TYPE.get(), pos, state);
    }

    // -------------------------------------------------------------------------
    // Item filter
    // -------------------------------------------------------------------------

    /**
     * Returns true if the stack qualifies for storage in the Junk Drawer.
     *
     * <p>Accepts items that are damageable OR that carry non-default component data (enchantments,
     * custom name, potion effects, etc.). This is the precise complement of what Manila Folders
     * accept.
     */
    public static boolean accepts(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.isDamageableItem()
                || !stack.getComponents().equals(stack.getItem().components());
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public int size() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.isEmpty();
    }

    public boolean isFull() {
        return items.size() >= CAPACITY;
    }

    /** Returns the item at {@code index} (count == 1), or {@link ItemStack#EMPTY}. */
    public ItemStack get(int index) {
        if (index < 0 || index >= items.size()) return ItemStack.EMPTY;
        return items.get(index);
    }

    /**
     * Appends one copy (count = 1) of the given stack.
     *
     * @return true if the item was added; false if the drawer is full or the item is not accepted.
     */
    public boolean addItem(ItemStack stack) {
        if (isFull() || !accepts(stack)) return false;
        items.add(stack.copyWithCount(1));
        setChanged();
        return true;
    }

    /**
     * Removes and returns the item at {@code index}.
     *
     * @return the removed stack (count == 1), or {@link ItemStack#EMPTY} if out of range.
     */
    public ItemStack removeItem(int index) {
        if (index < 0 || index >= items.size()) return ItemStack.EMPTY;
        ItemStack removed = items.remove(index);
        setChanged();
        return removed;
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

    private static final String TAG_ITEMS = "Items";

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (ItemStack stack : items) {
            list.add(stack.save(registries));
        }
        tag.put(TAG_ITEMS, list);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ListTag list = tag.getList(TAG_ITEMS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            ItemStack.parse(registries, list.getCompound(i)).ifPresent(items::add);
        }
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
