package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.DeadApostleEntity;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicDisplayMetadata;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.typemoonworld.api.MagicComplexity;

public final class MagicProficiencyContestHelper {
   public static final double RESIST_GAP = 10.0;
   public static final double COUNTER_GAP = 30.0;

   private MagicProficiencyContestHelper() {
   }

   public static Result contest(
      LivingEntity caster, LivingEntity target, String magicId, double casterProficiency, MagicComplexity complexity
   ) {
      if (caster == null || target == null || caster == target || magicId == null || magicId.isBlank()) {
         return Result.NORMAL;
      }
      if (MagicDisplayMetadata.isMartialMagic(normalizeMagicId(magicId))) {
         return Result.NORMAL;
      }
      Double targetProficiency = getComparableMagicProficiency(target, magicId);
      if (targetProficiency == null) {
         return Result.NORMAL;
      }
      return contestByValues(casterProficiency, targetProficiency);
   }

   public static Result contestByValues(double casterProficiency, double targetProficiency) {
      double gap = clampProficiency(targetProficiency) - clampProficiency(casterProficiency);
      if (gap > COUNTER_GAP) {
         return Result.COUNTERED;
      }
      if (gap > RESIST_GAP) {
         return Result.RESISTED;
      }
      return Result.NORMAL;
   }

   public static boolean blocksTargetEffect(
      LivingEntity caster, LivingEntity target, String magicId, double casterProficiency, MagicComplexity complexity
   ) {
      return contest(caster, target, magicId, casterProficiency, complexity) != Result.NORMAL;
   }

   public static Double getComparableMagicProficiency(LivingEntity entity, String magicId) {
      if (entity == null || magicId == null || magicId.isBlank()) {
         return null;
      }
      if (ServantIdentityHelper.definitionOf(entity) != null || entity instanceof DeadApostleEntity) {
         return null;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      String normalized = normalizeMagicId(magicId);
      if (vars.magic_proficiencies.containsKey(normalized)) {
         return clampProficiency(vars.magic_proficiencies.getOrDefault(normalized, 0.0));
      }
      if (vars.magic_proficiencies.containsKey(magicId)) {
         return clampProficiency(vars.magic_proficiencies.getOrDefault(magicId, 0.0));
      }
      boolean eligible = entity instanceof Player || entity instanceof MysticMagicianEntity || vars.is_magus || vars.player_max_mana >= 100.0;
      if (!eligible && fixedProficiency(vars, normalized) <= 0.0) {
         return null;
      }
      return clampProficiency(fixedProficiency(vars, normalized));
   }

   public static double resolveCasterProficiency(LivingEntity caster, String magicId, double fallback) {
      Double value = getComparableMagicProficiency(caster, magicId);
      return value == null ? clampProficiency(fallback) : value;
   }

   public static double fixedMagicProficiency(TypeMoonWorldModVariables.PlayerVariables vars, String magicId) {
      return clampProficiency(fixedProficiency(vars, normalizeMagicId(magicId)));
   }

   public static String normalizeMagicId(String magicId) {
      if (magicId == null) {
         return "";
      }
      String trimmed = magicId.trim();
      int colon = trimmed.indexOf(':');
      return colon >= 0 ? trimmed.substring(colon + 1) : trimmed;
   }

   public static double clampProficiency(double value) {
      if (Double.isNaN(value) || Double.isInfinite(value)) {
         return 0.0;
      }
      return Math.max(0.0, Math.min(100.0, value));
   }

   private static double fixedProficiency(TypeMoonWorldModVariables.PlayerVariables vars, String magicId) {
      if (vars == null || magicId == null) {
         return 0.0;
      }
      return switch (magicId) {
         case "structural_analysis" -> vars.proficiency_structural_analysis;
         case "projection" -> vars.proficiency_projection;
         case "reinforcement", "reinforcement_self", "reinforcement_other", "reinforcement_item", "topaz_reinforcement" -> vars.proficiency_reinforcement;
         case "jewel_magic_shoot", "jewel_random_shoot", "jewel_machine_gun",
              "ruby_throw", "sapphire_throw", "topaz_throw", "cyan_throw" -> vars.proficiency_jewel_magic_shoot;
         case "jewel_magic_release", "emerald_use" -> vars.proficiency_jewel_magic_release;
         case "unlimited_blade_works" -> vars.proficiency_unlimited_blade_works;
         case "sword_barrel_full_open" -> vars.proficiency_sword_barrel_full_open;
         case "gravity_magic" -> vars.proficiency_gravity_magic;
         case "gander", "gandr_machine_gun" -> vars.proficiency_gander;
         case "healing_magic" -> vars.proficiency_healing_magic;
         case "magic_bullet" -> vars.proficiency_magic_bullet;
         case "suggestion_magic" -> vars.proficiency_suggestion_magic;
         case "binding_magic" -> vars.proficiency_binding_magic;
         case "fire_magic", "ruby_flame_sword" -> vars.proficiency_fire_magic;
         case "water_magic", "sapphire_winter_frost", "emerald_winter_river" -> vars.proficiency_water_magic;
         case "wind_magic", "cyan_wind" -> vars.proficiency_wind_magic;
         case "earth_magic" -> vars.proficiency_earth_magic;
         case "time_alter" -> vars.proficiency_time_alter;
         case "spiritual_healing" -> vars.proficiency_spiritual_healing;
         case "baptism_rite", "black_key_fire_engraving", "stigma" -> vars.proficiency_baptism_rite;
         default -> 0.0;
      };
   }

   public enum Result {
      NORMAL,
      RESISTED,
      COUNTERED
   }
}
