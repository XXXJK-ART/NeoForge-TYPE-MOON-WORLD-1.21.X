package net.xxxjk.typemoonworld.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
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
   MagicComplexity resistanceComplexity,
   String canonStatus,
   String sourceKey,
   String magicType,
   String requiredOrigin,
   String requiredFoundation,
   String requiredCatalyst,
   String castMode,
   String resourceModel,
   String copyPolicy,
   boolean playerAllowed
) {
   private static final ResourceLocation INVALID_ID = ResourceLocation.fromNamespaceAndPath("typemoonworld", "invalid");
   private static final ResourceLocation BASIC = ResourceLocation.fromNamespaceAndPath("typemoonworld", "basic");
   private static final ResourceLocation NONE = ResourceLocation.fromNamespaceAndPath("typemoonworld", "none");
   public static final Codec<MagicDefinitionData> CODEC = Codec.PASSTHROUGH.comapFlatMap(
      MagicDefinitionData::fromDynamic,
      definition -> new Dynamic<>(JsonOps.INSTANCE, definition.toJson())
   );

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
      canonStatus = normalizeText(canonStatus, "mod_original");
      sourceKey = normalizeText(sourceKey, id.toString());
      magicType = normalizeText(magicType, category.getPath());
      requiredOrigin = emptyToNull(requiredOrigin);
      requiredFoundation = emptyToNull(requiredFoundation);
      requiredCatalyst = emptyToNull(requiredCatalyst);
      castMode = normalizeText(castMode, "instant");
      resourceModel = normalizeText(resourceModel, "personal_mana");
      copyPolicy = normalizeText(copyPolicy, copyable ? "copyable" : "knowledge_only");
   }

   /** Source-compatible form of the pre-canon 21-field constructor. */
   public MagicDefinitionData(ResourceLocation id, String nameKey, ResourceLocation category, ResourceLocation school,
         int complexity, int chantSegments, double manaCost, double sustainedManaCost, int cooldownTicks,
         boolean learnable, boolean wheelSelectable, boolean crestAllowed, boolean npcAllowed, boolean knowledgeOnly,
         boolean copyable, int npcGlobalCooldown, int npcCooldown, ResourceLocation prerequisiteMagic,
         double prerequisiteProficiency, List<ResourceLocation> requiredAttributes, MagicComplexity resistanceComplexity) {
      this(id, nameKey, category, school, complexity, chantSegments, manaCost, sustainedManaCost, cooldownTicks,
         learnable, wheelSelectable, crestAllowed, npcAllowed, knowledgeOnly, copyable, npcGlobalCooldown, npcCooldown,
         prerequisiteMagic, prerequisiteProficiency, requiredAttributes, resistanceComplexity,
         null, null, null, null, null, null, null, null, null, true);
   }

   private static String normalizeText(String value, String fallback) {
      return value == null || value.isBlank() ? fallback : value.trim().toLowerCase(java.util.Locale.ROOT);
   }

   private static String emptyToNull(String value) {
      return value == null || value.isBlank() ? null : value.trim();
   }

   public MagicDefinitionData(ResourceLocation id, String nameKey, ResourceLocation category, ResourceLocation school,
         int complexity, int chantSegments, double manaCost, double sustainedManaCost, int cooldownTicks,
         boolean learnable, boolean wheelSelectable, boolean crestAllowed, boolean npcAllowed, boolean knowledgeOnly,
         boolean copyable, int npcGlobalCooldown, int npcCooldown, ResourceLocation prerequisiteMagic,
         double prerequisiteProficiency,
         List<ResourceLocation> requiredAttributes) {
      this(id, nameKey, category, school, complexity, chantSegments, manaCost, sustainedManaCost, cooldownTicks,
         learnable, wheelSelectable, crestAllowed, npcAllowed, knowledgeOnly, copyable, npcGlobalCooldown, npcCooldown,
         prerequisiteMagic, prerequisiteProficiency, requiredAttributes, null,
         null, null, null, null, null, null, null, null, null, true);
   }

   /** Source-compatible constructor for v1 addons that do not declare attribute requirements. */
   public MagicDefinitionData(ResourceLocation id, String nameKey, ResourceLocation category, ResourceLocation school,
         int complexity, int chantSegments, double manaCost, double sustainedManaCost, int cooldownTicks,
         boolean learnable, boolean wheelSelectable, boolean crestAllowed, boolean npcAllowed, boolean knowledgeOnly,
         boolean copyable, int npcGlobalCooldown, int npcCooldown) {
      this(id, nameKey, category, school, complexity, chantSegments, manaCost, sustainedManaCost, cooldownTicks,
         learnable, wheelSelectable, crestAllowed, npcAllowed, knowledgeOnly, copyable, npcGlobalCooldown, npcCooldown,
         null, 0.0, List.of(), null,
         null, null, null, null, null, null, null, null, null, true);
   }

   /** Source-compatible constructor for v1 addons that do not declare attribute requirements. */
   public MagicDefinitionData(ResourceLocation id, String nameKey, ResourceLocation category, ResourceLocation school,
         double manaCost, int cooldownTicks, boolean learnable, boolean wheelSelectable, boolean crestAllowed,
         boolean npcAllowed, boolean knowledgeOnly, int npcGlobalCooldown, int npcCooldown, List<ResourceLocation> requiredAttributes) {
      this(id, nameKey, category, school, 50, 1, manaCost, 0.0, cooldownTicks, learnable, wheelSelectable, crestAllowed,
         npcAllowed, knowledgeOnly, true, npcGlobalCooldown, npcCooldown, null, 0.0, requiredAttributes, null,
         null, null, null, null, null, null, null, null, null, true);
   }

   /** Source-compatible constructor for older addon registrations that only carried the v1 fields. */
   public MagicDefinitionData(ResourceLocation id, String nameKey, ResourceLocation category, ResourceLocation school,
         double manaCost, int cooldownTicks, boolean learnable, boolean wheelSelectable, boolean crestAllowed,
         boolean npcAllowed, boolean knowledgeOnly, int npcGlobalCooldown, int npcCooldown) {
      this(id, nameKey, category, school, 50, 1, manaCost, 0.0, cooldownTicks, learnable, wheelSelectable, crestAllowed,
         npcAllowed, knowledgeOnly, true, npcGlobalCooldown, npcCooldown, null, 0.0, List.of(), null,
         null, null, null, null, null, null, null, null, null, true);
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
      if (!"mod_original".equals(this.canonStatus)) json.addProperty("canon_status", this.canonStatus);
      if (!this.id.toString().equals(this.sourceKey)) json.addProperty("source_key", this.sourceKey);
      if (!this.magicType.equals(this.category.getPath())) json.addProperty("magic_type", this.magicType);
      if (this.requiredOrigin != null) json.addProperty("required_origin", this.requiredOrigin);
      if (this.requiredFoundation != null) json.addProperty("required_foundation", this.requiredFoundation);
      if (this.requiredCatalyst != null) json.addProperty("required_catalyst", this.requiredCatalyst);
      if (!"instant".equals(this.castMode)) json.addProperty("cast_mode", this.castMode);
      if (!"personal_mana".equals(this.resourceModel)) json.addProperty("resource_model", this.resourceModel);
      if (!this.copyPolicy.equals(this.copyable ? "copyable" : "knowledge_only")) json.addProperty("copy_policy", this.copyPolicy);
      if (!this.playerAllowed) json.addProperty("player_allowed", false);
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

   private static DataResult<MagicDefinitionData> fromDynamic(Dynamic<?> dynamic) {
      try {
         JsonElement json = dynamic.convert(JsonOps.INSTANCE).getValue();
         if (json.isJsonObject()) {
            return DataResult.success(fromJson(json.getAsJsonObject()));
         }
         if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
            return DataResult.success(fromJsonString(json.getAsString()));
         }
         return DataResult.error(() -> "Expected magic definition object or JSON string");
      } catch (Exception exception) {
         return DataResult.error(exception::getMessage);
      }
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
      String canonStatus = json.has("canon_status") ? json.get("canon_status").getAsString() : null;
      String sourceKey = json.has("source_key") ? json.get("source_key").getAsString() : null;
      String magicType = json.has("magic_type") ? json.get("magic_type").getAsString() : null;
      String requiredOrigin = json.has("required_origin") ? json.get("required_origin").getAsString() : null;
      String requiredFoundation = json.has("required_foundation") ? json.get("required_foundation").getAsString() : null;
      String requiredCatalyst = json.has("required_catalyst") ? json.get("required_catalyst").getAsString() : null;
      String castMode = json.has("cast_mode") ? json.get("cast_mode").getAsString() : null;
      String resourceModel = json.has("resource_model") ? json.get("resource_model").getAsString() : null;
      String copyPolicy = json.has("copy_policy") ? json.get("copy_policy").getAsString() : null;
      boolean playerAllowed = !json.has("player_allowed") || json.get("player_allowed").getAsBoolean();
      return new MagicDefinitionData(id, nameKey, category, school, complexity, chantSegments, manaCost, sustainedManaCost,
         cooldownTicks, learnable, wheelSelectable, crestAllowed, npcAllowed, knowledgeOnly, copyable, npcGlobalCooldown,
         npcCooldown, prerequisiteMagic, prerequisiteProficiency, requiredAttributes, resistanceComplexity,
         canonStatus, sourceKey, magicType, requiredOrigin, requiredFoundation, requiredCatalyst, castMode, resourceModel,
         copyPolicy, playerAllowed);
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
         true, true, true, true, false, true, 12, 20, null, 0.0, List.of(), null,
         null, null, null, null, null, null, null, null, null, true);
   }

   public MagicDefinitionData withId(ResourceLocation newId) {
      String resolvedKey = this.nameKey.isBlank() ? "magic." + newId.getNamespace() + "." + newId.getPath() + ".name" : this.nameKey;
      String resolvedSource = this.sourceKey.equals(this.id.toString()) || this.id.equals(INVALID_ID) ? newId.toString() : this.sourceKey;
      return new MagicDefinitionData(newId, resolvedKey, this.category, this.school, this.complexity, this.chantSegments,
         this.manaCost, this.sustainedManaCost, this.cooldownTicks, this.learnable, this.wheelSelectable, this.crestAllowed,
         this.npcAllowed, this.knowledgeOnly, this.copyable, this.npcGlobalCooldown, this.npcCooldown, this.prerequisiteMagic,
         this.prerequisiteProficiency, this.requiredAttributes, this.resistanceComplexity, this.canonStatus, resolvedSource,
         this.magicType, this.requiredOrigin, this.requiredFoundation, this.requiredCatalyst, this.castMode, this.resourceModel,
         this.copyPolicy, this.playerAllowed);
   }

   public MagicDefinitionData withCanonMetadata(String status, String source, String type, String origin,
         String foundation, String catalyst, String mode, String resources, String policy, boolean allowedForPlayer) {
      return withCanonMetadata(status, source, type, origin, foundation, catalyst, mode, resources, policy,
         allowedForPlayer, this.npcAllowed);
   }

   public MagicDefinitionData withCanonMetadata(String status, String source, String type, String origin,
         String foundation, String catalyst, String mode, String resources, String policy, boolean allowedForPlayer,
         boolean allowedForNpc) {
      return new MagicDefinitionData(this.id, this.nameKey, this.category, this.school, this.complexity, this.chantSegments,
         this.manaCost, this.sustainedManaCost, this.cooldownTicks, this.learnable, this.wheelSelectable, this.crestAllowed,
         allowedForNpc, this.knowledgeOnly, this.copyable, this.npcGlobalCooldown, this.npcCooldown, this.prerequisiteMagic,
         this.prerequisiteProficiency, this.requiredAttributes, this.resistanceComplexity, status, source, type, origin,
         foundation, catalyst, mode, resources, policy, allowedForPlayer);
   }

   public MagicComplexity resolvedResistanceComplexity() {
      return this.resistanceComplexity != null ? this.resistanceComplexity : MagicComplexity.infer(this.id, this.manaCost, this.category);
   }
}
