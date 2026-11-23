"""Adapt Diarmuid's single-spear clips for Cu NPC spear techniques.

Only presentation is borrowed. Cu's server hit frames, costs and hit counts stay
in CuChulainnRules/AbilityService; these clips never invoke Diarmuid abilities.
"""
import json
import math
from pathlib import Path

DEST = Path(__file__).resolve().parents[1] / "src/main/resources/assets/typemoonworld/player_animations"


def part(x=0, y=0, z=0, pitch=0, yaw=0, roll=0):
    return dict(x=x, y=y, z=z, pitch=pitch, yaw=yaw, roll=roll)


def smooth(t):
    t = max(0, min(1, t))
    return t * t * (3 - 2 * t)


def curve(t, keys):
    for (a, av), (b, bv) in zip(keys, keys[1:]):
        if t <= b:
            return av + (bv - av) * smooth((t - a) / (b - a))
    return keys[-1][1]


def rotation(pitch=0, yaw=0, roll=0):
    p, y, r = map(math.radians, (pitch, yaw, roll))
    cp, sp, cy, sy, cr, sr = math.cos(p), math.sin(p), math.cos(y), math.sin(y), math.cos(r), math.sin(r)
    return [[cr*cy, cr*sy*sp-sr*cp, cr*sy*cp+sr*sp],
            [sr*cy, sr*sy*sp+cr*cp, sr*sy*cp-cr*sp], [-sy, cy*sp, cy*cp]]


def mul(a, b):
    return [[sum(a[r][k] * b[k][c] for k in range(3)) for c in range(3)] for r in range(3)]


def angles(matrix):
    return dict(pitch=math.degrees(math.atan2(matrix[2][1], matrix[2][2])),
                yaw=math.degrees(math.asin(max(-1, min(1, -matrix[2][0])))),
                roll=math.degrees(math.atan2(matrix[1][0], matrix[0][0])))


def sample(moves, tick):
    before = max((m for m in moves if m["tick"] <= tick), key=lambda m: m["tick"])
    after = min((m for m in moves if m["tick"] >= tick), key=lambda m: m["tick"])
    weight = 0 if before is after else (tick-before["tick"])/(after["tick"]-before["tick"])
    return {name: {axis: value + (after[name][axis]-value)*weight for axis, value in track.items()}
            for name, track in before.items() if name != "tick"}


def imported(source, tick):
    moves = source["emote"]["moves"]
    tracks = sample(moves, tick)
    # Keep only the red spear and upper body. Gae Bulg's blade is opposite to
    # Diarmuid's +Z mesh: rotate around the unchanged grip, not the NPC's arm.
    tracks = {name: tracks[name] for name in ("body", "torso", "rightArm", "leftArm", "rightItem")}
    item = tracks["rightItem"]
    item.update(angles(mul(rotation(**{k: item[k] for k in ("pitch", "yaw", "roll")}), rotation(roll=180))))
    return tracks


def write(name, end, pose, description):
    moves = []
    for tick in range(end+1):
        tracks = pose(tick)
        tracks = {n: {k: round(v, 4) for k, v in p.items()} for n, p in tracks.items()}
        moves.append(dict(tick=tick, **tracks))
    data = dict(name="cu_npc_"+name, version=3, description=description,
                emote=dict(beginTick=0, endTick=end, stopTick=end, degrees=True, moves=moves))
    (DEST/(data["name"]+".json")).write_text(json.dumps(data, indent=2)+"\n", encoding="utf-8")


def generate():
    dance = json.loads((DEST/"diarmuid_red_dance.json").read_text(encoding="utf-8"))
    sweep = json.loads((DEST/"diarmuid_red_sweep.json").read_text(encoding="utf-8"))
    write("sweep", 20, lambda t: imported(sweep, curve(t, [(0, 0), (3, 5), (6, 7), (8, 10), (10, 9), (12, 7), (14, 5), (20, 17)])),
          "Diarmuid red-spear sweep adapted to Cu's two hit frames 6/12; single Gae Bulg, original Cu damage and costs.")
    write("flurry", 20, lambda t: imported(dance, curve(t, [(0, 0), (4, 2), (8, 5), (12, 8), (20, 12)])),
          "Diarmuid red-spear dance retimed to Cu's 4/8/12 hits; presentation only, no Diarmuid effects.")
    write("dance", 12, lambda t: imported(dance, t),
          "Diarmuid red-spear flourish; one ordinary Cu NPC melee hit at frame 5 regardless of visible turns.")
    for name, path in (("thrust", "diarmuid_red_basic"), ("slash", "diarmuid_red_basic_slash_right")):
        source = json.loads((DEST/(path+".json")).read_text(encoding="utf-8"))
        end = source["emote"]["endTick"]
        write(name, 12, lambda t, s=source, e=end: imported(s, curve(t, [(0, 0), (5, 2), (12, e)])),
              "Diarmuid single-spear "+name+" adapted to Cu; one ordinary hit at frame 5, no extra damage or control.")
    print("Generated five Cu NPC spear clips adapted from Diarmuid.")


if __name__ == "__main__":
    generate()
