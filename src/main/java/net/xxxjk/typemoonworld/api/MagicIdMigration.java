package net.xxxjk.typemoonworld.api;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/** Stable aliases for wheel, crest, proficiency and learning data. */
public final class MagicIdMigration {
   private static final Map<String, String> ALIASES = Map.ofEntries(
      Map.entry("ubw", "unlimited_blade_works"),
      Map.entry("typemoonworld:ubw", "unlimited_blade_works"),
      Map.entry("gandr", "gander"),
      Map.entry("typemoonworld:gandr", "gander"),
      Map.entry("jewel_shoot", "jewel_magic_shoot"),
      Map.entry("jewel_release", "jewel_magic_release"),
      Map.entry("typemoonworld:jewel_shoot", "jewel_magic_shoot"),
      Map.entry("typemoonworld:jewel_release", "jewel_magic_release")
   );

   private MagicIdMigration() {}

   public static String normalize(String rawId) {
      if (rawId == null || rawId.isBlank()) return rawId;
      String direct = ALIASES.get(rawId);
      if (direct != null) return direct;
      ResourceLocation parsed = ResourceLocation.tryParse(rawId);
      if (parsed != null) {
         String aliased = ALIASES.get(parsed.getNamespace() + ":" + parsed.getPath());
         if (aliased != null) return aliased;
      }
      return rawId;
   }

   public static Map<String, String> aliases() {
      return Map.copyOf(ALIASES);
   }

   /** Applies aliases in-place while preserving the old short IDs when no alias exists. */
   public static void migratePlayerData(
      java.util.List<String> learned, java.util.List<String> selected,
      java.util.List<? extends Object> wheels, java.util.Map<String, Double> proficiencies,
      java.util.Map<String, Double> progress, java.util.Map<String, Long> progressTicks,
      java.util.Map<String, Integer> crestPractice
   ) {
      replaceList(learned);
      replaceList(selected);
      replaceMap(proficiencies);
      replaceMap(progress);
      replaceMap(progressTicks);
      replaceMap(crestPractice);
   }

   private static void replaceList(java.util.List<String> values) {
      if (values == null) return;
      for (int i = 0; i < values.size(); i++) values.set(i, normalize(values.get(i)));
   }

   private static <V> void replaceMap(java.util.Map<String, V> values) {
      if (values == null || values.isEmpty()) return;
      Map<String, V> migrated = new LinkedHashMap<>();
      for (Map.Entry<String, V> entry : values.entrySet()) {
         String id = normalize(entry.getKey());
         migrated.putIfAbsent(id, entry.getValue());
      }
      values.clear();
      values.putAll(migrated);
   }
}
