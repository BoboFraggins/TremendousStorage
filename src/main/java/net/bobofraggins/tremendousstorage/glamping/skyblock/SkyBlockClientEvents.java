package net.bobofraggins.tremendousstorage.glamping.skyblock;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterNamedRenderTypesEvent;

public class SkyBlockClientEvents {

    private SkyBlockClientEvents() {}

    @SubscribeEvent
    public static void registerRenderTypes(RegisterNamedRenderTypesEvent event) {
        // Writes only to the depth buffer.  The sky/void background that the engine
        // renders before terrain shows through at these pixels; the depth value prevents
        // any geometry behind the block from appearing.
        RenderType depthOnly = RenderType.create(
                "tremendousstorage:sky_block",
                DefaultVertexFormat.BLOCK,
                VertexFormat.Mode.QUADS,
                2097152,
                false,
                false,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeSolidShader))
                        .setTextureState(RenderStateShard.BLOCK_SHEET_MIPPED)
                        .setWriteMaskState(RenderStateShard.DEPTH_WRITE)
                        .setLightmapState(RenderStateShard.LIGHTMAP)
                        .createCompositeState(true));

        event.register(
                ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "sky_block"), depthOnly, depthOnly);
    }
}
