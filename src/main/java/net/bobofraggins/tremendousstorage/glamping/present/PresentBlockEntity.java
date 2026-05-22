package net.bobofraggins.tremendousstorage.glamping.present;

import net.bobofraggins.tremendousstorage.shared.register.BETypeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class PresentBlockEntity extends BlockEntity {

    @Nullable
    private BlockState wrappedState;

    @Nullable
    private CompoundTag wrappedEntityData;

    public PresentBlockEntity(BlockPos pos, BlockState state) {
        super(BETypeHelper.get("present"), pos, state);
    }

    public boolean hasWrappedBlock() {
        return wrappedState != null;
    }

    @Nullable
    public BlockState getWrappedState() {
        return wrappedState;
    }

    @Nullable
    public CompoundTag getWrappedEntityData() {
        return wrappedEntityData;
    }

    public void setWrappedBlock(@Nullable BlockState state, @Nullable CompoundTag entityData) {
        this.wrappedState = state;
        this.wrappedEntityData = entityData;
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (wrappedState != null) {
            output.store("wrapped_state", CompoundTag.CODEC, NbtUtils.writeBlockState(wrappedState));
        }
        if (wrappedEntityData != null) {
            output.store("wrapped_entity", CompoundTag.CODEC, wrappedEntityData);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        wrappedEntityData = input.read("wrapped_entity", CompoundTag.CODEC).orElse(null);
        // wrappedState is loaded lazily in onLoad() once the level is available
        input.read("wrapped_state", CompoundTag.CODEC).ifPresent(tag -> wrappedStateTag = tag);
    }

    // Temporary storage for the state tag until the level is available in onLoad()
    @Nullable
    private CompoundTag wrappedStateTag;

    @Override
    public void onLoad() {
        super.onLoad();
        if (wrappedStateTag != null && level != null) {
            wrappedState = NbtUtils.readBlockState(
                    level.holderLookup(net.minecraft.core.registries.Registries.BLOCK), wrappedStateTag);
            wrappedStateTag = null;
        }
    }
}
