package net.xxxjk.TYPE_MOON_WORLD.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

/** Tracking clients use server-confirmed guard state, including the local player. */
public final class ServantGuardPoseClient {
   private static final Map<UUID, Long> GUARD_UNTIL = new HashMap<>();
   private static ClientLevel lastLevel;

   private ServantGuardPoseClient() { }

   private static ClientLevel level() {
      ClientLevel level = Minecraft.getInstance().level;
      if (lastLevel != level) {
         GUARD_UNTIL.clear();
         lastLevel = level;
      }
      return level;
   }

   public static void apply(UUID playerId, boolean guarding) {
      ClientLevel level = level();
      if (level == null) return;
      if (guarding) GUARD_UNTIL.put(playerId, level.getGameTime() + 20);
      else GUARD_UNTIL.remove(playerId);
   }

   public static boolean isGuarding(Player player) {
      ClientLevel level = level();
      if (level == null || !player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servant_card_transformed) return false;
      Long until = GUARD_UNTIL.get(player.getUUID());
      if (until == null) return false;
      if (until <= level.getGameTime()) {
         GUARD_UNTIL.remove(player.getUUID());
         return false;
      }
      return true;
   }

   /** The same weapon guard pose as HumanoidServantRenderer. */
   public static void applyPose(HumanoidModel<?> model) {
      model.rightArm.xRot = -1.05F;
      model.leftArm.xRot = -1.05F;
      model.rightArm.yRot = -0.55F;
      model.leftArm.yRot = 0.55F;
      model.rightArm.zRot = 0.18F;
      model.leftArm.zRot = -0.18F;
      model.body.xRot = 0.10F;
      if (model instanceof PlayerModel<?> playerModel) {
         playerModel.rightSleeve.copyFrom(model.rightArm);
         playerModel.leftSleeve.copyFrom(model.leftArm);
         playerModel.jacket.copyFrom(model.body);
      }
   }
}
