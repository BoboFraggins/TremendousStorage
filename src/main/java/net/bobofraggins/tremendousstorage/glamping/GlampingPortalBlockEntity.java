package net.bobofraggins.tremendousstorage.glamping;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stores the glamping-dimension camp origin that this portal is linked to.
 *
 * <p>The first time the portal is used, {@link GlampingPortalBlock} allocates a camp,
 * carves the 16x16x16 space, and stores the origin here so every subsequent use
 * teleports to the same camp.
 */
public class GlampingPortalBlockEntity extends BlockEntity {

    @Nullable
    private BlockPos campOrigin = null;

    public GlampingPortalBlockEntity(BlockPos pos, BlockState state) {
        super(GlampingRegistration.GLAMPING_PORTAL_BE_TYPE.get(), pos, state);
    }

    public boolean hasCamp() {
        return campOrigin != null;
    }

    @Nullable
    public BlockPos getCampOrigin() {
        return campOrigin;
    }

    public void setCampOrigin(BlockPos pos) {
        campOrigin = pos;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (campOrigin != null) {
            tag.putLong("campOrigin", campOrigin.asLong());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains("campOrigin")) {
            campOrigin = BlockPos.of(tag.getLong("campOrigin"));
        }
    }
}
