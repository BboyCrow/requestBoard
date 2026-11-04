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


def post(x0, top):
    return box([x0, 0, 6], [x0 + 2, top, 10], {
        "north": face("#log", [x0, 16 - top, x0 + 2, 16]),
        "south": face("#log", [x0, 16 - top, x0 + 2, 16]),
        "west": face("#log", [6, 16 - top, 10, 16]),
        "east": face("#log", [6, 16 - top, 10, 16]),
        "up": face("#log_top", [x0, 6, x0 + 2, 10]),
        "down": face("#log_top", [x0, 6, x0 + 2, 10]),
    })


def panel(x0, y0, x1, y1):
    """A piece of the planks board (front at z=7, back at z=9)."""
    return box([x0, y0, 7], [x1, y1, 9], {
        "north": face("#planks", [x0, 16 - y1, x1, 16 - y0]),
        "south": face("#planks", [16 - x1, 16 - y1, 16 - x0, 16 - y0]),
        "up": face("#planks", [x0, 7, x1, 9]),
        "down": face("#planks", [x0, 7, x1, 9]),
        "west": face("#planks", [7, 16 - y1, 9, 16 - y0]),
        "east": face("#planks", [7, 16 - y1, 9, 16 - y0]),
    })


def rail(x0, x1):
    return box([x0, 2, 6.5], [x1, 3, 9.5], {
        "north": face("#log", [x0, 0, x1, 1]),
        "south": face("#log", [x0, 0, x1, 1]),
        "up": face("#log", [x0, 0, x1, 3]),
        "down": face("#log", [x0, 0, x1, 3]),
        "west": face("#log", [0, 0, 3, 1]),
        "east": face("#log", [0, 0, 3, 1]),
    })


NOTE_TEXTURES = 4
NOTE_LAYOUT_COUNT = 8


def note_layouts():
    """Random but reproducible note arrangements; the game picks one per block position."""
    rng = random.Random(1234)
    layouts = []
    while len(layouts) < NOTE_LAYOUT_COUNT:
        notes, tries = [], 0
        target = rng.randint(5, 8)
        while len(notes) < target and tries < 600:
            tries += 1
            w = rng.choice((2, 2.5, 3, 3, 3.5, 4))
            h = rng.choice((2.5, 3, 3.5, 4, 4.5))
            x0 = round(rng.uniform(2.5, 13.5 - w) * 2) / 2
            y0 = round(rng.uniform(3.5, 13.5 - h) * 2) / 2
            if any(x0 < b[2] + 0.25 and b[0] < x0 + w + 0.25 and y0 < b[3] + 0.25 and b[1] < y0 + h + 0.25 for b in notes):
                continue
            notes.append((x0, y0, x0 + w, y0 + h, rng.randrange(NOTE_TEXTURES)))
        layouts.append(notes)
    return layouts


def notes(layout):
    out = []
    for (x0, y0, x1, y1, t) in layout:
        tex = f"#note{t}"
        out.append(box([x0, y0, 6.8], [x1, y1, 7], {
            "north": face(tex, [0, 0, 16, 16]),
            "east": face(tex, [0, 0, 1, 16]),
            "west": face(tex, [0, 0, 1, 16]),
            "up": face(tex, [0, 0, 16, 1]),
            "down": face(tex, [0, 0, 16, 1]),
        }))
    return out


def note_textures():
    return {f"note{i}": f"{MOD}:block/request_note_{i}" for i in range(NOTE_TEXTURES)}


def brace(left):
    x0, angle = (0.5, -45) if left else (13.5, 45)
    return box([x0, 10.5, 7.5], [x0 + 2, 13.9, 8.5], {
        "north": face("#log", [0, 0, 2, 4]),
        "south": face("#log", [0, 0, 2, 4]),
        "east": face("#log", [0, 0, 1, 4]),
        "west": face("#log", [0, 0, 1, 4]),
    }, {"origin": [x0 + 1, 12.5, 8], "axis": "z", "angle": angle})


def wood_textures(wood):
    planks, log, log_top = WOODS[wood]
    return {
        "particle": f"minecraft:block/{planks}",
        "planks": f"minecraft:block/{planks}",
        "log": f"minecraft:block/{log}",
        "log_top": f"minecraft:block/{log_top}",
        **note_textures(),
    }


# Pieces of the board. Each is a model; the blockstate combines them depending on which
# neighbours the board is connected to. Model space: the front faces north, "left" is x=0.
WOOD_PIECES = {
    "core": lambda: [panel(2, 3, 14, 14)],
    "fill_left": lambda: [panel(0, 3, 2, 14)],
    "fill_right": lambda: [panel(14, 3, 16, 14)],
    "fill_up": lambda: [panel(2, 14, 14, 16)],
    "fill_down": lambda: [panel(2, 0, 14, 3)],
    "fill_up_left": lambda: [panel(0, 14, 2, 16)],
    "fill_up_right": lambda: [panel(14, 14, 16, 16)],
    "fill_down_left": lambda: [panel(0, 0, 2, 3)],
    "fill_down_right": lambda: [panel(14, 0, 16, 3)],
    "rail": lambda: [rail(2, 14)],
    "rail_left": lambda: [rail(0, 2)],
    "rail_right": lambda: [rail(14, 16)],
    "post_left": lambda: [post(0, 14)],
    "post_left_tall": lambda: [post(0, 16)],
    "post_right": lambda: [post(14, 14)],
    "post_right_tall": lambda: [post(14, 16)],
    "brace_left": lambda: [brace(True)],
    "brace_right": lambda: [brace(False)],
}

# When each piece is shown (in addition to facing + wood).
WOOD_PIECE_RULES = {
    "core": {},
    "fill_left": {"left": "true"},
    "fill_right": {"right": "true"},
    "fill_up": {"up": "true"},
    "fill_down": {"down": "true"},
    "fill_up_left": {"up": "true", "left": "true"},
    "fill_up_right": {"up": "true", "right": "true"},
    "fill_down_left": {"down": "true", "left": "true"},
    "fill_down_right": {"down": "true", "right": "true"},
    "rail": {"down": "false"},
    "rail_left": {"down": "false", "left": "true"},
    "rail_right": {"down": "false", "right": "true"},
    "post_left": {"left": "false", "up": "false"},
    "post_left_tall": {"left": "false", "up": "true"},
    "post_right": {"right": "false", "up": "false"},
    "post_right_tall": {"right": "false", "up": "true"},
    "brace_left": {"left": "false", "up": "false"},
    "brace_right": {"right": "false", "up": "false"},
}


def roof_layers(x0, x1, lower_only=False):
    u0, u1 = max(0, x0), min(16, x1)
    if u0 >= u1:
        u0, u1 = 0, 1
    out = [box([x0, 14, 3], [x1, 15, 13], {
        "north": face("#stone", [u0, 0, u1, 1]), "south": face("#stone", [u0, 0, u1, 1]),
        "east": face("#stone", [3, 0, 13, 1]), "west": face("#stone", [3, 0, 13, 1]),
        "up": face("#stone", [u0, 3, u1, 13]), "down": face("#stone", [u0, 3, u1, 13]),
    })]
    if not lower_only:
        out.append(box([x0, 15, 4.5], [x1, 16, 11.5], {
            "north": face("#stone", [u0, 1, u1, 2]), "south": face("#stone", [u0, 1, u1, 2]),
            "east": face("#stone", [4, 0, 11, 1]), "west": face("#stone", [4, 0, 11, 1]),
            "up": face("#stone", [u0, 4, u1, 11]),
        }))
    return out


ROOF_PIECES = {
    "core": lambda: roof_layers(0, 16),
    "overhang_left": lambda: roof_layers(-1, 0, lower_only=True),
    "overhang_right": lambda: roof_layers(16, 17, lower_only=True),
}
ROOF_PIECE_RULES = {
    "core": {"up": "false"},
    "overhang_left": {"up": "false", "left": "false"},
    "overhang_right": {"up": "false", "right": "false"},
}


def model(textures, elements):
    return {"parent": "minecraft:block/block", "textures": textures, "elements": elements}


# Woods that only exist in some Minecraft versions: their models live in generated/<version>/.
LATE_WOODS = {"pale_oak"}


def write_board_models(models, wood):
    tex = wood_textures(wood)
    for name, build in WOOD_PIECES.items():
        write_json(models / f"board_{wood}_{name}.json", model(tex, build()))
    item_parts = ["core", "rail", "post_left", "post_right", "brace_left", "brace_right"]
    write_json(models / f"board_{wood}.json", model(tex, [e for p in item_parts for e in WOOD_PIECES[p]()] + notes(note_layouts()[0])))


def generate_blockstate(out, woods):
    layouts = note_layouts()
    multipart = []
    for facing, y in FACINGS.items():
        multipart.append({"when": {"facing": facing}, "apply": [
            {"model": f"{MOD}:block/notes_{i}", **({"y": y} if y else {})} for i in range(len(layouts))
        ]})
        for wood in woods:
            for name, rule in WOOD_PIECE_RULES.items():
                apply = {"model": f"{MOD}:block/board_{wood}_{name}"}
                if y:
                    apply["y"] = y
                multipart.append({"when": {"facing": facing, "wood": wood, **rule}, "apply": apply})
        for roof in ROOFS:
            for name, rule in ROOF_PIECE_RULES.items():
                apply = {"model": f"{MOD}:block/roof_{roof}_{name}"}
                if y:
                    apply["y"] = y
                multipart.append({"when": {"facing": facing, "roof": roof, **rule}, "apply": apply})
    write_json(out / "assets" / MOD / "blockstates" / "request_board.json", {"multipart": multipart})
    for wood in woods:
        if wood in LATE_WOODS:
            write_board_models(out / "assets" / MOD / "models" / "block", wood)


def generate_models():
    models = ASSETS / "models" / "block"
    if models.exists():
        for old in models.glob("*.json"):
            old.unlink()
    clean(ASSETS / "blockstates")
    multipart = []
    for wood in [w for w in WOODS if w not in LATE_WOODS]:
        tex = wood_textures(wood)
        for name, build in WOOD_PIECES.items():
            write_json(models / f"board_{wood}_{name}.json", model(tex, build()))
        # Standalone board used by the item.
        item_parts = ["core", "rail", "post_left", "post_right", "brace_left", "brace_right"]
        write_json(models / f"board_{wood}.json", model(tex, [e for p in item_parts for e in WOOD_PIECES[p]()] + notes(note_layouts()[0])))
    for roof in ROOFS:
        tex_name, _ = ROOFS[roof]
        tex = {"particle": f"minecraft:block/{tex_name}", "stone": f"minecraft:block/{tex_name}"}
        for name, build in ROOF_PIECES.items():
            write_json(models / f"roof_{roof}_{name}.json", model(tex, build()))
        write_json(models / f"roof_{roof}.json", model(tex, [e for p in ROOF_PIECES.values() for e in p()]))

    layouts = note_layouts()
    for i, layout in enumerate(layouts):
        write_json(models / f"notes_{i}.json", model({"particle": f"{MOD}:block/request_note_0", **note_textures()}, notes(layout)))

    pass


GENERATED = ROOT / "generated"

# Minecraft versions built by Stonecutter, and the data formats each one reads.
MC_VERSIONS = {
    "1.21.1": {"ingredients": "objects", "loot": "functions", "item_models": "overrides", "missing_woods": ["pale_oak"],
               "adv_background": "minecraft:textures/block/spruce_planks.png", "icon_data": "int"},
    "1.21.11": {"ingredients": "ids", "loot": "functions", "item_models": "items",
                "adv_background": "minecraft:block/spruce_planks", "icon_data": "strings"},
    "26.3": {"ingredients": "ids", "loot": "modifiers", "item_models": "items",
             "adv_background": "minecraft:block/spruce_planks", "icon_data": "strings"},
}


def clean(path):
    import shutil
    if path.exists():
        shutil.rmtree(path)


def generate_json():
    generate_models()
    generate_icon_models()
    write_json(DATA / "minecraft" / "tags" / "block" / "mineable" / "axe.json", {"values": [f"{MOD}:request_board"]})
    # Anything whose format depends on the Minecraft version lives in generated/<version>/.
    for old in (DATA / MOD / "recipe", DATA / MOD / "loot_table", DATA / MOD / "advancement", ASSETS / "items"):
        clean(old)
    for mc, fmt in MC_VERSIONS.items():
        out = GENERATED / mc
        clean(out)
        generate_blockstate(out, woods_for(fmt))
        generate_recipes(out, fmt)
        generate_loot_table(out, fmt)
        generate_advancements(out, fmt)
        generate_item_models(out, fmt)


def woods_for(fmt):
    return [w for w in WOODS if w not in fmt.get("missing_woods", [])]


def ingredient(ids, fmt):
    if fmt["ingredients"] == "objects":
        objs = [{"item": i} for i in ids]
        return objs if len(objs) > 1 else objs[0]
    return ids if len(ids) > 1 else ids[0]


def generate_recipes(out, fmt):
    recipes = out / "data" / MOD / "recipe"
    for wood in woods_for(fmt):
        planks = WOODS[wood][0]
        for roof, (_, slabs) in ROOFS.items():
            write_json(recipes / f"request_board_{wood}_{roof}.json", {
                "type": "minecraft:crafting_shaped",
                "category": "misc",
                "group": "request_board",
                "key": {
                    "S": ingredient([f"minecraft:{s}" for s in slabs], fmt),
                    "P": ingredient([f"minecraft:{planks}"], fmt),
                    "B": ingredient(["minecraft:writable_book"], fmt),
                    "F": ingredient([f"minecraft:{wood}_fence"], fmt),
                },
                "pattern": ["SSS", "PBP", "F F"],
                "result": {
                    "id": f"{MOD}:request_board",
                    "count": 1,
                    "components": {"minecraft:block_state": {"wood": wood, "roof": roof}},
                },
            })


def generate_loot_table(out, fmt):
    copy = {"block": f"{MOD}:request_board", "properties": ["wood", "roof"]}
    if fmt["loot"] == "modifiers":
        pool = {
            "condition": {"type": "minecraft:survives_explosion"},
            "entries": [{"type": "minecraft:item", "name": f"{MOD}:request_board",
                         "modifier": [{"type": "minecraft:copy_state", **copy}]}],
            "rolls": 1,
        }
    else:
        pool = {
            "bonus_rolls": 0.0,
            "conditions": [{"condition": "minecraft:survives_explosion"}],
            "entries": [{"type": "minecraft:item", "name": f"{MOD}:request_board",
                         "functions": [{"function": "minecraft:copy_state", **copy}]}],
            "rolls": 1.0,
        }
    write_json(out / "data" / MOD / "loot_table" / "blocks" / "request_board.json", {
        "type": "minecraft:block",
        "pools": [pool],
        "random_sequence": f"{MOD}:blocks/request_board",
    })


def board_variant_index(woods, wood, roof):
    """Index used by the 1.21.1 item model override (see RequestBoardClient): wood ordinal * roofs + roof ordinal."""
    return woods.index(wood) * len(ROOFS) + list(ROOFS).index(roof)


def generate_item_models(out, fmt):
    assets = out / "assets" / MOD
    icon_names = list(ICONS)
    if fmt["item_models"] == "items":
        def select(prop, prefix, values, default):
            return {
                "type": "minecraft:select",
                "property": "minecraft:block_state",
                "block_state_property": prop,
                "cases": [{"when": v, "model": {"type": "minecraft:model", "model": f"{MOD}:block/{prefix}_{v}"}} for v in values if v != default],
                "fallback": {"type": "minecraft:model", "model": f"{MOD}:block/{prefix}_{default}"},
            }
        write_json(assets / "items" / "request_board.json", {"model": {
            "type": "minecraft:composite",
            "models": [select("wood", "board", woods_for(fmt), "oak"), select("roof", "roof", ROOFS, "stone")],
        }})
        write_json(assets / "items" / "advancement_icon.json", {"model": {
            "type": "minecraft:select",
            "property": "minecraft:custom_model_data",
            "cases": [{"when": n, "model": {"type": "minecraft:model", "model": f"{MOD}:item/icon_{n}"}} for n in icon_names],
            "fallback": {"type": "minecraft:model", "model": f"{MOD}:item/icon_{icon_names[0]}"},
        }})
        return

    # 1.21.1: one combined model per wood/roof pair, picked by an item property override.
    overrides = []
    woods = woods_for(fmt)
    for wood in woods:
        board_elements = [e for p in ["core", "rail", "post_left", "post_right", "brace_left", "brace_right"] for e in WOOD_PIECES[p]()]
        board_elements += notes(note_layouts()[0])
        for roof in ROOFS:
            tex_name, _ = ROOFS[roof]
            textures = {**wood_textures(wood), "stone": f"minecraft:block/{tex_name}"}
            roof_elements = [e for p in ROOF_PIECES.values() for e in p()]
            write_json(assets / "models" / "item" / f"request_board_{wood}_{roof}.json",
                       model(textures, board_elements + roof_elements))
            overrides.append({"predicate": {f"{MOD}:variant": board_variant_index(woods, wood, roof) / 1000.0},
                              "model": f"{MOD}:item/request_board_{wood}_{roof}"})
    write_json(assets / "models" / "item" / "request_board.json",
               {"parent": f"{MOD}:item/request_board_oak_stone", "overrides": overrides})
    write_json(assets / "models" / "item" / "advancement_icon.json", {
        "parent": f"{MOD}:item/icon_{icon_names[0]}",
        "overrides": [{"predicate": {"custom_model_data": i + 1}, "model": f"{MOD}:item/icon_{n}"} for i, n in enumerate(icon_names)],
    })


# ---------------------------------------------------------------- advancements

# id -> (parent, icon, frame, hidden)
ADVANCEMENTS = {
    "root": (None, f"{MOD}:request_board", "task", False),
    "first_request": ("root", "icon", "task", False),
    "requests_10": ("first_request", "icon", "goal", False),
    "requests_100": ("requests_10", "icon", "challenge", False),
    "patron": ("first_request", "icon", "goal", False),
    "accept_request": ("root", "icon", "task", False),
    "mission_accomplished": ("accept_request", "icon", "task", False),
    "community_pillar": ("mission_accomplished", "icon", "challenge", False),
    "village_legend": ("community_pillar", "icon", "challenge", False),
}


def generate_advancements(out, fmt):
    folder = out / "data" / MOD / "advancement"
    icon_names = list(ICONS)
    for name, (parent, icon, frame, hidden) in ADVANCEMENTS.items():
        if icon == "icon":
            data = icon_names.index(name) + 1 if fmt["icon_data"] == "int" else {"strings": [name]}
            icon_obj = {"id": f"{MOD}:advancement_icon", "count": 1, "components": {"minecraft:custom_model_data": data}}
        else:
            icon_obj = {"id": icon, "count": 1, "components": {"minecraft:block_state": {"wood": "oak", "roof": "stone"}}}
        display = {
            "icon": icon_obj,
            "title": {"translate": f"advancements.{MOD}.{name}.title"},
            "description": {"translate": f"advancements.{MOD}.{name}.description"},
            "frame": frame,
            "show_toast": True,
            "announce_to_chat": True,
            "hidden": hidden,
        }
        if parent is None:
            display["background"] = fmt["adv_background"]
            display["show_toast"] = False
            display["announce_to_chat"] = False
            criteria = {"done": {"trigger": "minecraft:inventory_changed",
                                 "conditions": {"items": [{"items": f"{MOD}:request_board"}]}}}
        else:
            criteria = {"done": {"trigger": "minecraft:impossible"}}
        adv = {"display": display, "criteria": criteria, "requirements": [["done"]]}
        if parent:
            adv = {"parent": f"{MOD}:{parent}", **adv}
        write_json(folder / f"{name}.json", adv)


# ---------------------------------------------------------------- textures

def textures():
    from PIL import Image, ImageDraw, ImageFilter

    rng = random.Random(42)
    tex = ASSETS / "textures"
    (tex / "gui").mkdir(parents=True, exist_ok=True)
    (tex / "block").mkdir(parents=True, exist_ok=True)

    def jitter(color, amount):
        return tuple(max(0, min(255, c + rng.randint(-amount, amount))) for c in color[:3]) + (color[3] if len(color) > 3 else 255,)

    # --- note textures used on the block (16x16), one per paper tint
    old_note = tex / "block" / "request_note.png"
    if old_note.exists():
        old_note.unlink()
    tints = [(236, 222, 180), (246, 238, 206), (240, 226, 150), (226, 214, 196)]
    pins = [(178, 52, 46), (60, 90, 170), (60, 130, 60), (178, 52, 46)]
    for i, tint in enumerate(tints):
        note = Image.new("RGBA", (16, 16))
        d = ImageDraw.Draw(note)
        for x in range(16):
            for y in range(16):
                note.putpixel((x, y), jitter(tint, 6))
        y = rng.randint(4, 5)
        while y < 15:
            x0 = rng.randint(2, 3)
            x1 = rng.randint(8, 13)
            d.line([(x0, y), (x1, y)], fill=(120, 104, 84, 255))
            y += rng.randint(2, 3)
        d.rectangle([7, 1, 8, 2], fill=pins[i] + (255,))
        note.save(tex / "block" / f"request_note_{i}.png")

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

    fantasy_textures(tex, random.Random(7))
    generate_icon_textures(tex)


# ---------------------------------------------------------------- adventure style

def torn_polygon(rng, x0, y0, x1, y1, jag=2.0, step=4):
    """Rectangle outline with ragged edges."""
    pts = []
    x = x0
    while x < x1:
        pts.append((x, y0 + rng.uniform(-jag, jag)))
        x += step
    y = y0
    while y < y1:
        pts.append((x1 + rng.uniform(-jag, jag), y))
        y += step
    x = x1
    while x > x0:
        pts.append((x, y1 + rng.uniform(-jag, jag)))
        x -= step
    y = y1
    while y > y0:
        pts.append((x0 + rng.uniform(-jag, jag), y))
        y -= step
    return pts


def parchment_layer(rng, size, poly, base, burn=14):
    """Aged parchment inside poly: noise, darker burnt edges, a few stains."""
    from PIL import Image, ImageDraw, ImageFilter
    w, h = size
    mask = Image.new("L", size, 0)
    ImageDraw.Draw(mask).polygon(poly, fill=255)
    depth = mask.filter(ImageFilter.GaussianBlur(burn))
    img = Image.new("RGBA", size, (0, 0, 0, 0))
    px, mp, dp = img.load(), mask.load(), depth.load()
    for y in range(h):
        for x in range(w):
            if mp[x, y] == 0:
                continue
            d = dp[x, y] / 255.0
            edge = max(0.0, 1.0 - d * 1.6)
            n = rng.randint(-6, 6)
            r = base[0] - int(edge * 130) + n
            g = base[1] - int(edge * 150) + n
            b = base[2] - int(edge * 150) + n // 2
            px[x, y] = (max(30, min(255, r)), max(18, min(255, g)), max(8, min(255, b)), 255)
    stains = Image.new("RGBA", size, (0, 0, 0, 0))
    sd = ImageDraw.Draw(stains)
    for _ in range(max(2, w * h // 6000)):
        sx, sy, r = rng.randint(0, w), rng.randint(0, h), rng.randint(4, 14)
        sd.ellipse([sx - r, sy - r, sx + r, sy + r], fill=(120, 80, 40, 22))
    stains = stains.filter(ImageFilter.GaussianBlur(3))
    stains.putalpha(Image.composite(stains.getchannel("A"), Image.new("L", size, 0), mask))
    return Image.alpha_composite(img, stains)


def scribble_lines(d, rng, x0, y0, x1, y1, color, spacing=(8, 10), stroke=1):
    """Lines of illegible handwriting."""
    y = y0
    while y < y1:
        x = x0 + rng.randint(0, 4)
        end = x1 - rng.randint(0, max(1, (x1 - x0) // 3))
        while x < end:
            word = rng.randint(6, 16)
            pts = []
            for i in range(0, word, 2):
                pts.append((x + i, y + rng.choice((-1, 0, 0, 1)) + (1 if i % 4 else 0)))
            if len(pts) > 1:
                d.line(pts, fill=color, width=stroke)
            x += word + rng.randint(4, 6)
        y += rng.randint(*spacing)


def big_scribble(d, rng, cx, y, width, color, height=7, centred=False):
    """A heading of large illegible letters."""
    steps = []
    total = 0
    while total < width:
        step = rng.randint(8, 10)
        if total + step > width:
            break
        steps.append(step)
        total += step
    x = cx - (total - 2) // 2 if centred else cx - width // 2
    for step in steps:
        h = rng.randint(height - 2, height + 2)
        kind = rng.random()
        if kind < 0.4:
            d.line([(x, y + h), (x + 3, y), (x + 6, y + h)], fill=color, width=2)
        elif kind < 0.7:
            d.arc([x, y, x + 7, y + h], 30, 330, fill=color, width=2)
        else:
            d.line([(x + 1, y), (x + 1, y + h)], fill=color, width=2)
            d.line([(x + 1, y + h // 2), (x + 6, y + h // 2)], fill=color, width=2)
        x += step


def wax_seal(d, x, y, r=9):
    d.ellipse([x - r - 2, y - r + 1, x + r + 1, y + r + 3], fill=(60, 10, 10, 120))
    d.ellipse([x - r, y - r, x + r, y + r], fill=(160, 24, 22, 255))
    d.ellipse([x - r + 3, y - r + 3, x + r - 3, y + r - 3], outline=(110, 10, 10, 255), width=2)
    d.ellipse([x - r + 2, y - r + 2, x - r + 6, y - r + 6], fill=(210, 80, 70, 255))
    d.line([(x - 3, y), (x + 3, y)], fill=(120, 12, 12, 255), width=2)
    d.line([(x, y - 3), (x, y + 3)], fill=(120, 12, 12, 255), width=2)


def pin(d, x, y):
    d.ellipse([x - 3, y - 3, x + 3, y + 3], fill=(40, 30, 24, 255))
    d.ellipse([x - 2, y - 2, x + 1, y + 1], fill=(150, 130, 100, 255))


def age_planks(board, rng, plank_h):
    """Weathers the planks: faded patches, dark stains, cracks, scratches, chipped edges, old nail holes."""
    from PIL import Image, ImageDraw, ImageFilter
    W, H = board.size
    # overall: duller and a bit darker, like wood left out in the weather
    from PIL import ImageEnhance
    rgb = board.convert("RGB")
    rgb = ImageEnhance.Color(rgb).enhance(0.72)
    rgb = ImageEnhance.Brightness(rgb).enhance(0.86)
    board.paste(rgb.convert("RGBA"))
    # faded / greyed patches
    fade = Image.new("L", board.size, 0)
    fd = ImageDraw.Draw(fade)
    for _ in range(34):
        x, y, r = rng.randint(0, W), rng.randint(0, H), rng.randint(20, 80)
        fd.ellipse([x - r, y - r * 0.6, x + r, y + r * 0.6], fill=rng.randint(70, 140))
    fade = fade.filter(ImageFilter.GaussianBlur(18))
    grey = Image.new("RGBA", board.size, (120, 96, 72, 255))
    grey.putalpha(fade)
    board.alpha_composite(grey)
    # dark water stains / dirt
    dirt = Image.new("L", board.size, 0)
    dd = ImageDraw.Draw(dirt)
    for _ in range(30):
        x, y, r = rng.randint(0, W), rng.randint(0, H), rng.randint(10, 46)
        dd.ellipse([x - r, y - r, x + r, y + r], fill=rng.randint(70, 160))
    dirt = dirt.filter(ImageFilter.GaussianBlur(10))
    dark = Image.new("RGBA", board.size, (40, 20, 6, 255))
    dark.putalpha(dirt)
    board.alpha_composite(dark)
    d = ImageDraw.Draw(board)
    # cracks along the grain
    for _ in range(70):
        row = rng.randint(0, H // plank_h)
        y = row * plank_h + rng.randint(4, plank_h - 4)
        x = rng.randint(0, W)
        length = rng.randint(30, 140)
        pts = [(x, y)]
        for i in range(length // 6):
            x += 6
            y += rng.choice((-1, 0, 0, 0, 1))
            pts.append((x, y))
        d.line(pts, fill=(36, 18, 6, 240), width=rng.choice((1, 2, 2, 3)))
        d.line([(a + 1, b + 1) for a, b in pts], fill=(196, 134, 80, 90), width=1)
    # scratches across
    for _ in range(60):
        x, y = rng.randint(0, W), rng.randint(0, H)
        dx, dy = rng.randint(-14, 14), rng.randint(-6, 6)
        d.line([(x, y), (x + dx, y + dy)], fill=(70, 38, 16, 150) if rng.random() < 0.6 else (210, 150, 96, 110))
    # chipped plank edges
    for _ in range(40):
        row = rng.randint(1, H // plank_h)
        y = row * plank_h
        x = rng.randint(0, W)
        w = rng.randint(4, 16)
        d.polygon([(x, y), (x + w, y), (x + w // 2, y + rng.randint(2, 5))], fill=(52, 26, 10, 255))
    # worm holes
    for _ in range(14):
        cx, cy = rng.randint(20, W - 20), rng.randint(20, H - 20)
        for _ in range(rng.randint(4, 9)):
            x, y = cx + rng.randint(-12, 12), cy + rng.randint(-8, 8)
            d.ellipse([x - 1, y - 1, x + 1, y + 1], fill=(26, 12, 4, 255))
    # splits at the plank ends
    for _ in range(16):
        row = rng.randint(0, H // plank_h - 1)
        x = rng.randint(30, W - 30)
        y0 = row * plank_h + 2
        d.polygon([(x, y0), (x + 3, y0), (x + 1, y0 + rng.randint(8, plank_h - 4))], fill=(30, 14, 4, 255))
    # old nail holes and rust streaks
    for _ in range(22):
        x, y = rng.randint(30, W - 30), rng.randint(30, H - 30)
        d.ellipse([x - 2, y - 2, x + 2, y + 2], fill=(30, 16, 6, 255))
        if rng.random() < 0.5:
            streak = Image.new("RGBA", board.size, (0, 0, 0, 0))
            ImageDraw.Draw(streak).line([(x, y + 2), (x + rng.randint(-1, 1), y + rng.randint(10, 22))], fill=(120, 56, 20, 120), width=3)
            board.alpha_composite(streak.filter(ImageFilter.GaussianBlur(1.5)))


def fantasy_textures(tex, rng):
    from PIL import Image, ImageDraw, ImageFilter

    S = 2  # drawn at twice the GUI resolution for extra detail
    W, H = 340 * S, 240 * S
    board = Image.new("RGBA", (W, H))
    px = board.load()

    # Warm horizontal planks
    plank_h = 19 * S
    tones = [(170, 98, 46), (156, 88, 40), (178, 106, 52), (148, 82, 36), (164, 94, 44)]
    offsets = [rng.randint(0, 400) for _ in range(40)]
    for y in range(H):
        row = y // plank_h
        base = tones[row % len(tones)]
        off = offsets[row % len(offsets)]
        ly = y % plank_h
        for x in range(W):
            g = math.sin((x + off) * 0.035 + math.sin((x + off) * 0.011) * 3 + ly * 0.45) * 14
            g += math.sin((x + off) * 0.21 + ly * 1.3) * 4
            c = [base[0] + g, base[1] + g * 0.7, base[2] + g * 0.4]
            if ly < 2:
                c = [58, 30, 14]
            elif ly < 4:
                c = [c[0] * 0.75, c[1] * 0.72, c[2] * 0.7]
            elif ly > plank_h - 3:
                c = [c[0] * 0.85, c[1] * 0.82, c[2] * 0.8]
            n = rng.randint(-5, 5)
            px[x, y] = (int(max(0, min(255, c[0] + n))), int(max(0, min(255, c[1] + n))), int(max(0, min(255, c[2] + n))), 255)
    d = ImageDraw.Draw(board)
    age_planks(board, rng, plank_h)
    d = ImageDraw.Draw(board)
    for row in range(H // plank_h + 1):
        yy = row * plank_h + plank_h // 2
        for xx in (22 * S, W - 22 * S):
            d.ellipse([xx - 3, yy - 3, xx + 3, yy + 3], fill=(54, 42, 34, 255))
            d.point((xx - 1, yy - 1), fill=(150, 140, 130, 255))

    # Posters
    ink = (52, 30, 12, 200)
    faint = (70, 44, 20, 150)

    def place(img, x, y, angle):
        img = img.rotate(angle, resample=Image.BICUBIC, expand=True)
        shadow = Image.new("RGBA", img.size, (20, 8, 2, 0))
        shadow.putalpha(img.getchannel("A").point(lambda a: int(a * 0.5)))
        shadow = shadow.filter(ImageFilter.GaussianBlur(4))
        board.alpha_composite(shadow, (x + 5, y + 6))
        board.alpha_composite(img, (x, y))

    def poster_framed(w, h, kind):
        img = parchment_layer(rng, (w, h), torn_polygon(rng, 3, 3, w - 3, h - 3, jag=2.5, step=6), (232, 206, 156))
        pd = ImageDraw.Draw(img)
        pd.rectangle([10, 10, w - 11, h - 11], outline=(110, 72, 36, 255), width=2)
        pd.rectangle([14, 14, w - 15, h - 15], outline=(140, 98, 52, 200), width=1)
        big_scribble(pd, rng, w // 2, 22, w - 50, (40, 22, 8, 235), height=11)
        bx0, by0, bx1 = 24, 44, w - 24
        by1 = by0 + int((bx1 - bx0) * 0.8)
        pd.rectangle([bx0, by0, bx1, by1], fill=(222, 196, 146, 255), outline=(80, 50, 24, 255), width=2)
        pd.rectangle([bx0 + 4, by0 + 4, bx1 - 4, by1 - 4], outline=(160, 120, 70, 180))
        scribble_lines(pd, rng, 22, by1 + 12, w - 22, h - 24, faint, spacing=(9, 11))
        if kind == "seal":
            wax_seal(pd, w - 32, h - 30)
        for cx, cy in ((10, 10), (w - 11, 10), (10, h - 11), (w - 11, h - 11)):
            pin(pd, cx, cy)
        return img

    def poster_notice(w, h):
        img = parchment_layer(rng, (w, h), torn_polygon(rng, 3, 3, w - 3, h - 3, jag=3, step=5), (236, 214, 166))
        pd = ImageDraw.Draw(img)
        big_scribble(pd, rng, w // 2, 16, w - 40, (40, 22, 8, 230), height=10)
        scribble_lines(pd, rng, 16, 40, w - 14, h - 26, faint)
        pd.line([(w - 54, h - 18), (w - 40, h - 22), (w - 30, h - 16), (w - 18, h - 20)], fill=ink, width=2)
        pin(pd, w // 2, 8)
        return img

    def poster_scroll(w, h):
        img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        body = parchment_layer(rng, (w - 8, h - 16), torn_polygon(rng, 2, 2, w - 10, h - 18, jag=1.5, step=6), (230, 208, 160))
        img.alpha_composite(body, (4, 8))
        pd = ImageDraw.Draw(img)
        for yy in (2, h - 12):
            for i in range(10):
                t = i / 9
                c = int(150 + 70 * math.sin(math.pi * t))
                pd.line([(0, yy + i), (w - 1, yy + i)], fill=(c, int(c * 0.78), int(c * 0.5), 255))
            pd.rectangle([0, yy, w - 1, yy + 9], outline=(96, 62, 30, 255))
        scribble_lines(pd, rng, 14, 26, w - 14, h - 22, faint)
        return img

    def poster_scrap(w, h):
        img = parchment_layer(rng, (w, h), torn_polygon(rng, 3, 3, w - 3, h - 3, jag=4.5, step=4), (226, 200, 150), burn=10)
        pd = ImageDraw.Draw(img)
        scribble_lines(pd, rng, 12, 14, w - 10, h - 10, faint)
        pin(pd, w // 2, 8)
        return img

    layout = [
        ("framed", 18, 30, 104, 150, -3),   # top-left, the dagger goes here
        ("notice", 136, 50, 130, 100, 2),
        ("scroll", 284, 26, 110, 140, -2),
        ("framed_seal", 420, 36, 108, 156, 3),
        ("scrap", 548, 30, 112, 92, -4),
        ("notice", 30, 214, 124, 104, 3),
        ("scrap", 176, 182, 96, 90, -2),
        ("framed_seal", 290, 186, 110, 160, -2),
        ("scroll", 424, 216, 104, 128, 2),
        ("framed", 552, 150, 104, 150, 4),
        ("scrap", 40, 344, 120, 86, -3),
        ("notice", 184, 300, 106, 140, 1),
        ("scrap", 548, 330, 96, 96, 3),
    ]
    for kind, x, y, w, h, angle in layout:
        if kind.startswith("framed"):
            img = poster_framed(w, h, "seal" if kind.endswith("seal") else "plain")
        elif kind == "notice":
            img = poster_notice(w, h)
        elif kind == "scroll":
            img = poster_scroll(w, h)
        else:
            img = poster_scrap(w, h)
        place(img, x, y, angle)

    # Dagger stuck into the top-left poster
    def dagger(tip_x, tip_y, angle_deg, length=86):
        a = math.radians(angle_deg)
        ux, uy = math.cos(a), math.sin(a)      # from tip towards pommel
        nx, ny = -uy, ux
        P = lambda t, o: (tip_x + ux * t + nx * o, tip_y + uy * t + ny * o)
        blade_len = length * 0.55
        layer = Image.new("RGBA", board.size, (0, 0, 0, 0))
        ld = ImageDraw.Draw(layer)
        # shadow on the board
        sh = [(p[0] + 6, p[1] + 7) for p in (P(blade_len * 0.4, 0), P(blade_len, 5), P(length, 4), P(length, -4), P(blade_len, -5))]
        ld.polygon(sh, fill=(20, 8, 2, 110))
        layer = layer.filter(ImageFilter.GaussianBlur(3))
        ld = ImageDraw.Draw(layer)
        ld.polygon([P(0, 0), P(blade_len, 6), P(blade_len, -6)], fill=(176, 182, 192, 255))
        ld.polygon([P(0, 0), P(blade_len, 0), P(blade_len, -6)], fill=(214, 220, 228, 255))
        ld.line([P(4, 0), P(blade_len - 2, 0)], fill=(120, 126, 136, 255), width=1)
        ld.polygon([P(blade_len, 15), P(blade_len + 6, 15), P(blade_len + 6, -15), P(blade_len, -15)], fill=(186, 140, 50, 255))
        ld.line([P(blade_len, 15), P(blade_len, -15)], fill=(230, 190, 90, 255), width=1)
        grip_end = length - 8
        ld.polygon([P(blade_len + 6, 5), P(grip_end, 5), P(grip_end, -5), P(blade_len + 6, -5)], fill=(110, 30, 26, 255))
        for t in range(int(blade_len + 9), int(grip_end), 5):
            ld.line([P(t, 5), P(t + 3, -5)], fill=(70, 16, 14, 255), width=2)
        cx, cy = P(length - 3, 0)
        ld.ellipse([cx - 7, cy - 7, cx + 7, cy + 7], fill=(186, 140, 50, 255))
        ld.ellipse([cx - 4, cy - 4, cx + 1, cy + 1], fill=(236, 200, 110, 255))
        # small tear where the blade enters
        tx, ty = P(0, 0)
        ld.line([(tx - 6, ty + 3), (tx + 6, ty - 3)], fill=(60, 34, 14, 255), width=2)
        board.alpha_composite(layer)


    # Thick wooden frame with metal corner brackets
    fw = 11 * S
    fpx = board.load()
    side_off = {"top": rng.randint(0, 500), "bottom": rng.randint(0, 500), "left": rng.randint(0, 500), "right": rng.randint(0, 500)}
    for y in range(H):
        for x in range(W):
            depth = min(x, y, W - 1 - x, H - 1 - y)
            if depth >= fw:
                continue
            # which frame bar this pixel belongs to (mitred corners)
            dl, dr, dt, db = x, W - 1 - x, y, H - 1 - y
            m = min(dl, dr, dt, db)
            side = "left" if m == dl else "right" if m == dr else "top" if m == dt else "bottom"
            along = y if side in ("left", "right") else x
            across = depth
            off = side_off[side]
            grain = math.sin((along + off) * 0.045 + math.sin((along + off) * 0.013) * 2.5 + across * 0.9) * 16
            grain += math.sin((along + off) * 0.31 + across * 2.1) * 5
            t = across / (fw - 1)
            bevel = 1.0 + 0.35 * math.sin(math.pi * t) - (0.35 if across < 2 else 0) - (0.3 if across > fw - 3 else 0)
            base = (108, 60, 28)
            r = (base[0] + grain) * bevel
            g = (base[1] + grain * 0.6) * bevel
            b = (base[2] + grain * 0.3) * bevel
            if across in (fw // 2 - 1, fw // 2) and side in ("top", "bottom", "left", "right"):
                r, g, b = r * 0.72, g * 0.7, b * 0.68  # carved groove
            n = rng.randint(-6, 6)
            fpx[x, y] = (int(max(0, min(255, r + n))), int(max(0, min(255, g + n))), int(max(0, min(255, b + n))), 255)
    d = ImageDraw.Draw(board)
    # mitre joints
    for cx, cy, sx_, sy_ in ((0, 0, 1, 1), (W - 1, 0, -1, 1), (0, H - 1, 1, -1), (W - 1, H - 1, -1, -1)):
        d.line([(cx, cy), (cx + sx_ * fw, cy + sy_ * fw)], fill=(40, 20, 8, 255), width=2)
    # scratches and nicks along the bars
    for _ in range(26):
        side = rng.choice(("top", "bottom", "left", "right"))
        a = rng.randint(30, (W if side in ("top", "bottom") else H) - 30)
        c = rng.randint(3, fw - 4)
        ln = rng.randint(5, 14)
        col = (60, 30, 12, 200) if rng.random() < 0.7 else (170, 110, 60, 200)
        if side == "top":
            d.line([(a, c), (a + ln, c + rng.choice((-1, 0, 1)))], fill=col)
        elif side == "bottom":
            d.line([(a, H - 1 - c), (a + ln, H - 1 - c + rng.choice((-1, 0, 1)))], fill=col)
        elif side == "left":
            d.line([(c, a), (c + rng.choice((-1, 0, 1)), a + ln)], fill=col)
        else:
            d.line([(W - 1 - c, a), (W - 1 - c + rng.choice((-1, 0, 1)), a + ln)], fill=col)
    # knots
    for _ in range(6):
        side = rng.choice(("top", "bottom", "left", "right"))
        a = rng.randint(60, (W if side in ("top", "bottom") else H) - 60)
        c = fw // 2
        kx, ky = {"top": (a, c), "bottom": (a, H - 1 - c), "left": (c, a), "right": (W - 1 - c, a)}[side]
        d.ellipse([kx - 4, ky - 2, kx + 4, ky + 2], fill=(62, 32, 12, 255))
        d.ellipse([kx - 2, ky - 1, kx + 2, ky + 1], fill=(44, 22, 8, 255))
    # nails along the frame
    for x in range(70, W - 60, 90):
        for y in (fw // 2, H - 1 - fw // 2):
            d.ellipse([x - 3, y - 3, x + 3, y + 3], fill=(48, 44, 40, 255))
            d.point((x - 1, y - 1), fill=(170, 160, 150, 255))
    for y in range(70, H - 60, 90):
        for x in (fw // 2, W - 1 - fw // 2):
            d.ellipse([x - 3, y - 3, x + 3, y + 3], fill=(48, 44, 40, 255))
            d.point((x - 1, y - 1), fill=(170, 160, 150, 255))
    # inner shadow where the frame meets the planks
    d.rectangle([fw, fw, W - 1 - fw, H - 1 - fw], outline=(30, 14, 4, 255), width=2)
    shadow = Image.new("RGBA", board.size, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rectangle([fw + 2, fw + 2, W - 3 - fw, H - 3 - fw], outline=(20, 8, 0, 120), width=5)
    board.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(3)))
    d = ImageDraw.Draw(board)
    for cx, cy, sx_, sy_ in ((0, 0, 1, 1), (W - 1, 0, -1, 1), (0, H - 1, 1, -1), (W - 1, H - 1, -1, -1)):
        poly = [(cx, cy), (cx + sx_ * 46, cy), (cx + sx_ * 46, cy + sy_ * 14), (cx + sx_ * 14, cy + sy_ * 14),
                (cx + sx_ * 14, cy + sy_ * 46), (cx, cy + sy_ * 46)]
        d.polygon(poly, fill=(58, 58, 64, 255), outline=(22, 22, 26, 255))
        for rx, ry in ((7, 7), (34, 7), (7, 34)):
            px_, py_ = cx + sx_ * rx, cy + sy_ * ry
            d.ellipse([px_ - 3, py_ - 3, px_ + 3, py_ + 3], fill=(120, 120, 128, 255))
            d.point((px_ - 1, py_ - 1), fill=(200, 200, 210, 255))

    dagger(112, 118, 225, length=104)

    # Header sign (carved, unreadable)
    sw, sh = 250, 40
    sx, sy = (W - sw) // 2, 3
    sign = Image.new("RGBA", (sw, sh), (0, 0, 0, 0))
    spx = sign.load()
    for y in range(sh):
        for x in range(sw):
            g = math.sin(x * 0.05 + y * 0.6) * 10 + rng.randint(-5, 5)
            spx[x, y] = (int(120 + g), int(70 + g * 0.7), int(34 + g * 0.4), 255)
    sd = ImageDraw.Draw(sign)
    sd.rectangle([0, 0, sw - 1, sh - 1], outline=(50, 26, 10, 255), width=3)
    sd.rectangle([4, 4, sw - 5, sh - 5], outline=(160, 100, 52, 255), width=1)
    big_scribble(sd, rng, sw // 2, (sh - 16) // 2, sw - 70, (46, 22, 8, 255), height=16, centred=True)
    for cx in (12, sw - 13):
        sd.ellipse([cx - 4, sh // 2 - 4, cx + 4, sh // 2 + 4], fill=(50, 44, 40, 255))
        sd.point((cx - 1, sh // 2 - 1), fill=(170, 160, 150, 255))
    board.alpha_composite(sign, (sx, sy))


    # Soft vignette
    vig = Image.new("L", board.size, 0)
    ImageDraw.Draw(vig).rectangle([60, 50, W - 60, H - 50], fill=255)
    vig = vig.filter(ImageFilter.GaussianBlur(60))
    dark = Image.new("RGBA", board.size, (20, 8, 0, 255))
    dark.putalpha(vig.point(lambda v: int((255 - v) * 0.35)))
    board.alpha_composite(dark)

    board.save(tex / "gui" / "board_fantasy.png")
    for old in ("card_fantasy.png", "row_fantasy.png"):
        if (tex / "gui" / old).exists():
            (tex / "gui" / old).unlink()


# ---------------------------------------------------------------- advancement icons (16x16 pixel art)

ICON_PALETTE = {
    "k": (46, 30, 18), "p": (240, 226, 188), "s": (204, 182, 138), "i": (96, 76, 58),
    "r": (200, 46, 40), "d": (124, 22, 22), "R": (232, 96, 84),
    "w": (248, 248, 252), "g": (176, 176, 188), "G": (120, 120, 132),
    "b": (132, 86, 46), "B": (84, 52, 26), "y": (240, 196, 70), "Y": (176, 126, 30), "o": (255, 236, 150),
    "c": (214, 212, 204), "C": (162, 160, 154), "D": (108, 106, 102),
    "e": (82, 182, 76), "E": (38, 112, 42), "l": (160, 102, 54), "L": (108, 64, 30),
    "u": (70, 120, 200), "U": (40, 70, 140),
}

ICONS = {
    # a pinned note and a quill
    "first_request": [
        "................",
        "......rr......w.",
        "..kkkkdrkkk..wwg",
        "..kppppppps.wwg.",
        "..kpiiiiips.wwg.",
        "..kppppppps.wg..",
        "..kpiiiiip.wwg..",
        "..kpppppp.wwg...",
        "..kpiiiii.wg.k..",
        "..kpppppwwg..k..",
        "..kpiiiwwg...k..",
        "..kppppwg...sk..",
        "..kpppbg...ssk..",
        "..ksssBssssssk..",
        "..kkkBkkkkkkkk..",
        "....B...........",
    ],
    # town crier's bell
    "requests_10": [
        "................",
        ".......kk.......",
        "......kBBk......",
        "......kkkk......",
        ".....kyyyyk.....",
        "....kyoyyyYk....",
        "....kyoyyyYk....",
        "....kyoyyyYk....",
        "...kyoyyyyyYk...",
        "...kyoyyyyyYk...",
        "..kyyyyyyyyyYk..",
        "..kYYYYYYYYYYk..",
        "..kkkkkkkkkkkk..",
        "......kYYk......",
        ".......kk.......",
        "................",
    ],
    # a royal decree: scroll with a gold seal
    "requests_100": [
        "................",
        "..kkkkkkkkkkkk..",
        ".kbBbbbbbbbbbBk.",
        "..kkkkkkkkkkkk..",
        "...kpppppppppk..",
        "...kpiiiiiiipk..",
        "...kppppppppsk..",
        "...kpiiiiiiisk..",
        "...kpppppppssk..",
        "...kpiiiiyyysk..",
        "...kpppppyoyYk..",
        "...kppppyoyyYk..",
        "..kkkkkkkYyYkk..",
        ".kbBbbbbbbYbbBk.",
        "..kkkkkkkkYkkk..",
        "..........Y.....",
    ],
    # a coin pouch, opened
    "patron": [
        "................",
        "......kkkk......",
        ".....kyoyyk.....",
        "....kyyyoyyk....",
        ".....kLkkLk.....",
        "......kLLk......",
        ".....kllllk.....",
        "....kllllllk....",
        "...kllyllllLk...",
        "...klllllllLk...",
        "..kllllyllllLk..",
        "..klllllllllLk..",
        "..kLllllllLLLk..",
        "...kLLLLLLLLk...",
        "....kkkkkkkk....",
        "................",
    ],
    # a quill signing a contract (accepting)
    "accept_request": [
        "................",
        "............kw..",
        "...........kwwg.",
        "..........kwwgk.",
        ".........kwwgk..",
        "..kkkkkkkwwgk...",
        "..kppppppwgkk...",
        "..kpiiiiwgkpk...",
        "..kpppppBkppk...",
        "..kpiiiBkpppk...",
        "..kpppppppppk...",
        "..kpieeepppsk...",
        "..kppeeeeppsk...",
        "..kpppppeessk...",
        "..kkkkkkkkkkk...",
        "................",
    ],
    # a stack of gold coins (reward share)
    "mission_accomplished": [
        "................",
        "................",
        "........kkkkk...",
        ".......kyoyyyk..",
        ".......kYyyyYk..",
        "...kkkkkkkkkkk..",
        "..kyoyyyykyyyk..",
        "..kYyyyyYkYyYk..",
        "..kkkkkkkkkkkk..",
        "..kyoyyyykyoyyk.",
        "..kYyyyyYkYyyYk.",
        "..kkkkkkkkkkkkk.",
        "..kyyoyyyyyyyyk.",
        "..kYYyyyyyyyYYk.",
        "...kkkkkkkkkkk..",
        "................",
    ],
    # a stone pillar
    "community_pillar": [
        "................",
        "..kkkkkkkkkkkk..",
        "..kccccccccccCk.",
        "..kkkkkkkkkkkkk.",
        "....kcCcCcCDk...",
        "....kcCcCcCDk...",
        "....kcCcCcCDk...",
        "....kcCcCcCDk...",
        "....kcCcCcCDk...",
        "....kcCcCcCDk...",
        "....kcCcCcCDk...",
        "....kcCcCcCDk...",
        "..kkkkkkkkkkkk..",
        "..kccccccccccCk.",
        ".kCCCCCCCCCCCCDk",
        ".kkkkkkkkkkkkkkk",
    ],
    # a golden laurel crown
    "village_legend": [
        "................",
        "................",
        "................",
        "...k...kk...k...",
        "..kok.koyk.kok..",
        "..kyk.kyyk.kyk..",
        "..kyykyyyykyyk..",
        "..kyyyyryyyyyk..",
        "..kyyyrdryyyyk..",
        "..kyyyyryyyyYk..",
        "..kYyyyyyyyYYk..",
        "..kkkkkkkkkkkk..",
        "..kyuyyyryyuYk..",
        "..kkkkkkkkkkkk..",
        "................",
        "................",
    ],
}


def generate_icon_textures(tex):
    from PIL import Image
    folder = tex / "item" / "icons"
    folder.mkdir(parents=True, exist_ok=True)
    for name, rows in ICONS.items():
        assert len(rows) == 16 and all(len(r) == 16 for r in rows), name
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch != ".":
                    img.putpixel((x, y), ICON_PALETTE[ch] + (255,))
        img.save(folder / f"{name}.png")


def generate_icon_models():
    """Per-icon models (shared). The item definition that picks one lives in generated/<version>/."""
    for name in ICONS:
        write_json(ASSETS / "models" / "item" / f"icon_{name}.json",
                   {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/icons/{name}"}})


if __name__ == "__main__":
    generate_json()
    textures()
    print("Resources generated.")
