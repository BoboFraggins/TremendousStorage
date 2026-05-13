package net.bobofraggins.tremendousstorage.shared.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;

public final class StorageTooltip {

    private static final int MAX_LINES = 5;

    private StorageTooltip() {}

    public record Entry(ItemStack item, long count) {}

    /** Reads the {@code Types} list from {@code BLOCK_ENTITY_DATA} and appends item lines. */
    public static void appendBlockEntityItems(ItemStack stack, List<Component> lines, TooltipContext context) {
        var data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) return;
        CompoundTag tag = data.copyTag();
        if (!tag.contains("Types")) return;
        ListTag types = tag.getList("Types", Tag.TAG_COMPOUND);

        List<Entry> entries = new ArrayList<>(types.size());
        for (int i = 0; i < types.size(); i++) {
            CompoundTag e = types.getCompound(i);
            long count = e.getLong("Count");
            if (count <= 0) continue;
            ItemStack.parse(context.registries(), e.getCompound("Type"))
                    .ifPresent(item -> entries.add(new Entry(item, count)));
        }
        appendSorted(entries, lines);
    }

    /** Sorts entries descending by count and appends up to {@value MAX_LINES} lines, then {@code ...} if truncated. */
    public static void appendSorted(List<Entry> entries, List<Component> lines) {
        if (entries.isEmpty()) return;
        entries.sort(Comparator.comparingLong(Entry::count).reversed());
        int shown = Math.min(entries.size(), MAX_LINES);
        for (int i = 0; i < shown; i++) {
            Entry e = entries.get(i);
            lines.add(Component.literal(CountFormat.format(e.count()) + " "
                            + e.item().getHoverName().getString())
                    .withStyle(ChatFormatting.GRAY));
        }
        if (entries.size() > MAX_LINES) {
            lines.add(Component.literal("...").withStyle(ChatFormatting.GRAY));
        }
    }
}
