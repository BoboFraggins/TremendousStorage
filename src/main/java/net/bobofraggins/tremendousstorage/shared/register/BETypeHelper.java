package net.bobofraggins.tremendousstorage.shared.register;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;

/** Breaks the Registration↔BlockEntity circular dependency by providing registry-based lookups. */
public final class BETypeHelper {
    private BETypeHelper() {}

    public static BlockEntityType<?> get(String key) {
        return BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(Identifier.fromNamespaceAndPath("tremendousstorage", key));
    }

    public static Fluid getFluid(String key) {
        return BuiltInRegistries.FLUID.getValue(Identifier.fromNamespaceAndPath("tremendousstorage", key));
    }

    @SuppressWarnings("unchecked")
    public static <T> DataComponentType<T> getDataComponentType(String key) {
        return (DataComponentType<T>) BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(
                Identifier.fromNamespaceAndPath("tremendousstorage", key));
    }
}
