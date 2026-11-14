# Jeanne Alter Reset Implementation Plan

> Inline execution of the user-provided, confirmed reset design.

**Goal:** Bring Jeanne Alter's servant data, player card, and NPC behavior into the current shared servant framework while preserving the confirmed local grudge, backwater, black flame, flag, and one-life death-lock mechanics.

**Architecture:** Keep the existing Jeanne action entry points and resource IDs where possible. Move shared Avenger class-skill math into reusable class-skill rules; keep Jeanne-specific state and attack resolution server authoritative, with player-card and NPC entry points delegating to the same rules.

**Tech Stack:** Java 21, NeoForge 21.1.x / Minecraft 1.21.1, JSON servant resources, JUnit and GameTest.

**Spec:** User attachment `已粘贴的文本.txt`, section `贞德〔Alter〕新版重置计划`.

## Global Constraints

- Preserve the configured Minecraft 1.21.1 and NeoForge 21.1.x versions.
- Do not overwrite unrelated existing workspace changes.
- Use the same core Jeanne values, costs, cooldowns, and damage rules for player card and NPC.
- Preserve the existing Jeanne resource IDs when compatibility requires them.

### Tasks

1. Add focused resource and rules regression tests for confirmed identity, skills, scaling, and attack timing.
2. Update Jeanne definition, skills, card slots, NPC configuration, and Noble Phantasm data to the confirmed plan.
3. Implement reusable Avenger, Oblivion Correction, and Self-Replenishment grade rules and migrate Jeanne to them.
4. Consolidate Jeanne damage, grudge, backwater, inferno proc, flag, and per-life death-lock behavior across player/NPC entry points.
5. Align cursed-lance damage with the 30-tick impact, and ensure Noble Phantasm stages follow the planned shared timing and cancellation behavior.
6. Update servant design docs and translations/resources as needed.
7. Run focused unit tests, GameTests where available, and the Gradle build; report any runtime checks not possible in this environment.
