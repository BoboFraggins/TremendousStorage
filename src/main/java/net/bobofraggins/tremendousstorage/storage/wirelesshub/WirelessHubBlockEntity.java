package net.bobofraggins.tremendousstorage.storage.wirelesshub;

import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.storage.accessterminal.AccessTerminalBFS;
import net.bobofraggins.tremendousstorage.storage.networkinterface.NetworkInterfaceBlockEntity;
import net.bobofraggins.tremendousstorage.storage.networkinterface.NiCacheHolder;
import net.bobofraggins.tremendousstorage.storage.networkinterface.NetworkScanResult;
import net.bobofraggins.tremendousstorage.storage.personalaccessterminal.PersonalAccessTerminalItem;
import net.bobofraggins.tremendousstorage.storage.recyclingbin.RecyclingBinBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Block entity for the Wireless Hub.
 *
 * <p>Holds two slots:
 * <ul>
 *   <li>Slot 0 (left): accepts an unlinked {@link PersonalAccessTerminalItem}. When an item is placed
 *       here, the hub performs a BFS scan from this block to find the Network Interface.
 *       If found and the network is valid, it writes the NI position as a data component
 *       on the item and moves it to slot 1.
 *   <li>Slot 1 (right): output-only. Player retrieves the linked Wireless SAT from here.
 * </ul>
 *
 * <p>Accepts {@link StorageTier} upgrades (right-click with a StorageUpgradeItem). Each tier
 * doubles the wireless range; default (WOOD) is 16 blocks, NETHERITE is infinite.
 *
 * <p>With the HAARP Upgrade applied (item or smithing table), adds a weather-control panel to the
 * UI. Every 60 ticks the hub checks if weather matches the desired mode; if not, it consumes
 * 1 bucket of Positive Vibes from the nearest connected Recycling Bin and adjusts the weather.
 */
public class WirelessHubBlockEntity extends BlockEntity implements MenuProvider, NiCacheHolder {

    // -------------------------------------------------------------------------
    // Tier & range
    // -------------------------------------------------------------------------

    private StorageTier tier = StorageTier.WOOD;

    public StorageTier getTier() {
        return tier;
    }

    public void setTier(StorageTier tier) {
        this.tier = tier;
        setChanged();
    }

    /**
     * Returns the wireless range in blocks for the current tier.
     * WOOD=16, COPPER=32, IRON=64, GOLD=128, DIAMOND=256, EMERALD=512, NETHERITE=infinite ({@link Integer#MAX_VALUE}).
     */
    public int getRange() {
        if (tier == StorageTier.NETHERITE) return Integer.MAX_VALUE;
        return 16 << tier.ordinal();
    }

    // -------------------------------------------------------------------------
    // Interdimensional Upgrade
    // -------------------------------------------------------------------------

    private boolean interdimensionalUpgrade = false;

    public boolean hasInterdimensionalUpgrade() {
        return interdimensionalUpgrade;
    }

    public void setInterdimensionalUpgrade(boolean value) {
        interdimensionalUpgrade = value;
        setChanged();
    }

    // -------------------------------------------------------------------------
    // HAARP Upgrade
    // -------------------------------------------------------------------------

    private boolean haarpUpgrade = false;
    private WeatherMode haarpMode = WeatherMode.OFF;
    private int weatherTickCounter = 0;

    /** Weather-check interval: 60 ticks = 3 seconds. */
    private static final int WEATHER_CHECK_INTERVAL = 60;

    /** Cost in mB to change weather: 1 bucket = 1000 mB of Positive Vibes. */
    private static final int VIBES_COST_MB = 1000;

    public boolean hasHaarpUpgrade() {
        return haarpUpgrade;
    }

    public void setHaarpUpgrade(boolean value) {
        haarpUpgrade = value;
        setChanged();
    }

    public WeatherMode getHaarpMode() {
        return haarpMode;
    }

    public void setHaarpMode(WeatherMode mode) {
        haarpMode = mode;
        setChanged();
    }

    // -------------------------------------------------------------------------
    // Connection state (synced to client)
    // -------------------------------------------------------------------------

    /** True when this hub is attached to an active network. Synced to client for dish animation. */
    private boolean connected = false;
    private int serverTickCounter = 0;

    public boolean isConnected() {
        return connected;
    }

    /** Called each server tick. Checks connection every 40 ticks and HAARP weather every 60 ticks. */
    public void serverTick() {
        if (level == null || level.isClientSide()) return;
        ServerLevel serverLevel = (ServerLevel) level;

        serverTickCounter++;
        if (serverTickCounter >= 40) {
            serverTickCounter = 0;
            boolean nowConnected = computeConnected(serverLevel);
            if (nowConnected != connected) {
                connected = nowConnected;
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        if (haarpUpgrade && haarpMode != WeatherMode.OFF) {
            weatherTickCounter++;
            if (weatherTickCounter >= WEATHER_CHECK_INTERVAL) {
                weatherTickCounter = 0;
                tickWeather(serverLevel);
            }
        }
    }

    private boolean computeConnected(ServerLevel level) {
        BlockPos niPos = getOrFindNiPos(level);
        if (niPos == null) return false;
        return level.getBlockEntity(niPos) instanceof NetworkInterfaceBlockEntity ni && ni.isNetworkValid();
    }

    /**
     * Checks if the current weather matches {@link #haarpMode}. If not, consumes 1 bucket of
     * Positive Vibes from the network and adjusts the weather.
     */
    private void tickWeather(ServerLevel serverLevel) {
        boolean raining   = serverLevel.isRaining();
        boolean thundering = serverLevel.isThundering();

        boolean needsChange = switch (haarpMode) {
            case NO_RAIN      -> raining;
            case RAIN         -> !raining || thundering;
            case THUNDERSTORM -> !thundering;
            case OFF          -> false;
        };

        if (!needsChange) return;

        if (!tryConsumeVibes(serverLevel)) return;

        switch (haarpMode) {
            case NO_RAIN      -> serverLevel.setWeatherParameters(6000, 0, false, false);
            case RAIN         -> serverLevel.setWeatherParameters(0, 6000, true, false);
            case THUNDERSTORM -> serverLevel.setWeatherParameters(0, 6000, true, true);
            case OFF          -> {} // unreachable
        }
    }

    /**
     * Searches the network for a Recycling Bin with enough Positive Vibes and drains
     * {@link #VIBES_COST_MB} mB from it.
     *
     * @return true if the fluid was successfully consumed
     */
    private boolean tryConsumeVibes(ServerLevel serverLevel) {
        BlockPos niPos = getOrFindNiPos(serverLevel);
        if (niPos == null) return false;
        if (!(serverLevel.getBlockEntity(niPos) instanceof NetworkInterfaceBlockEntity ni)) return false;
        if (!ni.isNetworkValid()) return false;

        NetworkScanResult scan = ni.getScan();
        if (scan == null) return false;

        // Check NI position neighbours and all tube-adjacent positions
        for (BlockPos candidate : scan.tubePositions()) {
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = candidate.relative(dir);
                if (serverLevel.getBlockEntity(neighbor) instanceof RecyclingBinBlockEntity rb) {
                    if (rb.extractVibes(VIBES_COST_MB, true) >= VIBES_COST_MB) {
                        rb.extractVibes(VIBES_COST_MB, false);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Client-side animation state (not persisted)
    // -------------------------------------------------------------------------

    @Nullable
    private BlockPos cachedNiPos = null;

    /** Incremented each client tick; drives the dish spin animation. */
    private int tickCount = 0;

    /** Called each client tick by the block ticker. */
    public void clientTick() {
        tickCount++;
    }

    /**
     * Returns the dish Y rotation angle in degrees for the current frame.
     * Completes one full revolution every 40 ticks (2 seconds).
     *
     * @param partialTick fractional tick for smooth interpolation
     */
    public float getDishAngle(float partialTick) {
        return (tickCount + partialTick) % 40f / 40f * 360f;
    }

    // -------------------------------------------------------------------------
    // Inventory
    // -------------------------------------------------------------------------

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (slot == 0 && !getStackInSlot(0).isEmpty()) {
                tryLink();
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) return stack.getItem() instanceof PersonalAccessTerminalItem;
            return false; // slot 1 is output-only from external perspective
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    public WirelessHubBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.WIRELESS_HUB_BE_TYPE.get(), pos, state);
    }

    // -------------------------------------------------------------------------
    // Linking logic
    // -------------------------------------------------------------------------

    /**
     * Attempts to find a reachable Network Interface via BFS and link the item in slot 0.
     * If successful, moves the linked item to slot 1.
     */
    private void tryLink() {
        if (level == null || level.isClientSide()) return;

        ItemStack sat = inventory.getStackInSlot(0);
        if (sat.isEmpty() || !(sat.getItem() instanceof PersonalAccessTerminalItem)) return;

        // Find the nearest NI reachable from this hub
        BlockPos niPos = getOrFindNiPos((ServerLevel) level);
        if (niPos == null) return;

        if (!(level.getBlockEntity(niPos) instanceof NetworkInterfaceBlockEntity ni)) return;
        if (!ni.isNetworkValid()) return;

        // Write the NI position, this hub's position, and this hub's dimension into the item
        sat.set(Registration.WIRELESS_NI_POS.get(), niPos);
        sat.set(Registration.WIRELESS_HUB_POS.get(), worldPosition);
        sat.set(Registration.WIRELESS_HUB_DIMENSION.get(), level.dimension().location());

        // Move to output slot (slot 1)
        inventory.setStackInSlot(1, sat);
        inventory.setStackInSlot(0, ItemStack.EMPTY);
        setChanged();
    }

    // -------------------------------------------------------------------------
    // Inventory accessor (for WirelessHubMenu)
    // -------------------------------------------------------------------------

    public IItemHandler getInventory() {
        return inventory;
    }

    // -------------------------------------------------------------------------
    // MenuProvider
    // -------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("screen.tremendousstorage.wireless_hub");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        ContainerData data = new ContainerData() {
            @Override public int get(int i) { return i == 0 ? haarpMode.ordinal() : 0; }
            @Override public void set(int i, int v) {}
            @Override public int getCount() { return 1; }
        };
        return new WirelessHubMenu(id, inv, worldPosition, inventory, haarpUpgrade, data);
    }

    // -------------------------------------------------------------------------
    // NiCacheHolder + setChanged
    // -------------------------------------------------------------------------

    @Override
    public void invalidateNiCache() {
        cachedNiPos = null;
    }

    @Override
    @Nullable
    public BlockPos getOrFindNiPos(ServerLevel level) {
        if (cachedNiPos != null && !(level.getBlockEntity(cachedNiPos) instanceof NetworkInterfaceBlockEntity)) {
            cachedNiPos = null;
        }
        if (cachedNiPos == null) cachedNiPos = AccessTerminalBFS.findNI(level, worldPosition);
        return cachedNiPos;
    }

    @Override
    public void setChanged() {
        invalidateNiCache();
        super.setChanged();
        if (level != null) {
            level.invalidateCapabilities(worldPosition);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // -------------------------------------------------------------------------
    // NBT persistence
    // -------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
        tag.putString("Tier", tier.getId());
        if (haarpUpgrade) tag.putBoolean("HaarpUpgrade", true);
        if (haarpMode != WeatherMode.OFF) tag.putInt("HaarpMode", haarpMode.ordinal());
        if (interdimensionalUpgrade) tag.putBoolean("InterdimensionalUpgrade", true);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        tier = StorageTier.fromId(tag.getString("Tier"));
        connected = tag.getBoolean("Connected");
        haarpUpgrade = tag.getBoolean("HaarpUpgrade");
        haarpMode = WeatherMode.fromOrdinal(tag.getInt("HaarpMode"));
        interdimensionalUpgrade = tag.getBoolean("InterdimensionalUpgrade");
    }

    // -------------------------------------------------------------------------
    // Client sync
    // -------------------------------------------------------------------------

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Tier", tier.getId());
        tag.putBoolean("Connected", connected);
        tag.putBoolean("HaarpUpgrade", haarpUpgrade);
        tag.putInt("HaarpMode", haarpMode.ordinal());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
