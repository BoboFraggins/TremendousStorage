package net.bobofraggins.intellistore.storage.filingcabinet;

import net.bobofraggins.intellistore.shared.priority.Priority;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.bobofraggins.intellistore.shared.ui.FilingCabinetMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Stores up to {@value #SLOT_COUNT} Manila Folder stacks. */
public class FilingCabinetBlockEntity extends BlockEntity implements net.minecraft.world.Container, MenuProvider {

    public static final int SLOT_COUNT = 8;

    private final NonNullList<ItemStack> folders = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private Priority priority = Priority.HIGH;
    private boolean voidExcess = false;

    public FilingCabinetBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.FILING_CABINET_BE_TYPE.get(), pos, state);
    }

    // -------------------------------------------------------------------------
    // Capability invalidation
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
