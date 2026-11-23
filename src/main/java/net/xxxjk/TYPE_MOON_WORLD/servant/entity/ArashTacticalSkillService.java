package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterTargeting;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public final class ArashTacticalSkillService {
   public static final double RETREAT_DISTANCE = 6.0;
   public static final String RETREAT_UNTIL = "ArashRetreatUntil";
   public static final double KNOCKBACK_RADIUS = 3.5;
   public static final double KNOCKBACK_CONE_DOT = 0.5;
   public static final float KNOCKBACK_DAMAGE = 18.0F;
   public static final double KNOCKBACK_STRENGTH = 1.25;

   private ArashTacticalSkillService() { }

   public static boolean performRetreat(LivingEntity caster) {
      if (!(caster.level() instanceof ServerLevel)) return false;
      LivingEntity target = caster instanceof Mob mob ? mob.getTarget() : null;
      Vec3 away = target == null ? caster.getLookAngle().scale(-1.0)
         : caster.position().subtract(target.position());
      away = new Vec3(away.x, 0.0, away.z);
      if (away.lengthSqr() < 1.0E-6) away = new Vec3(0.0, 0.0, 1.0);
      away = away.normalize();
      if (caster instanceof Mob mob) {
         mob.getNavigation().stop();
         mob.getMoveControl().strafe(0.0F, 0.0F);
         mob.getPersistentData().putLong(RETREAT_UNTIL, caster.level().getGameTime() + 10L);
         mob.getPersistentData().remove("ArashCrossoverUntil");
      }
      caster.setDeltaMovement(away.scale(RETREAT_DISTANCE / 4.5)
         .add(0.0, Math.max(0.0, 0.25 - caster.getDeltaMovement().y), 0.0));
      caster.hurtMarked = true;
      caster.hasImpulse = true;
      if (caster instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity servant) {
         servant.setCombatPose(1, 12);
         servant.triggerNamedActionAnimation("retreat");
      }
      return true;
   }

   public static boolean performMeleeKnockback(LivingEntity caster) {
      if (!(caster.level() instanceof ServerLevel level)) return false;
      Vec3 forward = new Vec3(caster.getLookAngle().x, 0.0, caster.getLookAngle().z);
      if (forward.lengthSqr() < 1.0E-6) return false;
      forward = forward.normalize();
      AABB area = caster.getBoundingBox().inflate(KNOCKBACK_RADIUS, 1.5, KNOCKBACK_RADIUS);
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
         entity -> entity != caster && entity.isAlive() && EntityUtils.isValidCombatTarget(caster, entity)
            && !(caster instanceof Player player && ServantMasterTargeting.isContractMaster(player, entity)))) {
         Vec3 offset = target.position().subtract(caster.position());
         Vec3 horizontal = new Vec3(offset.x, 0.0, offset.z);
         if (horizontal.lengthSqr() > KNOCKBACK_RADIUS * KNOCKBACK_RADIUS
            || horizontal.lengthSqr() < 1.0E-6 || forward.dot(horizontal.normalize()) < KNOCKBACK_CONE_DOT) continue;
         if (caster instanceof Player player) target.hurt(caster.damageSources().playerAttack(player), KNOCKBACK_DAMAGE);
         else if (caster instanceof Mob mob) target.hurt(caster.damageSources().mobAttack(mob), KNOCKBACK_DAMAGE);
         Vec3 push = horizontal.normalize().scale(KNOCKBACK_STRENGTH);
         target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.3, push.z));
         target.hurtMarked = true;
      }
      if (caster instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity servant) {
         servant.swing(InteractionHand.MAIN_HAND, true);
         servant.triggerNamedActionAnimation("melee_knockback");
      }
      return true;
   }
}
