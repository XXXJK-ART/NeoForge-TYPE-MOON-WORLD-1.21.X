package net.xxxjk.typemoonworld.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/** Static metadata for a magic executor. JSON loaders may produce the same shape. */
public record MagicDefinitionData(
   ResourceLocation id,
   String nameKey,
   ResourceLocation category,
   ResourceLocation school,
   double manaCost,
   int cooldownTicks,
   boolean learnable,
   boolean wheelSelectable,
   boolean crestAllowed,
   boolean npcAllowed,
   boolean knowledgeOnly,
   int npcGlobalCooldown,
   int npcCooldown,
   List<ResourceLocation> requiredAttributes,
   MagicComplexity resistanceComplexity
) {
   private static final ResourceLocation INVALID_ID = ResourceLocation.fromNamespaceAndPath("typemoonworld", "invalid");
   private static final ResourceLocation BASIC = ResourceLocation.fromNamespaceAndPath("typemoonworld", "basic");
   private static final ResourceLocation NONE = ResourceLocation.fromNamespaceAndPath("typemoonworld", "none");
   public static final Codec<MagicDefinitionData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      ResourceLocation.CODEC.optionalFieldOf("id", INVALID_ID).forGetter(MagicDefinitionData::id),
      Codec.STRING.optionalFieldOf("name_key", "").forGetter(MagicDefinitionData::nameKey),
      ResourceLocation.CODEC.optionalFieldOf("category", BASIC).forGetter(MagicDefinitionData::category),
      ResourceLocation.CODEC.optionalFieldOf("school", NONE).forGetter(MagicDefinitionData::school),
      Codec.DOUBLE.optionalFieldOf("mana_cost", 0.0).forGetter(MagicDefinitionData::manaCost),
      Codec.INT.optionalFieldOf("cooldown_ticks", 10).forGetter(MagicDefinitionData::cooldownTicks),
      Codec.BOOL.optionalFieldOf("learnable", true).forGetter(MagicDefinitionData::learnable),
      Codec.BOOL.optionalFieldOf("wheel_selectable", true).forGetter(MagicDefinitionData::wheelSelectable),
      Codec.BOOL.optionalFieldOf("crest_allowed", true).forGetter(MagicDefinitionData::crestAllowed),
      Codec.BOOL.optionalFieldOf("npc_allowed", true).forGetter(MagicDefinitionData::npcAllowed),
      Codec.BOOL.optionalFieldOf("knowledge_only", false).forGetter(MagicDefinitionData::knowledgeOnly),
      Codec.INT.optionalFieldOf("npc_global_cooldown", 12).forGetter(MagicDefinitionData::npcGlobalCooldown),
      Codec.INT.optionalFieldOf("npc_cooldown", 20).forGetter(MagicDefinitionData::npcCooldown),
      ResourceLocation.CODEC.listOf().optionalFieldOf("required_attributes", List.of()).forGetter(MagicDefinitionData::requiredAttributes),
      MagicComplexity.CODEC.optionalFieldOf("resistance_complexity").forGetter(data -> Optional.ofNullable(data.resistanceComplexity))
   ).apply(instance, (id, nameKey, category, school, manaCost, cooldownTicks, learnable, wheelSelectable, crestAllowed,
      npcAllowed, knowledgeOnly, npcGlobalCooldown, npcCooldown, requiredAttributes, resistanceComplexity) ->
      new MagicDefinitionData(id, nameKey, category, school, manaCost, cooldownTicks, learnable, wheelSelectable,
         crestAllowed, npcAllowed, knowledgeOnly, npcGlobalCooldown, npcCooldown, requiredAttributes,
         resistanceComplexity.orElse(null))));

   public MagicDefinitionData {
      if (id == null) throw new IllegalArgumentException("id");
      nameKey = nameKey == null ? "" : nameKey;
      category = category == null ? ResourceLocation.fromNamespaceAndPath("typemoonworld", "basic") : category;
      school = school == null ? ResourceLocation.fromNamespaceAndPath("typemoonworld", "none") : school;
      manaCost = Math.max(0.0, manaCost);
      cooldownTicks = Math.max(0, cooldownTicks);
      npcGlobalCooldown = Math.max(0, npcGlobalCooldown);
      npcCooldown = Math.max(0, npcCooldown);
      requiredAttributes = requiredAttributes == null ? List.of() : requiredAttributes.stream()
         .filter(Objects::nonNull).distinct().limit(32).toList();
   }

   public MagicDefinitionData(ResourceLocation id, String nameKey, ResourceLocation category, ResourceLocation school,
         double manaCost, int cooldownTicks, boolean learnable, boolean wheelSelectable, boolean crestAllowed,
         boolean npcAllowed, boolean knowledgeOnly, int npcGlobalCooldown, int npcCooldown,
         List<ResourceLocation> requiredAttributes) {
      this(id, nameKey, category, school, manaCost, cooldownTicks, learnable, wheelSelectable, crestAllowed,
         npcAllowed, knowledgeOnly, npcGlobalCooldown, npcCooldown, requiredAttributes, null);
   }

   /** Source-compatible constructor for v1 addons that do not declare attribute requirements. */
   public MagicDefinitionData(ResourceLocation id, String nameKey, ResourceLocation category, ResourceLocation school,
         double manaCost, int cooldownTicks, boolean learnable, boolean wheelSelectable, boolean crestAllowed,
         boolean npcAllowed, boolean knowledgeOnly, int npcGlobalCooldown, int npcCooldown) {
      this(id, nameKey, category, school, manaCost, cooldownTicks, learnable, wheelSelectable, crestAllowed,
         npcAllowed, knowledgeOnly, npcGlobalCooldown, npcCooldown, List.of(), null);
   }

   public static MagicDefinitionData defaults(ResourceLocation id) {
      return new MagicDefinitionData(id, "magic." + id.getNamespace() + "." + id.getPath() + ".name",
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "basic"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "none"), 0.0, 10,
         true, true, true, true, false, 12, 20, List.of(), null);
   }

   public MagicDefinitionData withId(ResourceLocation newId) {
      String resolvedKey = this.nameKey.isBlank() ? "magic." + newId.getNamespace() + "." + newId.getPath() + ".name" : this.nameKey;
      return new MagicDefinitionData(newId, resolvedKey, this.category, this.school, this.manaCost, this.cooldownTicks,
         this.learnable, this.wheelSelectable, this.crestAllowed, this.npcAllowed, this.knowledgeOnly,
         this.npcGlobalCooldown, this.npcCooldown, this.requiredAttributes, this.resistanceComplexity);
   }

   public MagicComplexity resolvedResistanceComplexity() {
      return this.resistanceComplexity != null ? this.resistanceComplexity : MagicComplexity.infer(this.id, this.manaCost, this.category);
   }
}
