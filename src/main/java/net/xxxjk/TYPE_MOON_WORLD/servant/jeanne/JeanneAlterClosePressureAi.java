package net.xxxjk.TYPE_MOON_WORLD.servant.jeanne;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterProtection;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

/** Lancelot-style close pressure layered over Jeanne Alter's skill planner. */
final class JeanneAlterClosePressureAi {
   private static final String LAST_DASH_TICK = "JeanneAlterAiLastDashTick";
   private static final String LAST_MAUL_TICK = "JeanneAlterAiLastMaulTick";
   private static final double MELEE_RANGE = 3.2D;

   private JeanneAlterClosePressureAi() { }

   static void tick(JeanneAlterEntity entity, ServerLevel level) {
      LivingEntity target = entity.getTarget();
      if (!validTarget(entity, target) || entity.isPerformingAction()
            || ServantCombatSystem.cannotAct(entity)) return;

      long now = level.getGameTime();
      double distance = entity.distanceTo(target);
      entity.getLookControl().setLookAt(target, 60.0F, 60.0F);
      if (distance > MELEE_RANGE) {
         rush(entity, target, now);
      } else {
         maul(entity, target, now);
      }
   }

   private static void rush(JeanneAlterEntity entity, LivingEntity target, long now) {
      entity.getNavigation().moveTo(target, 1.62D);
      CompoundTag data = entity.getPersistentData();
      if (now - data.getLong(LAST_DASH_TICK) < 34L) return;
      Vec3 direction = target.position().subtract(entity.position()).multiply(1.0D, 0.0D, 1.0D);
      if (direction.lengthSqr() < 1.0E-4D) return;
      data.putLong(LAST_DASH_TICK, now);
      entity.setDeltaMovement(entity.getDeltaMovement().add(direction.normalize().scale(0.88D)).add(0.0D, 0.08D, 0.0D));
      entity.hurtMarked = true;
   }

   private static void maul(JeanneAlterEntity entity, LivingEntity target, long now) {
      entity.getNavigation().moveTo(target, 1.55D);
      Vec3 direction = target.position().subtract(entity.position()).multiply(1.0D, 0.0D, 1.0D);
      if (direction.lengthSqr() > 1.0E-4D) {
         Vec3 pressure = direction.normalize().scale(0.26D);
         entity.setDeltaMovement(entity.getDeltaMovement().add(pressure.x, 0.02D, pressure.z));
         entity.hurtMarked = true;
      }
      CompoundTag data = entity.getPersistentData();
      if (now - data.getLong(LAST_MAUL_TICK) < 8L) return;
      data.putLong(LAST_MAUL_TICK, now);
      entity.triggerBasicAttackAnimation();
      entity.doHurtTarget(target);
   }

   private static boolean validTarget(JeanneAlterEntity entity, LivingEntity target) {
      return target != null && target != entity && target.isAlive()
         && !entity.isAlliedTo(target) && !target.isAlliedTo(entity)
         && !ServantMasterProtection.isProtectedMaster(entity, target)
         && EntityUtils.isValidCombatTarget(entity, target);
   }
}
