package net.bobofraggins.intellistore.tube;

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
 *   <li>{@link net.bobofraggins.intellistore.filingcabinet.FilingCabinetBlock}
 *   <li>{@link net.bobofraggins.intellistore.junkdrawer.JunkDrawerBlock}
 *   <li>{@link net.bobofraggins.intellistore.bulkstorage.BulkStorageContainerBlock}
 *   <li>{@link net.bobofraggins.intellistore.storagetransceiver.StorageAccessTerminalBlock}
 *   <li>{@link net.bobofraggins.intellistore.wirelesssat.WirelessHubBlock}
 *   <li>{@link net.bobofraggins.intellistore.stirlingengine.StirlingEngineBlock}
 * </ul>
 */
public interface NetworkConnector {}
