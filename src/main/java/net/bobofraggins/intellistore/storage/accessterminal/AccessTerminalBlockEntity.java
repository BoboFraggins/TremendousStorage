package net.bobofraggins.intellistore.storage.accessterminal;

import net.bobofraggins.intellistore.shared.register.Registration;
import net.bobofraggins.intellistore.storage.networkinterface.NetworkInterfaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for the Storage Access Terminal.
 *
 * <p>Exists solely to drive a server-side tick that keeps the {@code active} blockstate
 * property in sync with the connected Network Interface's power state. BFS runs every
 * 20 ticks (1 second) to avoid per-tick overhead.
 */
public class AccessTerminalBlockEntity extends BlockEntity {

    private int tickCounter = 0;

    public AccessTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.STORAGE_ACCESS_TERMINAL_BE_TYPE.get(), pos, state);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** Called each server tick by the block ticker. */
    public void serverTick() {
        if (++tickCounter < 20) return;
        tickCounter = 0;

        if (!(level instanceof ServerLevel serverLevel)) return;

        BlockPos niPos = AccessTerminalBFS.findNI(serverLevel, worldPosition);
        boolean powered = niPos != null
                && level.getBlockEntity(niPos) instanceof NetworkInterfaceBlockEntity ni
                && ni.isPowered();

        BlockState state = getBlockState();
        if (state.getValue(AccessTerminalBlock.ACTIVE) != powered) {
            level.setBlock(worldPosition, state.setValue(AccessTerminalBlock.ACTIVE, powered), 3);
        }
    }
}
