package net.bobofraggins.intellistore.networkinterface;

import net.bobofraggins.intellistore.register.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.bobofraggins.intellistore.ui.NetworkInterfaceMenu;

/**
 * Block entity for the lower half of a Network Interface block.
 *
 * <p>Holds a lazily-built {@link NetworkScanResult} that describes all storage blocks
 * reachable through the connected tube network (all colors). The cache is invalidated
 * whenever {@link #setChanged()} is called (which happens on neighbour changes).
 *
 * <p>Exposes an {@link IItemHandler} capability via {@link NiItemHandler}: insertion uses
 * the highest-priority storage first; extraction drains the lowest-priority storage first.
 */
public class NetworkInterfaceBlockEntity extends BlockEntity implements MenuProvider {

    /** Lazily built; {@code null} = stale. */
    private NetworkScanResult cachedScan = null;

    public NetworkInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.NETWORK_INTERFACE_BE_TYPE.get(), pos, state);
    }

    // -------------------------------------------------------------------------
    // Network scan
    // -------------------------------------------------------------------------

    /**
     * Returns the cached scan result, rebuilding it via BFS if stale.
     * Returns {@code null} on the client side or before the level is available.
     */
    public NetworkScanResult getScan() {
        if (level == null || level.isClientSide()) return null;
        if (cachedScan == null) {
            cachedScan = NetworkInterfaceBFS.scan((ServerLevel) level, worldPosition);
        }
        return cachedScan;
    }

    /** Returns {@code true} if exactly one Network Interface is present on the connected network. */
    public boolean isNetworkValid() {
        NetworkScanResult s = getScan();
        return s != null && s.isValid();
    }

    // -------------------------------------------------------------------------
    // IItemHandler capability
    // -------------------------------------------------------------------------

    /**
     * Returns the network's composite item handler, or {@code null} if the scan is unavailable.
     * Insert = highest-priority first; extract = lowest-priority first.
     */
    public IItemHandler getItemHandler() {
        NetworkScanResult s = getScan();
        return s == null ? null : new NiItemHandler(s.insertOrder());
    }

    // -------------------------------------------------------------------------
    // setChanged
    // -------------------------------------------------------------------------

    @Override
    public void setChanged() {
        cachedScan = null; // invalidate before capability notification fires
        super.setChanged();
        if (level != null) {
            level.invalidateCapabilities(worldPosition);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // -------------------------------------------------------------------------
    // MenuProvider
    // -------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("screen.intellistore.network_interface");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        ContainerData data = new ContainerData() {
            @Override
            public int get(int index) {
                return index == 0 ? (isNetworkValid() ? 1 : 0) : 0;
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return 1;
            }
        };
        return new NetworkInterfaceMenu(id, inv, worldPosition, data);
    }

    // -------------------------------------------------------------------------
    // NBT (no persistent fields — scan is transient)
    // -------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
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
