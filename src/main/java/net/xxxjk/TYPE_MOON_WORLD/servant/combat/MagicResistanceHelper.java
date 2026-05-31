package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Witch;
import net.xxxjk.TYPE_MOON_WORLD.entity.CyanWindFieldEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GanderProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MedeaBeamEffectEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MedeaMagicBoltEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.RubyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.SapphireProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.TopazProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;

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

      if (entity.getPersistentData().contains(MAGIC_RESISTANCE_LEVEL_TAG)) {
         return MagicResistanceRank.fromLevel(entity.getPersistentData().getInt(MAGIC_RESISTANCE_LEVEL_TAG));
      }

      if (entity instanceof ServantEntity servant && servant.getDefinition() != null) {
         if (servant.getDefinition().traits().contains(ServantTraitTag.MALE) && entity.getPersistentData().contains(MAGIC_RESISTANCE_LEVEL_TAG)) {
            return MagicResistanceRank.fromLevel(entity.getPersistentData().getInt(MAGIC_RESISTANCE_LEVEL_TAG));
         }
      }

      if (entity instanceof Witch || entity instanceof AbstractIllager) {
         return MagicResistanceRank.C;
      }

      return MagicResistanceRank.NONE;
   }

   public static float getDamageReduction(LivingEntity entity) {
      if (entity == null) {
         return 0.0F;
      }
      return Math.max(0.0F, entity.getPersistentData().getFloat(MAGIC_RESISTANCE_DAMAGE_REDUCTION_TAG));
   }

   public static float getDebuffResistance(LivingEntity entity) {
      if (entity == null) {
         return 0.0F;
      }
      return Math.max(0.0F, entity.getPersistentData().getFloat(MAGIC_RESISTANCE_DEBUFF_RESIST_TAG));
   }

   public static float applyMagicDamageReduction(LivingEntity entity, DamageSource source, float amount) {
      if (entity == null || source == null || amount <= 0.0F || !isMagicDamage(source)) {
         return amount;
      }
      return amount * (1.0F - getDamageReduction(entity));
   }

   public static int applyDebuffResistance(LivingEntity entity, int durationTicks) {
      if (entity == null || durationTicks <= 1) {
         return durationTicks;
      }
      float reduction = getDebuffResistance(entity);
      return Math.max(1, Math.round(durationTicks * (1.0F - reduction)));
   }

   public static boolean hasMagicResistance(LivingEntity entity) {
      return getMagicResistanceRank(entity) != MagicResistanceRank.NONE;
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

      var direct = source.getDirectEntity();
      return direct instanceof MedeaMagicBoltEntity
         || direct instanceof MedeaBeamEffectEntity
         || direct instanceof RubyProjectileEntity
         || direct instanceof SapphireProjectileEntity
         || direct instanceof TopazProjectileEntity
         || direct instanceof GanderProjectileEntity
         || direct instanceof CyanWindFieldEntity;
   }
}
