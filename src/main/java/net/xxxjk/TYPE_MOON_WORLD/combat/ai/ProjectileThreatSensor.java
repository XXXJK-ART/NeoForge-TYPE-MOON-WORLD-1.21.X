package net.xxxjk.TYPE_MOON_WORLD.combat.ai;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import java.util.Set;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantSkillDefinition.FactBypass;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshGateWeaponProjectileEntity;

public final class ProjectileThreatSensor {
   private static final int MAX_PROJECTILES = 32;

   private ProjectileThreatSensor() { }

   public static int scanLimit() { return MAX_PROJECTILES; }

   public static IncomingProjectile nearest(Mob observer, double searchRadius, double maximumImpactTicks) {
      Vec3 center = observer.position().add(0.0, observer.getBbHeight() * 0.5, 0.0);
      IncomingProjectile best = null;
      int checked = 0;
      for (Projectile projectile : observer.level().getEntitiesOfClass(Projectile.class,
         observer.getBoundingBox().inflate(searchRadius), projectile -> hostile(observer, projectile))) {
         if (++checked > MAX_PROJECTILES) break;
         Vec3 velocity = projectile.getDeltaMovement();
         double velocitySqr = velocity.lengthSqr();
         if (velocitySqr < 0.04) continue;
         double impactTicks = center.subtract(projectile.position()).dot(velocity) / velocitySqr;
         if (impactTicks < 0.0 || impactTicks > maximumImpactTicks) continue;
         Vec3 closest = projectile.position().add(velocity.scale(impactTicks));
         double dangerRadius = observer.getBbWidth() * 0.65 + 0.85;
         if (closest.distanceToSqr(center) > dangerRadius * dangerRadius) continue;
         if (best == null || impactTicks < best.impactTicks()) {
            best = new IncomingProjectile(projectile, impactTicks, closest, ProjectileThreatClassifier.classify(projectile));
         }
      }
      return best;
   }

   /** Gate of Babylon uses a custom Entity base, but is still a moving projectile threat. */
   public static IncomingProjectileLike nearestGateWeapon(Mob observer, double searchRadius, double maximumImpactTicks) {
      Vec3 center = observer.position().add(0.0, observer.getBbHeight() * 0.5, 0.0);
      IncomingProjectileLike best = null;
      for (GilgameshGateWeaponProjectileEntity gate : observer.level().getEntitiesOfClass(
         GilgameshGateWeaponProjectileEntity.class, observer.getBoundingBox().inflate(searchRadius),
         projectile -> hostileGate(observer, projectile))) {
         if (gate.getLaunchDelay() >= gate.tickCount) continue;
         Vec3 velocity = gate.getDeltaMovement();
         double velocitySqr = velocity.lengthSqr();
         if (velocitySqr < 0.04) continue;
         double impactTicks = center.subtract(gate.position()).dot(velocity) / velocitySqr;
         if (impactTicks < 0.0 || impactTicks > maximumImpactTicks) continue;
         Vec3 closest = gate.position().add(velocity.scale(impactTicks));
         double dangerRadius = observer.getBbWidth() * 0.65 + 0.85;
         if (closest.distanceToSqr(center) > dangerRadius * dangerRadius) continue;
         if (best == null || impactTicks < best.impactTicks()) {
            best = new IncomingProjectileLike(gate, impactTicks, closest,
               ProjectileThreatClassifier.classify(gate));
         }
      }
      return best;
   }

   private static boolean hostile(Mob observer, Projectile projectile) {
      Entity owner = projectile.getOwner();
      if (owner == observer) return false;
      return !(owner instanceof LivingEntity living && observer.isAlliedTo(living));
   }

   private static boolean hostileGate(Mob observer, GilgameshGateWeaponProjectileEntity gate) {
      LivingEntity owner = gate.getOwnerEntity();
      return owner != observer && !(owner != null && observer.isAlliedTo(owner));
   }

   public record IncomingProjectileLike(GilgameshGateWeaponProjectileEntity projectile,
                                        double impactTicks, Vec3 closestPoint,
                                        Set<FactBypass> bypasses) {
      public IncomingProjectileLike {
         bypasses = bypasses == null ? Set.of() : Set.copyOf(bypasses);
      }
   }

   public record IncomingProjectile(Projectile projectile, double impactTicks, Vec3 closestPoint,
                                    Set<FactBypass> bypasses) {
      public IncomingProjectile(Projectile projectile, double impactTicks, Vec3 closestPoint) {
         this(projectile, impactTicks, closestPoint, ProjectileThreatClassifier.classify(projectile));
      }

      public IncomingProjectile {
         bypasses = bypasses == null ? Set.of() : Set.copyOf(bypasses);
      }
   }
}
