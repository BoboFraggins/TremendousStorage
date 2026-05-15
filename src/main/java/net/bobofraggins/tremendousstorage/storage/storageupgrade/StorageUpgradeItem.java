package net.bobofraggins.tremendousstorage.storage.storageupgrade;

import net.bobofraggins.tremendousstorage.power.stirlingengine.StirlingEngineBlockEntity;
import net.bobofraggins.tremendousstorage.shared.config.TremendousStorageConfig;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.storage.barrel.BarrelBlock;
import net.bobofraggins.tremendousstorage.storage.barrel.BarrelBlockEntity;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlockEntity;
import net.bobofraggins.tremendousstorage.storage.networkinterface.NetworkInterfaceBlockEntity;
import net.bobofraggins.tremendousstorage.storage.tank.TankBlockEntity;
import net.bobofraggins.tremendousstorage.storage.wirelesshub.WirelessHubBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Upgrade item that raises a Tremendous Chest by one {@link StorageTier}.
 *
 * <p>Right-clicking the item on a matching block with the correct current tier consumes the item
 * and upgrades the block. The upgrade is retained in block-entity NBT and survives break/replace.
 */
public class StorageUpgradeItem extends Item {

    private final StorageTier from;
    private final StorageTier to;
    private final boolean storageOnly;

    public StorageUpgradeItem(StorageTier from, StorageTier to, Properties properties) {
        this(from, to, false, properties);
    }

    public StorageUpgradeItem(StorageTier from, StorageTier to, boolean storageOnly, Properties properties) {
        super(properties);
        this.from = from;
        this.to = to;
        this.storageOnly = storageOnly;
    }

    public StorageTier getFromTier() {
        return from;
    }

    public StorageTier getToTier() {
        return to;
    }

    public boolean isStorageOnly() {
        return storageOnly;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (to == StorageTier.NETHER_STAR && !TremendousStorageConfig.INCLUDE_NETHER_STAR_TIER_UPGRADE.get()) {
            return InteractionResult.PASS;
        }
        BlockEntity be = ctx.getLevel().getBlockEntity(ctx.getClickedPos());

        // Don't apply upgrade when clicking the barrel's item-display area — that zone is reserved
        // for insert/extract gestures and upgrades should only apply from the barrel-click ring.
        if (be instanceof BarrelBlockEntity) {
            BlockState state = ctx.getLevel().getBlockState(ctx.getClickedPos());
            Direction facing = state.getValue(BarrelBlock.FACING);
            if (ctx.getClickedFace() == facing) {
                Vec3 loc = ctx.getClickLocation();
                BlockPos pos = ctx.getClickedPos();
                if (BarrelBlock.isInItemArea(new BlockHitResult(loc, facing, pos, false), facing)) {
                    return InteractionResult.PASS;
                }
            }
        }

        boolean matches = false;
        if (be instanceof ChestBlockEntity bulk && bulk.isUpgradeable() && bulk.getTier() == from) {
            if (!ctx.getLevel().isClientSide()) {
                bulk.setTier(to);
            }
            matches = true;
        } else if (!storageOnly && be instanceof NetworkInterfaceBlockEntity ni && ni.getTier() == from) {
            if (!ctx.getLevel().isClientSide()) {
                ni.setTier(to);
            }
            matches = true;
        } else if (be instanceof TankBlockEntity tank && tank.getTier() == from) {
            if (!ctx.getLevel().isClientSide()) {
                tank.setTier(to);
            }
            matches = true;
        } else if (!storageOnly && be instanceof WirelessHubBlockEntity hub && hub.getTier() == from) {
            if (!ctx.getLevel().isClientSide()) {
                hub.setTier(to);
            }
            matches = true;
        } else if (!storageOnly && be instanceof StirlingEngineBlockEntity engine && engine.getTier() == from) {
            if (!ctx.getLevel().isClientSide()) {
                engine.setTier(to);
            }
            matches = true;
        } else if (be instanceof BarrelBlockEntity barrel && barrel.getTier() == from) {
            if (!ctx.getLevel().isClientSide()) {
                barrel.setTier(to);
            }
            matches = true;
        }

        if (matches) {
            if (!ctx.getLevel().isClientSide()) {
                ctx.getItemInHand().shrink(1);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}
