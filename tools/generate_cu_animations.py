"""Author PlayerAnimator clips with the same palm/shaft convention as Diarmuid."""
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / "src/main/resources/assets/typemoonworld/player_animations"


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


def matrix(pitch=0, yaw=0, roll=0):
    p, y, r = map(math.radians, (pitch, yaw, roll))
    cp, sp, cy, sy, cr, sr = math.cos(p), math.sin(p), math.cos(y), math.sin(y), math.cos(r), math.sin(r)
    return [[cr*cy, cr*sy*sp-sr*cp, cr*sy*cp+sr*sp],
            [sr*cy, sr*sy*sp+cr*cp, sr*sy*cp-cr*sp], [-sy, cy*sp, cy*cp]]


def mul(a, b):
    return [[sum(a[r][k]*b[k][c] for k in range(3)) for c in range(3)] for r in range(3)]


def aim(arm, yaw=0, elevation=0):
    # Invert the real hand and item-display transforms so the point follows its authored aim.
    grip = mul(matrix(**{k: arm[k] for k in ('pitch', 'yaw', 'roll')}), mul(matrix(pitch=-90), matrix(yaw=180)))
    # Gae Bulg's blade points down model -Z; Diarmuid's models use +Z.
    desired = mul(matrix(yaw=yaw), matrix(pitch=-elevation))
    item = mul(mul(list(map(list, zip(*grip))), desired), matrix(pitch=90))
    return part(pitch=math.degrees(math.atan2(item[2][1], item[2][2])),
                yaw=math.degrees(math.asin(max(-1, min(1, -item[2][0])))),
                roll=math.degrees(math.atan2(item[1][0], item[0][0])))


def pose(name, t, end):
    fade = smooth(t / 2) * smooth((end - t) / 3)
    right = part(-5, 2, pitch=-62*fade, yaw=-38*fade, roll=-8*fade)
    left = part(5, 2, pitch=-35*fade, yaw=25*fade, roll=12*fade)
    torso = part(pitch=5*fade, yaw=-12*fade)
    yaw, elevation, spin = 12*fade, 0, 0
    if name == 'sweep':
        # Two broad passes connect on server frames 6 and 12, with a short outside recovery.
        yaw = curve(t, [(0, 0), (3, -125), (6, 0), (8, 125), (10, 110), (12, 0), (14, -120), (end, 0)])
        torso['yaw'] = curve(t, [(0, 0), (3, -48), (6, 12), (8, 58), (10, 42), (12, -8), (14, -42), (end, 0)])
        right.update(pitch=-82*fade, yaw=yaw, roll=-9*fade)
        elevation = 6*math.sin(t*math.pi/6)*fade
        spin = 360*smooth((t-14)/4)
    elif name in ('flurry', 'thrust'):
        hits = [4, 8, 12] if name == 'flurry' else [3]
        drive = max(max(0, 1-abs(t-hit)/2) for hit in hits)
        drive = smooth(drive)
        right.update(pitch=(-62-36*drive)*fade, yaw=-55*(1-drive)*fade, roll=-8*(1-drive)*fade)
        torso.update(pitch=(5+13*drive)*fade, yaw=(-12-24*drive)*fade)
        yaw = 8*(1-drive)*fade
        if name == 'flurry':
            spin = 360*smooth((t-13)/5)
        else:
            torso['pitch'] += 16*fade
    elif name == 'raise':
        right.update(pitch=-155*fade, yaw=-25*fade, roll=25*fade)
        torso.update(pitch=-8*fade, yaw=-32*fade)
        left.update(pitch=-62*fade, yaw=30*fade, roll=-15*fade)
        yaw, elevation = 145*fade, 18*fade
    elif name == 'throw':
        throw = smooth((t-3)/2)
        right.update(pitch=curve(t, [(0, -155), (3, -155), (5, -85), (8, -35), (end, 0)]),
                     yaw=curve(t, [(0, -25), (3, -25), (5, 5), (8, 30), (end, 0)]), roll=20*(1-throw))
        torso.update(pitch=curve(t, [(0, -8), (3, -8), (6, 22), (10, 12), (end, 0)]),
                     yaw=curve(t, [(0, -32), (3, -32), (6, 36), (10, 18), (end, 0)]))
        yaw, elevation = 145*(1-throw), 18*(1-throw)
        left.update(pitch=-60*(1-throw)*fade, yaw=25*fade, roll=-12*fade)
    else:
        left.update(pitch=-98*fade, yaw=20*fade, roll=-22*fade)
        right.update(pitch=-38*fade, yaw=-18*fade, roll=8*fade)
        torso['yaw'] = 10*fade
        yaw, elevation = 25*fade, -28*fade
    item = aim(right, yaw, elevation)
    item['pitch'] += spin
    tracks = dict(body=part(), torso=torso, rightArm=right, leftArm=left, rightItem=item, leftItem=part())
    # Shoulder positions follow the torso while view direction and walking remain under vanilla control.
    rotation = matrix(**{k: torso[k] for k in ('pitch', 'yaw', 'roll')})
    for arm in (right, left):
        v = [arm[k] for k in ('x', 'y', 'z')]
        for i, key in enumerate(('x', 'y', 'z')):
            arm[key] = sum(rotation[i][j]*v[j] for j in range(3)) + torso[key]
    for track in tracks.values():
        for key, value in track.items():
            track[key] = round(value, 4)
    return tracks


for name, end in [('sweep', 20), ('flurry', 20), ('thrust', 12), ('raise', 20), ('throw', 16), ('rune', 22)]:
    clip = {'name': 'cu_'+name, 'version': 3,
            'description': 'PlayerAnimator single-spear action; shared player/NPC tracks with palm-correct item aim.',
            'emote': {'beginTick': 0, 'endTick': end, 'stopTick': end, 'degrees': True,
                      'moves': [{'tick': tick, **pose(name, tick, end)} for tick in range(end+1)]}}
    (DEST / ('cu_'+name+'.json')).write_text(json.dumps(clip, indent=2)+'\n', encoding='utf-8')
print('Generated six Cu Chulainn PlayerAnimator clips.')
