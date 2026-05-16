package net.bobofraggins.tremendousstorage.glamping;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public class GlampingDimension {

    public static final ResourceKey<Level> KEY = ResourceKey.create(
            Registries.DIMENSION, Identifier.fromNamespaceAndPath(TremendousStorage.MODID, "glamping"));

    public static final ResourceKey<DimensionType> TYPE_KEY = ResourceKey.create(
            Registries.DIMENSION_TYPE, Identifier.fromNamespaceAndPath(TremendousStorage.MODID, "glamping"));

    /** Each portal carves a 16x16x16 space for a camp. */
    public static final int CAMP_SIZE = 16;

    /** Y level of the bottom face of the carved camp space. */
    public static final int CAMP_BOTTOM_Y = 64;

    /**
     * Horizontal spacing between allocated camp origins.
     * Must be >= CAMP_SIZE to prevent camps from overlapping.
     */
    public static final int CAMP_SPACING = 32;
}
