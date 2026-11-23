"""Author third-person upper-body spear tracks and shared action definitions."""
import json
import math
import re
import argparse
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


def curve(tick, keys):
    for (start, a), (end, b) in zip(keys, keys[1:]):
        if tick <= end:
            return a + (b - a) * smooth((tick - start) / (end - start))
    return keys[-1][1]


def multiply(a, b):
    return [[sum(a[row][k] * b[k][col] for k in range(3))
             for col in range(3)] for row in range(3)]


def rotation(pitch=0, yaw=0, roll=0):
    p, y, r = map(math.radians, (pitch, yaw, roll))
    cp, sp, cy, sy, cr, sr = math.cos(p), math.sin(p), math.cos(y), math.sin(y), math.cos(r), math.sin(r)
    return [[cr * cy, cr * sy * sp - sr * cp, cr * sy * cp + sr * sp],
            [sr * cy, sr * sy * sp + cr * cp, sr * sy * cp - cr * sp],
            [-sy, cy * sp, cy * cp]]


def aimed_item(arm, yaw=0, elevation=0):
    # Invert the real third-person hand/display transforms, keeping the shaft on its aim line.
    grip = multiply(rotation(**{axis: arm[axis] for axis in ('pitch', 'yaw', 'roll')}),
                    multiply(rotation(pitch=-90), rotation(yaw=180)))
    target = multiply(rotation(yaw=yaw), multiply(rotation(pitch=-elevation), rotation(yaw=180)))
    item = multiply(multiply(list(map(list, zip(*grip))), target), rotation(pitch=90))
    return part(pitch=math.degrees(math.atan2(item[2][1], item[2][2])),
                yaw=math.degrees(math.asin(max(-1, min(1, -item[2][0])))),
                roll=math.degrees(math.atan2(item[1][0], item[0][0])))


def drag_pose():
    red = part(-5, 2, pitch=22, yaw=-15, roll=-6)
    yellow = part(5, 2, pitch=18, yaw=15, roll=6)
    # Reverse the screenshot's shafts at the unchanged grips, then lower the tips by 45 degrees.
    return {'body': part(), 'torso': part(), 'rightArm': red, 'leftArm': yellow,
            'rightItem': aimed_item(red, yaw=70, elevation=-45),
            'leftItem': aimed_item(yellow, yaw=-70, elevation=-45)}


def ready_pose():
    red = part(-5, 2, pitch=-40, yaw=-20, roll=-8)
    yellow = part(5, 2, pitch=-30, yaw=18, roll=8)
    return {'body': part(), 'torso': part(), 'rightArm': red, 'leftArm': yellow,
            'rightItem': aimed_item(red, yaw=12, elevation=-28),
            'leftItem': aimed_item(yellow, yaw=-16, elevation=-36)}


def thrust_pose(path, tick, duration, hits):
    result = ready_pose()
    # A chambered point already faces the target; the drive sends it forward, not up from the floor.
    result['rightArm'] = part(-5, 2, pitch=-64, yaw=-64, roll=-8)
    result['leftArm'] = part(5, 2, pitch=-56, yaw=56, roll=8)
    red = max((smooth(pulse(tick, t, min(2, max(1, t - 1)), 3)) for t, r, _, _ in hits if r), default=0)
    yellow = max((smooth(pulse(tick, t, min(2, max(1, t - 1)), 2)) for t, r, _, _ in hits if not r), default=0)
    release = smooth((duration - tick) / min(2, duration - hits[-1][0]))
    red *= release
    yellow *= release
    drive = max(red, yellow)
    recover = smooth((duration - tick) / 3)
    turn = (-38 * red + 32 * yellow) * recover
    if path == 'retreating_thrust':
        turn += curve(tick, [(0, 0), (4, 78), (7, 38), (9, 0), (duration, 0)])
    result['torso'] = part(pitch=18 * drive, yaw=turn, roll=-4 * red + 3 * yellow)
    for arm, item, side, attack in (('rightArm', 'rightItem', 1, red), ('leftArm', 'leftItem', -1, yellow)):
        rest = result[arm]
        result[arm] = part(-5 * side, 2,
                           pitch=rest['pitch'] + ((-96 if side == 1 else -84) - rest['pitch']) * attack,
                           yaw=rest['yaw'] * (1 - attack), roll=rest['roll'] * (1 - attack))
        yaw = (8 if side == 1 else -10) * (1 - attack)
        elevation = 0 if side == 1 else -8
        if path == 'rising_thrust' and side == 1:
            result[arm]['pitch'] -= 30 * attack
            elevation += 48 * attack
        # Aim in entity space even while the shoulders turn into the strike.
        result[item] = aimed_item(result[arm], yaw=yaw, elevation=elevation)
    # A small diagonal link occupies the space between the two yellow hits.
    # It vanishes at both contacts, preserving their aim and the short thrust lane.
    if path == 'yellow_double' and len(hits) >= 2:
        bridge = math.sin(math.pi * max(0, min(1, (tick - hits[0][0]) / max(1, hits[1][0] - hits[0][0])))) ** 2
        result['leftArm']['roll'] += 12 * bridge
        result['leftItem']['yaw'] -= 30 * bridge
        result['leftItem']['pitch'] += 20 * bridge
    if path == 'guard_counter':
        result['leftArm'] = part(5, 2, pitch=-56, yaw=56, roll=8)
        result['leftItem'] = aimed_item(result['leftArm'], yaw=-10, elevation=-8)
    return result


def pose(path, tick, duration, hits):
    outro = min(3, duration - hits[-1][0]) if hits else 3
    fade = smooth(tick / min(2, hits[0][0] if hits else 2)) * smooth((duration - tick) / outro)
    if path == 'mana_leap':
        fade *= smooth((14 - tick) / 4)
    red = max((smooth(pulse(tick, t, 2, 3)) for t, r, _, _ in hits if r), default=0)
    yellow = max((smooth(pulse(tick, t, 2, 2.5)) for t, r, _, _ in hits if not r), default=0)
    red_draw = max((smooth(pulse(tick, t - 2, 2, 2)) for t, r, _, _ in hits if r), default=0)
    yellow_draw = max((smooth(pulse(tick, t - 2, 2, 2)) for t, r, _, _ in hits if not r), default=0)
    low = path in ('cross_thrust', 'low_spin')
    result = {
        'body': part(),
        'torso': part(pitch=4 * fade),
        'rightArm': part(-5, 2, pitch=-40 * fade, yaw=-20 * fade),
        'leftArm': part(5, 2, pitch=-36 * fade, yaw=25 * fade),
        'rightItem': part(pitch=-40 * fade),
        'leftItem': part(pitch=-36 * fade),
    }
    if path in ('cross_thrust', 'advancing_thrust', 'yellow_double', 'rising_thrust',
                'retreating_thrust', 'rose_dance', 'red_basic', 'yellow_basic', 'guard_counter'):
        drive, side = max(red, yellow), red - yellow
        result['torso'] = part(pitch=((6 if low else 3) + 14 * drive) * fade,
                               yaw=(22 * red_draw - 38 * red - 22 * yellow_draw + 38 * yellow) * fade,
                               roll=-4 * side * fade)
        result['rightArm'] = part(-5, 2, pitch=(-40 + 24 * red_draw - 58 * red + 14 * yellow) * fade,
                                  yaw=(-25 + 25 * red - 10 * red_draw) * fade)
        result['leftArm'] = part(5, 2, pitch=(-36 + 22 * yellow_draw - 62 * yellow + 14 * red) * fade,
                                 yaw=(25 - 25 * yellow + 10 * yellow_draw) * fade)
        result['rightItem'] = aimed_item(result['rightArm'], yaw=-18 * (1 - red) * fade)
        result['leftItem'] = aimed_item(result['leftArm'], yaw=22 * (1 - yellow) * fade)
    if path == 'rising_thrust':
        lift = curve(tick, [(0, 0), (3, 0), (6, 1), (9, 1), (duration, 0)])
        result['rightArm']['pitch'] = (-40 - 95 * lift) * fade
        result['rightItem'] = aimed_item(result['rightArm'], elevation=48 * lift * fade)
        result['torso']['pitch'] = (4 - 9 * lift) * fade
    if path == 'red_sweep':
        hit = hits[0][0]
        arc = curve(tick, [(0, -30), (2, -112), (hit - 2, -112), (hit, 0), (hit + 2, 118), (duration, 25)])
        turn = curve(tick, [(0, 0), (2, -58), (hit - 2, -58), (hit, 12), (hit + 2, 66), (duration, 0)])
        result['torso'] = part(pitch=5 * fade, yaw=turn * fade,
                               roll=3 * math.sin(tick * math.pi / duration) * fade)
        result['rightArm'] = part(-5, 2, pitch=-82 * fade, yaw=arc * fade)
        result['rightItem'] = aimed_item(result['rightArm'], yaw=arc * fade)
    if path in ('low_spin', 'landing_sweep'):
        # Parallel, separated low grips open outward, brace, then twirl home.
        arc = curve(tick, [(0, 0), (2, 0), (6, 120), (9, 120), (12, 65), (duration, 0)])
        shoulder = curve(tick, [(0, 0), (2, 0), (6, 120), (9, 120), (12, 65), (duration, 0)])
        brace = curve(tick, [(0, 0), (2, 0.15), (6, 0.8), (8, 1), (10, 1), (duration, 0)])
        tuck = smooth(pulse(tick, 2, 2, 3))
        result['torso'] = part(y=0.35 * brace, pitch=3 * fade + 4 * brace)
        for arm, item, side in (('rightArm', 'rightItem', 1), ('leftArm', 'leftItem', -1)):
            result[arm] = part(-5 * side, 2 - 0.8 * tuck,
                               pitch=curve(tick, [(0, 0), (2, -68), (6, -78), (9, -74), (12, -50), (duration, 0)]),
                               yaw=side * shoulder,
                               roll=side * curve(tick, [(0, 0), (2, 0), (6, -12), (9, -18), (13, 20), (duration, 0)]))
            elevation = curve(tick, [(0, 0), (2, 18), (6, -6), (9, -10), (12, 10), (duration, 0)])
            result[item] = aimed_item(result[arm], yaw=side * arc, elevation=elevation)
            result[item]['pitch'] += side * 360 * smooth((tick - 10) / 5)
    if path == 'figure_eight':
        # Preserve the accepted eight-shaped gunflower, extending its cyclic middle section.
        result = original_pose(path, 2 + ((tick - 2) % 8), 36, [])
        for track in result.values():
            for channel in ('pitch', 'yaw', 'roll'):
                track[channel] *= fade
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
        # Release the takeoff pose while the player steers the remaining flight.
        flight = smooth((tick - 3) / 5) * fade
        result['torso']['pitch'] = 10 * flight
        result['rightArm']['pitch'] = -28 * fade
        result['leftArm']['pitch'] = -32 * fade
        result['rightItem'] = part(pitch=-25 * fade, yaw=-20 * flight)
        result['leftItem'] = part(pitch=-28 * fade, yaw=20 * flight)
    if path in ('cross_thrust', 'advancing_thrust', 'yellow_double', 'rising_thrust',
                'retreating_thrust', 'red_basic', 'yellow_basic', 'guard_counter'):
        result = thrust_pose(path, tick, duration, hits)
    if path == 'cross_guard':
        result = ready_pose()
        result['rightArm'] = part(-5, 2, pitch=-76, yaw=-12, roll=-8)
        result['rightItem'] = aimed_item(result['rightArm'], yaw=12, elevation=12)
    if path == 'mana_leap':
        result = drag_pose()
        release = smooth((14 - tick) / 4)
        for arm in ('rightArm', 'leftArm'):
            for axis in ('pitch', 'yaw', 'roll'):
                result[arm][axis] *= release
        result['rightItem'] = aimed_item(result['rightArm'], yaw=140 * release, elevation=-20 * release)
        result['leftItem'] = aimed_item(result['leftArm'], yaw=-20 * release, elevation=-30 * release)
        if tick >= 14:
            result['rightItem'] = result['leftItem'] = part()
    if path == 'double_spear_stance':
        result = drag_pose()
    if path == 'rose_dance' and tick < 8:
        result = thrust_pose(path, tick, duration, hits)
    if path in ('rose_dance', 'red_sweep'):
        # Flourishes occupy the gaps around strikes; the authored hit poses remain exact.
        clear = min(1, min(abs(tick - hit[0]) / 2 for hit in hits))
        for arm, item, side, is_red in (('rightArm', 'rightItem', 1, True), ('leftArm', 'leftItem', -1, False)):
            beats = [hit[0] for hit in hits if hit[1] == is_red]
            if not beats:
                continue
            prep = smooth(pulse(tick, beats[0] - 4, 2, 2)) if beats[0] >= 6 else 0
            follow = max(smooth(pulse(tick, beat + 2, 2, 2)) for beat in beats) * clear
            result[arm]['yaw'] += side * (14 * prep + 22 * follow) * fade
            result[arm]['roll'] += side * (24 * prep - 18 * follow) * fade
            result[item]['yaw'] += side * (18 * prep + 28 * follow) * fade
            result[item]['pitch'] += side * 70 * prep * fade
            spin_start = max(beats[-1] + 2, hits[-1][0] + 1)
            spin_end = duration - 1
            if spin_end - spin_start >= 3:
                result[item]['pitch'] += side * 360 * smooth((tick - spin_start) / (spin_end - spin_start))
    if path not in ('figure_eight', 'red_dance', 'yellow_dance'):
        # Keep the shoulder joints attached to the turning, leaning torso.
        pitch, yaw, roll = (math.radians(result['torso'][axis]) for axis in ('pitch', 'yaw', 'roll'))
        for name in ('rightArm', 'leftArm'):
            x, y, z = (result[name][axis] for axis in ('x', 'y', 'z'))
            y, z = y * math.cos(pitch) - z * math.sin(pitch), y * math.sin(pitch) + z * math.cos(pitch)
            x, z = x * math.cos(yaw) + z * math.sin(yaw), -x * math.sin(yaw) + z * math.cos(yaw)
            x, y = x * math.cos(roll) - y * math.sin(roll), x * math.sin(roll) + y * math.cos(roll)
            result[name].update(x=round(x + result['torso']['x'], 4),
                                y=round(y + result['torso']['y'], 4), z=round(z + result['torso']['z'], 4))
    # Hands keep their shoulder pivots and weapons keep their palms as rotation centers.
    # Omitted channels pass vanilla look, walking, jumping and crouching through PlayerAnimator.
    for name in ('head', 'rightLeg', 'leftLeg'):
        result.pop(name, None)
    result['body'] = part()
    for name in ('rightItem', 'leftItem'):
        result[name]['x'] = result[name]['y'] = result[name]['z'] = 0
    return result


def visual_duration(path, duration, hits):
    if not hits or path in ('figure_eight', 'red_dance', 'yellow_dance'):
        return duration
    return max(duration, hits[-1][0] + 10)


def attack_pose(path, tick, duration, hits, variant=0):
    result = pose(path, min(tick, duration), duration, hits)
    last = hits[-1][0] if hits else 0
    # Alternate basic attacks retain the contact frame but arrive on different cutting planes.
    if path in ('red_basic', 'yellow_basic') and variant and tick <= last + 1:
        arm, item, side = ('rightArm', 'rightItem', 1) if path == 'red_basic' else ('leftArm', 'leftItem', -1)
        phase = max(-1, min(1, tick - last))
        cut = phase * (65 if variant < 3 else 35)
        result[arm]['yaw'] += side * cut * (1 if variant != 2 else -1)
        elevation = (-phase * 40 if variant == 1 else phase * 40 if variant == 2 else phase * 65)
        result[item] = aimed_item(result[arm], yaw=side * cut * (1 if variant != 2 else -1), elevation=elevation + (0 if side == 1 else -8))
    return result


def connected_pose(path, tick, duration, hits, variant=0):
    end = visual_duration(path, duration, hits)
    result = attack_pose(path, tick, duration, hits, variant)
    if not hits or path in ('figure_eight', 'red_dance', 'yellow_dance', 'red_sweep', 'low_spin', 'landing_sweep', 'rose_dance'):
        return result
    last = hits[-1][0]
    if tick <= last + 1:
        return result
    # Six or more ticks for a complete outside turn, followed by two ticks to settle behind.
    progress = smooth((tick - last - 1) / (end - last - 3))
    settle = smooth((tick - end + 2) / 2)
    previous = attack_pose(path, min(last + 1, duration), duration, hits, variant)
    chamber = smooth((tick - last - 1) / 2)
    result['torso'] = part(**{axis: previous['torso'][axis] * (1 - chamber) for axis in ('pitch', 'yaw', 'roll')})
    for arm, item, side, red in (('rightArm', 'rightItem', 1, True), ('leftArm', 'leftItem', -1, False)):
        if not any(h[1] == red for h in hits):
            continue
        start = previous[arm]
        support = part(-5 * side, 2, pitch=-62, yaw=-side * 50, roll=-side * 12)
        result[arm] = part(**{axis: start[axis] + (support[axis] - start[axis]) * chamber for axis in start})
        direction = -side if path in ('rising_thrust', 'retreating_thrust') else side
        aimed = aimed_item(result[arm], yaw=side * 12, elevation=0 if red else -8)
        start_item = previous[item]
        result[item] = part(**{axis: start_item[axis] + (aimed[axis] - start_item[axis]) * chamber for axis in aimed})
        result[item]['pitch'] += direction * 360 * progress
        # A diagonal follow-through is distinct from the vertical spin and fades before settling.
        arc = math.sin(math.pi * progress) ** 2
        result[item]['yaw'] += side * (34 if path != 'guard_counter' else 48) * arc
        result[item]['roll'] += -side * (22 if path != 'rising_thrust' else 40) * arc
        if settle:
            rest_arm = pose(path, duration, duration, hits)[arm]
            for axis in result[arm]:
                result[arm][axis] += (rest_arm[axis] - result[arm][axis]) * settle
            rest_item = pose(path, duration, duration, hits)[item]
            for axis in ('pitch', 'yaw', 'roll'):
                target = rest_item[axis] + (direction * 360 if axis == 'pitch' else 0)
                result[item][axis] += (target - result[item][axis]) * settle
    return result


parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--animations-only', action='store_true', help='Preserve server metadata and translations.')
args = parser.parse_args()
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
    end = visual_duration(path, duration, hits)
    moves = [{"tick": t, **connected_pose(path, t, duration, hits)} for t in range(end + 1)]
    dump(RES / f"assets/typemoonworld/player_animations/diarmuid_{path}.json", {
        "name": f"diarmuid_{path}", "version": 3,
        "description": "Third-person upper-body spear technique; vanilla head and locomotion; shared player/NPC tracks.",
        "emote": {"beginTick": 0, "endTick": end, "stopTick": end, "degrees": True, "moves": moves}})
    if path in ('red_basic', 'yellow_basic'):
        for variant, suffix in enumerate(('slash_left', 'slash_right', 'rising_cut'), 1):
            name = f"diarmuid_{path}_{suffix}"
            dump(RES / f"assets/typemoonworld/player_animations/{name}.json", {
                'name': name, 'version': 3,
                'description': 'Directional basic attack; original contact timing; outside recovery turn.',
                'emote': {'beginTick': 0, 'endTick': end, 'stopTick': end, 'degrees': True,
                          'moves': [{'tick': t, **connected_pose(path, t, duration, hits, variant)} for t in range(end + 1)]}})
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

if not args.animations_only:
    dump(RES / "data/typemoonworld/servant/actions/diarmuid_ua_duibhne.json", {"servant": "diarmuid_ua_duibhne", "actions": actions})

for locale in ("zh_cn", "en_us"):
    if args.animations_only:
        break
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

print(f"Generated {len(matches)} upper-body animations and shared server action metadata.")
