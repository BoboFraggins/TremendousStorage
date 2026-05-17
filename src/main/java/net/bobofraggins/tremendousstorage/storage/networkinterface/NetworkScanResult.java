package net.bobofraggins.tremendousstorage.storage.networkinterface;

import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * The result of a full Network Interface BFS scan.
 *
 * @param insertOrder storage handlers sorted highest-priority first (used for slot
 *     resolution/extraction)
 * @param insertBuckets handlers grouped by priority ordinal, descending (used for two-phase
 *     insert)
 * @param blockList human-readable entries for the UI block list
 * @param isValid {@code true} if exactly one Network Interface is present on the network
 * @param totalFePerTick total FE/t consumed by all components in this network
 * @param tubePositions all tube block positions visited during the BFS scan (unmodifiable)
 * @param tanks all tank fluid handlers reachable in the network, in discovery order
 */
public record NetworkScanResult(
        List<ResourceHandler<ItemResource>> insertOrder,
        NavigableMap<Integer, List<ResourceHandler<ItemResource>>> insertBuckets,
        List<AttachedEntry> blockList,
        boolean isValid,
        int totalFePerTick,
        Set<BlockPos> tubePositions,
        List<ResourceHandler<FluidResource>> tanks) {}
