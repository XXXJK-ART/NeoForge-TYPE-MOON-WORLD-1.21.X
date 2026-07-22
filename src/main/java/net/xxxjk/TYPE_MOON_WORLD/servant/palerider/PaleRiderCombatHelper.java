package net.xxxjk.TYPE_MOON_WORLD.servant.palerider;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
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
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.SoulEchoEntity;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import org.joml.Vector3f;

public final class PaleRiderCombatHelper {
   public static final String TAG_UNDERWORLD_UNTIL = "PaleRiderUnderworldUntil";
   public static final String TAG_CALAMITY_ACTIVE = "PaleRiderCalamityActive";
   private static final String TAG_UNDERWORLD_COOLDOWN = "PaleRiderUnderworldCooldown";
   private static final String TAG_CALAMITY_COOLDOWN = "PaleRiderCalamityCooldown";
   private static final String TAG_LAST_RAT = "PaleRiderLastRat";
   private static final String TAG_LAST_DOMAIN_TICK = "PaleRiderLastDomainTick";
   private static final String TAG_LAST_FEAR_TICK = "PaleRiderLastFearTick";
   private static final String TAG_LAST_SOUL_REGEN = "PaleRiderLastSoulRegen";
   private static final String TAG_LAST_ENVIRONMENT = "PaleRiderLastEnvironment";
   private static final String TAG_DEATH_CHECKS = "PaleRiderDeathChecks";
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
      rider.tickPossession();
      if (now - rider.getPersistentData().getLong(TAG_LAST_FEAR_TICK) >= 20L) {
         rider.getPersistentData().putLong(TAG_LAST_FEAR_TICK, now);
         tickAuraAndInfections(rider, level);
      }
      tickUnderworld(rider, level, now);
      tickCalamity(rider, level, now);
      tickMounts(rider, level, now);
      if (rider.hasAnyDomain() && now - rider.getPersistentData().getLong(TAG_LAST_ENVIRONMENT) >= 100L) {
         rider.getPersistentData().putLong(TAG_LAST_ENVIRONMENT, now);
         VFXServerEffects.spawn(level, "pale_rider_domain_sustain", rider, 128.0);
      }
      tickPhaseAi(rider, level, now);
   }

   private static void tickPhaseAi(PaleRiderEntity rider, ServerLevel level, long now) {
      LivingEntity master = rider.getMaster();
      if (master != null) {
         LivingEntity threat = master.getLastHurtByMob();
         if (threat != null && threat.isAlive() && !rider.isAlliedTo(threat)) rider.setTarget(threat);
      } else if (rider.getLastHurtByMob() == null) {
         rider.setTarget(null);
         return;
      }
      LivingEntity target = rider.findPaleRiderEnemy(64.0);
      if (target == null) return;
      rider.setTarget(target);
      double ratio = rider.getHealth() / Math.max(1.0F, rider.getMaxHealth());
      int desiredRats = ratio > 0.8 ? 3 : ratio > 0.6 ? 8 : 100;
      int interval = ratio > 0.8 ? 240 : ratio > 0.6 ? 160 : 40;
      int currentRats = countOwned(level, rider, RatSwarmEntity.class);
      if (currentRats < desiredRats && currentRats < 100 && now - rider.getPersistentData().getLong(TAG_LAST_RAT) >= interval && rider.getCurrentMp() >= 10.0) {
         rider.setCurrentMp(rider.getCurrentMp() - 10.0);
         rider.getPersistentData().putLong(TAG_LAST_RAT, now);
         spawnRatSwarm(rider, level, false);
      }
      if (ratio <= 0.8 && !rider.isUnderworldActive() && rider.getSoulLibrary().size() >= 10
         && now >= rider.getPersistentData().getLong(TAG_UNDERWORLD_COOLDOWN) && rider.getCurrentMp() >= 150.0
         && (ratio <= 0.6 || countEnemies(rider, level, 32.0) >= 2 || target.getMaxHealth() >= 160.0F)) {
         startUnderworld(rider, level, now);
      }
      if (ratio <= 0.6 && !rider.isCalamityActive() && now >= rider.getPersistentData().getLong(TAG_CALAMITY_COOLDOWN)
         && rider.getCurrentMp() >= 170.0 && hasInfectedEnemy(rider, level, 25.0)) {
         startCalamity(rider, level, now);
      }
      if (!rider.hasAnyDomain() && !rider.isPassenger() && rider.distanceToSqr(target) > 18.0 * 18.0 && rider.getNavigation().isStuck()) {
         level.getEntitiesOfClass(Mob.class, rider.getBoundingBox().inflate(8.0),
            mob -> mob != rider && mob.isAlive() && !(mob instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity) && !mob.getType().is(net.neoforged.neoforge.common.Tags.EntityTypes.BOSSES))
            .stream().findFirst().ifPresent(rider::beginPossession);
      }
   }

   private static void startUnderworld(PaleRiderEntity rider, ServerLevel level, long now) {
      rider.endPossession();
      rider.setCurrentMp(rider.getCurrentMp() - 150.0);
      rider.getPersistentData().putLong(TAG_UNDERWORLD_UNTIL, now + 1200L);
      rider.getPersistentData().putLong(TAG_UNDERWORLD_COOLDOWN, now + 2400L);
      VFXServerEffects.spawn(level, "pale_rider_domain_open", rider, 128.0);
      List<SoulSnapshot> souls = rider.getSoulLibrary().takeStrongest(50);
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
      if (!rider.isUnderworldActive()) {
         if (rider.getPersistentData().getLong(TAG_UNDERWORLD_UNTIL) != 0L) {
            rider.getPersistentData().remove(TAG_UNDERWORLD_UNTIL);
            rider.returnAllLivingSouls();
            updateSoulSpeed(rider, 0);
            VFXServerEffects.spawn(level, "pale_rider_domain_end", rider, 128.0);
         }
         return;
      }
      int tiers = countLivingSoulTiers(rider);
      updateSoulSpeed(rider, tiers);
      if (now - rider.getPersistentData().getLong(TAG_LAST_SOUL_REGEN) >= 20L) {
         rider.getPersistentData().putLong(TAG_LAST_SOUL_REGEN, now);
         rider.setCurrentMp(Math.min(rider.getMaxMp(), rider.getCurrentMp() + tiers));
      }
      if (now % 10L == 0L) spawnDomainShell(level, rider.position(), 50.0, false);
   }

   private static void startCalamity(PaleRiderEntity rider, ServerLevel level, long now) {
      rider.endPossession();
      rider.setCurrentMp(rider.getCurrentMp() - 150.0);
      CompoundTag data = rider.getPersistentData();
      data.putBoolean(TAG_CALAMITY_ACTIVE, true);
      data.putLong(TAG_CALAMITY_COOLDOWN, now + 2400L);
      data.putBoolean(calamityEnabledTag(ApocalypseHorsemanEntity.Calamity.SWORD), true);
      data.putBoolean(calamityEnabledTag(ApocalypseHorsemanEntity.Calamity.FAMINE), true);
      data.putBoolean(calamityEnabledTag(ApocalypseHorsemanEntity.Calamity.BEAST), true);
      data.putLong(TAG_LAST_DOMAIN_TICK, now - 20L);
      data.remove(TAG_DEATH_CHECKS);
      VFXServerEffects.spawn(level, "pale_rider_domain_open", rider, 128.0);
      for (ApocalypseHorsemanEntity.Calamity calamity : ApocalypseHorsemanEntity.Calamity.values()) {
         ApocalypseHorsemanEntity horseman = ModEntities.APOCALYPSE_HORSEMAN.get().create(level);
         if (horseman == null) continue;
         horseman.setPaleRiderOwner(rider);
         horseman.setCalamity(calamity);
         double angle = Math.PI * 2.0 * calamity.ordinal() / 3.0;
         horseman.moveTo(rider.getX() + Math.cos(angle) * 4.0, rider.getY(), rider.getZ() + Math.sin(angle) * 4.0, rider.getYRot(), 0.0F);
         level.addFreshEntity(horseman);
      }
   }

   private static void tickCalamity(PaleRiderEntity rider, ServerLevel level, long now) {
      if (!rider.isCalamityActive()) return;
      if (!hasInfectedEnemy(rider, level, 25.0) || rider.getCurrentMp() < 20.0) {
         endCalamity(rider, level);
         return;
      }
      if (now - rider.getPersistentData().getLong(TAG_LAST_DOMAIN_TICK) < 20L) return;
      rider.getPersistentData().putLong(TAG_LAST_DOMAIN_TICK, now);
      rider.setCurrentMp(rider.getCurrentMp() - 20.0);
      List<LivingEntity> enemies = enemies(rider, level, 25.0);
      CompoundTag deathChecks = rider.getPersistentData().getCompound(TAG_DEATH_CHECKS);
      for (LivingEntity enemy : enemies) {
         if (isEnabled(rider, ApocalypseHorsemanEntity.Calamity.FAMINE)) {
            enemy.hurt(rider.damageSources().source(PaleRiderDamageTypes.FAMINE, rider), 5.0F);
            applyTimedAttackPenalty(enemy, FAMINE_ATTACK_ID, -0.25, now + 30L);
         }
         if (PaleRiderInfectionService.isInfected(enemy) && !deathChecks.getBoolean(enemy.getUUID().toString())) {
            deathChecks.putBoolean(enemy.getUUID().toString(), true);
            if (rider.getRandom().nextFloat() < 0.20F) {
               enemy.hurt(rider.damageSources().source(PaleRiderDamageTypes.CONCEPT_DEATH, rider), Math.max(1000.0F, enemy.getMaxHealth() * 4.0F));
               if (enemy.isAlive()) enemy.setHealth(0.0F);
            } else {
               enemy.hurt(rider.damageSources().source(PaleRiderDamageTypes.CONCEPT_DEATH, rider), 100.0F);
            }
         }
      }
      rider.getPersistentData().put(TAG_DEATH_CHECKS, deathChecks);
      if (isEnabled(rider, ApocalypseHorsemanEntity.Calamity.SWORD)) {
         for (int index = 0; index < 2 && !enemies.isEmpty(); index++) {
            LivingEntity target = enemies.get(rider.getRandom().nextInt(enemies.size()));
            level.addFreshEntity(new ConceptSwordEntity(level, rider, target, 30.0F + rider.getRandom().nextFloat() * 20.0F));
         }
      }
      if (isEnabled(rider, ApocalypseHorsemanEntity.Calamity.BEAST) && now % 40L == 0L && countOwned(level, rider, RatSwarmEntity.class) < 100) {
         spawnRatSwarm(rider, level, true);
      }
      spawnDomainShell(level, rider.position(), 25.0, true);
   }

   private static void endCalamity(PaleRiderEntity rider, ServerLevel level) {
      rider.getPersistentData().remove(TAG_CALAMITY_ACTIVE);
      rider.getPersistentData().remove(TAG_DEATH_CHECKS);
      for (ApocalypseHorsemanEntity horseman : level.getEntitiesOfClass(ApocalypseHorsemanEntity.class, rider.getBoundingBox().inflate(160.0),
         entity -> rider.getUUID().equals(entity.getPaleRiderOwnerUuid()))) horseman.discard();
      for (RatSwarmEntity rats : level.getEntitiesOfClass(RatSwarmEntity.class, rider.getBoundingBox().inflate(160.0),
         entity -> rider.getUUID().equals(entity.getPaleRiderOwnerUuid()) && entity.isDomainSpawned())) rats.discard();
      VFXServerEffects.spawn(level, "pale_rider_domain_end", rider, 128.0);
   }

   private static void tickMounts(PaleRiderEntity rider, ServerLevel level, long now) {
      if (!rider.hasAnyDomain()) {
         if (rider.getVehicle() instanceof ApocalypseHorseEntity) rider.stopRiding();
         for (ApocalypseHorseEntity horse : level.getEntitiesOfClass(ApocalypseHorseEntity.class, rider.getBoundingBox().inflate(160.0),
            entity -> rider.getUUID().equals(entity.getPaleRiderOwnerUuid()))) horse.discard();
         return;
      }
      ensureHorse(rider, rider, level, now);
      for (ApocalypseHorsemanEntity horseman : level.getEntitiesOfClass(ApocalypseHorsemanEntity.class, rider.getBoundingBox().inflate(160.0),
         entity -> rider.getUUID().equals(entity.getPaleRiderOwnerUuid()))) ensureHorse(rider, horseman, level, now);
   }

   private static void ensureHorse(PaleRiderEntity rider, LivingEntity passenger, ServerLevel level, long now) {
      if (passenger.isPassenger() || now % 100L != Math.floorMod(passenger.getId(), 100)) return;
      ApocalypseHorseEntity horse = ModEntities.APOCALYPSE_HORSE.get().create(level);
      if (horse == null) return;
      horse.setPaleRiderOwner(rider);
      horse.moveTo(passenger.getX(), passenger.getY(), passenger.getZ(), passenger.getYRot(), 0.0F);
      horse.setHealth(horse.getMaxHealth());
      level.addFreshEntity(horse);
      passenger.startRiding(horse, true);
   }

   private static void tickAuraAndInfections(PaleRiderEntity rider, ServerLevel level) {
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, rider.getBoundingBox().inflate(64.0), LivingEntity::isAlive)) {
         if (PaleRiderInfectionService.isInfected(living)) PaleRiderInfectionService.tick(living);
      }
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

   private static void spawnRatSwarm(PaleRiderEntity rider, ServerLevel level, boolean domain) {
      RatSwarmEntity swarm = ModEntities.RAT_SWARM.get().create(level);
      if (swarm == null) return;
      Vec3 offset = new Vec3(rider.getRandom().nextDouble() * 4.0 - 2.0, 0.0, rider.getRandom().nextDouble() * 4.0 - 2.0);
      swarm.moveTo(rider.getX() + offset.x, rider.getY(), rider.getZ() + offset.z, rider.getYRot(), 0.0F);
      swarm.setPaleRiderOwner(rider);
      swarm.setDomainSpawned(domain);
      swarm.setSwarmHealth(RatSwarmRules.MAX_HEALTH);
      level.addFreshEntity(swarm);
   }

   private static void applyTimedAttackPenalty(LivingEntity target, net.minecraft.resources.ResourceLocation id, double amount, long until) {
      AttributeInstance attack = target.getAttribute(Attributes.ATTACK_DAMAGE);
      if (attack == null) return;
      attack.removeModifier(id);
      attack.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
      target.getPersistentData().putLong("PaleRiderPenaltyUntil_" + id.getPath(), until);
   }

   public static void cleanupExpiredPenalties(LivingEntity target) {
      long now = target.level().getGameTime();
      cleanupPenalty(target, FEAR_ATTACK_ID, now);
      cleanupPenalty(target, FAMINE_ATTACK_ID, now);
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
      speed.removeModifier(id);
      if (tiers > 0) speed.addTransientModifier(new AttributeModifier(id, tiers * 0.05, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
   }

   public static void cleanupAll(PaleRiderEntity rider, boolean returnSouls) {
      rider.endPossession();
      if (!(rider.level() instanceof ServerLevel level)) return;
      if (returnSouls) rider.returnAllLivingSouls();
      for (OwnedPaleRiderMob owned : level.getEntitiesOfClass(OwnedPaleRiderMob.class, rider.getBoundingBox().inflate(192.0),
         entity -> rider.getUUID().equals(entity.getPaleRiderOwnerUuid()))) owned.discard();
      rider.getPersistentData().remove(TAG_UNDERWORLD_UNTIL);
      rider.getPersistentData().remove(TAG_CALAMITY_ACTIVE);
      updateSoulSpeed(rider, 0);
   }

   public static String calamityEnabledTag(ApocalypseHorsemanEntity.Calamity calamity) {
      return "PaleRiderCalamity" + calamity.name() + "Enabled";
   }

   private static boolean isEnabled(PaleRiderEntity rider, ApocalypseHorsemanEntity.Calamity calamity) {
      return rider.getPersistentData().getBoolean(calamityEnabledTag(calamity));
   }

   private static boolean hasInfectedEnemy(PaleRiderEntity rider, ServerLevel level, double radius) {
      return enemies(rider, level, radius).stream().anyMatch(PaleRiderInfectionService::isInfected);
   }

   private static List<LivingEntity> enemies(PaleRiderEntity rider, ServerLevel level, double radius) {
      return level.getEntitiesOfClass(LivingEntity.class, rider.getBoundingBox().inflate(radius),
         target -> target != rider && target.isAlive() && !target.isAlliedTo(rider) && !rider.isAlliedTo(target) && !EntityUtils.isImmunePlayerTarget(target));
   }

   private static int countEnemies(PaleRiderEntity rider, ServerLevel level, double radius) {
      return enemies(rider, level, radius).size();
   }

   private static <T extends OwnedPaleRiderMob> int countOwned(ServerLevel level, PaleRiderEntity rider, Class<T> type) {
      return level.getEntitiesOfClass(type, new AABB(-3.0E7, level.getMinBuildHeight(), -3.0E7, 3.0E7, level.getMaxBuildHeight(), 3.0E7),
         entity -> rider.getUUID().equals(entity.getPaleRiderOwnerUuid())).size();
   }

   private static void spawnDomainShell(ServerLevel level, Vec3 center, double radius, boolean inner) {
      DustParticleOptions dust = new DustParticleOptions(new Vector3f(inner ? 0.42F : 0.72F, inner ? 0.42F : 0.72F, inner ? 0.42F : 0.75F), 0.75F);
      for (int index = 0; index < 24; index++) {
         double theta = level.getRandom().nextDouble() * Math.PI * 2.0;
         double phi = Math.acos(2.0 * level.getRandom().nextDouble() - 1.0);
         level.sendParticles(dust, center.x + radius * Math.sin(phi) * Math.cos(theta), center.y + radius * Math.cos(phi),
            center.z + radius * Math.sin(phi) * Math.sin(theta), 1, 0.0, 0.0, 0.0, 0.0);
      }
   }

   private record ResourceLocationId(String path) {
   }
}
