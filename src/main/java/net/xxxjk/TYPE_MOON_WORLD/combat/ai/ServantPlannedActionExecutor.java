package net.xxxjk.TYPE_MOON_WORLD.combat.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantManeuverService;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantNavigationHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantCombatTempoService;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantTacticalProfileResolver;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantCombatActionContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatMotionService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.registry.ServantAddonRegistry;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactService;

/** Tick-driven execution for explicitly data-driven actions. */
public final class ServantPlannedActionExecutor {
   private static final String COOLDOWN_PREFIX = "TypeMoonPlannedActionCooldown_";
   private static final String ACTIVE_ACTION = "TypeMoonPlannedActionId";
   private static final int APPROACH_TIMEOUT = 30;
   private static final int MAX_STALLED_TICKS = 8;
   private static final String APPROACH_PATH_PREFIX = "ServantPlannedActionApproach";
   private static final Map<ServantEntity, ActionRuntime> ACTIVE = new WeakHashMap<>();

   private ServantPlannedActionExecutor() { }

   public static boolean canExecute(ServantEntity entity, LivingEntity target, AiActionDescriptor action, long now) {
      if (entity == null || target == null || action == null || !target.isAlive()
         || entity.level() != target.level() || entity.isPerformingAction() || isActive(entity)) return false;
      AiActionDescriptor.ManeuverSpec maneuver = action.maneuver();
      if (maneuver == null || maneuver.equals(AiActionDescriptor.ManeuverSpec.NONE)) return false;
      if (now < entity.getPersistentData().getLong(cooldownKey(action))
         || entity.getCurrentMp() + 1.0E-6 < action.manaCost()
         || ServantCombatSystem.currentStamina(entity) + 1.0E-6 < action.staminaCost()) return false;
      double distance = entity.distanceTo(target);
      if (distance > maneuver.effectiveApproachRange(action.maximumRange())
         || distance + 0.5 < action.minimumRange()) return false;
      var tactical = ServantTacticalProfileResolver.resolve(entity);
      double acceptableHeight = 3.0 + tactical.verticalMobility() * 9.0
         + (action.tags().contains(AiActionDescriptor.Tag.ANTI_AIR) ? 4.0 : 0.0);
      if (Math.abs(target.getY() - entity.getY()) > acceptableHeight) return false;
      if (action.tags().contains(AiActionDescriptor.Tag.ANTI_AIR)
         && !qualifiesAntiAirTarget(target.onGround(), ServantCombatMotionService.isLaunched(target),
            target.getY() - entity.getY(), target.getDeltaMovement().y, target.fallDistance)) return false;
      if (action.tags().contains(AiActionDescriptor.Tag.COUNTER)
         && !hasCounterWindow(entity, now)) return false;
      if (action.tags().contains(AiActionDescriptor.Tag.FINISHER)
         && !ServantCombatMotionService.isImpactStaggered(target)) return false;
      if (requiresProjectileSight(action) && !hasProjectileSight(entity, target)) return false;
      return EntityUtils.isValidCombatTarget(entity, target);
   }

   public static boolean execute(ServantEntity entity, LivingEntity target, AiActionDescriptor action, long now) {
      if (!canExecute(entity, target, action, now)) return false;
      String externalId = action.id().getPath();
      int separator = externalId.lastIndexOf('/');
      if (separator >= 0) externalId = externalId.substring(separator + 1);
      ServantAiContext context = ServantAiContext.forEntity(entity, target, now);
      var addonResult = ServantAddonRegistry.executeCombatAction(new ServantCombatActionContext(
         entity, target, context, entity.getDefinition(), externalId, entity.distanceTo(target),
         entity.getSensing().hasLineOfSight(target), now
      ));
      if (addonResult.handled()) {
         if (addonResult.success()) commit(entity, action, now);
         return addonResult.success();
      }

      ActionRuntime runtime = new ActionRuntime(action, target.getUUID(), entity.level().dimension().location(),
         now + APPROACH_TIMEOUT, entity.getHealth());
      ACTIVE.put(entity, runtime);
      entity.getPersistentData().putString(ACTIVE_ACTION, action.id().toString());
      if (inStrikeRange(entity, target, action)) return beginWindup(entity, target, runtime, now);
      runtime.stage = Stage.APPROACH;
      return true;
   }

   public static boolean tick(ServantEntity entity, long now) {
      ActionRuntime runtime = ACTIVE.get(entity);
      if (runtime == null) return false;
      if (!runtime.dimension.equals(entity.level().dimension().location())) {
         if (runtime.stage == Stage.APPROACH) clearApproachMovement(entity);
         clear(entity);
         return false;
      }
      LivingEntity target = resolveTarget(entity, runtime.targetUuid);
      if (!entity.isAlive() || target == null || !EntityUtils.isValidCombatTarget(entity, target)) {
         if (runtime.stage == Stage.APPROACH) clearApproachMovement(entity);
         clear(entity);
         return false;
      }
      if (runtime.stage != Stage.RECOVERY && runtime.stage != Stage.CANCELLED
         && ServantCombatSystem.cannotAct(entity)) {
         cancel(entity, runtime, now);
      }
      if (runtime.stage == Stage.WINDUP && entity.getHealth() < runtime.lastHealth) {
         double fraction = (runtime.lastHealth - entity.getHealth()) / Math.max(1.0F, entity.getMaxHealth());
         int power = Math.max(1, Math.min(5, 1 + (int)Math.floor(fraction / 0.05)));
         interrupt(entity, power, now);
      }
      runtime.lastHealth = entity.getHealth();

      return switch (runtime.stage) {
         case APPROACH -> tickApproach(entity, target, runtime, now);
         case WINDUP -> tickWindup(entity, target, runtime, now);
         case ACTIVE -> tickActive(entity, runtime, now);
         case RECOVERY, CANCELLED -> tickRecovery(entity, runtime, now);
      };
   }

   public static boolean isActive(ServantEntity entity) {
      return entity != null && ACTIVE.containsKey(entity);
   }

   public static Stage stage(ServantEntity entity) {
      ActionRuntime runtime = entity == null ? null : ACTIVE.get(entity);
      return runtime == null ? null : runtime.stage;
   }

   /**
    * Releases an action that is still trying to reach its target when the
    * combat tempo supervisor demands direct melee.  Once an action has paid
    * its resource cost and entered its telegraph, it owns the attack timeline
    * and must be allowed to finish.
    */
   public static boolean cancelForMeleeOverride(ServantEntity entity, long now) {
      ActionRuntime runtime = entity == null ? null : ACTIVE.get(entity);
      if (runtime == null || runtime.stage != Stage.APPROACH) return false;
      clearApproachMovement(entity);
      if (entity.level() instanceof ServerLevel level) {
         CombatThreatService.clearForSource(level, entity.getUUID());
      }
      clear(entity);
      entity.getPersistentData().remove("TypeMoonPlannedActionStalledTicks");
      entity.getPersistentData().remove("TypeMoonCombatThreat");
      return true;
   }

   public static boolean interrupt(ServantEntity entity, int power, long now) {
      ActionRuntime runtime = entity == null ? null : ACTIVE.get(entity);
      if (runtime == null || runtime.stage != Stage.APPROACH && runtime.stage != Stage.WINDUP
         || runtime.stage == Stage.WINDUP && (!runtime.action.threat().interruptible()
            || power < runtime.action.maneuver().interruptResistance())) return false;
      cancel(entity, runtime, now);
      return true;
   }

   public static boolean isCoolingDown(ServantEntity entity, AiActionDescriptor action, long now) {
      return entity != null && action != null && now < entity.getPersistentData().getLong(cooldownKey(action));
   }

   static int cooldownTicks(AiActionDescriptor action) {
      return Math.max(8, action.timing().windupTicks() + action.timing().activeTicks()
         + action.timing().recoveryTicks() + action.maneuver().comboCost() * 2);
   }

   static boolean qualifiesAntiAirTarget(boolean onGround, boolean launched, double heightDifference,
                                         double verticalSpeed, float fallDistance) {
      return launched || Math.abs(heightDifference) >= 1.5
         || !onGround && (Math.abs(verticalSpeed) >= 0.06 || fallDistance >= 0.5F);
   }

   private static boolean tickApproach(ServantEntity entity, LivingEntity target, ActionRuntime runtime, long now) {
      if (now > runtime.stageDeadline) {
         clearApproachMovement(entity);
         clear(entity);
         return false;
      }
      if (inStrikeRange(entity, target, runtime.action)) return beginWindup(entity, target, runtime, now);
      boolean moved = ServantManeuverService.approachForAction(entity, target, runtime.action, now,
         ServantTacticalProfileResolver.resolve(entity));
      runtime.stalledTicks = moved ? 0 : runtime.stalledTicks + 1;
      if (runtime.stalledTicks >= MAX_STALLED_TICKS) {
         clearApproachMovement(entity);
         clear(entity);
         return false;
      }
      return true;
   }

   private static boolean beginWindup(ServantEntity entity, LivingEntity target, ActionRuntime runtime, long now) {
      AiActionDescriptor action = runtime.action;
      if (entity.getCurrentMp() + 1.0E-6 < action.manaCost()
         || ServantCombatSystem.currentStamina(entity) + 1.0E-6 < action.staminaCost()
         || !ServantCombatSystem.tryConsumeStamina(entity, action.staminaCost())) {
         clear(entity);
         return false;
      }
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - action.manaCost()));
      clearApproachMovement(entity);
      runtime.stage = Stage.WINDUP;
      runtime.snapshotDirection = target.getEyePosition().subtract(entity.getEyePosition());
      if (runtime.snapshotDirection.lengthSqr() < 1.0E-4) runtime.snapshotDirection = entity.getLookAngle();
      runtime.snapshotDirection = runtime.snapshotDirection.normalize();
      runtime.stageDeadline = now + action.timing().windupTicks();
      commit(entity, action, now);
      triggerAnimation(entity, action.maneuver().control(), action.maneuver().movement());
      publishThreat(entity, target, runtime, now);
      CombatKnowledgeService.observeWindup(target, entity, action, now);
      return true;
   }

   private static boolean tickWindup(ServantEntity entity, LivingEntity target, ActionRuntime runtime, long now) {
      entity.getNavigation().stop();
      entity.faceToward(entity.getEyePosition().add(runtime.snapshotDirection));
      if (requiresProjectileSight(runtime.action) && !hasProjectileSight(entity, target)) {
         cancel(entity, runtime, now);
         return true;
      }
      if (now < runtime.stageDeadline) return true;
      applyHit(entity, target, runtime, now);
      runtime.stage = Stage.ACTIVE;
      runtime.stageDeadline = now + runtime.action.timing().activeTicks();
      return true;
   }

   private static boolean tickActive(ServantEntity entity, ActionRuntime runtime, long now) {
      if (now < runtime.stageDeadline) return true;
      runtime.stage = Stage.RECOVERY;
      runtime.stageDeadline = now + runtime.action.timing().recoveryTicks();
      return true;
   }

   private static boolean tickRecovery(ServantEntity entity, ActionRuntime runtime, long now) {
      if (now < runtime.stageDeadline) return true;
      clear(entity);
      return false;
   }

   private static void applyHit(ServantEntity entity, LivingEntity intended, ActionRuntime runtime, long now) {
      AiActionDescriptor action = runtime.action;
      CombatThreat threat = runtime.threat;
      if (threat == null) return;
      List<LivingEntity> victims = new ArrayList<>();
      if (action.tags().contains(AiActionDescriptor.Tag.AREA)) {
         double reach = Math.max(action.maximumRange(), Math.max(action.threat().radius(), action.threat().length())) + 2.0;
         AABB bounds = new AABB(threat.origin(), threat.origin()).inflate(reach);
         for (LivingEntity candidate : entity.level().getEntitiesOfClass(LivingEntity.class, bounds,
            candidate -> candidate != entity && candidate.isAlive() && !entity.isAlliedTo(candidate)
               && EntityUtils.isValidCombatTarget(entity, candidate))) {
            if (threatens(threat, candidate, action)) victims.add(candidate);
         }
      } else if (threatens(threat, intended, action)) {
         victims.add(intended);
      }

      boolean terrainTriggered = false;
      for (LivingEntity victim : victims) {
         if (requiresLineOfSight(action.threat().shape()) && !entity.getSensing().hasLineOfSight(victim)) continue;
         float damage = (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE)
            * action.maneuver().effectiveDamageScale(action.tags().contains(AiActionDescriptor.Tag.FINISHER)));
         boolean damaged = victim.hurt(entity.damageSources().mobAttack(entity), damage);
         ServantCombatTempoService.recordContact(entity, victim,
            damaged ? ServantCombatTempoService.ContactType.DAMAGE
               : ServantCombatTempoService.ContactType.BLOCKED, now);
         if (!damaged) continue;
         if (action.tags().contains(AiActionDescriptor.Tag.INTERRUPT) && victim instanceof ServantEntity servant) {
            interrupt(servant, action.maneuver().interruptLevel(), now);
         }
         boolean protectedFinisher = action.tags().contains(AiActionDescriptor.Tag.FINISHER)
            && ServantCombatMotionService.isImpactStaggered(victim);
         if (!protectedFinisher && (action.maneuver().horizontalForce() > 0.0 || action.maneuver().verticalForce() > 0.0)) {
            Vec3 direction = victim.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
            ServantCombatMotionService.launch(entity, victim, direction, action.maneuver().horizontalForce(),
               action.maneuver().verticalForce(), action.terrainTier(), action.maneuver().pursuitWindowTicks());
         }
         if (!terrainTriggered && triggersTerrainAtHit(action)
            && entity.level() instanceof ServerLevel level) {
            TerrainImpactService.impact(level, entity, victim.position().add(0.0, 0.2, 0.0),
               net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile.of(action.terrainTier()),
               TerrainImpactService.Shape.GROUND_LOWER_HEMISPHERE);
            terrainTriggered = true;
         }
      }
   }

   static boolean triggersTerrainAtHit(AiActionDescriptor action) {
      if (action == null || !action.tags().contains(AiActionDescriptor.Tag.TERRAIN_BREAK)) return false;
      String control = action.maneuver().control().toLowerCase(java.util.Locale.ROOT);
      return action.threat().shape() == CombatThreat.Shape.HEMISPHERE
         || control.contains("slam") || control.contains("stomp");
   }

   private static boolean threatens(CombatThreat threat, LivingEntity target, AiActionDescriptor action) {
      if (target == null || !target.isAlive()) return false;
      if (action.threat().shape() == CombatThreat.Shape.POINT) {
         return target.distanceToSqr(threat.origin()) <= (action.maximumRange() + 0.75) * (action.maximumRange() + 0.75);
      }
      return threat.threatens(target.getEyePosition(), target.getBbWidth() * 0.5);
   }

   private static void publishThreat(ServantEntity entity, LivingEntity target, ActionRuntime runtime, long now) {
      if (!(entity.level() instanceof ServerLevel level)) return;
      AiActionDescriptor action = runtime.action;
      Vec3 origin = action.threat().shape() == CombatThreat.Shape.SPHERE
         || action.threat().shape() == CombatThreat.Shape.HEMISPHERE
         ? entity.position().add(0.0, 0.2, 0.0) : entity.getEyePosition();
      Vec3 direction = action.threat().shape() == CombatThreat.Shape.HEMISPHERE
         ? new Vec3(0.0, 1.0, 0.0) : runtime.snapshotDirection;
      runtime.threat = new CombatThreat(action.id(), entity.getUUID(), target.getUUID(), origin, direction,
         action.threat().shape(), action.threat().radius(), Math.max(action.threat().length(), action.maximumRange()),
         action.threat().danger(), now, now + action.timing().windupTicks(),
         now + action.timing().windupTicks() + action.timing().activeTicks(), action.threat().blockable(),
         action.threat().dodgeable(), action.threat().interruptible());
      CombatThreatService.publish(level, runtime.threat);
   }

   private static boolean hasCounterWindow(ServantEntity entity, long now) {
      if (!(entity.level() instanceof ServerLevel level)) return false;
      return CombatThreatService.incoming(level, entity, now, 10L, threat -> {
         Entity source = level.getEntity(threat.sourceUuid());
         return source instanceof LivingEntity living && !entity.isAlliedTo(living) && threat.interruptible();
      }) != null;
   }

   private static boolean inStrikeRange(ServantEntity entity, LivingEntity target, AiActionDescriptor action) {
      double distance = entity.distanceTo(target);
      return distance + 0.5 >= action.minimumRange() && distance <= action.maximumRange() + 0.5;
   }

   private static boolean requiresLineOfSight(CombatThreat.Shape shape) {
      return shape == CombatThreat.Shape.POINT || shape == CombatThreat.Shape.LINE || shape == CombatThreat.Shape.CONE;
   }

   private static boolean requiresProjectileSight(AiActionDescriptor action) {
      return action.tags().contains(AiActionDescriptor.Tag.PROJECTILE)
         || action.tags().contains(AiActionDescriptor.Tag.NOBLE_PHANTASM);
   }

   private static boolean hasProjectileSight(ServantEntity entity, LivingEntity target) {
      Vec3 start = entity.getEyePosition();
      Vec3 end = target.getEyePosition();
      HitResult hit = entity.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
         ClipContext.Fluid.NONE, entity));
      return hit.getType() == HitResult.Type.MISS
         || hit.getLocation().distanceToSqr(end) < 0.36;
   }

   private static LivingEntity resolveTarget(ServantEntity entity, UUID targetUuid) {
      if (!(entity.level() instanceof ServerLevel level)) return null;
      Entity target = level.getEntity(targetUuid);
      return target instanceof LivingEntity living && living.level() == entity.level() ? living : null;
   }

   private static void cancel(ServantEntity entity, ActionRuntime runtime, long now) {
      boolean approachedOnly = runtime.stage == Stage.APPROACH;
      if (approachedOnly) clearApproachMovement(entity);
      else entity.getNavigation().stop();
      runtime.stage = Stage.CANCELLED;
      runtime.stageDeadline = approachedOnly
         ? now : now + Math.min(6, runtime.action.timing().recoveryTicks());
   }

   private static void clear(ServantEntity entity) {
      ACTIVE.remove(entity);
      entity.getPersistentData().remove(ACTIVE_ACTION);
   }

   private static void clearApproachMovement(ServantEntity entity) {
      ServantNavigationHelper.clearMovementState(entity, APPROACH_PATH_PREFIX);
   }

   private static void commit(ServantEntity entity, AiActionDescriptor action, long now) {
      entity.getPersistentData().putLong(cooldownKey(action), now + cooldownTicks(action));
   }

   private static String cooldownKey(AiActionDescriptor action) {
      return COOLDOWN_PREFIX + Integer.toUnsignedString(action.id().toString().hashCode());
   }

   private static boolean isMovement(String movement) {
      return "pursuit".equals(movement) || "intercept".equals(movement) || "gap_closer".equals(movement);
   }

   private static void triggerAnimation(ServantEntity entity, String control, String movement) {
      String requested = !"none".equals(control) ? control : movement;
      if (requested != null && !"none".equals(requested) && entity.hasActionAnimation(requested)) {
         entity.triggerNamedActionAnimation(requested);
      } else if ("launcher".equals(control) && entity.hasActionAnimation("uppercut")) {
         entity.triggerUppercutAnimation();
      } else if (isMovement(movement) && entity.hasActionAnimation("charge")) {
         entity.triggerChargeAnimation();
      } else {
         entity.triggerAttackSwing();
      }
   }

   public enum Stage { APPROACH, WINDUP, ACTIVE, RECOVERY, CANCELLED }

   private static final class ActionRuntime {
      final AiActionDescriptor action;
      final UUID targetUuid;
      final net.minecraft.resources.ResourceLocation dimension;
      Stage stage = Stage.APPROACH;
      long stageDeadline;
      int stalledTicks;
      float lastHealth;
      Vec3 snapshotDirection = Vec3.ZERO;
      CombatThreat threat;

      ActionRuntime(AiActionDescriptor action, UUID targetUuid, net.minecraft.resources.ResourceLocation dimension,
                    long approachDeadline, float health) {
         this.action = action;
         this.targetUuid = targetUuid;
         this.dimension = dimension;
         this.stageDeadline = approachDeadline;
         this.lastHealth = health;
      }
   }
}
