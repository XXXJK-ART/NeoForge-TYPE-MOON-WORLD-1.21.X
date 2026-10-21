package net.xxxjk.TYPE_MOON_WORLD.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.concealment.ServantConcealment;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public final class ServantConcealmentEvents {
   private ServantConcealmentEvents() { }

   @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
   public static void onAttack(AttackEntityEvent event) {
      if (event.getEntity().level().isClientSide()) return;
      ServantConcealment.breakConcealment(event.getEntity());
      if (event.getTarget() instanceof LivingEntity target) ServantConcealment.breakConcealment(target);
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
   public static void onIncomingDamage(LivingIncomingDamageEvent event) {
      if (event.getEntity().level().isClientSide() || event.getAmount() <= 0.0F) return;
      ServantConcealment.breakConcealment(event.getEntity());
      if (event.getSource().getEntity() instanceof LivingEntity attacker) {
         ServantConcealment.breakConcealment(attacker);
      }
   }

   @SubscribeEvent
   public static void onProjectileFired(EntityJoinLevelEvent event) {
      if (!event.getLevel().isClientSide() && event.getEntity() instanceof Projectile projectile
         && projectile.getOwner() instanceof LivingEntity owner) {
         ServantConcealment.breakConcealment(owner);
      }
   }
}
