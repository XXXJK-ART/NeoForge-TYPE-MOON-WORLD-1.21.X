package net.xxxjk.TYPE_MOON_WORLD.servant.ai;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.AiBlackboard;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.AiBrain;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.AiControl;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.AiIntent;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.AiMigrationPolicy;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.CombatThreat;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.CombatThreatService;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.EvasionMovementService;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.ProjectileThreatSensor;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.CombatMatchupEvaluator;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantSkillDefinition.FactType;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.ServantActionPlanner;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.ServantPlannedActionExecutor;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.ServantActionProfile;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.ServantActionRegistry;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.ServantPhaseService;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GawainEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantCommandMode;

/** Common pre-emptive defense layer. Character combat remains the fallback until migrated. */
public final class ServantTacticalController {
   private static final ResourceLocation EVADE_PROJECTILE = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "ai/evade_projectile");
   private static final ResourceLocation EVADE_ACTION = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "ai/evade_action");
   private static final ResourceLocation MASTER_FOLLOW = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "ai/master_follow");
   private static final ResourceLocation MASTER_GUARD = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "ai/master_guard");
   private static final ResourceLocation MASTER_STAY = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "ai/master_stay");
   private static final ResourceLocation PHASE_TRANSITION = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "ai/phase_transition");
   private static final ResourceLocation DISTANT_PURSUIT = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "ai/distant_pursuit");
   private static final ResourceLocation COMBAT_MANEUVER = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "ai/combat_maneuver");
   private static final ResourceLocation TACTICAL_REPOSITION = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "ai/tactical_reposition");

   private ServantTacticalController() { }

   public static boolean tick(ServantEntity entity) {
      if (!(entity.level() instanceof ServerLevel level) || !entity.isAlive() || !AiMigrationPolicy.isArbitrated(entity)) return false;
      EvasionMovementService.tickAirState(entity);
      long now = level.getGameTime();
      AiBrain brain = AiBrain.begin(entity);
      ServantTargetingService.refreshOrRecover(entity, now);
      rememberTarget(entity, brain.blackboard(), now);
      ServantCombatTempoService.TempoState tempo = ServantCombatTempoService.tick(entity, entity.getTarget(), now);
      ServantActionProfile actionProfile = ServantActionRegistry.get(entity.getServantId());
      ServantPhaseService.Update phase = ServantPhaseService.tick(entity, entity.getTarget(), actionProfile, now);
      if (phase.changed()) {
         brain.submit(AiIntent.of(PHASE_TRANSITION, AiIntent.PRIORITY_PHASE, phase.phase().ordinal(), 1, false,
            entity.getNavigation()::stop, AiControl.DEFEND, AiControl.LOOK));
      }
      boolean meleeOverride = tempo.meleeOverride();
      boolean orbiting = tempo.orbiting();
      boolean plannedActive = ServantPlannedActionExecutor.isActive(entity);
      if (meleeOverride && plannedActive
         && ServantPlannedActionExecutor.stage(entity) == ServantPlannedActionExecutor.Stage.APPROACH) {
         ServantPlannedActionExecutor.cancelForMeleeOverride(entity, now);
         plannedActive = false;
      }
      // The action timeline owns movement once a telegraph has started.  A
      // melee override only releases an approach path; windup/active/recovery
      // must remain uninterrupted.
      boolean keepActionTimeline = plannedActive
         && ServantPlannedActionExecutor.stage(entity) != ServantPlannedActionExecutor.Stage.APPROACH;
      java.util.List<net.xxxjk.TYPE_MOON_WORLD.combat.ai.AiActionDescriptor> candidates = plannedActive
         ? java.util.List.of()
          : ServantActionPlanner.candidates(entity, entity.getTarget(), phase.phase(), actionProfile, brain.blackboard());
      if (tempo.meleePressure() && entity.getTarget() != null && entity.distanceTo(entity.getTarget()) <= 6.0) {
         candidates = candidates.stream().filter(ServantTacticalController::allowedDuringMeleePressure).toList();
      }
      var selected = candidates.isEmpty() ? null : candidates.getFirst();
      if (selected == null) entity.getPersistentData().remove("TypeMoonAiSelectedAction");
      else entity.getPersistentData().putString("TypeMoonAiSelectedAction", selected.id().toString());
      submitMasterCommand(entity, brain);
      if (keepActionTimeline || (plannedActive && !meleeOverride)) submitActivePlannedAction(entity, brain, now);
      else if (!meleeOverride) {
         for (var candidate : candidates) submitPlannedAction(entity, candidate, brain, now);
         if (!orbiting) {
            submitCombatManeuver(entity, brain, now);
            submitTacticalReposition(entity, brain, now);
         }
         if (!orbiting) submitDistantPursuit(entity, brain, now);
      } else {
         // Legacy CombatModule is the final authority for forced contact. Any
         // stale route/side-step must be removed before it runs.
         LivingEntity forcedTarget = entity.getTarget();
         if (forcedTarget != null && forcedTarget.isAlive()) {
            entity.getLookControl().setLookAt(forcedTarget, 75.0F, 75.0F);
            entity.faceToward(forcedTarget.position());
         }
         ServantNavigationHelper.clearMovementState(entity);
      }
      if (!meleeOverride && !keepActionTimeline) {
         ServantCombatTempoService.submitFallback(entity, entity.getTarget(), brain, now, tempo);
      }

      if (entity.tickCount % 3 == Math.floorMod(entity.getId(), 3)) {
         ProjectileThreatSensor.IncomingProjectile projectile = ProjectileThreatSensor.nearest(entity, 12.0, 8.0);
         ProjectileThreatSensor.IncomingProjectileLike gateProjectile =
            ProjectileThreatSensor.nearestGateWeapon(entity, 12.0, 8.0);
         if (gateProjectile != null && (projectile == null
            || gateProjectile.impactTicks() < projectile.impactTicks())) {
            Entity owner = gateProjectile.projectile().getOwnerEntity();
            if (owner instanceof LivingEntity shooter && !entity.isAlliedTo(shooter)) {
               brain.blackboard().revealFact(shooter.getUUID(), FactType.PROJECTILE_PRESSURE, 0.8, now);
            }
            if (!ServantCombatDisposition.isRelentlessAdvance(entity)
               && (!(entity instanceof HeraclesEntity) && !(entity instanceof GawainEntity)
                  || entity.getRandom().nextFloat() < 0.04F)
               && !CombatMatchupEvaluator.canIgnoreProjectile(entity, gateProjectile)) {
               double utility = 100.0 - gateProjectile.impactTicks() * 8.0;
               brain.submit(AiIntent.of(EVADE_PROJECTILE, AiIntent.PRIORITY_LETHAL_DEFENSE, utility, 4, false,
                  () -> EvasionMovementService.tryAdvanceEvade(entity, gateProjectile.projectile().position(),
                     owner instanceof LivingEntity shooter ? shooter : null),
                  AiControl.DEFEND, AiControl.MOVE, AiControl.LOOK));
            }
         } else if (projectile != null) {
            Entity owner = projectile.projectile().getOwner();
            if (owner instanceof LivingEntity shooter && !entity.isAlliedTo(shooter)) {
               brain.blackboard().revealFact(shooter.getUUID(), FactType.PROJECTILE_PRESSURE, 0.8, now);
            }
            if (!ServantCombatDisposition.isRelentlessAdvance(entity)
               && (!(entity instanceof HeraclesEntity) && !(entity instanceof GawainEntity)
                  || entity.getRandom().nextFloat() < 0.04F)
               && !CombatMatchupEvaluator.canIgnoreProjectile(entity, projectile)) {
               double utility = 100.0 - projectile.impactTicks() * 8.0;
               brain.submit(AiIntent.of(EVADE_PROJECTILE, AiIntent.PRIORITY_LETHAL_DEFENSE, utility, 4, false,
                  () -> EvasionMovementService.tryAdvanceEvade(entity, projectile.projectile().position(),
                     owner instanceof LivingEntity shooter ? shooter : null),
                  AiControl.DEFEND, AiControl.MOVE, AiControl.LOOK));
            }
         }
      }

      for (CombatThreat threat : CombatThreatService.nearby(level, entity.position(), 64.0, now)) {
         if (!hostile(entity, level, threat.sourceUuid())) continue;
         boolean explicitlyTargeted = entity.getUUID().equals(threat.targetUuid());
         if (!explicitlyTargeted && !threat.threatens(entity.getEyePosition(), entity.getBbWidth() * 0.65)) continue;
         if (threat.ticksToImpact(now) > 20L || !threat.dodgeable()) continue;
         if (ServantCombatDisposition.isRelentlessAdvance(entity)) continue;
         brain.blackboard().observe(threat.sourceUuid(), threat.actionId(),
            entity.position().distanceTo(threat.origin()), 0.0, true, now);
         double utility = threat.danger() * 20.0 + Math.max(0.0, 20.0 - threat.ticksToImpact(now));
         Entity source = level.getEntity(threat.sourceUuid());
         boolean rangedPressure = threat.shape() == CombatThreat.Shape.LINE && threat.length() > 6.0
            || threat.shape() == CombatThreat.Shape.CONE && threat.length() > 6.0;
         brain.submit(AiIntent.of(EVADE_ACTION, AiIntent.PRIORITY_LETHAL_DEFENSE, utility, 5, false,
            () -> {
               if (rangedPressure) {
                  EvasionMovementService.tryAdvanceEvade(entity, threat.origin(),
                     source instanceof LivingEntity shooter ? shooter : null);
               } else {
                  EvasionMovementService.tryEvade(entity, threat.origin());
               }
            },
            AiControl.DEFEND, AiControl.MOVE, AiControl.LOOK));
      }

      AiBrain.Resolution resolution = brain.resolve();
      if (keepActionTimeline && resolution.intent() != null
         && resolution.intent().priority() > AiIntent.PRIORITY_ATTACK) {
         ServantPlannedActionExecutor.interrupt(entity, 5, now);
      }
      if (!resolution.executed()) {
         LivingEntity facingTarget = entity.getTarget();
         if (facingTarget != null && facingTarget.isAlive()) {
            // Do not let an old side-step path continue when no movement intent won.
            ServantNavigationHelper.stopIfMoving(entity);
            entity.getLookControl().setLookAt(facingTarget, 45.0F, 45.0F);
         }
      }
      return resolution.consumesLegacyControl();
   }

   static boolean allowedDuringMeleePressure(net.xxxjk.TYPE_MOON_WORLD.combat.ai.AiActionDescriptor action) {
      if (action == null) return false;
      return action.tags().contains(net.xxxjk.TYPE_MOON_WORLD.combat.ai.AiActionDescriptor.Tag.MELEE)
         || action.tags().contains(net.xxxjk.TYPE_MOON_WORLD.combat.ai.AiActionDescriptor.Tag.GUARD)
         || action.tags().contains(net.xxxjk.TYPE_MOON_WORLD.combat.ai.AiActionDescriptor.Tag.HEAL);
   }

   private static void submitActivePlannedAction(ServantEntity entity, AiBrain brain, long now) {
      ResourceLocation id = ResourceLocation.tryParse(entity.getPersistentData().getString("TypeMoonPlannedActionId"));
      if (id == null) id = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "ai/planned_action");
      brain.submit(AiIntent.attempt(id, AiIntent.PRIORITY_ATTACK, 100.0, 1, true,
         () -> ServantPlannedActionExecutor.tick(entity, now), AiControl.ATTACK, AiControl.MOVE, AiControl.LOOK));
   }

   private static void submitDistantPursuit(ServantEntity entity, AiBrain brain, long now) {
      LivingEntity target = entity.getTarget();
      if (!ServantPursuitService.shouldPursue(entity, target)) return;
      double distance = entity.distanceTo(target);
      brain.submit(AiIntent.of(DISTANT_PURSUIT, AiIntent.PRIORITY_POSITION, distance, 3, true,
         () -> ServantPursuitService.pursue(entity, target, now), AiControl.MOVE, AiControl.LOOK));
   }

   private static void submitPlannedAction(ServantEntity entity, net.xxxjk.TYPE_MOON_WORLD.combat.ai.AiActionDescriptor action,
                                           AiBrain brain, long now) {
      LivingEntity target = entity.getTarget();
      if (!ServantPlannedActionExecutor.canExecute(entity, target, action, now)) return;
      boolean cast = action.tags().contains(net.xxxjk.TYPE_MOON_WORLD.combat.ai.AiActionDescriptor.Tag.PROJECTILE)
         || action.tags().contains(net.xxxjk.TYPE_MOON_WORLD.combat.ai.AiActionDescriptor.Tag.NOBLE_PHANTASM);
      brain.submit(AiIntent.attempt(action.id(), AiIntent.PRIORITY_ATTACK,
         action.threat().danger() * 20.0 + action.maneuver().interruptLevel() * 6.0 + 10.0,
         Math.max(1, action.timing().windupTicks()), action.threat().interruptible(),
         () -> ServantPlannedActionExecutor.execute(entity, target, action, now),
         cast ? AiControl.CAST : AiControl.ATTACK, AiControl.LOOK));
   }

   private static void submitCombatManeuver(ServantEntity entity, AiBrain brain, long now) {
      LivingEntity target = entity.getTarget();
      if (isGawainHeraclesMatchup(entity, target)) return;
      if (ServantEngagementService.isMeleeDuel(entity, target)
         && target != null && entity.distanceTo(target) <= 16.0) return;
      if (!ServantManeuverService.shouldManeuver(entity, target)) return;
      if ((entity instanceof HeraclesEntity || entity instanceof GawainEntity)
         && target != null && entity.distanceTo(target) > 3.0) return;
      ServantAiDefinition.Tactical tactical = ServantTacticalProfileResolver.resolve(entity);
      boolean rangedPressure = ServantManeuverService.hasRangedPressure(entity, target);
      int priority = rangedPressure ? AiIntent.PRIORITY_ATTACK : AiIntent.PRIORITY_POSITION;
      double utility = entity.distanceTo(target) + tactical.pursuitAggression() * 20.0
         + (rangedPressure ? 120.0 : 0.0);
      brain.submit(AiIntent.attempt(COMBAT_MANEUVER, priority, utility, 3, true,
         () -> {
            if (!ServantManeuverService.trySideForwardReengage(entity, target, tactical, brain.blackboard(), now)) {
               return ServantManeuverService.maneuver(entity, target, now, tactical.interceptBias(), tactical.pursuitAggression());
            }
            return true;
         },
         AiControl.MOVE, AiControl.LOOK));
   }

   private static void submitTacticalReposition(ServantEntity entity, AiBrain brain, long now) {
      LivingEntity target = entity.getTarget();
      if (isGawainHeraclesMatchup(entity, target)) return;
      if (ServantEngagementService.isMeleeDuel(entity, target)
         && target != null && entity.distanceTo(target) <= 16.0) return;
      if (target == null || entity.getDefinition() == null) return;
      if (ServantCombatTempoService.inMeleePressure(entity, now)) return;
      if ((entity instanceof HeraclesEntity || entity instanceof GawainEntity)
         && entity.distanceTo(target) > 3.0) return;
      ServantAiDefinition.Tactical tactical = ServantTacticalProfileResolver.resolve(entity);
      if (!ServantManeuverService.shouldReposition(entity, target, tactical, now)) return;
      brain.submit(AiIntent.attempt(TACTICAL_REPOSITION, AiIntent.PRIORITY_POSITION,
         tactical.repositionDistance() + tactical.pursuitAggression() * 10.0, 4, true,
         () -> ServantManeuverService.reposition(entity, target, tactical, now), AiControl.MOVE, AiControl.LOOK));
   }

   private static void submitMasterCommand(ServantEntity entity, AiBrain brain) {
      ServerPlayer master = entity.getEntityMaster();
      if (master == null || master.level() != entity.level() || !master.isAlive()) return;
      LivingEntity attacker = master.getLastHurtByMob();
      if (attacker != null && attacker.isAlive() && !entity.isAlliedTo(attacker)
         && master.tickCount - master.getLastHurtByMobTimestamp() <= 100 && entity.getTarget() != attacker) {
         brain.submit(AiIntent.of(MASTER_GUARD, AiIntent.PRIORITY_COMMAND, 100.0, 1, true,
            () -> entity.setTarget(attacker), AiControl.ATTACK, AiControl.LOOK));
         return;
      }

      ServantCommandMode mode = entity.getCommandMode();
      if (mode == ServantCommandMode.STAY) {
         Vec3 anchor = Vec3.atCenterOf(entity.getStayAnchor());
         LivingEntity target = entity.getTarget();
         boolean targetOutside = target != null && target.distanceToSqr(anchor) > 12.0 * 12.0;
         if (entity.distanceToSqr(anchor) > 12.0 * 12.0 || targetOutside) {
            brain.submit(AiIntent.of(MASTER_STAY, AiIntent.PRIORITY_COMMAND, 90.0, 2, true, () -> {
               if (targetOutside) entity.setTarget(null);
               entity.getNavigation().moveTo(anchor.x, anchor.y, anchor.z, 1.25);
            }, AiControl.MOVE, AiControl.LOOK));
         }
         return;
      }

      double maxDistance = mode == ServantCommandMode.GUARD ? 10.0 : 8.0;
      if (entity.distanceToSqr(master) > maxDistance * maxDistance) {
         ResourceLocation id = mode == ServantCommandMode.GUARD ? MASTER_GUARD : MASTER_FOLLOW;
         brain.submit(AiIntent.of(id, AiIntent.PRIORITY_COMMAND, 80.0, 2, true,
            () -> entity.getNavigation().moveTo(master, 1.25), AiControl.MOVE, AiControl.LOOK));
      }
   }

   private static void rememberTarget(ServantEntity entity, AiBlackboard blackboard, long now) {
      LivingEntity target = entity.getTarget();
      if (target == null || !target.isAlive()) return;
      ServantTargetingService.remember(entity, target, now);
      blackboard.observe(target.getUUID(), null, entity.distanceTo(target), 0.0, false, now);
   }

   private static boolean hostile(ServantEntity observer, ServerLevel level, UUID sourceUuid) {
      Entity source = level.getEntity(sourceUuid);
      return source instanceof LivingEntity living && living != observer && !observer.isAlliedTo(living);
   }

   public static boolean isGawainHeraclesMatchup(LivingEntity actor, LivingEntity target) {
      return (actor instanceof GawainEntity && target instanceof HeraclesEntity)
         || (actor instanceof HeraclesEntity && target instanceof GawainEntity);
   }
}
