package net.bobofraggins.tremendousstorage.shared.storage;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Item tint source that returns the color of the {@link StorageTier} stored in the item's
 * block-entity data component. Used to tint item icons in inventory and GUI.
 */
public record StorageTierTintSource() implements ItemTintSource {

    public static final MapCodec<StorageTierTintSource> MAP_CODEC = MapCodec.unit(new StorageTierTintSource());

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity) {
        var customData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTagWithoutId();
            if (tag.contains("Tier")) {
                return StorageTier.fromId(tag.getStringOr("Tier", "")).getColor();
            }
        }
        return StorageTier.WOOD.getColor();
    }

    @Override
    public MapCodec<StorageTierTintSource> type() {
        return MAP_CODEC;
    }
}
