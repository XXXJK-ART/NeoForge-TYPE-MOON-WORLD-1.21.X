package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.xxxjk.TYPE_MOON_WORLD.servant.fanatic.FanaticDamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.DeadApostleEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.NeroChaosBeastLogic;
import net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.NeroChaosEntity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.typemoonworld.api.MagicComplexity;
import net.xxxjk.typemoonworld.api.MagecraftDamageSource;

public final class MagicResistanceHelper {
   public static final String MAGIC_RESISTANCE_LEVEL_TAG = "MagicResistanceLevel";
   public static final String MAGIC_RESISTANCE_DAMAGE_REDUCTION_TAG = "MagicResistanceDamageReduction";
   public static final String MAGIC_RESISTANCE_DEBUFF_RESIST_TAG = "MagicResistanceDebuffResistance";

   private MagicResistanceHelper() {
   }

   public static void setMagicResistance(LivingEntity entity, MagicResistanceRank rank, float damageReduction, float debuffResistance) {
      if (entity == null) {
         return;
      }

      CompoundTag data = entity.getPersistentData();
      data.putInt(MAGIC_RESISTANCE_LEVEL_TAG, rank == null ? MagicResistanceRank.NONE.level() : rank.level());
      data.putFloat(MAGIC_RESISTANCE_DAMAGE_REDUCTION_TAG, Math.max(0.0F, damageReduction));
      data.putFloat(MAGIC_RESISTANCE_DEBUFF_RESIST_TAG, Math.max(0.0F, debuffResistance));
   }

   public static MagicResistanceRank getMagicResistanceRank(LivingEntity entity) {
      if (entity == null) {
         return MagicResistanceRank.NONE;
      }

      ServantDefinition definition = ServantIdentityHelper.definitionOf(entity);
      if (definition != null) {
         MagicResistanceRank definitionRank = getDefinitionMagicResistanceRank(definition);
         if (definitionRank != MagicResistanceRank.NONE) {
            return definitionRank;
         }
         if (entity.getPersistentData().contains(MAGIC_RESISTANCE_LEVEL_TAG)) {
            return MagicResistanceRank.fromLevel(entity.getPersistentData().getInt(MAGIC_RESISTANCE_LEVEL_TAG));
         }
      }

      if (entity instanceof NeroChaosEntity || NeroChaosBeastLogic.isBeast(entity)) {
         return MagicResistanceRank.A;
      }
      if (entity instanceof DeadApostleEntity) {
         return MagicResistanceRank.E;
      }

      MagicResistanceRank manaRank = rankFromManaCapacity(entity);
      if (manaRank != MagicResistanceRank.NONE) {
         return manaRank;
      }

      if (entity instanceof Witch || entity instanceof AbstractIllager) {
         return MagicResistanceRank.C;
      }

      if (entity.getPersistentData().contains(MAGIC_RESISTANCE_LEVEL_TAG)) {
         return MagicResistanceRank.fromLevel(entity.getPersistentData().getInt(MAGIC_RESISTANCE_LEVEL_TAG));
      }

      return MagicResistanceRank.NONE;
   }

   public static float getDamageReduction(LivingEntity entity) {
      if (entity == null) {
         return 0.0F;
      }
      float stored = Math.max(0.0F, entity.getPersistentData().getFloat(MAGIC_RESISTANCE_DAMAGE_REDUCTION_TAG));
      if (stored > 0.0F) {
         return stored;
      }
      return damageReductionForDefinition(ServantIdentityHelper.definitionOf(entity), getMagicResistanceRank(entity));
   }

   public static float damageReductionForDefinition(ServantDefinition definition, MagicResistanceRank rank) {
      float fallback = damageReductionForRank(rank);
      if (definition != null
         && "enkidu".equals(definition.id())
         && rank == MagicResistanceRank.A) {
         return 0.55F;
      }
      if (definition != null) {
         for (String skillId : definition.skillIds()) {
            if (rankFromSkillId(skillId) != rank) continue;
            var skill = net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantSkillDataRegistry.get(skillId);
            if (skill == null) continue;
            for (var effect : skill.effects()) {
               if ("magic_resistance".equals(effect.effectType()) && effect.params().containsKey("damage_reduction")) {
                  return Math.clamp(effect.params().get("damage_reduction").asFloat(fallback), 0.0F, 1.0F);
               }
            }
         }
      }
      return fallback;
   }

   public static float getDebuffResistance(LivingEntity entity) {
      if (entity == null) {
         return 0.0F;
      }
      float stored = Math.max(0.0F, entity.getPersistentData().getFloat(MAGIC_RESISTANCE_DEBUFF_RESIST_TAG));
      return stored > 0.0F ? stored : debuffResistanceForRank(getMagicResistanceRank(entity));
   }

   public static float applyMagicDamageReduction(LivingEntity entity, DamageSource source, float amount) {
      MagecraftDamageSource magecraft = MagecraftDamageSource.find(source);
      return magecraft == null ? amount : applyMagicDamageReduction(entity, source, amount, magecraft.complexity());
   }

   public static float applyMagicDamageReduction(LivingEntity entity, DamageSource source, float amount, MagicComplexity complexity) {
      if (net.xxxjk.TYPE_MOON_WORLD.combat.OriginBulletHelper.isOriginBulletDamage(source)) return amount;
      if (source != null && source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return amount;
      if (source != null && source.is(FanaticDamageTypes.BYPASSES_DEFENSES)) {
         return amount;
      }
      if (entity == null || MagecraftDamageSource.find(source) == null || amount <= 0.0F || complexity == null) {
         return amount;
      }
      MagecraftDamageSource magecraft = MagecraftDamageSource.find(source);
      if (NoblePhantasmDamageClassifier.isNoblePhantasmDamage(source, amount) && !magecraft.isNoblePhantasmMagic()) return amount;
      if (Double.isFinite(magecraft.casterProficiency()) && MagicProficiencyContestHelper.blocksTargetEffect(
         source.getEntity() instanceof LivingEntity caster ? caster : null, entity, magecraft.magicId(), magecraft.casterProficiency(), complexity)) return 0.0F;
      MagicResistanceRank rank = getMagicResistanceRank(entity);
      if (cancelsOrdinaryMagic(rank, complexity)) {
         return 0.0F;
      }
      return amount * (1.0F - getDamageReduction(entity));
   }

   public static float applyMagicDamageReduction(
      LivingEntity entity, DamageSource source, float amount, MagicComplexity complexity,
      LivingEntity caster, String magicId, double casterProficiency
   ) {
      if (source != null && source.is(FanaticDamageTypes.BYPASSES_DEFENSES)) {
         return amount;
      }
      if (entity == null || source == null || !isMagicDamage(source) || amount <= 0.0F || complexity == null) {
         return amount;
      }
      if (MagicProficiencyContestHelper.blocksTargetEffect(caster, entity, magicId, casterProficiency, complexity)) {
         return 0.0F;
      }
      return applyMagicDamageReduction(entity, source, amount, complexity);
   }

   public static int applyDebuffResistance(LivingEntity entity, int durationTicks) {
      if (entity == null || durationTicks <= 0) {
         return durationTicks;
      }
      float reduction = getDebuffResistance(entity);
      return Math.max(1, Math.round(durationTicks * (1.0F - reduction)));
   }

   /**
    * Applies Fate-style resistance to low-mystery harmful magic state. Damage is
    * intentionally handled by {@link #applyMagicDamageReduction}; this method is
    * only for negative status duration.
    */
   public static int applyHarmfulMagicEffectResistance(LivingEntity entity, int durationTicks) {
      return applyHarmfulMagicEffectResistance(entity, durationTicks, MagicComplexity.ONE_VERSE);
   }

   public static int applyHarmfulMagicEffectResistance(LivingEntity entity, int durationTicks, MagicComplexity complexity) {
      if (entity == null || durationTicks <= 0) {
         return durationTicks;
      }
      MagicResistanceRank rank = getMagicResistanceRank(entity);
      MagicComplexity resolved = complexity == null ? MagicComplexity.ONE_VERSE : complexity;
      if (cancelsOrdinaryMagic(rank, resolved)) {
         return 0;
      }
      int adjusted = applyDebuffResistance(entity, durationTicks);
      int cap = harmfulMagicEffectDurationCap(rank, resolved);
      return cap <= 0 ? adjusted : Math.max(1, Math.min(adjusted, cap));
   }

   public static int applyHarmfulMagicEffectResistance(
      LivingEntity entity, int durationTicks, MagicComplexity complexity,
      LivingEntity caster, String magicId, double casterProficiency
   ) {
      if (entity == null || durationTicks <= 0) {
         return durationTicks;
      }
      MagicComplexity resolved = complexity == null ? MagicComplexity.ONE_VERSE : complexity;
      if (MagicProficiencyContestHelper.blocksTargetEffect(caster, entity, magicId, casterProficiency, resolved)) {
         return 0;
      }
      return applyHarmfulMagicEffectResistance(entity, durationTicks, resolved);
   }

   public static boolean blocksHarmfulMagicEffect(LivingEntity entity) {
      return blocksHarmfulMagicEffect(entity, MagicComplexity.ONE_VERSE);
   }

   public static boolean blocksHarmfulMagicEffect(LivingEntity entity, MagicComplexity complexity) {
      return cancelsOrdinaryMagic(getMagicResistanceRank(entity), complexity == null ? MagicComplexity.ONE_VERSE : complexity);
   }

   public static int harmfulMagicEffectDurationCap(MagicResistanceRank rank) {
      return harmfulMagicEffectDurationCap(rank, MagicComplexity.ONE_VERSE);
   }

   public static int harmfulMagicEffectDurationCap(MagicResistanceRank rank, MagicComplexity complexity) {
      MagicComplexity resolved = complexity == null ? MagicComplexity.ONE_VERSE : complexity;
      return switch (rank == null ? MagicResistanceRank.NONE : rank) {
         case EX, A -> 0;
         case B -> resolved.level() <= MagicComplexity.THREE_VERSE.level() ? 0 : 80;
         case C -> resolved.level() <= MagicComplexity.TWO_VERSE.level() ? 0 : 100;
         case D -> resolved.level() <= MagicComplexity.ONE_VERSE.level() ? 0 : 140;
         case E -> resolved == MagicComplexity.SIMPLE_ACTION ? 0 : 200;
         default -> Integer.MAX_VALUE;
      };
   }

   public static boolean hasMagicResistance(LivingEntity entity) {
      return getMagicResistanceRank(entity) != MagicResistanceRank.NONE;
   }

   public static float applyNoblePhantasmMagicResistance(LivingEntity entity, float amount) {
      // Ordinary Noble Phantasms are not magecraft and do not use Magic Resistance.
      return amount;
   }

   public static float applyNoblePhantasmMagicResistance(LivingEntity entity, DamageSource source, float amount) {
      MagecraftDamageSource magecraft = MagecraftDamageSource.find(source);
      return magecraft != null && magecraft.isNoblePhantasmMagic() ? applyMagicDamageReduction(entity, source, amount) : amount;
   }

   public static float damageReductionForRank(MagicResistanceRank rank) {
      return switch (rank == null ? MagicResistanceRank.NONE : rank) {
         case EX -> 0.85F;
         case A -> 0.45F;
         case B -> 0.35F;
         case C -> 0.22F;
         case D -> 0.12F;
         case E -> 0.06F;
         default -> 0.0F;
      };
   }

   public static float debuffResistanceForRank(MagicResistanceRank rank) {
      return switch (rank == null ? MagicResistanceRank.NONE : rank) {
         case EX -> 0.90F;
         case A -> 0.75F;
         case B -> 0.55F;
         case C -> 0.35F;
         case D -> 0.15F;
         case E -> 0.06F;
         default -> 0.0F;
      };
   }

   public static boolean cancelsOrdinaryMagic(MagicResistanceRank rank, MagicComplexity complexity) {
      MagicResistanceRank resolvedRank = rank == null ? MagicResistanceRank.NONE : rank;
      MagicComplexity resolvedComplexity = complexity == null ? MagicComplexity.ONE_VERSE : complexity;
      return switch (resolvedRank) {
         case EX -> true;
         case A -> resolvedComplexity.level() <= MagicComplexity.HIGH_THAUMATURGY.level();
         case B -> resolvedComplexity.level() <= MagicComplexity.THREE_VERSE.level();
         case C -> resolvedComplexity.level() <= MagicComplexity.TWO_VERSE.level();
         case D -> resolvedComplexity.level() <= MagicComplexity.ONE_VERSE.level();
         case E -> resolvedComplexity == MagicComplexity.SIMPLE_ACTION;
         default -> false;
      };
   }

   public static MagicComplexity complexityForMagic(String magicId) {
      if (magicId == null || magicId.isBlank()) {
         return MagicComplexity.ONE_VERSE;
      }
      return MagecraftDamageSource.resolveComplexity(magicId);
   }

   private static MagicResistanceRank getDefinitionMagicResistanceRank(ServantDefinition definition) {
      if (definition == null || definition.skillIds() == null) {
         return MagicResistanceRank.NONE;
      }
      MagicResistanceRank best = MagicResistanceRank.NONE;
      for (String skillId : definition.skillIds()) {
         MagicResistanceRank rank = rankFromSkillId(skillId);
         if (rank.isAtLeast(best)) {
            best = rank;
         }
      }
      return best;
   }

   private static MagicResistanceRank rankFromSkillId(String skillId) {
      if (skillId == null) {
         return MagicResistanceRank.NONE;
      }
      String normalized = skillId.trim().toLowerCase();
      if (normalized.startsWith("magic_resistance_")) {
         String rank = normalized.substring("magic_resistance_".length());
         if (rank.startsWith("a")) {
            return MagicResistanceRank.A;
         }
         if (rank.startsWith("b")) {
            return MagicResistanceRank.B;
         }
         if (rank.startsWith("c")) {
            return MagicResistanceRank.C;
         }
         if (rank.startsWith("d")) {
            return MagicResistanceRank.D;
         }
         if (rank.startsWith("ex")) {
            return MagicResistanceRank.EX;
         }
         if (rank.startsWith("e")) {
            return MagicResistanceRank.E;
         }
      }
      return MagicResistanceRank.NONE;
   }

   public static boolean isMagicDamage(DamageSource source) {
      return MagecraftDamageSource.find(source) != null;
   }

   private static MagicResistanceRank rankFromManaCapacity(LivingEntity entity) {
      if (!(entity instanceof Player) && !(entity instanceof MysticMagicianEntity)) {
         TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         return (vars.is_magus || vars.magicState().maxMana() >= 100.0) ? rankFromManaCapacity(vars.magicState().maxMana()) : MagicResistanceRank.NONE;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return (vars.is_magus || vars.magicState().maxMana() >= 100.0) ? rankFromManaCapacity(vars.magicState().maxMana()) : MagicResistanceRank.NONE;
   }

   public static MagicResistanceRank rankFromManaCapacity(double maxMana) {
      if (maxMana > 5000.0) return MagicResistanceRank.EX;
      if (maxMana >= 5000.0) return MagicResistanceRank.A;
      if (maxMana >= 3000.0) return MagicResistanceRank.B;
      if (maxMana >= 1000.0) return MagicResistanceRank.C;
      if (maxMana >= 500.0) return MagicResistanceRank.D;
      if (maxMana >= 100.0) return MagicResistanceRank.E;
      return MagicResistanceRank.NONE;
   }

}
