package net.xxxjk.TYPE_MOON_WORLD.servant.sasaki;

import net.minecraft.world.entity.LivingEntity;

/** Shared PlayerAnimator playback for sword actions used by multiple Servants. */
public final class SwordActionAnimationService {
   private SwordActionAnimationService() { }

   public static void play(LivingEntity owner, SasakiKojiroAction action) {
      if (action != null && owner instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.OkitaSoujiSaberEntity okita
         && !owner.level().isClientSide()) okita.beginSwordAction(action.durationTicks());
      SasakiKojiroActionAnimationService.play(owner, action);
   }
}
