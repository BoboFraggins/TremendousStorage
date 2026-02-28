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
- **Void Excess**: toggle in the right-click screen; when ON, the cabinet always accepts item inserts even when full, silently discarding overflow; saved to NBT; default OFF
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
- Locks to first fluid inserted; stays locked at 0 mB after drain
- Use Whiteout Tape (crafting grid or right-click) to clear the lock when empty
- Front face shows a **12×12 transparent window**; when filled, the fluid's texture is rendered inside it
- Window faces the direction the player is looking when placed (`FACING` blockstate)
- Right-click with empty hand → opens Tank Settings screen
- **Void Excess**: toggle in the settings screen; when ON, the tank always accepts fluid inserts even when full, silently discarding overflow; saved to NBT; default OFF
- Exposes `IFluidHandler` capability for pump/pipe automation
- Drops itself with stored fluid data intact when broken
- Recipe: glass on top, junk drawer in middle, bucket on bottom (vertical shaped)
- Pickaxe-minable

### Stirling Engine
- A heat-powered RF generator that converts heat from blocks placed below it into Redstone Flux
- **Heat sources and output rates**:
  - Lava: **50 FE/t**
  - Magma Block: **25 FE/t**
  - Lit Campfire or Lit Soul Campfire: **15 FE/t**
  - Any other block (or air): 0 FE/t
- **Internal buffer**: 100,000 FE
- Pushes up to 100 FE/t to each adjacent block exposing an `IEnergyStorage` capability (including tubes)
- Exposes `IEnergyStorage` capability for extraction by external conduits/cables
- Client-animated spinning copper flywheel when the engine is actively generating
- Jade tooltip: "Place Above Heat Source" when idle; stored / max FE when generating
- Recipe: iron nuggets in the four cardinal faces, stick in center, water bucket bottom-left, lava bucket bottom-right
- Pickaxe-minable

### Wireless Hub
- A linking station that pairs an unlinked Wireless SAT item with a reachable Network Interface
- Right-click → opens a 2-slot UI: left slot accepts an unlinked Wireless SAT; right slot is output-only
- Performs a BFS scan through the connected tube network to find the nearest reachable Network Interface
- If a valid NI is found, writes its position into the Wireless SAT item and moves it to the output slot automatically
- Drops itself (with inventory contents) intact when broken
- Recipe: Iron Ingot (top) / Paper Manila Folder (middle) / Ender Pearl (bottom) — vertical shaped
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

### Storage Interface
- A consumable attachment item placed on tube faces to connect storage blocks to the tube network
- Right-click an **empty tube face with a Storage Interface item in hand** → installs it on that face as an 8×8×2-pixel plate; consumes 1 item (not consumed in Creative)
- Right-click an **installed plate with empty hand** → opens the Storage Interface priority screen
- **Left-click (punch)** an installed plate → removes the plate and drops 1 Storage Interface item
- **Breaking the tube** → drops 1 Storage Interface item per installed face (in addition to the tube item)
- **Priority**: 5 levels (Lowest → Highest); default **Normal**; attachment priority overrides the connected storage block's own priority; priority is stored as a data component on the item and survives break/re-attach
- Recipe: 4× (cross of Iron Ingots with a Redstone Comparator in the center)

### Import Interface
- A consumable attachment item that, when installed on a tube face, automatically pulls items from the adjacent external inventory into the tube network
- Rendered as a **blue** 8×8×2-pixel plate on the tube face
- Right-click an **empty tube face with an Import Interface item in hand** → installs it; consumes 1 item (not consumed in Creative)
- Right-click an **installed plate with empty hand** → opens the filter screen
- **Left-click (punch)** an installed plate → removes the plate and drops 1 Import Interface item with filter data stored on it
- **Breaking the tube** → drops 1 Import Interface item per installed face with filter data intact
- **Transfer rate**: up to 64 items per operation, every 20 ticks (1 second); one slot scanned per cycle
- **Filter**: 9 ghost-item slots; click a slot with an item in hand to set it, click with empty hand to clear it; items can also be dragged from JEI without consuming them
- **Mode toggle**: Accept (transfers only items matching the filter) or Reject (transfers all items except those matching the filter); empty Accept filter = transfer all; empty Reject filter = transfer nothing
- Filter state and mode are stored as a data component on the item and survive break/re-attach
- Recipe: 4× (cross of Iron Ingots with Blue Dye in the center)

### Export Interface
- A consumable attachment item that, when installed on a tube face, automatically pulls items from the tube network and pushes them into the adjacent external inventory
- Rendered as a **red** 8×8×2-pixel plate on the tube face
- Right-click an **empty tube face with an Export Interface item in hand** → installs it; consumes 1 item (not consumed in Creative)
- Right-click an **installed plate with empty hand** → opens the filter screen
- **Left-click (punch)** an installed plate → removes the plate and drops 1 Export Interface item with filter data stored on it
- **Breaking the tube** → drops 1 Export Interface item per installed face with filter data intact
- **Transfer rate**: up to 64 items per operation, every 20 ticks (1 second); one slot scanned per cycle
- **Priority respected**: export pulls from the highest-priority storage in the network first
- **Filter**: identical to Import Interface — 9 ghost-item slots with Accept/Reject mode toggle; JEI drag supported
- Filter state and mode are stored as a data component on the item and survive break/re-attach
- Recipe: 4× (cross of Iron Ingots with Red Dye in the center)

### Placer Interface
- A consumable attachment item that, when installed on a tube face, automatically places blocks from the network into the world
- Rendered as a **green** 8×8×2-pixel plate on the tube face
- Right-click an **empty tube face with a Placer Interface item in hand** → installs it; consumes 1 item (not consumed in Creative)
- Right-click an **installed plate with empty hand** → opens the filter screen
- **Left-click (punch)** an installed plate → removes the plate and drops 1 Placer Interface item with filter data stored on it
- **Breaking the tube** → drops 1 Placer Interface item per installed face with filter data intact
- **Filter**: single ghost-item slot; must be a block item (something that can be placed in the world)
- Every **5 ticks**: if the block directly in front of the plate is air, extracts 1 matching item from the network and places it as a block
- If no filter is set, the Placer does nothing
- No recipes yet

### Breaker Interface
- A consumable attachment item that, when installed on a tube face, automatically breaks blocks in front of it and inserts the drops into the network
- Rendered as a **yellow** 8×8×2-pixel plate on the tube face
- Right-click an **empty tube face with a Breaker Interface item in hand** → installs it; consumes 1 item (not consumed in Creative)
- Right-click an **installed plate with empty hand** → opens the filter screen
- **Left-click (punch)** an installed plate → removes the plate and drops 1 Breaker Interface item with filter data stored on it
- **Breaking the tube** → drops 1 Breaker Interface item per installed face with filter data intact
- **Filter**: single ghost-item slot; if set, only breaks blocks whose default drop matches the filter; if empty, breaks any block
- **Silk Touch toggle**: button in the filter screen; when ON, drops are collected as if mined with Silk Touch; default OFF
- Every **5 ticks**: if the block directly in front matches the filter, breaks it and inserts all drops into the network (only proceeds if all drops fit)
- Filter and Silk Touch state are stored as a data component on the item and survive break/re-attach
- No recipes yet

### Wireless SAT
- A portable Storage Access Terminal carried as an item (stack size 1)
- Tooltip shows the linked Network Interface coordinates, or "Unlinked" if not yet configured
- Right-click (while linked and in range) → opens the full SAT UI: scrollable network item list, 3×3 crafting grid, player inventory
- Linked NI position stored as a data component — survives break/unload/reload
- Recipe (shapeless): Ender Pearl + Storage Access Terminal item → 1× Wireless SAT (unlinked)

### Personal Filing Cabinet
- A portable item (stack size 1) that holds up to **8 Manila Folders** of any tier
- Right-click in air → opens the Personal Filing Cabinet screen: 8 folder slots (2×4 grid) + player inventory
- Drag any locked Manila Folder into a slot; the folder stays inside the item as a data component
- **Auto-pickup**: when the player walks over a dropped item that matches a locked folder's item type, it is automatically routed into that folder instead of the regular inventory
- Only **locked** folders (already containing an item type) participate in auto-pickup; unlocked folders are skipped
- **Void Excess: ON** (default) — if the folder is full, overflow from pickup is silently discarded; the pickup event is denied so nothing goes to the regular inventory
- **Void Excess: OFF** — only actually-inserted items are consumed; surplus stays on the ground and is picked up normally by the inventory
- Toggle button in the screen switches Void Excess ON/OFF (persisted on the item)
- Multiple PFC items in inventory: all are checked in slot order; the first matching folder wins
- All folder contents and Void Excess state are stored as a data component on the PFC item — survive break/pick-up and log-out/log-in
- Recipe: Chest surrounded by 8 Paper Manila Folders (`FFF/FCF/FFF`)

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

### Network Interface
- A two-block-tall block (lower + upper half) that acts as the coordinator for a storage network
- **Lower half**: gray block body with a 1/4×1/4 status dot on each face; **upper half**: glass
- Status dot is **green** when the network is valid (exactly one NI connected), **red** otherwise
- Connects to all tube colors; BFS scans the entire reachable tube network on first access
- Exposes an `IItemHandler` capability over the whole network: inserts to highest-priority storage first, extracts from lowest-priority storage first
- Lazy-cached network scan; cache invalidates automatically when any neighbor changes
- Right-click → opens the **Network Interface screen**: shows validity status and a scrollable list of all attached blocks (e.g. "Filing Cabinet (2)", "Black Tubes (13)")
- **Sneak + right-click** → deposit all items from the player's inventory into the network (items are routed per the normal priority system; anything that doesn't fit stays in the inventory)
- Breaking either half removes both halves and drops one Network Interface item
- Recipe: Glass (top) / Brain (center) / Healing Salve Bucket (bottom) — vertical shaped
- Pickaxe-minable
- **Power**: requires FE to operate; base cost **5 FE/t** plus per-component costs (see Tube Network — Power)
- Accepts FE via `IEnergyStorage` capability (internal buffer: 100,000 FE); compatible with Stirling Engine, Pipez power pipes, Mekanism cables, Powah conduits, etc.
- Any tube in the connected network also exposes `IEnergyStorage`, so power can be injected through any tube face
- When underpowered the network becomes **inactive**: SAT and Wireless SAT cannot be opened, tube Import/Export attachments stop transferring, and the status dot turns red
- Jade tooltip: total FE/t consumed by the network; "Not Enough Power" in red when inactive

### Network Interface — Prerequisite Items & Fluid

#### Zombie Brain
- 20% drop from all Zombie-type mobs (Zombie, Husk, Drowned, Zombie Villager, etc.)

#### Healing Salve (fluid)
- Lava-like flowing fluid — does **not** form infinite source blocks; slow flow speed
- Created by right-clicking a **water-filled cauldron** with a **Glistering Melon Slice** (melon consumed, cauldron becomes Healing Salve Cauldron)
- Pick up with an empty bucket to get a **Healing Salve Bucket**

#### Brain
- Crafted by right-clicking a **Healing Salve Cauldron** with a **Zombie Brain**
- Both the Zombie Brain and the Healing Salve are consumed; the Brain pops out as an item entity

### Storage Access Terminal
- A single-block UI entry point for the tube storage network
- Right-click → opens a 176×256 screen with three sections stacked vertically:
  - **Top (scrollable item list)**: shows every item in the connected network, aggregated by type, sorted by count (desc) then name (asc); each row shows item icon, name, and abbreviated total count (e.g. `1.2k`, `3.5M`)
  - **Middle**: standard 3×3 crafting grid + result slot (craft from items pulled from the network list)
  - **Bottom**: player inventory (3×9) + hotbar
- **Click** a network row → extracts one stack (up to max stack size) into the player's inventory
- **Shift-click** a network row → extracts the full available count (up to max stack size) at once
- **Shift-click** a player inventory slot → inserts that slot's entire stack into the network
- Connects to the nearest Network Interface reachable by BFS through the tube network (any color)
- Shows "Network: Connected" (green) / "Network: Not Connected" (orange) status line
- No block entity — the NI lookup happens once at menu-open time and is passed to the client via the packet buffer
- Breaking drops 1 Storage Access Terminal item
- Recipe: vanilla Crafting Table surrounded by 8 Paper Manila Folders (`FFF/FCF/FFF`)
- Pickaxe-minable

### Tube Network
- All storage blocks reachable through the tube network form a **unified virtual inventory**
- Any block querying an `IItemHandler` capability on a tube receives a composite view of the entire network
- **Insertion** routes to the highest-priority storage first (AE2/RS style — the network decides, not the caller)
- **Priority source** (highest precedence first): Storage Interface attachment priority → storage block's own priority → Normal
- Storage blocks connected via multiple paths are deduplicated (counted once per network)
- Network view is built lazily on first capability access via BFS flood-fill and cached per tube; cache invalidates automatically when any neighbor changes
- Cascade prevention: cache-clear propagation stops immediately if a tube's cache is already stale

### Network Connectivity
The following IntelliStore blocks act as **color-agnostic network connectors**: Filing Cabinet, Junk Drawer, Bulk Storage Container, Storage Access Terminal, Wireless Hub, and Stirling Engine.

When the BFS encounters one of these blocks as a tube neighbor it:
1. Collects the block's `IItemHandler` as a storage endpoint (same as always)
2. **Continues the flood-fill** through all of that block's adjacent tubes — regardless of tube color

This allows different-colored tube runs connected through a shared connector block to form a single unified network:

```
[red tube] — [Filing Cabinet] — [blue tube] — [NI]
```

- Tube color is a topological property of the tube itself; different colors never connect *directly* to each other
- Color separation is reserved for future auto-crafting routing; it does **not** create isolated networks
- There is one singular network per Network Interface; all connected storage is always in the same pool

### Tube Network — Power
The network draws FE from the Network Interface's internal buffer each tick. Total cost is the sum of all connected components:

| Component | Cost |
|-----------|------|
| Network Interface (base) | 5 FE/t |
| Storage Access Terminal | 5 FE/t each |
| Wireless Hub | 25 FE/t each |
| Each tube attachment (Storage / Import / Export / Placer / Breaker Interface installed on a tube face) | 1 FE/t each |

- When the buffer runs dry the network becomes **inactive** (see Network Interface — Power above)
- Jade shows "Not Enough Power" in red on any tube with an attachment, on the SAT, and on the NI itself when inactive

---

## Optional Dependencies

### Jade (WAILA) — compileOnly, optional at runtime
Adds block tooltip overlays when looking at IntelliStore blocks:

| Block | Tooltip |
|-------|---------|
| Filing Cabinet | Open / Closed state; item icon + "Xk of Yk ItemName" per occupied folder |
| Junk Drawer | "Empty" or "X / Yk items" |
| Bulk Storage Container | "Empty" or "Xk / Yk items" |
| Fluid Tank | "Empty" or "Xk mB of Yk mB FluidName" |
| Stirling Engine | "Place Above Heat Source" when idle; "X / Y FE" when generating |
| Network Interface | Total FE/t consumed; "Not Enough Power" in red when inactive |
| Storage Access Terminal | "Not Enough Power" in red when network is inactive |
| Tube (with attachment) | "Not Enough Power" in red when network is inactive |

Counts are abbreviated: exact below 1000, `Xk` from 1,000, `XM` from 1,000,000.

### JEI — compileOnly, optional at runtime
- All standard shaped/shapeless recipes (filing cabinet, folder tiers, junk drawer, whiteout tape, fluid tank, bulk storage container, storage interface, wireless hub, wireless sat) appear in JEI automatically
- The custom crafting-grid recipes (folder insert/extract/merge, tape unlock) are intentionally not shown
- The Storage Access Terminal appears as a crafting station catalyst alongside crafting tables
- JEI's "+" button on any crafting recipe auto-fills the SAT's 3×3 crafting grid

### Mekanism — compileOnly, optional at runtime
Adds the **Gas Tank** block:
- Stores up to **128,000 mB** of a single Mekanism chemical type (gas, slurry, infuse type, pigment)
- Locks to the first chemical inserted; stays locked at 0 mB after drain
- Right-click with a Mekanism portable gas tank or any `IChemicalHandler` item → fills or drains the block
- Right-click with empty hand → opens Tank Settings screen
- **Void Excess**: when ON, silently discards chemical overflow; saved to NBT; default OFF
- Exposes `IChemicalHandler` block capability: compatible with Mekanism pressure tubes and Pipez gas pipes
- Drops itself with stored chemical data intact when broken
- Recipe (requires Mekanism): Glass (top) / Junk Drawer (middle) / Mekanism Basic Gas Tank (bottom) — vertical shaped
- Pickaxe-minable

### Ars Nouveau — compileOnly, optional at runtime
Adds the **Source Tank** block:
- Stores up to **100,000 source** — 10× the capacity of a vanilla Ars Nouveau Source Jar (10,000)
- Source is a plain integer — no type locking; the tank is always open to any source
- Right-click with empty hand → opens Tank Settings screen
- **Void Excess**: when ON, silently discards source overflow; saved to NBT; default OFF
- Exposes `ISourceCap` block capability: compatible with Ars Nouveau source relays, arcane pedestals, obelisks, and any other source-network block
- Drops itself with stored amount intact when broken
- Recipe (requires Ars Nouveau): Glass (top) / Junk Drawer (middle) / Ars Nouveau Source Jar (bottom) — vertical shaped
- Pickaxe-minable

### Curios — compileOnly, optional at runtime
- The Wireless SAT item is recognised in Curios accessory slots
- The SAT screen remains open while the Wireless SAT is equipped in any Curios slot (in addition to main inventory and off-hand)
