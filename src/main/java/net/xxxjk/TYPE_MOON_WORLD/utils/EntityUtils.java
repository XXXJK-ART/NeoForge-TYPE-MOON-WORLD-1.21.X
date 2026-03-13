package net.xxxjk.TYPE_MOON_WORLD.utils;

import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class EntityUtils {
   private static final double RIGHT_HAND_CAST_FORWARD = 0.78;
   private static final double RIGHT_HAND_CAST_RIGHT = 0.36;
   private static final double RIGHT_HAND_CAST_UP = -0.22;

   public static boolean isSpectatorPlayer(Entity entity) {
      return entity instanceof Player player && player.isSpectator();
   }

   public static boolean isImmunePlayerTarget(Entity entity) {
      return entity instanceof Player player && (player.isCreative() || player.isSpectator());
   }

   public static HitResult getRayTraceTarget(ServerPlayer player, double range) {
      float partialTicks = 1.0F;
      HitResult blockHit = player.pick(range, partialTicks, false);
      Vec3 eyePos = player.getEyePosition(partialTicks);
      double distToBlock = blockHit.getType() != Type.MISS ? blockHit.getLocation().distanceTo(eyePos) : range;
      Vec3 lookDir = player.getViewVector(partialTicks);
      Vec3 endPos = eyePos.add(lookDir.x * range, lookDir.y * range, lookDir.z * range);
      AABB searchBox = player.getBoundingBox().expandTowards(lookDir.scale(range)).inflate(1.0);
      EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
         player, eyePos, endPos, searchBox, e -> !e.isSpectator() && e.isPickable(), distToBlock * distToBlock
      );
      return (HitResult)(entityHit != null ? entityHit : blockHit);
   }

   public static Vec3 getAutoAimDirection(ServerPlayer caster, double range) {
      return getAutoAimDirection(caster, range, 50.0);
   }

   public static Vec3 getAutoAimDirection(ServerPlayer caster, double range, double maxAngleDeg) {
      if (caster == null) {
         return Vec3.ZERO;
      } else {
         LivingEntity target = findAutoAimTarget(caster, range, maxAngleDeg);
         Vec3 eyePos = caster.getEyePosition(1.0F);
         if (target != null) {
            Vec3 toTarget = target.getEyePosition().subtract(eyePos);
            if (toTarget.lengthSqr() > 1.0E-6) {
               return toTarget.normalize();
            }
         }

         Vec3 look = caster.getViewVector(1.0F);
         return look.lengthSqr() > 1.0E-6 ? look.normalize() : caster.getLookAngle().normalize();
      }
   }

   public static LivingEntity findAutoAimTarget(ServerPlayer caster, double range, double maxAngleDeg) {
      if (caster != null && !(range <= 0.0)) {
         if (getRayTraceTarget(caster, range) instanceof EntityHitResult entityHitResult
            && entityHitResult.getEntity() instanceof LivingEntity living
            && isValidCombatTarget(caster, living)) {
            return living;
         } else {
            Vec3 eyePos = caster.getEyePosition(1.0F);
            Vec3 lookDir = caster.getViewVector(1.0F).normalize();
            if (lookDir.lengthSqr() < 1.0E-6) {
               lookDir = caster.getLookAngle().normalize();
            }

            double cosThreshold = Math.cos(Math.toRadians(Math.max(1.0, Math.min(89.0, maxAngleDeg))));
            AABB searchBox = caster.getBoundingBox().expandTowards(lookDir.scale(range)).inflate(2.5);
            List<LivingEntity> candidates = caster.level()
               .getEntitiesOfClass(LivingEntity.class, searchBox, targetx -> targetx != null && targetx.isAlive() && isValidCombatTarget(caster, targetx));
            LivingEntity best = null;
            double bestScore = Double.MAX_VALUE;

            for (LivingEntity target : candidates) {
               if (caster.hasLineOfSight(target)) {
                  Vec3 toTarget = target.getEyePosition().subtract(eyePos);
                  double dist = toTarget.length();
                  if (!(dist < 1.0E-4) && !(dist > range + 2.0)) {
                     Vec3 dir = toTarget.scale(1.0 / dist);
                     double dot = lookDir.dot(dir);
                     if (!(dot < cosThreshold)) {
                        double score = dist + (1.0 - dot) * range * 0.8;
                        if (score < bestScore) {
                           bestScore = score;
                           best = target;
                        }
                     }
                  }
               }
            }

            return best;
         }
      } else {
         return null;
      }
   }

   public static boolean isValidCombatTarget(LivingEntity caster, LivingEntity target) {
      if (caster == null || target == null || !target.isAlive() || target == caster) {
         return false;
      } else if (isImmunePlayerTarget(target)) {
         return false;
      } else if (!caster.isAlliedTo(target) && !target.isAlliedTo(caster)) {
         if (caster instanceof Player casterPlayer) {
            if (target instanceof Player targetPlayer && !casterPlayer.canHarmPlayer(targetPlayer)) {
               return false;
            }

            if (target instanceof TamableAnimal tamable && tamable.isOwnedBy(casterPlayer)) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static Vec3 getRightHandCastAnchor(LivingEntity caster) {
      if (caster instanceof Player player) {
         HumanoidArm rightArm = HumanoidArm.RIGHT;
         InteractionHand rightHand = player.getMainArm() == rightArm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
         return getHandCastAnchor(player, rightHand);
      } else {
         float yawRad = caster.getYRot() * (float) (Math.PI / 180.0);
         Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0, Math.cos(yawRad));
         Vec3 up = new Vec3(0.0, 1.0, 0.0);
         Vec3 right = forward.cross(up);
         if (right.lengthSqr() < 1.0E-6) {
            right = new Vec3(1.0, 0.0, 0.0);
         } else {
            right = right.normalize();
         }

         return caster.getEyePosition().add(forward.scale(0.78)).add(right.scale(0.36)).add(up.scale(-0.22));
      }
   }

   public static boolean hasAnyEmptyHand(Player player) {
      return resolveEmptyCastingHand(player) != null;
   }

   public static InteractionHand resolveEmptyCastingHand(Player player) {
      boolean mainEmpty = player.getMainHandItem().isEmpty();
      boolean offEmpty = player.getOffhandItem().isEmpty();
      if (!mainEmpty && !offEmpty) {
         return null;
      } else if (mainEmpty && !offEmpty) {
         return InteractionHand.MAIN_HAND;
      } else {
         return !mainEmpty ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
      }
   }

   public static HumanoidArm getArmForHand(Player player, InteractionHand hand) {
      HumanoidArm mainArm = player.getMainArm();
      return hand == InteractionHand.MAIN_HAND ? mainArm : mainArm.getOpposite();
   }

   public static Vec3 getHandCastAnchor(Player player, InteractionHand hand) {
      float yawRad = player.getYRot() * (float) (Math.PI / 180.0);
      Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0, Math.cos(yawRad));
      Vec3 up = new Vec3(0.0, 1.0, 0.0);
      Vec3 sideBase = forward.cross(up);
      if (sideBase.lengthSqr() < 1.0E-6) {
         sideBase = new Vec3(1.0, 0.0, 0.0);
      } else {
         sideBase = sideBase.normalize();
      }

      HumanoidArm arm = getArmForHand(player, hand);
      double side = arm == HumanoidArm.RIGHT ? 0.36 : -0.36;
      return player.getEyePosition().add(forward.scale(0.78)).add(sideBase.scale(side)).add(up.scale(-0.22));
   }

   public static HumanoidArm resolveEmptyCastingArm(Player player) {
      InteractionHand hand = resolveEmptyCastingHand(player);
      return hand == null ? null : getArmForHand(player, hand);
   }

   public static Vec3 getCurrentEmptyHandCastAnchor(Player player) {
      InteractionHand hand = resolveEmptyCastingHand(player);
      return hand == null ? getRightHandCastAnchor(player) : getHandCastAnchor(player, hand);
   }

   public static void triggerSwarmAnger(Level level, LivingEntity attacker, LivingEntity target) {
      if (target != null && target.isAlive() && attacker != null && !level.isClientSide) {
         if (!(attacker instanceof Player player && (player.isCreative() || player.isSpectator()))) {
            double range = 32.0;
            UUID attackerUUID = attacker.getUUID();
            if (!(target instanceof ZombifiedPiglin) && !(target instanceof Piglin)) {
               if (target instanceof Wolf) {
                  for (Wolf wolf : level.getEntitiesOfClass(Wolf.class, target.getBoundingBox().inflate(range), e -> e != target && e.isAlive() && !e.isTame())) {
                     wolf.setPersistentAngerTarget(attackerUUID);
                     wolf.setRemainingPersistentAngerTime(400);
                     wolf.setTarget(attacker);
                  }
               } else if (target instanceof Bee) {
                  for (Bee bee : level.getEntitiesOfClass(Bee.class, target.getBoundingBox().inflate(range), e -> e != target && e.isAlive())) {
                     bee.setPersistentAngerTarget(attackerUUID);
                     bee.setRemainingPersistentAngerTime(400);
                     bee.setTarget(attacker);
                  }
               } else if (target instanceof Silverfish) {
                  for (Silverfish s : level.getEntitiesOfClass(Silverfish.class, target.getBoundingBox().inflate(10.0), e -> e != target && e.isAlive())) {
                     s.setTarget(attacker);
                  }
               }
            } else {
               for (Monster mob : level.getEntitiesOfClass(
                  Monster.class,
                  target.getBoundingBox().inflate(range),
                  e -> e != target && e.isAlive() && (e instanceof ZombifiedPiglin || e instanceof Piglin)
               )) {
                  if (mob instanceof NeutralMob neutral) {
                     neutral.setPersistentAngerTarget(attackerUUID);
                     neutral.setRemainingPersistentAngerTime(400);
                     mob.setTarget(attacker);
                  }
               }
            }
         }
      }
   }
}
