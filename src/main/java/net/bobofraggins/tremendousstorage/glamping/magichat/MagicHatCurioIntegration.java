package net.bobofraggins.tremendousstorage.glamping.magichat;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** Curio behaviour for the Lucky Magic Hat — grants +2 Luck while worn. */
public class MagicHatCurioIntegration implements ICurioItem {

    public static final MagicHatCurioIntegration INSTANCE = new MagicHatCurioIntegration();

    private static final Identifier LUCK_MODIFIER_ID =
            Identifier.fromNamespaceAndPath("tremendousstorage", "lucky_magic_hat_luck");

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
            SlotContext slotContext, Identifier id, ItemStack stack) {
        return ImmutableMultimap.of(
                Attributes.LUCK, new AttributeModifier(LUCK_MODIFIER_ID, 2.0, AttributeModifier.Operation.ADD_VALUE));
    }
}
