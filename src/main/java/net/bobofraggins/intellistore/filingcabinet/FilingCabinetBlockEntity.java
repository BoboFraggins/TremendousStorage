package net.bobofraggins.intellistore.filingcabinet;

import net.bobofraggins.intellistore.priority.Priority;
import net.bobofraggins.intellistore.register.Registration;
import net.bobofraggins.intellistore.ui.FilingCabinetMenu;
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
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Stores up to {@value #SLOT_COUNT} Manila Folder stacks and tracks the open/close state. */
public class FilingCabinetBlockEntity extends BlockEntity implements net.minecraft.world.Container, MenuProvider {

    public static final int SLOT_COUNT = 8;

    /** Speed at which the drawer opens/closes, in block units per tick. */
    public static final float OFFSET_SPEED = 0.1f;

    /** Fully-open drawer offset (Z, in model-space units, negative = pulled out). */
    public static final float OFFSET_OPEN = -0.75f;

    /** Fully-closed drawer offset. */
    public static final float OFFSET_CLOSED = 0.05f;

    private final NonNullList<ItemStack> folders = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private boolean isOpen = false;
    private Priority priority = Priority.HIGH;

    // Client-only animation state (not saved to NBT).
    public float drawerOffset = OFFSET_CLOSED;
    public float prevDrawerOffset = OFFSET_CLOSED;

    public FilingCabinetBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.FILING_CABINET_BE_TYPE.get(), pos, state);
    }

    // -------------------------------------------------------------------------
    // Tick
    // -------------------------------------------------------------------------

    /**
     * Server-side tick — no-op; all interaction state is synced via {@link #getUpdatePacket()}.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, FilingCabinetBlockEntity be) {
        // intentionally empty
    }

    /**
     * Client-side tick: smoothly animates the drawer toward its target offset.
     */
    public static void clientTick(Level level, BlockPos pos, BlockState state, FilingCabinetBlockEntity be) {
        be.prevDrawerOffset = be.drawerOffset;
        if (be.isOpen) {
            be.drawerOffset = Math.max(OFFSET_OPEN, be.drawerOffset - OFFSET_SPEED);
        } else {
            be.drawerOffset = Math.min(OFFSET_CLOSED, be.drawerOffset + OFFSET_SPEED);
        }
    }

    // -------------------------------------------------------------------------
    // Capability invalidation
    // -------------------------------------------------------------------------

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null) {
            // Invalidate cached capability references (hoppers, pipes, etc.)
            level.invalidateCapabilities(worldPosition);
            // Push the update packet to watching clients so the renderer stays in sync.
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

    public boolean isOpen() {
        return isOpen;
    }

    public void setOpen(boolean open) {
        this.isOpen = open;
        setChanged();
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority p) {
        this.priority = p;
        setChanged();
    }

    /** Returns the index of the first empty slot, or -1 if all slots are full. */
    public int firstEmptySlot() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (folders.get(i).isEmpty()) return i;
        }
        return -1;
    }

    /** Returns the highest-index occupied slot, or -1 if the cabinet is empty. */
    public int lastOccupiedSlot() {
        for (int i = SLOT_COUNT - 1; i >= 0; i--) {
            if (!folders.get(i).isEmpty()) return i;
        }
        return -1;
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
        ContainerData data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> priority.ordinal();
                    case 1 -> isOpen ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // Server-side data; changes come via packets
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
        return new FilingCabinetMenu(id, inv, worldPosition, data);
    }

    // -------------------------------------------------------------------------
    // NBT serialization
    // -------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, folders, registries);
        tag.putBoolean("IsOpen", isOpen);
        tag.putInt("Priority", priority.ordinal());
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, folders, registries);
        isOpen = tag.getBoolean("IsOpen");
        priority = Priority.fromOrdinal(tag.getInt("Priority"));
        // Snap animation to final position when loading (no lerp from wrong state).
        drawerOffset = prevDrawerOffset = isOpen ? OFFSET_OPEN : OFFSET_CLOSED;
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
