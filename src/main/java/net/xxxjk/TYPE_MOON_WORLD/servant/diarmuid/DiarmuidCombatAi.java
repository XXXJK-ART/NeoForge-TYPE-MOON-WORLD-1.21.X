package net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.DiarmuidUaDuibhneEntity;

public final class DiarmuidCombatAi {
   private DiarmuidCombatAi() { }

   public static boolean tick(DiarmuidUaDuibhneEntity entity, ServerLevel level) {
      LivingEntity target = entity.getTarget();
      if (!DiarmuidAbilityService.validTarget(entity, target)) return false;
      if (DiarmuidAbilityService.anyActive(entity)) { entity.getNavigation().stop(); return true; }
      if (!DiarmuidAbilityService.canAct(entity)) return true;
      if (!DiarmuidAbilityService.isDoubleSpearStance(entity) && DiarmuidAbilityService.hasSpear(entity, true)
         && DiarmuidAbilityService.hasSpear(entity, false) && tryAction(entity, DiarmuidAction.DOUBLE_SPEAR_STANCE)) return true;
      entity.faceToward(target.position());
      entity.getLookControl().setLookAt(target, 50, 50);
      double distance = DiarmuidAbilityService.distance(entity, target);
      long now = level.getGameTime();
      if (now % 4 == 0) {
         if (distance < 3 && entity.getLastHurtByMob() == target && entity.tickCount - entity.getLastHurtByMobTimestamp() < 40
            && tryAction(entity, DiarmuidAction.CROSS_GUARD)) return true;
         if (distance < 1.6) {
            if (tryAction(entity, DiarmuidAction.LOW_SPIN) || tryAction(entity, DiarmuidAction.RETREATING_THRUST)) return true;
         }
         if (distance > 5 && distance <= 9 && tryAction(entity, DiarmuidAction.MANA_LEAP)) return true;
         if (distance <= 3) {
            DiarmuidAction[] close = {DiarmuidAction.YELLOW_DOUBLE, DiarmuidAction.CROSS_THRUST,
               DiarmuidAction.FIGURE_EIGHT, DiarmuidAction.ROSE_DANCE, DiarmuidAction.LOW_SPIN};
            int start = entity.getRandom().nextInt(close.length);
            for (int i = 0; i < close.length; i++) if (tryAction(entity, close[(start + i) % close.length])) return true;
         } else if (distance <= 5) {
            if (tryAction(entity, DiarmuidAction.ADVANCING_THRUST) || tryAction(entity, DiarmuidAction.RED_SWEEP)
               || distance <= 4 && tryAction(entity, DiarmuidAction.RISING_THRUST)) return true;
         }
         if (distance <= 3 && DiarmuidAbilityService.basicSpear(entity, false)) return true;
         if (distance <= 5 && DiarmuidAbilityService.basicSpear(entity, true)) return true;
      }
      if (distance > (DiarmuidAbilityService.hasSpear(entity, true) ? 4 : 2.5)) entity.getNavigation().moveTo(target, 1.15);
      else entity.getNavigation().stop();
      return true;
   }

   private static boolean tryAction(DiarmuidUaDuibhneEntity entity, DiarmuidAction action) {
      return DiarmuidAbilityService.start(entity, action);
   }
}
