package net.xxxjk.typemoonworld.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * The small, stable canon layer shared by JSON definitions and legacy built-ins.
 * Values are deliberately strings so addon authors can extend the vocabulary
 * without a binary API change. Unknown entries remain explicitly MOD original.
 */
public final class MagicCanonMatrix {
   private MagicCanonMatrix() {}

   public record Entry(String canonStatus, String sourceKey, String magicType, String requiredOrigin,
         String requiredFoundation, String requiredCatalyst, String castMode, String resourceModel,
         String copyPolicy, boolean playerAllowed, boolean npcAllowed) {}

   private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

   static {
      ordinary("fire_magic", "elemental_magic");
      ordinary("water_magic", "elemental_magic");
      ordinary("wind_magic", "elemental_magic");
      ordinary("earth_magic", "elemental_magic");
      ordinary("magic_bullet", "ordinary_magic");
      ordinary("reinforcement", "reinforcement");
      ordinary("reinforcement_self", "reinforcement");
      ordinary("reinforcement_other", "reinforcement");
      ordinary("reinforcement_item", "reinforcement");
      ordinary("healing_magic", "ordinary_magic");
      ordinary("binding_magic", "ordinary_magic");
      ordinary("suggestion_magic", "ordinary_magic");
      ordinary("gravity_magic", "ordinary_magic");
      ordinary("structural_analysis", "structural_analysis");
      ordinary("projection", "projection");
      ordinary("flame_array", "elemental_array");
      ordinary("azure_water_array", "elemental_array");
      ordinary("gale_wind_array", "elemental_array");
      ordinary("rock_earth_array", "elemental_array");
      entry("magic_analysis", "canon", "fate/clock_tower", "ordinary_magic", null, null, null, "sustained", "personal_mana", "research_only", true, true);

      for (String id : Set.of("ruby_throw", "sapphire_throw", "emerald_use", "topaz_throw", "cyan_throw",
         "ruby_flame_sword", "sapphire_winter_frost", "emerald_winter_river", "topaz_reinforcement", "cyan_wind",
         "jewel_magic_shoot", "jewel_magic_release", "jewel_random_shoot", "jewel_machine_gun")) {
         entry(id, "canon", "fate/kaleid", "gemstone_magic", "gemstone_magic", null, "charged_gem", "instant", "gem_inventory",
            id.startsWith("jewel_magic_") ? "researchable" : "none", true, true);
      }

      entry("unlimited_blade_works", "character_exclusive", "fate/emiya", "reality_marble", "reality_marble", "projection", "sword",
         "ritual", "personal_mana", "researchable", true, false);
      entry("sword_barrel_full_open", "character_exclusive", "fate/emiya", "projection", "unlimited_blade_works", "sword",
         "sword", "instant", "personal_mana", "researchable", true, false);
      entry("ubw_sword_control", "character_exclusive", "fate/emiya", "projection", "unlimited_blade_works", "sword",
         "sword", "sustained", "personal_mana", "researchable", true, false);
      entry("broken_phantasm", "character_exclusive", "fate/emiya", "projection", "unlimited_blade_works", "sword",
         "sword", "instant", "personal_mana", "researchable", true, false);

      for (String id : Set.of("theology", "black_key_making", "baptism_rite", "stigma", "iron_armor_action", "cremation_rite")) {
         entry(id, "canon", "fate/church", "church_mystery", "church_mystery", "theology", "scripture_or_black_key", "ritual",
            "personal_mana", "researchable", true, true);
      }
      entry("rune_origin", "canon", "fate/lostbelt", "primordial_rune", "primordial_rune", "rune_foundation", "rune_program", "ritual",
         "personal_mana", "knowledge_only", true, true);
   }

   private static void ordinary(String id, String type) {
      entry(id, "canon", "fate/clock_tower", type, null, null, null, "instant", "personal_mana", "researchable", true, true);
   }

   private static void entry(String id, String status, String source, String type, String origin, String foundation,
         String catalyst, String mode, String resources, String copy, boolean player, boolean npc) {
      ENTRIES.put(id, new Entry(status, source, type, origin, foundation, catalyst, mode, resources, copy, player, npc));
   }

   // Compatibility overload for the expanded built-in declarations used by
   // older generated sources. The final metadata token is retained by the
   // current copy-policy slot; the intermediate token remains a legacy alias.
   private static void entry(String id, String status, String source, String type, String origin, String foundation,
         String catalyst, String mode, String resources, String legacyResource, String copy, boolean player, boolean npc) {
      entry(id, status, source, type, origin, foundation, catalyst, mode, resources, legacyResource, player, npc);
   }

   public static Entry forId(String rawId) {
      if (rawId == null || rawId.isBlank()) return new Entry("mod_original", "unknown", "mod_original", null, null, null,
         "instant", "personal_mana", "none", true, true);
      ResourceLocation parsed = ResourceLocation.tryParse(rawId);
      String path = parsed == null ? rawId : parsed.getPath();
      Entry known = ENTRIES.get(path);
      if (known != null) return known;
      String source = parsed == null ? "typemoonworld:legacy" : parsed.toString();
      return new Entry("mod_original", source, "mod_original", null, null, null, "instant", "personal_mana", "none", true, true);
   }

   public static Map<String, Entry> entries() {
      return Map.copyOf(ENTRIES);
   }

   public static MagicDefinitionData decorate(MagicDefinitionData definition) {
      if (definition == null) return null;
      Entry entry = forId(definition.id().toString());
      return definition.withCanonMetadata(entry.canonStatus(), entry.sourceKey(), entry.magicType(), entry.requiredOrigin(),
         entry.requiredFoundation(), entry.requiredCatalyst(), entry.castMode(), entry.resourceModel(), entry.copyPolicy(),
         entry.playerAllowed(), entry.npcAllowed());
   }

   public static MagicDefinitionData decorateIfKnown(MagicDefinitionData definition) {
      if (definition == null) return null;
      ResourceLocation id = definition.id();
      return ENTRIES.containsKey(id.getPath()) && "mod_original".equals(definition.canonStatus())
         ? decorate(definition) : definition;
   }
}
