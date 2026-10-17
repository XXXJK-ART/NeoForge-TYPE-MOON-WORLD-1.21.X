package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

/** Ordinary NPC duels use a lower damage budget; NP and instant-death rules retain their damage. */
@EventBusSubscriber(modid = "typemoonworld")
public final class ServantDuelDamage {
   public static final float ORDINARY_DAMAGE_SCALE = 0.30F;
   private ServantDuelDamage() { }
   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void incoming(LivingIncomingDamageEvent event) {
      if (event.getEntity().level().isClientSide() || !(event.getEntity() instanceof ServantEntity)) return;
      if (!(event.getSource().getEntity() instanceof ServantEntity)) return;
      if (event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)
         || NoblePhantasmDamageClassifier.isNoblePhantasmDamage(event.getSource(), event.getOriginalAmount())) return;
      event.setAmount(event.getAmount() * ORDINARY_DAMAGE_SCALE);
   }
}
