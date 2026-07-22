package net.xxxjk.TYPE_MOON_WORLD.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.PaleRiderEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.OwnedPaleRiderMob;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderInfectionService;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public final class PaleRiderEvents {
   private PaleRiderEvents() {
   }

   @SubscribeEvent
   public static void onEntityTick(EntityTickEvent.Post event) {
      if (event.getEntity() instanceof LivingEntity living && !living.level().isClientSide()
         && PaleRiderInfectionService.isInfected(living)) {
         PaleRiderInfectionService.tick(living);
      }
      if (event.getEntity() instanceof LivingEntity living && !living.level().isClientSide()) {
         net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderCombatHelper.cleanupExpiredPenalties(living);
      }
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public static void onLivingDeath(LivingDeathEvent event) {
      if (event.isCanceled() || !(event.getEntity().level() instanceof ServerLevel level)) {
         return;
      }
      PaleRiderEntity owner = resolveOwner(level, event.getSource().getEntity());
      if (owner == null) {
         owner = resolveOwner(level, event.getSource().getDirectEntity());
      }
      if (owner != null && owner.isAlive()) {
         owner.captureSoul(event.getEntity());
      }
   }

   private static PaleRiderEntity resolveOwner(ServerLevel level, Entity source) {
      if (source instanceof PaleRiderEntity rider) {
         return rider;
      }
      if (source instanceof OwnedPaleRiderMob owned) {
         return owned.getPaleRiderOwner();
      }
      if (source instanceof Projectile projectile && projectile.getOwner() != source) {
         PaleRiderEntity projectileOwner = resolveOwner(level, projectile.getOwner());
         if (projectileOwner != null) {
            return projectileOwner;
         }
      }
      if (source != null && source.getPersistentData().getBoolean(PaleRiderInfectionService.TAG_CONTROLLED)
         && source.getPersistentData().hasUUID(PaleRiderInfectionService.TAG_OWNER)
         && level.getEntity(source.getPersistentData().getUUID(PaleRiderInfectionService.TAG_OWNER)) instanceof PaleRiderEntity rider) {
         return rider;
      }
      return null;
   }
}
