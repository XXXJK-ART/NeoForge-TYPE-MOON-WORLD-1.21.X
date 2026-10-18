package net.xxxjk.TYPE_MOON_WORLD.network.state;

import net.minecraft.nbt.CompoundTag;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

/** Compact persistence/sync projection for the six player state domains. */
public final class PlayerStateDomainCodec {
   private static final int VERSION = 1;

   private PlayerStateDomainCodec() { }

   public static CompoundTag encode(TypeMoonWorldModVariables.PlayerVariables vars,
                                    TypeMoonWorldModVariables.PlayerVariables.StateDomain domain) {
      CompoundTag tag = new CompoundTag();
      tag.putInt("version", VERSION);
      switch (domain) {
         case MAGIC -> {
            var state = vars.magicState();
            tag.putDouble("mana", state.mana());
            tag.putDouble("max_mana", state.maxMana());
            tag.putDouble("mana_regen", state.manaRegen());
            tag.putDouble("restore_magic_moment", state.restoreMagicMoment());
            tag.putDouble("cooldown", state.cooldown());
            tag.putBoolean("circuit_open", state.circuitOpen());
            tag.putBoolean("analysis_active", state.analysisActive());
         }
         case SERVANT_CARD -> {
            var state = vars.servantCardState();
            tag.putBoolean("transformed", state.transformed());
            tag.putString("id", state.id() == null ? "" : state.id());
            tag.putDouble("mana", state.mana());
            tag.putDouble("max_mana", state.maxMana());
            tag.putDouble("mana_regen", state.manaRegen());
            tag.putInt("jump_charges", state.jumpCharges());
            tag.putInt("jump_recovery_ticks", state.jumpRecoveryTicks());
            tag.putLong("jump_recovery_end", state.jumpRecoveryEnd());
            tag.putString("skill_cooldowns", state.skillCooldowns());
            tag.putString("skill_cooldown_ends", state.skillCooldownEnds());
            tag.putInt("np_cooldown", state.npCooldown());
            tag.putLong("np_cooldown_end", state.npCooldownEnd());
            tag.putBoolean("flying", state.flying());
            tag.putInt("flight_mode", state.flightMode());
            tag.putLong("high_flight_until", state.highFlightUntil());
            tag.putLong("high_flight_cooldown_until", state.highFlightCooldownUntil());
            tag.putInt("oda_flight_ticks", state.odaFlightTicks());
            tag.putLong("oda_flight_cooldown_until", state.odaFlightCooldownUntil());
            tag.putLong("oda_flight_recharge_at", state.odaFlightRechargeAt());
            tag.putDouble("flight_forward", state.flightForward());
            tag.putDouble("flight_strafe", state.flightStrafe());
            tag.putDouble("flight_vertical", state.flightVertical());
            tag.putInt("flight_toggle_cooldown", state.flightToggleCooldown());
            tag.putInt("action_mode", state.actionMode());
            tag.putInt("transform_cooldown", state.transformCooldown());
            tag.putInt("release_cooldown", state.releaseCooldown());
            tag.putBoolean("death_release", state.deathRelease());
            tag.putInt("medea_dragonfang_stock", state.medeaDragonfangStock());
            tag.putInt("medea_mana_charm_stock", state.medeaManaCharmStock());
            tag.putInt("medea_heal_charm_stock", state.medeaHealCharmStock());
            tag.putInt("paracelsus_stone_stock", state.paracelsusStoneStock());
            tag.putInt("paracelsus_diamond_shield_stock", state.paracelsusDiamondShieldStock());
            tag.putInt("arash_arrow_stock", state.arashArrowStock());
            tag.putInt("royal_cannon_ammo", state.royalCannonAmmo());
            tag.putDouble("gilles_spellbook_mana", state.gillesSpellbookMana());
            tag.putDouble("gilles_spellbook_max_mana", state.gillesSpellbookMaxMana());
            tag.putInt("heracles_god_hand_lives", state.heraclesGodHandLives());
            tag.putString("enkidu_points", state.enkiduPoints());
            tag.putBoolean("medusa_mystic_eyes_active", state.medusaMysticEyesActive());
            tag.putBoolean("hassan_cloak_broken", state.hassanCloakBroken());
            tag.putInt("hassan_zabaniya_animation_until", state.hassanZabaniyaAnimationUntil());
         }
         case MASTER -> {
            var state = vars.masterState();
            tag.putBoolean("active", state.active());
            tag.putString("servant_uuid", state.servantUuid() == null ? "" : state.servantUuid());
            tag.putInt("command_spells", state.commandSpells());
         }
         case PROJECTION -> {
            var state = vars.projectionState();
            tag.putString("structure_id", state.selectedStructureId() == null ? "" : state.selectedStructureId());
         }
         case RUNE -> {
            var state = vars.runeState();
            tag.putBoolean("origin_unlocked", state.originUnlocked());
            tag.putInt("learned_count", state.learnedRunes().size());
         }
         case PASSIVE -> {
            var state = vars.passiveState();
            tag.putInt("body_strength", state.bodyStrength());
            tag.putInt("body_speed", state.bodySpeed());
         }
      }
      return tag;
   }
}
