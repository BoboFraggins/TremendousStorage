package net.bobofraggins.tremendousstorage.storage.items;

import java.util.function.Consumer;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * Drinkable potion that applies the {@link VexRepellentEffect}.
 * Duration is supplied at construction time; three tiers are registered in
 * {@link Registration}: base (1 min), extended (3 min), and long (8 min).
 */
public class VexRepellentPotionItem extends Item {

    private final int duration;

    public VexRepellentPotionItem(int duration) {
        super(new Item.Properties().stacksTo(1));
        this.duration = duration;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
        }
        if (!level.isClientSide()) {
            entity.addEffect(new MobEffectInstance(Registration.VEX_REPELLENT_EFFECT, duration, 0, false, true, true));
        }
        if (entity instanceof Player player) {
            player.awardStat(Stats.ITEM_USED.get(this));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    return new ItemStack(Items.GLASS_BOTTLE);
                }
                player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE));
            }
        }
        return stack;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay tooltip,
            Consumer<Component> tooltipAdder,
            TooltipFlag flag) {
        int totalSeconds = duration / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        tooltipAdder.accept(Component.translatable("effect.tremendousstorage.vex_repellent")
                .withStyle(ChatFormatting.BLUE)
                .append(Component.literal(String.format(" (%d:%02d)", minutes, seconds))
                        .withStyle(ChatFormatting.DARK_GRAY)));
    }
}
