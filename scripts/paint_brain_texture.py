#!/usr/bin/env python3
"""
paint_brain_texture.py — Copy brain.png (cropped to its non-transparent region)
into brain_texture.png and update the embedded base64 in brain.bbmodel.

Because stl_to_bbmodel.py --from-image bakes UV coordinates that map each voxel's
(vy, vz) index directly to a texture pixel, the texture just needs to BE the
cropped image (resized to the texture resolution if voxel counts differ slightly).

West-view convention already baked into the UVs:
  col = nz − 1 − vz   →  south on left, north on right
  row = ny − 1 − vy   →  Y-max at top

If the image appears mirrored, run with --flip-x.

Dependencies:
    pip install Pillow numpy

Usage:
    python paint_brain_texture.py
    python paint_brain_texture.py --flip-x
"""

import argparse
import base64
import json
from io import BytesIO

import numpy as np
from PIL import Image

BBMODEL   = "blockbench/brain.bbmodel"
BRAIN_PNG = "src/main/resources/assets/tremendousstorage/textures/item/brain.png"
TEX_PNG   = "src/main/resources/assets/tremendousstorage/textures/block/brain_texture.png"


def non_transparent_crop(path: str) -> np.ndarray:
    """Load an image and return the tightest crop of non-transparent pixels."""
    arr = np.array(Image.open(path).convert("RGBA"))
    alpha = arr[:, :, 3]
    rows = np.any(alpha > 0, axis=1)
    cols = np.any(alpha > 0, axis=0)
    r0, r1 = np.where(rows)[0][[0, -1]]
    c0, c1 = np.where(cols)[0][[0, -1]]
    print(f"  Crop: rows {r0}–{r1} ({r1-r0+1}px tall), cols {c0}–{c1} ({c1-c0+1}px wide)")
    return arr[r0:r1+1, c0:c1+1]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--flip-x", action="store_true",
                        help="Flip the image horizontally before writing (if mapping looks mirrored)")
    args = parser.parse_args()

    with open(BBMODEL) as f:
        model = json.load(f)

    tex_w = model["resolution"]["width"]
    tex_h = model["resolution"]["height"]
    print(f"Texture resolution from bbmodel: {tex_w}w × {tex_h}h")

    print(f"Cropping {BRAIN_PNG} ...")
    cropped = non_transparent_crop(BRAIN_PNG)   # shape (nh, nw, 4)

    if args.flip_x:
        cropped = cropped[:, ::-1, :]
        print("  Applied --flip-x")

    # Resize to texture resolution using nearest-neighbour (keeps pixel crispness).
    if cropped.shape[:2] != (tex_h, tex_w):
        print(f"  Resizing {cropped.shape[1]}×{cropped.shape[0]} → {tex_w}×{tex_h}")
        pil = Image.fromarray(cropped).resize((tex_w, tex_h), Image.NEAREST)
        cropped = np.array(pil)

    # Any fully-transparent pixel gets a neutral fallback so interior voxels are visible.
    fallback = np.array([180, 120, 100, 255], dtype=np.uint8)
    mask = cropped[:, :, 3] == 0
    if mask.any():
        print(f"  Transparent fallback applied to {mask.sum()} pixels")
        cropped[mask] = fallback

    out_img = Image.fromarray(cropped, "RGBA")
    out_img.save(TEX_PNG)
    print(f"Saved → {TEX_PNG}")

    buf = BytesIO()
    out_img.save(buf, format="PNG")
    b64 = "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()

    for tex in model["textures"]:
        if tex.get("id") == "0":
            tex["source"] = b64
            break

    with open(BBMODEL, "w") as f:
        json.dump(model, f, indent=2)
    print(f"Updated embedded texture in {BBMODEL}")


if __name__ == "__main__":
    main()
