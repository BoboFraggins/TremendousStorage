package net.bobofraggins.tremendousstorage.glamping.magichat;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/**
 * The Magic Hat item — right-click a mob to capture it, right-click again to release.
 *
 * <p>All entity data (health, effects, profession, age, equipment, …) is preserved via
 * a full {@link Entity#save(CompoundTag)} round-trip stored in {@link DataComponents#CUSTOM_DATA}.
 *
 * <p>When the hat contains a mob, right-clicking on a block face or in air releases it ~1.5
 * blocks in front of the player. Block placement is suppressed while a mob is stored.
 */
public class MagicHatItem extends BlockItem implements Equipable {

    private static final String MOB_KEY = "CapturedMob";

    static final ResourceLocation LUCK_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "lucky_magic_hat_luck");

    private static final ItemAttributeModifiers DEFAULT_MODIFIERS = ItemAttributeModifiers.builder()
            .add(
                    Attributes.LUCK,
                    new AttributeModifier(LUCK_MODIFIER_ID, 2.0, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.HEAD)
            .build();

    public MagicHatItem(Block block) {
        super(block, new Item.Properties().stacksTo(1));
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers() {
        return DEFAULT_MODIFIERS;
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
            CompoundTag mobTag = new CompoundTag();
            if (!target.save(mobTag)) return InteractionResult.PASS; // entity refused save
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
        return InteractionResult.sidedSuccess(player.level().isClientSide());
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
            return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide());
        }
        return super.useOn(ctx);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hasMob(stack)) {
            if (!level.isClientSide()) {
                releaseMob(stack, player, level);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        // No mob stored — equip to (or swap with) the vanilla head slot.
        return this.swapWithEquipmentSlot(this, level, player, hand);
    }

    private static void releaseMob(ItemStack stack, Player player, Level level) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return;
        CompoundTag wrapper = customData.copyTag();
        if (!wrapper.contains(MOB_KEY)) return;
        CompoundTag mobTag = wrapper.getCompound(MOB_KEY);

        Vec3 look = player.getLookAngle();
        double spawnX = player.getX() + look.x * 1.5;
        double spawnY = player.getY();
        double spawnZ = player.getZ() + look.z * 1.5;

        Entity entity = EntityType.loadEntityRecursive(mobTag, serverLevel, e -> {
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
        Optional<EntityType<?>> type = EntityType.by(data.copyTag().getCompound(MOB_KEY));
        return type.map(t -> (Component) Component.empty()
                        .append(base)
                        .append(" (")
                        .append(t.getDescription())
                        .append(")"))
                .orElse(base);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        if (hasMob(stack)) {
            lines.add(Component.translatable("item.tremendousstorage.magic_hat.tooltip_release"));
        } else {
            lines.add(Component.translatable("item.tremendousstorage.magic_hat.tooltip_capture"));
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
