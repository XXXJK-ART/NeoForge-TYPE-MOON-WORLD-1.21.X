package net.xxxjk.TYPE_MOON_WORLD.servant.palerider;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ApocalypseHorseEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ApocalypseHorsemanEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ConceptSwordEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.PaleRiderEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.RatSwarmEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.PaleRiderCrowEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.SoulEchoEntity;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import org.joml.Vector3f;

public final class PaleRiderCombatHelper {
   public static final String TAG_UNDERWORLD_UNTIL = "PaleRiderUnderworldUntil";
   public static final String TAG_UNDERWORLD_ACTIVE = "PaleRiderUnderworldActive";
   public static final String TAG_CALAMITY_ACTIVE = "PaleRiderCalamityActive";
   private static final String TAG_UNDERWORLD_COOLDOWN = "PaleRiderUnderworldCooldown";
   private static final String TAG_CALAMITY_COOLDOWN = "PaleRiderCalamityCooldown";
   private static final String TAG_LAST_RAT = "PaleRiderLastRat";
   private static final String TAG_LAST_DOMAIN_TICK = "PaleRiderLastDomainTick";
   private static final String TAG_LAST_FEAR_TICK = "PaleRiderLastFearTick";
   private static final String TAG_LAST_SOUL_REGEN = "PaleRiderLastSoulRegen";
   private static final String TAG_LAST_UNDERWORLD_ENVIRONMENT = "PaleRiderLastUnderworldEnvironment";
   private static final String TAG_LAST_CALAMITY_ENVIRONMENT = "PaleRiderLastCalamityEnvironment";
   private static final String TAG_LAST_DEATH_JUDGMENT = "PaleRiderLastDeathJudgment";
   private static final String TAG_NEXT_ASH_STEP = "PaleRiderNextAshStep";
   private static final String TAG_NEXT_PLAGUE_RUSH = "PaleRiderNextPlagueRush";
   private static final String TAG_NEXT_DEATH_PULSE = "PaleRiderNextDeathPulse";
   private static final String TAG_NEXT_POSSESSION_SCAN = "PaleRiderNextPossessionScan";
   private static final String TAG_MINOR_SKILL_LOCK_UNTIL = "PaleRiderMinorSkillLockUntil";
   private static final String TAG_MINOR_SKILL_DAMAGE = "PaleRiderMinorSkillDamage";
   private static final String TAG_LAST_PERSISTENT_STATE_TICK = "PaleRiderLastPersistentStateTick";
   private static final ResourceLocationId SPEED_ID = new ResourceLocationId("underworld_soul_speed");
   private static final net.minecraft.resources.ResourceLocation FEAR_ATTACK_ID =
      net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "pale_rider_fear_attack");
   private static final net.minecraft.resources.ResourceLocation FAMINE_ATTACK_ID =
      net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "pale_rider_famine_attack");

   private PaleRiderCombatHelper() {
   }

   public static void tick(PaleRiderEntity rider) {
      if (!(rider.level() instanceof ServerLevel level)) return;
      long now = level.getGameTime();
      tickPersistentState(rider);
      tickPhaseAi(rider, level, now);
   }

   public static void tickPersistentState(PaleRiderEntity rider) {
      if (!(rider.level() instanceof ServerLevel level) || !rider.isAlive()) return;
      long now = level.getGameTime();
      CompoundTag data = rider.getPersistentData();
      if (data.contains(TAG_LAST_PERSISTENT_STATE_TICK) && data.getLong(TAG_LAST_PERSISTENT_STATE_TICK) == now) return;
      data.putLong(TAG_LAST_PERSISTENT_STATE_TICK, now);
      rider.tickPossession();
      if (now - data.getLong(TAG_LAST_FEAR_TICK) >= 20L) {
         data.putLong(TAG_LAST_FEAR_TICK, now);
         tickAuraAndInfections(rider, level);
      }
      tickUnderworld(rider, level, now);
      tickCalamity(rider, level, now);
      if (InfectionRules.isScheduled(rider.getId(), now, 10)) tickMounts(rider, level, now);
      if (rider.isUnderworldActive() && now - rider.getPersistentData().getLong(TAG_LAST_UNDERWORLD_ENVIRONMENT) >= 80L) {
         rider.getPersistentData().putLong(TAG_LAST_UNDERWORLD_ENVIRONMENT, now);
         VFXServerEffects.spawn(level, "pale_rider_underworld_sustain", rider, 64.0);
      }
      if (rider.isCalamityActive() && now - rider.getPersistentData().getLong(TAG_LAST_CALAMITY_ENVIRONMENT) >= 80L) {
         rider.getPersistentData().putLong(TAG_LAST_CALAMITY_ENVIRONMENT, now);
         VFXServerEffects.spawn(level, "pale_rider_calamity_sustain", rider, 40.0);
      }
   }

   private static void tickPhaseAi(PaleRiderEntity rider, ServerLevel level, long now) {
      LivingEntity attacker = rider.getLastHurtByMob();
      if (attacker != null && attacker.isAlive() && !attacker.isAlliedTo(rider) && !rider.isAlliedTo(attacker)
         && !EntityUtils.isImmunePlayerTarget(attacker)) {
         rider.lockCombatTarget(attacker);
      }
      ensurePossession(rider, level);
      LivingEntity target = rider.findPaleRiderEnemy(64.0);
      if (target == null) {
         return;
      }
      tickMinorSkills(rider, level, target, now);
      double ratio = rider.getHealth() / Math.max(1.0F, rider.getMaxHealth());
      int desiredRats = ratio > 0.8 ? 3 : ratio > 0.6 ? 8 : 100;
      int interval = ratio > 0.8 ? 240 : ratio > 0.6 ? 160 : 40;
      int currentRats = countOwned(level, rider, RatSwarmEntity.class);
      if (currentRats < desiredRats && currentRats < 100 && now - rider.getPersistentData().getLong(TAG_LAST_RAT) >= interval && rider.getCurrentMp() >= 10.0) {
         rider.setCurrentMp(rider.getCurrentMp() - 10.0);
         rider.getPersistentData().putLong(TAG_LAST_RAT, now);
         spawnPlagueAnimals(rider, level, false);
      }
      if (ratio <= 0.8 && !rider.isUnderworldActive() && rider.getSoulLibrary().size() >= 10
         && now >= rider.getPersistentData().getLong(TAG_UNDERWORLD_COOLDOWN)
         && rider.getCurrentMp() >= PaleRiderCombatRules.UNDERWORLD_CAST_MP_COST
         && (ratio <= 0.6 || countEnemies(rider, level, 32.0) >= 2 || target.getMaxHealth() >= 160.0F)) {
         startUnderworld(rider, level, now);
      }
      if (ratio <= 0.6 && !rider.isCalamityActive() && now >= rider.getPersistentData().getLong(TAG_CALAMITY_COOLDOWN)
         && rider.getCurrentMp() >= PaleRiderCombatRules.CALAMITY_CAST_MP_COST + PaleRiderCombatRules.CALAMITY_UPKEEP_MP_PER_SECOND) {
         startCalamity(rider, level, now);
      }
   }

   private static void tickMinorSkills(PaleRiderEntity rider, ServerLevel level, LivingEntity target, long now) {
      CompoundTag data = rider.getPersistentData();
      if (now < data.getLong(TAG_MINOR_SKILL_LOCK_UNTIL)) return;
      Mob actor = combatActor(rider);
      if (actor == null || !actor.isAlive()) return;
      double distance = actor.distanceTo(target);
      boolean lineOfSight = actor.getSensing().hasLineOfSight(target);
      if (!lineOfSight && tryAshStep(rider, actor, target, level, now)) return;
      if (distance <= 5.0 && tryDeathPulse(rider, actor, level, now)) return;
      if (distance >= 4.0 && distance <= 15.0 && lineOfSight && tryPlagueRush(rider, actor, target, level, now)) return;
      if ((distance < 2.2 || distance > 15.0) && tryAshStep(rider, actor, target, level, now)) return;
   }

   private static Mob combatActor(PaleRiderEntity rider) {
      Mob host = rider.getPossessedHost();
      if (host != null) return host;
      return rider.getVehicle() instanceof Mob vehicle ? vehicle : rider;
   }

   private static boolean tryAshStep(PaleRiderEntity rider, Mob actor, LivingEntity target, ServerLevel level, long now) {
      CompoundTag data = rider.getPersistentData();
      if (now < data.getLong(TAG_NEXT_ASH_STEP) || rider.getCurrentMp() < PaleRiderCombatRules.ASH_STEP_MP_COST) return false;
      Vec3 away = actor.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
      if (away.lengthSqr() < 1.0E-4) away = new Vec3(1.0, 0.0, 0.0);
      away = away.normalize();
      Vec3 side = new Vec3(-away.z, 0.0, away.x).scale(actor.getRandom().nextBoolean() ? 1.8 : -1.8);
      Vec3 destination = target.position().add(away.scale(3.0)).add(side);
      AABB movedBox = actor.getBoundingBox().move(destination.subtract(actor.position()));
      if (!level.noCollision(actor, movedBox)) {
         destination = target.position().add(away.scale(3.5)).subtract(side);
         movedBox = actor.getBoundingBox().move(destination.subtract(actor.position()));
         if (!level.noCollision(actor, movedBox)) return false;
      }
      Vec3 origin = actor.position().add(0.0, actor.getBbHeight() * 0.5, 0.0);
      level.sendParticles(ParticleTypes.SQUID_INK, origin.x, origin.y, origin.z, 18, 0.35, 0.5, 0.35, 0.04);
      level.sendParticles(ParticleTypes.ASH, origin.x, origin.y, origin.z, 24, 0.45, 0.55, 0.45, 0.05);
      actor.teleportTo(destination.x, destination.y, destination.z);
      actor.getNavigation().stop();
      actor.getLookControl().setLookAt(target, 90.0F, 90.0F);
      level.sendParticles(ParticleTypes.LARGE_SMOKE, destination.x, destination.y + actor.getBbHeight() * 0.5, destination.z, 18, 0.4, 0.55, 0.4, 0.035);
      level.sendParticles(ParticleTypes.ASH, destination.x, destination.y + actor.getBbHeight() * 0.5, destination.z, 28, 0.5, 0.65, 0.5, 0.045);
      level.playSound(null, actor.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.75F, 0.55F);
      rider.setCurrentMp(rider.getCurrentMp() - PaleRiderCombatRules.ASH_STEP_MP_COST);
      data.putLong(TAG_NEXT_ASH_STEP, now + PaleRiderCombatRules.ASH_STEP_COOLDOWN_TICKS);
      lockMinorSkills(data, now);
      return true;
   }

   private static boolean tryPlagueRush(PaleRiderEntity rider, Mob actor, LivingEntity target, ServerLevel level, long now) {
      CompoundTag data = rider.getPersistentData();
      if (now < data.getLong(TAG_NEXT_PLAGUE_RUSH) || rider.getCurrentMp() < PaleRiderCombatRules.PLAGUE_RUSH_MP_COST) return false;
      Vec3 direction = target.position().subtract(actor.position()).multiply(1.0, 0.0, 1.0);
      if (direction.lengthSqr() < 1.0E-4) return false;
      direction = direction.normalize();
      double length = Math.min(7.0, Math.max(3.5, actor.distanceTo(target) - 1.0));
      actor.getLookControl().setLookAt(target, 90.0F, 90.0F);
      actor.setDeltaMovement(direction.x * 1.35, Math.max(actor.getDeltaMovement().y, 0.12), direction.z * 1.35);
      actor.hasImpulse = true;
      actor.getNavigation().moveTo(actor.getX() + direction.x * length, actor.getY(), actor.getZ() + direction.z * length, 1.45);
      AABB hitBox = actor.getBoundingBox().expandTowards(direction.scale(length)).inflate(1.1, 0.8, 1.1);
      for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, hitBox, entity -> canHit(rider, actor, entity))) {
         victim.invulnerableTime = 0;
         if (hurtWithMinorSkill(rider, victim, 12.0F) && victim.isAlive()) PaleRiderInfectionService.infect(victim, rider, 2);
         victim.push(direction.x * 0.75, 0.16, direction.z * 0.75);
         victim.hurtMarked = true;
      }
      for (double distance = 0.5; distance <= length; distance += 0.55) {
         Vec3 point = actor.position().add(direction.scale(distance)).add(0.0, actor.getBbHeight() * 0.45, 0.0);
         level.sendParticles(ParticleTypes.ASH, point.x, point.y, point.z, 3, 0.14, 0.2, 0.14, 0.025);
         level.sendParticles(ParticleTypes.LARGE_SMOKE, point.x, point.y, point.z, 1, 0.1, 0.15, 0.1, 0.015);
      }
      level.playSound(null, actor.blockPosition(), SoundEvents.PHANTOM_SWOOP, SoundSource.HOSTILE, 0.9F, 0.62F);
      rider.setCurrentMp(rider.getCurrentMp() - PaleRiderCombatRules.PLAGUE_RUSH_MP_COST);
      data.putLong(TAG_NEXT_PLAGUE_RUSH, now + PaleRiderCombatRules.PLAGUE_RUSH_COOLDOWN_TICKS);
      lockMinorSkills(data, now);
      return true;
   }

   private static boolean tryDeathPulse(PaleRiderEntity rider, Mob actor, ServerLevel level, long now) {
      CompoundTag data = rider.getPersistentData();
      if (now < data.getLong(TAG_NEXT_DEATH_PULSE) || rider.getCurrentMp() < PaleRiderCombatRules.DEATH_PULSE_MP_COST) return false;
      List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, actor.getBoundingBox().inflate(5.0, 3.0, 5.0),
         entity -> canHit(rider, actor, entity) && actor.getSensing().hasLineOfSight(entity));
      if (victims.isEmpty()) return false;
      Vec3 center = actor.position().add(0.0, actor.getBbHeight() * 0.45, 0.0);
      for (LivingEntity victim : victims) {
         victim.invulnerableTime = 0;
         if (hurtWithMinorSkill(rider, victim, 10.0F) && victim.isAlive()) PaleRiderInfectionService.infect(victim, rider, 1);
         Vec3 push = victim.position().subtract(actor.position()).multiply(1.0, 0.0, 1.0);
         if (push.lengthSqr() > 1.0E-4) {
            push = push.normalize();
            victim.push(push.x * 0.55, 0.12, push.z * 0.55);
            victim.hurtMarked = true;
         }
      }
      level.sendParticles(ParticleTypes.SCULK_SOUL, center.x, center.y, center.z, 32, 2.2, 0.75, 2.2, 0.06);
      level.sendParticles(ParticleTypes.SQUID_INK, center.x, center.y, center.z, 24, 1.8, 0.65, 1.8, 0.045);
      level.sendParticles(ParticleTypes.ASH, center.x, center.y, center.z, 52, 2.5, 0.9, 2.5, 0.055);
      level.playSound(null, actor.blockPosition(), SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.HOSTILE, 1.0F, 0.58F);
      rider.setCurrentMp(rider.getCurrentMp() - PaleRiderCombatRules.DEATH_PULSE_MP_COST);
      data.putLong(TAG_NEXT_DEATH_PULSE, now + PaleRiderCombatRules.DEATH_PULSE_COOLDOWN_TICKS);
      lockMinorSkills(data, now);
      return true;
   }

   private static boolean canHit(PaleRiderEntity rider, Mob actor, LivingEntity target) {
      return target != rider && target != actor && target.isAlive() && !target.isAlliedTo(rider) && !rider.isAlliedTo(target)
         && !EntityUtils.isImmunePlayerTarget(target);
   }

   private static void lockMinorSkills(CompoundTag data, long now) {
      data.putLong(TAG_MINOR_SKILL_LOCK_UNTIL, now + PaleRiderCombatRules.MINOR_SKILL_LOCK_TICKS);
   }

   private static boolean hurtWithMinorSkill(PaleRiderEntity rider, LivingEntity target, float damage) {
      rider.getPersistentData().putBoolean(TAG_MINOR_SKILL_DAMAGE, true);
      try {
         return target.hurt(rider.damageSources().mobAttack(rider), damage);
      } finally {
         rider.getPersistentData().remove(TAG_MINOR_SKILL_DAMAGE);
      }
   }

   public static boolean isMinorSkillDamage(PaleRiderEntity rider) {
      return rider != null && rider.getPersistentData().getBoolean(TAG_MINOR_SKILL_DAMAGE);
   }

   private static void ensurePossession(PaleRiderEntity rider, ServerLevel level) {
      if (rider.hasPossessedHost()) return;
      long now = level.getGameTime();
      if (now < rider.getPersistentData().getLong(TAG_NEXT_POSSESSION_SCAN)) return;
      rider.getPersistentData().putLong(TAG_NEXT_POSSESSION_SCAN, now + 40L + Math.floorMod(rider.getId(), 10));
      Mob infectedHost = level.getEntitiesOfClass(Mob.class, rider.getBoundingBox().inflate(96.0),
         mob -> mob != rider && mob.isAlive() && !(mob instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity)
            && !PaleRiderInfectionService.isForbiddenPossessionHost(mob)
            && !mob.getType().is(net.neoforged.neoforge.common.Tags.EntityTypes.BOSSES)
            && PaleRiderInfectionService.isInfectedBy(mob, rider))
         .stream().min((left, right) -> Double.compare(left.distanceToSqr(rider), right.distanceToSqr(rider))).orElse(null);
      if (infectedHost != null && rider.beginPossession(infectedHost)) return;
      SoulEchoEntity echo = level.getEntitiesOfClass(SoulEchoEntity.class, rider.getBoundingBox().inflate(96.0),
         soul -> soul.isAlive() && rider.getUUID().equals(soul.getPaleRiderOwnerUuid()))
         .stream().min((left, right) -> Double.compare(left.distanceToSqr(rider), right.distanceToSqr(rider))).orElse(null);
      if (echo != null) rider.beginPossession(echo);
   }

   private static void startUnderworld(PaleRiderEntity rider, ServerLevel level, long now) {
      rider.setCurrentMp(rider.getCurrentMp() - PaleRiderCombatRules.UNDERWORLD_CAST_MP_COST);
      rider.getPersistentData().putBoolean(TAG_UNDERWORLD_ACTIVE, true);
      rider.getPersistentData().remove(TAG_UNDERWORLD_UNTIL);
      rider.getPersistentData().putLong(TAG_UNDERWORLD_COOLDOWN, now + 2400L);
      VFXServerEffects.spawn(level, "pale_rider_underworld_open", rider, 64.0);
      int slots = Math.max(0, SoulLibrary.MAX_MANIFESTED_SOULS - countOwned(level, rider, SoulEchoEntity.class));
      List<SoulSnapshot> souls = rider.getSoulLibrary().takeStrongest(slots);
      int index = 0;
      for (SoulSnapshot soul : souls) {
         SoulEchoEntity echo = ModEntities.SOUL_ECHO.get().create(level);
         if (echo == null) {
            rider.getSoulLibrary().add(soul);
            continue;
         }
         double angle = Math.PI * 2.0 * index++ / Math.max(1, souls.size());
         double radius = 4.0 + (index % 5) * 1.4;
         echo.moveTo(rider.getX() + Math.cos(angle) * radius, rider.getY(), rider.getZ() + Math.sin(angle) * radius, rider.getYRot(), 0.0F);
         echo.setPaleRiderOwner(rider);
         echo.applySnapshot(soul);
         if (level.addFreshEntity(echo)) {
            rider.markManifested(soul);
         } else {
            rider.getSoulLibrary().add(soul);
         }
      }
   }

   private static void tickUnderworld(PaleRiderEntity rider, ServerLevel level, long now) {
      if (!rider.getPersistentData().getBoolean(TAG_UNDERWORLD_ACTIVE)
         && rider.getPersistentData().getLong(TAG_UNDERWORLD_UNTIL) > now) {
         rider.getPersistentData().putBoolean(TAG_UNDERWORLD_ACTIVE, true);
         rider.getPersistentData().remove(TAG_UNDERWORLD_UNTIL);
      }
      if (!rider.isUnderworldActive()) {
         if (rider.getPersistentData().getLong(TAG_UNDERWORLD_UNTIL) != 0L) {
            rider.getPersistentData().remove(TAG_UNDERWORLD_UNTIL);
            rider.returnAllLivingSouls();
            updateSoulSpeed(rider, 0);
            VFXServerEffects.spawn(level, "pale_rider_underworld_end", rider, 64.0);
         }
         return;
      }
      int tiers = countLivingSoulTiers(rider);
      updateSoulSpeed(rider, tiers);
      if (now - rider.getPersistentData().getLong(TAG_LAST_SOUL_REGEN) >= 20L) {
         rider.getPersistentData().putLong(TAG_LAST_SOUL_REGEN, now);
         rider.setCurrentMp(Math.min(rider.getMaxMp(), rider.getCurrentMp() + tiers));
      }
      if (InfectionRules.isScheduled(rider.getId(), now, 40)) spawnDomainShell(level, rider.position(), 50.0, false);
   }

   private static void startCalamity(PaleRiderEntity rider, ServerLevel level, long now) {
      rider.setCurrentMp(rider.getCurrentMp() - PaleRiderCombatRules.CALAMITY_CAST_MP_COST);
      CompoundTag data = rider.getPersistentData();
      data.putBoolean(TAG_CALAMITY_ACTIVE, true);
      PaleRiderCorruptionService.begin(rider);
      data.putLong(TAG_CALAMITY_COOLDOWN, now + 2400L);
      data.putBoolean(calamityEnabledTag(ApocalypseHorsemanEntity.Calamity.SWORD), true);
      data.putBoolean(calamityEnabledTag(ApocalypseHorsemanEntity.Calamity.FAMINE), true);
      data.putBoolean(calamityEnabledTag(ApocalypseHorsemanEntity.Calamity.BEAST), true);
      data.putLong(TAG_LAST_DOMAIN_TICK, now - 20L);
      data.putLong(TAG_LAST_DEATH_JUDGMENT, now - PaleRiderCombatRules.DEATH_JUDGMENT_INTERVAL_TICKS);
      net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantVoiceHelper.tryPlayPaleRiderFourCalamities(rider);
      VFXServerEffects.spawn(level, "pale_rider_calamity_open", rider, 40.0);
      ensureCalamityHorsemen(rider, level);
   }

   private static void tickCalamity(PaleRiderEntity rider, ServerLevel level, long now) {
      if (!rider.isCalamityActive()) return;
      if (InfectionRules.isScheduled(rider.getId(), now, 20)) ensureCalamityHorsemen(rider, level);
      PaleRiderCorruptionService.tickDomain(rider, level);
      if (rider.getCurrentMp() < PaleRiderCombatRules.CALAMITY_UPKEEP_MP_PER_SECOND) {
         endCalamity(rider, level);
         return;
      }
      if (now - rider.getPersistentData().getLong(TAG_LAST_DOMAIN_TICK) < 20L) return;
      rider.getPersistentData().putLong(TAG_LAST_DOMAIN_TICK, now);
      rider.setCurrentMp(rider.getCurrentMp() - PaleRiderCombatRules.CALAMITY_UPKEEP_MP_PER_SECOND);
      List<LivingEntity> enemies = enemies(rider, level, 25.0);
      boolean judgeDeath = now - rider.getPersistentData().getLong(TAG_LAST_DEATH_JUDGMENT) >= PaleRiderCombatRules.DEATH_JUDGMENT_INTERVAL_TICKS;
      if (judgeDeath) rider.getPersistentData().putLong(TAG_LAST_DEATH_JUDGMENT, now);
      for (LivingEntity enemy : enemies) {
         if (isEnabled(rider, ApocalypseHorsemanEntity.Calamity.FAMINE)) {
            enemy.hurt(rider.damageSources().source(PaleRiderDamageTypes.FAMINE, rider), 5.0F);
            applyTimedAttackPenalty(enemy, FAMINE_ATTACK_ID, -0.25, now + 30L);
         }
         if (judgeDeath && PaleRiderInfectionService.isInfected(enemy)) {
            int infectionLevel = PaleRiderInfectionService.getLevel(enemy);
            if (rider.getRandom().nextFloat() < InfectionRules.conceptDeathChance(infectionLevel)) {
               enemy.hurt(rider.damageSources().source(PaleRiderDamageTypes.CONCEPT_DEATH, rider), Math.max(1000.0F, enemy.getMaxHealth() * 4.0F));
               if (enemy.isAlive()) enemy.setHealth(0.0F);
            } else {
               enemy.hurt(rider.damageSources().source(PaleRiderDamageTypes.CONCEPT_DEATH, rider), 100.0F);
            }
         }
      }
      if (isEnabled(rider, ApocalypseHorsemanEntity.Calamity.SWORD)) {
         for (int index = 0; index < 2 && !enemies.isEmpty(); index++) {
            LivingEntity target = enemies.get(rider.getRandom().nextInt(enemies.size()));
            level.addFreshEntity(new ConceptSwordEntity(level, rider, target, 30.0F + rider.getRandom().nextFloat() * 20.0F));
         }
      }
      if (isEnabled(rider, ApocalypseHorsemanEntity.Calamity.BEAST) && now % 40L == 0L && countOwned(level, rider, RatSwarmEntity.class) < 100) {
         spawnPlagueAnimals(rider, level, true);
      }
      spawnDomainShell(level, rider.position(), 25.0, true);
   }

   private static void endCalamity(PaleRiderEntity rider, ServerLevel level) {
      rider.getPersistentData().remove(TAG_CALAMITY_ACTIVE);
      PaleRiderCorruptionService.end(rider);
      rider.getPersistentData().remove(TAG_LAST_DEATH_JUDGMENT);
      for (ApocalypseHorsemanEntity horseman : PaleRiderEntityIndex.owned(level, rider.getUUID(), ApocalypseHorsemanEntity.class,
         entity -> rider.getUUID().equals(entity.getPaleRiderOwnerUuid()) && !entity.isPaleRiderProxy())) horseman.discard();
      for (RatSwarmEntity rats : PaleRiderEntityIndex.owned(level, rider.getUUID(), RatSwarmEntity.class,
         entity -> rider.getUUID().equals(entity.getPaleRiderOwnerUuid()) && entity.isDomainSpawned())) rats.discard();
      for (PaleRiderCrowEntity crow : PaleRiderEntityIndex.owned(level, rider.getUUID(), PaleRiderCrowEntity.class,
         entity -> rider.getUUID().equals(entity.getPaleRiderOwnerUuid()) && entity.isDomainSpawned())) crow.discard();
      VFXServerEffects.spawn(level, "pale_rider_calamity_end", rider, 40.0);
   }

   private static void tickMounts(PaleRiderEntity rider, ServerLevel level, long now) {
      if (!rider.hasAnyDomain()) {
         if (rider.getVehicle() instanceof ApocalypseHorseEntity) rider.stopRiding();
         for (ApocalypseHorseEntity horse : PaleRiderEntityIndex.owned(level, rider.getUUID(), ApocalypseHorseEntity.class,
            entity -> rider.getUUID().equals(entity.getPaleRiderOwnerUuid()))) horse.discard();
         return;
      }
      for (ApocalypseHorseEntity horse : PaleRiderEntityIndex.owned(level, rider.getUUID(), ApocalypseHorseEntity.class,
         entity -> rider.getUUID().equals(entity.getPaleRiderOwnerUuid()) && entity.getPassengers().isEmpty())) horse.discard();
      if (rider.hasPossessedHost()) {
         ApocalypseHorsemanEntity proxy = PaleRiderEntityIndex.owned(level, rider.getUUID(), ApocalypseHorsemanEntity.class,
            entity -> rider.getUUID().equals(entity.getPaleRiderOwnerUuid()) && entity.isPaleRiderProxy()).stream().findFirst().orElse(null);
         if (proxy == null) {
            proxy = ModEntities.APOCALYPSE_HORSEMAN.get().create(level);
            if (proxy != null) {
               proxy.setPaleRiderOwner(rider);
               proxy.setPaleRiderProxy(true);
               proxy.moveTo(rider.getX() + 2.0, rider.getY(), rider.getZ(), rider.getYRot(), 0.0F);
               level.addFreshEntity(proxy);
            }
         }
         if (proxy != null) ensureHorse(rider, proxy, level, now);
      } else {
         for (ApocalypseHorsemanEntity proxy : PaleRiderEntityIndex.owned(level, rider.getUUID(), ApocalypseHorsemanEntity.class,
            entity -> rider.getUUID().equals(entity.getPaleRiderOwnerUuid()) && entity.isPaleRiderProxy())) proxy.discard();
         ensureHorse(rider, rider, level, now);
      }
      for (ApocalypseHorsemanEntity horseman : PaleRiderEntityIndex.owned(level, rider.getUUID(), ApocalypseHorsemanEntity.class,
         entity -> rider.getUUID().equals(entity.getPaleRiderOwnerUuid()))) ensureHorse(rider, horseman, level, now);
   }

   private static void ensureCalamityHorsemen(PaleRiderEntity rider, ServerLevel level) {
      List<ApocalypseHorsemanEntity> existing = PaleRiderEntityIndex.owned(level, rider.getUUID(), ApocalypseHorsemanEntity.class,
         entity -> rider.getUUID().equals(entity.getPaleRiderOwnerUuid()) && !entity.isPaleRiderProxy());
      for (ApocalypseHorsemanEntity.Calamity calamity : ApocalypseHorsemanEntity.Calamity.values()) {
         List<ApocalypseHorsemanEntity> matching = existing.stream().filter(entity -> entity.getCalamity() == calamity).toList();
         if (matching.isEmpty()) {
            spawnCalamityHorseman(rider, level, calamity);
         } else if (matching.size() > 1) {
            for (int index = 1; index < matching.size(); index++) matching.get(index).discard();
         }
      }
   }

   private static void spawnCalamityHorseman(PaleRiderEntity rider, ServerLevel level, ApocalypseHorsemanEntity.Calamity calamity) {
      ApocalypseHorsemanEntity horseman = ModEntities.APOCALYPSE_HORSEMAN.get().create(level);
      if (horseman == null) return;
      horseman.setPaleRiderOwner(rider);
      horseman.setCalamity(calamity);
      double angle = Math.PI * 2.0 * calamity.ordinal() / 3.0;
      horseman.moveTo(rider.getX() + Math.cos(angle) * 5.0, rider.getY(), rider.getZ() + Math.sin(angle) * 5.0, rider.getYRot(), 0.0F);
      level.addFreshEntity(horseman);
   }

   private static void ensureHorse(PaleRiderEntity rider, LivingEntity passenger, ServerLevel level, long now) {
      if (passenger.isPassenger()) return;
      ApocalypseHorseEntity horse = ModEntities.APOCALYPSE_HORSE.get().create(level);
      if (horse == null) return;
      horse.setPaleRiderOwner(rider);
      horse.moveTo(passenger.getX(), passenger.getY(), passenger.getZ(), passenger.getYRot(), 0.0F);
      horse.setHealth(horse.getMaxHealth());
      if (!level.addFreshEntity(horse) || !passenger.startRiding(horse, true)) horse.discard();
   }

   private static void tickAuraAndInfections(PaleRiderEntity rider, ServerLevel level) {
      for (LivingEntity enemy : enemies(rider, level, 15.0)) {
         applyTimedAttackPenalty(enemy, FEAR_ATTACK_ID, -0.15, level.getGameTime() + 30L);
         String key = "PaleRiderFearChecked_" + rider.getUUID();
         boolean checkedBefore = enemy.getPersistentData().contains(key);
         long checked = enemy.getPersistentData().getLong(key);
         if (!checkedBefore || level.getGameTime() - checked >= 600L) {
            enemy.getPersistentData().putLong(key, level.getGameTime());
            if (rider.getRandom().nextFloat() < 0.30F) {
               enemy.addEffect(new MobEffectInstance(ModMobEffects.PALE_RIDER_FEAR, 30, 0, false, true, true));
            }
         }
      }
   }

   private static void spawnPlagueAnimals(PaleRiderEntity rider, ServerLevel level, boolean domain) {
      if (!PaleRiderInfectionService.hasControlCapacity(rider, 1)) return;
      RatSwarmEntity swarm = ModEntities.RAT_SWARM.get().create(level);
      Vec3 offset = new Vec3(rider.getRandom().nextDouble() * 4.0 - 2.0, 0.0, rider.getRandom().nextDouble() * 4.0 - 2.0);
      if (swarm != null) {
         swarm.moveTo(rider.getX() + offset.x, rider.getY(), rider.getZ() + offset.z, rider.getYRot(), 0.0F);
         swarm.setPaleRiderOwner(rider);
         swarm.setDomainSpawned(domain);
         swarm.setSwarmHealth(RatSwarmRules.MAX_HEALTH);
         level.addFreshEntity(swarm);
      }
      if (!PaleRiderInfectionService.hasControlCapacity(rider, 1)
         || !PaleRiderCombatRules.canSpawnCrow(countOwnedCrows(level, rider))) return;
      PaleRiderCrowEntity crow = ModEntities.PALE_RIDER_CROW.get().create(level);
      if (crow == null) return;
      crow.moveTo(rider.getX() - offset.z, rider.getY() + 2.0, rider.getZ() + offset.x, rider.getYRot(), 0.0F);
      crow.setPaleRiderOwner(rider);
      crow.setDomainSpawned(domain);
      level.addFreshEntity(crow);
   }

   private static int countOwnedCrows(ServerLevel level, PaleRiderEntity rider) {
      return PaleRiderEntityIndex.owned(level, rider.getUUID(), PaleRiderCrowEntity.class,
         entity -> rider.getUUID().equals(entity.getPaleRiderOwnerUuid())).size();
   }

   private static void applyTimedAttackPenalty(LivingEntity target, net.minecraft.resources.ResourceLocation id, double amount, long until) {
      AttributeInstance attack = target.getAttribute(Attributes.ATTACK_DAMAGE);
      if (attack == null) return;
      AttributeModifier current = attack.getModifier(id);
      if (current == null || Math.abs(current.amount() - amount) > 1.0E-6
         || current.operation() != AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
         if (current != null) attack.removeModifier(id);
         attack.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
      }
      target.getPersistentData().putLong("PaleRiderPenaltyUntil_" + id.getPath(), until);
   }

   public static void cleanupExpiredPenalties(LivingEntity target) {
      long now = target.level().getGameTime();
      cleanupPenalty(target, FEAR_ATTACK_ID, now);
      cleanupPenalty(target, FAMINE_ATTACK_ID, now);
   }

   public static boolean hasExpiredPenaltyMarkers(LivingEntity target) {
      if (target == null) {
         return false;
      }
      CompoundTag data = target.getPersistentData();
      return data.contains("PaleRiderPenaltyUntil_" + FEAR_ATTACK_ID.getPath())
         || data.contains("PaleRiderPenaltyUntil_" + FAMINE_ATTACK_ID.getPath());
   }

   private static void cleanupPenalty(LivingEntity target, net.minecraft.resources.ResourceLocation id, long now) {
      String tag = "PaleRiderPenaltyUntil_" + id.getPath();
      if (target.getPersistentData().getLong(tag) > now) return;
      AttributeInstance attack = target.getAttribute(Attributes.ATTACK_DAMAGE);
      if (attack != null) attack.removeModifier(id);
      target.getPersistentData().remove(tag);
   }

   public static int countLivingSoulTiers(PaleRiderEntity rider) {
      if (!(rider.level() instanceof ServerLevel level)) return 0;
      int count = countOwned(level, rider, SoulEchoEntity.class);
      return count / 10;
   }

   private static void updateSoulSpeed(PaleRiderEntity rider, int tiers) {
      AttributeInstance speed = rider.getAttribute(Attributes.MOVEMENT_SPEED);
      if (speed == null) return;
      net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, SPEED_ID.path);
      double amount = tiers * 0.05;
      AttributeModifier current = speed.getModifier(id);
      if (tiers <= 0) {
         if (current != null) speed.removeModifier(id);
         return;
      }
      if (current == null || Math.abs(current.amount() - amount) > 1.0E-6
         || current.operation() != AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
         if (current != null) speed.removeModifier(id);
         speed.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
      }
   }

   public static void cleanupAll(PaleRiderEntity rider, boolean returnSouls) {
      rider.endPossession();
      if (!(rider.level() instanceof ServerLevel level)) return;
      if (returnSouls) rider.returnAllLivingSouls();
      for (OwnedPaleRiderMob owned : PaleRiderEntityIndex.owned(level, rider.getUUID(), OwnedPaleRiderMob.class,
         entity -> rider.getUUID().equals(entity.getPaleRiderOwnerUuid()))) owned.discard();
      for (PaleRiderCrowEntity crow : PaleRiderEntityIndex.owned(level, rider.getUUID(), PaleRiderCrowEntity.class,
         entity -> rider.getUUID().equals(entity.getPaleRiderOwnerUuid()))) crow.discard();
      rider.getPersistentData().remove(TAG_UNDERWORLD_UNTIL);
      rider.getPersistentData().remove(TAG_UNDERWORLD_ACTIVE);
      rider.getPersistentData().remove(TAG_CALAMITY_ACTIVE);
      rider.getPersistentData().remove(TAG_NEXT_POSSESSION_SCAN);
      PaleRiderCorruptionService.end(rider);
      updateSoulSpeed(rider, 0);
   }

   public static String calamityEnabledTag(ApocalypseHorsemanEntity.Calamity calamity) {
      return "PaleRiderCalamity" + calamity.name() + "Enabled";
   }

   private static boolean isEnabled(PaleRiderEntity rider, ApocalypseHorsemanEntity.Calamity calamity) {
      return rider.getPersistentData().getBoolean(calamityEnabledTag(calamity));
   }

   private static List<LivingEntity> enemies(PaleRiderEntity rider, ServerLevel level, double radius) {
      return level.getEntitiesOfClass(LivingEntity.class, rider.getBoundingBox().inflate(radius),
         target -> target != rider && target.isAlive() && !target.isAlliedTo(rider) && !rider.isAlliedTo(target) && !EntityUtils.isImmunePlayerTarget(target));
   }

   private static int countEnemies(PaleRiderEntity rider, ServerLevel level, double radius) {
      return enemies(rider, level, radius).size();
   }

   private static <T extends OwnedPaleRiderMob> int countOwned(ServerLevel level, PaleRiderEntity rider, Class<T> type) {
      return PaleRiderEntityIndex.owned(level, rider.getUUID(), type, entity -> rider.getUUID().equals(entity.getPaleRiderOwnerUuid())).size();
   }

   private static void spawnDomainShell(ServerLevel level, Vec3 center, double radius, boolean inner) {
      if (!level.hasNearbyAlivePlayer(center.x, center.y, center.z, radius + 32.0)) return;
      DustParticleOptions shellDust = new DustParticleOptions(new Vector3f(inner ? 0.24F : 0.68F, inner ? 0.24F : 0.7F, inner ? 0.26F : 0.74F), inner ? 1.15F : 0.9F);
      int shellSamples = inner ? 16 : 24;
      for (int index = 0; index < shellSamples; index++) {
         double theta = level.getRandom().nextDouble() * Math.PI * 2.0;
         double phi = Math.acos(2.0 * level.getRandom().nextDouble() - 1.0);
         level.sendParticles(shellDust, center.x + radius * Math.sin(phi) * Math.cos(theta), center.y + radius * Math.cos(phi),
            center.z + radius * Math.sin(phi) * Math.sin(theta), 1, 0.0, 0.0, 0.0, 0.0);
      }
      int deathSamples = inner ? 8 : 12;
      for (int index = 0; index < deathSamples; index++) {
         double theta = level.getRandom().nextDouble() * Math.PI * 2.0;
         double distance = radius * Math.sqrt(level.getRandom().nextDouble()) * 0.92;
         double x = center.x + Math.cos(theta) * distance;
         double z = center.z + Math.sin(theta) * distance;
         double y = center.y + level.getRandom().nextDouble() * Math.min(12.0, radius * 0.35);
         net.minecraft.core.particles.ParticleOptions particle = switch (index % 5) {
            case 0 -> ParticleTypes.SOUL;
            case 1 -> ParticleTypes.SCULK_SOUL;
            case 2 -> ParticleTypes.LARGE_SMOKE;
            default -> ParticleTypes.ASH;
         };
         level.sendParticles(particle, x, y, z, 1, 0.18, 0.35, 0.18, 0.015);
      }
   }

   private record ResourceLocationId(String path) {
   }
}
