package net.bobofraggins.tremendousstorage.experiencesyringe;

import java.util.List;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class ExperienceSyringeItem extends Item {

    /** XP points stored per bucket of XP fluid (1 bucket = 1 000 mB = 50 XP points). */
    public static final int XP_PER_BUCKET = 50;
    /** Total capacity in XP points (32 buckets). */
    public static final int CAPACITY = 32 * XP_PER_BUCKET; // 1 600
    /** Converts XP points to mB for fluid system display (1 XP = 20 mB). */
    public static int xpToMb(int xp) {
        return xp * 1000 / XP_PER_BUCKET;
    }
    /** Converts mB to XP points (truncates). */
    public static int mbToXp(int mb) {
        return mb * XP_PER_BUCKET / 1000;
    }

    public ExperienceSyringeItem() {
        super(new Properties().stacksTo(1));
    }

    /**
     * Right-click on air: withdraw enough XP from the syringe to bring the player to their next level.
     * Shift + right-click on air: store the player's current-level progress into the syringe,
     * leaving them at exactly the start of their current level.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        int stored = stack.getOrDefault(Registration.EXPERIENCE_SYRINGE_STORED_XP, 0);

        if (player.isShiftKeyDown()) {
            // Deposit: extract current-level progress, or a full level if at a boundary.
            int progressXp = Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
            if (progressXp == 0 && player.experienceLevel > 0) {
                // At an exact level boundary — extract the full previous level's worth of XP.
                progressXp = xpNeededForLevel(player.experienceLevel - 1);
            }
            int toStore = Math.min(progressXp, CAPACITY - stored);
            if (toStore > 0) {
                player.giveExperiencePoints(-toStore);
                stack.set(Registration.EXPERIENCE_SYRINGE_STORED_XP, stored + toStore);
            }
        } else {
            // Withdraw: give the player exactly enough XP to reach their next whole level.
            int progressXp = Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
            int neededForNext = player.getXpNeededForNextLevel() - progressXp;
            int toGive = Math.min(neededForNext, stored);
            if (toGive > 0) {
                player.giveExperiencePoints(toGive);
                stack.set(Registration.EXPERIENCE_SYRINGE_STORED_XP, stored - toGive);
            }
        }

        return InteractionResultHolder.success(stack);
    }

    /** XP required to advance from {@code level} to {@code level + 1}, matching vanilla formula. */
    private static int xpNeededForLevel(int level) {
        if (level >= 30) return 112 + (level - 30) * 9;
        if (level >= 16) return 37 + (level - 16) * 5;
        return 7 + level * 2;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int stored = stack.getOrDefault(Registration.EXPERIENCE_SYRINGE_STORED_XP, 0);
        int storedMb = xpToMb(stored);
        int capacityMb = xpToMb(CAPACITY);
        tooltip.add(Component.literal(storedMb + " / " + capacityMb + " mB  (" + stored + " XP)")
                .withStyle(stored == 0 ? ChatFormatting.DARK_GRAY : ChatFormatting.GREEN));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.getOrDefault(Registration.EXPERIENCE_SYRINGE_STORED_XP, 0) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int stored = stack.getOrDefault(Registration.EXPERIENCE_SYRINGE_STORED_XP, 0);
        return Math.round(13f * stored / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        // Neon green, matching the XP juice fluid tint.
        return 0x39FF14;
    }
}
