package net.bobofraggins.tremendousstorage.storage.endertank;

import java.util.function.Consumer;
import net.bobofraggins.tremendousstorage.shared.storage.TieredBlockItem;
import net.bobofraggins.tremendousstorage.storage.tank.TankItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/**
 * Block item for the Ender Tank. Extends {@link TankItem} to inherit
 * fluid tooltip and bucket interaction.
 */
public class EnderTankItem extends TankItem {

    public EnderTankItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Component base = Component.translatable(getDescriptionId());
        String suffix = TieredBlockItem.buildBedSuffix(stack, false);
        return suffix.isEmpty() ? base : Component.empty().append(base).append(suffix);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay lines,
            Consumer<Component> tooltipAdder,
            TooltipFlag flag) {
        // Call grandparent (TieredBlockItem) tooltip which does nothing, then add fluid line ourselves.
        // This avoids calling super which would call TieredBlockItem's getName (already handled above).
        // Actually just call super — the fluid tooltip line is added independently of the name.
        super.appendHoverText(stack, context, lines, tooltipAdder, flag);
        long linkId = linkIdFromStack(stack);
        if (linkId != -1L) {
            tooltipAdder.accept(Component.translatable(
                    "item.tremendousstorage.ender_tank.linked",
                    String.format("%016X", linkId).substring(12)));
        }
    }

    private static long linkIdFromStack(ItemStack stack) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) return -1L;
        CompoundTag tag = data.copyTag();
        return tag.getLongOr(EnderTankBlockEntity.TAG_LINK_ID, -1L);
    }
}
