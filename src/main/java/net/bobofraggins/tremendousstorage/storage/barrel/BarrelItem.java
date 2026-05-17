package net.bobofraggins.tremendousstorage.storage.barrel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.TieredBlockItem;
import net.bobofraggins.tremendousstorage.shared.util.CountFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

public class BarrelItem extends TieredBlockItem {

    public BarrelItem(Block block, Properties properties) {
        super(block, properties);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay lines,
            Consumer<Component> tooltipAdder,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, tooltipAdder, flag);
        appendBarrelContents(stack, tooltipAdder);
    }

    public static void appendBarrelContents(ItemStack stack, Consumer<Component> tooltipAdder) {
        BarrelContents contents = stack.getOrDefault(Registration.BARREL_CONTENTS.get(), BarrelContents.EMPTY);
        if (!contents.isLocked() || contents.count() <= 0) return;
        tooltipAdder.accept(Component.literal(CountFormat.format(contents.count()) + " "
                        + contents.storedItem().get().getHoverName().getString())
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public Component getName(ItemStack stack) {
        Component base = super.getName(stack);
        String locked = buildLockedSuffix(stack);
        return locked.isEmpty() ? base : Component.empty().append(base).append(locked);
    }

    public static String buildLockedSuffix(ItemStack stack) {
        BarrelContents contents = stack.get(Registration.BARREL_CONTENTS.get());
        if (contents == null || !contents.isLocked()) return "";

        String baseName = contents.storedItem().get().getHoverName().getString();

        var bedData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (bedData != null) {
            var tag = bedData.copyTagWithoutId();
            if (tag.getBooleanOr("CompactingUpgrade", false)) {
                List<String> names = new ArrayList<>();
                names.add(baseName);
                addChainName(names, tag.getStringOr("Compact1Id", ""));
                addChainName(names, tag.getStringOr("Compact2Id", ""));
                return " [" + String.join(" → ", names) + "]";
            }
        }
        return " [" + baseName + "]";
    }

    private static void addChainName(List<String> names, String id) {
        if (id.isEmpty()) return;
        Identifier rl = Identifier.tryParse(id);
        if (rl == null) return;
        Item item = BuiltInRegistries.ITEM.get(rl).map(h -> h.value()).orElse(null);
        if (item != null && item != Items.AIR)
            names.add(new ItemStack(item).getHoverName().getString());
    }
}
