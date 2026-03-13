package net.bobofraggins.intellistore.lazuriteore;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class LazuriteOreBlock extends DropExperienceBlock {

    public LazuriteOreBlock(BlockBehaviour.Properties properties) {
        super(UniformInt.of(2, 5), properties);
    }
}
