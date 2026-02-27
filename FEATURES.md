# IntelliStore — Feature Reference

NeoForge 1.21.1 · Minecraft 1.21.1

---

## Blocks

### Filing Cabinet
- Holds up to **8 Manila Folders** in internal slots
- Right-click with a folder in hand → inserts into the first empty slot
- Right-click with empty hand → opens the Filing Cabinet screen (toggle open/close + priority)
- Sneak + right-click with empty hand (while open) → extracts the topmost folder
- Drops itself with full folder inventory intact when broken
- Exposes an `IItemHandler` capability (8 slots, one per folder) for hopper/pipe automation
- **Priority**: 5 levels (Lowest → Highest); default **High**; set via the right-click screen
- Priority is saved to NBT and survives break/replace
- Recipe: iron bars ring around a chest (`III/ICI/III`)
- Pickaxe-minable

### Junk Drawer
- Stores up to **32,768 individual items**, one per slot (no stacking)
- Accepts **only** items that Manila Folders reject: damageable items (tools, armour, weapons) and items with non-default component data (enchanted books, named items, potions, etc.)
- No locking — any qualifying item may be freely added or removed at any time
- No player UI — automation only (hoppers, pipes, AE2 storage bus via `IItemHandler`)
- All slots exposed via `IItemHandler`; slot count is dynamic (grows/shrinks with contents)
- **Priority**: 5 levels (Lowest → Highest); default **Normal**; set via right-click screen
- Priority is saved to NBT and survives break/replace
- Drops itself with full contents intact when broken
- Recipe: iron blocks on corners, iron bars on edges, chest in center (`BIB/ICI/BIB`)
- Pickaxe-minable

### Bulk Storage Container
- Stores up to **32,768 total items** shared across any number of distinct item types
- Accepts **only** items that Manila Folders accept: non-damageable items with default component data (plain stackable items) — the precise complement of the Junk Drawer
- No locking — any qualifying item may be freely added or removed at any time
- No player UI — automation only (hoppers, pipes via `IItemHandler`)
- Slot count is dynamic (one slot per distinct stored type); each slot presents up to `maxStackSize` items for extraction
- **Priority**: 5 levels (Lowest → Highest); default **Low**; set via right-click screen
- Priority is saved to NBT and survives break/replace
- Drops itself with full contents intact when broken
- Recipe: iron bars on corners, iron blocks on edges, chest in center (`IBI/BCB/IBI`) — the Junk Drawer recipe with bars and blocks swapped
- Pickaxe-minable

### Fluid Tank
- Stores up to **1,024,000 mB** (1024 buckets) of a single fluid type
- No player UI — automation only (pumps, pipes via `IFluidHandler`)
- Locks to first fluid inserted; stays locked at 0 mB after drain
- Use Whiteout Tape (crafting grid or right-click) to clear the lock when empty
- Front face shows a **12×12 transparent window**; when filled, the fluid's texture is rendered inside it
- Window faces the direction the player is looking when placed (`FACING` blockstate)
- Drops itself with stored fluid data intact when broken
- Recipe: glass on top, junk drawer in middle, bucket on bottom (vertical shaped)
- Pickaxe-minable

---

## Items

### Manila Folder (7 tiers)
Single-item-type bulk storage carried as an item.

| Tier      | Capacity      | Recipe                                      |
|-----------|---------------|---------------------------------------------|
| Paper     | 4,096         | 5 paper in an L-shape → 8 folders           |
| Copper    | 16,384        | copper block (top) + copper ingots + paper folder (center) |
| Iron      | 65,536        | iron block + iron ingots + copper folder    |
| Gold      | 131,072       | gold block + gold ingots + iron folder      |
| Diamond   | 524,288       | diamond block + diamonds + gold folder      |
| Emerald   | 1,048,576     | emerald block + emeralds + diamond folder   |
| Netherite | 4,294,967,296 | netherite block + netherite ingots + emerald folder |

- Locks to the first item inserted via the crafting grid
- Tooltip shows current count, item name, and tier capacity
- Higher-tier folder textures have a 1px silhouette border in the tier's material colour

#### Crafting grid interactions
| Input | Output |
|-------|--------|
| Folder + matching item | Folder with count increased (up to capacity) |
| Folder with items | Items extracted (up to one stack), folder returned |
| Two same-type folders | Merged folder (combined count, capped at capacity; remainder returned) |
| Tape + locked-empty folder | Unlocked folder (tape loses 1 durability) |

### Whiteout Tape
- 32 durability (32 uses); does not leave a broken item — consumed entirely on last use
- **Crafting grid**: tape + locked-empty Manila Folder → unlocked folder (tape damaged by 1)
- **Crafting grid**: tape + locked-empty Fluid Tank item → unlocked tank item (tape damaged by 1)
- **Right-click on Fluid Tank**: if locked and amount == 0, clears the fluid lock (tape damaged by 1)
- **Right-click on Filing Cabinet**: clears all locked-but-empty folders in one use (tape damaged by 1); does nothing if no eligible folders found
- Creative players do not consume durability on right-click
- Recipe: paper ring around a slime ball and white dye

---

## Optional Dependencies

### Jade (WAILA) — `compileOnly`, optional at runtime
Adds block tooltip overlays when looking at IntelliStore blocks:

| Block | Tooltip |
|-------|---------|
| Filing Cabinet | Open / Closed state; item icon + "Xk of Yk ItemName" per occupied folder |
| Junk Drawer | "Empty" or "X / Yk items" |
| Bulk Storage Container | "Empty" or "Xk / Yk items" |
| Fluid Tank | "Empty" or "Xk mB of Yk mB FluidName" |

Counts are abbreviated: exact below 1000, `Xk` from 1,000, `XM` from 1,000,000.

### JEI — `compileOnly`, optional at runtime
All standard shaped/shapeless recipes (filing cabinet, folder tiers, junk drawer, whiteout tape,
fluid tank, bulk storage container) appear in JEI automatically. The custom crafting-grid recipes (folder storage, extract,
merge, tape) are intentionally not shown.

---

## Tubes

### Tube (16 colors)
- A 1/4-block × 1/4-block pipe that visually and logically connects to adjacent blocks
- Connects to: same-color tubes, and any block exposing an `IItemHandler` capability
- Different-colored tubes do **not** connect to each other
- Six blockstate boolean properties (`north`, `south`, `east`, `west`, `up`, `down`) drive collision shape and rendering
- Pre-computed 64-entry `VoxelShape` array for efficient collision at all connection states
- All visual geometry drawn by a custom `BlockEntityRenderer` (BESR): core cube, arms toward connected faces, and attachment plates
- Color tint applied at render time from `DyeColor.getTextureDiffuseColor()` — single shared white texture
- Recipe: iron bars – iron ingot – iron bars (horizontal row) → **8 white tubes**
- Dye recipe: any dye + any tube (shapeless) → **8 colored tubes** of that color
- Pickaxe-minable

### Storage Interface (tube attachment)
- Up to 6 attachments per tube (one per face), stored in the tube block entity
- **Right-click an empty tube face** → installs a Storage Interface on that face (visible as an 8×8×2-pixel plate)
- **Right-click an occupied face** → opens the Storage Interface priority screen
- Priority: 5 levels (Lowest → Highest); default **Normal**; attachment priority overrides the connected storage block's own priority
- Priority saved to NBT per-face and survives break/replace

### Tube Network
- All storage blocks reachable through same-color connected tubes form a **unified virtual inventory**
- Any block querying an `IItemHandler` capability on a tube receives a composite view of the entire network
- **Insertion** routes to the highest-priority storage first (AE2/RS style — the network decides, not the caller)
- **Priority source** (highest precedence first): Storage Interface attachment priority → storage block's own priority → Normal
- Storage blocks connected via multiple paths are deduplicated (counted once per network)
- Network view is built lazily on first capability access via BFS flood-fill and cached per tube; cache invalidates automatically when any neighbor changes
- Cascade prevention: cache-clear propagation stops immediately if a tube's cache is already stale
