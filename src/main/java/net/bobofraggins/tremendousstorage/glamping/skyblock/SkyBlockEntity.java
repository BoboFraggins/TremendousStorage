package net.bobofraggins.tremendousstorage.glamping.skyblock;

import net.bobofraggins.tremendousstorage.glamping.GlampingRegistration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SkyBlockEntity extends BlockEntity {

    public SkyBlockEntity(BlockPos pos, BlockState state) {
        super(GlampingRegistration.SKY_BLOCK_BE_TYPE.get(), pos, state);
    }
}
