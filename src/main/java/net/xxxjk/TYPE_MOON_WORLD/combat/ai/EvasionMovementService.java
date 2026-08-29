package net.xxxjk.TYPE_MOON_WORLD.combat.ai;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatFormulas;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import net.xxxjk.TYPE_MOON_WORLD.chain.service.BindingService;

public final class EvasionMovementService {
   private static final String TAG_LAST_EVASION = "TypeMoonAiLastEvasionTick";
   private static final String TAG_DOUBLE_JUMP_USED = "TypeMoonAiDoubleJumpUsed";
   private static final String TAG_IMPULSE_UNTIL = "TypeMoonAiEvasionImpulseUntil";
   private static final String TAG_IMPULSE_X = "TypeMoonAiEvasionImpulseX";
   private static final String TAG_IMPULSE_Z = "TypeMoonAiEvasionImpulseZ";
   private static final long EVASION_COOLDOWN = 12L;

   private EvasionMovementService() { }

   public static void tickAirState(Mob entity) {
      long now = entity.level().getGameTime();
      if (now < entity.getPersistentData().getLong(TAG_IMPULSE_UNTIL) && !entity.horizontalCollision) {
         double x = entity.getPersistentData().getDouble(TAG_IMPULSE_X);
         double z = entity.getPersistentData().getDouble(TAG_IMPULSE_Z);
         if (x * x + z * z >= 0.04) {
            entity.getNavigation().stop();
            Vec3 movement = entity.getDeltaMovement();
            entity.setDeltaMovement(x, movement.y, z);
            entity.hasImpulse = true;
         }
      } else {
         entity.getPersistentData().remove(TAG_IMPULSE_UNTIL);
         entity.getPersistentData().remove(TAG_IMPULSE_X);
         entity.getPersistentData().remove(TAG_IMPULSE_Z);
      }
      if (entity.onGround() || entity.isInWaterOrBubble() || entity.isInLava()) {
         entity.getPersistentData().putBoolean(TAG_DOUBLE_JUMP_USED, false);
      }
   }

   public static boolean tryEvade(ServantEntity entity, Vec3 threatOrigin) {
      ServantDefinition definition = entity.getDefinition();
      ServantParams params = definition == null ? null : definition.parameters();
      int agility = ServantCombatFormulas.agilityStep(params);
      boolean explicitDouble = definition != null && definition.specialization().hasCombatAction("double_jump");
      return tryEvade(entity, threatOrigin, agility, explicitDouble);
   }

   public static boolean tryEvade(Mob entity, Vec3 threatOrigin, int agility, boolean explicitDouble) {
      return tryEvade(entity, threatOrigin, agility, explicitDouble, null);
   }

   public static boolean tryAdvanceEvade(ServantEntity entity, Vec3 threatOrigin, LivingEntity shooter) {
      ServantDefinition definition = entity.getDefinition();
      ServantParams params = definition == null ? null : definition.parameters();
      int agility = ServantCombatFormulas.agilityStep(params);
      boolean explicitDouble = definition != null && definition.specialization().hasCombatAction("double_jump");
      return tryEvade(entity, threatOrigin, agility, explicitDouble, shooter == null ? null : shooter.position());
   }

   private static boolean tryEvade(Mob entity, Vec3 threatOrigin, int agility, boolean explicitDouble,
                                   Vec3 pressureTarget) {
      if (BindingService.isBound(entity.getUUID())) {
         return false;
      }
      long now = entity.level().getGameTime();
      if (entity.getPersistentData().contains(TAG_LAST_EVASION)
         && now < entity.getPersistentData().getLong(TAG_LAST_EVASION) + EVASION_COOLDOWN) return false;
      JumpCapability capability = jumpCapability(agility, explicitDouble, !entity.onGround(),
         entity.getPersistentData().getBoolean(TAG_DOUBLE_JUMP_USED));

      List<Vec3> candidates;
      if (pressureTarget != null) {
         Vec3 toward = horizontalToward(entity.position(), pressureTarget, entity.getLookAngle());
         candidates = forwardEvasionDirections(toward);
      } else {
         Vec3 away = horizontalAway(entity.position(), threatOrigin, entity.getLookAngle().scale(-1.0));
         candidates = candidateDirections(away);
      }
      boolean executed = entity.onGround()
         ? performGroundEvasion(entity, candidates, agility)
         : capability == JumpCapability.DOUBLE_JUMP && performDoubleJump(entity, candidates);
      if (!executed) return false;

      entity.getPersistentData().putLong(TAG_LAST_EVASION, now);
      entity.getNavigation().stop();
      Vec3 impulse = entity.getDeltaMovement();
      entity.getPersistentData().putLong(TAG_IMPULSE_UNTIL, now + 5L);
      entity.getPersistentData().putDouble(TAG_IMPULSE_X, impulse.x);
      entity.getPersistentData().putDouble(TAG_IMPULSE_Z, impulse.z);
      entity.hasImpulse = true;
      entity.hurtMarked = true;
      return true;
   }

   private static boolean performGroundEvasion(Mob entity, List<Vec3> candidates, int agility) {
      double distance = agility >= 3 ? 3.2 : 2.15;
      for (Vec3 direction : candidates) {
         if (!safeLanding(entity, direction, distance)) continue;
         double horizontal = agility >= 3 ? 0.82 : 0.62;
         double vertical = agility >= 3 ? 0.46 : 0.12;
         face(entity, direction);
         entity.setDeltaMovement(direction.x * horizontal, Math.max(entity.getDeltaMovement().y, vertical), direction.z * horizontal);
         return true;
      }
      if (entity.onGround() && !candidates.isEmpty()) {
         Vec3 direction = candidates.getFirst();
         face(entity, direction);
         double horizontal = agility >= 3 ? 0.82 : 0.62;
         double vertical = agility >= 3 ? 0.46 : 0.12;
         entity.setDeltaMovement(direction.x * horizontal, Math.max(entity.getDeltaMovement().y, vertical), direction.z * horizontal);
         return true;
      }
      return false;
   }

   private static boolean performDoubleJump(Mob entity, List<Vec3> candidates) {
      if (entity.getPersistentData().getBoolean(TAG_DOUBLE_JUMP_USED)) return false;
      for (Vec3 direction : candidates) {
         if (!safeAirCorridor(entity, direction, 2.4)) continue;
         entity.getPersistentData().putBoolean(TAG_DOUBLE_JUMP_USED, true);
         face(entity, direction);
         entity.setDeltaMovement(direction.x * 0.9, Math.max(0.42, entity.getDeltaMovement().y + 0.28), direction.z * 0.9);
         entity.fallDistance = 0.0F;
         return true;
      }
      return false;
   }

   private static boolean safeLanding(Mob entity, Vec3 direction, double distance) {
      Level level = entity.level();
      Vec3 offset = direction.scale(distance);
      AABB destination = entity.getBoundingBox().move(offset.x, 0.15, offset.z);
      if (!level.noCollision(entity, destination)) return false;
      BlockPos feet = BlockPos.containing(entity.position().add(offset));
      for (int drop = 0; drop <= 3; drop++) {
         BlockPos ground = feet.below(drop + 1);
         BlockState state = level.getBlockState(ground);
         if (state.getFluidState().is(FluidTags.LAVA)) return false;
         if (!state.getFluidState().isEmpty()) continue;
         if (state.isFaceSturdy(level, ground, Direction.UP)) return true;
      }
      return false;
   }

   private static boolean safeAirCorridor(Mob entity, Vec3 direction, double distance) {
      Vec3 offset = direction.scale(distance);
      AABB swept = entity.getBoundingBox().expandTowards(offset.x, 0.75, offset.z).inflate(0.1);
      if (!entity.level().noCollision(entity, swept)) return false;
      BlockPos destination = BlockPos.containing(entity.position().add(offset));
      return !entity.level().getFluidState(destination).is(FluidTags.LAVA);
   }

   private static Vec3 horizontalAway(Vec3 position, Vec3 threat, Vec3 fallback) {
      Vec3 away = threat == null ? fallback : position.subtract(threat);
      away = new Vec3(away.x, 0.0, away.z);
      if (away.lengthSqr() < 1.0E-5) away = new Vec3(fallback.x, 0.0, fallback.z);
      return away.lengthSqr() < 1.0E-5 ? new Vec3(0.0, 0.0, 1.0) : away.normalize();
   }

   private static Vec3 horizontalToward(Vec3 position, Vec3 target, Vec3 fallback) {
      Vec3 toward = target == null ? fallback : target.subtract(position);
      toward = new Vec3(toward.x, 0.0, toward.z);
      if (toward.lengthSqr() < 1.0E-5) toward = new Vec3(fallback.x, 0.0, fallback.z);
      return toward.lengthSqr() < 1.0E-5 ? new Vec3(0.0, 0.0, 1.0) : toward.normalize();
   }

   static List<Vec3> forwardEvasionDirections(Vec3 toward) {
      Vec3 forward = toward.lengthSqr() < 1.0E-5 ? new Vec3(0.0, 0.0, 1.0) : toward.normalize();
      Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
      List<Vec3> candidates = new ArrayList<>(4);
      candidates.add(forward.add(side).normalize());
      candidates.add(forward.subtract(side).normalize());
      double tanSeventy = Math.tan(Math.toRadians(70.0));
      candidates.add(forward.add(side.scale(tanSeventy)).normalize());
      candidates.add(forward.subtract(side.scale(tanSeventy)).normalize());
      return candidates;
   }

   private static List<Vec3> candidateDirections(Vec3 away) {
      Vec3 side = new Vec3(-away.z, 0.0, away.x);
      List<Vec3> candidates = new ArrayList<>(5);
      candidates.add(away);
      candidates.add(away.add(side.scale(0.65)).normalize());
      candidates.add(away.subtract(side.scale(0.65)).normalize());
      candidates.add(side);
      candidates.add(side.scale(-1.0));
      return candidates;
   }

   public static JumpCapability jumpCapability(int agility, boolean explicitDoubleJump, boolean airborne, boolean doubleJumpUsed) {
      if (airborne) return !doubleJumpUsed && (agility >= 5 || explicitDoubleJump) ? JumpCapability.DOUBLE_JUMP : JumpCapability.NONE;
      if (agility < 3) return JumpCapability.BACKSTEP;
      return JumpCapability.SINGLE_JUMP;
   }

   public enum JumpCapability { NONE, BACKSTEP, SINGLE_JUMP, DOUBLE_JUMP }

   private static void face(Mob entity, Vec3 direction) {
      if (entity instanceof ServantEntity servant) servant.faceVector(direction);
      else entity.getLookControl().setLookAt(entity.getX() + direction.x * 4.0, entity.getEyeY(), entity.getZ() + direction.z * 4.0, 40.0F, 40.0F);
   }
}
