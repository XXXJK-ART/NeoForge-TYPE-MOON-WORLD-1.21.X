package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.xxxjk.TYPE_MOON_WORLD.servant.fanatic.FanaticDamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.api.MagicDefinitionRegistry;
import net.xxxjk.TYPE_MOON_WORLD.combat.OriginBulletHelper;
import net.xxxjk.TYPE_MOON_WORLD.entity.ArtoriaExcaliburBeamEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.CyanWindFieldEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GanderProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshEaBeamEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MagicBulletProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MedeaBeamEffectEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MedeaMagicBoltEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.RubyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.SapphireProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.TopazProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.DeadApostleEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.NeroChaosBeastLogic;
import net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.NeroChaosEntity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import net.xxxjk.typemoonworld.api.MagicComplexity;

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
      ServantDefinition definition = ServantIdentityHelper.definitionOf(entity);
      if (definition != null
         && "enkidu".equals(definition.id())
         && getMagicResistanceRank(entity) == MagicResistanceRank.A) {
         return 0.55F;
      }
      return damageReductionForRank(getMagicResistanceRank(entity));
   }

   public static float getDebuffResistance(LivingEntity entity) {
      if (entity == null) {
         return 0.0F;
      }
      float stored = Math.max(0.0F, entity.getPersistentData().getFloat(MAGIC_RESISTANCE_DEBUFF_RESIST_TAG));
      return stored > 0.0F ? stored : debuffResistanceForRank(getMagicResistanceRank(entity));
   }

   public static float applyMagicDamageReduction(LivingEntity entity, DamageSource source, float amount) {
      return applyMagicDamageReduction(entity, source, amount, ordinaryMagicComplexity(source));
   }

   public static float applyMagicDamageReduction(LivingEntity entity, DamageSource source, float amount, MagicComplexity complexity) {
      if (OriginBulletHelper.isOriginBulletDamage(source)) {
         return amount;
      }
      if (source != null && source.is(FanaticDamageTypes.BYPASSES_DEFENSES)) {
         return amount;
      }
      if (entity == null || source == null || amount <= 0.0F || !isMagicDamage(source) || complexity == null) {
         return amount;
      }
      MagicResistanceRank rank = getMagicResistanceRank(entity);
      if (cancelsOrdinaryMagic(rank, complexity)) {
         return 0.0F;
      }
      if (amount <= smallMagicImmunityThreshold(rank)) {
         return 0.0F;
      }
      float adjusted = amount <= smallMagicHalfThreshold(rank) ? amount * 0.5F : amount;
      return adjusted * (1.0F - getDamageReduction(entity, complexity));
   }

   public static float applyMagicDamageReduction(
      LivingEntity entity, DamageSource source, float amount, MagicComplexity complexity,
      LivingEntity caster, String magicId, double casterProficiency
   ) {
      if (OriginBulletHelper.isOriginBulletDamage(source)) {
         return amount;
      }
      if (source != null && source.is(FanaticDamageTypes.BYPASSES_DEFENSES)) {
         return amount;
      }
      if (entity == null || source == null || amount <= 0.0F || !isMagicDamage(source) || complexity == null) {
         return amount;
      }
      if (MagicProficiencyContestHelper.blocksTargetEffect(caster, entity, magicId, casterProficiency, complexity)) {
         return 0.0F;
      }
      return applyMagicDamageReduction(entity, source, amount, complexity);
   }

   public static int applyDebuffResistance(LivingEntity entity, int durationTicks) {
      if (entity == null || durationTicks <= 1) {
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
      if (entity == null || durationTicks <= 1) {
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
      if (entity == null || durationTicks <= 1) {
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
         case EX -> resolved == MagicComplexity.HIGH_THAUMATURGY ? 20 : 0;
         case A -> resolved.level() <= MagicComplexity.THREE_VERSE.level() ? 0 : 60;
         case B -> resolved.level() <= MagicComplexity.TWO_VERSE.level() ? 0 : 80;
         case C -> resolved.level() <= MagicComplexity.ONE_VERSE.level() ? 0 : 100;
         case D -> resolved == MagicComplexity.SIMPLE_ACTION ? 0 : 140;
         case E -> 200;
         default -> Integer.MAX_VALUE;
      };
   }

   public static boolean hasMagicResistance(LivingEntity entity) {
      return getMagicResistanceRank(entity) != MagicResistanceRank.NONE;
   }

   public static float applyNoblePhantasmMagicResistance(LivingEntity entity, float amount) {
      if (entity == null || amount <= 0.0F) {
         return amount;
      }
      return amount * (1.0F - getDamageReduction(entity));
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

   private static float smallMagicImmunityThreshold(MagicResistanceRank rank) {
      return switch (rank == null ? MagicResistanceRank.NONE : rank) {
         case A -> 12.0F;
         case B -> 8.0F;
         case C -> 5.0F;
         case D -> 3.0F;
         case EX -> 40.0F;
         case E -> 1.0F;
         default -> 0.0F;
      };
   }

   private static float smallMagicHalfThreshold(MagicResistanceRank rank) {
      return switch (rank == null ? MagicResistanceRank.NONE : rank) {
         case A -> 30.0F;
         case B -> 22.0F;
         case C -> 16.0F;
         case D -> 10.0F;
         case EX -> 80.0F;
         case E -> 6.0F;
         default -> 0.0F;
      };
   }

   public static boolean cancelsOrdinaryMagic(MagicResistanceRank rank, MagicComplexity complexity) {
      MagicResistanceRank resolvedRank = rank == null ? MagicResistanceRank.NONE : rank;
      MagicComplexity resolvedComplexity = complexity == null ? MagicComplexity.ONE_VERSE : complexity;
      return switch (resolvedRank) {
         case EX -> true;
         case A -> resolvedComplexity.level() <= MagicComplexity.THREE_VERSE.level();
         case B -> resolvedComplexity.level() <= MagicComplexity.TWO_VERSE.level();
         case C -> resolvedComplexity.level() <= MagicComplexity.ONE_VERSE.level();
         case D -> resolvedComplexity == MagicComplexity.SIMPLE_ACTION;
         default -> false;
      };
   }

   public static MagicComplexity complexityForMagic(String magicId) {
      if (magicId == null || magicId.isBlank()) {
         return MagicComplexity.ONE_VERSE;
      }
      var definition = MagicDefinitionRegistry.get(magicId);
      if (definition != null) {
         return definition.resolvedResistanceComplexity();
      }
      ResourceLocation id = magicId == null || magicId.indexOf(':') < 0
         ? ResourceLocation.fromNamespaceAndPath("typemoonworld", magicId == null ? "" : magicId)
         : ResourceLocation.tryParse(magicId);
      return MagicComplexity.infer(id, 0.0, null);
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
         if (rank.startsWith("e")) {
            return MagicResistanceRank.E;
         }
      }
      return MagicResistanceRank.NONE;
   }

   public static boolean isMagicDamage(DamageSource source) {
      if (source == null) {
         return false;
      }
      if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         return false;
      }
      if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)) {
         return true;
      }
      if (source.is(FanaticDamageTypes.MARROW)) {
         return true;
      }

      var direct = source.getDirectEntity();
      return direct instanceof MedeaMagicBoltEntity
         || direct instanceof MedeaBeamEffectEntity
         || direct instanceof RubyProjectileEntity
         || direct instanceof SapphireProjectileEntity
         || direct instanceof TopazProjectileEntity
         || direct instanceof MagicBulletProjectileEntity
         || direct instanceof GanderProjectileEntity
         || direct instanceof CyanWindFieldEntity
         || direct instanceof ArtoriaExcaliburBeamEntity
         || direct instanceof GilgameshEaBeamEntity;
   }

   private static MagicResistanceRank rankFromManaCapacity(LivingEntity entity) {
      if (!(entity instanceof Player) && !(entity instanceof MysticMagicianEntity)) {
         TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         return (vars.is_magus || vars.player_max_mana >= 100.0) ? rankFromManaCapacity(vars.player_max_mana) : MagicResistanceRank.NONE;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return (vars.is_magus || vars.player_max_mana >= 100.0) ? rankFromManaCapacity(vars.player_max_mana) : MagicResistanceRank.NONE;
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

   private static float getDamageReduction(LivingEntity entity, MagicComplexity complexity) {
      float base = getDamageReduction(entity);
      if (complexity == MagicComplexity.HIGH_THAUMATURGY) {
         return Math.min(0.95F, base * 0.7F);
      }
      if (complexity == MagicComplexity.THREE_VERSE) {
         return Math.min(0.95F, base * 0.85F);
      }
      return base;
   }

   private static MagicComplexity ordinaryMagicComplexity(DamageSource source) {
      if (source == null || OriginBulletHelper.isOriginBulletDamage(source)) {
         return null;
      }
      var direct = source.getDirectEntity();
      if (direct instanceof ArtoriaExcaliburBeamEntity || direct instanceof GilgameshEaBeamEntity) {
         return null;
      }
      if (direct instanceof GanderProjectileEntity || direct instanceof MagicBulletProjectileEntity
         || direct instanceof RubyProjectileEntity || direct instanceof SapphireProjectileEntity
         || direct instanceof TopazProjectileEntity) {
         return MagicComplexity.SIMPLE_ACTION;
      }
      if (direct instanceof CyanWindFieldEntity || direct instanceof MedeaMagicBoltEntity) {
         return MagicComplexity.TWO_VERSE;
      }
      if (direct instanceof MedeaBeamEffectEntity) {
         return MagicComplexity.HIGH_THAUMATURGY;
      }
      if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC) || source.is(FanaticDamageTypes.MARROW)) {
         return MagicComplexity.ONE_VERSE;
      }
      return null;
   }
}
