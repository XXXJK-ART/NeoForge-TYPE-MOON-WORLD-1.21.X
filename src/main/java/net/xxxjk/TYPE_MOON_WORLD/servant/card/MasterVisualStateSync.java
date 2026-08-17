package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.network.MasterVisualStateMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ModNetwork;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public final class MasterVisualStateSync {
   private MasterVisualStateSync() {
   }

   public static void broadcast(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (player == null || vars == null) {
         return;
      }
      try {
         PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, build(player, vars));
      } catch (UnsupportedOperationException ignored) {
         // Mock and compatibility connections may not negotiate this visual-only payload.
      }
   }

   public static void broadcast(ServerPlayer player) {
      if (player != null) {
         broadcast(player, player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES));
      }
   }

   @SubscribeEvent
   public static void onStartTracking(PlayerEvent.StartTracking event) {
      if (event.getEntity() instanceof ServerPlayer tracker) {
         Entity target = event.getTarget();
         if (target instanceof ServerPlayer targetPlayer) {
            TypeMoonWorldModVariables.PlayerVariables vars = targetPlayer.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            ModNetwork.sendToPlayer(tracker, build(targetPlayer, vars));
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         broadcast(player);
      }
   }

   @SubscribeEvent
   public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         broadcast(player);
      }
   }

   @SubscribeEvent
   public static void onPlayerRespawned(PlayerEvent.PlayerRespawnEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         broadcast(player);
      }
   }

   @SubscribeEvent
   public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         try {
            PacketDistributor.sendToPlayersTrackingEntity(
               player,
               new MasterVisualStateMessage(player.getUUID(), false, 0, "default", false)
            );
         } catch (UnsupportedOperationException ignored) {
            // Visual cleanup is irrelevant for connections that do not support the payload.
         }
      }
   }

   private static MasterVisualStateMessage build(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      return new MasterVisualStateMessage(
         player.getUUID(),
         vars.master_active,
         Math.max(0, vars.master_command_spells),
         vars.master_command_spell_style == null || vars.master_command_spell_style.isBlank() ? "default" : vars.master_command_spell_style,
         vars.master_command_spell_pose_active && vars.master_active
      );
   }
}
