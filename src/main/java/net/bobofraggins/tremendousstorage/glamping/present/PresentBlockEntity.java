package net.bobofraggins.tremendousstorage.glamping.present;

import net.bobofraggins.tremendousstorage.shared.register.BETypeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PresentBlockEntity extends BlockEntity {

    public PresentBlockEntity(BlockPos pos, BlockState state) {
        super(BETypeHelper.get("present"), pos, state);
    }
}
