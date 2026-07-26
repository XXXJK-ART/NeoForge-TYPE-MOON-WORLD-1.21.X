package net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.HumanNpcEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.NpcScaleHelper;
import org.jetbrains.annotations.Nullable;

public abstract class DeadApostleEntity extends Monster {
   public static final double MIN_BODY_SCALE = 0.6;
   public static final double MAX_BODY_SCALE = 1.0;
   private static final String BODY_SCALE_V2 = "TypeMoonDeadApostleBodyScaleV2";
   private static final String CHINESE_GENERATED_NAME_V2 = "TypeMoonDeadApostleChineseNameV2";
   private static final EntityDataAccessor<Integer> SUN_TICKS = SynchedEntityData.defineId(DeadApostleEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> FEMALE = SynchedEntityData.defineId(DeadApostleEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> NAME_CULTURE = SynchedEntityData.defineId(DeadApostleEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> INHERITED_NAME = SynchedEntityData.defineId(DeadApostleEntity.class, EntityDataSerializers.BOOLEAN);

   protected DeadApostleEntity(EntityType<? extends Monster> type, Level level) {
      super(type, level);
      if (!canSwim()) {
         this.setPathfindingMalus(PathType.WATER, -1.0F);
      }
   }

   public static AttributeSupplier.Builder attributes(double health, double attack, double armor, double speed) {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, health)
         .add(Attributes.ATTACK_DAMAGE, attack)
         .add(Attributes.ARMOR, armor)
         .add(Attributes.MOVEMENT_SPEED, speed)
         .add(Attributes.FOLLOW_RANGE, 32.0)
         .add(Attributes.SCALE, 1.0);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(SUN_TICKS, 0);
      builder.define(FEMALE, false);
      builder.define(NAME_CULTURE, -1);
      builder.define(INHERITED_NAME, false);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(1, new DeadApostleCombatGoal(this));
      this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.95));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Villager.class, true));
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, HumanNpcEntity.class, true));
   }

   @Override
   public boolean isAlliedTo(Entity entity) {
      return entity instanceof DeadApostleEntity || super.isAlliedTo(entity);
   }

   @Override
   public boolean canAttack(LivingEntity target) {
      return !(target instanceof DeadApostleEntity) && super.canAttack(target);
   }

   @Override
   protected int decreaseAirSupply(int air) {
      return canSwim() ? super.decreaseAirSupply(air) : air;
   }

   protected boolean canSwim() {
      return true;
   }

   protected boolean burnsInSun() {
      return false;
   }

   protected int sunlightDebuffDelay() {
      return -1;
   }

   protected boolean receivesGeneratedName() {
      return false;
   }

   protected void applyStageSpecialization() {
   }

   public boolean isFemale() {
      return this.entityData.get(FEMALE);
   }

   public int getNameCulture() {
      return this.entityData.get(NAME_CULTURE);
   }

   public boolean hasInheritedName() {
      return this.entityData.get(INHERITED_NAME);
   }

   public void initializeGeneratedName(@Nullable Boolean femaleHint) {
      if (!receivesGeneratedName() || this.hasCustomName()) return;
      boolean female = femaleHint != null ? femaleHint : this.random.nextBoolean();
      int culture = this.random.nextInt(3);
      this.entityData.set(FEMALE, female);
      this.entityData.set(NAME_CULTURE, culture);
      this.entityData.set(INHERITED_NAME, false);
      this.setCustomName(Component.literal(DeadApostleNameGenerator.generate(this.random, culture, female)));
      this.setCustomNameVisible(true);
      getPersistentData().putBoolean(CHINESE_GENERATED_NAME_V2, true);
   }

   public void inheritName(Component name, boolean visible, @Nullable Boolean femaleHint) {
      if (!receivesGeneratedName() || name == null) return;
      this.entityData.set(FEMALE, femaleHint != null ? femaleHint : this.random.nextBoolean());
      this.entityData.set(NAME_CULTURE, -1);
      this.entityData.set(INHERITED_NAME, true);
      this.setCustomName(name.copy());
      this.setCustomNameVisible(visible);
   }

   @Nullable
   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                       @Nullable SpawnGroupData spawnGroupData) {
      SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
      ensureBodyScale();
      initializeGeneratedName(null);
      applyStageSpecialization();
      ensureCurrentStageAttributes();
      return data;
   }

   @Override
   public void aiStep() {
      super.aiStep();
      if (this.level().isClientSide) return;
      ensureBodyScale();
      ensureChineseGeneratedName();
      ensureCurrentStageAttributes();
      boolean exposed = isExposedToSun();
      if (burnsInSun() && exposed) {
         this.igniteForSeconds(8.0F);
      }
      int delay = sunlightDebuffDelay();
      if (delay >= 0) {
         int ticks = exposed ? Math.min(1200, this.entityData.get(SUN_TICKS) + 1) : 0;
         this.entityData.set(SUN_TICKS, ticks);
         if (exposed && ticks >= delay) {
            this.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false));
            this.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 40, 1, false, false));
         }
      }
   }

   private boolean isExposedToSun() {
      if (!this.level().isDay() || this.level().isClientSide) return false;
      BlockPos eye = BlockPos.containing(this.getX(), this.getEyeY(), this.getZ());
      return this.level().canSeeSky(eye) && this.level().getBrightness(net.minecraft.world.level.LightLayer.SKY, eye) > 11;
   }

   private void ensureBodyScale() {
      if (getPersistentData().getBoolean(BODY_SCALE_V2)) return;
      double scale = MIN_BODY_SCALE + random.nextDouble() * (MAX_BODY_SCALE - MIN_BODY_SCALE);
      NpcScaleHelper.setInheritedScale(this, scale);
      getPersistentData().putBoolean(BODY_SCALE_V2, true);
   }

   private void ensureChineseGeneratedName() {
      if (!receivesGeneratedName() || getPersistentData().getBoolean(CHINESE_GENERATED_NAME_V2)) return;
      if (!hasInheritedName() && hasCustomName() && getNameCulture() >= 0) {
         setCustomName(Component.literal(DeadApostleNameGenerator.generate(random, getNameCulture(), isFemale())));
         setCustomNameVisible(true);
      }
      getPersistentData().putBoolean(CHINESE_GENERATED_NAME_V2, true);
   }

   public void inheritBodyScale(double scale) {
      NpcScaleHelper.setInheritedScale(this, scale);
      getPersistentData().putBoolean(BODY_SCALE_V2, true);
   }

   protected void ensureCurrentStageAttributes() {
   }

   protected final void migrateStageAttributes(String versionTag, double maximumHealth, double movementSpeed) {
      if (getPersistentData().getBoolean(versionTag)) return;
      float previousMaximum = getMaxHealth();
      float healthRatio = previousMaximum > 0.0F ? Mth.clamp(getHealth() / previousMaximum, 0.0F, 1.0F) : 1.0F;
      getAttribute(Attributes.MAX_HEALTH).setBaseValue(maximumHealth);
      getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(movementSpeed);
      if (isAlive()) setHealth(Math.max(1.0F, (float)maximumHealth * healthRatio));
      getPersistentData().putBoolean(versionTag, true);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putInt("DeadApostleSunTicks", this.entityData.get(SUN_TICKS));
      tag.putBoolean("DeadApostleFemale", isFemale());
      tag.putInt("DeadApostleNameCulture", getNameCulture());
      tag.putBoolean("DeadApostleInheritedName", hasInheritedName());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.entityData.set(SUN_TICKS, tag.getInt("DeadApostleSunTicks"));
      this.entityData.set(FEMALE, tag.getBoolean("DeadApostleFemale"));
      this.entityData.set(NAME_CULTURE, tag.contains("DeadApostleNameCulture") ? tag.getInt("DeadApostleNameCulture") : -1);
      this.entityData.set(INHERITED_NAME, tag.getBoolean("DeadApostleInheritedName"));
   }

   private static final class DeadApostleCombatGoal extends Goal {
      private final DeadApostleEntity apostle;
      private int pathCooldown;
      private int attackCooldown;
      private int circleTicks;
      private int dodgeCooldown;
      private int approachCooldown;
      private float circleDirection;

      private DeadApostleCombatGoal(DeadApostleEntity apostle) {
         this.apostle = apostle;
         setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      @Override public boolean canUse() {
         LivingEntity target = apostle.getTarget();
         return target != null && target.isAlive() && apostle.canAttack(target);
      }

      @Override public boolean canContinueToUse() { return canUse(); }
      @Override public boolean requiresUpdateEveryTick() { return true; }

      @Override public void start() {
         chooseCircleDirection();
         apostle.setSprinting(true);
      }

      @Override public void stop() {
         apostle.getNavigation().stop();
         apostle.setSprinting(false);
      }

      @Override public void tick() {
         LivingEntity target = apostle.getTarget();
         if (target == null) return;
         apostle.getLookControl().setLookAt(target, 35.0F, 35.0F);
         if (pathCooldown > 0) pathCooldown--;
         if (attackCooldown > 0) attackCooldown--;
         if (dodgeCooldown > 0) dodgeCooldown--;
         if (approachCooldown > 0) approachCooldown--;
         if (--circleTicks <= 0) chooseCircleDirection();

         if (isAdvancedStage() && tryDodgeIncomingProjectile()) return;

         double distanceSqr = apostle.distanceToSqr(target);
         double distance = Math.sqrt(distanceSqr);
         double reach = apostle.getBbWidth() * 1.7F + target.getBbWidth() + 0.7F;
         double reachSqr = reach * reach;
         if (distanceSqr > reachSqr) {
            if (isAdvancedStage() && trySpecialApproach(target, distance)) return;
            if (pathCooldown <= 0) {
               apostle.getNavigation().moveTo(target, combatSpeed());
               pathCooldown = 4 + apostle.getRandom().nextInt(5);
            }
            if (apostle.horizontalCollision && apostle.onGround() && apostle.getRandom().nextFloat() < 0.18F) {
               apostle.getJumpControl().jump();
            }
            return;
         }

         apostle.getNavigation().stop();
         apostle.getMoveControl().strafe(0.22F, circleDirection * 0.55F);
         if (attackCooldown <= 0 && apostle.hasLineOfSight(target)) {
            apostle.swing(InteractionHand.MAIN_HAND);
            apostle.doHurtTarget(target);
            attackCooldown = apostle instanceof NightKinEntity ? 10 : apostle instanceof LivingDeadEntity ? 12 : 16;
         }
      }

      private boolean isAdvancedStage() {
         return apostle instanceof LivingDeadEntity || apostle instanceof NightKinEntity;
      }

      private boolean tryDodgeIncomingProjectile() {
         if (dodgeCooldown > 0) return false;
         double searchRadius = apostle instanceof NightKinEntity ? 10.0 : 8.0;
         Vec3 center = apostle.position().add(0.0, apostle.getBbHeight() * 0.5, 0.0);
         Projectile nearestThreat = null;
         double nearestImpact = Double.MAX_VALUE;
         for (Projectile projectile : apostle.level().getEntitiesOfClass(Projectile.class,
            apostle.getBoundingBox().inflate(searchRadius), this::isHostileProjectile)) {
            Vec3 velocity = projectile.getDeltaMovement();
            double velocitySqr = velocity.lengthSqr();
            if (velocitySqr < 0.04) continue;
            Vec3 toApostle = center.subtract(projectile.position());
            double impactTicks = toApostle.dot(velocity) / velocitySqr;
            if (impactTicks < 0.0 || impactTicks > 8.0) continue;
            Vec3 closestPoint = projectile.position().add(velocity.scale(impactTicks));
            double missDistanceSqr = closestPoint.distanceToSqr(center);
            double dangerRadius = apostle.getBbWidth() * 0.65 + 0.85;
            if (missDistanceSqr <= dangerRadius * dangerRadius && impactTicks < nearestImpact) {
               nearestThreat = projectile;
               nearestImpact = impactTicks;
            }
         }
         if (nearestThreat == null) return false;

         Vec3 flight = nearestThreat.getDeltaMovement();
         Vec3 lateral = new Vec3(-flight.z, 0.0, flight.x);
         if (lateral.lengthSqr() < 1.0E-5) return false;
         lateral = lateral.normalize().scale(apostle.getRandom().nextBoolean() ? 1.0 : -1.0);
         double strength = apostle instanceof NightKinEntity ? 0.92 : 0.72;
         Vec3 dodge = lateral.scale(strength);
         apostle.getNavigation().stop();
         apostle.setDeltaMovement(dodge.x, Math.max(0.16, apostle.getDeltaMovement().y), dodge.z);
         apostle.hasImpulse = true;
         dodgeCooldown = apostle instanceof NightKinEntity ? 8 : 13;
         pathCooldown = 5;
         chooseCircleDirection();
         return true;
      }

      private boolean isHostileProjectile(Projectile projectile) {
         Entity owner = projectile.getOwner();
         if (owner == apostle) return false;
         return !(owner instanceof LivingEntity living && apostle.isAlliedTo(living));
      }

      private boolean trySpecialApproach(LivingEntity target, double distance) {
         if (approachCooldown > 0 || !apostle.onGround() || distance < 3.5 || distance > 16.0) return false;
         Vec3 horizontal = target.position().subtract(apostle.position()).multiply(1.0, 0.0, 1.0);
         if (horizontal.lengthSqr() < 1.0E-5) return false;
         Vec3 direction = horizontal.normalize();
         boolean leap = distance <= 9.0 && apostle.getRandom().nextBoolean();
         if (leap) {
            double forward = apostle instanceof NightKinEntity ? 0.88 : 0.72;
            double upward = apostle instanceof NightKinEntity ? 0.52 : 0.44;
            apostle.setDeltaMovement(direction.x * forward, upward, direction.z * forward);
            approachCooldown = apostle instanceof NightKinEntity ? 24 : 32;
         } else {
            double dash = apostle instanceof NightKinEntity ? 1.18 : 0.96;
            apostle.setDeltaMovement(direction.x * dash, 0.10, direction.z * dash);
            approachCooldown = apostle instanceof NightKinEntity ? 18 : 25;
         }
         apostle.getNavigation().stop();
         apostle.hasImpulse = true;
         pathCooldown = 7;
         return true;
      }

      private double combatSpeed() {
         if (apostle instanceof NightKinEntity) return 1.42;
         if (apostle instanceof LivingDeadEntity) return 1.32;
         if (apostle instanceof GhoulEntity) return 1.20;
         return 1.14;
      }

      private void chooseCircleDirection() {
         circleDirection = apostle.getRandom().nextBoolean() ? 1.0F : -1.0F;
         circleTicks = 18 + apostle.getRandom().nextInt(25);
      }
   }
}
