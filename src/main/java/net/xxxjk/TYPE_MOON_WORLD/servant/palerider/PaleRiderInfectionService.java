package net.xxxjk.TYPE_MOON_WORLD.servant.palerider;

import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.PaleRiderEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.neoforged.neoforge.common.Tags;

public final class PaleRiderInfectionService {
   public static final int MAX_CONTROLLED = 100;
   public static final String TAG_LEVEL = "PaleRiderInfectionLevel";
   public static final String TAG_OWNER = "PaleRiderInfectionOwner";
   public static final String TAG_UNTIL = "PaleRiderInfectionUntil";
   public static final String TAG_IMMUNE_UNTIL = "PaleRiderInfectionImmuneUntil";
   public static final String TAG_CONTROLLED = "PaleRiderControlled";
   public static final String TAG_STATIONARY_ANCHOR = "PaleRiderStationaryAnchor";
   public static final String TAG_PREVIOUS_NO_AI = "PaleRiderPreviousNoAi";
   public static final String TAG_WAS_IN_CALAMITY = "PaleRiderInfectionWasInCalamity";
   public static final String TAG_CALAMITY_EXIT_CLEANSE = "PaleRiderInfectionCalamityExitCleanse";
   private static final String LEGACY_TAG_LAST_SERVICE_TICK = "PaleRiderInfectionLastServiceTick";
   private static final String TAG_NEXT_CONTROLLED_ATTACK = "PaleRiderControlledNextAttack";
   private static final String TAG_NEXT_CONTROLLED_TARGET_SCAN = "PaleRiderControlledNextTargetScan";
   private static final String TAG_CONTROLLED_COMMAND_TARGET = "PaleRiderControlledCommandTarget";
   private static final String TAG_NEXT_CONTROLLED_NAVIGATION = "PaleRiderControlledNextNavigation";
   private static final String TAG_LETHAL_NEXT_TICK = "PaleRiderLethalNextTick";

   private PaleRiderInfectionService() {
   }

   public static boolean infect(LivingEntity target, LivingEntity owner, int addedLevels) {
      if (target == null || owner == null || !target.isAlive() || target == owner || target.isAlliedTo(owner)) {
         return false;
      }
      if (isPaleRiderCardPlayer(target) || isStoutArash(target)) {
         return false;
      }
      long now = target.level().getGameTime();
      CompoundTag data = target.getPersistentData();
      if (data.getLong(TAG_IMMUNE_UNTIL) > now) {
         return false;
      }
      int previous = getLevel(target);
      int next = Math.min(InfectionRules.MAX_LEVEL, previous + Math.max(1, addedLevels));
      boolean sameOwner = data.hasUUID(TAG_OWNER) && owner.getUUID().equals(data.getUUID(TAG_OWNER));
      long remaining = data.getLong(TAG_UNTIL) - now;
      if (next == previous && sameOwner && remaining > InfectionRules.EFFECT_REFRESH_THRESHOLD_TICKS) {
         retainInfectionEffectOnly(target);
         return false;
      }
      data.putInt(TAG_LEVEL, next);
      data.putUUID(TAG_OWNER, owner.getUUID());
      data.putLong(TAG_UNTIL, now + InfectionRules.DURATION_TICKS);
      retainInfectionEffectOnly(target);
      boolean forceEffectRefresh = next > previous || !sameOwner;
      refreshEffect(target, ModMobEffects.PALE_RIDER_INFECTION, next - 1, forceEffectRefresh);
      if (next > previous && canControl(target) && target.getRandom().nextDouble() < controlChance(target, next)) {
         beginControl((Mob)target, owner);
      }
      return next > previous;
   }

   public static void tick(LivingEntity target) {
      if (!(target.level() instanceof ServerLevel serverLevel)) {
         return;
      }
      long now = serverLevel.getGameTime();
      boolean controlled = isControlled(target);
      boolean maintenanceTick = InfectionRules.isScheduled(target.getId(), now, InfectionRules.DAMAGE_INTERVAL_TICKS);
      boolean controlledAiTick = controlled && target instanceof Mob
         && InfectionRules.isScheduled(target.getId(), now, InfectionRules.CONTROLLED_AI_INTERVAL_TICKS);
      if (!maintenanceTick && !controlledAiTick) {
         return;
      }
      if (isPaleRiderCardPlayer(target) || isStoutArash(target)) {
         if (target.getPersistentData().contains(TAG_LEVEL) || target.hasEffect(ModMobEffects.PALE_RIDER_INFECTION)) {
            cleanse(target, false);
         }
         return;
      }
      int level = getLevel(target);
      if (level <= 0) {
         if (target.getPersistentData().contains(TAG_LEVEL)) cleanse(target, false);
         return;
      }
      if (target.getPersistentData().getLong(TAG_UNTIL) <= now) {
         if (isControlled(target) && getOwner(serverLevel, target) instanceof LivingEntity livingOwner && livingOwner.isAlive()) {
            target.getPersistentData().putInt(TAG_LEVEL, InfectionRules.MAX_LEVEL);
            target.getPersistentData().putLong(TAG_UNTIL, now + InfectionRules.DURATION_TICKS);
            target.addEffect(new MobEffectInstance(ModMobEffects.PALE_RIDER_INFECTION, InfectionRules.DURATION_TICKS,
               InfectionRules.MAX_LEVEL - 1, false, true, true));
         } else {
            cleanse(target, false);
            return;
         }
      }
      LivingEntity owner = getOwner(serverLevel, target);
      if (owner == null || !owner.isAlive()) {
         cleanse(target, false);
         return;
      }
      if (owner instanceof PaleRiderEntity rider && tickCalamityExit(target, rider, level, now)) {
         return;
      }
      if (controlled && maintenanceTick) {
         PaleRiderEntityIndex.registerControlled(owner.getUUID(), target);
      }
      if (maintenanceTick) {
         if (!controlled) {
            target.hurt(owner.damageSources().source(PaleRiderDamageTypes.INFECTION, owner), InfectionRules.damagePerSecond(level));
         }
         if (InfectionRules.isScheduled(target.getId(), now, InfectionRules.SPREAD_INTERVAL_TICKS)) {
            spread(serverLevel, target, owner, level);
         }
      }
      if (controlledAiTick && target instanceof Mob mob) {
         tickControlledMob(serverLevel, mob, owner, now);
      }
   }

   private static void spread(ServerLevel level, LivingEntity source, LivingEntity owner, int infectionLevel) {
      int damageIntervals = InfectionRules.SPREAD_INTERVAL_TICKS / InfectionRules.DAMAGE_INTERVAL_TICKS;
      if (source.getRandom().nextDouble() >= InfectionRules.spreadChanceForInterval(infectionLevel, damageIntervals)) {
         return;
      }
      AABB area = source.getBoundingBox().inflate(5.0);
      LivingEntity nearest = null;
      double nearestDistance = Double.MAX_VALUE;
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
         candidate -> canReceiveInfection(candidate, source, owner))) {
         double distance = target.distanceToSqr(source);
         if (distance < nearestDistance) {
            nearest = target;
            nearestDistance = distance;
         }
      }
      if (nearest != null) infect(nearest, owner, 1);
   }

   public static void cleanse(LivingEntity target, boolean grantImmunity) {
      CompoundTag data = target.getPersistentData();
      data.remove(TAG_LEVEL);
      data.remove(TAG_OWNER);
      data.remove(TAG_UNTIL);
      data.remove(LEGACY_TAG_LAST_SERVICE_TICK);
      data.remove(TAG_WAS_IN_CALAMITY);
      data.remove(TAG_CALAMITY_EXIT_CLEANSE);
      target.removeEffect(ModMobEffects.PALE_RIDER_INFECTION);
      if (grantImmunity) {
         data.putLong(TAG_IMMUNE_UNTIL, target.level().getGameTime() + InfectionRules.IMMUNITY_TICKS);
      }
      endControl(target);
   }

   public static int getLevel(LivingEntity target) {
      CompoundTag data = target.getPersistentData();
      if (data.getLong(TAG_UNTIL) <= target.level().getGameTime()) {
         return isControlled(target) ? InfectionRules.MAX_LEVEL : 0;
      }
      return Math.max(0, Math.min(InfectionRules.MAX_LEVEL, data.getInt(TAG_LEVEL)));
   }

   public static boolean isInfected(LivingEntity target) {
      return getLevel(target) > 0;
   }

   public static boolean isPaleRiderCardPlayer(LivingEntity target) {
      if (!(target instanceof Player player)) return false;
      var vars = player.getData(net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.servant_card_transformed && "pale_rider".equals(vars.servant_card_id);
   }

   public static boolean isControlled(Entity entity) {
      return entity != null && entity.getPersistentData().getBoolean(TAG_CONTROLLED)
         && entity.getPersistentData().hasUUID(TAG_OWNER);
   }

   public static boolean isStationaryAnchor(Entity entity) {
      return entity != null && entity.getPersistentData().getBoolean(TAG_STATIONARY_ANCHOR);
   }

   public static boolean isForbiddenPossessionHost(Entity entity) {
      return entity instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.ApocalypseHorseEntity
         || entity instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.ApocalypseHorsemanEntity;
   }

   public static void markStationaryAnchor(Mob mob) {
      if (mob == null) return;
      CompoundTag data = mob.getPersistentData();
      data.putBoolean(TAG_STATIONARY_ANCHOR, true);
      data.putDouble(TAG_STATIONARY_ANCHOR + "X", mob.getX());
      data.putDouble(TAG_STATIONARY_ANCHOR + "Y", mob.getY());
      data.putDouble(TAG_STATIONARY_ANCHOR + "Z", mob.getZ());
      mob.setNoGravity(true);
      holdStationaryAnchor(mob);
   }

   public static void holdStationaryAnchor(Mob mob) {
      if (!isStationaryAnchor(mob)) return;
      CompoundTag data = mob.getPersistentData();
      double x = data.getDouble(TAG_STATIONARY_ANCHOR + "X");
      double y = data.getDouble(TAG_STATIONARY_ANCHOR + "Y");
      double z = data.getDouble(TAG_STATIONARY_ANCHOR + "Z");
      mob.setTarget(null);
      mob.setAggressive(false);
      mob.getNavigation().stop();
      mob.setDeltaMovement(Vec3.ZERO);
      mob.setNoGravity(true);
      if (mob.distanceToSqr(x, y, z) > 1.0E-6) mob.moveTo(x, y, z, mob.getYRot(), mob.getXRot());
   }

   public static LivingEntity getCommandTarget(Mob mob) {
      if (!(mob.level() instanceof ServerLevel level)) return null;
      LivingEntity owner = getOwner(level, mob);
      if (owner instanceof net.minecraft.server.level.ServerPlayer player && isPaleRiderCardPlayer(player)) {
         if (player.getPersistentData().getInt("PaleRiderCardCommand") != 2) return null;
         CompoundTag data = mob.getPersistentData();
         LivingEntity target = data.hasUUID(TAG_CONTROLLED_COMMAND_TARGET)
            && level.getEntity(data.getUUID(TAG_CONTROLLED_COMMAND_TARGET)) instanceof LivingEntity living ? living : null;
         return isValidCommandTarget(mob, player, target) ? target : null;
      }
      if (!(owner instanceof Mob ownerMob)) return null;
      LivingEntity target = owner instanceof PaleRiderEntity rider ? rider.findPaleRiderEnemy(96.0) : ownerMob.getTarget();
      return isValidCommandTarget(mob, owner, target) ? target : null;
   }

   public static boolean isInfectedBy(LivingEntity target, LivingEntity owner) {
      CompoundTag data = target.getPersistentData();
      return isInfected(target) && data.hasUUID(TAG_OWNER) && owner.getUUID().equals(data.getUUID(TAG_OWNER));
   }

   public static boolean forceControl(Mob mob, LivingEntity owner) {
      return mob != null && owner != null && canControl(mob) && beginControl(mob, owner);
   }

   public static int controlledCount(LivingEntity owner) {
      if (owner == null || !(owner.level() instanceof ServerLevel level)) return 0;
      return PaleRiderEntityIndex.controlledCount(level, owner.getUUID(), entity ->
         isControlled(entity) || entity instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.RatSwarmEntity
            || entity instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.PaleRiderCrowEntity);
   }

   public static boolean hasControlCapacity(LivingEntity owner, int amount) {
      return owner != null && amount >= 0 && controlledCount(owner) + amount <= MAX_CONTROLLED;
   }

   public static boolean arePaleRiderAllies(Entity first, Entity second) {
      if (first == null || second == null) return false;
      LivingEntity firstOwner = getPaleRiderController(first);
      if (firstOwner != null && (second == firstOwner || firstOwner.isAlliedTo(second) || second.isAlliedTo(firstOwner))) {
         return true;
      }
      LivingEntity secondOwner = getPaleRiderController(second);
      return secondOwner != null && (first == secondOwner || secondOwner.isAlliedTo(first) || first.isAlliedTo(secondOwner));
   }

   private static boolean canControl(LivingEntity target) {
      return target instanceof Mob && !(target instanceof Player) && !(target instanceof ServantEntity) && !target.getType().is(Tags.EntityTypes.BOSSES);
   }

   private static double controlChance(LivingEntity target, int level) {
      net.minecraft.resources.ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
      boolean vanilla = typeId != null && "minecraft".equals(typeId.getNamespace());
      return InfectionRules.ordinaryControlChance(level, vanilla);
   }

   private static boolean beginControl(Mob mob, LivingEntity owner) {
      CompoundTag data = mob.getPersistentData();
      boolean alreadyControlledByOwner = data.getBoolean(TAG_CONTROLLED) && data.hasUUID(TAG_OWNER)
         && owner.getUUID().equals(data.getUUID(TAG_OWNER));
      boolean alreadyCountedOwnedUnit = mob instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.RatSwarmEntity swarm
         && owner.getUUID().equals(swarm.getPaleRiderOwnerUuid())
         || mob instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.PaleRiderCrowEntity crow
            && owner.getUUID().equals(crow.getPaleRiderOwnerUuid());
      if (!alreadyControlledByOwner && !alreadyCountedOwnedUnit && !hasControlCapacity(owner, 1)) return false;
      if (alreadyCountedOwnedUnit && controlledCount(owner) > MAX_CONTROLLED) return false;
      if (data.hasUUID(TAG_OWNER) && !owner.getUUID().equals(data.getUUID(TAG_OWNER))) {
         PaleRiderEntityIndex.unregisterControlled(data.getUUID(TAG_OWNER), mob);
      }
      if (!data.getBoolean(TAG_CONTROLLED)) {
         data.putBoolean(TAG_PREVIOUS_NO_AI, mob.isNoAi());
      }
      data.putBoolean(TAG_CONTROLLED, true);
      data.putInt(TAG_LEVEL, InfectionRules.MAX_LEVEL);
      data.putUUID(TAG_OWNER, owner.getUUID());
      data.putLong(TAG_UNTIL, mob.level().getGameTime() + InfectionRules.DURATION_TICKS);
      PaleRiderEntityIndex.registerControlled(owner.getUUID(), mob);
      mob.addEffect(new MobEffectInstance(ModMobEffects.PALE_RIDER_INFECTION, InfectionRules.DURATION_TICKS,
         InfectionRules.MAX_LEVEL - 1, false, true, true));
      mob.setNoAi(false);
      if (owner instanceof Mob ownerMob && ownerMob.getTarget() == mob) {
         ownerMob.setTarget(null);
      }
      mob.setTarget(owner instanceof Mob ownerMob ? ownerMob.getTarget() : null);
      return true;
   }

   private static void tickControlledMob(ServerLevel level, Mob mob, LivingEntity owner, long now) {
      if (InfectionRules.isScheduled(mob.getId(), now, InfectionRules.CONTROLLED_PARTICLE_INTERVAL_TICKS)) {
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
            mob.getX(), mob.getY() + mob.getBbHeight() * 0.55, mob.getZ(), 1, mob.getBbWidth() * 0.35, mob.getBbHeight() * 0.35, mob.getBbWidth() * 0.35, 0.005);
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.ASH,
            mob.getX(), mob.getY() + mob.getBbHeight() * 0.65, mob.getZ(), 1, mob.getBbWidth() * 0.4, mob.getBbHeight() * 0.4, mob.getBbWidth() * 0.4, 0.01);
      }

      if (isStationaryAnchor(mob)) {
         return;
      }

      if (owner instanceof net.minecraft.server.level.ServerPlayer cardOwner
         && isPaleRiderCardPlayer(cardOwner)
         && cardOwner.getPersistentData().getInt("PaleRiderCardCommand") == 4
         && !(mob instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.RatSwarmEntity)
         && !(mob instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.PaleRiderCrowEntity)) {
         long next = mob.getPersistentData().getLong(TAG_LETHAL_NEXT_TICK);
         if (now >= next) {
            mob.getPersistentData().putLong(TAG_LETHAL_NEXT_TICK, now + 10L);
            float damage = Math.max(2.0F, mob.getMaxHealth() * 0.08F);
            mob.invulnerableTime = 0;
            if (mob.hurt(cardOwner.damageSources().source(PaleRiderDamageTypes.INFECTION, cardOwner), damage)
               && mob.isDeadOrDying()) {
               var vars = cardOwner.getData(net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables.PLAYER_VARIABLES);
               vars.servant_card_mana = Math.min(vars.servant_card_max_mana, vars.servant_card_mana + 100.0);
               vars.syncMana(cardOwner);
            }
         }
         return;
      }

      if (mob.getPersistentData().getBoolean("PaleRiderPossessed")) return;

      if (owner instanceof net.minecraft.server.level.ServerPlayer cardOwner && isPaleRiderCardPlayer(cardOwner)) {
         int command = cardOwner.getPersistentData().getInt("PaleRiderCardCommand");
         if (command == 0) {
            mob.getPersistentData().remove(TAG_CONTROLLED_COMMAND_TARGET);
            clearTarget(mob);
            return;
         }
         if (command == 1) {
            mob.getPersistentData().remove(TAG_CONTROLLED_COMMAND_TARGET);
            clearTarget(mob);
            if (!mob.getNavigation().isDone()) mob.getNavigation().stop();
            return;
         }
         if (command == 3) {
            mob.getPersistentData().remove(TAG_CONTROLLED_COMMAND_TARGET);
            clearTarget(mob);
            if (mob.distanceToSqr(cardOwner) > 4.0) {
               if (canRefreshNavigation(mob, now, 20)) mob.getNavigation().moveTo(cardOwner, 1.05);
            } else if (!mob.getNavigation().isDone()) {
               mob.getNavigation().stop();
            }
            return;
         }
         LivingEntity nearbyTarget = isValidCommandTarget(mob, cardOwner, mob.getTarget()) ? mob.getTarget() : null;
         if (nearbyTarget == null && mob.getPersistentData().hasUUID(TAG_CONTROLLED_COMMAND_TARGET)
            && level.getEntity(mob.getPersistentData().getUUID(TAG_CONTROLLED_COMMAND_TARGET)) instanceof LivingEntity cached
            && isValidCommandTarget(mob, cardOwner, cached)) {
            nearbyTarget = cached;
         }
         if (nearbyTarget == null) {
            mob.getPersistentData().remove(TAG_CONTROLLED_COMMAND_TARGET);
            clearTarget(mob);
            long nextScan = mob.getPersistentData().getLong(TAG_NEXT_CONTROLLED_TARGET_SCAN);
            if (now < nextScan) {
               return;
            }
            mob.getPersistentData().putLong(TAG_NEXT_CONTROLLED_TARGET_SCAN, now + 40L + Math.floorMod(mob.getId(), 16));
            nearbyTarget = findNearestEnemy(level, mob, cardOwner, 24.0);
         }
         if (nearbyTarget != null) {
            cacheCommandTarget(mob, nearbyTarget);
            if (mob.getTarget() != nearbyTarget) mob.setTarget(nearbyTarget);
            if (!mob.isAggressive()) mob.setAggressive(true);
            if (canRefreshNavigation(mob, now, 12)) mob.getNavigation().moveTo(nearbyTarget, 1.0);
         } else {
            mob.getPersistentData().remove(TAG_CONTROLLED_COMMAND_TARGET);
         }
         return;
      }

      LivingEntity commandTarget = owner instanceof PaleRiderEntity rider ? rider.findPaleRiderEnemy(96.0)
         : owner instanceof Mob ownerMob ? ownerMob.getTarget() : null;
      if (!isValidCommandTarget(mob, owner, commandTarget)) {
         clearTarget(mob);
         Vec3 gatheringPosition = gatheringPosition(mob, owner);
         if (mob.distanceToSqr(gatheringPosition) > 2.25) {
            if (canRefreshNavigation(mob, now, 20)) {
               mob.getNavigation().moveTo(gatheringPosition.x, gatheringPosition.y, gatheringPosition.z, 1.05);
            }
         } else if (!mob.getNavigation().isDone()) {
            mob.getNavigation().stop();
         }
         return;
      }

      if (mob.getTarget() != commandTarget) mob.setTarget(commandTarget);
      if (!mob.isAggressive()) mob.setAggressive(true);
      mob.getLookControl().setLookAt(commandTarget, 30.0F, 30.0F);
      if (canRefreshNavigation(mob, now, 12)) mob.getNavigation().moveTo(commandTarget, 1.0);
      double reach = mob.getBbWidth() + commandTarget.getBbWidth() + 1.25;
      if (mob.distanceToSqr(commandTarget) <= reach * reach
         && mob.getSensing().hasLineOfSight(commandTarget)
         && now >= mob.getPersistentData().getLong(TAG_NEXT_CONTROLLED_ATTACK)) {
         mob.getPersistentData().putLong(TAG_NEXT_CONTROLLED_ATTACK, now + 20L);
         mob.swing(InteractionHand.MAIN_HAND, true);
         if (mob.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            mob.doHurtTarget(commandTarget);
         } else {
            commandTarget.hurt(mob.damageSources().mobAttack(mob), 2.0F);
         }
      }
   }

   private static Vec3 gatheringPosition(Mob mob, LivingEntity owner) {
      int hash = mob.getUUID().hashCode() & Integer.MAX_VALUE;
      double angle = Math.toRadians(hash % 360);
      double radius = 2.5 + (hash / 360 % 3) * 1.25;
      return owner.position().add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
   }

   private static LivingEntity findNearestEnemy(ServerLevel level, Mob mob, LivingEntity owner, double radius) {
      LivingEntity nearest = null;
      double nearestDistance = Double.MAX_VALUE;
      for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(radius),
         target -> isValidCommandTarget(mob, owner, target))) {
         double distance = candidate.distanceToSqr(mob);
         if (distance < nearestDistance) {
            nearest = candidate;
            nearestDistance = distance;
         }
      }
      return nearest;
   }

   private static boolean canRefreshNavigation(Mob mob, long now, int interval) {
      CompoundTag data = mob.getPersistentData();
      if (now < data.getLong(TAG_NEXT_CONTROLLED_NAVIGATION)) return false;
      data.putLong(TAG_NEXT_CONTROLLED_NAVIGATION, now + interval + Math.floorMod(mob.getId(), 5));
      return true;
   }

   private static void cacheCommandTarget(Mob mob, LivingEntity target) {
      CompoundTag data = mob.getPersistentData();
      if (!data.hasUUID(TAG_CONTROLLED_COMMAND_TARGET)
         || !target.getUUID().equals(data.getUUID(TAG_CONTROLLED_COMMAND_TARGET))) {
         data.putUUID(TAG_CONTROLLED_COMMAND_TARGET, target.getUUID());
      }
   }

   private static void clearTarget(Mob mob) {
      if (mob.getTarget() != null) mob.setTarget(null);
      if (mob.isAggressive()) mob.setAggressive(false);
   }

   private static boolean canReceiveInfection(LivingEntity target, LivingEntity source, LivingEntity owner) {
      if (isStoutArash(target)
         || target == source || target == owner || !target.isAlive() || isPaleRiderCardPlayer(target)
         || arePaleRiderAllies(owner, target)
         || net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils.isImmunePlayerTarget(target)) {
         return false;
      }
      CompoundTag data = target.getPersistentData();
      return getLevel(target) < InfectionRules.MAX_LEVEL || !data.hasUUID(TAG_OWNER)
         || !owner.getUUID().equals(data.getUUID(TAG_OWNER));
   }

   private static boolean isStoutArash(LivingEntity target) {
      return target instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArashEntity
         || target instanceof net.minecraft.server.level.ServerPlayer player
            && net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardArashSkills.isArash(player);
   }

   private static void refreshEffect(
      LivingEntity target,
      net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
      int amplifier,
      boolean force
   ) {
      MobEffectInstance current = target.getEffect(effect);
      if (force || current == null || current.getAmplifier() != amplifier
         || current.getDuration() <= InfectionRules.EFFECT_REFRESH_THRESHOLD_TICKS) {
         target.addEffect(new MobEffectInstance(effect, InfectionRules.DURATION_TICKS, amplifier, false, true, true));
      }
   }

   private static void retainInfectionEffectOnly(LivingEntity target) {
      MobEffectInstance infection = target.getEffect(ModMobEffects.PALE_RIDER_INFECTION);
      target.removeAllEffects();
      if (infection != null) {
         target.addEffect(infection);
      }
   }

   private static boolean isValidCommandTarget(Mob mob, LivingEntity owner, LivingEntity target) {
      return target != null && target != mob && target.isAlive() && !arePaleRiderAllies(owner, target)
         && !net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils.isImmunePlayerTarget(target);
   }

   private static void endControl(LivingEntity target) {
      CompoundTag data = target.getPersistentData();
      boolean wasControlled = data.getBoolean(TAG_CONTROLLED);
      boolean previousNoAi = data.getBoolean(TAG_PREVIOUS_NO_AI);
      UUID owner = data.hasUUID(TAG_OWNER) ? data.getUUID(TAG_OWNER) : null;
      data.remove(TAG_CONTROLLED);
      data.remove(TAG_PREVIOUS_NO_AI);
      data.remove(TAG_NEXT_CONTROLLED_ATTACK);
      data.remove(TAG_NEXT_CONTROLLED_TARGET_SCAN);
      data.remove(TAG_CONTROLLED_COMMAND_TARGET);
      data.remove(TAG_NEXT_CONTROLLED_NAVIGATION);
      data.remove(TAG_LETHAL_NEXT_TICK);
      PaleRiderEntityIndex.unregisterControlled(owner, target);
      if (wasControlled && target instanceof Mob mob) {
         mob.setNoAi(previousNoAi);
         mob.setTarget(null);
      }
   }

   public static LivingEntity getOwner(ServerLevel level, LivingEntity target) {
      CompoundTag data = target.getPersistentData();
      if (!data.hasUUID(TAG_OWNER)) {
         return null;
      }
      UUID id = data.getUUID(TAG_OWNER);
      return level.getEntity(id) instanceof LivingEntity living ? living : null;
   }

   private static boolean tickCalamityExit(LivingEntity target, PaleRiderEntity rider, int infectionLevel, long now) {
      CompoundTag data = target.getPersistentData();
      boolean inside = rider.isCalamityActive() && target.distanceToSqr(rider) <= PaleRiderCorruptionService.CALAMITY_RADIUS_SQR;
      if (inside) {
         data.putBoolean(TAG_WAS_IN_CALAMITY, true);
         data.remove(TAG_CALAMITY_EXIT_CLEANSE);
         return false;
      }
      if (!data.getBoolean(TAG_WAS_IN_CALAMITY)) return false;
      if (!data.contains(TAG_CALAMITY_EXIT_CLEANSE)) {
         data.putLong(TAG_CALAMITY_EXIT_CLEANSE, now + InfectionRules.calamityExitCleanseTicks(infectionLevel));
      }
      if (now < data.getLong(TAG_CALAMITY_EXIT_CLEANSE)) return false;
      cleanse(target, false);
      return true;
   }

   private static LivingEntity getPaleRiderController(Entity entity) {
      if (entity instanceof PaleRiderEntity rider) return rider;
      if (entity instanceof OwnedPaleRiderMob owned) return owned.getPaleRiderLivingOwner();
      if (entity instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.PaleRiderCrowEntity crow) return crow.getPaleRiderLivingOwner();
      if (isControlled(entity) && entity.level() instanceof ServerLevel level
         && level.getEntity(entity.getPersistentData().getUUID(TAG_OWNER)) instanceof LivingEntity owner) {
         return owner;
      }
      return null;
   }
}
