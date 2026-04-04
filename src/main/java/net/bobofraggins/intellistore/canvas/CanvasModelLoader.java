package net.bobofraggins.intellistore.canvas;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;

/** Geometry loader for the canvas connected-texture block model. */
public class CanvasModelLoader implements IGeometryLoader<CanvasModelGeometry> {

    public static final CanvasModelLoader INSTANCE = new CanvasModelLoader();

    private CanvasModelLoader() {}

    @Override
    public CanvasModelGeometry read(JsonObject jsonObject, JsonDeserializationContext context) {
        return new CanvasModelGeometry();
    }
}
