package net.bobofraggins.intellistore.storage.networkinterface;

import java.util.List;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The result of a full Network Interface BFS scan.
 *
 * @param insertOrder    storage handlers sorted highest-priority first (used for item insertion and extraction)
 * @param blockList      human-readable entries for the UI block list
 * @param isValid        {@code true} if exactly one Network Interface is present on the network
 * @param totalFePerTick total FE/t consumed by all network components (NI + SATs + Hubs + attachments)
 */
public record NetworkScanResult(
        List<IItemHandler> insertOrder, List<AttachedEntry> blockList, boolean isValid, int totalFePerTick) {}
