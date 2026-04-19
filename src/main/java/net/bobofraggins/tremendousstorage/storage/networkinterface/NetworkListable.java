package net.bobofraggins.tremendousstorage.storage.networkinterface;

/**
 * Implemented by block entities that appear in the Network Interface's connected-device list.
 *
 * <p>Separates the UI title ({@link net.minecraft.world.MenuProvider#getDisplayName()}, which
 * returns just the base block name) from the network list name ({@link #getNetworkName()}, which
 * includes the full upgrade suffix).
 */
public interface NetworkListable {
    /** Returns the full display name for the NI connected-device list, including any upgrade suffix. */
    String getNetworkName();
}
