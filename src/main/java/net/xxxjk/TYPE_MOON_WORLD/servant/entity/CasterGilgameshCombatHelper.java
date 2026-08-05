package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshGateWeaponProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.RoyalCannonProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantFlightHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterTargeting;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.GilgameshDivineShield;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSkillUtils;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import org.joml.Vector3f;

/** Server-side skills, flight and Royal Cannon state for Caster Gilgamesh. */
public final class CasterGilgameshCombatHelper {
   public static final String AMMO_TAG = "RoyalCannonAmmo";
   public static final String FIRING_TAG = "RoyalCannonFiring";
   public static final int MAX_AMMO = 5000;
   public static final int STARTING_AMMO = 500;
   public static final int CANNON_SHOTS_PER_ROUND = 30;
   public static final float CANNON_DAMAGE_PER_SHOT = 30.0F;
   public static final float CANNON_EXPLOSION_RADIUS = 3.0F;
   private static final String LAST_LEADER = "CasterGilgameshLastLeader";
   private static final String LAST_RETURN = "CasterGilgameshLastReturn";
   private static final String LEADER_UNTIL = "CasterGilgameshLeaderUntil";
   private static final String RETURN_UNTIL = "CasterGilgameshReturnUntil";
   private static final String LAST_HEAL = "CasterGilgameshLastHeal";
   private static final String LAST_MP = "CasterGilgameshLastMp";
   private static final String LAST_SHIELD = "CasterGilgameshLastShield";
   private static final String LAST_CANNON_ROUND = "CasterGilgameshLastCannonRound";
   private static final String LAST_GATE_OF_BABYLON = "CasterGilgameshLastGateOfBabylon";
   private static final String FLIGHT_LAST_CONTACT = "CasterGilgameshFlightLastContact";
   private static final String LAST_APPLIED_PHASE = "CasterGilgameshLastAppliedPhase";
   private static final String WORKSHOP_TYPE = "CasterGilgameshWorkshop";
   private static final String CENTER_X = "CasterGilgameshWorkshopX";
   private static final String CENTER_Y = "CasterGilgameshWorkshopY";
   private static final String CENTER_Z = "CasterGilgameshWorkshopZ";
   private static final String WORKSHOP_MANA_TICK = "CasterGilgameshWorkshopManaTick";
   private static final ResourceLocation LEADER_ATTACK_ID =
      ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "caster_gilgamesh_leader_attack");
   private static final ResourceLocation WORKSHOP_ARMOR_ID =
      ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "caster_gilgamesh_workshop_armor");
   private static final DustParticleOptions GOLD =
      new DustParticleOptions(new Vector3f(1.0F, 0.72F, 0.12F), 1.15F);
   private static final String[] GATE_WEAPONS = {"durandal", "gram", "vajra", "harpe", "fangtian_huaji", "pseudo_spiral_sword", "gae_bulg"};
   private static final long GATE_OF_BABYLON_COOLDOWN = 12L * 20L;
   private static final long FLIGHT_STALLED_TICKS = 8L * 20L;
   private static final double DIVINE_SHIELD_MP_COST = 30.0;
   private static final double DIVINE_SHIELD_DETECTION_RANGE = 24.0;

   private CasterGilgameshCombatHelper() {}

   public static void tick(CasterGilgameshEntity entity) {
      if (!(entity.level() instanceof ServerLevel level) || !entity.isAlive()) return;
      long now = level.getGameTime();
      CompoundTag data = entity.getPersistentData();
      initialize(entity, data);
      tickAmmo(entity, data);
      tickWorkshop(entity, level, data);
      tickTimedBuffs(entity, level, data, now);
      tickPhaseEntry(entity, level, data, now);
      tickDivineShield(entity, level);

      LivingEntity target = entity.getTarget();
      if (target == null || !target.isAlive() || entity.isAlliedTo(target)) {
         data.putBoolean(FIRING_TAG, false);
         entity.setFlyingMode(false);
         data.remove(FLIGHT_LAST_CONTACT);
         return;
      }

      updateFlight(entity, target, now, data);
      int phase = entity.getCombatPhase();
      if (tryGateOfBabylon(entity, level, target, data, now, phase)) {
         return;
      }
      if (now >= data.getLong(LAST_LEADER) + leaderCooldown(phase) && entity.getCurrentMp() >= 20.0) {
         useLeader(entity, level, data, now);
      }
      if (now >= data.getLong(LAST_RETURN) + returnCooldown(phase) && entity.getCurrentMp() >= 15.0) {
         useReturn(entity, level, data, now);
      }
      if (entity.distanceTo(target) < minimumCannonDistance(phase)) {
         data.putBoolean(FIRING_TAG, false);
         useEmergencyItem(entity, level, data, now);
      } else {
         data.putBoolean(FIRING_TAG, hasValidCannonResources(entity, target));
      }
      if (data.getBoolean(FIRING_TAG) && now >= data.getLong(LAST_CANNON_ROUND) + cannonInterval(phase)) {
         fireRoyalCannon(entity, level, target, data);
         data.putLong(LAST_CANNON_ROUND, now);
      }
   }

   private static void tickDivineShield(CasterGilgameshEntity entity, ServerLevel level) {
      GilgameshDivineShield.tick(entity);
      if (GilgameshDivineShield.isActive(entity)
         || entity.getCurrentMp() < DIVINE_SHIELD_MP_COST
         || GilgameshDivineShield.isOnCooldown(entity)) {
         return;
      }
      boolean incomingProjectile = !level.getEntitiesOfClass(
         Projectile.class,
         entity.getBoundingBox().inflate(DIVINE_SHIELD_DETECTION_RANGE),
         projectile -> isIncomingHostileProjectile(entity, projectile)
      ).isEmpty();
      if (!incomingProjectile) return;

      entity.setCurrentMp(entity.getCurrentMp() - DIVINE_SHIELD_MP_COST);
      GilgameshDivineShield.activate(entity);
   }

   private static boolean isIncomingHostileProjectile(CasterGilgameshEntity entity, Projectile projectile) {
      if (!projectile.isAlive() || projectile.getOwner() == entity) return false;
      Entity owner = projectile.getOwner();
      if (owner != null && (entity.isAlliedTo(owner) || owner.isAlliedTo(entity))) return false;

      Vec3 towardGilgamesh = entity.getBoundingBox().getCenter().subtract(projectile.position());
      Vec3 motion = projectile.getDeltaMovement();
      return motion.lengthSqr() < 1.0E-6 || motion.dot(towardGilgamesh) > 0.0;
   }

   private static void initialize(CasterGilgameshEntity entity, CompoundTag data) {
      if (!data.contains(AMMO_TAG)) data.putInt(AMMO_TAG, STARTING_AMMO);
      if (!data.contains(FIRING_TAG)) data.putBoolean(FIRING_TAG, false);
      if (!data.contains(LAST_LEADER)) data.putLong(LAST_LEADER, -700L);
      if (!data.contains(LAST_RETURN)) data.putLong(LAST_RETURN, -600L);
      if (!data.contains(LAST_CANNON_ROUND)) data.putLong(LAST_CANNON_ROUND, -20L);
      if (!data.contains(LAST_APPLIED_PHASE)) data.putInt(LAST_APPLIED_PHASE, 1);
      if (!data.getBoolean("CasterGilgameshPassivesInitialized")) {
         data.putBoolean("CasterGilgameshPassivesInitialized", true);
         data.putBoolean("DivinityActive", true);
         data.putBoolean("ClairvoyanceExActive", true);
         data.putFloat("DivinityFlatDamage", 5.0F);
         data.putBoolean("CasterWandDominionActive", true);
         data.putFloat("CasterWandDominionMultiplier", 1.20F);
      }
      if (!data.contains(WORKSHOP_TYPE)) {
         data.putInt(WORKSHOP_TYPE, 1);
         data.putDouble(CENTER_X, entity.getX());
         data.putDouble(CENTER_Y, entity.getY());
         data.putDouble(CENTER_Z, entity.getZ());
      }
   }

   private static void tickAmmo(CasterGilgameshEntity entity, CompoundTag data) {
      if (entity.tickCount % 20 == 0) {
         data.putInt(AMMO_TAG, Math.min(MAX_AMMO, Math.max(0, data.getInt(AMMO_TAG)) + ammoRecoveryPerSecond(entity.getCombatPhase())));
      }
   }

   private static void tickPhaseEntry(CasterGilgameshEntity entity, ServerLevel level, CompoundTag data, long now) {
      int phase = entity.getCombatPhase();
      int previous = data.getInt(LAST_APPLIED_PHASE);
      if (phase <= previous) return;
      data.putInt(LAST_APPLIED_PHASE, phase);

      int ammoBonus = phase == 2 ? 150 : 350;
      double mpBonus = phase == 2 ? 90.0 : 180.0;
      data.putInt(AMMO_TAG, Math.min(MAX_AMMO, Math.max(0, data.getInt(AMMO_TAG)) + ammoBonus));
      entity.setCurrentMp(Math.min(entity.getMaxMp(), entity.getCurrentMp() + mpBonus));
      if (phase >= 3) {
         entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 1, false, true, true));
         entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 180, 3, false, true, true));
         data.putLong(LAST_SHIELD, now);
      }
      level.sendParticles(GOLD, entity.getX(), entity.getY() + entity.getBbHeight() * 0.72, entity.getZ(),
         phase == 2 ? 90 : 150, 1.2, 0.8, 1.2, 0.10);
      level.sendParticles(ParticleTypes.ENCHANT, entity.getX(), entity.getY() + 1.0, entity.getZ(),
         phase == 2 ? 36 : 60, 1.2, 0.9, 1.2, 0.12);
   }

   private static void tickWorkshop(CasterGilgameshEntity entity, ServerLevel level, CompoundTag data) {
      if (entity.tickCount % 10 != 0) return;
      Vec3 center = workshopCenter(data);
      boolean inside = entity.position().distanceToSqr(center) <= 15.0 * 15.0;
      data.putBoolean("CasterGilgameshInsideWorkshop", inside);
      AttributeInstance armor = entity.getAttribute(Attributes.ARMOR);
      if (inside) {
         ServantCardSkillUtils.addOrReplaceMultiplied(armor, WORKSHOP_ARMOR_ID, 0.20);
         if (entity.tickCount - data.getLong(WORKSHOP_MANA_TICK) >= 10L) {
            entity.setCurrentMp(Math.min(entity.getMaxMp(), entity.getCurrentMp() + entity.getMaxMp() * 0.015));
            data.putLong(WORKSHOP_MANA_TICK, entity.tickCount);
         }
         for (LivingEntity ally : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(15.0),
            e -> e.isAlive() && (e == entity || e.isAlliedTo(entity)))) {
            ServantCardSkillUtils.addOrReplaceMultiplied(ally.getAttribute(Attributes.ARMOR), WORKSHOP_ARMOR_ID, 0.20);
         }
      } else if (armor != null) {
         armor.removeModifier(WORKSHOP_ARMOR_ID);
      }
   }

   private static void tickTimedBuffs(CasterGilgameshEntity entity, ServerLevel level, CompoundTag data, long now) {
      GilgameshCombatHelper.tickClairvoyanceEx(entity, level, data);
      if (now >= data.getLong(LEADER_UNTIL)) {
         for (LivingEntity ally : level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(30.0),
            e -> e.isAlive() && (e == entity || e.isAlliedTo(entity)))) {
            AttributeInstance attack = ally.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attack != null) attack.removeModifier(LEADER_ATTACK_ID);
         }
      }
      if (now >= data.getLong(RETURN_UNTIL)) {
         data.remove("CasterGilgameshReturnTargets");
      }
   }

   private static void useLeader(CasterGilgameshEntity entity, ServerLevel level, CompoundTag data, long now) {
      data.putLong(LAST_LEADER, now);
      data.putLong(LEADER_UNTIL, now + 500L);
      entity.setCurrentMp(entity.getCurrentMp() - 20.0);
      for (LivingEntity ally : level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(30.0),
         e -> e.isAlive() && (e == entity || e.isAlliedTo(entity)))) {
         ServantCardSkillUtils.addOrReplaceMultiplied(ally.getAttribute(Attributes.ATTACK_DAMAGE), LEADER_ATTACK_ID, 0.25);
      }
      level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, entity.getX(), entity.getY() + 1.0, entity.getZ(), 36, 2.0, 1.0, 2.0, 0.04);
      ServantVoiceHelper.tryPlayCasterGilgameshShot(entity);
   }

   private static void useReturn(CasterGilgameshEntity entity, ServerLevel level, CompoundTag data, long now) {
      data.putLong(LAST_RETURN, now);
      data.putLong(RETURN_UNTIL, now + 400L);
      entity.setCurrentMp(entity.getCurrentMp() - 15.0);
      for (LivingEntity ally : level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(20.0),
         e -> e.isAlive() && (e == entity || e.isAlliedTo(entity)))) {
         ally.getPersistentData().putLong("CasterGilgameshCritBuffUntil", now + 400L);
      }
      level.sendParticles(ParticleTypes.ENCHANT, entity.getX(), entity.getY() + 1.0, entity.getZ(), 32, 1.5, 1.0, 1.5, 0.08);
   }

   private static void useEmergencyItem(CasterGilgameshEntity entity, ServerLevel level, CompoundTag data, long now) {
      int phase = entity.getCombatPhase();
      double healThreshold = phase >= 3 ? 0.55 : 0.40;
      double shieldThreshold = phase >= 3 ? 0.42 : 0.20;
      if (entity.getHealth() <= entity.getMaxHealth() * healThreshold && now >= data.getLong(LAST_HEAL) + 400L) {
         entity.heal((float)(entity.getMaxHealth() * 0.40));
         data.putLong(LAST_HEAL, now);
         level.sendParticles(ParticleTypes.HAPPY_VILLAGER, entity.getX(), entity.getY() + 1.0, entity.getZ(), 18, 0.5, 0.6, 0.5, 0.04);
      }
      if (entity.getCurrentMp() <= entity.getMaxMp() * 0.30 && now >= data.getLong(LAST_MP) + 400L) {
         entity.setCurrentMp(Math.min(entity.getMaxMp(), entity.getCurrentMp() + entity.getMaxMp() * 0.30));
         data.putLong(LAST_MP, now);
      }
      if (entity.getHealth() <= entity.getMaxHealth() * shieldThreshold && now >= data.getLong(LAST_SHIELD) + 400L) {
         entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 4, false, true, true));
         data.putLong(LAST_SHIELD, now);
      }
   }

   private static void updateFlight(CasterGilgameshEntity entity, LivingEntity target, long now, CompoundTag data) {
      double distance = entity.distanceTo(target);
      if (distance <= 14.0 || entity.getSensing().hasLineOfSight(target)) {
         data.putLong(FLIGHT_LAST_CONTACT, now);
      } else if (entity.isFlyingMode() && now - data.getLong(FLIGHT_LAST_CONTACT) >= FLIGHT_STALLED_TICKS) {
         entity.setFlyingMode(false);
         entity.getNavigation().moveTo(target, 1.25);
         data.putLong(FLIGHT_LAST_CONTACT, now);
         return;
      }
      entity.setFlyingMode(true);
      entity.getNavigation().stop();
      entity.fallDistance = 0.0F;
      double desiredY = ServantFlightHelper.desiredHoverY(entity, target);
      Vec3 away = entity.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
      if (away.lengthSqr() < 1.0E-4) away = new Vec3(1.0, 0.0, 0.0);
      away = away.normalize();
      int phase = entity.getCombatPhase();
      double preferred = preferredRange(phase);
      double radial = distance < minimumCannonDistance(phase) + 6.0 ? 0.24 + phase * 0.04 : distance > preferred + 8.0 ? -0.08 : 0.0;
      Vec3 orbit = new Vec3(-away.z, 0.0, away.x).scale(Math.sin(entity.tickCount * (0.035 + phase * 0.01)) * (phase >= 2 ? 4.0 : 2.2));
      Vec3 desired = target.position().add(away.scale(preferred)).add(orbit)
         .subtract(entity.position()).normalize();
      Vec3 motion = desired.scale(0.14 + phase * 0.025).add(new Vec3(0.0,
         ServantFlightHelper.verticalVelocityToward(entity.getY(), desiredY + (phase - 1) * 0.8, 0.12, 0.02, 0.14, 0.14), 0.0));
      if (radial != 0.0) motion = motion.add(away.scale(radial));
      entity.setDeltaMovement(motion);
      entity.faceToward(target.position());
   }

   private static boolean tryGateOfBabylon(CasterGilgameshEntity entity, ServerLevel level, LivingEntity target,
                                           CompoundTag data, long now, int phase) {
      if (now - data.getLong(LAST_GATE_OF_BABYLON) < GATE_OF_BABYLON_COOLDOWN
         || entity.getCurrentMp() < 18.0
         || entity.getRandom().nextFloat() > (phase >= 3 ? 0.12F : 0.06F)) {
         return false;
      }
      data.putLong(LAST_GATE_OF_BABYLON, now);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 18.0));
      int count = phase >= 3 ? 18 : phase >= 2 ? 12 : 8;
      fireGateOfBabylon(entity, level, target, count, phase >= 3 ? 24.0F : 18.0F);
      return true;
   }

   private static void fireGateOfBabylon(CasterGilgameshEntity entity, ServerLevel level, LivingEntity target,
                                         int count, float damage) {
      Vec3 forward = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0)
         .subtract(entity.position().add(0.0, entity.getBbHeight() * 0.55, 0.0));
      if (forward.lengthSqr() < 1.0E-4) forward = entity.getLookAngle();
      forward = forward.normalize();
      Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
      if (right.lengthSqr() < 1.0E-4) right = new Vec3(1.0, 0.0, 0.0);
      right = right.normalize();
      Vec3 center = entity.position().add(0.0, entity.getBbHeight() * 0.85, 0.0).add(forward.scale(-1.9));
      for (int i = 0; i < count; i++) {
         double row = i / 6;
         double side = (i % 6 - 2.5) * 0.75;
         Vec3 gate = center.add(right.scale(side)).add(0.0, 0.45 + row * 0.55, 0.0);
         Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0).subtract(gate).normalize();
         GilgameshGateWeaponProjectileEntity projectile = new GilgameshGateWeaponProjectileEntity(
            level, entity, gate, aim, GATE_WEAPONS[Math.floorMod(i, GATE_WEAPONS.length)], damage);
         projectile.setHomingTarget(target);
         projectile.setLaunchDelay(14 + (i / 6) * 4);
         projectile.setEffectStride(count >= 12 ? 2 : 1);
         level.addFreshEntity(projectile);
      }
      VFXServerEffects.spawnOriented(level, "gilgamesh_gate", center, forward, 128.0);
      level.sendParticles(GOLD, center.x, center.y, center.z, 24 + count, 1.2, 0.8, 1.2, 0.06);
      ServantVoiceHelper.tryPlayCasterGilgameshShot(entity);
   }

   private static boolean hasValidCannonResources(CasterGilgameshEntity entity, LivingEntity target) {
      CompoundTag data = entity.getPersistentData();
      return target != null && target.isAlive()
         && data.getInt(AMMO_TAG) >= CANNON_SHOTS_PER_ROUND
         && entity.getCurrentMp() >= CANNON_SHOTS_PER_ROUND;
   }

   private static void fireRoyalCannon(CasterGilgameshEntity entity, ServerLevel level, LivingEntity target, CompoundTag data) {
      if (!hasValidCannonResources(entity, target)) {
         data.putBoolean(FIRING_TAG, false);
         return;
      }
      spawnRoyalCannonVolley(level, entity, target, CANNON_SHOTS_PER_ROUND, CANNON_DAMAGE_PER_SHOT, 1.0F, true);
      data.putInt(AMMO_TAG, data.getInt(AMMO_TAG) - CANNON_SHOTS_PER_ROUND);
      entity.setCurrentMp(entity.getCurrentMp() - CANNON_SHOTS_PER_ROUND);
      entity.triggerNamedActionAnimation("standing");
      ServantVoiceHelper.tryPlayCasterGilgameshNp(entity);
   }

   public static int spawnRoyalCannonVolley(ServerLevel level, LivingEntity owner, LivingEntity target, int shots,
                                           float damage, float damageMultiplier, boolean explosive) {
      if (level == null || owner == null || target == null || !owner.isAlive() || !target.isAlive() || shots <= 0) {
         return 0;
      }
      Vec3 forward = target.position().subtract(owner.position()).multiply(1.0, 0.0, 1.0);
      if (forward.lengthSqr() < 1.0E-4) forward = owner.getLookAngle().multiply(1.0, 0.0, 1.0);
      forward = forward.normalize();
      Vec3 behind = forward.scale(-1.0);
      Vec3 center = owner.position().add(0.0, owner.getBbHeight() * 0.75, 0.0).add(behind.scale(2.0));
      Vec3 right = new Vec3(-forward.z, 0.0, forward.x).normalize();
      List<LivingEntity> targets = collectVolleyTargets(level, owner, target, Math.max(1, Math.min(shots, 8)));
      for (int i = 0; i < shots; i++) {
         LivingEntity shotTarget = targets.get(targets.size() == 1 ? 0 : Math.floorMod(i * 7, targets.size()));
         double angle = (Math.PI * 2.0 * i / shots) + owner.getRandom().nextDouble() * 0.22;
         double ring = 1.05 + (i % 3) * 0.48;
         double side = Math.cos(angle) * ring;
         double height = Math.sin(angle) * 1.18 + ((i & 1) == 0 ? 0.34 : -0.26);
         Vec3 start = center.add(right.scale(side)).add(behind.scale(0.18 + owner.getRandom().nextDouble() * 0.68)).add(0.0, height, 0.0);
         Vec3 targetOffset = right.scale((owner.getRandom().nextDouble() - 0.5) * 2.6)
            .add(0.0, (owner.getRandom().nextDouble() - 0.5) * 1.4, 0.0)
            .add(forward.scale((owner.getRandom().nextDouble() - 0.5) * 1.2));
         Vec3 predicted = shotTarget.position()
            .add(shotTarget.getDeltaMovement().scale(5.0 + owner.getRandom().nextDouble() * 5.0))
            .add(0.0, shotTarget.getBbHeight() * (0.42 + owner.getRandom().nextDouble() * 0.22), 0.0)
            .add(targetOffset);
         Vec3 direct = predicted.subtract(start).normalize();
         Vec3 launchFan = right.scale(Math.cos(angle) * (0.26 + owner.getRandom().nextDouble() * 0.22))
            .add(behind.scale(0.16 + owner.getRandom().nextDouble() * 0.24))
            .add(0.0, Math.sin(angle) * 0.22 + (owner.getRandom().nextDouble() - 0.35) * 0.18, 0.0);
         Vec3 aim = direct.scale(explosive ? 0.76 : 0.88).add(launchFan).normalize();
         Vec3 curve = right.scale(Math.cos(angle + Math.PI * 0.5))
            .add(forward.scale(Math.sin(angle) * 0.38))
            .add(0.0, Math.cos(angle * 0.7) * 0.28, 0.0);
         if (curve.lengthSqr() < 1.0E-6) curve = right;
         curve = curve.normalize();
         RoyalCannonProjectileEntity projectile = new RoyalCannonProjectileEntity(level, owner, start, aim, damage);
         projectile.setHomingTarget(shotTarget);
         projectile.setDamageMultiplier(damageMultiplier);
         if (explosive) {
            projectile.configureRoyalCannon(CANNON_EXPLOSION_RADIUS, curve, 0.055 + owner.getRandom().nextDouble() * 0.035);
         }
         level.addFreshEntity(projectile);
         level.sendParticles(GOLD, start.x, start.y, start.z, 12, 0.12, 0.12, 0.12, 0.035);
         level.sendParticles(ParticleTypes.END_ROD, start.x, start.y, start.z, 3, 0.05, 0.05, 0.05, 0.015);
      }
      spawnRoyalCannonGateFx(level, center, forward, right, shots);
      return shots;
   }

   private static List<LivingEntity> collectVolleyTargets(ServerLevel level, LivingEntity owner, LivingEntity primary, int limit) {
      List<LivingEntity> result = new ArrayList<>();
      if (isValidVolleyTarget(owner, primary)) {
         result.add(primary);
      }
      level.getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(48.0), e -> isValidVolleyTarget(owner, e))
         .stream()
         .sorted(Comparator.comparingDouble(owner::distanceToSqr))
         .forEach(candidate -> {
            if (result.size() < limit && result.stream().noneMatch(existing -> existing.getId() == candidate.getId())) {
               result.add(candidate);
            }
         });
      return result.isEmpty() && primary != null ? List.of(primary) : result;
   }

   private static boolean isValidVolleyTarget(LivingEntity owner, LivingEntity target) {
      return owner != null && target != null && target.isAlive() && target != owner
         && !target.isAlliedTo(owner) && !owner.isAlliedTo(target)
         && !isProtectedMasterTarget(owner, target)
         && !EntityUtils.isImmunePlayerTarget(target);
   }

   public static boolean isProtectedMasterTarget(LivingEntity owner, LivingEntity target) {
      if (owner == null || target == null) return false;
      if (ServantMasterTargeting.isContractMaster(owner, target)) return true;
      return owner instanceof ServantEntity servant
         && target instanceof net.minecraft.server.level.ServerPlayer master
         && servant.isBoundTo(master);
   }

   private static void spawnRoyalCannonGateFx(ServerLevel level, Vec3 center, Vec3 forward, Vec3 right, int shots) {
      VFXServerEffects.spawnOriented(level, "gilgamesh_gate", center, forward, 128.0);
      Vec3 up = new Vec3(0.0, 1.0, 0.0);
      int points = 42;
      for (int i = 0; i < points; i++) {
         double angle = Math.PI * 2.0 * i / points;
         double radius = 1.9 + 0.18 * Math.sin(angle * 3.0);
         Vec3 point = center.add(right.scale(Math.cos(angle) * radius)).add(up.scale(Math.sin(angle) * radius));
         level.sendParticles(GOLD, point.x, point.y, point.z, 1, 0.025, 0.025, 0.025, 0.012);
      }
      level.sendParticles(GOLD, center.x, center.y, center.z, 90 + shots * 6, 2.4, 1.8, 2.4, 0.12);
      level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 34 + shots, 1.6, 1.25, 1.6, 0.065);
      level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 1, 0.0, 0.0, 0.0, 0.0);
   }

   private static Vec3 workshopCenter(CompoundTag data) {
      return new Vec3(data.getDouble(CENTER_X), data.getDouble(CENTER_Y), data.getDouble(CENTER_Z));
   }

   public static float magicDamage(CasterGilgameshEntity entity, float base) {
      if (entity == null) return base;
      return entity.getPersistentData().getBoolean("CasterWandDominionActive")
         ? base * entity.getPersistentData().getFloat("CasterWandDominionMultiplier") : base;
   }

   private static int ammoRecoveryPerSecond(int phase) {
      return phase >= 3 ? 3 : phase >= 2 ? 2 : 1;
   }

   private static long leaderCooldown(int phase) {
      return phase >= 3 ? 520L : phase >= 2 ? 620L : 700L;
   }

   private static long returnCooldown(int phase) {
      return phase >= 3 ? 440L : phase >= 2 ? 520L : 600L;
   }

   private static long cannonInterval(int phase) {
      return phase >= 3 ? 10L : phase >= 2 ? 15L : 20L;
   }

   private static double minimumCannonDistance(int phase) {
      return phase >= 3 ? 6.0 : phase >= 2 ? 7.0 : 8.0;
   }

   private static double preferredRange(int phase) {
      return phase >= 3 ? 30.0 : phase >= 2 ? 25.0 : 20.0;
   }

   public static void tryPlayVictory(CasterGilgameshEntity entity, LivingEntity victim) {
      ServantVoiceHelper.tryPlayVictory(entity, victim);
   }
}
