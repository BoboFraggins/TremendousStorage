package net.bobofraggins.intellistore.external.arsnouveau;

import net.bobofraggins.intellistore.shared.ui.TankSettingsMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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
 * Stores Ars Nouveau source in quantities up to {@value #CAPACITY}.
 *
 * <p>Source is a plain {@code int} — there is no "type"; the tank is always unlocked.
 * All automation access is via the {@link SourceTankSourceCapHandler} {@code ISourceCap}
 * capability, which Ars Nouveau's source network queries directly.
 */
public class SourceTankBlockEntity extends BlockEntity implements MenuProvider {

    /** 100,000 source — 10× a vanilla Source Jar (10,000). */
    public static final int CAPACITY = 100_000;

    private int amount = 0;
    private boolean voidExcess = false;

    public SourceTankBlockEntity(BlockPos pos, BlockState state) {
        super(SourceTankRegistration.SOURCE_TANK_BE_TYPE.get(), pos, state);
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public int getAmount() {
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
    // Mutation — used by SourceTankSourceCapHandler
    // -------------------------------------------------------------------------

    /**
     * Receives up to {@code requested} source into the tank.
     *
     * @param requested amount to receive
     * @param simulate  if true, don't modify state
     * @return how much was actually received
     */
    public int receive(int requested, boolean simulate) {
        if (requested <= 0) return 0;
        int space = CAPACITY - amount;
        int toReceive = Math.min(requested, space);
        if (toReceive <= 0) return 0;
        if (!simulate) {
            amount += toReceive;
            setChanged();
        }
        return toReceive;
    }

    /**
     * Extracts up to {@code requested} source from the tank.
     *
     * @param requested amount to extract
     * @param simulate  if true, don't modify state
     * @return how much was actually extracted
     */
    public int extract(int requested, boolean simulate) {
        if (requested <= 0 || amount == 0) return 0;
        int toExtract = Math.min(requested, amount);
        if (!simulate) {
            amount -= toExtract;
            setChanged();
        }
        return toExtract;
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
        return Component.translatable("block.intellistore.source_tank");
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

    private static final String TAG_AMOUNT = "Amount";
    private static final String TAG_VOID_EXCESS = "VoidExcess";

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(TAG_AMOUNT, amount);
        tag.putBoolean(TAG_VOID_EXCESS, voidExcess);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        amount = tag.getInt(TAG_AMOUNT);
        voidExcess = tag.getBoolean(TAG_VOID_EXCESS);
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
