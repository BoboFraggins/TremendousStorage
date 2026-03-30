#!/usr/bin/env python3
"""
merge_voxels.py — Greedy 3D cuboid merging for voxel bbmodels.

Algorithm
---------
This is the "Greedy Meshing" algorithm used in voxel engines such as Minecraft
modding utilities.  The 3D cuboid variant works as follows:

  1. Build a set of occupied voxel positions (integer indices).
  2. Iterate over every unprocessed occupied voxel in scan order.
  3. From that voxel, greedily extend a rectangular prism:
       a. Extend along axis A as far as every next voxel is occupied and unprocessed.
       b. Extend along axis B: advance one slice at a time, accepting the step only if
          EVERY voxel in the current A×B face is occupied and unprocessed.
       c. Extend along axis C: advance one layer at a time, accepting only if EVERY
          voxel in the current A×B plane is occupied and unprocessed.
  4. Mark all voxels in the prism as processed.
  5. Emit the prism as a single bbmodel cube element.

Because merging is valid regardless of per-voxel colour (each face of the merged
cuboid gets a rectangular UV region that covers exactly the pixels its constituent
voxels would have shown), adjacent voxels of any colour can always be combined.

The result depends on which axis is extended first.  This script tries all six
axis-order permutations and keeps the one that produces the fewest cuboids.

UV mapping (specific to brain.bbmodel produced by stl_to_bbmodel --from-image)
-------------------------------------------------------------------------------
The texture is organised as:
    col = ny − 1 − vy   (vy = ny−1 → south → col 0 = left in west view)
    row = nz − 1 − vz   (vz = nz−1 → display top → row 0 = top)

A merged cuboid at voxel-grid position (ix, iy, iz) with size (dx, dy, dz) gets:

  West  face (spans vy, vz):  full dy×dz region  — richest face, main view
  East  face (spans vy, vz):  same region, U reversed (opposite look direction)
  North face (spans vx, vy at fixed vz = iz):     horizontal strip, height 1
  South face (spans vx, vy at fixed vz = iz+dz−1): horizontal strip, U reversed
  Up    face (spans vx, vz at fixed vy = iy+dy−1): vertical strip, width 1
  Down  face (spans vx, vz at fixed vy = iy):       vertical strip, V reversed

Dependencies: none (stdlib only)

Usage:
    python merge_voxels.py blockbench/brain.bbmodel blockbench/brain_merged.bbmodel
    python merge_voxels.py blockbench/brain.bbmodel blockbench/brain_merged.bbmodel --order xyz
"""

import argparse
import itertools
import json
import uuid as _uuid


# ── grid extraction ──────────────────────────────────────────────────────────

def extract_grid(elements):
    """
    Parse bbmodel elements into integer voxel indices.

    Returns
    -------
    occupied : set of (vx, vy, vz)
    voxel_unit : float  — Blockbench units per voxel
    floor : (x_floor, y_floor, z_floor)
    dims : (nx, ny, nz)
    rotation : dict | None  — rotation block from the first element, if present
    """
    voxel_unit = round(elements[0]['to'][0] - elements[0]['from'][0], 6)

    x_floor = min(e['from'][0] for e in elements)
    y_floor = min(e['from'][1] for e in elements)
    z_floor = min(e['from'][2] for e in elements)

    occupied = set()
    for e in elements:
        vx = round((e['from'][0] - x_floor) / voxel_unit)
        vy = round((e['from'][1] - y_floor) / voxel_unit)
        vz = round((e['from'][2] - z_floor) / voxel_unit)
        occupied.add((int(vx), int(vy), int(vz)))

    nx = max(v[0] for v in occupied) + 1
    ny = max(v[1] for v in occupied) + 1
    nz = max(v[2] for v in occupied) + 1

    rotation = None
    first = elements[0]
    if first.get('rotation') and any(r != 0 for r in first['rotation']):
        rotation = {
            'rotation': first['rotation'],
            'origin':   first.get('origin', [8, 8, 8]),
        }

    return occupied, voxel_unit, (x_floor, y_floor, z_floor), (nx, ny, nz), rotation


# ── greedy merge ─────────────────────────────────────────────────────────────

def _greedy_merge_order(occupied, dims, order):
    """
    Run one pass of greedy 3D cuboid merging using the given axis extension order.

    Parameters
    ----------
    order : tuple of 3 ints, e.g. (0,1,2) = extend X first, then Y, then Z.

    Returns list of (ix, iy, iz, dx, dy, dz).
    """
    nx, ny, nz = dims
    sizes = [nx, ny, nz]

    # Map axis indices to range iterators
    ranges = [range(nx), range(ny), range(nz)]

    processed = set()
    cuboids = []

    a0, a1, a2 = order          # axis indices in extension order

    for c2 in ranges[a2]:
        for c1 in ranges[a1]:
            for c0 in ranges[a0]:
                pos0 = [0, 0, 0]
                pos0[a0], pos0[a1], pos0[a2] = c0, c1, c2
                key0 = (pos0[0], pos0[1], pos0[2])

                if key0 not in occupied or key0 in processed:
                    continue

                # Extend along first axis
                d0 = 0
                while True:
                    p = [0, 0, 0]
                    p[a0], p[a1], p[a2] = c0 + d0, c1, c2
                    k = (p[0], p[1], p[2])
                    if p[a0] >= sizes[a0] or k not in occupied or k in processed:
                        break
                    d0 += 1

                # Extend along second axis (check full d0-wide slice)
                d1 = 1
                while c1 + d1 < sizes[a1]:
                    ok = True
                    for i0 in range(d0):
                        p = [0, 0, 0]
                        p[a0], p[a1], p[a2] = c0 + i0, c1 + d1, c2
                        k = (p[0], p[1], p[2])
                        if k not in occupied or k in processed:
                            ok = False
                            break
                    if ok:
                        d1 += 1
                    else:
                        break

                # Extend along third axis (check full d0×d1 plane)
                d2 = 1
                while c2 + d2 < sizes[a2]:
                    ok = True
                    for i0 in range(d0):
                        for i1 in range(d1):
                            p = [0, 0, 0]
                            p[a0], p[a1], p[a2] = c0 + i0, c1 + i1, c2 + d2
                            k = (p[0], p[1], p[2])
                            if k not in occupied or k in processed:
                                ok = False
                                break
                        if not ok:
                            break
                    if ok:
                        d2 += 1
                    else:
                        break

                # Mark all voxels in the cuboid as processed
                for i0 in range(d0):
                    for i1 in range(d1):
                        for i2 in range(d2):
                            p = [0, 0, 0]
                            p[a0], p[a1], p[a2] = c0 + i0, c1 + i1, c2 + i2
                            processed.add((p[0], p[1], p[2]))

                # Store as (ix, iy, iz, dx, dy, dz)
                extents = [d0, d1, d2]
                origin  = [c0, c1, c2]
                ix = origin[0];  iy = origin[1];  iz = origin[2]
                dx = extents[0]; dy = extents[1]; dz = extents[2]
                # Re-map from axis order back to (x,y,z)
                result = [0, 0, 0, 0, 0, 0]
                result[a0] = c0;       result[a0 + 3] = d0
                result[a1] = c1;       result[a1 + 3] = d1
                result[a2] = c2;       result[a2 + 3] = d2
                cuboids.append(tuple(result))

    return cuboids


def greedy_merge(occupied, dims, order='best'):
    """
    Run greedy 3D cuboid merging.

    order : 'best' tries all 6 axis permutations and returns the one with
            fewest cuboids.  Otherwise pass e.g. 'xyz', 'yzx', 'zxy', etc.
    """
    axis_map = {'x': 0, 'y': 1, 'z': 2}

    if order == 'best':
        permutations = list(itertools.permutations([0, 1, 2]))
    else:
        permutations = [tuple(axis_map[c] for c in order.lower())]

    best = None
    for perm in permutations:
        result = _greedy_merge_order(occupied, dims, perm)
        label = ''.join('xyz'[i] for i in perm)
        print(f"  order={label}: {len(result)} cuboids")
        if best is None or len(result) < len(best):
            best = result

    return best


# ── UV computation ────────────────────────────────────────────────────────────

def face_uvs(ix, iy, iz, dx, dy, dz, ny, nz):
    """
    Compute per-face UV [u0, v0, u1, v1] for a merged cuboid.

    Texture layout: col = ny−1−vy, row = nz−1−vz
    (ny = texture width = number of Y voxels,
     nz = texture height = number of Z voxels)
    """
    # Column range for vy in [iy, iy+dy)
    col0 = ny - iy - dy          # col for vy = iy+dy−1 (south, left in west view)
    col1 = ny - iy               # col for vy = iy−1   (exclusive right)

    # Row range for vz in [iz, iz+dz)
    row0 = nz - iz - dz          # row for vz = iz+dz−1 (highest Z, display top)
    row1 = nz - iz               # row for vz = iz−1    (exclusive bottom)

    # Single-slice values
    col_south = ny - iy - dy     # col for vy = iy+dy−1 (south end)
    col_north = ny - 1 - iy      # col for vy = iy      (north end)
    row_top   = nz - iz - dz     # row for vz = iz+dz−1 (display top)
    row_bot   = nz - 1 - iz      # row for vz = iz      (display bottom)

    # North/South faces span vx (no texture info) and vy.  vy is stored in the
    # texture's U direction (columns), but the face's Y axis maps to UV's V
    # direction — these axes are perpendicular, so there is no axis-aligned UV
    # that can display all dy colours correctly.  Merging dy > 1 voxels would
    # produce vertical stripes (vy colours rendered as horizontal bands in U,
    # stretched across the face's vx width instead of its vy height).
    # Fix: sample the centre vy position as a single representative 1×1 pixel.
    col_center = ny - 1 - (iy + (dy - 1) // 2)

    # Up/Down faces span vx (no texture info) and vz.  vz is stored in the
    # texture's V direction (rows), but the face's Z axis maps to UV's V
    # direction — however, merging dz > 1 voxels produces dz horizontal bands
    # on the south-display face (after rotation +Y→+Z), which look like stripes.
    # Fix: sample the centre vz position as a single representative 1×1 pixel.
    row_center = nz - 1 - (iz + (dz - 1) // 2)

    return {
        # West (−X): Blockbench west face U axis = +Z_mesh, V axis = −Y_mesh.
        # After rotation [90,0,180]: Z_mesh→display Y (vertical), Y_mesh→display Z (horizontal).
        # So U must span vz (dz wide) and V must span vy (dy tall).
        # West face: U=0 at north (high row=row1), U=max at south (low row=row0); V=0 at top (col0).
        'west':  [row1,       col0,    row0,           col1       ],
        # East (+X): U axis = −Z_mesh (south→north reversed). U=0 at south (row0), U=max at north (row1).
        'east':  [row0,       col0,    row1,           col1       ],
        # North/South: centre-vy representative pixel — no stripes
        'north': [col_center, row_bot, col_center + 1, row_bot + 1],
        'south': [col_center, row_top, col_center + 1, row_top + 1],
        # Up (+Y): centre-vz representative pixel — no horizontal band stripes
        'up':    [col_south,  row_center, col_south + 1, row_center + 1],
        # Down (−Y): same but at vy=iy
        'down':  [col_north,  row_center, col_north + 1, row_center + 1],
    }


# ── model assembly ────────────────────────────────────────────────────────────

def build_merged_model(model, cuboids, voxel_unit, x_floor, y_floor, z_floor,
                       ny, nz, rotation):
    elements = []
    element_uuids = []

    for ix, iy, iz, dx, dy, dz in cuboids:
        fx = round(x_floor + ix * voxel_unit, 6)
        fy = round(y_floor + iy * voxel_unit, 6)
        fz = round(z_floor + iz * voxel_unit, 6)
        tx = round(fx + dx * voxel_unit, 6)
        ty = round(fy + dy * voxel_unit, 6)
        tz = round(fz + dz * voxel_unit, 6)

        uvs = face_uvs(ix, iy, iz, dx, dy, dz, ny, nz)
        faces = {face: {"uv": uv, "texture": 0} for face, uv in uvs.items()}

        eid = str(_uuid.uuid4())
        element_uuids.append(eid)

        elem = {
            "name": f"b{len(elements)}",
            "box_uv": False,
            "rescale": False,
            "locked": False,
            "from":   [fx, fy, fz],
            "to":     [tx, ty, tz],
            "autouv": 0,
            "color":  0,
            "origin": [fx, fy, fz],
            "faces":  faces,
            "type":   "cube",
            "uuid":   eid,
        }

        if rotation:
            elem['rotation'] = rotation['rotation']
            elem['origin']   = rotation['origin']

        elements.append(elem)

    outliner = [{
        "name":       "voxels",
        "origin":     [8, 8, 8],
        "uuid":       str(_uuid.uuid4()),
        "export":     True,
        "isOpen":     False,
        "locked":     False,
        "visibility": True,
        "autouv":     0,
        "children":   element_uuids,
    }]

    merged = dict(model)
    merged['elements'] = elements
    merged['outliner'] = outliner
    return merged


# ── exposed-face counter ──────────────────────────────────────────────────────

def count_exposed_faces(elements):
    """Count faces with no neighbouring cube on the other side."""
    voxel_unit = round(elements[0]['to'][0] - elements[0]['from'][0], 6)
    positions = {
        (round(e['from'][0], 4), round(e['from'][1], 4), round(e['from'][2], 4))
        for e in elements
    }
    dirs = [
        (-voxel_unit, 0, 0), (voxel_unit, 0, 0),
        (0, -voxel_unit, 0), (0,  voxel_unit, 0),
        (0, 0, -voxel_unit), (0, 0,  voxel_unit),
    ]
    exposed = 0
    for e in elements:
        fx, fy, fz = round(e['from'][0], 4), round(e['from'][1], 4), round(e['from'][2], 4)
        for dx, dy, dz in dirs:
            if (round(fx+dx, 4), round(fy+dy, 4), round(fz+dz, 4)) not in positions:
                exposed += 1
    return exposed


# ── main ─────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(
        description="Greedy 3D cuboid merging for voxel Blockbench models.")
    parser.add_argument("input",  help="Input .bbmodel (voxel model)")
    parser.add_argument("output", help="Output .bbmodel (merged)")
    parser.add_argument(
        "--order", default="best", metavar="AXES",
        help="Axis extension order: xyz|yzx|zxy|… or 'best' to try all 6 (default: best)",
    )
    args = parser.parse_args()

    with open(args.input) as f:
        model = json.load(f)

    elements   = model['elements']
    tex_w      = model['resolution']['width']
    tex_h      = model['resolution']['height']

    print(f"Input  : {len(elements)} cubes,  "
          f"exposed faces: {count_exposed_faces(elements)}")

    occupied, voxel_unit, floor, dims, rotation = extract_grid(elements)
    nx, ny, nz = dims
    print(f"Grid   : {nx}×{ny}×{nz}  voxel_unit={voxel_unit:.4f}  "
          f"texture={tex_w}×{tex_h}")
    if rotation:
        print(f"Rotation: {rotation['rotation']} (preserved on all merged elements)")

    print(f"\nTrying axis orders ({args.order}) …")
    cuboids = greedy_merge(occupied, dims, args.order)

    merged_model = build_merged_model(
        model, cuboids, voxel_unit, *floor, ny, nz, rotation)

    out_exposed = count_exposed_faces(merged_model['elements'])
    print(f"\nOutput : {len(cuboids)} cuboids,  exposed faces: {out_exposed}")
    print(f"Reduction: {len(elements)/len(cuboids):.2f}× fewer elements, "
          f"{count_exposed_faces(elements)/max(out_exposed,1):.2f}× fewer exposed faces")

    with open(args.output, 'w') as f:
        json.dump(merged_model, f, indent=2)

    print(f"Saved  → {args.output!r}")


if __name__ == "__main__":
    main()
