package net.xxxjk.typemoonworld.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** Static metadata for a magic executor. JSON loaders may produce the same shape. */
public record MagicDefinitionData(
   ResourceLocation id,
   String nameKey,
   ResourceLocation category,
   ResourceLocation school,
   int complexity,
   int chantSegments,
   double manaCost,
   double sustainedManaCost,
   int cooldownTicks,
   boolean learnable,
   boolean wheelSelectable,
   boolean crestAllowed,
   boolean npcAllowed,
   boolean knowledgeOnly,
   boolean copyable,
   int npcGlobalCooldown,
   int npcCooldown,
   ResourceLocation prerequisiteMagic,
   double prerequisiteProficiency,
   List<ResourceLocation> requiredAttributes,
   MagicComplexity resistanceComplexity
) {
   private static final ResourceLocation INVALID_ID = ResourceLocation.fromNamespaceAndPath("typemoonworld", "invalid");
   private static final ResourceLocation BASIC = ResourceLocation.fromNamespaceAndPath("typemoonworld", "basic");
   private static final ResourceLocation NONE = ResourceLocation.fromNamespaceAndPath("typemoonworld", "none");
   public static final Codec<MagicDefinitionData> CODEC = Codec.STRING.xmap(MagicDefinitionData::fromJsonString, MagicDefinitionData::toJsonString);

   public MagicDefinitionData {
      if (id == null) throw new IllegalArgumentException("id");
      nameKey = nameKey == null ? "" : nameKey;
      category = category == null ? ResourceLocation.fromNamespaceAndPath("typemoonworld", "basic") : category;
      school = school == null ? ResourceLocation.fromNamespaceAndPath("typemoonworld", "none") : school;
      complexity = Math.max(0, complexity);
      chantSegments = Math.max(0, chantSegments);
      manaCost = Math.max(0.0, manaCost);
      sustainedManaCost = Math.max(0.0, sustainedManaCost);
      cooldownTicks = Math.max(0, cooldownTicks);
      npcGlobalCooldown = Math.max(0, npcGlobalCooldown);
      npcCooldown = Math.max(0, npcCooldown);
      prerequisiteProficiency = Math.max(0.0, Math.min(100.0, prerequisiteProficiency));
      requiredAttributes = requiredAttributes == null ? List.of() : requiredAttributes.stream()
         .filter(Objects::nonNull).distinct().limit(32).toList();
   }

   public MagicDefinitionData(ResourceLocation id, String nameKey, ResourceLocation category, ResourceLocation school,
         int complexity, int chantSegments, double manaCost, double sustainedManaCost, int cooldownTicks,
         boolean learnable, boolean wheelSelectable, boolean crestAllowed, boolean npcAllowed, boolean knowledgeOnly,
         boolean copyable, int npcGlobalCooldown, int npcCooldown, ResourceLocation prerequisiteMagic,
         double prerequisiteProficiency,
         List<ResourceLocation> requiredAttributes) {
      this(id, nameKey, category, school, complexity, chantSegments, manaCost, sustainedManaCost, cooldownTicks,
         learnable, wheelSelectable, crestAllowed, npcAllowed, knowledgeOnly, copyable, npcGlobalCooldown, npcCooldown,
         prerequisiteMagic, prerequisiteProficiency, requiredAttributes, null);
   }

   /** Source-compatible constructor for v1 addons that do not declare attribute requirements. */
   public MagicDefinitionData(ResourceLocation id, String nameKey, ResourceLocation category, ResourceLocation school,
         int complexity, int chantSegments, double manaCost, double sustainedManaCost, int cooldownTicks,
         boolean learnable, boolean wheelSelectable, boolean crestAllowed, boolean npcAllowed, boolean knowledgeOnly,
         boolean copyable, int npcGlobalCooldown, int npcCooldown) {
      this(id, nameKey, category, school, complexity, chantSegments, manaCost, sustainedManaCost, cooldownTicks,
         learnable, wheelSelectable, crestAllowed, npcAllowed, knowledgeOnly, copyable, npcGlobalCooldown, npcCooldown,
         null, 0.0, List.of(), null);
   }

   /** Source-compatible constructor for v1 addons that do not declare attribute requirements. */
   public MagicDefinitionData(ResourceLocation id, String nameKey, ResourceLocation category, ResourceLocation school,
         double manaCost, int cooldownTicks, boolean learnable, boolean wheelSelectable, boolean crestAllowed,
         boolean npcAllowed, boolean knowledgeOnly, int npcGlobalCooldown, int npcCooldown, List<ResourceLocation> requiredAttributes) {
      this(id, nameKey, category, school, 50, 1, manaCost, 0.0, cooldownTicks, learnable, wheelSelectable, crestAllowed,
         npcAllowed, knowledgeOnly, true, npcGlobalCooldown, npcCooldown, null, 0.0, requiredAttributes, null);
   }

   /** Source-compatible constructor for older addon registrations that only carried the v1 fields. */
   public MagicDefinitionData(ResourceLocation id, String nameKey, ResourceLocation category, ResourceLocation school,
         double manaCost, int cooldownTicks, boolean learnable, boolean wheelSelectable, boolean crestAllowed,
         boolean npcAllowed, boolean knowledgeOnly, int npcGlobalCooldown, int npcCooldown) {
      this(id, nameKey, category, school, 50, 1, manaCost, 0.0, cooldownTicks, learnable, wheelSelectable, crestAllowed,
         npcAllowed, knowledgeOnly, true, npcGlobalCooldown, npcCooldown, null, 0.0, List.of(), null);
   }

   public JsonObject toJson() {
      JsonObject json = new JsonObject();
      json.addProperty("id", this.id.toString());
      if (!this.nameKey.isBlank()) json.addProperty("name_key", this.nameKey);
      if (this.category != null) json.addProperty("category", this.category.toString());
      if (this.school != null) json.addProperty("school", this.school.toString());
      if (this.complexity != 50) json.addProperty("complexity", this.complexity);
      if (this.chantSegments != 1) json.addProperty("chant_segments", this.chantSegments);
      if (this.manaCost != 0.0) json.addProperty("mana_cost", this.manaCost);
      if (this.sustainedManaCost != 0.0) json.addProperty("sustained_mana_cost", this.sustainedManaCost);
      if (this.cooldownTicks != 10) json.addProperty("cooldown_ticks", this.cooldownTicks);
      if (!this.learnable) json.addProperty("learnable", this.learnable);
      if (!this.wheelSelectable) json.addProperty("wheel_selectable", this.wheelSelectable);
      if (!this.crestAllowed) json.addProperty("crest_allowed", this.crestAllowed);
      if (!this.npcAllowed) json.addProperty("npc_allowed", this.npcAllowed);
      if (this.knowledgeOnly) json.addProperty("knowledge_only", this.knowledgeOnly);
      if (!this.copyable) json.addProperty("copyable", this.copyable);
      if (this.npcGlobalCooldown != 12) json.addProperty("npc_global_cooldown", this.npcGlobalCooldown);
      if (this.npcCooldown != 20) json.addProperty("npc_cooldown", this.npcCooldown);
      if (this.prerequisiteMagic != null) json.addProperty("prerequisite_magic", this.prerequisiteMagic.toString());
      if (this.prerequisiteProficiency != 0.0) json.addProperty("prerequisite_proficiency", this.prerequisiteProficiency);
      if (!this.requiredAttributes.isEmpty()) {
         JsonArray array = new JsonArray();
         for (ResourceLocation attribute : this.requiredAttributes) {
            if (attribute != null) array.add(attribute.toString());
         }
         json.add("required_attributes", array);
      }
      if (this.resistanceComplexity != null) json.addProperty("resistance_complexity", this.resistanceComplexity.name());
      return json;
   }

   public String toJsonString() {
      return toJson().toString();
   }

   public static MagicDefinitionData fromJsonString(String text) {
      if (text == null || text.isBlank()) {
         return defaults(INVALID_ID);
      }
      return fromJson(JsonParser.parseString(text).getAsJsonObject());
   }

   public static MagicDefinitionData fromJson(JsonObject json) {
      if (json == null) {
         return defaults(INVALID_ID);
      }
      ResourceLocation id = parseLocation(json, "id", INVALID_ID);
      String nameKey = json.has("name_key") ? json.get("name_key").getAsString() : "";
      ResourceLocation category = parseLocation(json, "category", BASIC);
      ResourceLocation school = parseLocation(json, "school", NONE);
      int complexity = json.has("complexity") ? json.get("complexity").getAsInt() : 50;
      int chantSegments = json.has("chant_segments") ? json.get("chant_segments").getAsInt() : 1;
      double manaCost = json.has("mana_cost") ? json.get("mana_cost").getAsDouble() : 0.0;
      double sustainedManaCost = json.has("sustained_mana_cost") ? json.get("sustained_mana_cost").getAsDouble() : 0.0;
      int cooldownTicks = json.has("cooldown_ticks") ? json.get("cooldown_ticks").getAsInt() : 10;
      boolean learnable = !json.has("learnable") || json.get("learnable").getAsBoolean();
      boolean wheelSelectable = !json.has("wheel_selectable") || json.get("wheel_selectable").getAsBoolean();
      boolean crestAllowed = !json.has("crest_allowed") || json.get("crest_allowed").getAsBoolean();
      boolean npcAllowed = !json.has("npc_allowed") || json.get("npc_allowed").getAsBoolean();
      boolean knowledgeOnly = json.has("knowledge_only") && json.get("knowledge_only").getAsBoolean();
      boolean copyable = !json.has("copyable") || json.get("copyable").getAsBoolean();
      int npcGlobalCooldown = json.has("npc_global_cooldown") ? json.get("npc_global_cooldown").getAsInt() : 12;
      int npcCooldown = json.has("npc_cooldown") ? json.get("npc_cooldown").getAsInt() : 20;
      ResourceLocation prerequisiteMagic = json.has("prerequisite_magic") ? ResourceLocation.tryParse(json.get("prerequisite_magic").getAsString()) : null;
      double prerequisiteProficiency = json.has("prerequisite_proficiency") ? json.get("prerequisite_proficiency").getAsDouble() : 0.0;
      List<ResourceLocation> requiredAttributes = new ArrayList<>();
      if (json.has("required_attributes") && json.get("required_attributes").isJsonArray()) {
         for (JsonElement element : json.getAsJsonArray("required_attributes")) {
            ResourceLocation parsed = ResourceLocation.tryParse(element.getAsString());
            if (parsed != null) requiredAttributes.add(parsed);
         }
      }
      MagicComplexity resistanceComplexity = null;
      if (json.has("resistance_complexity")) {
         try {
            resistanceComplexity = MagicComplexity.valueOf(json.get("resistance_complexity").getAsString().toUpperCase(java.util.Locale.ROOT));
         } catch (IllegalArgumentException ignored) {
            resistanceComplexity = null;
         }
      }
      return new MagicDefinitionData(id, nameKey, category, school, complexity, chantSegments, manaCost, sustainedManaCost,
         cooldownTicks, learnable, wheelSelectable, crestAllowed, npcAllowed, knowledgeOnly, copyable, npcGlobalCooldown,
         npcCooldown, prerequisiteMagic, prerequisiteProficiency, requiredAttributes, resistanceComplexity);
   }

   private static ResourceLocation parseLocation(JsonObject json, String key, ResourceLocation fallback) {
      if (json == null || key == null || !json.has(key)) return fallback;
      ResourceLocation parsed = ResourceLocation.tryParse(json.get(key).getAsString());
      return parsed == null ? fallback : parsed;
   }

   public static MagicDefinitionData defaults(ResourceLocation id) {
      return new MagicDefinitionData(id, "magic." + id.getNamespace() + "." + id.getPath() + ".name",
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "basic"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "none"), 50, 1, 0.0, 0.0, 10,
         true, true, true, true, false, true, 12, 20, null, 0.0, List.of(), null);
   }

   public MagicDefinitionData withId(ResourceLocation newId) {
      String resolvedKey = this.nameKey.isBlank() ? "magic." + newId.getNamespace() + "." + newId.getPath() + ".name" : this.nameKey;
      return new MagicDefinitionData(newId, resolvedKey, this.category, this.school, this.complexity, this.chantSegments,
         this.manaCost, this.sustainedManaCost, this.cooldownTicks, this.learnable, this.wheelSelectable, this.crestAllowed,
         this.npcAllowed, this.knowledgeOnly, this.copyable, this.npcGlobalCooldown, this.npcCooldown, this.prerequisiteMagic,
         this.prerequisiteProficiency, this.requiredAttributes, this.resistanceComplexity);
   }

   public MagicComplexity resolvedResistanceComplexity() {
      return this.resistanceComplexity != null ? this.resistanceComplexity : MagicComplexity.infer(this.id, this.manaCost, this.category);
   }
}
