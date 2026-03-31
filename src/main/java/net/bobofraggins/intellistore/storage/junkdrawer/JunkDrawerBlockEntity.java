package net.bobofraggins.intellistore.storage.junkdrawer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.bobofraggins.intellistore.shared.priority.Priority;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.bobofraggins.intellistore.shared.storage.KeyCounter;
import net.bobofraggins.intellistore.shared.storage.StorageKey;
import net.bobofraggins.intellistore.shared.storage.StorageTier;
import net.bobofraggins.intellistore.storage.networkinterface.NiCacheHolder;
import net.bobofraggins.intellistore.storage.networkinterface.NiLink;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stores individual items, one per slot (capacity determined by {@link StorageTier}).
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
public class JunkDrawerBlockEntity extends BlockEntity implements MenuProvider, NiCacheHolder {

    // keySet provides O(1) duplicate detection; orderedKeys provides stable index-based access
    private final Set<StorageKey> keySet = new HashSet<>();
    private final List<StorageKey> orderedKeys = new ArrayList<>();
    private long cachedTotalCount = 0;
    private Priority priority = Priority.NORMAL;
    private StorageTier tier = StorageTier.PAPER;

    public long getCapacity() {
        return tier.getCapacity();
    }

    private final NiLink niLink = new NiLink();

    // -------------------------------------------------------------------------
    // Door animation state (client-side only)
    // -------------------------------------------------------------------------

    /** Number of players with this block's menu open (synced from server via block event). */
    public int openCount = 0;

    /** Animation progress last tick (0 = closed, 1 = fully open). */
    public float prevDoorAngle = 0f;

    /** Animation progress this tick. */
    public float doorAngle = 0f;

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
            return player.containerMenu instanceof JunkDrawerMenu m
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

    /** Called from {@link JunkDrawerBlock#onRemove} to force-close all openers. */
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, JunkDrawerBlockEntity be) {
        be.openersCounter.recheckOpeners(level, pos, state);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, JunkDrawerBlockEntity be) {
        be.prevDoorAngle = be.doorAngle;
        if (be.openCount > 0 && be.doorAngle < 1f) {
            be.doorAngle = Math.min(1f, be.doorAngle + 0.1f);
        } else if (be.openCount == 0 && be.doorAngle > 0f) {
            be.doorAngle = Math.max(0f, be.doorAngle - 0.1f);
        }
    }

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

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
        return stack.isDamageableItem() || !stack.isComponentsPatchEmpty();
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public int size() {
        return orderedKeys.size();
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

    /** Returns the item at {@code index} (count == 1), or {@link ItemStack#EMPTY}. */
    public ItemStack get(int index) {
        if (index < 0 || index >= orderedKeys.size()) return ItemStack.EMPTY;
        return orderedKeys.get(index).toDisplayStack();
    }

    /**
     * Appends one copy (count = 1) of the given stack.
     *
     * @return true if the item was added; false if the drawer is full, the item is not accepted,
     *     or an identical item (same type + components) is already present.
     */
    public boolean addItem(ItemStack stack) {
        if (isFull() || !accepts(stack)) return false;
        StorageKey key = StorageKey.of(stack);
        if (!keySet.add(key)) return false; // O(1) duplicate check
        orderedKeys.add(key);
        cachedTotalCount++;
        notifyItemsChanged();
        return true;
    }

    /**
     * Removes and returns the item at {@code index}.
     *
     * @return the removed stack (count == 1), or {@link ItemStack#EMPTY} if out of range.
     */
    public ItemStack removeItem(int index) {
        if (index < 0 || index >= orderedKeys.size()) return ItemStack.EMPTY;
        StorageKey key = orderedKeys.remove(index);
        keySet.remove(key);
        cachedTotalCount--;
        notifyItemsChanged();
        return key.toDisplayStack();
    }

    // -------------------------------------------------------------------------
    // KeyCounter contribution
    // -------------------------------------------------------------------------

    /** Adds all stored items (count = 1 each) to the given KeyCounter. */
    public void populateKeyCounter(KeyCounter kc) {
        for (StorageKey key : orderedKeys) {
            kc.add(key, 1L);
        }
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
        Component base = Component.translatable("block.intellistore.junk_drawer");
        if (tier == StorageTier.PAPER) return base;
        String label =
                Character.toUpperCase(tier.getId().charAt(0)) + tier.getId().substring(1);
        return base.copy().append(Component.literal(" (" + label + ")"));
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
        return new JunkDrawerMenu(id, inv, worldPosition, data);
    }

    // -------------------------------------------------------------------------
    // NiCacheHolder + setChanged
    // -------------------------------------------------------------------------

    /**
     * Lightweight notification for item-content mutations (addItem/removeItem).
     *
     * <p>Persists NBT and tells the NI the contents changed, but does NOT invalidate the
     * topology cache or fire {@code level.invalidateCapabilities}.
     */
    private void notifyItemsChanged() {
        super.setChanged();
        if (level instanceof ServerLevel sl) {
            niLink.notifyChanged(sl, worldPosition, getBlockState());
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

    private static final String TAG_ITEMS = "Items";

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (StorageKey key : orderedKeys) {
            list.add(key.toDisplayStack().save(registries));
        }
        tag.put(TAG_ITEMS, list);
        tag.putInt("Priority", priority.ordinal());
        tag.putString("Tier", tier.getId());
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        keySet.clear();
        orderedKeys.clear();
        cachedTotalCount = 0;
        ListTag list = tag.getList(TAG_ITEMS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            ItemStack.parse(registries, list.getCompound(i)).ifPresent(stack -> {
                StorageKey key = StorageKey.of(stack);
                if (keySet.add(key)) {
                    orderedKeys.add(key);
                    cachedTotalCount++;
                }
            });
        }
        priority = Priority.fromOrdinal(tag.getInt("Priority"));
        tier = StorageTier.fromId(tag.getString("Tier"));
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
