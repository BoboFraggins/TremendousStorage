# TODO

## Sky Block depth-only rendering

The sky block's "window to the skybox" effect is broken on NeoForge 21.1.224+.

`RegisterNamedRenderTypesEvent` now requires the block render type to be a chunk buffer layer
(`solid`, `cutout`, `translucent`, etc.). The original custom depth-only render type (which wrote
only to the depth buffer so the skybox background showed through) is no longer permitted.

As a workaround, the block now uses `RenderType.cutout()`. Transparent texture pixels will be
discarded, but the depth-only sky effect is gone — you'll see through to whatever geometry is
behind the block rather than the skybox specifically.

**Fix options:**
- Use a block entity renderer (BERs have full RenderType freedom and run outside the chunk buffer)
- Implement a custom `BakedModel` via a model loader and override `getRenderTypes()` — note that
  `ChunkRenderTypeSet` still restricts you to chunk layers for the world pass, so a BER is likely
  the cleaner path
