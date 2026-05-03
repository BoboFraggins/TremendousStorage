package net.bobofraggins.tremendousstorage.external.jei;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Data carrier for a tank right-click extraction recipe shown in JEI.
 *
 * <p>Two instances are registered:
 * <ul>
 *   <li>Glass Bottle + XP Juice Tank → Bottle o' Enchanting (250 mB)
 *   <li>Empty Bucket + XP Juice Tank → XP Juice Bucket (1,000 mB)
 * </ul>
 */
public record TankExtractionJeiRecipe(ItemStack tank, ItemStack container, ItemStack result) {

    public static TankExtractionJeiRecipe bottleOEnchanting() {
        return new TankExtractionJeiRecipe(
                XpJuiceTankItem.create(), new ItemStack(Items.GLASS_BOTTLE), new ItemStack(Items.EXPERIENCE_BOTTLE));
    }

    public static TankExtractionJeiRecipe xpJuiceBucket() {
        return new TankExtractionJeiRecipe(
                XpJuiceTankItem.create(),
                new ItemStack(Items.BUCKET),
                new ItemStack(Registration.XP_JUICE_BUCKET.get()));
    }
}
