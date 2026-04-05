package net.bobofraggins.intellistore.storage.tube;

import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Exposes a tube block entity's local energy buffer as an {@link IEnergyStorage} capability.
 *
 * <p>This allows energy pipes (Pipez, Mekanism cables, etc.) adjacent to any tube to
 * inject energy, which the tube then distributes to adjacent non-tube blocks each server tick.
 */
public class TubeEnergyHandler implements IEnergyStorage {

    private final TubeBlockEntity tubeBE;

    public TubeEnergyHandler(TubeBlockEntity tubeBE) {
        this.tubeBE = tubeBE;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return tubeBE.receiveTubeEnergy(maxReceive, simulate);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return tubeBE.extractTubeEnergy(maxExtract, simulate);
    }

    @Override
    public int getEnergyStored() {
        return tubeBE.getTubeEnergy();
    }

    @Override
    public int getMaxEnergyStored() {
        return tubeBE.getMaxTubeEnergy();
    }

    @Override
    public boolean canExtract() {
        return tubeBE.getTubeEnergy() > 0;
    }

    @Override
    public boolean canReceive() {
        return tubeBE.getTubeEnergy() < tubeBE.getMaxTubeEnergy();
    }
}
