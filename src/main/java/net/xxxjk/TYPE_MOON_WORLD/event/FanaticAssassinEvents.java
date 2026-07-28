package net.xxxjk.TYPE_MOON_WORLD.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.FanaticAssassinEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.fanatic.FanaticAssassinCombatHelper;

@EventBusSubscriber(modid = "typemoonworld")
public final class FanaticAssassinEvents {
   private FanaticAssassinEvents() {
   }

   @SubscribeEvent
   public static void rejectMentalEffects(MobEffectEvent.Applicable event) {
      if (event.getEntity() instanceof FanaticAssassinEntity
         && event.getEffectInstance().getEffect().is(FanaticAssassinCombatHelper.MENTAL_EFFECTS)) {
         event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
      }
   }
}
