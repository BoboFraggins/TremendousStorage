package net.bobofraggins.intellistore.storage.networkinterface;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Exposes a NetworkConnector block entity's connected Network Interface energy buffer as an
 * {@link IEnergyStorage} capability.
 *
 * <p>This allows any power source (Stirling Engine, Pipez cables, Mekanism cables, etc.) adjacent
 * to any NetworkConnector block (Filing Cabinet, Junk Drawer, Bulk Storage, Wireless Hub, Access
 * Terminal) to inject energy into the network's NI buffer — the same way {@link
 * net.bobofraggins.intellistore.storage.tube.TubeEnergyHandler} does for tubes.
 *
 * <p>The NI is located on each call via {@link AccessTerminalBFS#findNI}. Only
 * {@code receiveEnergy} is supported; extraction is not allowed.
 */
public class ConnectorEnergyHandler implements IEnergyStorage {

    private final BlockEntity be;

    public ConnectorEnergyHandler(BlockEntity be) {
        this.be = be;
    }

    private NetworkInterfaceBlockEntity resolveNi() {
        if (!(be.getLevel() instanceof ServerLevel sl)) return null;
        if (!(be instanceof NiCacheHolder holder)) return null;
        var niPos = holder.getOrFindNiPos(sl);
        if (niPos == null) return null;
        return sl.getBlockEntity(niPos) instanceof NetworkInterfaceBlockEntity ni ? ni : null;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        NetworkInterfaceBlockEntity ni = resolveNi();
        return ni != null ? ni.receiveEnergy(maxReceive, simulate) : 0;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        NetworkInterfaceBlockEntity ni = resolveNi();
        return ni != null ? ni.getEnergyStored() : 0;
    }

    @Override
    public int getMaxEnergyStored() {
        return NetworkInterfaceBlockEntity.MAX_ENERGY;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }
}
