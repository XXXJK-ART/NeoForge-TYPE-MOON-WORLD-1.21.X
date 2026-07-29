# Type Moon World API test addon

This fixture is intentionally outside the production source set. Copy it into a NeoForge 1.21.x addon project and set the API Maven dependency to `net.xxxjk:typemoonworld-api`.

It exercises servant/card/skill/NP/AI/magic, projection items and structures, command spells, NP projectiles, particles, sounds, custom damage types, gems, master profiles and generic GUI registration from one addon namespace.

The fixture action profile also demonstrates an opt-in shared maneuver with bounded approach,
damage scaling, and interruption resistance. Actions without `maneuver` keep their addon executor.
