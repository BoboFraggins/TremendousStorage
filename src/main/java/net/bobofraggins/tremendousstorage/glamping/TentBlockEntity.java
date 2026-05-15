package net.bobofraggins.tremendousstorage.glamping;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Persists the tent's name and its assigned glamping-dimension camp origin.
 *
 * <p>Stored on the FOOT half only. The name is copied from the item's
 * {@code CUSTOM_NAME} component when the tent is placed, and restored to the
 * dropped item stack when the tent is broken, so anvil renames survive the
 * place/break cycle.
 *
 * <p>{@code campOrigin} is null until the tent is right-clicked for the first
 * time. On first use, a camp is derived from the name seed and stored here so
 * all subsequent uses teleport to the same location.
 */
public class TentBlockEntity extends BlockEntity {

    @Nullable
    private String tentName = null;

    @Nullable
    private BlockPos campOrigin = null;

    public TentBlockEntity(BlockPos pos, BlockState state) {
        super(GlampingRegistration.TENT_BE_TYPE.get(), pos, state);
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

    @Nullable
    public String getTentName() {
        return tentName;
    }

    public void setTentName(@Nullable String name) {
        tentName = name;
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (tentName != null) output.putString("tentName", tentName);
        if (campOrigin != null) output.putLong("campOrigin", campOrigin.asLong());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tentName = input.getString("tentName").orElse(null);
        campOrigin = input.getLong("campOrigin").map(BlockPos::of).orElse(null);
    }
}
