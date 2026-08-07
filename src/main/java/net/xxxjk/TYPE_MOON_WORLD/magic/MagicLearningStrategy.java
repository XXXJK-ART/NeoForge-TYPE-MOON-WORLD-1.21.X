package net.xxxjk.TYPE_MOON_WORLD.magic;

import java.util.Map;
import java.util.Set;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

/** Central rules for magic complexity and acquisition. Values are intentionally stable for saves. */
public final class MagicLearningStrategy {
   private MagicLearningStrategy() {}

   private record Rule(int complexity, boolean analysis, boolean material, boolean research, boolean copy, boolean sword, boolean divine) {}

   private static final Map<String, Rule> RULES = Map.ofEntries(
      Map.entry("magic_analysis", new Rule(55, false, false, true, false, false, false)),
      Map.entry("projection", new Rule(35, true, true, true, true, false, false)),
      Map.entry("structural_analysis", new Rule(30, true, true, true, true, false, false)),
      Map.entry("unlimited_blade_works", new Rule(100, false, true, true, true, true, false)),
      Map.entry("sword_barrel_full_open", new Rule(85, false, true, true, true, true, false)),
      Map.entry("broken_phantasm", new Rule(75, false, true, true, true, true, false)),
      Map.entry("time_alter", new Rule(70, false, true, true, true, false, false)),
      Map.entry("reinforcement", new Rule(25, true, true, true, true, false, false)),
      Map.entry("gravity_magic", new Rule(45, true, true, true, true, false, false)),
      Map.entry("gander", new Rule(40, true, true, true, true, false, false)),
      Map.entry("healing_magic", new Rule(35, true, true, true, true, false, false)),
      Map.entry("magic_bullet", new Rule(20, true, true, true, true, false, false)),
      Map.entry("suggestion_magic", new Rule(45, true, true, true, true, false, false)),
      Map.entry("binding_magic", new Rule(45, true, true, true, true, false, false)),
      Map.entry("fire_magic", new Rule(25, true, true, true, true, false, false)),
      Map.entry("water_magic", new Rule(25, true, true, true, true, false, false)),
      Map.entry("wind_magic", new Rule(25, true, true, true, true, false, false)),
      Map.entry("earth_magic", new Rule(25, true, true, true, true, false, false)),
      Map.entry("spiritual_healing", new Rule(50, true, true, true, true, false, false)),
      Map.entry("baptism_rite", new Rule(65, true, true, true, true, false, false)),
      Map.entry("black_key_fire_engraving", new Rule(60, true, true, true, true, false, false)),
      Map.entry("stigma", new Rule(60, true, true, true, true, false, false))
      ,Map.entry("reinforcement_self", new Rule(25, true, true, true, true, false, false))
      ,Map.entry("reinforcement_other", new Rule(25, true, true, true, true, false, false))
      ,Map.entry("reinforcement_item", new Rule(25, true, true, true, true, false, false))
      ,Map.entry("jewel_magic_shoot", new Rule(35, true, true, true, true, false, false))
      ,Map.entry("jewel_magic_release", new Rule(50, true, true, true, true, false, false))
      ,Map.entry("jewel_random_shoot", new Rule(40, true, true, true, true, false, false))
      ,Map.entry("jewel_machine_gun", new Rule(65, true, true, true, true, false, false))
      ,Map.entry("gandr_machine_gun", new Rule(70, true, false, false, false, false, false))
      ,Map.entry("ruby_throw", new Rule(30, true, false, false, false, false, false))
      ,Map.entry("sapphire_throw", new Rule(30, true, false, false, false, false, false))
      ,Map.entry("emerald_use", new Rule(30, true, false, false, false, false, false))
      ,Map.entry("topaz_throw", new Rule(30, true, false, false, false, false, false))
      ,Map.entry("cyan_throw", new Rule(30, true, false, false, false, false, false))
      ,Map.entry("ruby_flame_sword", new Rule(55, true, false, false, false, false, false))
      ,Map.entry("sapphire_winter_frost", new Rule(55, true, false, false, false, false, false))
      ,Map.entry("emerald_winter_river", new Rule(55, true, false, false, false, false, false))
      ,Map.entry("topaz_reinforcement", new Rule(55, true, false, false, false, false, false))
      ,Map.entry("cyan_wind", new Rule(55, true, false, false, false, false, false))
   );
   private static final Set<String> DEFAULT_ANALYZABLE = Set.of("projection", "structural_analysis", "reinforcement", "gravity_magic", "gander", "healing_magic", "magic_bullet", "suggestion_magic", "binding_magic", "fire_magic", "water_magic", "wind_magic", "earth_magic", "spiritual_healing", "baptism_rite", "black_key_fire_engraving", "stigma");

   private static Rule rule(String id) { return RULES.getOrDefault(id, new Rule(50, true, false, false, false, false, false)); }
   public static int complexity(String id) { return rule(id).complexity(); }
   public static int verses(String id) { int c = complexity(id); return c <= 20 ? 1 : c <= 40 ? 2 : c <= 60 ? 3 : c <= 80 ? 4 : 5; }
   public static double learningChance(String id, double analysisProficiency) {
      return Math.max(0.0, Math.min(1.0, (100.0 - complexity(id) + analysisProficiency) / 100.0));
   }
   public static int analysisVerseLimit(double analysisProficiency) {
      if (analysisProficiency <= 30.0) return 1;
      if (analysisProficiency <= 40.0) return 2;
      if (analysisProficiency <= 74.0) return 3;
      return Integer.MAX_VALUE;
   }
   public static double researchManaCost(String id, double proficiency) {
      if ("magic_analysis".equals(id)) return 300.0 + Math.max(0.0, Math.min(100.0, proficiency)) * 8.0;
      return 300.0 + complexity(id) * 8.0;
   }
   public static int researchTicks(String id, double proficiency) {
      if ("magic_analysis".equals(id)) return 200 + (int)Math.round((100.0 - Math.max(0.0, Math.min(100.0, proficiency))) * 4.0);
      return 200 + complexity(id) * 4;
   }
   public static boolean canAnalyze(String id) { return rule(id).analysis() && (RULES.containsKey(id) || DEFAULT_ANALYZABLE.contains(id)); }
   public static boolean canLearnFromMaterial(String id) { return rule(id).material(); }
   public static boolean canResearch(String id) { return rule(id).research(); }
   public static boolean canCopy(String id) { return rule(id).copy(); }
   public static boolean requiresSword(String id) { return rule(id).sword(); }
   public static boolean isDivine(String id) { return rule(id).divine() || complexity(id) >= 90; }
   public static boolean materialAllowed(TypeMoonWorldModVariables.PlayerVariables vars, String id) {
      return materialAllowed(vars.player_magic_attributes_sword, id);
   }
   public static boolean materialAllowed(boolean hasSwordAttribute, String id) {
      return canLearnFromMaterial(id) && (!requiresSword(id) || hasSwordAttribute);
   }
   public static String pageItemPath(String id) {
      return switch (id) {
         case "healing_magic" -> "magic_page_healing";
         case "magic_bullet" -> "magic_page_magic_bullet";
         case "suggestion_magic" -> "magic_page_suggestion";
         case "binding_magic" -> "magic_page_binding";
         case "fire_magic" -> "magic_page_fire";
         case "water_magic" -> "magic_page_water";
         case "wind_magic" -> "magic_page_wind";
         case "earth_magic" -> "magic_page_earth";
         case "gravity_magic" -> "magic_scroll_gravity_broken";
         case "gander" -> "magic_scroll_gander_broken";
         case "broken_phantasm" -> "magic_scroll_broken_phantasm_broken";
         case "reinforcement_self", "reinforcement_other", "reinforcement_item" -> "magic_page_reinforcement";
         case "jewel_magic_shoot", "jewel_random_shoot" -> "magic_scroll_basic_jewel_broken";
         case "jewel_magic_release" -> "magic_scroll_advanced_jewel_broken";
         case "jewel_machine_gun" -> "magic_scroll_machine_gun_broken";
         default -> "magic_page_" + id;
      };
   }
   public static boolean isConcretePagePath(String path) {
      return RULES.entrySet().stream().anyMatch(entry -> entry.getValue().material() && pageItemPath(entry.getKey()).equals(path));
   }
}
