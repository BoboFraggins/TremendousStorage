package net.bobofraggins.tremendousstorage.shared.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;

public final class StorageTooltip {

    private static final int MAX_LINES = 5;

    private StorageTooltip() {}

    public record Entry(ItemStack item, long count) {}

    /** Reads the {@code Types} list from {@code BLOCK_ENTITY_DATA} and appends item lines. */
    public static void appendBlockEntityItems(
            ItemStack stack, Consumer<Component> tooltipAdder, TooltipContext context) {
        var data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) return;
        CompoundTag tag = data.copyTagWithoutId();
        if (!tag.contains("Types")) return;
        ListTag types = tag.getListOrEmpty("Types");
        net.minecraft.resources.RegistryOps<net.minecraft.nbt.Tag> ops =
                context.registries().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);

        List<Entry> entries = new ArrayList<>(types.size());
        for (int i = 0; i < types.size(); i++) {
            CompoundTag e = types.getCompoundOrEmpty(i);
            long count = e.getLongOr("Count", 0L);
            if (count <= 0) continue;
            java.util.Optional<ItemStack> opt = e.getCompound("Type")
                    .flatMap(typeTag ->
                            ItemStack.OPTIONAL_CODEC.parse(ops, typeTag).result());
            opt.filter(item -> !item.isEmpty()).ifPresent(item -> entries.add(new Entry(item, count)));
        }
        appendSorted(entries, tooltipAdder);
    }

    /** Sorts entries descending by count and appends up to {@value MAX_LINES} lines, then {@code ...} if truncated. */
    public static void appendSorted(List<Entry> entries, Consumer<Component> tooltipAdder) {
        if (entries.isEmpty()) return;
        entries.sort(Comparator.comparingLong(Entry::count).reversed());
        int shown = Math.min(entries.size(), MAX_LINES);
        for (int i = 0; i < shown; i++) {
            Entry e = entries.get(i);
            tooltipAdder.accept(Component.literal(CountFormat.format(e.count()) + " "
                            + e.item().getHoverName().getString())
                    .withStyle(ChatFormatting.GRAY));
        }
        if (entries.size() > MAX_LINES) {
            tooltipAdder.accept(Component.literal("...").withStyle(ChatFormatting.GRAY));
        }
    }
}
