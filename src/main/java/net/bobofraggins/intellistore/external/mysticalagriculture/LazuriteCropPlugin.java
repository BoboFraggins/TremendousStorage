package net.bobofraggins.intellistore.external.mysticalagriculture;

import com.blakebr0.mysticalagriculture.api.IMysticalAgriculturePlugin;
import com.blakebr0.mysticalagriculture.api.MysticalAgriculturePlugin;
import com.blakebr0.mysticalagriculture.api.crop.Crop;
import com.blakebr0.mysticalagriculture.api.crop.CropTier;
import com.blakebr0.mysticalagriculture.api.crop.CropType;
import com.blakebr0.mysticalagriculture.api.lib.LazyIngredient;
import com.blakebr0.mysticalagriculture.api.registry.ICropRegistry;
import net.bobofraggins.intellistore.IntelliStore;
import net.minecraft.resources.ResourceLocation;

/**
 * Registers the Lazurite crop with Mystical Agriculture via its plugin system.
 *
 * <p>Discovered automatically by MA's annotation scanner ({@code ModFileScanData});
 * no manual wiring in {@link net.bobofraggins.intellistore.shared.register.Registration} is needed.
 * MA auto-registers the crop block, {@code intellistore:lazurite_essence}, and
 * {@code intellistore:lazurite_seeds} items, which resolve to our model/texture files.
 */
@MysticalAgriculturePlugin
public class LazuriteCropPlugin implements IMysticalAgriculturePlugin {

    @Override
    public void onRegisterCrops(ICropRegistry registry) {
        Crop lazurite = new Crop(
                ResourceLocation.fromNamespaceAndPath(IntelliStore.MODID, "lazurite"),
                CropTier.THREE,
                CropType.RESOURCE,
                LazyIngredient.item("intellistore:lazurite_ingot"));
        // Mid-tone blue from the lazurite ingot palette
        lazurite.setColor(0x2E558A);
        registry.register(lazurite);
    }
}
