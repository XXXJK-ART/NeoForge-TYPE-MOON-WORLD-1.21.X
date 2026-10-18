# VFX 2.0 world materials

Existing effect JSON remains valid. New fields are optional and are resolved on the client.

```json
{
  "screen_effects": [
    {"type":"radial_blur","intensity":0.2,"duration":0.4}
  ],
  "emitters": [{
    "rate": 1200,
    "particle_lifetime": 0.5,
    "priority": "critical",
    "renderer": "shader_quad",
    "material": {"shader":"vfx_energy","bloom":1.0},
    "motion": {"type":"turbulence_rise","amount":0.03,"frequency":3.0},
    "component": {"type":"sphere","radius":1.0,"sample_count":96}
  }]
}
```

The built-in shader whitelist is `vfx_energy`, `vfx_polar`, `vfx_sdf`, and `vfx_smoke`.
Unknown shader ids fall back to the vanilla material. `priority` controls adaptive
budgeting; `critical` effects are retained longest under pressure. `renderer` values
`shader_quad` and `volume` currently use the safe billboard path until a dedicated
geometry backend is available.

Client controls are stored in the NeoForge client config: `vfxQuality`,
`vfxDistortion`, and `vfxMaxParticles`. Legacy `vfxBloom` and `vfxScreenEffects`
keys remain readable but are ignored; fullscreen post processing is removed.
Runtime inspection is
available through `/vfx client quality`, `/vfx debug stats`, and `/vfx debug profile`.

## Legacy effect migration

Effect files that omit the VFX 2.0 fields are migrated when the client parses them.
Shape components use the polar/SDF material where appropriate; beam, lightning,
parametric and smoke-like roles use the energy/smoke material. The renderer falls
back to a shader billboard or decal, and CPU turbulence/orbit motion is added for
eligible legacy shapes. Explicit `material`, `renderer`, and `motion` fields always
take precedence. All `screen_effects` and `environment` entries are accepted only
as legacy metadata. They never change screen color, fog, weather, saturation,
vignette or blur. Scene-wide bloom and duel flash packets are also disabled at
their entry points, regardless of existing config values. No fullscreen shader
chains are shipped. Legacy vanilla `flash` particles resolve to small `end_rod`
particles. World-space particles, beams, decals and emissive materials remain.

Mod-created lightning bolts carry the synced no-gravity flag (which does not
affect stationary lightning). A client-only mixin skips only those bolts' calls
to `Level.setSkyFlashTime`, retaining their geometry, damage, fire and lifetime.
Vanilla has no per-bolt flash event; natural lightning remains unchanged.

This compatibility pass keeps the existing JSON schema stable. It does not claim
SSBO/GPU particles, world-position reconstruction, or a dedicated distortion
render target; those remain separate renderer upgrades.
