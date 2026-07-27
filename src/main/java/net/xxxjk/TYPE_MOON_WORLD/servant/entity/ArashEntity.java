package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceHelper;
import org.jetbrains.annotations.Nullable;

public final class ArashEntity extends ServantEntity {
   public static final String SERVANT_KEY = "arash";
   public static final String TAG_STELLA_USED = "ArashStellaUsed";
   public static final String TAG_STELLA_CHANTING = "ArashStellaChanting";
   public static final String TAG_STELLA_SACRIFICE = "ArashStellaSacrifice";
   public static final String TAG_STELLA_SACRIFICE_TICKS = "ArashStellaSacrificeTicks";
   public static final String TAG_STELLA_SACRIFICE_HEALTH = "ArashStellaSacrificeHealth";
   private static final ResourceLocation STOUT_HEALTH_ID = ResourceLocation.fromNamespaceAndPath(
      TYPE_MOON_WORLD.MOD_ID, "arash_stout_ex_health");

   public ArashEntity(EntityType<? extends ArashEntity> type, Level level) {
      super(type, level, SERVANT_KEY);
   }

   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                       @Nullable SpawnGroupData groupData) {
      SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, groupData);
      applyStoutHealth(true);
      return result;
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      applyStoutHealth(false);
   }

   private void applyStoutHealth(boolean heal) {
      var health = this.getAttribute(Attributes.MAX_HEALTH);
      if (health == null) return;
      float previous = this.getHealth();
      AttributeModifier existing = health.getModifier(STOUT_HEALTH_ID);
      if (existing == null || existing.amount() != 100.0 || existing.operation() != AttributeModifier.Operation.ADD_VALUE) {
         if (existing != null) health.removeModifier(STOUT_HEALTH_ID);
         health.addPermanentModifier(new AttributeModifier(STOUT_HEALTH_ID, 100.0, AttributeModifier.Operation.ADD_VALUE));
      }
      this.setHealth(heal ? (float)ArashCombatRules.MAX_HEALTH : Math.min(previous, (float)ArashCombatRules.MAX_HEALTH));
      this.getPersistentData().putBoolean("StoutExArashActive", true);
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      if (amount <= 0.0F) return false;
      if (isDiseaseDamage(source)) return false;
      if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         if (!this.getPersistentData().getBoolean(TAG_STELLA_CHANTING)
            && this.getRandom().nextFloat() < ArashCombatRules.FUTURE_SIGHT_DODGE_CHANCE) {
            if (this.level() instanceof ServerLevel level) {
               level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                  this.getX(), this.getY() + 1.0, this.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
            }
            return false;
         }
         amount *= ArashCombatRules.STOUT_DAMAGE_MULTIPLIER;
      }
      return super.hurt(source, amount);
   }

   private static boolean isDiseaseDamage(DamageSource source) {
      return source.typeHolder().unwrapKey().map(key -> {
         String path = key.location().getPath();
         return path.contains("infection") || path.contains("toxin") || path.contains("poison")
            || path.contains("wither") || path.contains("wound");
      }).orElse(false);
   }

   @Override
   public boolean canBeAffected(MobEffectInstance effect) {
      Holder<MobEffect> type = effect.getEffect();
      if (type == MobEffects.POISON || type == MobEffects.WITHER || type == MobEffects.WEAKNESS
         || type == MobEffects.MOVEMENT_SLOWDOWN || type == MobEffects.DIG_SLOWDOWN
         || type == MobEffects.HUNGER || type == MobEffects.CONFUSION
         || type == ModMobEffects.PALE_RIDER_INFECTION || type == ModMobEffects.PALE_RIDER_FEAR
         || type == ModMobEffects.FANATIC_TOXIN || type == ModMobEffects.FANATIC_WOUNDED
         || type == ModMobEffects.FANATIC_CIRCUIT_DISRUPTION) {
         return false;
      }
      return super.canBeAffected(effect);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide && this.tickCount == 1) {
         applyStoutHealth(false);
         MagicResistanceHelper.setMagicResistance(this, net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceRank.C, 0.20F, 0.10F);
      }
      if (!this.level().isClientSide) tickStellaSacrifice();
   }

   public void beginStellaSacrifice() {
      CompoundTag data = this.getPersistentData();
      data.putBoolean(TAG_STELLA_SACRIFICE, true);
      data.putInt(TAG_STELLA_SACRIFICE_TICKS, 0);
      data.putFloat(TAG_STELLA_SACRIFICE_HEALTH, Math.max(1.0F, this.getHealth()));
      this.getNavigation().stop();
      this.setTarget(null);
   }

   private void tickStellaSacrifice() {
      CompoundTag data = this.getPersistentData();
      if (!data.getBoolean(TAG_STELLA_SACRIFICE) || !this.isAlive()) return;
      int elapsed = data.getInt(TAG_STELLA_SACRIFICE_TICKS) + 1;
      data.putInt(TAG_STELLA_SACRIFICE_TICKS, elapsed);
      float initialHealth = Math.max(1.0F, data.getFloat(TAG_STELLA_SACRIFICE_HEALTH));
      if (elapsed >= ArashCombatRules.STELLA_SACRIFICE_TICKS) {
         this.kill();
         return;
      }
      this.setHealth(Math.min(this.getHealth(), ArashCombatRules.stellaRemainingHealth(initialHealth, elapsed)));
      if (this.level() instanceof ServerLevel level && elapsed % 3 == 0) {
         level.sendParticles(elapsed < 120 ? net.minecraft.core.particles.ParticleTypes.END_ROD
               : net.minecraft.core.particles.ParticleTypes.FIREWORK,
            this.getX(), this.getY() + this.getBbHeight() * 0.55, this.getZ(),
            8 + elapsed / 12, 0.42, this.getBbHeight() * 0.5, 0.42, 0.035);
      }
   }
}
