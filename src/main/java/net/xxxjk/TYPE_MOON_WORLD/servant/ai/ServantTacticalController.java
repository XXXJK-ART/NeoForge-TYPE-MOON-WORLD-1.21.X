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
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.ServantActionPlanner;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.ServantActionProfile;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.ServantActionRegistry;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.ServantPhaseService;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
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

   private ServantTacticalController() { }

   public static boolean tick(ServantEntity entity) {
      if (!(entity.level() instanceof ServerLevel level) || !entity.isAlive() || !AiMigrationPolicy.isArbitrated(entity)) return false;
      EvasionMovementService.tickAirState(entity);
      long now = level.getGameTime();
      AiBrain brain = AiBrain.begin(entity);
      ServantTargetingService.refreshOrRecover(entity, now);
      rememberTarget(entity, brain.blackboard(), now);
      ServantActionProfile actionProfile = ServantActionRegistry.get(entity.getServantId());
      ServantPhaseService.Update phase = ServantPhaseService.tick(entity, entity.getTarget(), actionProfile, now);
      if (phase.changed()) {
         brain.submit(AiIntent.of(PHASE_TRANSITION, AiIntent.PRIORITY_PHASE, phase.phase().ordinal(), 1, false,
            entity.getNavigation()::stop, AiControl.DEFEND, AiControl.LOOK));
      }
      var selected = ServantActionPlanner.select(entity, entity.getTarget(), phase.phase(), actionProfile, brain.blackboard());
      if (selected == null) entity.getPersistentData().remove("TypeMoonAiSelectedAction");
      else entity.getPersistentData().putString("TypeMoonAiSelectedAction", selected.id().toString());
      ServantActionPlanner.publishActiveThreat(entity, entity.getTarget(), selected, now);
      submitMasterCommand(entity, brain);
      submitDistantPursuit(entity, brain, now);

      if (entity.tickCount % 3 == Math.floorMod(entity.getId(), 3)) {
         ProjectileThreatSensor.IncomingProjectile projectile = ProjectileThreatSensor.nearest(entity, 12.0, 8.0);
         if (projectile != null) {
            double utility = 100.0 - projectile.impactTicks() * 8.0;
            brain.submit(AiIntent.of(EVADE_PROJECTILE, AiIntent.PRIORITY_LETHAL_DEFENSE, utility, 4, false,
               () -> EvasionMovementService.tryEvade(entity, projectile.projectile().position()),
               AiControl.DEFEND, AiControl.MOVE, AiControl.LOOK));
         }
      }

      for (CombatThreat threat : CombatThreatService.nearby(level, entity.position(), 64.0, now)) {
         if (!hostile(entity, level, threat.sourceUuid())) continue;
         boolean explicitlyTargeted = entity.getUUID().equals(threat.targetUuid());
         if (!explicitlyTargeted && !threat.threatens(entity.getEyePosition(), entity.getBbWidth() * 0.65)) continue;
         if (threat.ticksToImpact(now) > 20L || !threat.dodgeable()) continue;
         brain.blackboard().observe(threat.sourceUuid(), threat.actionId(),
            entity.position().distanceTo(threat.origin()), 0.0, true, now);
         double utility = threat.danger() * 20.0 + Math.max(0.0, 20.0 - threat.ticksToImpact(now));
         brain.submit(AiIntent.of(EVADE_ACTION, AiIntent.PRIORITY_LETHAL_DEFENSE, utility, 5, false,
            () -> EvasionMovementService.tryEvade(entity, threat.origin()),
            AiControl.DEFEND, AiControl.MOVE, AiControl.LOOK));
      }

      return brain.resolve().consumesLegacyControl();
   }

   private static void submitDistantPursuit(ServantEntity entity, AiBrain brain, long now) {
      LivingEntity target = entity.getTarget();
      if (!ServantPursuitService.shouldPursue(entity, target)) return;
      double distance = entity.distanceTo(target);
      brain.submit(AiIntent.of(DISTANT_PURSUIT, AiIntent.PRIORITY_POSITION, distance, 3, true,
         () -> ServantPursuitService.pursue(entity, target, now), AiControl.MOVE, AiControl.LOOK));
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
}
