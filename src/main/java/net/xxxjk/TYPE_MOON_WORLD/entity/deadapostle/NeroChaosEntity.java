package net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle;

import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.combat.deadapostle.DeadApostleCombatProfile;
import net.xxxjk.TYPE_MOON_WORLD.combat.deadapostle.DeadApostleCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.combat.deadapostle.NeroChaosRules;
import net.xxxjk.TYPE_MOON_WORLD.entity.HumanNpcEntity;

public class NeroChaosEntity extends DeadApostleEntity {
   public static final String COMBAT_PROFILE_ID = "nero_chaos";
   public static final String TAG_REMAINING_LIVES = "NeroChaosRemainingLives";
   public static final String TAG_CHAOS_FORM = "NeroChaosChaosForm";
   public static final String TAG_CHAOS_ENERGY = "NeroChaosChaosEnergy";
   private static final String TAG_DEVOUR_TARGET = "NeroChaosDevourTarget";
   private static final String TAG_DEVOUR_UNTIL = "NeroChaosDevourUntil";
   private static final String TAG_DEVOUR_COOLDOWN = "NeroChaosDevourCooldown";
   private static final EntityDataAccessor<Integer> REMAINING_LIVES =
      SynchedEntityData.defineId(NeroChaosEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> CHAOS_FORM =
      SynchedEntityData.defineId(NeroChaosEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> CHAOS_ENERGY =
      SynchedEntityData.defineId(NeroChaosEntity.class, EntityDataSerializers.INT);

   public NeroChaosEntity(EntityType<? extends Monster> type, Level level) {
      super(type, level);
      setPersistenceRequired();
      setSilent(true);
   }

   public static AttributeSupplier.Builder createAttributes() {
      DeadApostleCombatProfile profile = DeadApostleCombatProfile.neroChaos();
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, profile.maxHealth())
         .add(Attributes.ATTACK_DAMAGE, profile.attackDamage())
         .add(Attributes.MOVEMENT_SPEED, profile.movementSpeed())
         .add(Attributes.ARMOR, profile.armor())
         .add(Attributes.ARMOR_TOUGHNESS, profile.armorToughness())
         .add(Attributes.KNOCKBACK_RESISTANCE, profile.knockbackResistance())
         .add(Attributes.FOLLOW_RANGE, profile.followRange())
         .add(Attributes.SCALE, 1.0);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(REMAINING_LIVES, 666);
      builder.define(CHAOS_FORM, false);
      builder.define(CHAOS_ENERGY, 100);
   }

   @Override
   protected void registerGoals() {
      goalSelector.addGoal(0, new FloatGoal(this));
      goalSelector.addGoal(1, new NeroChaosCombatGoal(this));
      goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.85));
      goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
      goalSelector.addGoal(7, new RandomLookAroundGoal(this));
      targetSelector.addGoal(1, new HurtByTargetGoal(this));
      targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
      targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Villager.class, true));
      targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
      targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, HumanNpcEntity.class, true));
   }

   @Override
   protected void customServerAiStep() {
      if (DeadApostleCombatSystem.tick(this)) return;
      long now = level().getGameTime();
      if (tickDevour(now)) return;
      tickChaosEnergy(now);
      if (!isChaosForm()) tickBeastRelease(now);
   }

   public String getCombatProfileId() {
      return COMBAT_PROFILE_ID;
   }

   public int getRemainingLives() {
      return Math.max(0, entityData.get(REMAINING_LIVES));
   }

   public void setRemainingLives(int lives) {
      int value = NeroChaosRules.clampLives(lives);
      entityData.set(REMAINING_LIVES, value);
      getPersistentData().putInt(TAG_REMAINING_LIVES, value);
   }

   public boolean isChaosForm() {
      return entityData.get(CHAOS_FORM);
   }

   public void setChaosForm(boolean value) {
      if (value == isChaosForm()) return;
      entityData.set(CHAOS_FORM, value);
      getPersistentData().putBoolean(TAG_CHAOS_FORM, value);
      if (value) {
         entityData.set(CHAOS_ENERGY, Math.max(1, getChaosEnergy()));
      }
      refreshDimensions();
   }

   public int getChaosEnergy() {
      return Math.max(0, entityData.get(CHAOS_ENERGY));
   }

   public void reviveFromDeath() {
      setHealth(getMaxHealth());
      deathTime = 0;
      hurtTime = 0;
      invulnerableTime = 0;
      setDeltaMovement(0.0, 0.0, 0.0);
      getPersistentData().remove(DeadApostleCombatSystem.TAG_STUN_UNTIL);
      setChaosForm(false);
   }

   public boolean isDevouring() {
      return getPersistentData().getLong(TAG_DEVOUR_UNTIL) > level().getGameTime()
         && getPersistentData().hasUUID(TAG_DEVOUR_TARGET);
   }

   @Override
   protected boolean burnsInSun() {
      return false;
   }

   @Override
   protected boolean receivesGeneratedName() {
      return false;
   }

   @Override
   public boolean isPushable() {
      return !isChaosForm() && super.isPushable();
   }

   @Override
   public void knockback(double strength, double x, double z) {
      if (!isChaosForm()) super.knockback(strength, x, z);
   }

   @Override
   public boolean causeFallDamage(float fallDistance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
      return isChaosForm() ? false : super.causeFallDamage(fallDistance, multiplier, source);
   }

   @Override
   public boolean isAlliedTo(Entity other) {
      return NeroChaosBeastLogic.isAlliedToOwner(this, other) || super.isAlliedTo(other);
   }

   @Override
   protected EntityDimensions getDefaultDimensions(Pose pose) {
      return isChaosForm() ? EntityDimensions.scalable(0.35F, 0.75F) : super.getDefaultDimensions(pose);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putInt(TAG_REMAINING_LIVES, getRemainingLives());
      tag.putBoolean(TAG_CHAOS_FORM, isChaosForm());
      tag.putInt(TAG_CHAOS_ENERGY, getChaosEnergy());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      setRemainingLives(tag.contains(TAG_REMAINING_LIVES) ? tag.getInt(TAG_REMAINING_LIVES) : 666);
      entityData.set(CHAOS_FORM, tag.getBoolean(TAG_CHAOS_FORM));
      entityData.set(CHAOS_ENERGY, tag.contains(TAG_CHAOS_ENERGY) ? tag.getInt(TAG_CHAOS_ENERGY) : 100);
      refreshDimensions();
   }

   private void tickBeastRelease(long now) {
      if (now % 20L != Math.floorMod(getId(), 20)) return;
      int active = countOwnedBeasts();
      int desired;
      LivingEntity target = getTarget();
      if (target == null || !target.isAlive()) {
         desired = 2 + getRandom().nextInt(4);
      } else {
         desired = NeroChaosRules.combatBeastTarget(getRemainingLives());
      }
      desired = Math.min(NeroChaosRules.MAX_ACTIVE_BEASTS, desired);
      while (active < desired && active < NeroChaosRules.MAX_ACTIVE_BEASTS && getRemainingLives() > 0) {
         if (!spawnBeast()) break;
         active++;
      }
      if (NeroChaosRules.shouldEnterChaosForm(getRemainingLives(), active)) setChaosForm(true);
   }

   private boolean spawnBeast() {
      if (!(level() instanceof ServerLevel serverLevel)) return false;
      EntityType<? extends Mob> type = switch (getRandom().nextInt(4)) {
         case 0 -> net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.NERO_CHAOS_HOUND.get();
         case 1 -> net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.NERO_CHAOS_SERPENT.get();
         case 2 -> net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.NERO_CHAOS_STAG.get();
         default -> net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.NERO_CHAOS_BIRD.get();
      };
      Mob beast = type.create(serverLevel);
      if (beast == null) return false;
      double angle = getRandom().nextDouble() * Math.PI * 2.0;
      double radius = 2.0 + getRandom().nextDouble() * 3.0;
      beast.moveTo(getX() + Math.cos(angle) * radius, getY(), getZ() + Math.sin(angle) * radius,
         getYRot(), 0.0F);
      NeroChaosBeastLogic.setOwner(beast, this);
      serverLevel.addFreshEntity(beast);
      return true;
   }

   private int countOwnedBeasts() {
      return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(96.0),
         entity -> entity instanceof NeroChaosBeastLogic.NeroChaosBeastEntityMarker marker
            && getUUID().equals(marker.neroChaosOwnerUuid())).size();
   }

   private void tickChaosEnergy(long now) {
      if (!isChaosForm()) {
         entityData.set(CHAOS_ENERGY, Math.min(100, getChaosEnergy() + (tickCount % 20 == 0 ? 1 : 0)));
         return;
      }
      if (tickCount % 20 == 0) {
         int energy = Math.max(0, getChaosEnergy() - 1);
         entityData.set(CHAOS_ENERGY, energy);
         if (energy <= 0) setChaosForm(false);
      }
      if (level() instanceof ServerLevel serverLevel && tickCount % 3 == 0) {
         serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.55, getZ(),
            5, 0.35, 0.45, 0.35, 0.02);
      }
      if (isChaosForm()) {
         removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
         removeEffect(MobEffects.LEVITATION);
      }
   }

   private boolean tickDevour(long now) {
      var data = getPersistentData();
      long until = data.getLong(TAG_DEVOUR_UNTIL);
      if (until > now) {
         if (!data.hasUUID(TAG_DEVOUR_TARGET)) return false;
         Entity entity = level() instanceof ServerLevel serverLevel
            ? serverLevel.getEntity(data.getUUID(TAG_DEVOUR_TARGET)) : null;
         if (!(entity instanceof LivingEntity prey)) {
            data.remove(TAG_DEVOUR_UNTIL);
            data.remove(TAG_DEVOUR_TARGET);
            return false;
         }
         if (!prey.isAlive()) {
            heal(50.0F);
            data.remove(TAG_DEVOUR_UNTIL);
            data.remove(TAG_DEVOUR_TARGET);
            data.putLong(TAG_DEVOUR_COOLDOWN, now + 20L);
            return true;
         }
         if (!isValidPrey(prey)) {
            data.remove(TAG_DEVOUR_UNTIL);
            data.remove(TAG_DEVOUR_TARGET);
            return false;
         }
         if (distanceToSqr(prey) > 3.0 * 3.0) {
            if (prey instanceof Player) {
               data.remove(TAG_DEVOUR_UNTIL);
               data.remove(TAG_DEVOUR_TARGET);
               return false;
            }
            Vec3 contact = prey.position().subtract(position()).multiply(1.0, 0.0, 1.0);
            if (contact.lengthSqr() < 1.0E-6) contact = getLookAngle().multiply(1.0, 0.0, 1.0);
            contact = contact.normalize().scale(1.0);
            prey.teleportTo(getX() + contact.x, getY(), getZ() + contact.z);
         }
         getNavigation().stop();
         getLookControl().setLookAt(prey, 35.0F, 35.0F);
         swing(InteractionHand.MAIN_HAND);
         prey.invulnerableTime = 0;
         prey.hurt(damageSources().mobAttack(this), 1.5F);
         prey.setDeltaMovement(Vec3.ZERO);
         if (!prey.isAlive()) {
            heal(50.0F);
            data.remove(TAG_DEVOUR_UNTIL);
            data.remove(TAG_DEVOUR_TARGET);
            data.putLong(TAG_DEVOUR_COOLDOWN, now + 20L);
         }
         return true;
      }
      if (now < data.getLong(TAG_DEVOUR_COOLDOWN)) return false;
      LivingEntity target = getTarget();
      if (target != null && isValidPrey(target) && distanceToSqr(target) <= 3.0 * 3.0) {
         data.putUUID(TAG_DEVOUR_TARGET, target.getUUID());
         data.putLong(TAG_DEVOUR_UNTIL, now + 20L);
         return true;
      }
      return false;
   }

   private boolean isValidPrey(LivingEntity prey) {
      return prey.isAlive() && prey != this
         && !(prey instanceof DeadApostleEntity)
         && !NeroChaosBeastLogic.isBeast(prey)
         && !isAlliedTo(prey);
   }

   private static final class NeroChaosCombatGoal extends net.minecraft.world.entity.ai.goal.Goal {
      private final NeroChaosEntity nero;

      private NeroChaosCombatGoal(NeroChaosEntity nero) {
         this.nero = nero;
         setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      @Override
      public boolean canUse() {
         return nero.getTarget() != null && nero.getTarget().isAlive();
      }

      @Override
      public boolean canContinueToUse() {
         return canUse();
      }

      @Override
      public void tick() {
         LivingEntity target = nero.getTarget();
         if (target == null) return;
         if (nero.isDevouring()) {
            nero.getNavigation().stop();
            return;
         }
         nero.getLookControl().setLookAt(target, 35.0F, 35.0F);
         double distanceSqr = nero.distanceToSqr(target);
         if (distanceSqr > 16.0) {
            nero.getNavigation().moveTo(target, nero.isChaosForm() ? 1.35 : 1.05);
         } else {
            nero.getNavigation().stop();
            nero.getMoveControl().strafe(0.16F, 0.45F);
         }
      }
   }
}
