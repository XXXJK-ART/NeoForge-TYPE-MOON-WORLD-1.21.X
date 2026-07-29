package net.xxxjk.TYPE_MOON_WORLD.servant.concealment;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.network.ConcealmentStateMessage;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public final class ConcealmentStateSync {
   private static final String TAG_SYNCED = "TypeMoonConcealmentStateSynced";
   private static final String TAG_LAST = "TypeMoonConcealmentLastState";

   private ConcealmentStateSync() {
   }

   public static void update(LivingEntity entity, boolean concealed) {
      if (entity.level().isClientSide) return;
      var data = entity.getPersistentData();
      if (data.getBoolean(TAG_SYNCED) && data.getBoolean(TAG_LAST) == concealed) return;
      data.putBoolean(TAG_SYNCED, true);
      data.putBoolean(TAG_LAST, concealed);
      broadcast(entity, concealed);
   }

   @SubscribeEvent
   public static void onStartTracking(PlayerEvent.StartTracking event) {
      if (!(event.getEntity() instanceof ServerPlayer tracker) || !(event.getTarget() instanceof LivingEntity target)) return;
      if (NetworkRegistry.hasChannel(tracker.connection, ConcealmentStateMessage.TYPE.id())) {
         PacketDistributor.sendToPlayer(tracker,
            new ConcealmentStateMessage(target.getUUID(), ServantConcealment.isFullyConcealed(target)));
      }
   }

   @SubscribeEvent
   public static void onDeath(LivingDeathEvent event) {
      if (!event.getEntity().level().isClientSide) broadcast(event.getEntity(), false);
   }

   @SubscribeEvent
   public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) broadcast(player, false);
   }

   @SubscribeEvent
   public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         broadcast(player, false);
         player.getPersistentData().remove(TAG_SYNCED);
      }
   }

   private static void broadcast(LivingEntity entity, boolean concealed) {
      try {
         PacketDistributor.sendToPlayersTrackingEntityAndSelf(
            entity, new ConcealmentStateMessage(entity.getUUID(), concealed));
      } catch (UnsupportedOperationException ignored) {
         // Visual-only compatibility payload.
      }
   }
}
