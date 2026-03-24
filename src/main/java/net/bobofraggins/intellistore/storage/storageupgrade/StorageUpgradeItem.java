package net.bobofraggins.intellistore.storage.storageupgrade;

import net.bobofraggins.intellistore.power.stirlingengine.StirlingEngineBlockEntity;
import net.bobofraggins.intellistore.shared.storage.StorageTier;
import net.bobofraggins.intellistore.storage.bulkstorage.BulkStorageContainerBlockEntity;
import net.bobofraggins.intellistore.storage.junkdrawer.JunkDrawerBlockEntity;
import net.bobofraggins.intellistore.storage.networkinterface.NetworkInterfaceBlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Upgrade item that raises a Bulk Storage Container or Junk Drawer by one {@link StorageTier}.
 *
 * <p>Right-clicking the item on a matching block with the correct current tier consumes the item
 * and upgrades the block. The upgrade is retained in block-entity NBT and survives break/replace.
 */
public class StorageUpgradeItem extends Item {

    private final StorageTier from;
    private final StorageTier to;

    public StorageUpgradeItem(StorageTier from, StorageTier to, Properties properties) {
        super(properties);
        this.from = from;
        this.to = to;
    }

    public StorageTier getFromTier() {
        return from;
    }

    public StorageTier getToTier() {
        return to;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        BlockEntity be = ctx.getLevel().getBlockEntity(ctx.getClickedPos());

        boolean matches = false;
        if (be instanceof BulkStorageContainerBlockEntity bulk && bulk.getTier() == from) {
            if (!ctx.getLevel().isClientSide()) {
                bulk.setTier(to);
            }
            matches = true;
        } else if (be instanceof JunkDrawerBlockEntity junk && junk.getTier() == from) {
            if (!ctx.getLevel().isClientSide()) {
                junk.setTier(to);
            }
            matches = true;
        } else if (be instanceof StirlingEngineBlockEntity engine && engine.getTier() == from) {
            if (!ctx.getLevel().isClientSide()) {
                engine.setTier(to);
            }
            matches = true;
        } else if (be instanceof NetworkInterfaceBlockEntity ni && ni.getTier() == from) {
            if (!ctx.getLevel().isClientSide()) {
                ni.setTier(to);
            }
            matches = true;
        }

        if (matches) {
            if (!ctx.getLevel().isClientSide()) {
                ctx.getItemInHand().shrink(1);
            }
            return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide());
        }

        return InteractionResult.PASS;
    }
}
