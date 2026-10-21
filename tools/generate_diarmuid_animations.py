"""Author full-body dual-spear tracks and project server definitions from DiarmuidAction."""
import json
import math
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"
JAVA = ROOT / "src/main/java/net/xxxjk/TYPE_MOON_WORLD/servant/diarmuid/DiarmuidAction.java"
TEXT = JAVA.read_text(encoding="utf-8")
PATTERN = re.compile(r'^   (\w+)\((-?\d+), "([a-z_]+)", (\d+), (\d+), (\d+), (\d+), (\d+)(.*?)(?=^   [A-Z_]+\(|^   public static final)', re.M | re.S)
HIT = re.compile(r'hit\((\d+), (true|false), (\d+), (\d+)\)')


def dump(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def part(x=0, y=0, z=0, pitch=0, yaw=0, roll=0):
    return dict(zip(("x", "y", "z", "pitch", "yaw", "roll"),
                    (round(v, 4) for v in (x, y, z, pitch, yaw, roll))))


def pulse(tick, center, before=3, after=3):
    return max(0, 1 - abs(tick - center) / (before if tick < center else after))


def original_pose(path, tick, duration, hits):
    # Independent thrust, sweep and recovery curves; the long and short weapons never share a track.
    fade = min(1, tick / 2) * min(1, (duration - tick) / 3)
    red = max((pulse(tick, t, 4, 3) for t, r, _, _ in hits if r), default=0)
    yellow = max((pulse(tick, t, 2, 2) for t, r, _, _ in hits if not r), default=0)
    gather = max((pulse(tick, t - 3, 2, 3) for t, _, _, _ in hits), default=0)
    sweep = path in ("red_sweep", "low_spin", "figure_eight")
    low = path in ("cross_thrust", "low_spin")
    phase = tick / max(1, duration)
    turn = (-28 * gather + 34 * red - 22 * yellow) * fade
    bend = (18 if low else 5) * fade + 9 * red
    right_yaw = (-40 + 70 * phase) * fade if sweep else (-20 * gather + 18 * red) * fade
    left_yaw = (24 - 45 * yellow) * fade
    result = {
        "body": part(y=-1.5 * fade if low else 0),
        "head": part(pitch=-bend * 0.4, yaw=-turn * 0.4),
        "torso": part(pitch=bend, yaw=turn, roll=(5 * red - 4 * yellow) * fade),
        "rightArm": part(-5, 2, -2.5 * red, -52 * fade - 36 * red + 18 * gather, right_yaw, (18 + 15 * red) * fade),
        "leftArm": part(5, 2, -1.7 * yellow, -58 * fade - 38 * yellow + 20 * red, left_yaw, (-25 - 12 * yellow) * fade),
        "rightLeg": part(-1.9, 12, 1.3 * fade, (18 + 22 * red) * fade, -8 * fade, 5 * fade),
        "leftLeg": part(1.9, 12, -1.3 * fade, (-20 - 16 * yellow) * fade, 8 * fade, -5 * fade),
        "rightItem": part(z=-7 * red + 3 * gather, pitch=-16 * red, yaw=16 * red, roll=5 * red),
        "leftItem": part(z=-4 * yellow + 2 * red, pitch=-24 * yellow, yaw=-20 * yellow, roll=-10 * yellow),
    }
    if path in ("advancing_thrust", "retreating_thrust", "rose_dance"):
        result["body"]["z"] = round((-1 if path == "retreating_thrust" else 1) * 1.5 * fade, 4)
    if path == "rising_thrust":
        result["rightArm"]["pitch"] -= 55 * red
        result["rightItem"]["pitch"] -= 42 * red
        result["torso"]["pitch"] -= 17 * red
        result["body"]["y"] = -2 * gather + red
    if path == "red_sweep" or path == "low_spin":
        arc = (tick / max(1, hits[-1][0]) * (240 if low else 180) - 90) * fade
        result["rightItem"]["yaw"] = arc
        result["torso"]["yaw"] = arc * 0.42
        result["leftItem"]["yaw"] = -arc * 0.4
    if path == "figure_eight":
        theta = (tick - 2) * math.pi / 4
        for name, sign, offset in (("rightArm", 1, 0), ("leftArm", -1, math.pi)):
            result[name]["pitch"] = (-90 + 65 * math.sin(theta + offset)) * fade
            result[name]["yaw"] = 65 * math.sin((theta + offset) / 2) * fade
            result[name]["roll"] = sign * (45 + 55 * math.cos(theta + offset)) * fade
        for name, offset in (("rightItem", 0), ("leftItem", math.pi)):
            result[name]["pitch"] = 140 * math.sin(theta + offset) * fade
            result[name]["yaw"] = 75 * math.sin(2 * theta + offset) * fade
            result[name]["z"] = 3 * math.cos(theta + offset) * fade
        result["torso"]["yaw"] = 26 * math.sin(theta) * fade
    if path == "rose_dance" and tick < 7:
        twirl = math.sin(tick * math.pi / 7) * fade
        result["rightArm"]["roll"] += 75 * twirl
        result["leftArm"]["roll"] -= 75 * twirl
        result["rightItem"]["pitch"] = -130 * twirl
        result["leftItem"]["pitch"] = 130 * twirl
    if path == "cross_guard":
        result["rightArm"] = part(-5, 2, -1.5 * fade, -82 * fade, -45 * fade, 45 * fade)
        result["leftArm"] = part(5, 2, -1.5 * fade, -82 * fade, 45 * fade, -45 * fade)
        result["rightItem"]["roll"] = 45 * fade
        result["leftItem"]["roll"] = -45 * fade
    if path == "mana_leap":
        flight = max(0, math.sin(math.pi * min(1, max(0, (tick - 4) / 16))))
        result["torso"]["pitch"] = 20 * flight
        result["rightArm"]["pitch"] = (40 * flight - 35 * fade)
        result["leftArm"]["pitch"] = (40 * flight - 35 * fade)
        result["rightLeg"]["pitch"] = -65 * flight
        result["leftLeg"]["pitch"] = 45 * flight
        result["rightItem"]["yaw"] = -35 * flight
        result["leftItem"]["yaw"] = 35 * flight
    return result


def smooth(value):
    value = max(0, min(1, value))
    return value * value * (3 - 2 * value)


def pose(path, tick, duration, hits):
    # No root/pelvis translation: displacement belongs to the authoritative entity timeline.
    # Item rotations compensate the raised forearm so thrusts remain level with the target.
    fade = smooth(tick / 2) * smooth((duration - tick) / 3)
    red = max((pulse(tick, t, 3, 3) for t, r, _, _ in hits if r), default=0)
    yellow = max((pulse(tick, t, 2, 2) for t, r, _, _ in hits if not r), default=0)
    low = path in ('cross_thrust', 'low_spin')
    result = {
        'body': part(), 'head': part(pitch=-3 * fade),
        'torso': part(pitch=(10 if low else 4) * fade, yaw=(10 * red - 9 * yellow) * fade),
        'rightArm': part(-5, 2, pitch=(-58 - 28 * red) * fade, yaw=(-10 + 10 * red) * fade, roll=8 * fade),
        'leftArm': part(5, 2, pitch=(-53 - 32 * yellow) * fade, yaw=(12 - 12 * yellow) * fade, roll=-8 * fade),
        'rightLeg': part(-1.9, 12, pitch=(10 + 10 * red) * fade, yaw=-5 * fade),
        'leftLeg': part(1.9, 12, pitch=(-10 - 9 * yellow) * fade, yaw=5 * fade),
        'rightItem': part(pitch=(-58 - 28 * red) * fade),
        'leftItem': part(pitch=(-53 - 32 * yellow) * fade),
    }
    if path == 'rising_thrust':
        lift = smooth((tick - 2) / 5) * smooth((duration - tick) / 4)
        result['rightArm']['pitch'] = (-55 - 65 * lift) * fade
        result['rightItem']['pitch'] = (-55 - 20 * lift) * fade
        result['torso']['pitch'] = (8 - 14 * lift) * fade
    if path in ('red_sweep', 'low_spin', 'landing_sweep'):
        # Wind up at the waist, drive the sweep with the shoulders, then recover.
        # The grip stays fixed in the palm; neither wrist spins through a full turn.
        double = path != 'red_sweep'
        first = smooth((tick - 2) / 6)
        second = smooth((tick - 6) / 6) if double else 0
        turn = (-38 + 76 * first - 48 * second) * fade
        result['body'] = part(pitch=(5 if double else 2) * fade, roll=-3 * math.sin(tick * math.pi / duration) * fade)
        result['torso'] = part(pitch=(13 if double else 7) * fade, yaw=turn)
        result['head']['yaw'] = -turn * 0.6
        result['rightArm'] = part(-5, 2, pitch=(-70 - 12 * math.sin(first * math.pi)) * fade,
                                  yaw=(-32 + 72 * first) * fade, roll=12 * fade)
        result['rightItem'] = part(pitch=-76 * fade, yaw=(-16 + 32 * first) * fade)
        if double:
            result['leftArm'] = part(5, 2, pitch=(-66 - 16 * math.sin(second * math.pi)) * fade,
                                     yaw=(32 - 70 * second) * fade, roll=-12 * fade)
            result['leftItem'] = part(pitch=-72 * fade, yaw=(16 - 32 * second) * fade)
    if path == 'figure_eight':
        # Preserve the accepted eight-shaped gunflower, extending its cyclic middle section.
        result = original_pose(path, 2 + ((tick - 2) % 8), 36, [])
        for track in result.values():
            for channel in ('pitch', 'yaw', 'roll'):
                track[channel] *= fade
        result['body'] = part(pitch=3 * math.sin(tick * math.pi / 4) * fade, roll=4 * math.sin(tick * math.pi / 4) * fade)
        for name in ('rightArm', 'leftArm'):
            result[name]['z'] = 0
        for name in ('rightItem', 'leftItem'):
            result[name]['x'] = result[name]['y'] = result[name]['z'] = 0
    if path in ('red_dance', 'yellow_dance'):
        left = path == 'yellow_dance'
        # The actual skill-8 shoulder loop, sampled over two complete revolutions.
        dance = original_pose('figure_eight', 2 + tick * 1.4, 24, [])
        arm, item = ('leftArm', 'leftItem') if left else ('rightArm', 'rightItem')
        result[arm] = dance[arm]
        result[arm]['z'] = 0
        result[item] = part(pitch=dance[item]['pitch'] * fade, yaw=dance[item]['yaw'] * fade)
        result[arm]['pitch'] *= fade
        result[arm]['yaw'] *= fade
        result[arm]['roll'] *= fade
        result['torso']['yaw'] = 12 * math.sin(tick * math.pi / 4) * fade
    if path == 'rose_dance' and tick <= 7:
        twirl = original_pose('figure_eight', tick, 26, [])
        for name in ('rightArm', 'leftArm', 'rightItem', 'leftItem'):
            result[name] = twirl[name]
            result[name]['z'] = 0
    if path == 'cross_guard':
        result['rightArm'] = part(-5, 2, pitch=-78 * fade, yaw=-18 * fade, roll=12 * fade)
        result['leftArm'] = part(5, 2, pitch=-78 * fade, yaw=18 * fade, roll=-12 * fade)
        result['rightItem'] = part(pitch=-78 * fade, yaw=42 * fade)
        result['leftItem'] = part(pitch=-78 * fade, yaw=-42 * fade)
    if path == 'mana_leap':
        flight = smooth((tick - 3) / 5)
        result['body'] = part(pitch=8 * flight)
        result['torso']['pitch'] = 10 * flight
        result['rightArm']['pitch'] = -28 * fade
        result['leftArm']['pitch'] = -32 * fade
        result['rightItem'] = part(pitch=-25 * fade, yaw=-20 * flight)
        result['leftItem'] = part(pitch=-28 * fade, yaw=20 * flight)
        result['rightLeg']['pitch'] = -48 * flight
        result['leftLeg']['pitch'] = 40 * flight
    if path == 'double_spear_stance':
        for name in ('rightArm', 'leftArm'):
            result[name]['pitch'] = -42 * fade
        result['rightItem'] = part(pitch=-36 * fade, yaw=-15 * fade)
        result['leftItem'] = part(pitch=-36 * fade, yaw=15 * fade)
    # Hands keep their shoulder pivots and weapons keep their palms as rotation centers.
    result['body']['x'] = result['body']['y'] = result['body']['z'] = 0
    for name in ('rightItem', 'leftItem'):
        result[name]['x'] = result[name]['y'] = result[name]['z'] = 0
    return result


actions = []
names_zh = ["伏身·交叉刺", "红蔷薇·踏进突刺", "黄蔷薇·连环点刺", "回身·长枪横扫", "挑枪·上扬刺",
            "伏身·双枪回旋", "退步·回马枪", "魔力放出·跃身", "骑士武略·交叉架枪", "枪舞·八字回环", "双蔷薇·贯击连舞",
            "红蔷薇·探刺", "黄蔷薇·点刺", "骑士武略·反刺", "双枪架势（开关）", "红蔷薇·单手八字枪舞", "黄蔷薇·单手八字枪舞", "落地·双枪横扫"]
matches = list(PATTERN.finditer(TEXT))
assert len(matches) == 18, len(matches)
for index, match in enumerate(matches):
    constant, slot, path, mana, cooldown, duration, angle, count, tail = match.groups()
    slot, mana, cooldown, duration, angle, count = map(int, (slot, mana, cooldown, duration, angle, count))
    hits = [(int(t), r == "true", int(d), int(l)) for t, r, d, l in HIT.findall(tail)]
    if path == 'figure_eight':
        hits = [(t, ((t - 6) // 4) % 2 == 0, 10, 4 if ((t - 6) // 4) % 2 == 0 else 3)
                for t in range(6, duration, 4)]
    moves = [{"tick": t, **pose(path, t, duration, hits)} for t in range(duration + 1)]
    dump(RES / f"assets/typemoonworld/player_animations/diarmuid_{path}.json", {
        "name": f"diarmuid_{path}", "version": 3,
        "description": "Authored full-body dual-spear technique; shared by PlayerAnimator and NPC sampling.",
        "emote": {"beginTick": 0, "endTick": duration, "stopTick": duration, "degrees": True, "moves": moves}})
    reach = max((h[3] for h in hits), default=6 if slot == 6 else 5 if path == 'cross_guard' else 0)
    windup = hits[0][0] if hits else (4 if slot == 6 else 2)
    last = hits[-1][0] if hits else (20 if slot == 6 else min(12, duration - 1))
    actions.append({
        "id": f"typemoonworld:diarmuid/{path}", "tags": ["melee", "area" if count > 1 else "pursuit"] if hits else ["guard" if slot == 7 else "gap_closer"],
        "minimum_range": 0, "maximum_range": reach, "mana_cost": mana, "stamina_cost": 0,
        "timing": {"windup": windup, "active": max(1, last - windup + 1), "recovery": max(0, duration - last - 1)},
        "threat": {"shape": "sphere" if angle >= 240 else "cone", "radius": reach if count > 1 else 0.5,
                   "length": reach, "danger": 4 if slot == 9 else 3,
                   "blockable": bool(hits), "dodgeable": bool(hits), "interruptible": True, "friendly_fire_radius": 0},
        "terrain_impact": "none"})

dump(RES / "data/typemoonworld/servant/actions/diarmuid_ua_duibhne.json", {"servant": "diarmuid_ua_duibhne", "actions": actions})

for locale in ("zh_cn", "en_us"):
    file = RES / f"assets/typemoonworld/lang/{locale}.json"
    values = json.loads(file.read_text(encoding="utf-8-sig"))
    for index, match in enumerate(matches):
        path = match.group(3)
        values[f"skill.typemoonworld.servant_card.diarmuid_{path}"] = names_zh[index] if locale == "zh_cn" else path.replace("_", " ").title()
    values["trait.typemoonworld.beloved"] = "所爱之人" if locale == "zh_cn" else "Beloved"
    values["trait.typemoonworld.fianna_knights"] = "费奥纳骑士" if locale == "zh_cn" else "Fianna Knights"
    values["trait.typemoonworld.knight"] = "骑士" if locale == "zh_cn" else "Knight"
    # Preserve existing formatting and modifications; only append missing entries.
    original = file.read_text(encoding="utf-8-sig")
    existing = json.loads(original)
    additions = {k: v for k, v in values.items() if k not in existing}
    if additions:
        end = original.rfind("}")
        prefix = original[:end].rstrip()
        body = json.dumps(additions, ensure_ascii=False, indent=2)[1:-1].strip("\n")
        file.write_text(prefix + ",\n" + body + "\n}" + original[end + 1:], encoding="utf-8")

print(f"Generated {len(matches)} full-body animations and shared server action metadata.")
