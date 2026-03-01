package net.bobofraggins.intellistore.storage.networkinterface;

import com.mojang.serialization.MapCodec;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** A single-block-tall Network Interface. */
public class NetworkInterfaceBlock extends BaseEntityBlock {

    public static final MapCodec<NetworkInterfaceBlock> CODEC = simpleCodec(NetworkInterfaceBlock::new);

    @Override
    public MapCodec<NetworkInterfaceBlock> codec() {
        return CODEC;
    }

    public NetworkInterfaceBlock(Properties props) {
        super(props);
    }

    // -------------------------------------------------------------------------
    // Block entity
    // -------------------------------------------------------------------------

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NetworkInterfaceBlockEntity(pos, state);
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(
                type, Registration.NETWORK_INTERFACE_BE_TYPE.get(), (lvl, pos, st, be) -> be.serverTick());
    }

    // -------------------------------------------------------------------------
    // Neighbour change — invalidate scan cache
    // -------------------------------------------------------------------------

    @Override
    public void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            net.minecraft.world.level.block.Block neighborBlock,
            net.minecraft.core.BlockPos neighborPos,
            boolean movedByPiston) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof NetworkInterfaceBlockEntity ni) {
            ni.setChanged();
        }
    }

    // -------------------------------------------------------------------------
    // Interaction
    // -------------------------------------------------------------------------

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (player.isShiftKeyDown()) {
            // Sneak + right-click: deposit all player inventory items into the network
            if (!(level.getBlockEntity(pos) instanceof NetworkInterfaceBlockEntity ni)) {
                return InteractionResult.FAIL;
            }
            IItemHandler network = ni.getItemHandler();
            if (network == null) return InteractionResult.FAIL;
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                net.minecraft.world.item.ItemStack held = player.getInventory().getItem(slot);
                if (held.isEmpty()) continue;
                net.minecraft.world.item.ItemStack remainder = ItemHandlerHelper.insertItem(network, held, false);
                player.getInventory().setItem(slot, remainder);
            }
            player.getInventory().setChanged();
            player.displayClientMessage(Component.translatable("action.intellistore.deposit_complete"), true);
            return InteractionResult.SUCCESS;
        }

        if (level.getBlockEntity(pos) instanceof MenuProvider mp) {
            player.openMenu(mp, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }
}
