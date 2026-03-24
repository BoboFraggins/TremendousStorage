package net.bobofraggins.intellistore.external.jei;

/**
 * Marker recipe object for the JEI Healing Salve cauldron recipe guide.
 *
 * <p>There is exactly one instance: {@link #INSTANCE}. The category
 * {@link HealingSalveCauldronCategory} uses it solely as a data carrier for the single "heal a
 * Zombie Brain in a Healing Salve Cauldron" guide entry.
 */
public final class HealingSalveCauldronJeiRecipe {

    public static final HealingSalveCauldronJeiRecipe INSTANCE = new HealingSalveCauldronJeiRecipe();

    private HealingSalveCauldronJeiRecipe() {}
}
