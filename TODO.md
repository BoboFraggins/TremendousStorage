# TODO

## Performance: O() Complexity Analysis

### Network Scanning

**`NetworkInterfaceBFS.scan()` — O((T + B) + B log B)**

- BFS traversal: O(T + B) where T = tube count, B = non-tube neighbor blocks. Each position is visited at most once via `visitedTubes`/`collectedStorage` HashSets.
- Handler sort (`handlerEntries.sort`): O(B log B) — standard comparison sort over all storage neighbors found.
- UI block list (`Collections.sort(sortedKeys)`): O(B log B) — sorts unique block type names alphabetically.
- `bridgeConnectorCluster()`: O(C) sub-BFS over connector chains, absorbed into the B term above.

Sort order impact: None on asymptotic complexity. The `handlerEntries` sort and `insertBuckets` TreeMap (built with `Comparator.reverseOrder()`) both produce the same O(B log B) work regardless of the priority distribution.

**`AccessTerminalBFS.findNI()` — O(T + B) worst case, often much better**

Returns on the first NI found — large networks with a nearby NI exit almost immediately. Worst case is a full traversal of the reachable network with no NI.

---

### Inventory Access

**`NiItemHandler.insertItem()` — O(S) worst case, O(S_preferred) typical**

Two-phase insert: first tries handlers where `IPreferredStorage.canStore(item)` returns true, then falls back to all handlers in priority order via `insertBuckets`. Within each bucket, tries handlers linearly. Each `insertItem` call on an underlying `IItemHandler` is effectively O(slots_in_that_handler).

**`NiItemHandler.extractItem()` / `getStackInSlot()` — O(H) slot resolution**

This is the most notable concern. Slot numbers are virtual — resolving slot N requires linearly scanning through handlers: slot 0…size(handler_0)-1 in handler 0, then handler 1, etc. For a large network with many handlers, this is O(H) per call where H = total handler count. There's no slot index. Every extraction, slot lookup, and `broadcastChanges()` comparison traverses this linear mapping.

**`extractItem()` scan — O(S) per extraction request**

`SatExtractPacket.handle()` scans all slots via `getStackInSlot()` to find a match, each call doing O(H) resolution — giving O(S·H) in the absolute worst case, though S and H are correlated (S ≈ H × avg_slots_per_handler).

---

### Item Counting (`KeyCounter`)

**`KeyCounter.add()` — O(log V) damageable, O(1) non-damageable**

- Non-damageable items: `UnorderedVariantMap` uses a HashMap → O(1) per add.
- Damageable items: `FuzzyVariantMap` uses a TreeMap sorted by damage value → O(log V) per add, where V = number of distinct damage values for that item type.

This IS an O() win from sort order. The ascending-damage TreeMap enables O(log V) range queries ("find all stacks with damage ≤ threshold") vs. O(V) with an unsorted structure. The sort order choice directly improves query complexity.

**Inventory cache rebuild — O(S log V)**

Iterates all S slots, calling `KeyCounter.add()` O(log V) per slot. Practically O(S) since V is small for most items.

---

### Packet / Display

**`buildContentsPacket()` — O(N log N) always**

Sorts all N distinct item types for display regardless of which sort mode (name A→Z, name Z→A, quantity, etc.) is selected. All modes are comparison sorts — sort mode does not change the O() complexity, only the comparator constant factor.

---

### Summary

| Operation | Complexity | Notes |
|---|---|---|
| Full network scan | O((T+B) + B log B) | Dominated by sort when B is large |
| Find NI (SAT BFS) | O(T+B) worst, O(1) best | Early exit on first NI found |
| Insert item | O(S) | Two-phase preferred-storage optimization helps average case |
| Slot resolution | **O(H)** | Linear scan — no index; largest practical concern |
| `KeyCounter.add()` damageable | O(log V) | TreeMap sort order gives real O() benefit here |
| Cache rebuild | O(S log V) | Triggered on topology invalidation |
| Contents packet sort | O(N log N) | Sort mode choice is constant-factor only, not O() |
| `tryConsumeVibes()` | O(T) | Scans all tube neighbors |

---

### Improvement: Index Virtual Slots in `NiItemHandler`

The O(H) linear slot resolution in `NiItemHandler` means that very large networks (dozens of chests) will feel sluggish on extraction and `broadcastChanges()` polling. An index mapping virtual slot → (handler, local slot) would reduce this to O(1) but would need to be invalidated on every rescan.
