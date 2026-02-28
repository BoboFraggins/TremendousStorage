package net.bobofraggins.intellistore.storage.junkdrawer;

import java.util.ArrayList;
import java.util.List;
import net.bobofraggins.intellistore.shared.priority.Priority;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.bobofraggins.intellistore.shared.ui.PriorityMenu;
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
public class JunkDrawerBlockEntity extends BlockEntity implements MenuProvider {

    public static final int CAPACITY = 32_768;

    private final List<ItemStack> items = new ArrayList<>();
    private Priority priority = Priority.NORMAL;

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
        return Component.translatable("block.intellistore.junk_drawer");
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
        tag.putInt("Priority", priority.ordinal());
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ListTag list = tag.getList(TAG_ITEMS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            ItemStack.parse(registries, list.getCompound(i)).ifPresent(items::add);
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
