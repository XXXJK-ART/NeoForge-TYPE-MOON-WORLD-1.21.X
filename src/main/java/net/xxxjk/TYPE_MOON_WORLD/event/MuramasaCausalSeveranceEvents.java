package net.xxxjk.TYPE_MOON_WORLD.event;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.magic.MuramasaDamageTypes;

/**
 * Keeps Tsumukari's causal severance damage outside every ordinary protection
 * layer, including protection handlers registered by other modules.
 */
@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public final class MuramasaCausalSeveranceEvents {
   private MuramasaCausalSeveranceEvents() {
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
   public static void forceCausalDamage(LivingIncomingDamageEvent event) {
      if (isCausalSeverance(event)) {
         restoreCausalDamage(event);
      }
   }

   @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
   public static void finalizeCausalDamage(LivingIncomingDamageEvent event) {
      if (isCausalSeverance(event)) {
         restoreCausalDamage(event);
      }
   }

   private static boolean isCausalSeverance(LivingIncomingDamageEvent event) {
      return event.getSource().is(MuramasaDamageTypes.TSUMUKARI_MURAMASA);
   }

   private static void restoreCausalDamage(LivingIncomingDamageEvent event) {
      event.setCanceled(false);
      event.setAmount(Float.MAX_VALUE);
      event.setInvulnerabilityTicks(0);
      for (DamageContainer.Reduction reduction : DamageContainer.Reduction.values()) {
         event.addReductionModifier(reduction, (container, amount) -> 0.0F);
      }
   }
}
