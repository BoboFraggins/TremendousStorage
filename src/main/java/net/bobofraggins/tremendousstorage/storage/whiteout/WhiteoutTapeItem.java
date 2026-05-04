package net.bobofraggins.tremendousstorage.storage.whiteout;

import net.bobofraggins.tremendousstorage.storage.barrel.BarrelBlock;
import net.bobofraggins.tremendousstorage.storage.barrel.BarrelBlockEntity;
import net.bobofraggins.tremendousstorage.storage.filingcabinet.FilingCabinetBlockEntity;
import net.bobofraggins.tremendousstorage.storage.manillafolder.FolderContents;
import net.bobofraggins.tremendousstorage.storage.manillafolder.ManillaFolderItem;
import net.bobofraggins.tremendousstorage.storage.tank.TankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Whiteout Tape — a limited-use tool that unlocks locked-but-empty storage blocks and folders.
 *
 * <p>Each use costs one durability. When the tape would reach max damage it is consumed entirely.
 *
 * <p>In the crafting grid: tape + locked-empty folder or fluid-tank item → unlocked item
 * (handled by {@link FolderTapeRecipe}).
 *
 * <p>Right-click on a block:
 * <ul>
 *   <li><b>Tremendous Tank</b>: if locked and empty (amount == 0), clears the fluid lock.
 *   <li><b>Filing Cabinet</b>: clears all folders in the cabinet that are locked but empty
 *       (storedItem present, count == 0). One tape use per click, regardless of how many
 *       folders are cleared.
 * </ul>
 */
public class WhiteoutTapeItem extends Item {

    public static final int MAX_DURABILITY = 32;

    public WhiteoutTapeItem() {
        super(new Item.Properties().stacksTo(1).durability(MAX_DURABILITY));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getLevel().isClientSide()) return InteractionResult.SUCCESS;

        var level = ctx.getLevel();
        var pos = ctx.getClickedPos();
        var be = level.getBlockEntity(pos);
        var tape = ctx.getItemInHand();

        if (be instanceof BarrelBlockEntity barrel) {
            // Skip the item-display area — that zone is for extract gestures only.
            BlockState state = level.getBlockState(pos);
            Direction facing = state.getValue(BarrelBlock.FACING);
            if (ctx.getClickedFace() == facing) {
                BlockPos bp = ctx.getClickedPos();
                if (BarrelBlock.isInItemArea(new BlockHitResult(ctx.getClickLocation(), facing, bp, false), facing)) {
                    return InteractionResult.PASS;
                }
            }
            if (!barrel.isLocked() || barrel.getCount() > 0) return InteractionResult.PASS;
            barrel.clearItem();
            consumeDurability(tape, ctx);
            return InteractionResult.SUCCESS;
        }

        if (be instanceof TankBlockEntity tank) {
            if (!tank.isLocked() || tank.getAmount() > 0) {
                // Not locked, or still has fluid — nothing to do
                return InteractionResult.PASS;
            }
            tank.clearFluid();
            consumeDurability(tape, ctx);
            return InteractionResult.SUCCESS;
        }

        if (be instanceof FilingCabinetBlockEntity cabinet) {
            boolean anyCleared = false;
            for (int i = 0; i < FilingCabinetBlockEntity.SLOT_COUNT; i++) {
                ItemStack folder = cabinet.getFolder(i);
                if (folder.isEmpty()) continue;
                FolderContents contents = ManillaFolderItem.getContents(folder);
                if (contents.storedItem().isPresent() && contents.count() == 0) {
                    // Clear this folder's lock
                    cabinet.setFolder(i, ManillaFolderItem.setContents(folder, FolderContents.EMPTY));
                    anyCleared = true;
                }
            }
            if (!anyCleared) return InteractionResult.PASS;
            consumeDurability(tape, ctx);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    /**
     * Damages the tape by 1. If that would hit max damage, shrinks the stack to 0 (destroys it)
     * instead of leaving a broken item.
     */
    private static void consumeDurability(ItemStack tape, UseOnContext ctx) {
        if (ctx.getPlayer() != null && ctx.getPlayer().isCreative()) return;
        int newDamage = tape.getDamageValue() + 1;
        if (newDamage >= tape.getMaxDamage()) {
            tape.shrink(1);
        } else {
            tape.setDamageValue(newDamage);
        }
    }
}
