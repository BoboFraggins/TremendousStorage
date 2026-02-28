package net.bobofraggins.intellistore.external.mekanism;

import mekanism.api.chemical.ChemicalStack;
import net.bobofraggins.intellistore.shared.ui.TankSettingsMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stores a single Mekanism chemical type in quantities up to {@value #CAPACITY} mB.
 *
 * <p>The tank is unlocked ({@code storedChemical} is EMPTY) when fresh and locks to the first
 * chemical inserted. It stays locked at amount 0 after a full drain.
 *
 * <p>All interaction is via the {@link GasTankChemicalHandler} IChemicalHandler capability,
 * exposed to Mekanism pressure tubes and Pipez gas pipes.
 */
public class GasTankBlockEntity extends BlockEntity implements MenuProvider {

    public static final long CAPACITY = 128_000L;

    /** Type key — always has amount=1. EMPTY means unlocked. */
    private ChemicalStack storedChemical = ChemicalStack.EMPTY;

    private long amount = 0L;
    private boolean voidExcess = false;

    public GasTankBlockEntity(BlockPos pos, BlockState state) {
        super(GasTankRegistration.GAS_TANK_BE_TYPE.get(), pos, state);
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public boolean isLocked() {
        return !storedChemical.isEmpty();
    }

    /** Returns the stored chemical type (amount=1), or EMPTY if unlocked. */
    public ChemicalStack getStoredChemical() {
        return storedChemical;
    }

    public long getAmount() {
        return amount;
    }

    public boolean isVoidExcess() {
        return voidExcess;
    }

    public void setVoidExcess(boolean voidExcess) {
        this.voidExcess = voidExcess;
        setChanged();
    }

    // -------------------------------------------------------------------------
    // Mutation
    // -------------------------------------------------------------------------

    /**
     * Attempts to insert up to {@code requested} mB of the given chemical.
     *
     * @param chemical  the chemical type to insert (amount ignored for type check)
     * @param requested how many mB to insert
     * @param simulate  if true, don't modify state
     * @return how many mB were actually inserted
     */
    public long insert(ChemicalStack chemical, long requested, boolean simulate) {
        if (chemical.isEmpty() || requested <= 0) return 0;

        if (!storedChemical.isEmpty() && !storedChemical.getChemical().equals(chemical.getChemical())) {
            return 0; // locked to a different chemical
        }

        long space = CAPACITY - amount;
        long toInsert = Math.min(requested, space);
        if (toInsert <= 0) return 0;

        if (!simulate) {
            if (storedChemical.isEmpty()) {
                storedChemical = chemical.copyWithAmount(1);
            }
            amount += toInsert;
            setChanged();
        }
        return toInsert;
    }

    /**
     * Extracts up to {@code requested} mB.
     *
     * @param simulate if true, don't modify state
     * @return the extracted ChemicalStack (may be smaller than requested), or EMPTY
     */
    public ChemicalStack extract(long requested, boolean simulate) {
        if (storedChemical.isEmpty() || amount == 0 || requested <= 0) return ChemicalStack.EMPTY;

        long toExtract = Math.min(requested, amount);
        if (toExtract <= 0) return ChemicalStack.EMPTY;

        ChemicalStack result = storedChemical.copyWithAmount(toExtract);
        if (!simulate) {
            amount -= toExtract;
            setChanged();
        }
        return result;
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
    // MenuProvider
    // -------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.intellistore.gas_tank");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        ContainerData data = new ContainerData() {
            @Override
            public int get(int index) {
                return index == 0 ? (voidExcess ? 1 : 0) : 0;
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return 1;
            }
        };
        return new TankSettingsMenu(id, inv, worldPosition, data);
    }

    // -------------------------------------------------------------------------
    // NBT
    // -------------------------------------------------------------------------

    private static final String TAG_CHEMICAL = "StoredChemical";
    private static final String TAG_AMOUNT = "Amount";
    private static final String TAG_VOID_EXCESS = "VoidExcess";

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!storedChemical.isEmpty()) {
            tag.put(TAG_CHEMICAL, storedChemical.saveOptional(registries));
        }
        tag.putLong(TAG_AMOUNT, amount);
        tag.putBoolean(TAG_VOID_EXCESS, voidExcess);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        voidExcess = tag.getBoolean(TAG_VOID_EXCESS);
        if (tag.contains(TAG_CHEMICAL, Tag.TAG_COMPOUND)) {
            storedChemical = ChemicalStack.OPTIONAL_CODEC
                    .parse(
                            registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),
                            tag.get(TAG_CHEMICAL))
                    .result()
                    .orElse(ChemicalStack.EMPTY);
            if (!storedChemical.isEmpty()) {
                storedChemical = storedChemical.copyWithAmount(1);
            }
        } else {
            storedChemical = ChemicalStack.EMPTY;
        }
        amount = tag.getLong(TAG_AMOUNT);
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
