package net.xxxjk.TYPE_MOON_WORLD.servant.ai;

import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.ServantPlannedActionExecutor;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatMotionService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CasterGilgameshCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CasterGilgameshEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EnkiduCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EnkiduEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GilgameshEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MedeaEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

/** Final per-tick safety pass for servant flight, independent of tactical arbitration. */
public final class ServantFlightCombatService {
   public static final int DESCENT_TICKS = 40;
   public static final int FORCE_CONTACT_TICKS = 100;
   private static final String LAST_CONTROL_TICK = "TypeMoonFlightControlTick";

   private ServantFlightCombatService() { }

   public static void markControlled(ServantEntity entity, long now) {
      if (entity != null) entity.getPersistentData().putLong(LAST_CONTROL_TICK, now);
   }

   public static void tickIndependentController(ServantEntity entity) {
      if (entity instanceof CasterGilgameshEntity casterGilgamesh
         && casterGilgamesh.isAlive() && !casterGilgamesh.isSpiritualDissolving()) {
         boolean meleeHandled = CasterGilgameshCombatHelper.tickIndependentMelee(casterGilgamesh);
         if (!meleeHandled && !casterGilgamesh.wasTacticalAiHandledThisTick()) {
            CasterGilgameshCombatHelper.tick(casterGilgamesh);
         }
      }
   }

   public static void tick(ServantEntity entity) {
      if (entity == null || entity.level().isClientSide()) return;
      long now = entity.level().getGameTime();
      if (!isManagedFlight(entity, now)) {
         if (shouldForceGroundedGravity(entity, now)) {
            entity.setNoGravity(false);
         }
         ServantFlightHelper.clearAllAnchors(entity);
         entity.getPersistentData().remove(LAST_CONTROL_TICK);
         return;
      }

      entity.setNoGravity(true);
      entity.fallDistance = 0.0F;
      LivingEntity target = validTarget(entity);
      if (target == null) {
         stabilizeWithoutTarget(entity);
         return;
      }

      long disconnected = ServantCombatTempoService.disconnectedTicks(entity, now);
      boolean recentlyControlled = now - entity.getPersistentData().getLong(LAST_CONTROL_TICK) <= 1L;
      boolean actionOwnsMovement = actionOwnsMovement(entity);
      Vec3 motion = entity.getDeltaMovement();
      double desiredY = ServantFlightHelper.desiredHoverY(entity, target);
      double desiredVertical = ServantFlightHelper.verticalVelocityToward(
         entity.getY(), desiredY, 0.12, 0.025,
         ServantFlightHelper.MAX_VERTICAL_SPEED_UP, ServantFlightHelper.MAX_VERTICAL_SPEED_DOWN);
      double vertical = safeVerticalVelocity(entity.getY(), desiredY, motion.y, desiredVertical,
         disconnected, recentlyControlled && !entity.wasTacticalAiHandledThisTick());
      Vec3 corrected = new Vec3(motion.x, vertical, motion.z);

      boolean canRedirect = !actionOwnsMovement && !entity.isPerformingAction()
         && !ServantCombatSystem.cannotAct(entity);
      if (canRedirect && shouldForceContact(disconnected)) {
         corrected = contactMotion(entity, target, corrected, desiredY, 0.62);
      } else if (canRedirect && shouldDescend(disconnected) && !recentlyControlled
         && corrected.horizontalDistanceSqr() < 0.018) {
         corrected = contactMotion(entity, target, corrected, desiredY, 0.34);
      }

      corrected = capHorizontal(corrected, shouldForceContact(disconnected) ? 0.82 : 0.68);
      corrected = collisionLimited(entity, corrected);
      entity.setDeltaMovement(corrected);
      entity.hasImpulse = true;
   }

   public static boolean forceMeleeApproach(ServantEntity entity, LivingEntity target, long now) {
      if (!isManagedFlight(entity, now) || target == null || !target.isAlive()
         || target.level() != entity.level() || ServantCombatSystem.cannotAct(entity)
         || entity.isPerformingAction() || actionOwnsMovement(entity)) {
         return false;
      }
      entity.getNavigation().stop();
      entity.getLookControl().setLookAt(target, 75.0F, 75.0F);
      entity.faceToward(target.position());
      double desiredY = ServantFlightHelper.desiredHoverY(entity, target);
      Vec3 motion = contactMotion(entity, target, entity.getDeltaMovement(), desiredY, 0.72);
      entity.setDeltaMovement(collisionLimited(entity, capHorizontal(motion, 0.88)));
      entity.hasImpulse = true;
      markControlled(entity, now);
      return true;
   }

   public static boolean shouldForceGroundedGravity(ServantEntity entity, long now) {
      return entity != null
         && !isManagedFlight(entity, now)
         && entity.isNoGravity()
         && entity.isAlive()
         && !entity.isSpiritualTransitionLocked()
         && !actionOwnsMovement(entity)
         && !ServantCombatMotionService.isLaunched(entity);
   }

   public static boolean shouldDescend(long disconnectedTicks) {
      return disconnectedTicks >= DESCENT_TICKS;
   }

   public static boolean shouldForceContact(long disconnectedTicks) {
      return disconnectedTicks >= FORCE_CONTACT_TICKS;
   }

   public static double safeVerticalVelocity(double currentY, double desiredY, double currentVelocity,
                                             double desiredVelocity, long disconnectedTicks,
                                             boolean roleControlledThisTick) {
      double result = roleControlledThisTick ? currentVelocity : currentVelocity * 0.55 + desiredVelocity;
      if (currentY > desiredY + 1.25 || shouldDescend(disconnectedTicks) && currentY > desiredY + 0.35) {
         result = Math.min(result, -0.06);
      } else if (currentY < desiredY - 0.75) {
         result = Math.max(result, 0.05);
      }
      return ServantFlightHelper.clampVerticalSpeed(result);
   }

   private static Vec3 contactMotion(ServantEntity entity, LivingEntity target, Vec3 current,
                                     double desiredY, double approachSpeed) {
      Vec3 toward = target.position().add(0.0, Math.min(1.0, target.getBbHeight() * 0.45), 0.0)
         .subtract(entity.position().add(0.0, entity.getBbHeight() * 0.35, 0.0));
      Vec3 horizontal = toward.multiply(1.0, 0.0, 1.0);
      Vec3 forward = horizontal.lengthSqr() > 1.0E-4 ? horizontal.normalize() : Vec3.ZERO;
      double vertical = ServantFlightHelper.verticalVelocityToward(
         entity.getY(), desiredY, 0.16, 0.03,
         ServantFlightHelper.MAX_VERTICAL_SPEED_UP, ServantFlightHelper.MAX_VERTICAL_SPEED_DOWN);
      if (entity.distanceTo(target) <= ServantCombatTempoService.basicAttackReach(entity, target) + 0.15) {
         forward = Vec3.ZERO;
         vertical = Math.min(vertical, 0.0);
      }
      entity.getNavigation().stop();
      entity.getLookControl().setLookAt(target, 75.0F, 75.0F);
      entity.faceToward(target.position());
      return new Vec3(current.x * 0.28 + forward.x * approachSpeed, vertical,
         current.z * 0.28 + forward.z * approachSpeed);
   }

   private static Vec3 capHorizontal(Vec3 motion, double maximum) {
      double horizontal = motion.horizontalDistance();
      if (horizontal <= maximum || horizontal < 1.0E-5) return motion;
      double scale = maximum / horizontal;
      return new Vec3(motion.x * scale, motion.y, motion.z * scale);
   }

   private static Vec3 collisionLimited(ServantEntity entity, Vec3 wanted) {
      Vec3 limited = Entity.collideBoundingBox(entity, wanted, entity.getBoundingBox(), entity.level(), List.of());
      return new Vec3(limited.x, ServantFlightHelper.clampVerticalSpeed(limited.y), limited.z);
   }

   private static void stabilizeWithoutTarget(ServantEntity entity) {
      double desiredY = ServantFlightHelper.desiredHoverY(entity, null);
      Vec3 motion = entity.getDeltaMovement();
      double vertical = ServantFlightHelper.verticalVelocityToward(
         entity.getY(), desiredY, 0.1, 0.02,
         ServantFlightHelper.MAX_VERTICAL_SPEED_UP, ServantFlightHelper.MAX_VERTICAL_SPEED_DOWN);
      entity.setDeltaMovement(collisionLimited(entity,
         new Vec3(motion.x * 0.72, safeVerticalVelocity(entity.getY(), desiredY, motion.y, vertical,
            DESCENT_TICKS, false), motion.z * 0.72)));
      entity.hasImpulse = true;
   }

   private static boolean actionOwnsMovement(ServantEntity entity) {
      if (!ServantPlannedActionExecutor.isActive(entity)) return false;
      ServantPlannedActionExecutor.Stage stage = ServantPlannedActionExecutor.stage(entity);
      return stage != ServantPlannedActionExecutor.Stage.APPROACH;
   }

   private static LivingEntity validTarget(ServantEntity entity) {
      LivingEntity target = entity.getTarget();
      return target != null && target.isAlive() && target.level() == entity.level() ? target : null;
   }

   private static boolean isManagedFlight(ServantEntity entity, long now) {
      if (entity instanceof MedeaEntity medea) return medea.isFlyingMode();
      if (entity instanceof GilgameshEntity gilgamesh) return gilgamesh.isFlyingMode();
      if (entity instanceof CasterGilgameshEntity casterGilgamesh) return casterGilgamesh.isFlyingMode();
      if (entity instanceof EnkiduEntity enkidu) return EnkiduCombatHelper.isFlying(enkidu);
      return entity instanceof OdaNobunagaEntity oda && OdaNobunagaCombatHelper.isCombatFlying(oda, now);
   }

}
