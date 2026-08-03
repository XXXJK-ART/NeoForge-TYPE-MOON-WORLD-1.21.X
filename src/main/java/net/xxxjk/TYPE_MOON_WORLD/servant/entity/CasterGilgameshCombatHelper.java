package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.Comparator;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.RoyalCannonProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantFlightHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSkillUtils;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import org.joml.Vector3f;

/** Server-side skills, flight and Royal Cannon state for Caster Gilgamesh. */
public final class CasterGilgameshCombatHelper {
   public static final String AMMO_TAG = "RoyalCannonAmmo";
   public static final String FIRING_TAG = "RoyalCannonFiring";
   public static final int MAX_AMMO = 5000;
   public static final int STARTING_AMMO = 500;
   public static final int CANNON_SHOTS_PER_ROUND = 10;
   public static final float CANNON_EXPLOSION_RADIUS = 3.0F;
   private static final String LAST_LEADER = "CasterGilgameshLastLeader";
   private static final String LAST_RETURN = "CasterGilgameshLastReturn";
   private static final String LEADER_UNTIL = "CasterGilgameshLeaderUntil";
   private static final String RETURN_UNTIL = "CasterGilgameshReturnUntil";
   private static final String LAST_HEAL = "CasterGilgameshLastHeal";
   private static final String LAST_MP = "CasterGilgameshLastMp";
   private static final String LAST_SHIELD = "CasterGilgameshLastShield";
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

   private CasterGilgameshCombatHelper() {}

   public static void tick(CasterGilgameshEntity entity) {
      if (!(entity.level() instanceof ServerLevel level) || !entity.isAlive()) return;
      long now = level.getGameTime();
      CompoundTag data = entity.getPersistentData();
      initialize(entity, data);
      tickAmmo(entity, data);
      tickWorkshop(entity, level, data);
      tickTimedBuffs(entity, level, data, now);

      LivingEntity target = entity.getTarget();
      if (target == null || !target.isAlive() || entity.isAlliedTo(target)) {
         data.putBoolean(FIRING_TAG, false);
         entity.setFlyingMode(false);
         return;
      }

      updateFlight(entity, target);
      if (now >= data.getLong(LAST_LEADER) + 700L && entity.getCurrentMp() >= 20.0) {
         useLeader(entity, level, data, now);
      }
      if (now >= data.getLong(LAST_RETURN) + 600L && entity.getCurrentMp() >= 15.0) {
         useReturn(entity, level, data, now);
      }
      if (entity.distanceTo(target) < 8.0) {
         data.putBoolean(FIRING_TAG, false);
         useEmergencyItem(entity, level, data, now);
      } else {
         data.putBoolean(FIRING_TAG, hasValidCannonResources(entity, target));
      }
      if (data.getBoolean(FIRING_TAG) && entity.tickCount % 20 == 0) {
         fireRoyalCannon(entity, level, target, data);
      }
   }

   private static void initialize(CasterGilgameshEntity entity, CompoundTag data) {
      if (!data.contains(AMMO_TAG)) data.putInt(AMMO_TAG, STARTING_AMMO);
      if (!data.contains(FIRING_TAG)) data.putBoolean(FIRING_TAG, false);
      if (!data.contains(LAST_LEADER)) data.putLong(LAST_LEADER, -700L);
      if (!data.getBoolean("CasterGilgameshPassivesInitialized")) {
         data.putBoolean("CasterGilgameshPassivesInitialized", true);
         data.putBoolean("DivinityActive", true);
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
         data.putInt(AMMO_TAG, Math.min(MAX_AMMO, Math.max(0, data.getInt(AMMO_TAG)) + 1));
      }
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
      if (entity.getHealth() <= entity.getMaxHealth() * 0.40 && now >= data.getLong(LAST_HEAL) + 400L) {
         entity.heal((float)(entity.getMaxHealth() * 0.40));
         data.putLong(LAST_HEAL, now);
         level.sendParticles(ParticleTypes.HAPPY_VILLAGER, entity.getX(), entity.getY() + 1.0, entity.getZ(), 18, 0.5, 0.6, 0.5, 0.04);
      }
      if (entity.getCurrentMp() <= entity.getMaxMp() * 0.30 && now >= data.getLong(LAST_MP) + 400L) {
         entity.setCurrentMp(Math.min(entity.getMaxMp(), entity.getCurrentMp() + entity.getMaxMp() * 0.30));
         data.putLong(LAST_MP, now);
      }
      if (entity.getHealth() <= entity.getMaxHealth() * 0.20 && now >= data.getLong(LAST_SHIELD) + 400L) {
         entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 4, false, true, true));
         data.putLong(LAST_SHIELD, now);
      }
   }

   private static void updateFlight(CasterGilgameshEntity entity, LivingEntity target) {
      entity.setFlyingMode(true);
      entity.getNavigation().stop();
      entity.fallDistance = 0.0F;
      double desiredY = ServantFlightHelper.desiredHoverY(entity, target);
      Vec3 away = entity.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
      if (away.lengthSqr() < 1.0E-4) away = new Vec3(1.0, 0.0, 0.0);
      away = away.normalize();
      double distance = entity.distanceTo(target);
      double radial = distance < 16.0 ? 0.18 : distance > 28.0 ? -0.08 : 0.0;
      Vec3 desired = target.position().add(away.scale(20.0)).add(0.0, 0.0, 0.0)
         .subtract(entity.position()).normalize();
      Vec3 motion = desired.scale(0.16).add(new Vec3(0.0,
         ServantFlightHelper.verticalVelocityToward(entity.getY(), desiredY, 0.12, 0.02, 0.14, 0.14), 0.0));
      if (radial != 0.0) motion = motion.add(away.scale(radial));
      entity.setDeltaMovement(motion);
      entity.faceToward(target.position());
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
      Vec3 forward = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      if (forward.lengthSqr() < 1.0E-4) forward = entity.getLookAngle().multiply(1.0, 0.0, 1.0);
      forward = forward.normalize();
      Vec3 behind = forward.scale(-1.0);
      Vec3 center = entity.position().add(0.0, entity.getBbHeight() * 0.75, 0.0).add(behind.scale(2.0));
      Vec3 right = new Vec3(-forward.z, 0.0, forward.x).normalize();
      for (int i = 0; i < CANNON_SHOTS_PER_ROUND; i++) {
         double angle = (Math.PI * 2.0 * i / CANNON_SHOTS_PER_ROUND) + entity.getRandom().nextDouble() * 0.22;
         double ring = 0.8 + (i % 3) * 0.35;
         double side = Math.cos(angle) * ring;
         double height = Math.sin(angle) * 0.95 + ((i & 1) == 0 ? 0.24 : -0.24);
         Vec3 start = center.add(right.scale(side)).add(behind.scale(entity.getRandom().nextDouble() * 0.45)).add(0.0, height, 0.0);
         Vec3 targetOffset = right.scale((entity.getRandom().nextDouble() - 0.5) * 2.6)
            .add(0.0, (entity.getRandom().nextDouble() - 0.5) * 1.4, 0.0)
            .add(forward.scale((entity.getRandom().nextDouble() - 0.5) * 1.2));
         Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0).add(targetOffset)
            .subtract(start).normalize();
         RoyalCannonProjectileEntity projectile = new RoyalCannonProjectileEntity(level, entity, start, aim, 20.0F);
         projectile.setHomingTarget(target);
         projectile.setExplosive(CANNON_EXPLOSION_RADIUS);
         level.addFreshEntity(projectile);
      }
      data.putInt(AMMO_TAG, data.getInt(AMMO_TAG) - CANNON_SHOTS_PER_ROUND);
      entity.setCurrentMp(entity.getCurrentMp() - CANNON_SHOTS_PER_ROUND);
      entity.triggerNamedActionAnimation("standing");
      level.sendParticles(GOLD, center.x, center.y, center.z, 130, 2.2, 1.7, 2.2, 0.10);
      level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 44, 1.5, 1.2, 1.5, 0.06);
      ServantVoiceHelper.tryPlayCasterGilgameshNp(entity);
   }

   private static Vec3 workshopCenter(CompoundTag data) {
      return new Vec3(data.getDouble(CENTER_X), data.getDouble(CENTER_Y), data.getDouble(CENTER_Z));
   }

   public static float magicDamage(CasterGilgameshEntity entity, float base) {
      if (entity == null) return base;
      return entity.getPersistentData().getBoolean("CasterWandDominionActive")
         ? base * entity.getPersistentData().getFloat("CasterWandDominionMultiplier") : base;
   }

   public static void tryPlayVictory(CasterGilgameshEntity entity, LivingEntity victim) {
      ServantVoiceHelper.tryPlayVictory(entity, victim);
   }
}
