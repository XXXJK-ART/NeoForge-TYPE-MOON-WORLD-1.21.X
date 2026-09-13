# VFX 2.0 material and screen effects

Existing effect JSON remains valid. New fields are optional and are resolved on the client.

```json
{
  "screen_effects": [
    {"type":"white_flash","intensity":0.4,"duration":0.25},
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

Client controls are stored in the NeoForge client config: `vfxQuality`, `vfxBloom`,
`vfxScreenEffects`, `vfxDistortion`, and `vfxMaxParticles`. Runtime inspection is
available through `/vfx client quality`, `/vfx debug stats`, and `/vfx debug profile`.
