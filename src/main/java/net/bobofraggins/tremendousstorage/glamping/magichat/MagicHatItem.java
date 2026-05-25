package net.bobofraggins.tremendousstorage.glamping.magichat;

import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * The Magic Hat item — right-click a mob to capture it, right-click again to release.
 *
 * <p>All entity data (health, effects, profession, age, equipment, …) is preserved via
 * a full {@link Entity#save(CompoundTag)} round-trip stored in {@link DataComponents#CUSTOM_DATA}.
 *
 * <p>When the hat contains a mob, right-clicking on a block face or in air releases it ~1.5
 * blocks in front of the player. Block placement is always suppressed.
 */
public class MagicHatItem extends BlockItem {

    static final String MOB_KEY = "CapturedMob";

    static final Identifier LUCK_MODIFIER_ID =
            Identifier.fromNamespaceAndPath("tremendousstorage", "lucky_magic_hat_luck");

    public static final ItemAttributeModifiers DEFAULT_MODIFIERS = ItemAttributeModifiers.builder()
            .add(
                    Attributes.LUCK,
                    new AttributeModifier(LUCK_MODIFIER_ID, 2.0, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.HEAD)
            .build();

    public MagicHatItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    // -------------------------------------------------------------------------
    // Capture — right-click directly on a living entity
    // -------------------------------------------------------------------------

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (hasMob(stack)) return InteractionResult.PASS;
        if (target instanceof Player) return InteractionResult.PASS;
        if (!target.isAlive()) return InteractionResult.PASS;

        if (!player.level().isClientSide()) {
            TagValueOutput _tagOut = TagValueOutput.createWithContext(
                    ProblemReporter.DISCARDING, player.level().registryAccess());
            if (!target.save(_tagOut)) return InteractionResult.PASS;
            CompoundTag mobTag = _tagOut.buildResult();
            CompoundTag wrapper = new CompoundTag();
            wrapper.put(MOB_KEY, mobTag);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(wrapper));
            player.setItemInHand(hand, stack);
            target.discard();
            player.level()
                    .playSound(
                            null,
                            target.getX(),
                            target.getY(),
                            target.getZ(),
                            SoundEvents.ILLUSIONER_CAST_SPELL,
                            SoundSource.NEUTRAL,
                            1.0f,
                            1.0f);
        }
        return InteractionResult.SUCCESS;
    }

    // -------------------------------------------------------------------------
    // Release — right-click on a block face or in air
    // -------------------------------------------------------------------------

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (hasMob(ctx.getItemInHand())) {
            if (!ctx.getLevel().isClientSide()) {
                releaseMob(ctx.getItemInHand(), ctx.getPlayer(), ctx.getLevel());
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hasMob(stack)) {
            if (!level.isClientSide()) {
                releaseMob(stack, player, level);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private static void releaseMob(ItemStack stack, Player player, Level level) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return;
        CompoundTag wrapper = customData.copyTag();
        if (!wrapper.contains(MOB_KEY)) return;
        CompoundTag mobTag = wrapper.getCompoundOrEmpty(MOB_KEY);

        Vec3 look = player.getLookAngle();
        double spawnX = player.getX() + look.x * 1.5;
        double spawnY = player.getY();
        double spawnZ = player.getZ() + look.z * 1.5;

        Entity entity = EntityType.loadEntityRecursive(mobTag, serverLevel, EntitySpawnReason.LOAD, e -> {
            e.setPos(spawnX, spawnY, spawnZ);
            e.setDeltaMovement(Vec3.ZERO);
            return e;
        });
        if (entity != null) {
            serverLevel.addFreshEntityWithPassengers(entity);
            serverLevel.playSound(
                    null, spawnX, spawnY, spawnZ, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.NEUTRAL, 1.0f, 1.0f);
        }

        // Clear mob data from the item
        wrapper.remove(MOB_KEY);
        if (wrapper.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(wrapper));
        }
    }

    // -------------------------------------------------------------------------
    // Display — item name includes the captured mob type
    // -------------------------------------------------------------------------

    @Override
    public Component getName(ItemStack stack) {
        Component base = super.getName(stack);
        if (!hasMob(stack)) return base;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return base;
        Optional<EntityType<?>> type =
                data.copyTag().getCompoundOrEmpty(MOB_KEY).getString("id").flatMap(EntityType::byString);
        return type.map(t -> (Component) Component.empty()
                        .append(base)
                        .append(" (")
                        .append(t.getDescription())
                        .append(")"))
                .orElse(base);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay lines,
            Consumer<Component> tooltipAdder,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, tooltipAdder, flag);
        if (hasMob(stack)) {
            tooltipAdder.accept(Component.translatable("item.tremendousstorage.magic_hat.tooltip_release"));
        } else {
            tooltipAdder.accept(Component.translatable("item.tremendousstorage.magic_hat.tooltip_capture"));
        }
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    public static boolean hasMob(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(MOB_KEY);
    }
}
