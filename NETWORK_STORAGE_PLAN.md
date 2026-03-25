# Network Storage Implementation Plan

## Prerequisite Note: fastutil

NeoForge bundles fastutil (`it.unimi.dsi.fastutil`), so `Object2LongOpenHashMap`,
`Reference2ObjectOpenHashMap`, and the AVL tree maps are available without adding any Gradle
dependencies.

---

## Phase 1 — `StorageKey` (foundation)

**New file**: `shared/storage/StorageKey.java`

```
final class StorageKey {
    final Item item;                     // primary key (singleton — identity == is valid)
    final DataComponentPatch components; // secondary key (copied from source stack)
    final int hashCode;                  // pre-computed once at construction

    static StorageKey of(ItemStack stack);      // factory — rejects empty, copies stack
    static StorageKey ofFuzzy(ItemStack stack); // dropSecondary — for JunkDrawer range queries
    boolean isPrimarilyEqual(StorageKey other); // item == only, ignores components
    ItemStack toDisplayStack();                 // reconstructs ItemStack(item, 1, components)
}
```

`equals()` short-circuits on `hashCode` mismatch before touching `DataComponentPatch`. `hashCode`
is computed via `ItemStack.hashItemAndComponents()` after applying the components. The factory
is the only constructor path.

- [x] Create `shared/storage/StorageKey.java` with `of()`, `ofFuzzy()`, `isPrimarilyEqual()`, `toDisplayStack()`
- [x] Implement `equals()` with hash short-circuit and `hashCode()` using `ItemStack.hashItemAndComponents()`

---

## Phase 2 — BulkStorage data structure (biggest bang for buck)

**Modified**: `BulkStorageContainerBlockEntity`

Replace the parallel `List<ItemStack> types` / `List<Long> counts` with:

```java
// null until ensureLoaded(); both always non-null together
private Object2LongOpenHashMap<StorageKey> items;  // O(1) lookup by key
private List<StorageKey> orderedKeys;              // parallel list for O(1) slot access
private long cachedTotalCount;                     // maintained incrementally
```

Operation costs after the change:

| Operation | Before | After |
|---|---|---|
| `findType(ItemStack)` | O(n) scan | O(1) hash lookup |
| `insert()` — existing type | O(n) scan + set | O(1) |
| `insert()` — new type | O(n) scan + add | O(1) amortized |
| `extract(int index)` — count > 0 | O(1) | O(1) |
| `extract(int index)` — type removed | O(1) | O(1) swap-with-last |
| `totalCount()` | O(n) sum | O(1) field |

Type removal (count → 0) uses a swap-with-last trick on `orderedKeys`:
`orderedKeys.set(index, orderedKeys.removeLast())` — O(1) at the cost of changing
iteration order, which is acceptable since BulkStorage has no guaranteed ordering contract.

**Lazy deserialization**: initialise `items` / `orderedKeys` to `null`. Populate on first
`ensureLoaded()` call (called at the top of every insert/extract/query method). This avoids
deserialising every BulkStorage block in a loaded chunk during world load.

**`BulkStorageContainerItemHandler`** requires no interface changes — it already calls through
to the block entity's `insert`/`extract`/`getType`/`getCount`/`typeCount` methods.

**JunkDrawer** (`List<ItemStack> items`, O(n) linear scan for duplicate detection): same
treatment — replace with `Set<StorageKey>` + `List<StorageKey> orderedKeys` +
`long cachedTotalCount`. JunkDrawer never merges counts so it stays a set, not a map. Smaller
performance impact than BulkStorage but worth doing consistently.

- [x] Replace `List<ItemStack> types` / `List<Long> counts` in `BulkStorageContainerBlockEntity` with `Object2LongOpenHashMap<StorageKey> items` + `List<StorageKey> orderedKeys` + `long cachedTotalCount`
- [ ] Implement lazy `ensureLoaded()` (init from NBT on first access) — skipped per "Does BulkStorage need the full treatment?" decision
- [x] Update `insert()` — O(1) map put, maintain `cachedTotalCount`
- [x] Update `extract(int index)` — swap-with-last on removal
- [x] Update NBT serialization/deserialization to use the new structure
- [x] Replace `List<ItemStack> items` in `JunkDrawer` with `Set<StorageKey>` + `List<StorageKey> orderedKeys` + `long cachedTotalCount`
- [x] Update JunkDrawer NBT serialization/deserialization

---

## Phase 3 — `KeyCounter` + `VariantCounter` (NI aggregate index)

**New package**: `storage/networkinterface/cache/` (or `shared/storage/cache/`)

**`VariantCounter` (interface)**:

```java
long getAmount(StorageKey key);
void add(StorageKey key, long delta);
void remove(StorageKey key);         // removes entry entirely
void clear();
Iterable<Object2LongMap.Entry<StorageKey>> entries();
boolean isEmpty();
```

Two implementations:

- `UnorderedVariantMap` — `Object2LongOpenHashMap<StorageKey>` — for all non-damageable items.
- `FuzzyVariantMap` — `Object2LongAVLTreeMap<StorageKey>` sorted by ascending damage value —
  for `item.getMaxDamage() > 0`. Exposes `findFuzzy(int minDamage, int maxDamage)` via `subMap`
  for O(log n) range queries (e.g. JunkDrawer extraction of "any sword with ≤ 50% durability").

**`KeyCounter`**:

```java
Reference2ObjectOpenHashMap<Item, VariantCounter> outerMap;

void add(StorageKey key, long amount);
void remove(StorageKey key, long amount);
long getAmount(StorageKey key);
@Nullable VariantCounter getVariants(Item item);
void clear();
Iterable<Object2LongMap.Entry<StorageKey>> allEntries(); // for diff + terminal
```

The `Reference2ObjectOpenHashMap` outer key uses `==` identity on `Item` singletons — no
hashing at the outer level at all. Looking up "total Oak Logs" is one identity comparison + one
hash map lookup into the `VariantCounter`.

- [x] Create `VariantCounter` interface
- [x] Implement `UnorderedVariantMap` (`Object2LongOpenHashMap<StorageKey>`)
- [x] Implement `FuzzyVariantMap` (`Object2LongAVLTreeMap<StorageKey>`) with `findFuzzy(int, int)`
- [x] Create `KeyCounter` with `Reference2ObjectOpenHashMap<Item, VariantCounter>` outer map
- [x] Implement `KeyCounter.add()`, `remove()`, `getAmount()`, `getVariants()`, `clear()`, `allEntries()`

---

## Phase 4 — Separate topology-dirty from content-dirty

**Current problem**: `setChanged()` on any storage block calls `invalidateNiCache()`, and
the NI's own `setChanged()` nulls `cachedScan` + `cachedHandler`. This means a single item
insertion into BulkStorage triggers a full BFS re-scan on the next NI access. That BFS is cheap
but unnecessary for content-only changes.

**Fix**: Introduce two levels of cache dirtiness in `NetworkInterfaceBlockEntity`:

```java
private boolean topologyDirty = false;  // tube/block added or removed → re-scan needed
private boolean contentsDirty = false;  // items inserted/extracted → rebuild KeyCounter only
```

Add a method: `markContentsDirty()` — sets `contentsDirty = true`, does NOT null `cachedScan`.

Update `NiCacheHolder` (the interface implemented by FilingCabinet, BulkStorage, JunkDrawer):
add `notifyNiContentsChanged(ServerLevel level)`. Each storage block's `setChanged()` calls this
for item-content mutations, and only calls `invalidateNiCache()` (full topology invalidation)
for structural changes (tier upgrade, block removal).

The storage blocks already have `getOrFindNiPos()` — use it to obtain the NI BE, then call
`markContentsDirty()` on it.

- [ ] Add `topologyDirty` and `contentsDirty` flags to `NetworkInterfaceBlockEntity` — added `contentsDirty`; `topologyDirty` is redundant with `cachedScan == null` so omitted
- [x] Add `markContentsDirty()` method (sets `contentsDirty`, does NOT null `cachedScan`)
- [x] Add `notifyNiContentsChanged(ServerLevel)` to `NiCacheHolder` interface (as a default method)
- [x] Update FilingCabinet, BulkStorage, and JunkDrawer `setChanged()` to call `notifyNiContentsChanged()` for item mutations and `invalidateNiCache()` only for structural changes

---

## Phase 5 — `StorageService` (aggregate cache inside NI BE)

Add to `NetworkInterfaceBlockEntity`:

```java
private KeyCounter cachedAvailableStacks;                    // null until first build
private Object2LongOpenHashMap<StorageKey> cachedAmountSnapshot; // shadow copy for diff
private long nextSerial = 1;                                 // stable client ID per key
private Object2LongOpenHashMap<StorageKey> keySerials;       // StorageKey → serial number
```

`getCachedInventory()` — returns `cachedAvailableStacks`, rebuilding lazily if `contentsDirty`
(or null).

`rebuildCache()`:
1. Walk `getScan().insertOrder()` — call `getStackInSlot`/`getSlots` on every handler to collect
   all stacks into a fresh `KeyCounter`.
2. Diff against `cachedAmountSnapshot`: collect all `StorageKey` whose amounts changed.
3. Update `cachedAmountSnapshot`.
4. Assign new serial numbers to keys appearing for the first time.
5. Notify watchers (Phase 6) for each changed key.

**Rebuild schedule** (in `serverTick()`):
- If `contentsDirty && hasWatchers()`: rebuild every tick (fast — just hash map iteration,
  no disk I/O).
- If `contentsDirty && !hasWatchers()`: leave dirty; rebuild lazily on next
  `getCachedInventory()`.

This replaces the current pattern where `AccessTerminalMenu` iterates all handler slots every
time to build its `List<ItemStack>` / `List<Long>` response.

- [ ] Add `cachedAvailableStacks`, `cachedAmountSnapshot`, `nextSerial`, `keySerials` fields to `NetworkInterfaceBlockEntity`
- [ ] Implement `getCachedInventory()` with lazy rebuild on `contentsDirty`
- [ ] Implement `rebuildCache()`: collect all stacks into fresh `KeyCounter`, diff against snapshot, update snapshot, assign serials, notify watchers
- [ ] Update `serverTick()` with rebuild schedule (every tick if watchers present, lazy otherwise)
- [ ] Update `AccessTerminalMenu` to use `getCachedInventory()` instead of iterating handler slots directly

---

## Phase 6 — Two-phase insert in `NiItemHandler`

Add `IPreferredStorage` interface (optional capability on `IItemHandler` wrappers):

```java
interface IPreferredStorage {
    boolean isPreferredFor(StorageKey key);
}
```

Implementations:
- `BulkStorageContainerItemHandler`: `items.containsKey(key)` — O(1) with the new map.
- `FilingCabinetItemHandler`: any slot whose `FolderContents.accepts(stack)` returns true.

Change `NetworkScanResult.insertOrder` from `List<IItemHandler>` to
`NavigableMap<Integer, List<IItemHandler>> insertBuckets` (keyed by `Priority.ordinal()`,
descending). The BFS already sorts by priority — just group instead of flatten.

**New `NiItemHandler.insertItem()` algorithm**:

```
for each priority bucket, highest first:
    phase 1: for each handler in bucket where isPreferredFor(key): insert
    if remaining == 0: return
    phase 2: for each handler in bucket where !isPreferredFor(key): insert
    if remaining == 0: return
```

This prevents item fragmentation: Dirt goes into BulkStorage that already holds Dirt rather
than scattering across every container at that priority level.

**Reentrancy guard**: add `boolean operationInProgress` to `NiItemHandler`. If a mount/unmount
request arrives mid-insert (via `setChanged()` → capability invalidation), queue it as a sealed
record and drain after the operation completes:

```java
sealed interface PendingOp permits MountRequest, UnmountRequest {}
record MountRequest(IItemHandler handler, Priority priority) implements PendingOp {}
record UnmountRequest(IItemHandler handler) implements PendingOp {}

private final List<PendingOp> pendingOps = new ArrayList<>();
```

- [ ] Create `IPreferredStorage` interface with `isPreferredFor(StorageKey)`
- [ ] Implement `IPreferredStorage` in `BulkStorageContainerItemHandler` (`items.containsKey(key)`)
- [ ] Implement `IPreferredStorage` in `FilingCabinetItemHandler` (slot accepts check)
- [ ] Change `NetworkScanResult.insertOrder` to `NavigableMap<Integer, List<IItemHandler>> insertBuckets`
- [ ] Rewrite `NiItemHandler.insertItem()` with two-phase algorithm (preferred-first per bucket)
- [ ] Add `operationInProgress` flag and `pendingOps` queue to `NiItemHandler`
- [ ] Drain `pendingOps` after each insert/extract completes

---

## Phase 7 — Watcher system

**`INetworkListener`** and **`IKeySubscription`** interfaces (new, in `shared/storage/`):

```java
interface INetworkListener {
    void onSubscriptionSet(IKeySubscription subscription);
    void onStackChange(StorageKey what, long newAmount); // 0 = item gone from network
}

interface IKeySubscription {
    void add(StorageKey key);       // watch specific key
    void setWatchAll(boolean all);  // watch all changes
}
```

**`WatcherRegistry`** (held by NI BE):

```java
SetMultimap<StorageKey, INetworkListener> perKeyInterests;
List<INetworkListener> globalWatchers;
```

After each cache rebuild diff, for each changed key: notify `perKeyInterests.get(key)` then
`globalWatchers`.

**`HandlerInterceptor`** — thin wrapper around each `IItemHandler` in the scan result. Intercepts
every call to `insertItem`/`extractItem`. After a non-simulate operation, calls
`ni.markContentsDirty()`. Optionally compares before/after `ContainerState`
(EMPTY / NORMAL / FULL) to drive visual indicators — for now, just the dirty propagation is
needed; LED state can be added later.

- [ ] Create `INetworkListener` interface with `onSubscriptionSet()` and `onStackChange()`
- [ ] Create `IKeySubscription` interface with `add()` and `setWatchAll()`
- [ ] Create `WatcherRegistry` (held by NI BE) with `perKeyInterests` multimap and `globalWatchers` list
- [ ] Wire `WatcherRegistry` notifications into `rebuildCache()` diff loop
- [ ] Create `HandlerInterceptor` wrapping `IItemHandler`; call `markContentsDirty()` after non-simulate operations
- [ ] Define `ContainerState` enum (EMPTY / NORMAL / FULL) for future LED indicators

---

## Phase 8 — Client-side `NetworkInventoryRepo` / delta terminal updates

**Current**: `AccessTerminalMenu` sends `RequestSatContentsPacket` → server iterates all slots
→ sends full `List<ItemStack>` + `List<Long>` → client replaces `NetworkInventoryPane.allStacks`.

**Target**: incremental delta updates.

Server side (in `AccessTerminalMenu` / its server tick):
- When the menu is opened, send the full inventory as `(serial, ItemStack, long count)` tuples.
- On each subsequent tick where `rebuildCache()` found changed keys, send only the deltas:
  `(serial, newAmount)` per changed key. A `newAmount` of 0 means removal.

Client side (`NetworkInventoryPane`):

```java
Long2ObjectOpenHashMap<GridEntry> entriesBySerial; // server-assigned stable IDs
List<GridEntry> view;                              // sorted/filtered display list
boolean viewDirty;                                 // true = rebuild view before next render
```

`GridEntry` holds `(long serial, StorageKey key, long amount)`.

**Incremental vs. full rebuild**:
- On delta update while the player is scrolling: update only the affected entry in
  `entriesBySerial`, do not re-sort `view` — items stay in place.
- On delta update while idle: set `viewDirty = true`; rebuild `view` from scratch before
  next render.

The existing full-dump path stays as a fallback and for initial population on menu open.

- [ ] Add `NetworkInventoryRepo` client-side class with `Long2ObjectOpenHashMap<GridEntry> entriesBySerial`, `List<GridEntry> view`, `boolean viewDirty`
- [ ] Define `GridEntry` record (`long serial`, `StorageKey key`, `long amount`)
- [ ] Add server-side delta tracking in `AccessTerminalMenu`: collect changed keys each tick from `rebuildCache()` diff
- [ ] Implement full-dump packet (sent on menu open): `(serial, ItemStack, count)` tuples
- [ ] Implement delta packet (sent each tick with changes): `(serial, newAmount)` per changed key; `0` signals removal
- [ ] Update `NetworkInventoryPane` to apply full-dump and delta packets to `NetworkInventoryRepo`
- [ ] Implement `viewDirty` logic: skip re-sort during scroll, rebuild before next render otherwise

---

## Does BulkStorage need the full treatment?

**Yes for the data structure** (Phase 2). The O(n) `findType()` is the core bottleneck — at
Netherite tier with tens of thousands of distinct types, every single insert pays a full list
scan.

**No for `CellInventory` as a separate class.** AE2's `CellInventory` models a drive cell
with a two-axis capacity model (type slots + byte budget). BulkStorage uses a simpler
single-axis model (total item count only). Wrapping it in a similar abstraction adds complexity
without benefit — just refactor the internals directly.

**No for lazy NBT deserialization** right now. The lazy-load benefit applies to item-cell form
factors (the cell's inventory isn't loaded until you open the drive). IntelliStore's BulkStorage
blocks hold data in block entity NBT, which Minecraft only loads on chunk load regardless. Worth
revisiting if BulkStorage ever gains an item-cell form factor.

**Yes for `StorageKey`** across the board. Using `StorageKey.of()` as the map key everywhere
gives consistent identity semantics and the pre-computed hash benefit at every call site.

---

## Build order

Each phase depends on the one before it. The single highest-value isolated change is
**Phase 2** — it fixes the O(n) scan in BulkStorage with a self-contained refactor and no API
surface changes visible outside the package. Phase 1 (`StorageKey`) is small (one new class,
no callers until Phase 2) and should land immediately before it.

| Phase | Deliverable | Depends on |
|---|---|---|
| 1 | `StorageKey` | — |
| 2 | BulkStorage + JunkDrawer hash maps | 1 |
| 3 | `KeyCounter` + `VariantCounter` | 1 |
| 4 | Separate topology/content dirty | — |
| 5 | `StorageService` (NI aggregate cache) | 3, 4 |
| 6 | Two-phase insert | 1, 5 |
| 7 | Watcher system + `HandlerInterceptor` | 5 |
| 8 | Client delta updates (`NetworkInventoryRepo`) | 7 |
