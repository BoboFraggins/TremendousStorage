# IntelliStore

A tech-themed storage mod for Minecraft 1.21.1 (NeoForge), built around the office filing metaphor.
Store enormous quantities of a single item type, or organize a myriad of items you only have one or
two of — all in a tidy Filing Cabinet full of Manila Folders.

## Features

### Manila Folders
A Manila Folder is a single inventory item that holds a large quantity of one item type. Folders
come in tiered materials with increasing capacity:

| Tier      | Default Capacity  |
|-----------|-------------------|
| Paper     | 4,096             |
| Copper    | 16,384            |
| Iron      | 65,536            |
| Gold      | 131,072           |
| Diamond   | 524,288           |
| Emerald   | 1,048,576         |
| Netherite | 4,294,967,296     |

All tier limits are configurable via the server config.

A fresh folder accepts any item. Once an item is placed inside, the folder locks to that item type.
Items can be inserted into and extracted from a folder using the crafting grid.

### Filing Cabinets
A Filing Cabinet is a block that holds up to 8 Manila Folders. Right-click to open or close it.
While open, right-click with a folder in hand to insert it, or right-click with an empty hand to
extract a folder.

## Planned
- Fluid folders (store a single fluid type, measured in mB)
- Mekanism gas storage (soft dependency)
- JEI/REI recipe integration
- WAILA / The One Probe tooltips

## Credits

IntelliStore is inspired by and derived in part from
**[Real Filing Cabinet](https://github.com/bafomdad/realfilingcabinet)** by
[bafomdad](https://github.com/bafomdad), which is licensed under the
[MIT License](https://github.com/bafomdad/realfilingcabinet/blob/master/License.md).

The core folder storage concept, crafting grid insert/extract behavior, and filing cabinet block
interaction model all originate from that project. Thank you to bafomdad for the original work.

## License

MIT License — Copyright (c) 2026 BoboFraggins. See [LICENSE](LICENSE) for details.
