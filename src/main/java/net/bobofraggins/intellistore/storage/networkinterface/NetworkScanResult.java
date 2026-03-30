package net.bobofraggins.intellistore.storage.networkinterface;

import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The result of a full Network Interface BFS scan.
 *
 * @param insertOrder storage handlers sorted highest-priority first (used for slot
 *     resolution/extraction)
 * @param insertBuckets handlers grouped by priority ordinal, descending (used for two-phase
 *     insert)
 * @param blockList human-readable entries for the UI block list
 * @param isValid {@code true} if exactly one Network Interface is present on the network
 * @param totalFePerTick total FE/t consumed by all network components (NI + SATs + Hubs +
 *     attachments)
 * @param tubePositions all tube block positions visited during the BFS scan (unmodifiable)
 */
public record NetworkScanResult(
        List<IItemHandler> insertOrder,
        NavigableMap<Integer, List<IItemHandler>> insertBuckets,
        List<AttachedEntry> blockList,
        boolean isValid,
        int totalFePerTick,
        Set<BlockPos> tubePositions) {}
