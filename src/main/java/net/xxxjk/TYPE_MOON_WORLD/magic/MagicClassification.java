package net.xxxjk.TYPE_MOON_WORLD.magic;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public final class MagicClassification {
   private static final Map<String, MagicClassification.ManaCostType> MAGIC_COST_TYPES = Map.ofEntries(
      Map.entry("jewel_magic_shoot", MagicClassification.ManaCostType.ONE_TIME),
      Map.entry("jewel_magic_release", MagicClassification.ManaCostType.ONE_TIME),
      Map.entry("jewel_random_shoot", MagicClassification.ManaCostType.ONE_TIME),
      Map.entry("jewel_machine_gun", MagicClassification.ManaCostType.ONE_TIME),
      Map.entry("gandr_machine_gun", MagicClassification.ManaCostType.ONE_TIME),
      Map.entry("projection", MagicClassification.ManaCostType.ONE_TIME),
      Map.entry("structural_analysis", MagicClassification.ManaCostType.ONE_TIME),
      Map.entry("broken_phantasm", MagicClassification.ManaCostType.ONE_TIME),
      Map.entry("gravity_magic", MagicClassification.ManaCostType.ONE_TIME),
      Map.entry("gander", MagicClassification.ManaCostType.SUSTAINED_OR_INDIRECT),
      Map.entry("reinforcement", MagicClassification.ManaCostType.SUSTAINED_OR_INDIRECT),
      Map.entry("reinforcement_self", MagicClassification.ManaCostType.SUSTAINED_OR_INDIRECT),
      Map.entry("reinforcement_other", MagicClassification.ManaCostType.SUSTAINED_OR_INDIRECT),
      Map.entry("reinforcement_item", MagicClassification.ManaCostType.SUSTAINED_OR_INDIRECT),
      Map.entry("unlimited_blade_works", MagicClassification.ManaCostType.SUSTAINED_OR_INDIRECT),
      Map.entry("sword_barrel_full_open", MagicClassification.ManaCostType.SUSTAINED_OR_INDIRECT)
   );
   private static final Set<String> ALL_MAGIC_IDS = Collections.unmodifiableSet(MAGIC_COST_TYPES.keySet());
   private static final Map<String, MagicClassification.MagicSchoolType> MAGIC_SCHOOL_TYPES = Map.ofEntries(
      Map.entry("gander", MagicClassification.MagicSchoolType.NORDIC), Map.entry("gandr_machine_gun", MagicClassification.MagicSchoolType.NORDIC)
   );

   private MagicClassification() {
   }

   public static boolean isKnownMagic(String magicId) {
      return magicId != null && MAGIC_COST_TYPES.containsKey(magicId);
   }

   public static MagicClassification.ManaCostType getManaCostType(String magicId) {
      return MAGIC_COST_TYPES.getOrDefault(magicId, MagicClassification.ManaCostType.ONE_TIME);
   }

   public static Set<String> getAllMagicIds() {
      return ALL_MAGIC_IDS;
   }

   public static MagicClassification.MagicSchoolType getSchoolType(String magicId) {
      return MAGIC_SCHOOL_TYPES.getOrDefault(magicId, MagicClassification.MagicSchoolType.NONE);
   }

   public static enum MagicSchoolType {
      NONE,
      NORDIC;
   }

   public static enum ManaCostType {
      ONE_TIME,
      SUSTAINED_OR_INDIRECT;
   }
}
