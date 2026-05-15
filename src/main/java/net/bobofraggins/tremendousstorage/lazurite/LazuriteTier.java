package net.bobofraggins.tremendousstorage.lazurite;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

public final class LazuriteTier {

    private LazuriteTier() {}

    public static final TagKey<Item> LAZURITE_MATERIALS = TagKey.create(
            Registries.ITEM, ResourceLocation.fromNamespaceAndPath("tremendousstorage", "lazurite_tool_materials"));

    public static final ToolMaterial INSTANCE =
            new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 1024, 4.0f, 4.0f, 14, LAZURITE_MATERIALS);

    public static final ToolMaterial PAXEL =
            new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 2048, 4.0f, 4.0f, 14, LAZURITE_MATERIALS);
}
