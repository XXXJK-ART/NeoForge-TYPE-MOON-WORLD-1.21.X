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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.combat.deadapostle.DeadApostleCombatProfile;
import net.xxxjk.TYPE_MOON_WORLD.combat.deadapostle.DeadApostleCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.combat.deadapostle.NeroChaosRules;
import net.xxxjk.TYPE_MOON_WORLD.entity.HumanNpcEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.NpcScaleHelper;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public class NeroChaosEntity extends DeadApostleEntity {
   public static final String COMBAT_PROFILE_ID = "nero_chaos";
   public static final String TAG_REMAINING_LIVES = "NeroChaosRemainingLives";
   public static final String TAG_CHAOS_FORM = "NeroChaosChaosForm";
   public static final String TAG_CHAOS_ENERGY = "NeroChaosChaosEnergy";
   public static final String TAG_BEAST_REGROUP_UNTIL = "NeroChaosBeastRegroupUntil";
   private static final String TAG_FIXED_SCALE = "NeroChaosFixedScaleV1";
   private static final String DEAD_APOSTLE_BODY_SCALE_V2 = "TypeMoonDeadApostleBodyScaleV2";
   private static final String TAG_DEVOUR_TARGET = "NeroChaosDevourTarget";
   private static final String TAG_DEVOUR_UNTIL = "NeroChaosDevourUntil";
   private static final String TAG_DEVOUR_COOLDOWN = "NeroChaosDevourCooldown";
   private static final String TAG_PENDING_BEAST_REVIVES = "NeroChaosPendingBeastRevives";
   private static final String TAG_NEXT_BEAST_REVIVE = "NeroChaosNextBeastRevive";
   private static final String TAG_CROWD_AOE_COOLDOWN = "NeroChaosCrowdAoeCooldown";
   private static final String TAG_BODY_STRIKE_COOLDOWN = "NeroChaosBodyStrikeCooldown";
   private static final int CROWD_AOE_MIN_ENEMIES = 4;
   private static final double CROWD_AOE_RADIUS = 6.25;
   private static final long CROWD_AOE_COOLDOWN_TICKS = 100L;
   private static final float CROWD_AOE_DAMAGE = 24.0F;
   private static final long BODY_STRIKE_COOLDOWN_TICKS = 14L;
   private static final float BODY_STRIKE_DAMAGE = 14.0F;
   private static final float BODY_STRIKE_CHAOS_DAMAGE = 18.0F;
   private static final long DEVOUR_DURATION_TICKS = 20L;
   private static final float DEVOUR_START_RATIO = 0.42F;
   private static final float DEVOUR_START_MIN_HEALTH = 12.0F;
   private static final float DEVOUR_FINISH_RATIO = 0.18F;
   private static final float DEVOUR_FINISH_BUFFER = 80.0F;
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
      targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
         target -> !EntityUtils.isImmunePlayerTarget(target)));
      targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Villager.class, true));
      targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
      targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, HumanNpcEntity.class, true));
   }

   @Override
   protected void customServerAiStep() {
      ensureNeroScale();
      long now = level().getGameTime();
      tickBeastRevival(now);
      if (EntityUtils.isImmunePlayerTarget(getTarget())) {
         setTarget(null);
      }
      if (DeadApostleCombatSystem.tick(this)) return;
      boolean devouring = tickDevour(now);
      tickChaosEnergy(now);
      if (!devouring) {
         // The chaos form must not leave Nero fighting alone if his combat beasts were lost.
         tickBeastRelease(now);
         tickCrowdAoe(now);
      }
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

   public int getPendingBeastRevives() {
      return Math.max(0, getPersistentData().getInt(TAG_PENDING_BEAST_REVIVES));
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

   public boolean tryConsumeLifeAndRevive() {
      int lives = getRemainingLives();
      if (!NeroChaosRules.shouldReviveAfterLethal(lives, false)) {
         return false;
      }
      setRemainingLives(NeroChaosRules.consumeLife(lives));
      NeroChaosBeastLogic.regroupOwnedBeasts(this);
      reviveFromDeath();
      getPersistentData().putLong(DeadApostleCombatSystem.TAG_INVULN_UNTIL, level().getGameTime() + 20L);
      if (level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, getX(), getY() + getBbHeight() * 0.58, getZ(),
            36, 0.6, 0.75, 0.6, 0.12);
         level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + getBbHeight() * 0.45, getZ(),
            28, 0.55, 0.55, 0.55, 0.025);
         level.playSound(null, blockPosition(), SoundEvents.TOTEM_USE, SoundSource.HOSTILE, 0.9F, 0.65F);
      }
      return true;
   }

   public void queueBeastRevival() {
      var data = getPersistentData();
      int pending = Math.max(0, data.getInt(TAG_PENDING_BEAST_REVIVES));
      if (getRemainingLives() + pending >= NeroChaosRules.MAX_LIVES) return;
      data.putInt(TAG_PENDING_BEAST_REVIVES, Math.min(NeroChaosRules.MAX_LIVES, pending + 1));
      if (data.getLong(TAG_NEXT_BEAST_REVIVE) <= level().getGameTime()) {
         data.putLong(TAG_NEXT_BEAST_REVIVE, level().getGameTime() + NeroChaosRules.BEAST_REVIVAL_DELAY_TICKS);
      }
   }

   public void reabsorbBeast(Mob beast) {
      if (beast == null || beast.level() != level() || !beast.isAlive()) return;
      if (!getUUID().equals(NeroChaosBeastLogic.ownerUuid(beast))) return;
      setRemainingLives(NeroChaosRules.restoreLife(getRemainingLives()));
      if (level() instanceof ServerLevel serverLevel) {
         serverLevel.sendParticles(ParticleTypes.SMOKE, beast.getX(), beast.getY() + beast.getBbHeight() * 0.5,
            beast.getZ(), 8, 0.25, 0.25, 0.25, 0.02);
      }
      beast.discard();
   }

   public boolean spawnSuccessorFromOwnedBeast() {
      if (!(level() instanceof ServerLevel serverLevel)) return false;
      Mob successorBeast = findOwnedBeastForSuccession(serverLevel);
      if (successorBeast == null) return false;

      NeroChaosEntity successor = ModEntities.NERO_CHAOS.get().create(serverLevel);
      if (successor == null) return false;
      successor.moveTo(successorBeast.getX(), successorBeast.getY(), successorBeast.getZ(),
         successorBeast.getYRot(), successorBeast.getXRot());
      successor.setRemainingLives(getRemainingLives());
      successor.entityData.set(CHAOS_ENERGY, getChaosEnergy());
      successor.getPersistentData().putInt(TAG_PENDING_BEAST_REVIVES, getPendingBeastRevives());
      long nextRevive = getPersistentData().getLong(TAG_NEXT_BEAST_REVIVE);
      if (nextRevive > level().getGameTime()) {
         successor.getPersistentData().putLong(TAG_NEXT_BEAST_REVIVE, nextRevive);
      }
      successor.reviveFromDeath();
      LivingEntity target = getTarget();
      if (target != null && target.isAlive() && !successor.isAlliedTo(target)) {
         successor.setTarget(target);
      }
      serverLevel.addFreshEntity(successor);
      transferOwnedBeastsToSuccessor(serverLevel, successor, successorBeast);
      successorBeast.discard();
      serverLevel.sendParticles(ParticleTypes.SMOKE, successor.getX(), successor.getY() + successor.getBbHeight() * 0.5,
         successor.getZ(), 36, 0.5, 0.65, 0.5, 0.05);
      return true;
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
      tag.putInt(TAG_PENDING_BEAST_REVIVES, getPendingBeastRevives());
      tag.putLong(TAG_NEXT_BEAST_REVIVE, getPersistentData().getLong(TAG_NEXT_BEAST_REVIVE));
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      setRemainingLives(tag.contains(TAG_REMAINING_LIVES) ? tag.getInt(TAG_REMAINING_LIVES) : 666);
      entityData.set(CHAOS_FORM, tag.getBoolean(TAG_CHAOS_FORM));
      entityData.set(CHAOS_ENERGY, tag.contains(TAG_CHAOS_ENERGY) ? tag.getInt(TAG_CHAOS_ENERGY) : 100);
      getPersistentData().putInt(TAG_PENDING_BEAST_REVIVES,
         tag.contains(TAG_PENDING_BEAST_REVIVES) ? tag.getInt(TAG_PENDING_BEAST_REVIVES) : 0);
      if (tag.contains(TAG_NEXT_BEAST_REVIVE)) {
         getPersistentData().putLong(TAG_NEXT_BEAST_REVIVE, tag.getLong(TAG_NEXT_BEAST_REVIVE));
      }
      refreshDimensions();
   }

   private void tickBeastRelease(long now) {
      if (now % 20L != Math.floorMod(getId(), 20)) return;
      if (now < getPersistentData().getLong(TAG_BEAST_REGROUP_UNTIL)) return;
      int active = countOwnedBeasts();
      int desired;
      LivingEntity target = getTarget();
      int nearbyEnemies = countNearbyEnemies(48.0);
      if ((target == null || !target.isAlive()) && nearbyEnemies <= 0) {
         desired = 0;
      } else {
         desired = NeroChaosRules.combatBeastTarget(getRemainingLives(), nearbyEnemies);
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
      EntityType<? extends Mob> type = switch (getRandom().nextInt(7)) {
         case 0 -> net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.NERO_CHAOS_HOUND.get();
         case 1 -> net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.NERO_CHAOS_SERPENT.get();
         case 2 -> net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.NERO_CHAOS_STAG.get();
         case 3 -> net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.NERO_CHAOS_BIRD.get();
         case 4 -> net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.NERO_CHAOS_BEAR.get();
         case 5 -> net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.NERO_CHAOS_CAT.get();
         default -> net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.NERO_CHAOS_BAT.get();
      };
      Mob beast = type.create(serverLevel);
      if (beast == null) return false;
      double angle = getRandom().nextDouble() * Math.PI * 2.0;
      double radius = 2.0 + getRandom().nextDouble() * 3.0;
      beast.moveTo(getX() + Math.cos(angle) * radius, getY(), getZ() + Math.sin(angle) * radius,
         getYRot(), 0.0F);
      NeroChaosBeastLogic.setOwner(beast, this);
      if (!serverLevel.addFreshEntity(beast)) return false;
      setRemainingLives(NeroChaosRules.consumeLife(getRemainingLives()));
      return true;
   }

   private int countOwnedBeasts() {
      return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(96.0),
         entity -> entity instanceof NeroChaosBeastLogic.NeroChaosBeastEntityMarker marker
            && getUUID().equals(marker.neroChaosOwnerUuid())).size();
   }

   private int countNearbyEnemies(double radius) {
      return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius),
         entity -> entity != this && entity.isAlive() && canAttack(entity)
            && !isAlliedTo(entity)
            && !entity.isAlliedTo(this)
            && !EntityUtils.isImmunePlayerTarget(entity)).size();
   }

   private void tickCrowdAoe(long now) {
      if (now % 20L != Math.floorMod(getId() + 7, 20)) return;
      var data = getPersistentData();
      if (now < data.getLong(TAG_CROWD_AOE_COOLDOWN)) return;
      var enemies = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(CROWD_AOE_RADIUS),
         entity -> entity != this && entity.isAlive() && canAttack(entity)
            && !isAlliedTo(entity)
            && !entity.isAlliedTo(this)
            && !EntityUtils.isImmunePlayerTarget(entity));
      if (enemies.size() < CROWD_AOE_MIN_ENEMIES || getRandom().nextFloat() > 0.35F) return;

      data.putLong(TAG_CROWD_AOE_COOLDOWN, now + CROWD_AOE_COOLDOWN_TICKS);
      swing(InteractionHand.MAIN_HAND);
      for (LivingEntity enemy : enemies) {
         enemy.invulnerableTime = 0;
         enemy.hurt(damageSources().mobAttack(this), CROWD_AOE_DAMAGE);
         Vec3 away = enemy.position().subtract(position()).multiply(1.0, 0.0, 1.0);
         if (away.lengthSqr() < 1.0E-6) away = getLookAngle().multiply(1.0, 0.0, 1.0);
         away = away.normalize();
         enemy.setDeltaMovement(enemy.getDeltaMovement().add(away.x * 0.7, 0.18, away.z * 0.7));
         enemy.hurtMarked = true;
      }
      if (level() instanceof ServerLevel serverLevel) {
         serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + getBbHeight() * 0.52, getZ(),
            6, 1.6, 0.35, 1.6, 0.0);
         serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY() + getBbHeight() * 0.35, getZ(),
            28, 1.9, 0.25, 1.9, 0.04);
         serverLevel.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
            SoundSource.HOSTILE, 1.0F, 0.7F);
      }
   }

   private void tickBeastRevival(long now) {
      var data = getPersistentData();
      int pending = Math.max(0, data.getInt(TAG_PENDING_BEAST_REVIVES));
      if (pending <= 0) {
         data.remove(TAG_NEXT_BEAST_REVIVE);
         return;
      }
      long next = data.getLong(TAG_NEXT_BEAST_REVIVE);
      if (next <= 0L) {
         data.putLong(TAG_NEXT_BEAST_REVIVE, now + NeroChaosRules.BEAST_REVIVAL_DELAY_TICKS);
         return;
      }
      if (now < next) return;
      if (getRemainingLives() < NeroChaosRules.MAX_LIVES) {
         setRemainingLives(NeroChaosRules.restoreLife(getRemainingLives()));
      }
      pending--;
      if (pending > 0) {
         data.putInt(TAG_PENDING_BEAST_REVIVES, pending);
         data.putLong(TAG_NEXT_BEAST_REVIVE, now + NeroChaosRules.BEAST_REVIVAL_DELAY_TICKS);
      } else {
         data.remove(TAG_PENDING_BEAST_REVIVES);
         data.remove(TAG_NEXT_BEAST_REVIVE);
      }
   }

   private Mob findOwnedBeastForSuccession(ServerLevel serverLevel) {
      Mob best = null;
      double bestDistance = Double.MAX_VALUE;
      for (Entity entity : serverLevel.getEntities().getAll()) {
         if (!(entity instanceof Mob mob) || !mob.isAlive() || !NeroChaosBeastLogic.isBeast(mob)) continue;
         if (!getUUID().equals(NeroChaosBeastLogic.ownerUuid(mob))) continue;
         double distance = distanceToSqr(mob);
         if (distance < bestDistance) {
            best = mob;
            bestDistance = distance;
         }
      }
      return best;
   }

   private void transferOwnedBeastsToSuccessor(ServerLevel serverLevel, NeroChaosEntity successor, Mob absorbedBeast) {
      for (Entity entity : serverLevel.getEntities().getAll()) {
         if (!(entity instanceof Mob mob) || mob == absorbedBeast || !mob.isAlive()) continue;
         if (!NeroChaosBeastLogic.isBeast(mob)) continue;
         if (getUUID().equals(NeroChaosBeastLogic.ownerUuid(mob))) {
            NeroChaosBeastLogic.setOwner(mob, successor);
         }
      }
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
         boolean finishing = now + 1L >= until
            || prey.getHealth() <= Math.max(6.0F, prey.getMaxHealth() * DEVOUR_FINISH_RATIO);
         float damage = finishing
            ? Math.max(prey.getHealth() + DEVOUR_FINISH_BUFFER, prey.getMaxHealth() * 3.0F)
            : 1.5F;
         prey.hurt(damageSources().mobAttack(this), damage);
         prey.setDeltaMovement(Vec3.ZERO);
         if (finishing && prey.isAlive()) {
            prey.setInvulnerable(false);
            prey.invulnerableTime = 0;
            prey.setHealth(0.0F);
            prey.die(damageSources().genericKill());
         }
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
      if (target != null && isValidPrey(target)
         && distanceToSqr(target) <= 3.0 * 3.0
         && shouldBeginDevour(target)) {
         data.putUUID(TAG_DEVOUR_TARGET, target.getUUID());
         data.putLong(TAG_DEVOUR_UNTIL, now + DEVOUR_DURATION_TICKS);
         return true;
      }
      return false;
   }

   public boolean tryBodyStrike(LivingEntity target, long now) {
      if (target == null || !target.isAlive()) return false;
      var data = getPersistentData();
      if (now < data.getLong(TAG_BODY_STRIKE_COOLDOWN)) return false;
      if (distanceToSqr(target) > 6.0 * 6.0) return false;
      swing(InteractionHand.MAIN_HAND);
      target.invulnerableTime = 0;
      float damage = isChaosForm() ? BODY_STRIKE_CHAOS_DAMAGE : BODY_STRIKE_DAMAGE;
      boolean hit = target.hurt(damageSources().mobAttack(this), damage);
      if (hit) {
         Vec3 push = target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
         if (push.lengthSqr() < 1.0E-6) push = getLookAngle().multiply(1.0, 0.0, 1.0);
         push = push.normalize();
         target.setDeltaMovement(target.getDeltaMovement().add(push.x * 0.45, 0.12, push.z * 0.45));
         target.hurtMarked = true;
         if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + getBbHeight() * 0.48, getZ(),
               4, 0.35, 0.2, 0.35, 0.0);
            serverLevel.sendParticles(ParticleTypes.CRIT, getX(), getY() + getBbHeight() * 0.45, getZ(),
               8, 0.2, 0.2, 0.2, 0.04);
            serverLevel.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
               SoundSource.HOSTILE, 0.85F, isChaosForm() ? 0.8F : 0.95F);
         }
      }
      data.putLong(TAG_BODY_STRIKE_COOLDOWN, now + (isChaosForm() ? BODY_STRIKE_COOLDOWN_TICKS - 2L : BODY_STRIKE_COOLDOWN_TICKS));
      return hit;
   }

   private boolean isValidPrey(LivingEntity prey) {
      return prey.isAlive() && prey != this
         && !EntityUtils.isImmunePlayerTarget(prey)
         && !(prey instanceof DeadApostleEntity)
         && !NeroChaosBeastLogic.isBeast(prey)
         && !isAlliedTo(prey);
   }

   private boolean shouldBeginDevour(LivingEntity prey) {
      float maxHealth = Math.max(1.0F, prey.getMaxHealth());
      float threshold = Math.max(DEVOUR_START_MIN_HEALTH, maxHealth * DEVOUR_START_RATIO);
      if (isChaosForm()) {
         threshold = Math.max(threshold, maxHealth * 0.55F);
      }
      if (prey instanceof Player) {
         threshold *= 0.92F;
      }
      return prey.getHealth() <= threshold;
   }

   private void ensureNeroScale() {
      if (getPersistentData().getBoolean(TAG_FIXED_SCALE)) return;
      NpcScaleHelper.setInheritedScale(this, 1.0);
      getPersistentData().putBoolean(DEAD_APOSTLE_BODY_SCALE_V2, true);
      getPersistentData().putBoolean(TAG_FIXED_SCALE, true);
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
      public boolean requiresUpdateEveryTick() {
         return true;
      }

      @Override
      public void tick() {
         LivingEntity target = nero.getTarget();
         if (target == null) return;
         if (nero.isDevouring()) {
            nero.getNavigation().stop();
            return;
         }
         if (tryDodgeIncomingProjectile()) return;
         nero.getLookControl().setLookAt(target, 35.0F, 35.0F);
         double distanceSqr = nero.distanceToSqr(target);
         long now = nero.level().getGameTime();
         if (distanceSqr <= 36.0 && nero.tryBodyStrike(target, now)) {
            nero.getNavigation().stop();
            return;
         }
         if (distanceSqr > 16.0) {
            nero.getNavigation().moveTo(target, nero.isChaosForm() ? 1.35 : 1.05);
         } else {
            nero.getNavigation().stop();
            nero.getMoveControl().strafe(0.18F, distanceSqr < 9.0 ? 0.65F : 0.42F);
         }
      }

      private boolean tryDodgeIncomingProjectile() {
         long now = nero.level().getGameTime();
         if (nero.getPersistentData().getLong("TypeMoonAiProjectileScanTick") == now
            || nero.tickCount % 3 != Math.floorMod(nero.getId(), 3)) return false;
         net.xxxjk.TYPE_MOON_WORLD.combat.ai.ProjectileThreatSensor.IncomingProjectile incoming =
            net.xxxjk.TYPE_MOON_WORLD.combat.ai.ProjectileThreatSensor.nearest(nero, 10.0, 8.0);
         if (incoming == null) return false;
         boolean moved = net.xxxjk.TYPE_MOON_WORLD.combat.ai.EvasionMovementService.tryEvade(
            nero, incoming.projectile().position(), 5, true);
         if (moved) {
            nero.getNavigation().stop();
         }
         return moved;
      }
   }
}
