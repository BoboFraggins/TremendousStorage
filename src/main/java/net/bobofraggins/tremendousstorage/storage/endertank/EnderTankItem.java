package net.bobofraggins.tremendousstorage.storage.endertank;

import java.util.List;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.storage.tank.TankItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;

/**
 * Block item for the Ender Tank. Extends {@link TankItem} to inherit
 * fluid tooltip and bucket interaction, and appends "(Ender)" or "(Tier/Ender)" to the name.
 */
public class EnderTankItem extends TankItem {

    public EnderTankItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Component base = super.getName(stack);
        // super.getName already appended the tier suffix via TieredBlockItem.
        // We need to replace it with the ender suffix instead.
        // Rebuild from the raw block name translation.
        Component blockName = Component.translatable(getDescriptionId());
        StorageTier tier = tierFromStack(stack);
        if (tier == StorageTier.WOOD) {
            return Component.empty().append(blockName).append(" (Ender)");
        }
        return Component.empty().append(blockName).append(" (" + capitalize(tier.getId()) + "/Ender)");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        // Call grandparent (TieredBlockItem) tooltip which does nothing, then add fluid line ourselves.
        // This avoids calling super which would call TieredBlockItem's getName (already handled above).
        // Actually just call super — the fluid tooltip line is added independently of the name.
        super.appendHoverText(stack, context, lines, flag);
        long linkId = linkIdFromStack(stack);
        if (linkId != -1L) {
            lines.add(Component.translatable(
                    "item.tremendousstorage.ender_tank.linked",
                    String.format("%016X", linkId).substring(12)));
        }
    }

    private static StorageTier tierFromStack(ItemStack stack) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) return StorageTier.WOOD;
        return StorageTier.fromId(data.copyTag().getString("Tier"));
    }

    private static long linkIdFromStack(ItemStack stack) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) return -1L;
        CompoundTag tag = data.copyTag();
        return tag.contains(EnderTankBlockEntity.TAG_LINK_ID) ? tag.getLong(EnderTankBlockEntity.TAG_LINK_ID) : -1L;
    }

    private static String capitalize(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
