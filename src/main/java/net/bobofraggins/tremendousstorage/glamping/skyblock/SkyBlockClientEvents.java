package net.bobofraggins.tremendousstorage.glamping.skyblock;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterNamedRenderTypesEvent;

public class SkyBlockClientEvents {

    private SkyBlockClientEvents() {}

    @SubscribeEvent
    public static void registerRenderTypes(RegisterNamedRenderTypesEvent event) {
        // NeoForge 21.1.224+ requires the block render type to be a chunk buffer layer.
        // The original depth-only render type is no longer allowed here; cutout is the
        // closest standard type (transparent texture pixels will be discarded).
        ResourceLocation skyBlockTexture =
                ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "textures/block/sky_block.png");
        event.register(
                ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "sky_block"),
                RenderType.cutout(),
                RenderType.entityCutout(skyBlockTexture));
    }
}
