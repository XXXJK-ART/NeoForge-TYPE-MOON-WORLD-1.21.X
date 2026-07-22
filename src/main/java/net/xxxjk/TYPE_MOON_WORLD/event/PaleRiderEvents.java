package net.xxxjk.TYPE_MOON_WORLD.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
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

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onControlledFriendlyFire(LivingIncomingDamageEvent event) {
      Entity attacker = event.getSource().getEntity();
      Entity direct = event.getSource().getDirectEntity();
      if (PaleRiderInfectionService.arePaleRiderAllies(event.getEntity(), attacker)
         || PaleRiderInfectionService.arePaleRiderAllies(event.getEntity(), direct)) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public static void onPaleRiderMeleeInfection(LivingIncomingDamageEvent event) {
      if (!event.isCanceled() && event.getSource().is(DamageTypes.MOB_ATTACK)
         && event.getSource().getEntity() instanceof PaleRiderEntity rider) {
         PaleRiderInfectionService.infect(event.getEntity(), rider, 2);
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onControlledTargetChange(LivingChangeTargetEvent event) {
      if (event.getEntity() instanceof Mob mob && PaleRiderInfectionService.isControlled(mob)) {
         event.setNewAboutToBeSetTarget(PaleRiderInfectionService.getCommandTarget(mob));
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
      if (owner == null && event.getSource().is(net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderDamageTypes.INFECTION)
         && PaleRiderInfectionService.getOwner(level, event.getEntity()) instanceof PaleRiderEntity rider) {
         owner = rider;
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
