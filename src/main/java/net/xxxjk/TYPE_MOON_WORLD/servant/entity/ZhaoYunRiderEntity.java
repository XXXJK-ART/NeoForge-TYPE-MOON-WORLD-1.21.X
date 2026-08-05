package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.UUID;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.ZhaoYunHakuryuEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantAnimations;
import net.xxxjk.TYPE_MOON_WORLD.servant.zhaoyun.ZhaoYunDamageTypes;
import org.jetbrains.annotations.Nullable;

public final class ZhaoYunRiderEntity extends ServantEntity {
   public static final String SERVANT_KEY = "zhao_yun_rider";
   public static final String TAG_MOUNT = "ZhaoYunHakuryu";
   public static final String TAG_MOUNT_UNLOCKED = "ZhaoYunHakuryuUnlocked";
   public static final String TAG_PHASE = "ZhaoYunCombatPhase";
   public static final String TAG_NP_UNTIL = "ZhaoYunChangbanpoUntil";
   public static final String TAG_NP_COOLDOWN = "ZhaoYunChangbanpoCooldown";
   private static final String TAG_NP_CHANT_UNTIL = "ZhaoYunChangbanpoChantUntil";
   private static final String TAG_NP_INITIAL_CHARGE = "ZhaoYunChangbanpoInitialCharge";
   private static final String TAG_NP_INITIAL_DISTANCE = "ZhaoYunChangbanpoInitialDistance";
   private static final String TAG_NP_SAFE_X = "ZhaoYunChangbanpoSafeX";
   private static final String TAG_NP_SAFE_Y = "ZhaoYunChangbanpoSafeY";
   private static final String TAG_NP_SAFE_Z = "ZhaoYunChangbanpoSafeZ";
   private static final String TAG_NP_REASSESS_UNTIL = "ZhaoYunChangbanpoReassessUntil";
   private static final String TAG_AOKO_UNTIL = "ZhaoYunAokoSwordUntil";
   private static final String TAG_AOKO_COOLDOWN = "ZhaoYunAokoSwordCooldown";
   private static final String TAG_TECHNIQUE_COOLDOWN = "ZhaoYunTechniqueCooldown";
   private static final String TAG_TECHNIQUE_INDEX = "ZhaoYunTechniqueIndex";
   public static final String TAG_FORCE_MELEE_UNTIL = "ZhaoYunForceMeleeUntil";
   private static final String TAG_ORBIT_TICKS = "ZhaoYunOrbitTicks";
   private static final String TAG_ORBIT_LAST_DISTANCE = "ZhaoYunOrbitLastDistance";
   private static final String TAG_ORBIT_TARGET = "ZhaoYunOrbitTarget";
   private static final int NP_DURATION = 300;
   private static final int NP_CHANT_DURATION = 60;
   private static final int NP_COOLDOWN = 800;
   private static final float NP_INITIAL_DAMAGE = 200.0F;
   private static final double NP_INITIAL_DISTANCE = 50.0;
   private static final float NP_DAMAGE = 100.0F;
   private static final double CHANGBANPO_MP_COST = 150.0;
   private static final int AOKO_DURATION = 300;
   private static final int AOKO_COOLDOWN = 400;
   private static final int AOKO_SECOND_STRIKE_DELAY = 10;
   private static final float AOKO_SECOND_STRIKE_BASE_DAMAGE = 20.0F;
   private static final ResourceLocation RIDING_ARMOR_MODIFIER =
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "zhao_yun_riding_armor");
   private static final EntityDataAccessor<Integer> COMBAT_PHASE = SynchedEntityData.defineId(
      ZhaoYunRiderEntity.class, EntityDataSerializers.INT);
   @Nullable private UUID mountUuid;
   private final Set<UUID> npContactTargets = new HashSet<>();
   private final List<PendingAokoStrike> pendingAokoStrikes = new ArrayList<>();

   public ZhaoYunRiderEntity(EntityType<? extends ZhaoYunRiderEntity> type, Level level) {
      super(type, level, SERVANT_KEY);
   }

   @Override
   public void stopRiding() {
      Entity vehicle = getVehicle();
      super.stopRiding();
      if (vehicle instanceof ZhaoYunHakuryuEntity mount && mount.isAlive()) {
         // Hakuryu is summoned for Zhao Yun's current ride. Once he gets off,
         // remove that mount instead of leaving a stray persistent horse.
         onHakuryuDismounted(mount);
         mount.discard();
      }
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      if (isChangbanpoActive()) {
         amount *= 0.05F;
      }
      return super.hurt(source, amount);
   }

   @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(COMBAT_PHASE, 1);
   }

   @Override public void tick() {
      super.tick();
      alignMountedPose();
      if (!(level() instanceof ServerLevel server)) return;
      int nextPhase = Math.max(getCombatPhase(), computeCombatPhase());
      if (nextPhase != getCombatPhase()) setCombatPhase(nextPhase);
      ensureHakuryu(server);
      updateRidingArmor();
      tickAokoSecondStrikes(server);
      tickZhaoYunSkills(server);
      tickZhaoYunTechniques(server);
      if (!isChangbanpoCasting() && !isAokoSwordActive() && getCombatPhase() >= 2
         && getTarget() != null && getTarget().isAlive()
         && distanceToSqr(getTarget()) <= 64.0 && server.getGameTime() % 40L == 0L) {
         startAokoSword();
      }
      if (isChangbanpoCharging()) {
         // Chanting is only an audio/visual invocation. Zhao Yun remains free
         // to walk (or ride an already-unlocked Hakuryu) until release.
         if (server.getGameTime() >= getPersistentData().getLong(TAG_NP_CHANT_UNTIL)) {
            releaseChangbanpo(server);
         }
      }
      if (!isChangbanpoCasting() && !isAokoSwordActive() && getCombatPhase() >= 2 && getTarget() != null && getTarget().isAlive()
         && distanceToSqr(getTarget()) > 9.0 && distanceToSqr(getTarget()) <= 1024.0
         && server.getGameTime() >= getPersistentData().getLong(TAG_NP_COOLDOWN)) {
         startChangbanpo();
      }
      long npUntil = getPersistentData().getLong(TAG_NP_UNTIL);
      if (npUntil > 0L && server.getGameTime() >= npUntil) {
         endChangbanpo();
      }
      alignMountedPose();
   }

   @Override protected void customServerAiStep() {
      if (isChangbanpoActive()) {
         // The mount owns the complete noble-phantasm movement and collision
         // loop. Prevent ordinary melee/navigation AI from competing with it.
         getNavigation().stop();
         setDeltaMovement(Vec3.ZERO);
         return;
      }
      super.customServerAiStep();
      if (isPassenger() && getVehicle() instanceof ZhaoYunHakuryuEntity) {
         // The mount owns movement while riding; keep the servant's tactical
         // controller from applying a separate navigation velocity.
         getNavigation().stop();
         setDeltaMovement(Vec3.ZERO);
      }
   }

   private void alignMountedPose() {
      if (!(getVehicle() instanceof ZhaoYunHakuryuEntity mount)) return;
      float yaw = mount.getYRot();
      setYRot(yaw);
      setYBodyRot(yaw);
      setYHeadRot(yaw);
      setXRot(0.0F);
   }

   private void updateRidingArmor() {
      AttributeInstance armor = getAttribute(Attributes.ARMOR);
      if (armor == null) return;
      AttributeModifier current = armor.getModifier(RIDING_ARMOR_MODIFIER);
      boolean active = isPassenger() && getPersistentData().getBoolean("RidingAPlusActive");
      if (!active) {
         if (current != null) armor.removeModifier(RIDING_ARMOR_MODIFIER);
         return;
      }
      double bonus = getPersistentData().getDouble("RidingAPlusArmorBonus");
      if (current != null && Math.abs(current.amount() - bonus) < 1.0E-6
         && current.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) return;
      if (current != null) armor.removeModifier(RIDING_ARMOR_MODIFIER);
      armor.addTransientModifier(new AttributeModifier(
         RIDING_ARMOR_MODIFIER, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
   }

   private void tickZhaoYunSkills(ServerLevel level) {
      if (isChangbanpoCasting()) return;
      long now = level.getGameTime();
      if (getHealth() <= getMaxHealth() * 0.5F) {
         if (!getPersistentData().getBoolean("ZhaoYunDragonGallActive")) {
            getPersistentData().putBoolean("ZhaoYunDragonGallActive", true);
            triggerNamedActionAnimation("dragon_gall");
            VFXServerEffects.spawnReplayable(level, "servant_zhao_yun_dragon_gall", this, 24.0F);
         }
         int amplifier = getCombatPhase() >= 3 ? 1 : 0;
         if (!hasEffect(MobEffects.DAMAGE_BOOST) || getEffect(MobEffects.DAMAGE_BOOST).getDuration() < 10
            || getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() != amplifier) {
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30, amplifier, true, false));
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, amplifier, true, false));
         }
      } else {
         getPersistentData().putBoolean("ZhaoYunDragonGallActive", false);
      }

      ServerPlayer master = getEntityMaster();
      if (master == null || !master.isAlive() || master.level() != level) {
         // Master-protection skills are unavailable for a masterless Zhao
         // Yun. Clear any stale rescue state left by a broken contract so it
         // cannot keep protecting an old player after unbinding.
         getPersistentData().remove("ZhaoYunRescueUntil");
         getPersistentData().remove("ZhaoYunRescueDefenseUntil");
      } else if (master.getHealth() <= master.getMaxHealth() * 0.5F
         && getCurrentMp() >= 20.0 && now >= getPersistentData().getLong("ZhaoYunRescueCooldown")) {
         setCurrentMp(getCurrentMp() - 20.0);
         getPersistentData().putLong("ZhaoYunRescueCooldown", now + 400L);
         getPersistentData().putLong("ZhaoYunRescueUntil", now + 1200L);
         // Resistance I plus the shared incoming-damage factor in
         // CommonEvents gives the intended 30% rescue defense.
         addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 0, true, false));
         master.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 0, true, false));
         getPersistentData().putLong("ZhaoYunRescueDefenseUntil", now + 1200L);
         master.getPersistentData().putLong("ZhaoYunRescueDefenseUntil", now + 1200L);
         triggerNamedActionAnimation("rescue");
         VFXServerEffects.spawnReplayable(level, "servant_zhao_yun_rescue", master, 32.0F);
      }

      if (getTarget() != null && getTarget().isAlive() && now >= getPersistentData().getLong("ZhaoYunSevenOutCooldown")) {
         int enemies = level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(6.0),
            entity -> entity != this && entity != getHakuryu() && entity.isAlive() && !isAlliedTo(entity)).size();
         if (enemies >= 2) {
            getPersistentData().putLong("ZhaoYunSevenOutCooldown", now + 60L);
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 1, true, false));
            triggerNamedActionAnimation("seven_out");
            VFXServerEffects.spawnReplayable(level, "servant_zhao_yun_seven_out", this, 32.0F);
         }
      }
   }

   /** Resolves Qinggang Sword follow-up hits after their half-second delay. */
   private void tickAokoSecondStrikes(ServerLevel level) {
      if (pendingAokoStrikes.isEmpty()) return;
      long now = level.getGameTime();
      Iterator<PendingAokoStrike> iterator = pendingAokoStrikes.iterator();
      while (iterator.hasNext()) {
         PendingAokoStrike pending = iterator.next();
         if (pending.dueTick > now) continue;
         iterator.remove();
         Entity entity = level.getEntity(pending.targetId);
         if (!(entity instanceof LivingEntity target) || !target.isAlive()
            || target == this || target == getHakuryu() || target == getEntityMaster()
            || isAlliedTo(target) || EntityUtils.isImmunePlayerTarget(target)) continue;

         float damage = AOKO_SECOND_STRIKE_BASE_DAMAGE;
         target.invulnerableTime = 0;
         // Qinggang's follow-up is a genuine delayed attack: it bypasses
         // armor, but not evasion, i-frames, or Twelve Trials.
         target.hurt(damageSources().source(ZhaoYunDamageTypes.QINGGANG_SECOND_HIT, this), damage);
         level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.55,
            target.getZ(), 8, 0.18, 0.25, 0.18, 0.02);
         level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT,
            SoundSource.HOSTILE, 0.7F, 1.35F);
      }
   }

   private void queueAokoSecondStrike(LivingEntity target) {
      if (!(level() instanceof ServerLevel level)) return;
      pendingAokoStrikes.add(new PendingAokoStrike(target.getUUID(), level.getGameTime() + AOKO_SECOND_STRIKE_DELAY));
   }

   private static final class PendingAokoStrike {
      private final UUID targetId;
      private final long dueTick;

      private PendingAokoStrike(UUID targetId, long dueTick) {
         this.targetId = targetId;
         this.dueTick = dueTick;
      }
   }

   /** Small, unnamed weapon techniques shared by the normal combat AI. */
   private void tickZhaoYunTechniques(ServerLevel level) {
      if (isChangbanpoCasting()) return;
      LivingEntity target = getTarget();
      if (target == null || !target.isAlive() || isAlliedTo(target)
         || EntityUtils.isImmunePlayerTarget(target)) return;
      long now = level.getGameTime();
      if (getPersistentData().getLong(TAG_FORCE_MELEE_UNTIL) > now) return;
      if (now < getPersistentData().getLong(TAG_TECHNIQUE_COOLDOWN)) return;

      ZhaoYunHakuryuEntity mount = getVehicle() instanceof ZhaoYunHakuryuEntity value && value.isAlive() ? value : null;
      double distance = distanceTo(target);
      int index = getPersistentData().getInt(TAG_TECHNIQUE_INDEX) % 3;
      boolean used;
      if (mount != null && distance >= 2.0 && distance <= 10.0) {
         used = switch (index) {
            case 0 -> performMountedLanceCharge(level, mount, target);
            case 1 -> performMountedSweepingPass(level, mount, target);
            default -> performMountedDragonRein(level, mount, target);
         };
      } else if (mount == null && distance >= 1.8 && distance <= 8.0) {
         used = switch (index) {
            case 0 -> performSpearLungingThrust(level, target);
            case 1 -> performSpearDrivingSlash(level, target);
            default -> performSpearSweepingAdvance(level, target);
         };
      } else {
         return;
      }
      if (used) {
         getPersistentData().putInt(TAG_TECHNIQUE_INDEX, index + 1);
         getPersistentData().putLong(TAG_TECHNIQUE_COOLDOWN, now + (mount == null ? 42L : 34L));
      }
   }

   /** Dedicated combat loop used by CombatModule instead of the generic servant fallback. */
   public void tickDedicatedCombat(LivingEntity target, long now) {
      if (!(level() instanceof ServerLevel level) || target == null || !target.isAlive()
         || isAlliedTo(target) || EntityUtils.isImmunePlayerTarget(target)) {
         if (target != null && EntityUtils.isImmunePlayerTarget(target)) setTarget(null);
         return;
      }
      setTarget(target);
      getLookControl().setLookAt(target, 45.0F, 45.0F);
      if (isChangbanpoActive() || isPerformingAction()) return;

      double distance = distanceTo(target);
      updateOrbitRecovery(target, distance, now);
      boolean forceMelee = getPersistentData().getLong(TAG_FORCE_MELEE_UNTIL) > now;
      if (isChangbanpoCharging()) {
         if (!isPassenger() && distance > 4.0) getNavigation().moveTo(target, 1.15);
         return;
      }
      if (distance <= 5.5 && now - getPersistentData().getLong("ZhaoYunLastBasicAttack") >= 10L) {
         getPersistentData().putLong("ZhaoYunLastBasicAttack", now);
         resetOrbitRecovery();
         triggerAttackSwing();
         doHurtTarget(target);
         return;
      }
      if (forceMelee) {
         // Break out of high-speed circling and deliberately close the
         // distance for a short, stable point-blank melee window.
         Vec3 toward = horizontalDirection(target);
         if (toward != null) {
            if (isPassenger() && getVehicle() instanceof ZhaoYunHakuryuEntity mount) {
               mount.setYRot((float)(Math.atan2(-toward.x, toward.z) * 180.0 / Math.PI));
               mount.setDeltaMovement(toward.scale(Math.min(0.62,
                  Math.max(0.42, mount.getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.15)))
                  .add(0.0, mount.getDeltaMovement().y, 0.0));
               mount.hasImpulse = true;
            } else {
               getNavigation().moveTo(target, 0.78);
            }
         }
         return;
      }
      if (!isPassenger() && distance > 4.0) {
         getNavigation().moveTo(target, getCombatPhase() >= 3 ? 1.35 : 1.2);
      }
   }

   private void updateOrbitRecovery(LivingEntity target, double distance, long now) {
      CompoundTag data = getPersistentData();
      if (!data.hasUUID(TAG_ORBIT_TARGET) || !data.getUUID(TAG_ORBIT_TARGET).equals(target.getUUID())) {
         data.putUUID(TAG_ORBIT_TARGET, target.getUUID());
         data.putDouble(TAG_ORBIT_LAST_DISTANCE, distance);
         data.putInt(TAG_ORBIT_TICKS, 0);
         return;
      }
      double previousDistance = data.getDouble(TAG_ORBIT_LAST_DISTANCE);
      boolean stableBand = distance >= 3.0 && distance <= 11.0
         && Math.abs(distance - previousDistance) < 0.22;
      int orbitTicks = data.getInt(TAG_ORBIT_TICKS);
      if (stableBand) {
         orbitTicks++;
      } else {
         orbitTicks = Math.max(0, orbitTicks - 3);
      }
      data.putDouble(TAG_ORBIT_LAST_DISTANCE, distance);
      if (orbitTicks >= 100) {
         data.putLong(TAG_FORCE_MELEE_UNTIL, now + 100L);
         data.putLong(TAG_TECHNIQUE_COOLDOWN, now + 100L);
         orbitTicks = 0;
      }
      data.putInt(TAG_ORBIT_TICKS, orbitTicks);
   }

   private void resetOrbitRecovery() {
      CompoundTag data = getPersistentData();
      data.putInt(TAG_ORBIT_TICKS, 0);
      data.remove(TAG_FORCE_MELEE_UNTIL);
   }

   private boolean performSpearLungingThrust(ServerLevel level, LivingEntity target) {
      Vec3 direction = horizontalDirection(target);
      if (direction == null) return false;
      faceVector(direction);
      setDeltaMovement(direction.x * 1.35, getDeltaMovement().y, direction.z * 1.35);
      hasImpulse = true;
      triggerNamedActionAnimation("spear_lunging_thrust");
      hurtTechniqueTargets(level, getBoundingBox().expandTowards(direction.scale(4.5)).inflate(0.8, 0.7, 0.8),
         getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.45, direction, 1.05, 0.3);
      spawnTechniqueFx(level, direction, ParticleTypes.CRIT);
      return true;
   }

   private boolean performSpearDrivingSlash(ServerLevel level, LivingEntity target) {
      Vec3 direction = horizontalDirection(target);
      if (direction == null) return false;
      faceVector(direction);
      setDeltaMovement(direction.x * 1.15, getDeltaMovement().y, direction.z * 1.15);
      hasImpulse = true;
      triggerNamedActionAnimation("spear_driving_slash");
      hurtTechniqueTargets(level, getBoundingBox().expandTowards(direction.scale(3.9)).inflate(1.15, 0.85, 1.15),
         getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.55, direction, 0.85, 0.22);
      spawnTechniqueFx(level, direction, ParticleTypes.SWEEP_ATTACK);
      return true;
   }

   private boolean performSpearSweepingAdvance(ServerLevel level, LivingEntity target) {
      Vec3 direction = horizontalDirection(target);
      if (direction == null) return false;
      faceVector(direction);
      setDeltaMovement(direction.x * 0.95, getDeltaMovement().y, direction.z * 0.95);
      hasImpulse = true;
      triggerNamedActionAnimation("spear_sweeping_advance");
      hurtTechniqueTargets(level, getBoundingBox().expandTowards(direction.scale(3.5)).inflate(1.6, 0.8, 1.6),
         getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.25, direction, 0.7, 0.2);
      spawnTechniqueFx(level, direction, ParticleTypes.CLOUD);
      return true;
   }

   private boolean performMountedLanceCharge(ServerLevel level, ZhaoYunHakuryuEntity mount, LivingEntity target) {
      Vec3 direction = horizontalDirection(target);
      if (direction == null) return false;
      mount.setYRot((float)(Math.atan2(-direction.x, direction.z) * 180.0 / Math.PI));
      double speed = mount.getAttributeValue(Attributes.MOVEMENT_SPEED) * 4.8;
      mount.setDeltaMovement(direction.x * speed, mount.getDeltaMovement().y, direction.z * speed);
      mount.hasImpulse = true;
      triggerNamedActionAnimation("mounted_lance_charge");
      hurtTechniqueTargets(level, mount.getBoundingBox().expandTowards(direction.scale(5.2)).inflate(1.0, 0.8, 1.0),
         getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.8, direction, 1.35, 0.35);
      spawnMountedTechniqueFx(level, mount, direction, ParticleTypes.CRIT);
      return true;
   }

   private boolean performMountedSweepingPass(ServerLevel level, ZhaoYunHakuryuEntity mount, LivingEntity target) {
      Vec3 direction = horizontalDirection(target);
      if (direction == null) return false;
      mount.setYRot((float)(Math.atan2(-direction.x, direction.z) * 180.0 / Math.PI));
      double speed = mount.getAttributeValue(Attributes.MOVEMENT_SPEED) * 3.3;
      mount.setDeltaMovement(direction.x * speed, mount.getDeltaMovement().y, direction.z * speed);
      mount.hasImpulse = true;
      triggerNamedActionAnimation("mounted_sweeping_pass");
      hurtTechniqueTargets(level, mount.getBoundingBox().expandTowards(direction.scale(4.3)).inflate(1.8, 0.9, 1.8),
         getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.35, direction, 1.0, 0.25);
      spawnMountedTechniqueFx(level, mount, direction, ParticleTypes.SWEEP_ATTACK);
      return true;
   }

   private boolean performMountedDragonRein(ServerLevel level, ZhaoYunHakuryuEntity mount, LivingEntity target) {
      triggerNamedActionAnimation("mounted_dragon_rein");
      AABB area = mount.getBoundingBox().inflate(3.6, 1.0, 3.6);
      hurtTechniqueTargets(level, area, getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.15,
         horizontalDirection(target), 0.8, 0.3);
      level.sendParticles(ParticleTypes.DRAGON_BREATH, mount.getX(), mount.getY() + 0.7, mount.getZ(),
         18, 1.4, 0.35, 1.4, 0.04);
      level.playSound(null, mount.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.2F, 0.65F);
      return true;
   }

   @Nullable
   private Vec3 horizontalDirection(LivingEntity target) {
      Vec3 delta = target.position().subtract(position());
      Vec3 horizontal = new Vec3(delta.x, 0.0, delta.z);
      return horizontal.lengthSqr() < 1.0E-4 ? null : horizontal.normalize();
   }

   private void hurtTechniqueTargets(ServerLevel level, AABB box, double damage, @Nullable Vec3 direction,
                                     double knockback, double verticalKnockback) {
      for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box,
         entity -> entity != this && entity != getHakuryu() && entity.isAlive()
            && !isAlliedTo(entity) && entity != getEntityMaster()
            && !EntityUtils.isImmunePlayerTarget(entity))) {
         victim.hurt(damageSources().mobAttack(this), (float)damage);
         Vec3 push = direction != null ? direction : victim.position().subtract(position()).multiply(1.0, 0.0, 1.0).normalize();
         if (push.lengthSqr() > 1.0E-4) {
            victim.push(push.x * knockback, verticalKnockback, push.z * knockback);
            victim.hurtMarked = true;
         }
      }
   }

   private void spawnTechniqueFx(ServerLevel level, Vec3 direction, net.minecraft.core.particles.SimpleParticleType particle) {
      level.sendParticles(particle, getX() + direction.x * 2.2, getY() + getBbHeight() * 0.55,
         getZ() + direction.z * 2.2, 5, 0.15, 0.2, 0.15, 0.02);
      level.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0F, 1.1F);
   }

   private void spawnMountedTechniqueFx(ServerLevel level, ZhaoYunHakuryuEntity mount, Vec3 direction,
                                        net.minecraft.core.particles.SimpleParticleType particle) {
      level.sendParticles(particle, mount.getX() + direction.x * 2.6, mount.getY() + 1.0,
         mount.getZ() + direction.z * 2.6, 8, 0.25, 0.3, 0.25, 0.03);
      level.sendParticles(ParticleTypes.CLOUD, mount.getX(), mount.getY() + 0.2, mount.getZ(),
         12, 0.7, 0.15, 0.7, 0.04);
      level.playSound(null, mount.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.25F, 0.85F);
   }

   @Override public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(
                                       net.minecraft.world.level.ServerLevelAccessor level,
                                       net.minecraft.world.DifficultyInstance difficulty,
                                       net.minecraft.world.entity.MobSpawnType spawnType,
                                       @Nullable net.minecraft.world.entity.SpawnGroupData groupData) {
      net.minecraft.world.entity.SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, groupData);
      setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(ModItems.YAJIAO_QIANG.get()));
      setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0.0F);
      return result;
   }

   private void ensureHakuryu(ServerLevel level) {
      // Hakuryu is first materialized by the Noble Phantasm. Once unlocked it
      // remains persistent and is recreated if the entity is lost.
      if (!getPersistentData().getBoolean(TAG_MOUNT_UNLOCKED)) return;
      if (mountUuid != null && level.getEntity(mountUuid) instanceof ZhaoYunHakuryuEntity mount && mount.isAlive()) {
         mount.bindRider(this, getEntityMaster());
         if (getPersistentData().getBoolean(TAG_MOUNT_UNLOCKED)
            && !isPassenger() && !mount.getPassengers().contains(this)) {
            startRiding(mount, true);
         }
         return;
      }
      ZhaoYunHakuryuEntity mount = ModEntities.ZHAO_YUN_HAKURYU.get().create(level);
      if (mount == null) return;
      mount.moveTo(getX(), getY(), getZ(), getYRot(), 0.0F);
      mount.setHealth(mount.getMaxHealth());
      mount.bindRider(this, getEntityMaster());
      if (!level.addFreshEntity(mount)) return;
      mountUuid = mount.getUUID();
      if (getPersistentData().getBoolean(TAG_MOUNT_UNLOCKED)) startRiding(mount, true);
   }

   @Nullable public ZhaoYunHakuryuEntity getHakuryu() {
      if (!(level() instanceof ServerLevel level) || mountUuid == null) return null;
      Entity entity = level.getEntity(mountUuid);
      return entity instanceof ZhaoYunHakuryuEntity mount ? mount : null;
   }

   public boolean isChangbanpoActive() {
      return getPersistentData().getLong(TAG_NP_UNTIL) > level().getGameTime();
   }

   public boolean isChangbanpoCharging() {
      long chantUntil = getPersistentData().getLong(TAG_NP_CHANT_UNTIL);
      return !isChangbanpoActive() && chantUntil > 0L && chantUntil >= level().getGameTime();
   }

   public boolean isChangbanpoCasting() {
      return isChangbanpoCharging() || isChangbanpoActive();
   }

   public boolean isAokoSwordActive() {
      return getPersistentData().getLong(TAG_AOKO_UNTIL) > level().getGameTime();
   }

   public boolean startAokoSword() {
      if (!(level() instanceof ServerLevel level) || isChangbanpoCasting() || isAokoSwordActive()
         || getCurrentMp() < 50.0 || !hasMasterNoblePhantasmPermission()
         || level.getGameTime() < getPersistentData().getLong(TAG_AOKO_COOLDOWN)) return false;
      long now = level.getGameTime();
      setCurrentMp(getCurrentMp() - 50.0);
      getPersistentData().putLong(TAG_AOKO_UNTIL, now + AOKO_DURATION);
      getPersistentData().putLong(TAG_AOKO_COOLDOWN, now + AOKO_COOLDOWN);
      // Qinggang Sword has no voice line; use only a brief weapon flourish.
      triggerNamedActionAnimation("spear_flourish");
      level.sendParticles(ParticleTypes.CRIT, getX(), getY() + getBbHeight() * 0.6, getZ(),
         12, 0.3, 0.45, 0.3, 0.03);
      return true;
   }

   /** Public name used by action/AI integrations. */
   public boolean startQinggangSword() {
      return startAokoSword();
   }

   public boolean startChangbanpo() {
      if (!(level() instanceof ServerLevel level) || isChangbanpoCasting() || isAokoSwordActive() || getCurrentMp() < CHANGBANPO_MP_COST
         || !hasMasterNoblePhantasmPermission()
         || level.getGameTime() < getPersistentData().getLong(TAG_NP_COOLDOWN)) return false;
      setCurrentMp(getCurrentMp() - CHANGBANPO_MP_COST);
      long now = level.getGameTime();
      getPersistentData().putLong(TAG_NP_CHANT_UNTIL, now + NP_CHANT_DURATION);
      getPersistentData().putLong(TAG_NP_COOLDOWN, level.getGameTime() + NP_COOLDOWN);
      ServantVoiceHelper.tryPlayZhaoYunNp(this);
      return true;
   }

   private void releaseChangbanpo(ServerLevel level) {
      if (!isChangbanpoCharging()) return;
      getPersistentData().remove(TAG_NP_CHANT_UNTIL);
      getPersistentData().putBoolean(TAG_MOUNT_UNLOCKED, true);
      ensureHakuryu(level);
      getPersistentData().putBoolean(TAG_NP_INITIAL_CHARGE, true);
      getPersistentData().putDouble(TAG_NP_INITIAL_DISTANCE, 0.0);
      ZhaoYunHakuryuEntity initialMount = getHakuryu();
      if (initialMount != null) rememberNpSafePosition(initialMount);
      npContactTargets.clear();
      long now = level.getGameTime();
      getPersistentData().putLong(TAG_NP_UNTIL, now + NP_DURATION);
      if (initialMount != null) {
         initialMount.setNpActive(true);
         if (!isPassenger()) startRiding(initialMount, true);
      }
      triggerNamedActionAnimation("changbanpo_no_ikki_gake");
      // Bind the full effect to Hakuryu itself. The mount owns the charge
      // movement, so every continuously emitted dragon/beam trail follows it.
      VFXServerEffects.spawnReplayable(level, "servant_zhao_yun_changbanpo", initialMount, NP_DURATION / 20.0F);
   }

   public void onHakuryuDeath(ZhaoYunHakuryuEntity mount) {
      if (mountUuid == null || !mountUuid.equals(mount.getUUID())) return;
      mountUuid = null;
      getPersistentData().putBoolean(TAG_MOUNT_UNLOCKED, false);
      if (isChangbanpoActive()) endChangbanpo();
   }

   public void onHakuryuDismounted(ZhaoYunHakuryuEntity mount) {
      if (mountUuid != null && mountUuid.equals(mount.getUUID())) {
         mountUuid = null;
         getPersistentData().putBoolean(TAG_MOUNT_UNLOCKED, false);
      }
      if (isChangbanpoCasting()) endChangbanpo();
   }

   public void endChangbanpo() {
      getPersistentData().remove(TAG_NP_UNTIL);
      getPersistentData().remove(TAG_NP_CHANT_UNTIL);
      getPersistentData().remove(TAG_NP_INITIAL_CHARGE);
      getPersistentData().remove(TAG_NP_INITIAL_DISTANCE);
      getPersistentData().remove(TAG_NP_REASSESS_UNTIL);
      npContactTargets.clear();
      if (getHakuryu() != null) getHakuryu().setNpActive(false);
      getPersistentData().remove(TAG_NP_SAFE_X);
      getPersistentData().remove(TAG_NP_SAFE_Y);
      getPersistentData().remove(TAG_NP_SAFE_Z);
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      boolean hit = super.doHurtTarget(target);
      if (hit && target instanceof LivingEntity living && isAokoSwordActive()
         && living.isAlive() && !isAlliedTo(living) && living != getEntityMaster()) {
         queueAokoSecondStrike(living);
      }
      return hit;
   }

   public boolean isRescueProtecting(long gameTime) {
      return getPersistentData().getLong("ZhaoYunRescueUntil") > gameTime;
   }

   public void tickChangbanpoMount(ZhaoYunHakuryuEntity mount) {
      if (!(level() instanceof ServerLevel level)) return;
      if (mount.onGround() || hasSolidSupport(level, mount)) rememberNpSafePosition(mount);
      CompoundTag data = getPersistentData();
      long now = level.getGameTime();
      long reassessUntil = data.getLong(TAG_NP_REASSESS_UNTIL);
      if (reassessUntil > now) {
         // The opening dash ends with a deliberate one-second halt. Keep the
         // mount grounded and visible while the tactical target is refreshed.
         Vec3 current = mount.getDeltaMovement();
         mount.setDeltaMovement(0.0, current.y, 0.0);
         return;
      }
      if (reassessUntil > 0L) {
         data.remove(TAG_NP_REASSESS_UNTIL);
         retargetChangbanpo(level, mount);
      }
      double initialDistance = data.getDouble(TAG_NP_INITIAL_DISTANCE);
      boolean initialDash = initialDistance < NP_INITIAL_DISTANCE;
      LivingEntity target = getTarget();
      if (!initialDash && (target == null || !target.isAlive())) {
         // Once the bounded opening dash is over, do not keep driving into the
         // distance when the last target has disappeared.
         Vec3 current = mount.getDeltaMovement();
         mount.setDeltaMovement(0.0, current.y, 0.0);
         return;
      }
      if (target != null && target.isAlive()) {
         Vec3 towardTarget = target.position().subtract(mount.position());
         Vec3 horizontalTarget = new Vec3(towardTarget.x, 0.0, towardTarget.z);
         if (horizontalTarget.lengthSqr() > 1.0E-4) {
            mount.setYRot((float)(Math.atan2(-horizontalTarget.x, horizontalTarget.z) * 180.0 / Math.PI));
         }
      }
      // Read yaw only. Pitch must never turn the charge into an upward/downward
      // flight path or let the mount fall into the void.
      float yaw = mount.getYRot() * ((float)Math.PI / 180.0F);
      Vec3 flat = new Vec3(-net.minecraft.util.Mth.sin(yaw), 0.0, net.minecraft.util.Mth.cos(yaw));
      if (flat.lengthSqr() < 1.0E-4) return;
      flat = flat.normalize();
      boolean initialCharge = getPersistentData().getBoolean(TAG_NP_INITIAL_CHARGE);
      // Move the mount explicitly below. Do not leave horizontal velocity set,
      // otherwise PathfinderMob.tick() applies the previous step a second time
      // on the next tick and the rider can shoot out of tracking range.
      double speed = Math.min(0.95, mount.getAttributeValue(Attributes.MOVEMENT_SPEED) * 2.6);
      double travel = speed;
      if (initialDistance < NP_INITIAL_DISTANCE) {
         travel = Math.min(speed, NP_INITIAL_DISTANCE - initialDistance);
      }
      Vec3 delta = flat.scale(travel);
      AABB nextBox = mount.getBoundingBox().move(delta);
      if (containsBedrock(level, mount.getBoundingBox(), delta)) {
         endChangbanpo();
         return;
      }
      // Do not destroy the block directly below the mount: removing its
      // support while gravity is active is what caused high-ground launches
      // to fall into the void.
      breakChangbanpoBlocks(level, mount, delta);
      mount.setYRot((float)(Math.atan2(-flat.x, flat.z) * 180.0 / Math.PI));
      double verticalVelocity = mount.getDeltaMovement().y;
      mount.move(net.minecraft.world.entity.MoverType.SELF, delta);
      // The horizontal step was already applied by move(). Keeping it in
      // delta movement would make the base entity tick apply the same step
      // again before the next Changbanpo update.
      mount.setDeltaMovement(0.0, verticalVelocity, 0.0);
      if (mount.horizontalCollision && mount.onGround()) {
         mount.jumpWithZhaoYunPower();
      }
      if (initialDistance < NP_INITIAL_DISTANCE) {
         double updatedDistance = initialDistance + travel;
         data.putDouble(TAG_NP_INITIAL_DISTANCE, updatedDistance);
         if (updatedDistance >= NP_INITIAL_DISTANCE - 1.0E-4) {
            // The 200-damage opening dash ends at 50 blocks. Any later
            // contacts during the remaining 15-second NP window deal 100.
            data.putBoolean(TAG_NP_INITIAL_CHARGE, false);
            data.putLong(TAG_NP_REASSESS_UNTIL, now + 20L);
            setTarget(null);
            Vec3 current = mount.getDeltaMovement();
            mount.setDeltaMovement(0.0, current.y, 0.0);
         }
      }
      if (mount.getY() < level.getMinBuildHeight() - 4) {
         restoreNpSafePosition(mount);
         endChangbanpo();
         return;
      }
      AABB hitBox = mount.getBoundingBox().minmax(nextBox).inflate(2.2, 1.4, 2.2);
      ServerPlayer master = getEntityMaster();
      if (master != null && !master.isPassenger() && mount.getPassengers().size() < 2
         && hitBox.intersects(master.getBoundingBox())) {
         master.startRiding(mount, true);
      }
      Set<UUID> contactsThisTick = new HashSet<>();
      for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, hitBox,
         entity -> entity != this && entity != mount && entity.isAlive() && entity != getEntityMaster()
            && !isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(entity))) {
         UUID victimId = victim.getUUID();
         contactsThisTick.add(victimId);
         if (!npContactTargets.contains(victimId)) {
            victim.invulnerableTime = 0;
            float collisionDamage = initialCharge ? NP_INITIAL_DAMAGE : NP_DAMAGE;
            // Route collision damage through the normal damage event. Direct
            // setHealth() bypassed Twelve Trials, Battle Continuation, dodge,
            // and other servant-specific defensive rules.
            victim.hurt(damageSources().mobAttack(this), collisionDamage);
            victim.invulnerableTime = 0;
         }
      }
      npContactTargets.retainAll(contactsThisTick);
      npContactTargets.addAll(contactsThisTick);
   }

   private void retargetChangbanpo(ServerLevel level, ZhaoYunHakuryuEntity mount) {
      LivingEntity nearest = null;
      double nearestDistance = Double.MAX_VALUE;
      AABB search = mount.getBoundingBox().inflate(48.0, 12.0, 48.0);
      for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, search,
         entity -> entity != this && entity != mount && entity.isAlive()
            && entity != getEntityMaster() && !isAlliedTo(entity)
            && !EntityUtils.isImmunePlayerTarget(entity))) {
         double distance = mount.distanceToSqr(candidate);
         if (distance < nearestDistance) {
            nearest = candidate;
            nearestDistance = distance;
         }
      }
      setTarget(nearest);
   }

   private static boolean containsBedrock(ServerLevel level, AABB startBox, Vec3 delta) {
      // At NP speed a single-tick box check can tunnel through a one-block bedrock wall.
      int steps = Math.max(1, (int) Math.ceil(delta.length() / 0.45));
      for (int step = 0; step <= steps; step++) {
         double progress = (double) step / steps;
         AABB box = startBox.move(delta.scale(progress)).inflate(0.04);
         BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
         BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
         for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.BEDROCK)) return true;
         }
      }
      return false;
   }

   private static void breakChangbanpoBlocks(ServerLevel level, ZhaoYunHakuryuEntity mount, Vec3 desired) {
      Vec3 forward = new Vec3(desired.x, 0.0, desired.z);
      if (forward.lengthSqr() < 1.0E-4) return;
      forward = forward.normalize();
      BlockPos base = mount.blockPosition();
      int broken = 0;
      int maxBroken = 80;
      // Break a broad corridor ahead of the horse, matching Pegasus' charge
      // behavior instead of only removing blocks at the final tick position.
      for (int distance = 0; distance < 7 && broken < maxBroken; distance++) {
         BlockPos check = base.offset((int)Math.round(forward.x * (distance + 1)), 0,
            (int)Math.round(forward.z * (distance + 1)));
         for (BlockPos pos : BlockPos.betweenClosed(check.offset(-2, -1, -2), check.offset(2, 4, 2))) {
            // Preserve the support directly beneath the ground mount; the
            // corridor starts at its feet and extends forward/upward.
            if (pos.getY() < base.getY()) continue;
            BlockState state = level.getBlockState(pos);
            float hardness = state.getDestroySpeed(level, pos);
            if (!state.isAir() && !state.is(Blocks.BEDROCK) && hardness >= 0.0F && hardness < 75.0F) {
               level.removeBlock(pos, false);
               if (++broken >= maxBroken) break;
            }
         }
      }
   }

   private void rememberNpSafePosition(ZhaoYunHakuryuEntity mount) {
      getPersistentData().putDouble(TAG_NP_SAFE_X, mount.getX());
      getPersistentData().putDouble(TAG_NP_SAFE_Y, mount.getY());
      getPersistentData().putDouble(TAG_NP_SAFE_Z, mount.getZ());
   }

   private void restoreNpSafePosition(ZhaoYunHakuryuEntity mount) {
      CompoundTag data = getPersistentData();
      if (!data.contains(TAG_NP_SAFE_X) || !data.contains(TAG_NP_SAFE_Y) || !data.contains(TAG_NP_SAFE_Z)) {
         mount.setDeltaMovement(Vec3.ZERO);
         return;
      }
      mount.teleportTo(data.getDouble(TAG_NP_SAFE_X), data.getDouble(TAG_NP_SAFE_Y), data.getDouble(TAG_NP_SAFE_Z));
      mount.setDeltaMovement(Vec3.ZERO);
      mount.fallDistance = 0.0F;
   }

   private static boolean hasSolidSupport(ServerLevel level, ZhaoYunHakuryuEntity mount) {
      double y = mount.getY() - 0.08;
      for (double xOffset : new double[]{-0.75, 0.0, 0.75}) {
         for (double zOffset : new double[]{-0.75, 0.0, 0.75}) {
            BlockPos below = BlockPos.containing(mount.getX() + xOffset, y, mount.getZ() + zOffset);
            if (level.getBlockState(below).isSolidRender(level, below)) return true;
         }
      }
      return false;
   }

   @Override
   protected String getLoopAnimationOverride(ServantAnimations animations, boolean moving) {
      if (isPassenger() && getVehicle() instanceof ZhaoYunHakuryuEntity) return "riding";
      return super.getLoopAnimationOverride(animations, moving);
   }

   public int getCombatPhase() { return entityData.get(COMBAT_PHASE); }

   public void setCombatPhase(int phase) { entityData.set(COMBAT_PHASE, Math.max(1, Math.min(3, phase))); }

   private int computeCombatPhase() {
      float ratio = getHealth() / Math.max(1.0F, getMaxHealth());
      return ratio <= 0.333F ? 3 : ratio <= 0.666F ? 2 : 1;
   }

   @Override public boolean isAlliedTo(Entity other) {
      return super.isAlliedTo(other) || other == getHakuryu()
         || getEntityMaster() != null && (other == getEntityMaster() || getEntityMaster().isAlliedTo(other));
   }

   @Override public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      if (mountUuid != null) tag.putUUID(TAG_MOUNT, mountUuid);
      tag.putBoolean(TAG_MOUNT_UNLOCKED, getPersistentData().getBoolean(TAG_MOUNT_UNLOCKED));
      tag.putInt(TAG_PHASE, getCombatPhase());
      tag.putLong(TAG_NP_UNTIL, getPersistentData().getLong(TAG_NP_UNTIL));
      tag.putLong(TAG_NP_CHANT_UNTIL, getPersistentData().getLong(TAG_NP_CHANT_UNTIL));
      tag.putDouble(TAG_NP_INITIAL_DISTANCE, getPersistentData().getDouble(TAG_NP_INITIAL_DISTANCE));
      tag.putLong(TAG_NP_COOLDOWN, getPersistentData().getLong(TAG_NP_COOLDOWN));
   }

   @Override public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      if (tag.hasUUID(TAG_MOUNT)) mountUuid = tag.getUUID(TAG_MOUNT);
      getPersistentData().putBoolean(TAG_MOUNT_UNLOCKED, tag.getBoolean(TAG_MOUNT_UNLOCKED));
      setCombatPhase(tag.contains(TAG_PHASE) ? tag.getInt(TAG_PHASE) : computeCombatPhase());
      getPersistentData().putLong(TAG_NP_UNTIL, tag.getLong(TAG_NP_UNTIL));
      getPersistentData().putLong(TAG_NP_CHANT_UNTIL, tag.getLong(TAG_NP_CHANT_UNTIL));
      getPersistentData().putDouble(TAG_NP_INITIAL_DISTANCE, tag.getDouble(TAG_NP_INITIAL_DISTANCE));
      getPersistentData().putLong(TAG_NP_COOLDOWN, tag.getLong(TAG_NP_COOLDOWN));
   }
}
