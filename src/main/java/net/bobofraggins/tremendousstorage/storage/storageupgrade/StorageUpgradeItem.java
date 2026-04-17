package net.bobofraggins.tremendousstorage.storage.storageupgrade;

import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.power.stirlingengine.StirlingEngineBlockEntity;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlockEntity;
import net.bobofraggins.tremendousstorage.storage.networkinterface.NetworkInterfaceBlockEntity;
import net.bobofraggins.tremendousstorage.storage.tank.TankBlockEntity;
import net.bobofraggins.tremendousstorage.storage.wirelesshub.WirelessHubBlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Upgrade item that raises a Tremendous Chest by one {@link StorageTier}.
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
        if (be instanceof ChestBlockEntity bulk && bulk.isUpgradeable() && bulk.getTier() == from) {
            if (!ctx.getLevel().isClientSide()) {
                bulk.setTier(to);
            }
            matches = true;
        } else if (be instanceof NetworkInterfaceBlockEntity ni && ni.getTier() == from) {
            if (!ctx.getLevel().isClientSide()) {
                ni.setTier(to);
            }
            matches = true;
        } else if (be instanceof TankBlockEntity tank && tank.getTier() == from) {
            if (!ctx.getLevel().isClientSide()) {
                tank.setTier(to);
            }
            matches = true;
        } else if (be instanceof WirelessHubBlockEntity hub && hub.getTier() == from) {
            if (!ctx.getLevel().isClientSide()) {
                hub.setTier(to);
            }
            matches = true;
        } else if (be instanceof StirlingEngineBlockEntity engine && engine.getTier() == from) {
            if (!ctx.getLevel().isClientSide()) {
                engine.setTier(to);
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
