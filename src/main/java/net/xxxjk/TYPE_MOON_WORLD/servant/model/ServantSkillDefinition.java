package net.xxxjk.TYPE_MOON_WORLD.servant.model;

import java.util.List;
import java.util.Map;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ServantSkillDefinition(
   String id,
   String displayName,
   String displayNameZh,
   SkillType type,
   int mpCost,
   int cooldownTicks,
   int durationTicks,
   List<SkillEffectEntry> effects,
   AiProfile ai
) {
   public static final Codec<ServantSkillDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.STRING.optionalFieldOf("id", "").forGetter(ServantSkillDefinition::id),
      Codec.STRING.optionalFieldOf("display_name", "").forGetter(ServantSkillDefinition::displayName),
      Codec.STRING.optionalFieldOf("display_name_zh", "").forGetter(ServantSkillDefinition::displayNameZh),
      Codec.STRING.xmap(SkillType::fromKey, value -> value.name().toLowerCase()).optionalFieldOf("type", SkillType.ACTIVE).forGetter(ServantSkillDefinition::type),
      Codec.INT.optionalFieldOf("mp_cost", 0).forGetter(ServantSkillDefinition::mpCost),
      Codec.INT.optionalFieldOf("cooldown_ticks", 0).forGetter(ServantSkillDefinition::cooldownTicks),
      Codec.INT.optionalFieldOf("duration_ticks", 0).forGetter(ServantSkillDefinition::durationTicks),
      SkillEffectEntry.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(ServantSkillDefinition::effects),
      AiProfile.CODEC.optionalFieldOf("ai", AiProfile.NONE).forGetter(ServantSkillDefinition::ai)
   ).apply(instance, ServantSkillDefinition::new));

   public ServantSkillDefinition(String id, String displayName, String displayNameZh, SkillType type,
                                 int mpCost, int cooldownTicks, int durationTicks, List<SkillEffectEntry> effects) {
      this(id, displayName, displayNameZh, type, mpCost, cooldownTicks, durationTicks, effects, AiProfile.NONE);
   }

   public ServantSkillDefinition {
      id = id == null ? "" : id;
      displayName = displayName == null ? "" : displayName;
      displayNameZh = displayNameZh == null ? "" : displayNameZh;
      type = type == null ? SkillType.ACTIVE : type;
      mpCost = Math.max(0, mpCost);
      cooldownTicks = Math.max(0, cooldownTicks);
      durationTicks = Math.max(0, durationTicks);
      effects = effects == null ? List.of() : List.copyOf(effects);
      ai = ai == null ? AiProfile.NONE : ai;
   }

   public record AiProfile(List<CombatFact> facts) {
      public static final AiProfile NONE = new AiProfile(List.of());
      public static final Codec<AiProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
         CombatFact.CODEC.listOf().optionalFieldOf("facts", List.of()).forGetter(AiProfile::facts)
      ).apply(instance, AiProfile::new));

      public AiProfile {
         facts = facts == null ? List.of() : List.copyOf(facts);
      }
   }

   public record CombatFact(FactType type, double strength, List<FactCondition> conditions,
                            List<FactBypass> bypassedBy) {
      public static final Codec<CombatFact> CODEC = RecordCodecBuilder.create(instance -> instance.group(
         Codec.STRING.xmap(FactType::fromKey, FactType::key).fieldOf("type").forGetter(CombatFact::type),
         Codec.DOUBLE.optionalFieldOf("strength", 1.0).forGetter(CombatFact::strength),
         Codec.STRING.xmap(FactCondition::fromKey, FactCondition::key).listOf()
            .optionalFieldOf("requires", List.of()).forGetter(CombatFact::conditions),
         Codec.STRING.xmap(FactBypass::fromKey, FactBypass::key).listOf()
            .optionalFieldOf("bypassed_by", List.of()).forGetter(CombatFact::bypassedBy)
      ).apply(instance, CombatFact::new));

      public CombatFact {
         type = type == null ? FactType.UNKNOWN : type;
         strength = Math.max(0.0, Math.min(1.0, strength));
         conditions = conditions == null ? List.of() : List.copyOf(conditions);
         bypassedBy = bypassedBy == null ? List.of() : List.copyOf(bypassedBy);
      }
   }

   public enum FactType {
      UNKNOWN,
      MELEE_PRESSURE,
      PROJECTILE_PRESSURE,
      MAGIC_PRESSURE,
      AREA_CONTROL,
      CONTROL,
      GAP_CLOSE,
      PURSUIT,
      HEAL,
      PREDICTION,
      PROJECTILE_NEGATION,
      MAGIC_RESISTANCE,
      SHIELD,
      REVIVE,
      REGENERATION,
      DAMAGE_THRESHOLD,
      ADAPTIVE_DEFENSE,
      CONCEALMENT,
      ANTI_DIVINE,
      ANTI_MYSTERY;

      public static FactType fromKey(String key) {
         if (key == null || key.isBlank()) return UNKNOWN;
         try {
            return valueOf(key.trim().toUpperCase(java.util.Locale.ROOT));
         } catch (IllegalArgumentException ignored) {
            return UNKNOWN;
         }
      }

      public String key() {
         return name().toLowerCase(java.util.Locale.ROOT);
      }
   }

   public enum FactCondition {
      UNKNOWN,
      MOBILE,
      NOT_SILENCED,
      LOW_HEALTH,
      DAYLIGHT,
      ON_GROUND,
      HAS_MANA;

      public static FactCondition fromKey(String key) {
         if (key == null || key.isBlank()) return UNKNOWN;
         try {
            return valueOf(key.trim().toUpperCase(java.util.Locale.ROOT));
         } catch (IllegalArgumentException ignored) {
            return UNKNOWN;
         }
      }

      public String key() {
         return name().toLowerCase(java.util.Locale.ROOT);
      }
   }

   public enum FactBypass {
      UNKNOWN,
      IMMOBILIZE,
      STUN,
      FREEZE,
      EXPLOSION,
      PIERCING,
      SURE_HIT,
      RULE_BREAKER,
      ANTI_DIVINE,
      ANTI_MYSTERY;

      public static FactBypass fromKey(String key) {
         if (key == null || key.isBlank()) return UNKNOWN;
         try {
            return valueOf(key.trim().toUpperCase(java.util.Locale.ROOT));
         } catch (IllegalArgumentException ignored) {
            return UNKNOWN;
         }
      }

      public String key() {
         return name().toLowerCase(java.util.Locale.ROOT);
      }
   }
   public enum SkillType {
      ACTIVE,
      PASSIVE;

      public static SkillType fromKey(String key) {
         if (key == null) {
            return ACTIVE;
         }

         return switch (key.toLowerCase()) {
            case "passive" -> PASSIVE;
            default -> ACTIVE;
         };
      }
   }
}
