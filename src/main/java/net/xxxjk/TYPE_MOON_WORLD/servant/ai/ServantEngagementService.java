package net.xxxjk.TYPE_MOON_WORLD.servant.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.AiActionDescriptor;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.ServantActionProfile;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.ServantActionRegistry;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantClassType;

/** Shared spacing rules for melee and ranged combatants. */
public final class ServantEngagementService {
   private static final double MIN_DIRECTION_SQR = 1.0E-5;

   private ServantEngagementService() {
   }

   public enum CombatRole {
      MELEE,
      RANGED
   }

   public enum Matchup {
      MELEE_VS_MELEE,
      MELEE_VS_RANGED,
      RANGED_VS_MELEE,
      RANGED_VS_RANGED
   }

   public record RangeBand(double minimum, double preferred, double maximum) {
      public RangeBand {
         minimum = Math.max(0.0, minimum);
         preferred = Math.max(minimum, preferred);
         maximum = Math.max(preferred, maximum);
      }
   }

   public static CombatRole role(LivingEntity entity) {
      if (entity instanceof ServantEntity servant && servant.getDefinition() != null) {
         ServantActionProfile profile = ServantActionRegistry.get(servant.getDefinition().id());
         int projectileActions = countActions(profile, AiActionDescriptor.Tag.PROJECTILE);
         int meleeActions = countActions(profile, AiActionDescriptor.Tag.MELEE);
         return role(servant.getDefinition().classType(), projectileActions, meleeActions);
      }
      if (entity instanceof RangedAttackMob
         || entity instanceof MysticMagicianEntity magician && magician.isRangedWeaponMode()
         || entity.getMainHandItem().getItem() instanceof BowItem
         || entity.getMainHandItem().getItem() instanceof CrossbowItem
         || entity.getMainHandItem().is(Items.TRIDENT)) {
         return CombatRole.RANGED;
      }
      return CombatRole.MELEE;
   }

   public static CombatRole role(ServantClassType classType, int projectileActions, int meleeActions) {
      if (classType == ServantClassType.ARCHER || classType == ServantClassType.CASTER) {
         return CombatRole.RANGED;
      }
      return projectileActions > meleeActions ? CombatRole.RANGED : CombatRole.MELEE;
   }

   public static Matchup matchup(ServantEntity attacker, LivingEntity target) {
      boolean attackerRanged = role(attacker) == CombatRole.RANGED;
      boolean targetRanged = role(target) == CombatRole.RANGED;
      if (attackerRanged) {
         return targetRanged ? Matchup.RANGED_VS_RANGED : Matchup.RANGED_VS_MELEE;
      }
      return targetRanged ? Matchup.MELEE_VS_RANGED : Matchup.MELEE_VS_MELEE;
   }

   public static RangeBand rangedBand(LivingEntity target, double minimum, double preferred, double maximum) {
      return rangedBand(role(target) == CombatRole.RANGED, minimum, preferred, maximum);
   }

   public static RangeBand rangedBand(boolean targetRanged, double minimum, double preferred, double maximum) {
      if (targetRanged) {
         return new RangeBand(Math.max(3.0, minimum - 2.0), Math.max(minimum, preferred - 2.0), maximum);
      }
      return new RangeBand(minimum, preferred + 2.0, maximum + 3.0);
   }

   public static boolean maintainRangedPosition(
      ServantEntity entity,
      LivingEntity target,
      long gameTick,
      double minimum,
      double preferred,
      double maximum,
      double speed,
      String keyPrefix
   ) {
      RangeBand band = rangedBand(target, minimum, preferred, maximum);
      Vec3 destination = rangedDestination(entity, target, gameTick, band);
      double distance = entity.distanceTo(target);
      double movementSpeed = distance < band.minimum() ? speed * 1.12 : distance > band.maximum() ? speed * 1.08 : speed;
      return ServantNavigationHelper.moveToPositionThrottled(
         entity,
         destination,
         movementSpeed,
         gameTick,
         ServantNavigationHelper.SHORT_REPATH_INTERVAL,
         1.0,
         keyPrefix
      );
   }

   public static Vec3 rangedDestination(LivingEntity entity, LivingEntity target, long gameTick, RangeBand band) {
      double distance = entity.distanceTo(target);
      Vec3 radial = horizontal(entity.position().subtract(target.position()));
      if (radial.lengthSqr() < MIN_DIRECTION_SQR) {
         radial = horizontal(entity.getLookAngle()).scale(-1.0);
      }
      if (radial.lengthSqr() < MIN_DIRECTION_SQR) {
         radial = new Vec3(1.0, 0.0, 0.0);
      }
      radial = radial.normalize();

      boolean targetRanged = role(target) == CombatRole.RANGED;
      int direction = ((entity.getId() + (int)(gameTick / 80L)) & 1) == 0 ? 1 : -1;
      double angle = targetRanged ? (distance < band.minimum() ? 38.0 : 24.0)
         : distance < band.minimum() ? 16.0 : 10.0;
      Vec3 placementDirection = rotateHorizontal(radial, Math.toRadians(angle * direction));
      double desiredRadius = distance < band.minimum() ? band.preferred() + (targetRanged ? 0.0 : 2.0)
         : distance > band.maximum() ? band.preferred() : band.preferred();
      Vec3 prediction = cappedHorizontal(target.getDeltaMovement().scale(targetRanged ? 4.0 : 6.0), 6.0);
      Vec3 predictedTarget = target.position().add(prediction);
      Vec3 side = new Vec3(-radial.z, 0.0, radial.x).scale(direction * (targetRanged ? 3.0 : 1.5));
      Vec3 destination = predictedTarget.add(placementDirection.scale(desiredRadius)).add(side);
      return new Vec3(destination.x, target.getY(), destination.z);
   }

   public static Vec3 meleeApproachPoint(LivingEntity entity, LivingEntity target, long gameTick) {
      double distance = entity.distanceTo(target);
      if (role(target) != CombatRole.RANGED || distance <= 7.0) {
         return target.position();
      }
      double leadTicks = Math.max(2.0, Math.min(8.0, distance * 0.35));
      Vec3 predicted = target.position().add(cappedHorizontal(target.getDeltaMovement().scale(leadTicks), 8.0));
      Vec3 approach = horizontal(predicted.subtract(entity.position()));
      if (approach.lengthSqr() < MIN_DIRECTION_SQR) {
         return predicted;
      }
      approach = approach.normalize();
      int direction = ((entity.getId() + (int)(gameTick / 50L)) & 1) == 0 ? 1 : -1;
      Vec3 side = new Vec3(-approach.z, 0.0, approach.x).scale(direction * Math.min(3.0, 1.5 + distance * 0.025));
      return predicted.add(side);
   }

   private static int countActions(ServantActionProfile profile, AiActionDescriptor.Tag tag) {
      if (profile == null) {
         return 0;
      }
      int count = 0;
      for (AiActionDescriptor action : profile.actions()) {
         if (action.tags().contains(tag)) {
            count++;
         }
      }
      return count;
   }

   private static Vec3 horizontal(Vec3 vector) {
      return new Vec3(vector.x, 0.0, vector.z);
   }

   private static Vec3 cappedHorizontal(Vec3 vector, double maximumLength) {
      Vec3 horizontal = horizontal(vector);
      double lengthSqr = horizontal.lengthSqr();
      if (lengthSqr <= maximumLength * maximumLength) {
         return horizontal;
      }
      return horizontal.normalize().scale(maximumLength);
   }

   private static Vec3 rotateHorizontal(Vec3 vector, double radians) {
      double cosine = Math.cos(radians);
      double sine = Math.sin(radians);
      return new Vec3(vector.x * cosine - vector.z * sine, 0.0, vector.x * sine + vector.z * cosine);
   }
}
