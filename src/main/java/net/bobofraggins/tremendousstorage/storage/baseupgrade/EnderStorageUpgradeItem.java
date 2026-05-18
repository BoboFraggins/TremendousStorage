package net.bobofraggins.tremendousstorage.storage.baseupgrade;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackBlockEntity;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackContents;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlockEntity;
import net.bobofraggins.tremendousstorage.storage.enderchest.EnderChestBlockEntity;
import net.bobofraggins.tremendousstorage.storage.endertank.EnderTankBlockEntity;
import net.bobofraggins.tremendousstorage.storage.tank.TankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Upgrade item that converts a placed storage block to its linked Ender variant.
 *
 * <p>Right-clicking on a supported placed block transforms it in-place: the block type
 * changes to its ender equivalent, all existing data (inventory, tier, upgrades, fluids)
 * is preserved, and a second linked ender item is given to the player (or dropped nearby).
 * Both the placed block and the second item share the same freshly generated link ID so
 * their shared inventories stay in sync.
 *
 * <p>Supported targets: Tremendous Chest, Tremendous Backpack, Picnic Basket, Tremendous Tank.
 * Manila Folder and Wireless SAT have no block form and are handled via crafting table only.
 */
public class EnderStorageUpgradeItem extends BaseUpgradeItem {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public EnderStorageUpgradeItem(Item.Properties properties) {
        super(properties);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay tooltip,
            Consumer<Component> tooltipAdder,
            TooltipFlag flag) {
        tooltipAdder.accept(Component.translatable("item.tremendousstorage.ender_storage_upgrade.tooltip"));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Block block = level.getBlockState(pos).getBlock();

        boolean isTarget = block == Registration.TREMENDOUS_CHEST.get()
                || block == Registration.TREMENDOUS_BACKPACK_BLOCK.get()
                || block == Registration.PICNIC_BASKET_BLOCK.get()
                || block == Registration.TANK.get();

        if (!isTarget) return InteractionResult.PASS;

        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            ItemStack secondItem = null;

            if (block == Registration.TREMENDOUS_CHEST.get() && be instanceof ChestBlockEntity chest) {
                secondItem = applyEnderToChest(level, pos, chest);
            } else if (block == Registration.TREMENDOUS_BACKPACK_BLOCK.get()
                    && be instanceof BackpackBlockEntity backpack) {
                secondItem = applyEnderToBackpack(level, pos, backpack);
            } else if (block == Registration.PICNIC_BASKET_BLOCK.get() && be instanceof ChestBlockEntity basket) {
                secondItem = applyEnderToPicnicBasket(level, pos, basket);
            } else if (block == Registration.TANK.get() && be instanceof TankBlockEntity tank) {
                secondItem = applyEnderToTank(level, pos, tank);
            }

            if (secondItem != null) {
                Player player = ctx.getPlayer();
                if (player == null || !player.getInventory().add(secondItem)) {
                    double x = pos.getX() + 0.5;
                    double y = pos.getY() + 1.0;
                    double z = pos.getZ() + 0.5;
                    level.addFreshEntity(new ItemEntity(level, x, y, z, secondItem));
                }
                ctx.getItemInHand().shrink(1);
            }
        }

        return InteractionResult.SUCCESS;
    }

    // -------------------------------------------------------------------------
    // Per-type ender conversion helpers
    // -------------------------------------------------------------------------

    private static CompoundTag stripInstanceUpgrades(CompoundTag tag) {
        tag.remove("CraftingUpgrade");
        tag.remove("MagnetUpgrade");
        tag.remove("PullerUpgrade");
        tag.remove("PullerSides");
        return tag;
    }

    private static ItemStack applyEnderToChest(Level level, BlockPos pos, ChestBlockEntity chest) {
        long linkId = SECURE_RANDOM.nextLong();
        CompoundTag savedTag = chest.saveWithoutMetadata(level.registryAccess());
        savedTag.putLong(EnderChestBlockEntity.TAG_LINK_ID, linkId);

        level.setBlock(pos, Registration.ENDER_TREMENDOUS_CHEST.get().defaultBlockState(), Block.UPDATE_ALL);

        BlockEntity newBe = level.getBlockEntity(pos);
        if (newBe != null) {
            TypedEntityData.of(newBe.getType(), savedTag).loadInto(newBe, level.registryAccess());
            newBe.setChanged();
        }

        ItemStack second = new ItemStack(Registration.ENDER_TREMENDOUS_CHEST_ITEM.get());
        second.set(
                DataComponents.BLOCK_ENTITY_DATA,
                TypedEntityData.of(
                        Registration.ENDER_TREMENDOUS_CHEST_BE_TYPE.get(), stripInstanceUpgrades(savedTag.copy())));
        return second;
    }

    private static ItemStack applyEnderToBackpack(Level level, BlockPos pos, BackpackBlockEntity backpack) {
        long linkId = SECURE_RANDOM.nextLong();
        CompoundTag savedTag = backpack.saveWithoutMetadata(level.registryAccess());
        savedTag.putLong(EnderChestBlockEntity.TAG_LINK_ID, linkId);

        level.setBlock(pos, Registration.ENDER_TREMENDOUS_BACKPACK_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);

        BlockEntity newBe = level.getBlockEntity(pos);
        if (newBe != null) {
            TypedEntityData.of(newBe.getType(), savedTag).loadInto(newBe, level.registryAccess());
            newBe.setChanged();
        }

        // Build the BackpackContents component for the second item — crafting upgrade is instance-only,
        // so the second linked item gets hasCraftingUpgrade=false.
        List<BackpackContents.Entry> entries = new ArrayList<>();
        for (int i = 0; i < backpack.typeCount(); i++) {
            entries.add(new BackpackContents.Entry(backpack.getType(i), backpack.getCount(i)));
        }
        BackpackContents contents = new BackpackContents(
                entries, backpack.getTier(), backpack.getPriority(), backpack.getSortMode(), false);

        ItemStack second = new ItemStack(Registration.ENDER_TREMENDOUS_BACKPACK_ITEM.get());
        second.set(
                DataComponents.BLOCK_ENTITY_DATA,
                TypedEntityData.of(
                        Registration.ENDER_TREMENDOUS_BACKPACK_BE_TYPE.get(), stripInstanceUpgrades(savedTag.copy())));
        second.set(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), contents);
        second.set(Registration.ENDER_LINK_ID.get(), linkId);
        return second;
    }

    private static ItemStack applyEnderToPicnicBasket(Level level, BlockPos pos, ChestBlockEntity basket) {
        long linkId = SECURE_RANDOM.nextLong();
        CompoundTag savedTag = basket.saveWithoutMetadata(level.registryAccess());
        savedTag.putLong(EnderChestBlockEntity.TAG_LINK_ID, linkId);

        level.setBlock(pos, Registration.ENDER_PICNIC_BASKET_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);

        BlockEntity newBe = level.getBlockEntity(pos);
        if (newBe != null) {
            TypedEntityData.of(newBe.getType(), savedTag).loadInto(newBe, level.registryAccess());
            newBe.setChanged();
        }

        ItemStack second = new ItemStack(Registration.ENDER_PICNIC_BASKET_ITEM.get());
        second.set(
                DataComponents.BLOCK_ENTITY_DATA,
                TypedEntityData.of(
                        Registration.ENDER_PICNIC_BASKET_BE_TYPE.get(), stripInstanceUpgrades(savedTag.copy())));
        return second;
    }

    private static ItemStack applyEnderToTank(Level level, BlockPos pos, TankBlockEntity tank) {
        long linkId = SECURE_RANDOM.nextLong();
        CompoundTag savedTag = tank.saveWithoutMetadata(level.registryAccess());
        savedTag.putLong(EnderTankBlockEntity.TAG_LINK_ID, linkId);

        level.setBlock(pos, Registration.ENDER_TANK.get().defaultBlockState(), Block.UPDATE_ALL);

        BlockEntity newBe = level.getBlockEntity(pos);
        if (newBe != null) {
            TypedEntityData.of(newBe.getType(), savedTag).loadInto(newBe, level.registryAccess());
            newBe.setChanged();
        }

        ItemStack second = new ItemStack(Registration.ENDER_TANK_ITEM.get());
        second.set(
                DataComponents.BLOCK_ENTITY_DATA,
                TypedEntityData.of(Registration.ENDER_TANK_BE_TYPE.get(), savedTag.copy()));
        return second;
    }
}
