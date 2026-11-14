"""Adapt Cu/Diarmuid single-spear motions to Yajiao's model and display transform.

These are presentation clips; hit counts, damage, resources and AI timing are unchanged.
"""
import copy
import json
import math
from pathlib import Path
from generate_cu_npc_animations import sample

ASSETS = Path(__file__).resolve().parents[1] / "src/main/resources/assets/typemoonworld"
DEST = ASSETS / "player_animations"


def mul(a, b):
    return [[sum(a[r][k] * b[k][c] for k in range(3)) for c in range(3)] for r in range(3)]


def transpose(a):
    return list(map(list, zip(*a)))


def axes(pitch, yaw, roll):
    p, y, r = map(math.radians, (pitch, yaw, roll))
    cp, sp, cy, sy, cr, sr = math.cos(p), math.sin(p), math.cos(y), math.sin(y), math.cos(r), math.sin(r)
    return ([[1, 0, 0], [0, cp, -sp], [0, sp, cp]],
            [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]],
            [[cr, -sr, 0], [sr, cr, 0], [0, 0, 1]])


def rotation(pitch=0, yaw=0, roll=0):
    x, y, z = axes(pitch, yaw, roll)
    return mul(z, mul(y, x))


def display(name):
    data = json.loads((ASSETS / f"models/item/{name}.json").read_text(encoding="utf-8"))
    x, y, z = axes(*data["display"]["thirdperson_righthand"]["rotation"])
    return mul(x, mul(y, z))


def angles(matrix):
    return dict(pitch=math.degrees(math.atan2(matrix[2][1], matrix[2][2])),
                yaw=math.degrees(math.asin(max(-1, min(1, -matrix[2][0])))),
                roll=math.degrees(math.atan2(matrix[1][0], matrix[0][0])))


def blend(a, b, weight):
    return {bone: {axis: value + (b[bone][axis] - value) * weight
                   for axis, value in track.items()} for bone, track in a.items()}


def continuous_angles(mapped, previous):
    if previous is None:
        return mapped
    # Choose both equivalent Euler representations before unwrapping; this prevents
    # the wrist reversing at a gimbal seam even when the weapon direction is smooth.
    alternate = dict(pitch=mapped['pitch'] + 180, yaw=180 - mapped['yaw'], roll=mapped['roll'] + 180)
    choices = [{k: v + 360 * round((previous[k] - v) / 360) for k, v in candidate.items()}
               for candidate in (mapped, alternate)]
    return min(choices, key=lambda c: sum((c[k] - previous[k]) ** 2 for k in c))


def generate():
    # Cu's -Z blade and Yajiao's baked +X blade require a quarter turn at the grip.
    correction = mul(rotation(roll=180), mul(display("gae_bulg"), mul(rotation(yaw=90), transpose(display("yajiao_qiang")))))
    sources = {name: json.loads((DEST / f"{name}.json").read_text(encoding="utf-8"))
               for name in ('diarmuid_red_basic', 'diarmuid_red_basic_slash_right', 'diarmuid_red_dance', 'diarmuid_figure_eight')}

    def imported(name, tick):
        tracks = sample(sources[name]['emote']['moves'], tick)
        return {bone: tracks[bone] for bone in ('body', 'torso', 'rightArm', 'leftArm', 'rightItem')}

    neutral = imported('diarmuid_red_basic', 0)

    def basic(source, tick, end):
        # Retain the contact at tick 5, give the outside recovery turn time to read.
        src = sources[source]['emote']
        source_tick = tick * 2 / 5 if tick <= 5 else 2 + (tick - 5) * (src['endTick'] - 2) / (end - 5)
        return imported(source, source_tick)

    def recovery(tick):
        progress = tick / 12
        weight = math.sin(math.pi * progress) ** 2
        pose = copy.deepcopy(neutral)
        pose['rightArm'].update(pitch=-64 + 2 * weight, yaw=-64 + 22 * weight, roll=-8 - 4 * weight)
        pose['leftArm']['pitch'] -= 24 * weight
        # One deliberate outward turn, then lower the elbow and bring the shaft back.
        pose['rightItem']['pitch'] += 360 * (progress * progress * (3 - 2 * progress))
        pose['rightItem']['yaw'] += 26 * weight
        pose['rightItem']['roll'] -= 18 * weight
        return pose

    def held(tick):
        cycle = imported('diarmuid_figure_eight', 10 + (max(4, tick) - 4) % 8)
        # Zhao Yun carries one spear. Keep the free arm as a compact counterbalance
        # instead of copying the second spear's shoulder circle.
        cycle['leftArm'].update(pitch=-28, yaw=14, roll=-8)
        return blend(neutral, cycle, min(1, tick / 4))

    def combo(tick):
        if tick < 16:
            return basic('diarmuid_red_basic', tick, 16)
        local = tick - 16
        pose = basic('diarmuid_red_basic_slash_right', local, 16)
        return blend(neutral, pose, min(1, local / 3))

    clips = (
        ('thrust', 16, False, lambda t: basic('diarmuid_red_basic', t, 16)),
        ('sweep', 18, False, lambda t: basic('diarmuid_red_basic_slash_right', t, 18)),
        ('dance', 24, False, lambda t: imported('diarmuid_red_dance', t / 2)),
        ('combo', 31, True, combo),
        ('held_dance', 11, True, held),
        ('recovery', 12, False, recovery),
    )
    for name, end, loop, pose in clips:
        moves = []
        previous = None
        for tick in range(end + 1):
            frame = copy.deepcopy(pose(tick))
            frame['tick'] = tick
            for track in ("body", "torso"):
                for axis in ("x", "y", "z"):
                    frame[track][axis] = 0
            item = frame["rightItem"]
            mapped = angles(mul(rotation(**{k: item[k] for k in ("pitch", "yaw", "roll")}), correction))
            # Unwrap equivalent Euler angles to avoid a full reverse turn at the +/-180 seam.
            mapped = continuous_angles(mapped, previous)
            previous = mapped
            item.update({k: round(v, 4) for k, v in mapped.items()})
            moves.append(frame)
        path = "zhao_yun_spear_" + name
        emote = dict(beginTick=0, endTick=end, stopTick=end, degrees=True, moves=moves)
        if loop:
            emote.update(isLoop=True, returnTick=4 if name == 'held_dance' else 0)
        output = dict(name=path, version=3,
                      description="Diarmuid single-spear technique with outside wrist recovery, adapted to Yajiao; server owns hits.",
                      emote=emote)
        (DEST / f"{path}.json").write_text(json.dumps(output, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    generate()
