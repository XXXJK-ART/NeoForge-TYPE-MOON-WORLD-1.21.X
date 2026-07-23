package net.xxxjk.TYPE_MOON_WORLD.magic;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import net.xxxjk.TYPE_MOON_WORLD.api.MagicDefinitionRegistry;

public final class MagicClassification {
   private static final Map<String, MagicClassification.ManaCostType> MAGIC_COST_TYPES = Map.ofEntries(
      Map.entry("ruby_throw", MagicClassification.ManaCostType.ONE_TIME),
      Map.entry("sapphire_throw", MagicClassification.ManaCostType.ONE_TIME),
      Map.entry("emerald_use", MagicClassification.ManaCostType.ONE_TIME),
      Map.entry("topaz_throw", MagicClassification.ManaCostType.ONE_TIME),
      Map.entry("cyan_throw", MagicClassification.ManaCostType.ONE_TIME),
      Map.entry("ruby_flame_sword", MagicClassification.ManaCostType.ONE_TIME),
      Map.entry("sapphire_winter_frost", MagicClassification.ManaCostType.ONE_TIME),
      Map.entry("emerald_winter_river", MagicClassification.ManaCostType.ONE_TIME),
      Map.entry("topaz_reinforcement", MagicClassification.ManaCostType.ONE_TIME),
      Map.entry("cyan_wind", MagicClassification.ManaCostType.ONE_TIME),
      Map.entry("jewel_magic_shoot", MagicClassification.ManaCostType.ONE_TIME),
      Map.entry("jewel_magic_release", MagicClassification.ManaCostType.ONE_TIME),
      Map.entry("jewel_random_shoot", MagicClassification.ManaCostType.ONE_TIME),
      Map.entry("jewel_machine_gun", MagicClassification.ManaCostType.ONE_TIME),
      Map.entry("gandr_machine_gun", MagicClassification.ManaCostType.ONE_TIME),
      Map.entry("projection", MagicClassification.ManaCostType.ONE_TIME),
      Map.entry("structural_analysis", MagicClassification.ManaCostType.ONE_TIME),
      Map.entry("broken_phantasm", MagicClassification.ManaCostType.ONE_TIME),
      Map.entry("gravity_magic", MagicClassification.ManaCostType.ONE_TIME),
      Map.entry("healing_magic", MagicClassification.ManaCostType.SUSTAINED_OR_INDIRECT),
      Map.entry("magic_bullet", MagicClassification.ManaCostType.ONE_TIME),
      Map.entry("suggestion_magic", MagicClassification.ManaCostType.ONE_TIME),
      Map.entry("binding_magic", MagicClassification.ManaCostType.ONE_TIME),
      Map.entry("fire_magic", MagicClassification.ManaCostType.ONE_TIME),
      Map.entry("water_magic", MagicClassification.ManaCostType.ONE_TIME),
      Map.entry("wind_magic", MagicClassification.ManaCostType.ONE_TIME),
      Map.entry("earth_magic", MagicClassification.ManaCostType.ONE_TIME),
      Map.entry("time_alter", MagicClassification.ManaCostType.SUSTAINED_OR_INDIRECT),
      Map.entry("spiritual_healing", MagicClassification.ManaCostType.SUSTAINED_OR_INDIRECT),
      Map.entry("baptism_rite", MagicClassification.ManaCostType.SUSTAINED_OR_INDIRECT),
      Map.entry("gander", MagicClassification.ManaCostType.SUSTAINED_OR_INDIRECT),
      Map.entry("reinforcement", MagicClassification.ManaCostType.SUSTAINED_OR_INDIRECT),
      Map.entry("reinforcement_self", MagicClassification.ManaCostType.SUSTAINED_OR_INDIRECT),
      Map.entry("reinforcement_other", MagicClassification.ManaCostType.SUSTAINED_OR_INDIRECT),
      Map.entry("reinforcement_item", MagicClassification.ManaCostType.SUSTAINED_OR_INDIRECT),
      Map.entry("unlimited_blade_works", MagicClassification.ManaCostType.SUSTAINED_OR_INDIRECT),
      Map.entry("sword_barrel_full_open", MagicClassification.ManaCostType.SUSTAINED_OR_INDIRECT),
      Map.entry("bajiquan", MagicClassification.ManaCostType.SUSTAINED_OR_INDIRECT),
      Map.entry("ganryu", MagicClassification.ManaCostType.SUSTAINED_OR_INDIRECT)
   );
   private static final Set<String> ALL_MAGIC_IDS = Collections.unmodifiableSet(MAGIC_COST_TYPES.keySet());
   private static final Map<String, MagicClassification.MagicSchoolType> MAGIC_SCHOOL_TYPES = Map.ofEntries(
      Map.entry("gander", MagicClassification.MagicSchoolType.NORDIC),
      Map.entry("gandr_machine_gun", MagicClassification.MagicSchoolType.NORDIC),
      Map.entry("baptism_rite", MagicClassification.MagicSchoolType.CHURCH)
   );

   private MagicClassification() {
   }

   public static boolean isKnownMagic(String magicId) {
      return magicId != null && (MAGIC_COST_TYPES.containsKey(magicId) || MagicDefinitionRegistry.contains(magicId));
   }

   public static MagicClassification.ManaCostType getManaCostType(String magicId) {
      return MAGIC_COST_TYPES.getOrDefault(magicId, MagicClassification.ManaCostType.ONE_TIME);
   }

   public static Set<String> getAllMagicIds() {
      java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>(ALL_MAGIC_IDS);
      ids.addAll(MagicDefinitionRegistry.ids());
      return java.util.Collections.unmodifiableSet(ids);
   }

   public static MagicClassification.MagicSchoolType getSchoolType(String magicId) {
      if (MagicDefinitionRegistry.contains(magicId)) {
         String school = MagicDefinitionRegistry.get(magicId).school().getPath();
         if ("nordic".equals(school)) return MagicSchoolType.NORDIC;
         if ("church".equals(school)) return MagicSchoolType.CHURCH;
      }
      return MAGIC_SCHOOL_TYPES.getOrDefault(magicId, MagicClassification.MagicSchoolType.NONE);
   }

   public static enum MagicSchoolType {
      NONE,
      NORDIC,
      CHURCH;
   }

   public static enum ManaCostType {
      ONE_TIME,
      SUSTAINED_OR_INDIRECT;
   }
}
