package net.xxxjk.TYPE_MOON_WORLD.event;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.xxxjk.TYPE_MOON_WORLD.entity.GanderProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.magic.nordic.MagicGander;
import net.xxxjk.TYPE_MOON_WORLD.magic.nordic.MagicGandrMachineGun;

@EventBusSubscriber(
   modid = "typemoonworld"
)
public class GanderLifecycleCleanupEvents {
   @SubscribeEvent
   public static void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         cleanupPlayerState(player);
         if (player.getServer() != null) {
            for (ServerLevel level : player.getServer().getAllLevels()) {
               discardOwnedGanders(level, player);
            }
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         cleanupPlayerState(player);
         if (player.level() instanceof ServerLevel currentLevel) {
            discardOwnedGanders(currentLevel, player);
         }

         if (player.getServer() != null) {
            ServerLevel fromLevel = player.getServer().getLevel(event.getFrom());
            if (fromLevel != null && fromLevel != player.level()) {
               discardOwnedGanders(fromLevel, player);
            }
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerDeath(LivingDeathEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         cleanupPlayerState(player);
         if (player.level() instanceof ServerLevel level) {
            discardOwnedGanders(level, player);
         }
      }
   }

   private static void cleanupPlayerState(ServerPlayer player) {
      MagicGander.forceCleanup(player);
      MagicGandrMachineGun.forceCleanup(player);
   }

   private static void discardOwnedGanders(ServerLevel level, ServerPlayer owner) {
      List<GanderProjectileEntity> pending = new ArrayList<>();

      for (Entity entity : level.getEntities().getAll()) {
         if (entity instanceof GanderProjectileEntity projectile && projectile.isOwnedBy(owner)) {
            pending.add(projectile);
         }
      }

      for (GanderProjectileEntity projectile : pending) {
         projectile.discard();
      }
   }
}
