#!/usr/bin/env python3
"""
stl_to_bbmodel.py  —  Convert an STL file to a hollow voxel Blockbench .bbmodel.

Dependencies:
    pip install trimesh scipy numpy

Usage:
    python stl_to_bbmodel.py brain.stl brain.bbmodel
    python stl_to_bbmodel.py brain.stl brain.bbmodel --max-size 12
"""

import argparse
import base64
import json
import os
import struct
import sys
import uuid as _uuid
import zlib

import numpy as np
import trimesh
from scipy.ndimage import binary_erosion


# 6-connected structuring element for erosion — gives a proper 1-voxel-thick shell.
_SHELL_STRUCT = np.array(
    [[[0, 0, 0], [0, 1, 0], [0, 0, 0]],
     [[0, 1, 0], [1, 1, 1], [0, 1, 0]],
     [[0, 0, 0], [0, 1, 0], [0, 0, 0]]],
    dtype=bool,
)


def load_mesh(path: str) -> trimesh.Trimesh:
    mesh = trimesh.load(path, force="mesh")
    if not isinstance(mesh, trimesh.Trimesh):
        raise SystemExit(f"error: could not load a single mesh from {path!r}")
    return mesh


def voxelize_surface(mesh: trimesh.Trimesh, pitch: float) -> np.ndarray:
    """Return a boolean (nx, ny, nz) array; True = surface voxel."""
    voxel_grid = mesh.voxelized(pitch)
    try:
        solid = voxel_grid.fill().matrix
    except Exception:
        print("  Warning: mesh may not be watertight; using raw voxelization.")
        solid = voxel_grid.matrix
    interior = binary_erosion(solid, structure=_SHELL_STRUCT)
    return solid & ~interior


def pitch_from_max_size(mesh: trimesh.Trimesh, max_size: int) -> float:
    return mesh.extents.max() / max_size


def pitch_from_image(mesh: trimesh.Trimesh, img_path: str):
    """
    Non-uniformly scale the mesh so that after voxelization with pitch=1:
      Y voxels ≈ image non-transparent height  (west-view vertical)
      Z voxels ≈ image non-transparent width   (west-view horizontal)
    Returns (scaled_mesh, nw, nh).
    """
    from PIL import Image as _PIL
    img = _PIL.open(img_path).convert("RGBA")
    alpha = np.array(img)[:, :, 3]
    rows = np.any(alpha > 0, axis=1)
    cols = np.any(alpha > 0, axis=0)
    r0, r1 = np.where(rows)[0][[0, -1]]
    c0, c1 = np.where(cols)[0][[0, -1]]
    nh = int(r1 - r0 + 1)   # target mesh-Z voxels (→ display vertical)
    nw = int(c1 - c0 + 1)   # target mesh-Y voxels (→ display horizontal)
    print(f"  Image non-transparent: {nw}w × {nh}h  (rows {r0}–{r1}, cols {c0}–{c1})")

    # After rotation [X=90, Z=180]: mesh Y → display Z (horizontal), mesh Z → display Y (up).
    # So image width (nw) drives mesh-Y and image height (nh) drives mesh-Z.
    sy = nw / mesh.extents[1]   # scale so Y extent → nw units (horizontal)
    sz = nh / mesh.extents[2]   # scale so Z extent → nh units (vertical)
    sx = (sy + sz) / 2           # average scale for X
    scaled = mesh.copy()
    scaled.apply_scale([sx, sy, sz])
    return scaled, nw, nh


def _make_blank_png(width: int = 16, height: int = 16) -> str:  # noqa: E501
    """Return a base64 data URI for a solid white PNG (no external deps)."""
    def png_chunk(tag: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    ihdr = png_chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0))
    row = b"\x00" + b"\xff\xff\xff" * width   # filter byte 0 + white RGB pixels
    idat = png_chunk(b"IDAT", zlib.compress(row * height))
    iend = png_chunk(b"IEND", b"")
    png = b"\x89PNG\r\n\x1a\n" + ihdr + idat + iend
    return "data:image/png;base64," + base64.b64encode(png).decode()


def _placeholder_texture(name: str, width: int = 16, height: int = 16) -> dict:
    return {
        "path": "",
        "name": name,
        "folder": "block",
        "namespace": "",
        "id": "0",
        "particle": False,
        "render_mode": "default",
        "render_sides": "auto",
        "frame_time": 1,
        "frame_interpolate": False,
        "visible": True,
        "internal": True,
        "saved": False,
        "uuid": str(_uuid.uuid4()),
        "source": _make_blank_png(width, height),
    }


def build_bbmodel(surface: np.ndarray, name: str, img_nw: int = 0, img_nh: int = 0) -> dict:
    """
    Build a bbmodel from a boolean voxel surface grid.

    When img_nw/img_nh are provided (image-based mode), UV coordinates are baked
    directly from voxel indices so the texture maps 1-to-1 with the source image:
      col = nz − 1 − vz   (west-view convention: south on left)
      row = ny − 1 − vy   (Y-max at top)
    Bilateral symmetry is built in: east-half voxels (vx > nx/2) use mirrored col.
    Texture resolution = (nz × ny).

    Otherwise, all faces default to [0, 0, 16, 16] for manual UV work in Blockbench.
    """
    nx, ny, nz = surface.shape
    longest = max(nx, ny, nz)
    unit = 16.0 / longest       # Blockbench units per voxel

    ox = (16 - nx * unit) / 2
    oy = (16 - ny * unit) / 2
    oz = (16 - nz * unit) / 2

    use_image_uv = (img_nw > 0 and img_nh > 0)
    # After rotation [X=90, Z=180]: mesh-Y → display-Z (horizontal), mesh-Z → display-Y (up).
    # tex_w spans the horizontal axis (mesh-Y = ny), tex_h spans vertical (mesh-Z = nz).
    tex_w = ny if use_image_uv else 16
    tex_h = nz if use_image_uv else 16

    elements = []
    element_uuids = []

    for vx, vy, vz in np.argwhere(surface):
        fx = round(ox + vx * unit, 6)
        fy = round(oy + vy * unit, 6)
        fz = round(oz + vz * unit, 6)
        tx = round(fx + unit, 6)
        ty = round(fy + unit, 6)
        tz = round(fz + unit, 6)

        if use_image_uv:
            # mesh-Y → horizontal: south (vy=ny-1) on left (col=0).
            col = ny - 1 - int(vy)
            # mesh-Z → vertical: Z-max (vz=nz-1) at top (row=0).
            row = nz - 1 - int(vz)
            # All voxels at the same (vy, vz) share the same pixel regardless of vx —
            # bilateral symmetry is inherent since we project along the X axis.
            face = {"uv": [col, row, col + 1, row + 1], "texture": 0}
        else:
            face = {"uv": [0, 0, 16, 16], "texture": 0}

        eid = str(_uuid.uuid4())
        element_uuids.append(eid)
        elements.append({
            "name": "b",
            "box_uv": False,
            "rescale": False,
            "locked": False,
            "from": [fx, fy, fz],
            "to":   [tx, ty, tz],
            "autouv": 0,
            "color": 0,
            "origin": [fx, fy, fz],
            "faces": {d: dict(face) for d in ("north", "east", "south", "west", "up", "down")},
            "type": "cube",
            "uuid": eid,
        })

    outliner = [{
        "name": "voxels",
        "origin": [8, 8, 8],
        "uuid": str(_uuid.uuid4()),
        "export": True,
        "isOpen": False,
        "locked": False,
        "visibility": True,
        "autouv": 0,
        "children": element_uuids,
    }]

    return {
        "meta": {
            "format_version": "4.0",
            "model_format": "java_block",
            "box_uv": False,
        },
        "name": name,
        "geometry": f"geometry.{name}",
        "visible_box": [1, 1, 0],
        "variable_placeholders": "",
        "render_mode": "default",
        "ambientocclusion": True,
        "resolution": {"width": tex_w, "height": tex_h},
        "elements": elements,
        "outliner": outliner,
        "textures": [_placeholder_texture(name, tex_w, tex_h)],
    }


def main():
    parser = argparse.ArgumentParser(description="Convert an STL to a Blockbench bbmodel.")
    parser.add_argument("input",  help="Input .stl file")
    parser.add_argument("output", help="Output .bbmodel file")
    size_group = parser.add_mutually_exclusive_group()
    size_group.add_argument(
        "--max-size", type=int, default=16, metavar="N",
        help="Voxel resolution along the longest axis (default: 16)",
    )
    size_group.add_argument(
        "--from-image", metavar="PATH",
        help="Scale mesh to match the non-transparent pixel dimensions of this image "
             "and bake image-aligned UV coordinates into the model.",
    )
    args = parser.parse_args()

    name = args.output
    for suffix in (".bbmodel", ".json"):
        if name.endswith(suffix):
            name = name[: -len(suffix)]
    name = os.path.basename(name)

    print(f"Loading {args.input!r} ...")
    mesh = load_mesh(args.input)
    print(f"  Extents: {mesh.extents.round(3)}")

    img_nw = img_nh = 0
    if args.from_image:
        print(f"Scaling mesh to match image {args.from_image!r} ...")
        mesh, img_nw, img_nh = pitch_from_image(mesh, args.from_image)
        print(f"  Scaled extents: {mesh.extents.round(3)}")
        pitch = 1.0
        print(f"Voxelizing with pitch=1 (target grid ≈ {img_nw}Z × {img_nh}Y) ...")
    else:
        pitch = pitch_from_max_size(mesh, args.max_size)
        print(f"Voxelizing at --max-size {args.max_size} (pitch={pitch:.4f}) ...")

    surface = voxelize_surface(mesh, pitch)
    total = int(surface.sum())
    print(f"  Voxel grid : {surface.shape}  (X×Y×Z)")
    print(f"  Surface cubes: {total}")

    print("Building bbmodel ...")
    model = build_bbmodel(surface, name, img_nw, img_nh)

    with open(args.output, "w") as fh:
        json.dump(model, fh, indent=2)

    print(f"Saved → {args.output!r}  ({total} cubes)")


if __name__ == "__main__":
    main()
