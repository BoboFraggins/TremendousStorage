package net.bobofraggins.tremendousstorage.storage.endertank;

import com.mojang.serialization.MapCodec;
import net.bobofraggins.tremendousstorage.shared.recipe.AbstractEnderCraftingRecipe;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * Crafting recipe: Tremendous Tank (any tier) + Ender Storage Upgrade →
 * two linked Ender Tremendous Tanks sharing a freshly generated 64-bit {@code linkId}.
 *
 * <p>The BED (tier, voidExcess) and the {@link Registration#TANK_CONTENTS} component
 * are copied from the input tank to both outputs. The second tank is returned via
 * {@link #getRemainingItems} in the base slot, as with vanilla crafting remainder items.
 */
public class EnderTankCraftingRecipe extends AbstractEnderCraftingRecipe {

    private static final EnderTankCraftingRecipe INSTANCE = new EnderTankCraftingRecipe();

    public static final MapCodec<EnderTankCraftingRecipe> CODEC = MapCodec.unit(INSTANCE);

    public static final StreamCodec<RegistryFriendlyByteBuf, EnderTankCraftingRecipe> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return stack.getItem() == Registration.TANK_ITEM.get() || stack.getItem() == Registration.ENDER_TANK_ITEM.get();
    }

    @Override
    protected boolean isAlreadyEnder(ItemStack stack) {
        return stack.getItem() == Registration.ENDER_TANK_ITEM.get();
    }

    @Override
    protected long getExistingLinkId(ItemStack stack) {
        var existing = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (existing == null) return -1L;
        CompoundTag tag = existing.getUnsafe();
        return tag.getLongOr(EnderTankBlockEntity.TAG_LINK_ID, -1L);
    }

    @Override
    protected ItemStack makeEnderItem(ItemStack base, long linkId) {
        ItemStack result = new ItemStack(Registration.ENDER_TANK_ITEM.get());
        var existing = base.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag tag = existing != null ? existing.getUnsafe() : new CompoundTag();
        tag.putLong(EnderTankBlockEntity.TAG_LINK_ID, linkId);
        TagValueOutput _tagOut = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        _tagOut.store(tag);
        BlockItem.setBlockEntityData(result, Registration.ENDER_TANK_BE_TYPE.get(), _tagOut);
        var contents = base.get(Registration.TANK_CONTENTS.get());
        if (contents != null) result.set(Registration.TANK_CONTENTS.get(), contents);
        return result;
    }

    public ItemStack getResultItem(net.minecraft.core.HolderLookup.Provider registries) {
        return new ItemStack(Registration.ENDER_TANK_ITEM.get());
    }

    @Override
    public RecipeSerializer<? extends CraftingRecipe> getSerializer() {
        return Registration.ENDER_TANK_CRAFTING_RECIPE.get();
    }
}
