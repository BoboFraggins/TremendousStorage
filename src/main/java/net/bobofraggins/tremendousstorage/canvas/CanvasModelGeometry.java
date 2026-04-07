package net.bobofraggins.tremendousstorage.canvas;

import java.util.function.Function;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;

/** Unbaked geometry for canvas blocks. Bakes a {@link CanvasBakedModel} from the 16 connection textures. */
public class CanvasModelGeometry implements IUnbakedGeometry<CanvasModelGeometry> {

    @Override
    public BakedModel bake(
            IGeometryBakingContext context,
            ModelBaker baker,
            Function<Material, TextureAtlasSprite> spriteGetter,
            ModelState modelState,
            ItemOverrides overrides) {
        TextureAtlasSprite[] sprites = new TextureAtlasSprite[16];
        for (int mask = 0; mask < 16; mask++) {
            sprites[mask] = spriteGetter.apply(context.getMaterial(CanvasBakedModel.maskToName(mask)));
        }
        TextureAtlasSprite particle = spriteGetter.apply(context.getMaterial("particle"));
        return new CanvasBakedModel(sprites, particle);
    }
}
