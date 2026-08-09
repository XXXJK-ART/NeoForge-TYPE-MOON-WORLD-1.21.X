package net.xxxjk.typemoonworld.api;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.resources.ResourceLocation;

public enum MagicComplexity {
   SIMPLE_ACTION(0, "simple_action"),
   ONE_VERSE(1, "one_verse"),
   TWO_VERSE(2, "two_verse"),
   THREE_VERSE(3, "three_verse"),
   HIGH_THAUMATURGY(4, "high_thaumaturgy");

   public static final Codec<MagicComplexity> CODEC = Codec.STRING.xmap(MagicComplexity::fromKey, MagicComplexity::key);

   private final int level;
   private final String key;

   MagicComplexity(int level, String key) {
      this.level = level;
      this.key = key;
   }

   public int level() {
      return this.level;
   }

   public String key() {
      return this.key;
   }

   public static MagicComplexity fromKey(String key) {
      if (key == null || key.isBlank()) {
         return ONE_VERSE;
      }
      String normalized = key.trim().toLowerCase(Locale.ROOT);
      for (MagicComplexity complexity : values()) {
         if (complexity.key.equals(normalized) || complexity.name().toLowerCase(Locale.ROOT).equals(normalized)) {
            return complexity;
         }
      }
      return ONE_VERSE;
   }

   public static MagicComplexity infer(ResourceLocation id, double manaCost, ResourceLocation category) {
      String path = id == null ? "" : id.getPath();
      return switch (path) {
         case "gander", "magic_bullet", "ruby_throw", "sapphire_throw", "topaz_throw", "cyan_throw",
            "fire_magic", "water_magic", "wind_magic", "earth_magic" -> SIMPLE_ACTION;
         case "binding_magic", "suggestion_magic", "gravity_magic", "reinforcement", "reinforcement_self",
            "reinforcement_other", "reinforcement_item", "topaz_reinforcement", "emerald_use" -> ONE_VERSE;
         case "healing_magic", "spiritual_healing", "ruby_flame_sword", "sapphire_winter_frost",
            "emerald_winter_river", "cyan_wind", "baptism_rite", "black_key_fire_engraving" -> TWO_VERSE;
         case "projection", "structural_analysis", "time_alter", "jewel_random_shoot", "jewel_machine_gun",
            "gandr_machine_gun", "stigma" -> THREE_VERSE;
         case "unlimited_blade_works", "sword_barrel_full_open", "broken_phantasm" -> HIGH_THAUMATURGY;
         default -> inferFromCost(manaCost, category);
      };
   }

   private static MagicComplexity inferFromCost(double manaCost, ResourceLocation category) {
      if (category != null && "ritual".equals(category.getPath())) {
         return HIGH_THAUMATURGY;
      }
      if (manaCost >= 180.0) return HIGH_THAUMATURGY;
      if (manaCost >= 100.0) return THREE_VERSE;
      if (manaCost >= 50.0) return TWO_VERSE;
      if (manaCost >= 20.0) return ONE_VERSE;
      return SIMPLE_ACTION;
   }
}
