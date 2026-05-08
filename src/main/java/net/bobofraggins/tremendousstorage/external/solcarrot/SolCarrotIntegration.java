package net.bobofraggins.tremendousstorage.external.solcarrot;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Set;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.attachment.AttachmentType;

/**
 * Optional integration with Spice of Life: Carrot Edition.
 *
 * <p>All access to SoLC classes is via reflection so no compile-time dependency is needed.
 * Every public method is safe to call regardless of whether the mod is installed.
 */
public final class SolCarrotIntegration {

    private SolCarrotIntegration() {}

    private static boolean initialized = false;
    private static AttachmentType<?> foodAttachment;
    private static Field foodsField;
    private static Constructor<?> foodInstanceCtor;

    @SuppressWarnings("unchecked")
    private static void init() {
        if (initialized) return;
        initialized = true;
        if (!ModList.get().isLoaded("solcarrot")) return;
        try {
            Class<?> solCarrotClass = Class.forName("com.cazsius.solcarrot.SOLCarrot");
            Field attachmentField = solCarrotClass.getField("FOOD_ATTACHMENT");
            foodAttachment = (AttachmentType<?>) attachmentField.get(null);

            Class<?> foodListClass = Class.forName("com.cazsius.solcarrot.tracking.FoodList");
            foodsField = foodListClass.getDeclaredField("foods");
            foodsField.setAccessible(true);

            Class<?> foodInstanceClass = Class.forName("com.cazsius.solcarrot.tracking.FoodInstance");
            foodInstanceCtor = foodInstanceClass.getConstructor(Item.class);
        } catch (Exception e) {
            foodAttachment = null;
            foodsField = null;
            foodInstanceCtor = null;
        }
    }

    /** Returns true if SoLC is installed and its API is accessible. */
    public static boolean isInstalled() {
        init();
        return foodAttachment != null;
    }

    /**
     * Returns true if the player has not yet eaten this food according to SoLC.
     * Always returns false when SoLC is not installed.
     */
    @SuppressWarnings("unchecked")
    public static boolean isNewFood(Player player, ItemStack food) {
        init();
        if (foodAttachment == null || foodsField == null || foodInstanceCtor == null) return false;
        try {
            Object foodList = player.getData((AttachmentType<Object>) foodAttachment);
            Set<?> foods = (Set<?>) foodsField.get(foodList);
            Object fi = foodInstanceCtor.newInstance(food.getItem());
            return !foods.contains(fi);
        } catch (Exception e) {
            return false;
        }
    }
}
