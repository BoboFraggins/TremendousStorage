package net.bobofraggins.intellistore.tube;

import net.bobofraggins.intellistore.priority.Priority;
import net.bobofraggins.intellistore.register.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stores the attachment state for each of the six tube faces.
 *
 * <p>Each face may independently have a "Storage Interface" attachment, which carries
 * its own {@link Priority} value used when the network routes items.
 */
public class TubeBlockEntity extends BlockEntity {

    /** Whether a Storage Interface is installed on each face (indexed by Direction ordinal). */
    private final boolean[] hasAttachment = new boolean[6];

    /** Priority for each Storage Interface attachment (default NORMAL). */
    private final Priority[] attachmentPriority = new Priority[] {
        Priority.NORMAL, Priority.NORMAL, Priority.NORMAL,
        Priority.NORMAL, Priority.NORMAL, Priority.NORMAL
    };

    /**
     * Cached network view; {@code null} means stale and will be rebuilt on next access.
     * Only valid on the server side.
     */
    private NetworkItemHandler networkCache = null;

    public TubeBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.TUBE_BE_TYPE.get(), pos, state);
    }

    // -------------------------------------------------------------------------
    // Network view
    // -------------------------------------------------------------------------

    /**
     * Returns this tube's network-wide {@link NetworkItemHandler}, building it lazily
     * via BFS if the cache is stale.
     *
     * <p>Returns {@code null} on the client side or before the level is available,
     * which NeoForge treats as "capability absent".
     */
    public NetworkItemHandler getNetworkView() {
        if (level == null || level.isClientSide()) return null;
        if (networkCache == null) {
            DyeColor color = ((TubeBlock) getBlockState().getBlock()).getColor();
            networkCache = TubeNetwork.buildNetworkView((ServerLevel) level, worldPosition, color);
        }
        return networkCache;
    }

    /** Returns true if the network cache is currently populated (not stale). */
    public boolean hasNetworkCache() {
        return networkCache != null;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public boolean hasAttachment(int faceIndex) {
        return faceIndex >= 0 && faceIndex < 6 && hasAttachment[faceIndex];
    }

    public void setAttachment(int faceIndex, boolean present) {
        if (faceIndex < 0 || faceIndex >= 6) return;
        hasAttachment[faceIndex] = present;
        if (!present) attachmentPriority[faceIndex] = Priority.NORMAL;
        setChanged();
    }

    public Priority getAttachmentPriority(int faceIndex) {
        if (faceIndex < 0 || faceIndex >= 6) return Priority.NORMAL;
        return attachmentPriority[faceIndex];
    }

    public void setAttachmentPriority(int faceIndex, Priority p) {
        if (faceIndex < 0 || faceIndex >= 6) return;
        attachmentPriority[faceIndex] = p;
        setChanged();
    }

    // -------------------------------------------------------------------------
    // setChanged
    // -------------------------------------------------------------------------

    @Override
    public void setChanged() {
        networkCache = null;  // clear cache before capability invalidation fires
        super.setChanged();
        if (level != null) {
            level.invalidateCapabilities(worldPosition);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // -------------------------------------------------------------------------
    // NBT
    // -------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // Pack 6 booleans into a single byte
        int mask = 0;
        for (int i = 0; i < 6; i++) {
            if (hasAttachment[i]) mask |= (1 << i);
        }
        tag.putByte("Attachments", (byte) mask);
        // Save priorities as a byte array
        byte[] prios = new byte[6];
        for (int i = 0; i < 6; i++) {
            prios[i] = (byte) attachmentPriority[i].ordinal();
        }
        tag.putByteArray("AttachmentPriorities", prios);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int mask = tag.getByte("Attachments") & 0xFF;
        for (int i = 0; i < 6; i++) {
            hasAttachment[i] = (mask & (1 << i)) != 0;
        }
        byte[] prios = tag.getByteArray("AttachmentPriorities");
        for (int i = 0; i < 6; i++) {
            attachmentPriority[i] = (i < prios.length)
                    ? Priority.fromOrdinal(prios[i] & 0xFF)
                    : Priority.NORMAL;
        }
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
