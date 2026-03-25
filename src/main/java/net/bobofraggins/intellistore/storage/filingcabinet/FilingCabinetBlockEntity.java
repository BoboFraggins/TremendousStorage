package net.bobofraggins.intellistore.storage.filingcabinet;

import javax.annotation.Nullable;
import net.bobofraggins.intellistore.shared.priority.Priority;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.bobofraggins.intellistore.storage.accessterminal.AccessTerminalBFS;
import net.bobofraggins.intellistore.storage.networkinterface.NiCacheHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Stores up to {@value #SLOT_COUNT} Manila Folder stacks. */
public class FilingCabinetBlockEntity extends BlockEntity
        implements net.minecraft.world.Container, MenuProvider, NiCacheHolder {

    public static final int SLOT_COUNT = 8;

    private final NonNullList<ItemStack> folders = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private Priority priority = Priority.HIGH;
    private boolean voidExcess = false;

    @Nullable
    private BlockPos cachedNiPos = null;

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
                        net.bobofraggins.intellistore.storage.networkinterface.NetworkInterfaceBlockEntity)) {
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

    // -------------------------------------------------------------------------
    // MenuProvider
    // -------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.intellistore.filing_cabinet");
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
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, folders, registries);
        priority = Priority.fromOrdinal(tag.getInt("Priority"));
        voidExcess = tag.getBoolean("VoidExcess");
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
