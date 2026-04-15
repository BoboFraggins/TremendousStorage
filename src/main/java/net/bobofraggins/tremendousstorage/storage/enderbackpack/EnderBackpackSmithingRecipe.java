package net.bobofraggins.tremendousstorage.storage.enderbackpack;

import com.mojang.serialization.MapCodec;
import net.bobofraggins.tremendousstorage.shared.recipe.AbstractEnderSmithingRecipe;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackContents;
import net.bobofraggins.tremendousstorage.storage.enderchest.EnderChestBlockEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Smithing-table recipe: Tremendous Backpack (any tier) + Ender Storage Upgrade →
 * two linked Ender Tremendous Backpacks sharing a freshly generated 64-bit {@code linkId}.
 *
 * <p>All data (tier, priority, sort mode, crafting-upgrade flag, inventory) is copied from the
 * input backpack to both outputs via the {@link BackpackContents} data component.
 * The second backpack is returned via {@link #getRemainingItems} in the base slot.
 */
public class EnderBackpackSmithingRecipe extends AbstractEnderSmithingRecipe {

    private static final EnderBackpackSmithingRecipe INSTANCE = new EnderBackpackSmithingRecipe();

    public static final MapCodec<EnderBackpackSmithingRecipe> CODEC = MapCodec.unit(INSTANCE);

    public static final StreamCodec<RegistryFriendlyByteBuf, EnderBackpackSmithingRecipe> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return stack.getItem() == Registration.TREMENDOUS_BACKPACK.get()
                || stack.getItem() == Registration.ENDER_TREMENDOUS_BACKPACK_ITEM.get();
    }

    @Override
    protected boolean isAlreadyEnder(ItemStack stack) {
        return stack.getItem() == Registration.ENDER_TREMENDOUS_BACKPACK_ITEM.get();
    }

    @Override
    protected long getExistingLinkId(ItemStack stack) {
        Long id = stack.get(Registration.ENDER_LINK_ID.get());
        return id != null ? id : -1L;
    }

    @Override
    protected ItemStack makeEnderItem(ItemStack base, long linkId) {
        ItemStack result = new ItemStack(Registration.ENDER_TREMENDOUS_BACKPACK_ITEM.get());

        // Copy BackpackContents (item-form inventory, tier, priority, etc.)
        BackpackContents contents = base.get(Registration.TREMENDOUS_BACKPACK_CONTENTS.get());
        if (contents != null) {
            result.set(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), contents);
        }

        // Copy or create block_entity_data (for when the ender backpack is placed as a block)
        CustomData existing = base.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag tag = existing != null ? existing.copyTag() : new CompoundTag();
        // If no block_entity_data but BackpackContents is present, seed it
        if (existing == null && contents != null) {
            tag.putString("Tier", contents.tier().getId());
            tag.putInt("Priority", contents.priority().ordinal());
            tag.putString("SortMode", contents.sortMode().name());
            if (contents.hasCraftingUpgrade()) tag.putBoolean("CraftingUpgrade", true);
        }
        tag.putLong(EnderChestBlockEntity.TAG_LINK_ID, linkId);
        result.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));

        // Set link ID as a standalone component for fast access in item-form openUi
        result.set(Registration.ENDER_LINK_ID.get(), linkId);

        return result;
    }

    @Override
    public ItemStack getResultItem(net.minecraft.core.HolderLookup.Provider registries) {
        return new ItemStack(Registration.ENDER_TREMENDOUS_BACKPACK_ITEM.get());
    }

    @Override
    protected ItemStack makeSecondEnderItem(ItemStack second) {
        // Strip instance upgrade flags from BLOCK_ENTITY_DATA (crafting, magnet, puller).
        second = super.makeSecondEnderItem(second);
        // Also strip from the BackpackContents item component, which mirrors the crafting flag.
        BackpackContents contents = second.get(Registration.TREMENDOUS_BACKPACK_CONTENTS.get());
        if (contents != null && contents.hasCraftingUpgrade()) {
            second.set(
                    Registration.TREMENDOUS_BACKPACK_CONTENTS.get(),
                    new BackpackContents(
                            contents.entries(), contents.tier(), contents.priority(), contents.sortMode(), false));
        }
        return second;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Registration.ENDER_BACKPACK_SMITHING_RECIPE.get();
    }
}
