package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.xxxjk.TYPE_MOON_WORLD.magic.npc.NpcCombatPersonality;
import net.xxxjk.TYPE_MOON_WORLD.magic.npc.NpcCombatStyle;
import net.xxxjk.TYPE_MOON_WORLD.magic.npc.NpcCombatTemperament;
import net.xxxjk.TYPE_MOON_WORLD.magic.npc.NpcMagicCastBridge;
import org.jetbrains.annotations.Nullable;

public class LeffLaynorFlaurosEntity extends MysticMagicianEntity {
   public static final double MAX_HEALTH = 40.0;
   public static final double FIXED_SCALE = 0.92;
   public static final String TAG_PERSONALITY = "TypeMoonLeffPersonality";
   public static final String TAG_IMAGINARY_DISPLACEMENT_ACTIVE = "TypeMoonLeffImaginaryDisplacementActive";
   public static final String TAG_IMAGINARY_DISPLACEMENT_COOLDOWN = "TypeMoonLeffImaginaryDisplacementCooldown";
   private static final String TAG_HEALTH_MIGRATED = "TypeMoonLeffHealthV2";

   public LeffLaynorFlaurosEntity(EntityType<? extends net.minecraft.world.entity.PathfinderMob> type, Level level) {
      super(type, level);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return MysticMagicianEntity.createAttributes()
         .add(Attributes.MAX_HEALTH, MAX_HEALTH)
         .add(Attributes.MOVEMENT_SPEED, 0.28)
         .add(Attributes.ATTACK_DAMAGE, 5.0)
         .add(Attributes.ARMOR, 8.0)
         .add(Attributes.FOLLOW_RANGE, 48.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.25)
         .add(Attributes.SCALE, FIXED_SCALE);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide()) {
         NpcScaleHelper.ensureFixedScale(this, FIXED_SCALE);
         ensureFixedHealth();
         tickPersonality();
         tickImaginaryDisplacement();
         if (this.tickCount % 40 == 0) {
            NpcMagicCastBridge.ensureLeffProfile(this);
         }
      }
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      if (!this.level().isClientSide() && this.getPersistentData().getInt(TAG_IMAGINARY_DISPLACEMENT_ACTIVE) > 0 && source != null && amount > 0.0F) {
         Entity attacker = source.getEntity();
         if (attacker == null) {
            attacker = source.getDirectEntity();
         }
         if (attacker instanceof net.minecraft.world.entity.LivingEntity living && living != this && living.isAlive()) {
            living.invulnerableTime = 0;
            living.hurt(this.damageSources().mobAttack(this), amount);
            living.invulnerableTime = 0;
         }
         if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(), 36, 0.7, 0.8, 0.7, 0.08);
         }
         return false;
      }
      return super.hurt(source, amount);
   }

   @Nullable
   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, @Nullable SpawnGroupData data) {
      SpawnGroupData result = super.finalizeSpawn(level, difficulty, type, data);
      NpcScaleHelper.ensureFixedScale(this, FIXED_SCALE);
      this.setCustomName(Component.translatable("entity.typemoonworld.leff_laynor_flauros"));
      this.setCustomNameVisible(true);
      this.setCombatPersonality(NpcCombatPersonality.EVIL);
      this.setCombatTemperament(NpcCombatTemperament.STEADY);
      this.setCombatStyle(NpcCombatStyle.RANGED_BURST);
      NpcMagicCastBridge.configureLeff(this);
      ensureFixedHealth();
      this.setHealth(this.getMaxHealth());
      this.setPersistenceRequired();
      return result;
   }

   private void ensureFixedHealth() {
      var maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
      if (maxHealth == null) {
         return;
      }
      boolean migrated = this.getPersistentData().getBoolean(TAG_HEALTH_MIGRATED);
      if (Double.compare(maxHealth.getBaseValue(), MAX_HEALTH) != 0) {
         maxHealth.setBaseValue(MAX_HEALTH);
      }
      if (!migrated) {
         this.setHealth(this.getMaxHealth());
         this.getPersistentData().putBoolean(TAG_HEALTH_MIGRATED, true);
      } else if (this.getHealth() > this.getMaxHealth()) {
         this.setHealth(this.getMaxHealth());
      }
   }

   private void tickPersonality() {
      int personality = (this.tickCount / 200) % 3;
      this.getPersistentData().putInt(TAG_PERSONALITY, personality);
      if (personality == 0) {
         this.setCombatStyle(NpcCombatStyle.CONTROL_DRAIN);
      } else {
         this.setCombatStyle(NpcCombatStyle.RANGED_BURST);
      }
   }

   private void tickImaginaryDisplacement() {
      var data = this.getPersistentData();
      int active = data.getInt(TAG_IMAGINARY_DISPLACEMENT_ACTIVE);
      if (active > 0) {
         data.putInt(TAG_IMAGINARY_DISPLACEMENT_ACTIVE, active - 1);
      }
      int cooldown = data.getInt(TAG_IMAGINARY_DISPLACEMENT_COOLDOWN);
      if (cooldown > 0) {
         data.putInt(TAG_IMAGINARY_DISPLACEMENT_COOLDOWN, cooldown - 1);
      }
   }
}
