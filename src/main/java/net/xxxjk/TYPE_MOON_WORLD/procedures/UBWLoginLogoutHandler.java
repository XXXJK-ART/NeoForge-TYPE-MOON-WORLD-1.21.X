package net.xxxjk.TYPE_MOON_WORLD.procedures;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.DimensionTransition;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works.ChantHandler;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.world.dimension.ModDimensions;

@EventBusSubscriber
public class UBWLoginLogoutHandler {
   @SubscribeEvent
   public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (vars.is_in_ubw) {
            ChantHandler.returnFromUBW(player, vars);
         } else if (hasHajunReturnData(player)) {
            returnFromHajunOnRelog(player);
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (vars.is_in_ubw) {
            ChantHandler.returnFromUBW(player, vars);
         }
      }
   }

   private static void returnFromHajunOnRelog(ServerPlayer player) {
      if (player.server == null) {
         return;
      }
      String dimensionId = player.getPersistentData().contains("OdaHajunTargetReturnDim")
         ? player.getPersistentData().getString("OdaHajunTargetReturnDim")
         : player.getPersistentData().getString("OdaHajunReturnDim");
      ServerLevel returnLevel = player.server.overworld();
      ResourceLocation location = ResourceLocation.tryParse(dimensionId);
      if (location != null) {
         ServerLevel resolved = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, location));
         if (resolved != null) returnLevel = resolved;
      }
      String prefix = player.getPersistentData().contains("OdaHajunTargetReturnDim") ? "OdaHajunTargetReturn" : "OdaHajunReturn";
      double x = player.getPersistentData().contains(prefix + "X") ? player.getPersistentData().getDouble(prefix + "X") : player.getX();
      double y = player.getPersistentData().contains(prefix + "Y") ? player.getPersistentData().getDouble(prefix + "Y") : player.getY();
      double z = player.getPersistentData().contains(prefix + "Z") ? player.getPersistentData().getDouble(prefix + "Z") : player.getZ();
      player.changeDimension(new DimensionTransition(returnLevel, new net.minecraft.world.phys.Vec3(x, y, z), net.minecraft.world.phys.Vec3.ZERO, player.getYRot(), player.getXRot(), DimensionTransition.DO_NOTHING));
      player.getPersistentData().remove("OdaHajunReturnDim");
      player.getPersistentData().remove("OdaHajunReturnX");
      player.getPersistentData().remove("OdaHajunReturnY");
      player.getPersistentData().remove("OdaHajunReturnZ");
      player.getPersistentData().remove("OdaHajunTargetOwner");
      player.getPersistentData().remove("OdaHajunTargetReturnDim");
      player.getPersistentData().remove("OdaHajunTargetReturnX");
      player.getPersistentData().remove("OdaHajunTargetReturnY");
      player.getPersistentData().remove("OdaHajunTargetReturnZ");
      player.getPersistentData().remove("OdaHajunTargetPrimary");
   }

   private static boolean hasHajunReturnData(ServerPlayer player) {
      return player.getPersistentData().contains("OdaHajunReturnDim") || player.getPersistentData().contains("OdaHajunTargetReturnDim");
   }
}
