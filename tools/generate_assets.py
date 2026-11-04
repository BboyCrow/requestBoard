"""Generates RequestBoard's JSON resources and textures.

Run from the repository root:  python tools/generate_assets.py
Needs Pillow (pip install pillow) for the textures.

Everything written here is generated: edit this script, not the output files.
"""
import json
import math
import random
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src" / "main" / "resources"
ASSETS = RES / "assets" / "requestboard"
DATA = RES / "data"
MOD = "requestboard"

# wood name -> (planks texture, log side texture, log top texture, planks item, fence item)
WOODS = {}
for w in ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak"]:
    WOODS[w] = (f"{w}_planks", f"{w}_log", f"{w}_log_top")
WOODS["crimson"] = ("crimson_planks", "crimson_stem", "crimson_stem_top")
WOODS["warped"] = ("warped_planks", "warped_stem", "warped_stem_top")
WOODS["bamboo"] = ("bamboo_planks", "bamboo_block", "bamboo_block_top")

# roof name -> (texture, accepted slab items)
ROOFS = {
    "stone": ("stone", ["stone_slab", "smooth_stone_slab"]),
    "cobblestone": ("cobblestone", ["cobblestone_slab"]),
    "mossy_cobblestone": ("mossy_cobblestone", ["mossy_cobblestone_slab", "mossy_stone_brick_slab"]),
    "stone_bricks": ("stone_bricks", ["stone_brick_slab"]),
    "cobbled_deepslate": ("cobbled_deepslate", ["cobbled_deepslate_slab", "polished_deepslate_slab"]),
    "deepslate_bricks": ("deepslate_bricks", ["deepslate_brick_slab", "deepslate_tile_slab"]),
    "blackstone": ("polished_blackstone_bricks", ["blackstone_slab", "polished_blackstone_slab", "polished_blackstone_brick_slab"]),
    "andesite": ("andesite", ["andesite_slab", "polished_andesite_slab"]),
    "granite": ("granite", ["granite_slab", "polished_granite_slab"]),
    "diorite": ("diorite", ["diorite_slab", "polished_diorite_slab"]),
    "sandstone": ("sandstone_top", ["sandstone_slab", "smooth_sandstone_slab", "cut_sandstone_slab"]),
    "red_sandstone": ("red_sandstone_top", ["red_sandstone_slab", "smooth_red_sandstone_slab", "cut_red_sandstone_slab"]),
    "bricks": ("bricks", ["brick_slab"]),
    "mud_bricks": ("mud_bricks", ["mud_brick_slab"]),
    "tuff": ("tuff", ["tuff_slab", "polished_tuff_slab", "tuff_brick_slab"]),
    "nether_bricks": ("nether_bricks", ["nether_brick_slab"]),
}

FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}


def write_json(path: Path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def box(frm, to, faces, rotation=None):
    element = {"from": frm, "to": to, "faces": faces}
    if rotation:
        element["rotation"] = rotation
    return element


def face(tex, uv=None, cull=None):
    f = {"texture": tex}
    if uv:
        f["uv"] = uv
    if cull:
        f["cullface"] = cull
    return f


def board_model(wood):
    planks, log, log_top = WOODS[wood]
    elements = []
    # Posts (logs)
    for x0 in (0, 14):
        elements.append(box([x0, 0, 6], [x0 + 2, 16, 10], {
            "north": face("#log", [x0, 0, x0 + 2, 16]),
            "south": face("#log", [x0, 0, x0 + 2, 16]),
            "west": face("#log", [6, 0, 10, 16]),
            "east": face("#log", [6, 0, 10, 16]),
            "up": face("#log_top", [x0, 6, x0 + 2, 10]),
            "down": face("#log_top", [x0, 6, x0 + 2, 10], "down"),
        }))
    # Board
    elements.append(box([2, 3, 7], [14, 14, 9], {
        "north": face("#planks", [2, 2, 14, 13]),
        "south": face("#planks", [2, 2, 14, 13]),
        "up": face("#planks", [2, 7, 14, 9]),
        "down": face("#planks", [2, 7, 14, 9]),
    }))
    # Bottom rail
    elements.append(box([2, 2, 6.5], [14, 3, 9.5], {
        "north": face("#log", [2, 0, 14, 1]),
        "south": face("#log", [2, 0, 14, 1]),
        "up": face("#log", [2, 0, 14, 3]),
        "down": face("#log", [2, 0, 14, 3]),
    }))
    # Notes pinned on the front
    for (x0, y0, x1, y1) in [(3, 8, 6.5, 12.5), (7, 4.5, 10.5, 10), (11, 8.5, 13.5, 12.5), (3.5, 4, 6, 7)]:
        elements.append(box([x0, y0, 6.8], [x1, y1, 7], {
            "north": face("#note", [0, 0, 16, 16]),
            "east": face("#note", [0, 0, 1, 16]),
            "west": face("#note", [0, 0, 1, 16]),
            "up": face("#note", [0, 0, 16, 1]),
            "down": face("#note", [0, 0, 16, 1]),
        }))
    # Braces between posts and roof
    for x0, angle in ((0.5, -45), (13.5, 45)):
        elements.append(box([x0, 11.5, 7.5], [x0 + 2, 15.5, 8.5], {
            "north": face("#log", [0, 0, 2, 4]),
            "south": face("#log", [0, 0, 2, 4]),
            "east": face("#log", [0, 0, 1, 4]),
            "west": face("#log", [0, 0, 1, 4]),
        }, {"origin": [x0 + 1, 13.5, 8], "axis": "z", "angle": angle}))
    return {
        "parent": "minecraft:block/block",
        "textures": {
            "particle": f"minecraft:block/{planks}",
            "planks": f"minecraft:block/{planks}",
            "log": f"minecraft:block/{log}",
            "log_top": f"minecraft:block/{log_top}",
            "note": f"{MOD}:block/request_note",
        },
        "elements": elements,
    }


def roof_model(roof):
    tex, _ = ROOFS[roof]
    side = lambda: face("#stone", [0, 0, 16, 2])
    return {
        "parent": "minecraft:block/block",
        "textures": {"particle": f"minecraft:block/{tex}", "stone": f"minecraft:block/{tex}"},
        "elements": [
            box([-1, 14, 3], [17, 15, 13], {
                "north": side(), "south": side(), "east": face("#stone", [3, 0, 13, 1]), "west": face("#stone", [3, 0, 13, 1]),
                "up": face("#stone", [0, 3, 16, 13]), "down": face("#stone", [0, 3, 16, 13]),
            }),
            box([0, 15, 4.5], [16, 16, 11.5], {
                "north": side(), "south": side(), "east": face("#stone", [4, 0, 11, 1]), "west": face("#stone", [4, 0, 11, 1]),
                "up": face("#stone", [0, 4, 16, 11], "up"),
            }),
        ],
    }


def generate_json():
    models = ASSETS / "models" / "block"
    for wood in WOODS:
        write_json(models / f"board_{wood}.json", board_model(wood))
    for roof in ROOFS:
        write_json(models / f"roof_{roof}.json", roof_model(roof))

    multipart = []
    for facing, y in FACINGS.items():
        for wood in WOODS:
            apply = {"model": f"{MOD}:block/board_{wood}"}
            if y:
                apply["y"] = y
            multipart.append({"when": {"facing": facing, "wood": wood}, "apply": apply})
        for roof in ROOFS:
            apply = {"model": f"{MOD}:block/roof_{roof}"}
            if y:
                apply["y"] = y
            multipart.append({"when": {"facing": facing, "roof": roof}, "apply": apply})
    write_json(ASSETS / "blockstates" / "request_board.json", {"multipart": multipart})

    def select(prop, prefix, values, default):
        return {
            "type": "minecraft:select",
            "property": "minecraft:block_state",
            "block_state_property": prop,
            "cases": [{"when": v, "model": {"type": "minecraft:model", "model": f"{MOD}:block/{prefix}_{v}"}} for v in values if v != default],
            "fallback": {"type": "minecraft:model", "model": f"{MOD}:block/{prefix}_{default}"},
        }

    write_json(ASSETS / "items" / "request_board.json", {"model": {
        "type": "minecraft:composite",
        "models": [select("wood", "board", WOODS, "oak"), select("roof", "roof", ROOFS, "stone")],
    }})

    recipes = DATA / MOD / "recipe"
    for old in recipes.glob("*.json") if recipes.exists() else []:
        old.unlink()
    for wood, (planks, _, _) in WOODS.items():
        for roof, (_, slabs) in ROOFS.items():
            write_json(recipes / f"request_board_{wood}_{roof}.json", {
                "type": "minecraft:crafting_shaped",
                "category": "misc",
                "group": "request_board",
                "key": {
                    "S": [f"minecraft:{s}" for s in slabs] if len(slabs) > 1 else f"minecraft:{slabs[0]}",
                    "P": f"minecraft:{planks}",
                    "B": "minecraft:writable_book",
                    "F": f"minecraft:{wood}_fence",
                },
                "pattern": ["SSS", "PBP", "F F"],
                "result": {
                    "id": f"{MOD}:request_board",
                    "components": {"minecraft:block_state": {"wood": wood, "roof": roof}},
                },
            })

    write_json(DATA / MOD / "loot_table" / "blocks" / "request_board.json", {
        "type": "minecraft:block",
        "pools": [{
            "condition": {"type": "minecraft:survives_explosion"},
            "entries": [{
                "type": "minecraft:item",
                "name": f"{MOD}:request_board",
                "modifier": [{"type": "minecraft:copy_state", "block": f"{MOD}:request_board", "properties": ["wood", "roof"]}],
            }],
            "rolls": 1,
        }],
        "random_sequence": f"{MOD}:blocks/request_board",
    })
    write_json(DATA / "minecraft" / "tags" / "block" / "mineable" / "axe.json", {"values": [f"{MOD}:request_board"]})


# ---------------------------------------------------------------- textures

def textures():
    from PIL import Image, ImageDraw, ImageFilter

    rng = random.Random(42)
    tex = ASSETS / "textures"
    (tex / "gui").mkdir(parents=True, exist_ok=True)
    (tex / "block").mkdir(parents=True, exist_ok=True)

    def jitter(color, amount):
        return tuple(max(0, min(255, c + rng.randint(-amount, amount))) for c in color[:3]) + (color[3] if len(color) > 3 else 255,)

    # --- note texture used on the block (16x16)
    note = Image.new("RGBA", (16, 16), (236, 222, 180, 255))
    d = ImageDraw.Draw(note)
    for x in range(16):
        for y in range(16):
            note.putpixel((x, y), jitter((236, 222, 180), 6))
    for y in (5, 8, 11, 13):
        x0 = rng.randint(2, 3)
        x1 = rng.randint(9, 13)
        d.line([(x0, y), (x1, y)], fill=(120, 104, 84, 255))
    d.rectangle([7, 1, 8, 2], fill=(178, 52, 46, 255))
    note.save(tex / "block" / "request_note.png")

    # --- board background (340x240), drawn at 1:1 in the GUI
    W, H = 340, 240
    board = Image.new("RGBA", (W, H))
    px = board.load()
    plank_h = 20
    for y in range(H):
        row = y // plank_h
        base = (138, 94, 56) if row % 2 == 0 else (128, 86, 50)
        offset = (row * 97) % 160
        for x in range(W):
            grain = int(10 * math.sin((x + offset) * 0.09 + row) + 6 * math.sin((x * 0.31 + y * 0.7)))
            c = (base[0] + grain // 2, base[1] + grain // 3, base[2] + grain // 4)
            if y % plank_h == 0:
                c = (82, 54, 30)
            elif y % plank_h == 1:
                c = (100, 66, 38)
            if (x + offset) % 160 == 0 and y % plank_h > 1:
                c = (92, 60, 34)
            px[x, y] = jitter(c, 5)
    d = ImageDraw.Draw(board)

    def illegible_note(x, y, w, h, color, tilt):
        layer = Image.new("RGBA", (w + 12, h + 12), (0, 0, 0, 0))
        ld = ImageDraw.Draw(layer)
        ld.rectangle([7, 8, w + 7, h + 8], fill=(40, 24, 12, 90))  # shadow
        ld.rectangle([5, 5, w + 5, h + 5], fill=color)
        for yy in range(5, h + 6):
            for xx in range(5, w + 6):
                layer.putpixel((xx, yy), jitter(color, 7))
        ink = (96, 84, 70, 200)
        ly = 13
        while ly < h:
            lx = 9
            end = w - rng.randint(2, 10)
            while lx < end:
                seg = rng.randint(3, 9)
                ld.line([(lx, ly), (min(lx + seg, end), ly + rng.choice((0, 0, 1)))], fill=ink)
                lx += seg + rng.randint(2, 3)
            ly += rng.randint(5, 7)
        ld.rectangle([w // 2 + 3, 3, w // 2 + 6, 6], fill=(170, 46, 40, 255))
        ld.point((w // 2 + 4, 4), fill=(230, 120, 110, 255))
        layer = layer.rotate(tilt, resample=Image.NEAREST, expand=True)
        board.alpha_composite(layer, (x, y))

    palette = [(244, 232, 190, 255), (250, 240, 160, 255), (236, 214, 170, 255), (210, 228, 240, 255), (240, 206, 206, 255), (226, 236, 196, 255)]
    placed = []
    for _ in range(400):
        if len(placed) >= 26:
            break
        w, h = rng.randint(28, 48), rng.randint(26, 44)
        x, y = rng.randint(4, W - w - 16), rng.randint(4, H - h - 16)
        if any(abs(x - px_) < 34 and abs(y - py_) < 30 for px_, py_ in placed):
            continue
        placed.append((x, y))
        illegible_note(x, y, w, h, rng.choice(palette), rng.choice((-8, -5, -3, 0, 3, 5, 8)))
    # frame
    frame_dark, frame_mid, frame_light = (58, 36, 20, 255), (92, 60, 34, 255), (124, 84, 50, 255)
    for i in range(6):
        c = frame_dark if i in (0, 5) else (frame_mid if i in (1, 4) else frame_light)
        d.rectangle([i, i, W - 1 - i, H - 1 - i], outline=c)
    for cx, cy in ((10, 10), (W - 11, 10), (10, H - 11), (W - 11, H - 11)):
        d.rectangle([cx - 1, cy - 1, cx + 1, cy + 1], fill=(170, 170, 176, 255))
    board.save(tex / "gui" / "board.png")

    # --- parchment scroll
    def parchment(W, H, name):
        img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        roll = 12
        body = Image.new("RGBA", (W - 16, H - 2 * roll + 6))
        bw, bh = body.size
        for y in range(bh):
            for x in range(bw):
                edge = min(x, bw - 1 - x, y, bh - 1 - y)
                shade = max(0, 14 - edge) * 3
                noise = rng.randint(-6, 6)
                body.putpixel((x, y), (234 - shade + noise, 214 - shade + noise, 168 - shade + noise // 2, 255))
        for _ in range(W * H // 900):
            sx, sy = rng.randint(4, bw - 6), rng.randint(4, bh - 6)
            r = rng.randint(2, 5)
            spot = Image.new("RGBA", body.size, (0, 0, 0, 0))
            ImageDraw.Draw(spot).ellipse([sx - r, sy - r, sx + r, sy + r], fill=(160, 120, 70, 26))
            body = Image.alpha_composite(body, spot)
        # ragged sides
        bd = ImageDraw.Draw(body)
        for y in range(bh):
            if rng.random() < 0.25:
                bd.point((0, y), fill=(0, 0, 0, 0))
                bd.point((bw - 1, y), fill=(0, 0, 0, 0))
        img.alpha_composite(body, (8, roll - 3))

        def roller(y0):
            rd = ImageDraw.Draw(img)
            for i in range(roll):
                t = i / (roll - 1)
                light = int(70 * math.sin(math.pi * t))
                c = (176 + light // 2, 140 + light // 2, 92 + light // 3, 255)
                rd.line([(3, y0 + i), (W - 4, y0 + i)], fill=c)
            rd.rectangle([3, y0, W - 4, y0 + roll - 1], outline=(110, 78, 44, 255))
            for xk in (0, W - 4):
                rd.rectangle([xk, y0 + 2, xk + 3, y0 + roll - 3], fill=(96, 62, 34, 255))
        roller(0)
        roller(H - roll)
        img.save(tex / "gui" / name)

    parchment(260, 220, "parchment.png")
    parchment(328, 228, "parchment_large.png")

    # --- mod icon (128x128, pixel art scaled up)
    icon = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    idr = ImageDraw.Draw(icon)
    idr.rectangle([3, 9, 5, 30], fill=(96, 62, 34, 255))
    idr.rectangle([26, 9, 28, 30], fill=(96, 62, 34, 255))
    idr.rectangle([5, 10, 26, 25], fill=(160, 112, 66, 255))
    for yy in (13, 17, 21):
        idr.line([(6, yy), (25, yy)], fill=(140, 96, 56, 255))
    idr.polygon([(1, 9), (30, 9), (27, 4), (4, 4)], fill=(118, 118, 124, 255))
    idr.line([(1, 9), (30, 9)], fill=(84, 84, 90, 255))
    for (x0, y0, x1, y1, c) in [(7, 12, 12, 18, (244, 232, 190, 255)), (14, 14, 19, 22, (250, 240, 160, 255)), (21, 11, 25, 17, (210, 228, 240, 255))]:
        idr.rectangle([x0, y0, x1, y1], fill=c)
        idr.line([(x0 + 1, y0 + 3), (x1 - 1, y0 + 3)], fill=(110, 96, 80, 255))
        idr.line([(x0 + 1, y0 + 5), (x1 - 2, y0 + 5)], fill=(110, 96, 80, 255))
        idr.point(((x0 + x1) // 2, y0), fill=(178, 52, 46, 255))
    icon.resize((128, 128), Image.NEAREST).save(ASSETS / "icon.png")


if __name__ == "__main__":
    generate_json()
    textures()
    print("Resources generated.")
