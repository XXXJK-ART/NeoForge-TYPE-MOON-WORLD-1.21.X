package net.xxxjk.TYPE_MOON_WORLD.magic;

import net.minecraft.world.entity.Entity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.talent.TalentService;

/** Applies diminishing returns to every proficiency increase, including batch operations. */
public final class MagicProficiencyService {
   private MagicProficiencyService() {}

   public static double get(TypeMoonWorldModVariables.PlayerVariables vars, String id) {
      if (vars == null || id == null) return 0.0;
      if (TalentService.isTalent(id)) return TalentService.proficiency(vars, id);
      return switch (MagicLearningStrategy.normalizeDisplayId(id)) {
         case "magic_analysis" -> vars.proficiency_magic_analysis;
         case "structural_analysis" -> vars.proficiency_structural_analysis;
         case "projection" -> vars.proficiency_projection;
         case "reinforcement" -> vars.proficiency_reinforcement;
         case "jewel_magic_shoot" -> vars.proficiency_jewel_magic_shoot;
         case "jewel_magic_release", "jewel_machine_gun" -> vars.proficiency_jewel_magic_release;
         case "unlimited_blade_works" -> vars.proficiency_unlimited_blade_works;
         case "sword_barrel_full_open" -> vars.proficiency_sword_barrel_full_open;
         case "gravity_magic" -> vars.proficiency_gravity_magic;
         case "gander" -> vars.proficiency_gander;
         case "healing_magic" -> vars.proficiency_healing_magic;
         case "magic_bullet" -> vars.proficiency_magic_bullet;
         case "suggestion_magic" -> vars.proficiency_suggestion_magic;
         case "binding_magic" -> vars.proficiency_binding_magic;
         case "fire_magic" -> vars.proficiency_fire_magic;
         case "water_magic" -> vars.proficiency_water_magic;
         case "wind_magic" -> vars.proficiency_wind_magic;
         case "earth_magic" -> vars.proficiency_earth_magic;
         case "time_alter" -> vars.proficiency_time_alter;
         case "spiritual_healing" -> vars.proficiency_spiritual_healing;
         case "baptism_rite" -> vars.proficiency_baptism_rite;
         default -> vars.magic_proficiencies.getOrDefault(id, 0.0);
      };
   }

   public static double add(TypeMoonWorldModVariables.PlayerVariables vars, String id, double baseGain) {
      if (vars == null || id == null || baseGain <= 0.0) return get(vars, id);
      if (TalentService.isTalent(id)) return get(vars, id);
      double current = get(vars, id);
      double value = calculateValue(id, current, baseGain);
      set(vars, id, value);
      return value;
   }

   public static double calculateValue(String id, double current, double baseGain) {
      current = Math.max(0.0, Math.min(100.0, current));
      if (id == null || baseGain <= 0.0) return Math.round(current * 100.0) / 100.0;
      double stage = "magic_analysis".equals(id) ? (current < 50.0 ? 1.0 : current < 75.0 ? 0.25 : 0.10) : 1.0;
      double effective = baseGain * Math.max(0.05, 1.0 - current / 100.0) * stage;
      return Math.min(100.0, Math.round((current + effective) * 100.0) / 100.0);
   }

   public static void set(TypeMoonWorldModVariables.PlayerVariables vars, String id, double value) {
      if (TalentService.isTalent(id)) return;
      value = Math.max(0.0, Math.min(100.0, Math.round(value * 100.0) / 100.0));
      switch (MagicLearningStrategy.normalizeDisplayId(id)) {
         case "magic_analysis" -> vars.proficiency_magic_analysis = value;
         case "structural_analysis" -> vars.proficiency_structural_analysis = value;
         case "projection" -> vars.proficiency_projection = value;
         case "reinforcement" -> vars.proficiency_reinforcement = value;
         case "jewel_magic_shoot" -> vars.proficiency_jewel_magic_shoot = value;
         case "jewel_magic_release", "jewel_machine_gun" -> vars.proficiency_jewel_magic_release = value;
         case "unlimited_blade_works" -> vars.proficiency_unlimited_blade_works = value;
         case "sword_barrel_full_open" -> vars.proficiency_sword_barrel_full_open = value;
         case "gravity_magic" -> vars.proficiency_gravity_magic = value;
         case "gander" -> vars.proficiency_gander = value;
         case "healing_magic" -> vars.proficiency_healing_magic = value;
         case "magic_bullet" -> vars.proficiency_magic_bullet = value;
         case "suggestion_magic" -> vars.proficiency_suggestion_magic = value;
         case "binding_magic" -> vars.proficiency_binding_magic = value;
         case "fire_magic" -> vars.proficiency_fire_magic = value;
         case "water_magic" -> vars.proficiency_water_magic = value;
         case "wind_magic" -> vars.proficiency_wind_magic = value;
         case "earth_magic" -> vars.proficiency_earth_magic = value;
         case "time_alter" -> vars.proficiency_time_alter = value;
         case "spiritual_healing" -> vars.proficiency_spiritual_healing = value;
         case "baptism_rite" -> vars.proficiency_baptism_rite = value;
         default -> vars.magic_proficiencies.put(id, value);
      }
   }

   public static double add(Entity entity, String id, double baseGain) {
      if (entity == null) return 0.0;
      var vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      double result = add(vars, id, baseGain);
      vars.syncProficiency(entity);
      return result;
   }
}
