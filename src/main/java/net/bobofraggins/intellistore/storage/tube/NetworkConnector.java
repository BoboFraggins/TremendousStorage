package net.bobofraggins.intellistore.storage.tube;

/**
 * Marker interface for IntelliStore blocks that act as color-agnostic network
 * connectors. When the BFS encounters one of these blocks as a neighbor, it
 * collects that block's {@code IItemHandler} as usual and then continues the
 * flood-fill through all of its own adjacent tubes (any color). This allows
 * chains like:
 *
 * <pre>
 *   [red tube] — [Filing Cabinet] — [blue tube] — [NI]
 * </pre>
 *
 * to form a single unified network even though the two tube segments have
 * different colors.
 *
 * <p>Blocks that implement this interface:
 * <ul>
 *   <li>{@link net.bobofraggins.intellistore.storage.filingcabinet.FilingCabinetBlock}
 *   <li>{@link net.bobofraggins.intellistore.storage.bulkstorage.BulkStorageContainerBlock}
 *   <li>{@link net.bobofraggins.intellistore.storage.accessterminal.AccessTerminalBlock}
 *   <li>{@link net.bobofraggins.intellistore.storage.wirelesshub.WirelessHubBlock}
 *   <li>{@link net.bobofraggins.intellistore.storage.fluidtank.FluidTankBlock}
 *   <li>{@link net.bobofraggins.intellistore.storage.networkinterface.NetworkInterfaceBlock}
 * </ul>
 */
public interface NetworkConnector {}
