# Type Moon World Addon API v1

The addon API is published as `typemoonworld-api`. The main mod must also be installed at
runtime. Addons should depend on the API with `compileOnly` and never import packages under
`net.xxxjk.TYPE_MOON_WORLD`.

## Registration

```java
public final class MyAddon {
    public MyAddon() {
        AddonRegistrar addon = TypeMoonWorldApi.addon("myaddon");
        ResourceLocation action = ResourceLocation.fromNamespaceAndPath("myaddon", "meteor");
        addon.cards().registerAction(action, context -> {
            // Server-authoritative effect.
            return ExecutionResult.SUCCESS;
        });
        addon.cards().bindSlot(
            ResourceLocation.fromNamespaceAndPath("myaddon", "nero"), 0, action
        );
        ItemStack summonItem = addon.servants().createSummonItem(
            ResourceLocation.fromNamespaceAndPath("myaddon", "nero")
        );
    }
}
```

The corresponding card data goes in `data/myaddon/servant/cards/nero.json`:

```json
{
  "servant_id": "myaddon:nero",
  "actions": [
    {
      "slot": 0,
      "action": "myaddon:meteor",
      "translation_key": "skill.myaddon.nero.meteor"
    }
  ]
}
```

Action IDs, servant IDs and magic IDs must use the addon's namespace. Registration is rejected
after common setup has frozen the registries. Legacy bare IDs remain readable for built-in
content and are not valid for new addon registrations.

## Resources

Servant definitions use `data/<namespace>/servant/definitions`. Card slot bindings use
`data/<namespace>/servant/cards`. Models, textures, animations, sounds and translations remain
ordinary addon resources and are not copied into the main mod.

## Magic and events

Magic definitions use `data/<namespace>/magic/definitions/<id>.json`. Register a definition
and executor under the same addon namespace. The same executor is used for player and NPC
casts; the context identifies the caster, target, preset and proficiency.

Attribute-gated magic declares stable attribute IDs in JSON. The server checks these requirements
for player, crest, engraved-gem and NPC casts before invoking the executor:

```json
{
  "name_key": "magic.myaddon.imaginary_spell",
  "school": "myaddon:imaginary",
  "required_attributes": ["typemoonworld:imaginary_number"]
}
```

Addons can also inspect the same read-only state in callbacks without depending on internal player variables:

```java
boolean imaginary = TypeMoonWorldApi.magicAttributes(context.caster())
   .has(MagicAttributes.IMAGINARY_NUMBER);
```

Built-in IDs are exposed by `MagicAttributes`: `EARTH`, `WATER`, `FIRE`, `WIND`, `ETHER`,
`NONE`, `IMAGINARY_NUMBER`, and `SWORD`. Datagen can emit the requirement through
`MagicDefinitionBuilder.requireAttribute(...)`.

`MagicCastEvent.Pre` is cancellable and `MagicCastEvent.Post` reports the result.
`ServantSummonEvent`, `ServantContractEvent`, `ServantTransformEvent`, and
`ServantActionEvent` provide equivalent lifecycle hooks. These events are posted on the
NeoForge global event bus and are server-authoritative.

The universal `servant_summon` and `servant_armor_generic_*` items carry the servant ID in
`CUSTOM_DATA`. Use `createSummonItem` and `createCard` rather than registering duplicate item
types for ordinary addon content.

## Other façades

The API also exposes state and utility façades without exposing `PlayerVariables` or internal
services:

```java
BodyTrainingAccess training = TypeMoonWorldApi.bodyTraining(player);
training.award(5);
training.allocate("strength");

ServantFormAccess servant = TypeMoonWorldApi.servantForm(serverPlayer);
servant.transform(ResourceLocation.fromNamespaceAndPath("myaddon", "nero"));
servant.triggerAction(0);

MasterAccess master = TypeMoonWorldApi.master(serverPlayer);
master.activate();
master.bind(otherPlayer);

TypeMoonWorldApi.projectionEffects().blockPlace(level, position);
```

Gem affinity is registered through `addon.gems().registerMagic(...)`. Custom affinities are
used by the existing engraving chance calculation and engraved gems can invoke the registered
magic executor, so an addon does not need to patch the gem/chisel code.

Master cards are also extensible:

```java
addon.masters().register(
    new MasterProfileData(id, "master.myaddon.nero", "default", 500, 5, 10),
    context -> {
        context.magicKnowledge().learn(magicId);
        context.magicKnowledge().setProficiency(magicId, 40);
        context.player().getInventory().add(new ItemStack(Items.DIAMOND));
    }
);
ItemStack card = addon.masters().createCard(id);
```

The generated master card goes through the same server-side snapshot, activation and restore
flow as built-in master cards.

`MasterProfileEvent` and `BodyTrainingEvent` expose cancellable activation, XP award and stat
allocation hooks on the NeoForge event bus.

## Extended execution APIs

`addon.ai()` registers weighted tactical profiles. A profile can choose an action by distance and
health ratio; the runtime AI uses the same follow/retreat values for data and code profiles.
Existing `AiTacticProfile` registrations remain compatible. Addons that want cinematic movement,
route interception and recovery policy can call `registerAdvanced` with an
`AdvancedAiTacticProfile`; its base profile still owns weighted action selection. The advanced
fields describe behavior rather than granting abilities, so teleport, flight and destructive
actions must still be implemented and registered by the addon.

Data actions under `data/<namespace>/servant/actions` enter the shared tick-driven executor only
when they declare `maneuver`; legacy actions without it remain owned by their combat helper. The
optional `approach_range`, `damage_scale`, and `interrupt_resistance` fields control bounded path
approach, shared hit damage, and windup interruption resistance. Semantic tags such as `counter`,
`anti_air`, `finisher`, and `terrain_break` are runtime conditions rather than cosmetic weights.
`addon.projections()` registers item and structure executors, while addon-owned entities remain
ordinary NeoForge registrations. `addon.projectiles()` is the NP special-projectile hook.

`addon.commandSpells()` registers server-only command spell callbacks. Clients invoke these with
`CustomCommandSpellMessage`; the framework checks master state, command-spell count, cancellation,
and bounded IDs before deducting the returned cost. `addon.damageTypes()` creates namespaced
`ResourceKey<DamageType>`/`DamageSource` values backed by datapack damage types.

`addon.effects()` provides bounded particle and sound dispatch helpers. Register the actual
`ParticleOptions`/`SoundEvent` through the normal NeoForge registry first; the API does not mutate
vanilla registries after setup.

For magic UI, register `MagicOption` values with `addon.client().registerControl(...)`. Boolean,
enum, integer-range, target and item controls are rendered by the generic screen; submitted NBT is
revalidated by `MagicPresetRegistry` on the server. Complex screens can still use
`registerMagicOptions(...)`.

The server sends `DefinitionSnapshotMessage` on login and datapack sync. Client extensions should
read `DefinitionSnapshotStore.current()` and never trust local-only definitions.

## Data and tests

`docs/schema` contains JSON Schema files for all static definitions. The public
`net.xxxjk.typemoonworld.api.datagen` builders write the same directory layout with
`DataProvider.saveStable`. `test-addon/` is a standalone fixture covering generated servants,
cards, magic, AI, projection, command spells, projectiles and GUI controls. Built-in GameTests
verify that the Codec registries are populated and magic definitions are available after reload.

Skill JSON may optionally declare `ai.facts`. Facts influence bounded action utility, target
matchup scoring, positioning, and defensive intent; they never implement the described effect.
Old JSON and the eight-argument `ServantSkillDefinition` constructor remain compatible. Addons
should declare facts only for effects their executor or combat helper actually enforces, including
the matching `requires` and `bypassed_by` conditions for conditional defenses.
