package net.bobofraggins.tremendousstorage.external.jade;

import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;

public enum PresentJadeComponentProvider implements IComponentProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public Identifier getUid() {
        return PresentJadeDataProvider.UID;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        Optional<String> blockId = data.getString("WrappedBlock");
        if (blockId.isEmpty()) return;

        Identifier rl = Identifier.tryParse(blockId.get());
        if (rl == null) return;

        net.minecraft.world.level.block.Block block = BuiltInRegistries.BLOCK.getValue(rl);
        Item item = block.asItem();
        Component wrappedName = item == Items.AIR ? block.getName() : item.getName(new ItemStack(item));

        Component baseName = Component.translatable(accessor.getBlock().getDescriptionId());
        tooltip.remove(JadeIds.MC_BLOCK_DISPLAY);
        tooltip.add(
                0,
                Component.empty()
                        .append(baseName)
                        .append(" (")
                        .append(wrappedName)
                        .append(")"));
    }
}
