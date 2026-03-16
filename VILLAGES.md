# Adding Custom Buildings to Vanilla Villages

A specification for NeoForge 1.21.1 mods that want to inject custom
structures into vanilla (and modded) village generation without
overwriting existing data files.

---

## Overview

Minecraft villages are assembled at world-generation time from a pool
of weighted **structure pieces** using the **Jigsaw** system. Each
village type (plains, desert, savanna, snowy, taiga) draws pieces from
named **template pools**. To add a custom building to a village, you
need to:

1. Build the structure and export it as an `.nbt` template file.
2. Place **Jigsaw blocks** inside it so the game knows how it connects
   to the village street grid.
3. Register it as an entry in the appropriate vanilla template pool
   using NeoForge's additive pool modifier system.
4. Optionally apply **structure processors** for aging effects, biome-
   appropriate block substitution, or loot table injection.

---

## 1. Structure Templates (.nbt files)

A structure template is an NBT snapshot of a region created with a
**Structure Block** in-game (`/give @s structure_block`).

### Workflow

1. Build your structure in a creative world.
2. Place a Structure Block, set it to **Save** mode, define the
   bounding box, and click **Save**.
3. The file is written to
   `saves/<world>/generated/<namespace>/structures/<name>.nbt`.
4. Copy it into your mod's resources at:
   ```
   src/main/resources/data/<namespace>/structures/<path>.nbt
   ```

### Sizing conventions (matching vanilla villages)

| Building type  | Typical footprint |
|----------------|-------------------|
| Small house    | 7 × 7 – 9 × 9     |
| Medium house   | 9 × 9 – 12 × 12   |
| Large building | 12 × 12 – 16 × 16 |
| Street segment | 3 wide, variable length |

Keep the Y origin at ground level (y = 0 inside the template = the
surface the building sits on).

---

## 2. Jigsaw Blocks

**Jigsaw blocks** (`minecraft:jigsaw`) are the connectors that let the
game stitch pieces together. They are invisible in normal play and are
removed after world generation completes.

### Properties

Each Jigsaw block carries three key fields (set in the block entity):

| Field | Purpose |
|-------|---------|
| **Name** | The "socket" this block exposes on the current piece. |
| **Target** | The socket name it wants to connect to on the *next* piece. |
| **Pool** | The template pool the game picks the next piece from. |
| **Joint type** | `rollable` (piece can rotate) or `aligned` (fixed orientation). |
| **Final state** | The block that replaces the Jigsaw block after generation. |

### Connecting a building to the village street

Every vanilla village piece that attaches to the road grid uses:

```
Name:   minecraft:building_entrance
Target: minecraft:path_join_point
Pool:   minecraft:village/<type>/terminators
```

Your building's **door-side** Jigsaw block should mirror this:

```
Name:   minecraft:building_entrance
Target: minecraft:path_join_point
Pool:   minecraft:village/<type>/terminators
Joint:  rollable
Final state: air  (or the threshold block of your doorway)
```

Place this block at the **outer edge of the doorstep**, oriented so
its arrow points *away* from the building (outward into the street).

### Interior Jigsaw blocks (optional)

If your building has multiple floors or modular add-ons, you can chain
further Jigsaw blocks inside the structure pointing to your own pools.

---

## 3. Template Pools

A **template pool** is a JSON file that lists weighted structure pieces.
The game samples from this list (with replacement) each time it needs
to place a new piece during village assembly.

### File location

```
data/<namespace>/worldgen/template_pool/<pool_name>.json
```

### Schema

```json
{
  "fallback": "minecraft:empty",
  "elements": [
    {
      "weight": 1,
      "element": {
        "element_type": "minecraft:single_pool_element",
        "location": "<namespace>:<path/to/structure>",
        "projection": "rigid",
        "processors": "<namespace>:<processor_list_id>"
      }
    }
  ]
}
```

#### element_type values

| Type | Use |
|------|-----|
| `minecraft:single_pool_element` | One fixed .nbt template |
| `minecraft:list_pool_element` | Multiple templates placed together |
| `minecraft:legacy_single_pool_element` | Vanilla-style with hardcoded processors |
| `minecraft:empty_pool_element` | Intentional empty slot (for sparsity) |

#### projection values

| Value | Effect |
|-------|--------|
| `rigid` | Structure placed exactly as exported (terrain not smoothed) |
| `terrain_matching` | Bottom row adapts to terrain height |

Use `rigid` for buildings on flat pads; `terrain_matching` for paths
and fences.

---

## 4. Injecting Into Existing Village Pools (NeoForge)

Overwriting a vanilla template pool JSON in your mod would replace the
*entire* pool, wiping out vanilla houses. NeoForge provides an additive
mechanism instead.

### Registered structure pool modifier

Create a class that implements `IStructurePoolModifier` and is
registered as a `StructurePoolModifier` via `DeferredRegister`:

```java
DeferredRegister<StructurePoolModifier> MODIFIERS =
    DeferredRegister.create(NeoForgeRegistries.Keys.STRUCTURE_POOL_MODIFIERS, MODID);
```

The modifier receives the pool's `ResourceLocation` and a mutable list
of elements. Append your entry only for the pools you care about:

```java
@Override
public void modify(ResourceLocation poolId,
                   List<Pair<StructurePoolElement, Integer>> elements,
                   RegistryAccess registryAccess) {

    if (!poolId.equals(ResourceLocation.withDefaultNamespace("village/plains/houses"))) return;

    StructureTemplatePool.Projection projection =
        StructureTemplatePool.Projection.RIGID;

    StructurePoolElement entry = SinglePoolElement
        .single(MODID + ":village/plains/my_building", myProcessorListHolder)
        .apply(projection);

    elements.add(Pair.of(entry, WEIGHT));
}
```

Register the modifier on the mod event bus the same way as any other
deferred register.

### Data-driven alternative (datapacks / other mods)

If you want your additions to be overridable by server operators, you
can expose them as a datapack layer. Place your pool modifier
configuration under:

```
data/<namespace>/neoforge/structure_pool_modifiers/<name>.json
```

The exact schema for this file is defined by NeoForge's codec for
`StructurePoolModifier` and varies by modifier type.

### Vanilla pool IDs for each village type

| Biome | House pool | Street pool |
|-------|-----------|-------------|
| Plains | `minecraft:village/plains/houses` | `minecraft:village/plains/streets` |
| Desert | `minecraft:village/desert/houses` | `minecraft:village/desert/streets` |
| Savanna | `minecraft:village/savanna/houses` | `minecraft:village/savanna/streets` |
| Snowy | `minecraft:village/snowy/houses` | `minecraft:village/snowy/streets` |
| Taiga | `minecraft:village/taiga/houses` | `minecraft:village/taiga/streets` |

To add to all five, call the modifier for each pool ID in turn.

---

## 5. Structure Processors

**Structure processors** transform blocks within a template during
placement. They run in sequence; the output of each feeds the next.

### File location

```
data/<namespace>/worldgen/processor_list/<name>.json
```

### Schema

```json
{
  "processors": [
    { "processor_type": "...", ... },
    { "processor_type": "...", ... }
  ]
}
```

### Common processor types

#### Block degradation / random replacement
`minecraft:rule`

Replaces one block type with another at a given probability:

```json
{
  "processor_type": "minecraft:rule",
  "rules": [
    {
      "input_predicate":  { "predicate_type": "minecraft:block_match", "block": "minecraft:cobblestone" },
      "output_state":     { "Name": "minecraft:mossy_cobblestone" },
      "position_predicate": { "predicate_type": "minecraft:always_true" },
      "location_predicate": { "predicate_type": "minecraft:always_true" },
      "chance": 0.2
    }
  ]
}
```

#### Biome-appropriate wood substitution

Vanilla uses this to swap oak planks for spruce in taiga villages.
Apply the same approach to substitute your own decorative blocks per
biome. Use separate processor lists per village type and reference the
appropriate one in each pool entry.

#### Loot table injection
`minecraft:block_entity_mod_data`

Sets NBT on containers (chests, barrels) placed during generation so
they generate loot from a loot table on first open:

```json
{
  "processor_type": "minecraft:block_entity_mod_data",
  "rules": [
    {
      "input_predicate": { "predicate_type": "minecraft:block_match", "block": "minecraft:chest" },
      "output_nbt": { "LootTable": "<namespace>:<loot_table_path>" }
    }
  ]
}
```

#### Capping structure height to avoid air columns
`minecraft:gravity`

Drops blocks downward until they hit a solid surface, preventing
floating blocks when the village generates on uneven terrain:

```json
{
  "processor_type": "minecraft:gravity",
  "heightmap": "WORLD_SURFACE_WG",
  "offset": 0
}
```

---

## 6. Loot Tables for Chests

Place your loot table at:

```
data/<namespace>/loot_table/chests/<name>.json
```

Reference it from the processor list (see §5) using the full resource
location `<namespace>:chests/<name>`.

A minimal loot table:

```json
{
  "type": "minecraft:chest",
  "pools": [
    {
      "rolls": { "type": "minecraft:uniform", "min": 3, "max": 6 },
      "entries": [
        {
          "type": "minecraft:item",
          "name": "<namespace>:<item>",
          "weight": 10,
          "functions": [
            { "function": "minecraft:set_count", "count": { "min": 1, "max": 3, "type": "minecraft:uniform" } }
          ]
        }
      ]
    }
  ]
}
```

---

## 7. Villager Professions and Trade Workstations

If your building contains a custom workstation block, you can associate
a villager profession with it:

1. Register a `VillagerProfession` bound to your workstation's
   `PoiType` (Point of Interest).
2. Register trade offer providers via `VillagerTradesEvent` on the
   NeoForge event bus (not the mod event bus).
3. The profession will cause unemployed villagers to path-find to your
   workstation and claim the job.

The `PoiType` must declare the set of block states that count as
"this workstation" and the number of tickets it provides.

---

## 8. Testing

### Fast iteration
Use the `/place structure <namespace>:<path>` command to place your
template at your feet without needing village generation. Check that
Jigsaw blocks are oriented correctly before committing the file.

### Village generation
Use `/locate structure minecraft:village_plains` (or the appropriate
type) to find a village quickly, then inspect it with
`/data get block <x> <y> <z>` on any suspicious block.

### Verifying pool injection
Enable `--debug-jigsaw` JVM flag or use the `F3` overlay during
generation to see which pool each piece was drawn from.

### Seed locking for reproducibility
Set a fixed `level-seed` in `server.properties` and delete/recreate
the world between test runs so you always generate the same village
layout.

---

## 9. File Checklist

| File | Required |
|------|----------|
| `data/<ns>/structures/village/<type>/<name>.nbt` | Yes |
| `data/<ns>/worldgen/template_pool/<pool>.json` | Only if creating a *new* pool |
| `data/<ns>/worldgen/processor_list/<name>.json` | Yes (can use `minecraft:empty` list) |
| `data/<ns>/loot_table/chests/<name>.json` | If building has chests |
| Java: `IStructurePoolModifier` implementation | Yes (to inject into existing pools) |
| Registration in mod event bus | Yes |

---

## 10. Weights and Rarity

The probability that your building appears in any given village is
approximately:

```
P(your building) = your_weight / (sum of all weights in the pool)
```

Vanilla house pools typically have a total weight in the range of
30–80 depending on village type. Adding a weight-1 entry to a pool
with total weight 50 gives roughly a 2% chance per "house slot"
attempted. Since a village generates 4–12 house slots, most villages
will have 0–1 of your building, which is appropriate for rare or
specialised structures.

Use higher weights (3–5) if the building is a common feature like an
extra well or market stall; keep it at 1 for a unique landmark.
