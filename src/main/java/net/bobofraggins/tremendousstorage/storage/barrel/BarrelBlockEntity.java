package net.bobofraggins.tremendousstorage.storage.barrel;

import java.util.Optional;
import net.bobofraggins.tremendousstorage.shared.priority.Priority;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.storage.networkinterface.NetworkListable;
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
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class BarrelBlockEntity extends BlockEntity implements MenuProvider, NetworkListable {

    /** Type key — always count=1; empty means unlocked. */
    protected ItemStack storedItem = ItemStack.EMPTY;

    protected long count = 0L;
    protected StorageTier tier = StorageTier.WOOD;
    protected boolean voidExcess = false;
    protected Priority priority = Priority.NORMAL;

    private final NiLink niLink = new NiLink();

    public BarrelBlockEntity(BlockPos pos, BlockState state) {
        this(Registration.BARREL_BE_TYPE.get(), pos, state);
    }

    protected BarrelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public boolean isLocked() {
        return !storedItem.isEmpty();
    }

    public ItemStack getStoredItem() {
        return storedItem;
    }

    public long getCount() {
        return count;
    }

    public long getCapacity() {
        return tier.getCapacity();
    }

    public StorageTier getTier() {
        return tier;
    }

    public void setTier(StorageTier tier) {
        this.tier = tier;
        setChanged();
        if (level != null && !level.isClientSide) {
            BarrelBlock.setTierBlockState(level, worldPosition, tier);
        }
    }

    public boolean isVoidExcess() {
        return voidExcess;
    }

    public void setVoidExcess(boolean value) {
        this.voidExcess = value;
        setChanged();
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority value) {
        this.priority = value;
        setChanged();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            BarrelBlock.setTierBlockState(level, worldPosition, tier);
        }
    }

    // -------------------------------------------------------------------------
    // Insert / extract
    // -------------------------------------------------------------------------

    /**
     * Inserts up to {@code amount} items from {@code stack} into the barrel.
     *
     * @return count not inserted (remainder)
     */
    public long insert(ItemStack stack, long amount, boolean simulate) {
        if (stack.isEmpty() || amount <= 0) return amount;
        if (isLocked() && !ItemStack.isSameItemSameComponents(storedItem, stack)) return amount;

        long space = getCapacity() - count;
        long toInsert = Math.min(amount, voidExcess ? amount : space);
        if (toInsert <= 0) return amount;

        if (!simulate) {
            if (!isLocked()) storedItem = stack.copyWithCount(1);
            if (voidExcess) {
                count = Math.min(count + amount, getCapacity());
            } else {
                count += toInsert;
            }
            notifyChanged();
        }
        return voidExcess ? 0 : (amount - toInsert);
    }

    /** Extracts up to {@code amount} items. */
    public ItemStack extract(long amount, boolean simulate) {
        if (!isLocked() || count == 0 || amount <= 0) return ItemStack.EMPTY;
        long toExtract = Math.min(amount, count);
        if (toExtract <= 0) return ItemStack.EMPTY;
        ItemStack result = storedItem.copyWithCount((int) Math.min(toExtract, Integer.MAX_VALUE));
        if (!simulate) {
            count -= toExtract;
            notifyChanged();
        }
        return result;
    }

    /** Clears lock. Only safe when count == 0. */
    public void clearItem() {
        storedItem = ItemStack.EMPTY;
        count = 0;
        notifyChanged();
    }

    // -------------------------------------------------------------------------
    // Drop items when block is broken without preserving container
    // -------------------------------------------------------------------------

    public void drops() {
        // Items are preserved in the dropped block item via collectImplicitComponents.
        // This method intentionally left empty (contents stay in item).
    }

    // -------------------------------------------------------------------------
    // setChanged / notify
    // -------------------------------------------------------------------------

    /** Directly sets item type and count without going through insert/extract logic. */
    protected void loadContents(ItemStack newStoredItem, long newCount) {
        storedItem = newStoredItem.isEmpty() ? ItemStack.EMPTY : newStoredItem.copyWithCount(1);
        count = newCount;
        super.setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    protected void notifyChanged() {
        super.setChanged();
        if (level instanceof ServerLevel sl) {
            niLink.notifyChanged(sl, worldPosition, getBlockState());
            sl.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
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
    // MenuProvider / ExtendedMenuProvider
    // -------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.tremendousstorage.barrel");
    }

    @Override
    public String getNetworkName() {
        return getDisplayName().getString();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        ContainerData data = new SimpleContainerData(2) {
            @Override
            public int get(int index) {
                if (index == 0) return voidExcess ? 1 : 0;
                if (index == 1) return priority.ordinal();
                return 0;
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return 2;
            }
        };
        return new BarrelMenu(id, inv, worldPosition, data);
    }

    // -------------------------------------------------------------------------
    // NBT
    // -------------------------------------------------------------------------

    private static final String TAG_ITEM = "StoredItem";
    private static final String TAG_COUNT = "Count";
    private static final String TAG_TIER = "Tier";
    private static final String TAG_VOID_EXCESS = "VoidExcess";
    private static final String TAG_PRIORITY = "Priority";

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!storedItem.isEmpty()) {
            tag.put(TAG_ITEM, storedItem.save(registries));
        }
        tag.putLong(TAG_COUNT, count);
        tag.putString(TAG_TIER, tier.getId());
        tag.putBoolean(TAG_VOID_EXCESS, voidExcess);
        tag.putInt(TAG_PRIORITY, priority.ordinal());
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TAG_ITEM)) {
            storedItem = ItemStack.parseOptional(registries, tag.getCompound(TAG_ITEM));
            if (!storedItem.isEmpty()) storedItem = storedItem.copyWithCount(1);
        } else {
            storedItem = ItemStack.EMPTY;
        }
        count = tag.getLong(TAG_COUNT);
        tier = StorageTier.fromId(tag.getString(TAG_TIER));
        voidExcess = tag.getBoolean(TAG_VOID_EXCESS);
        priority = Priority.fromOrdinal(tag.getInt(TAG_PRIORITY));
    }

    // -------------------------------------------------------------------------
    // Item component sync
    // -------------------------------------------------------------------------

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(
                Registration.BARREL_CONTENTS.get(),
                new BarrelContents(storedItem.isEmpty() ? Optional.empty() : Optional.of(storedItem), count));
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        BarrelContents contents = input.get(Registration.BARREL_CONTENTS);
        if (contents != null) {
            storedItem = contents.storedItem().map(s -> s.copyWithCount(1)).orElse(ItemStack.EMPTY);
            count = contents.count();
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove(TAG_ITEM);
        tag.remove(TAG_COUNT);
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
