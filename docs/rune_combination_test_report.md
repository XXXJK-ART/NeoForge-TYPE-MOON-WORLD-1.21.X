# Rune Combination Test Report

## Result

- Matrix size: 60 combinations (20 direct release, 20 weapon enchantments, 20 armor reinforcement programs).
- JVM result: 60/60 valid, finite mana cost, and bytecode length equal to sequence length.
- Coverage: all 24 registered runes appeared at least once.
- Command: `./gradlew.bat test --tests '*Rune*'`
- A server GameTest attempt was blocked before rune execution by the existing mock-player login error `Payload neoforge:sync_attachments may not be sent to the client`.

## Rune Semantics

The dispatcher uses the following authoritative action keys. `T/E/M/X` mean trigger, effect, modifier, and terminal slots. Modifier keys are copied into projectile NBT before the entity is spawned; effect and terminal keys are resolved at impact when a projectile is involved.

| Rune | T | E | M | X |
|---|---|---|---|---|
| fehu | materialize | life_drain | split | harvest |
| uruz | charge | strength_amplify | pierce | wild |
| thurisaz | lightning | lightning_addon | volley | thunderstorm |
| ansuz | mind_blast | mind_addon | guidance | revelation |
| raidho | teleport | haste | ricochet | portal |
| kenaz | fireball | fire_addon | delay | embers |
| gebo | life_link | connection | chain | share |
| wunjo | inspire | light_addon | empower | aura |
| hagalaz | ice_spike | frost_addon | delay | ice_blast |
| nauthiz | shadow_chain | shadow_addon | gravity | curse_residue |
| isa | freeze_aura | stasis | freeze_behavior | ice_prison |
| jera | vine_bind | nature_addon | cycle | overgrowth |
| eihwaz | defense_barrier | protection_addon | rebound | rewind |
| perthro | fate_dice | random_addon | random_modifier | unknown |
| algiz | guardian_shield | sanctuary_addon | sanctuary | bulwark |
| sowilo | beam | light_addon | acceleration | cleanse |
| tiwaz | sure_strike | sharpness | precision | judgment |
| berkano | healing_wave | healing_addon | renewal | flourishing_field |
| ehwaz | teleport_15 | transfer | synchronize | harmony |
| mannaz | mirror_clone | self_addon | mirror | fusion |
| laguz | water_impact | water_addon | liquid | tide |
| ingwaz | nature_enchant | earth_addon | storage | germination |
| dagaz | dawn | cleanse_addon | cleanse | dawn_field |
| othala | ancestor_summon | soul_addon | territory | territory_arrival |

## Tested Combinations

`T/E/M/X` are the four source bands. A `-` band is empty.

### Direct release (20)

```text
00 T fehu+wunjo       E ansuz+hagalaz   M nauthiz  X perthro
01 T uruz+hagalaz     E raidho+nauthiz  M isa      X algiz
02 T thurisaz+nauthiz E kenaz+isa       M jera     X sowilo
03 T ansuz+isa        E gebo+jera       M eihwaz   X tiwaz
04 T raidho+jera      E wunjo+eihwaz    M perthro  X berkano
05 T kenaz+eihwaz     E hagalaz+perthro M algiz    X ehwaz
06 T gebo+perthro     E nauthiz+algiz   M sowilo   X mannaz
07 T wunjo+algiz      E isa+sowilo      M tiwaz    X laguz
08 T hagalaz+sowilo   E jera+tiwaz      M berkano  X ingwaz
09 T nauthiz+tiwaz    E eihwaz+berkano  M ehwaz    X dagaz
10 T isa+berkano      E perthro+ehwaz   M mannaz   X othala
11 T jera+ehwaz       E algiz+mannaz    M laguz    X fehu
12 T eihwaz+mannaz    E sowilo+laguz    M ingwaz   X uruz
13 T perthro+laguz    E tiwaz+ingwaz    M dagaz    X thurisaz
14 T algiz+ingwaz     E berkano+dagaz   M othala   X ansuz
15 T sowilo+dagaz     E ehwaz+othala    M fehu     X raidho
16 T tiwaz+othala     E mannaz+fehu     M uruz     X kenaz
17 T berkano+fehu     E laguz+uruz      M thurisaz X gebo
18 T ehwaz+uruz       E ingwaz+thurisaz M ansuz    X wunjo
19 T mannaz+thurisaz  E dagaz+ansuz     M raidho   X hagalaz
```

### Weapon enchantment (20)

```text
00 E thurisaz+wunjo M jera+ehwaz
01 E ansuz+hagalaz  M eihwaz+mannaz
02 E raidho+nauthiz M perthro+laguz
03 E kenaz+isa      M algiz+ingwaz
04 E gebo+jera      M sowilo+dagaz
05 E wunjo+eihwaz   M tiwaz+othala
06 E hagalaz+perthro M berkano+fehu
07 E nauthiz+algiz  M ehwaz+uruz
08 E isa+sowilo     M mannaz+thurisaz
09 E jera+tiwaz     M laguz+ansuz
10 E eihwaz+berkano M ingwaz+raidho
11 E perthro+ehwaz  M dagaz+kenaz
12 E algiz+mannaz   M othala+gebo
13 E sowilo+laguz   M fehu+wunjo
14 E tiwaz+ingwaz   M uruz+hagalaz
15 E berkano+dagaz  M thurisaz+nauthiz
16 E ehwaz+othala   M ansuz+isa
17 E mannaz+fehu    M raidho+jera
18 E laguz+uruz     M kenaz+eihwaz
19 E ingwaz+thurisaz M gebo+perthro
```

### Armor reinforcement (20)

```text
00 M uruz+gebo      X sowilo
01 M thurisaz+wunjo X tiwaz
02 M ansuz+hagalaz  X berkano
03 M raidho+nauthiz X ehwaz
04 M kenaz+isa      X mannaz
05 M gebo+jera      X laguz
06 M wunjo+eihwaz   X ingwaz
07 M hagalaz+perthro X dagaz
08 M nauthiz+algiz  X othala
09 M isa+sowilo     X fehu
10 M jera+tiwaz     X uruz
11 M eihwaz+berkano X thurisaz
12 M perthro+ehwaz  X ansuz
13 M algiz+mannaz   X raidho
14 M sowilo+laguz   X kenaz
15 M tiwaz+ingwaz   X gebo
16 M berkano+dagaz  X wunjo
17 M ehwaz+othala   X hagalaz
18 M mannaz+fehu    X nauthiz
19 M laguz+uruz     X isa
```

## Notable Effects

- `thurisaz + kenaz + isa`: lightning trigger, fire projectile, and freeze slow; `isa` also slows entities in area effects.
- `hagalaz + sowilo + tiwaz`: storm/ice area pressure plus a piercing beam and critical modifier.
- `nauthiz + algiz + mannaz`: shadow/drain pressure, resistance, confusion, and caster healing from drain.
- `berkano + dagaz`: healing/nature plus glowing transformation; useful as a sustain utility program.
- `uruz + thurisaz` on armor: damage boost reinforcement plus protection from the `detonate` terminal path.
- `wunjo + eihwaz` on armor: regeneration/speed-related recovery and resistance-oriented reinforcement.
