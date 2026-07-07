package net.xxxjk.TYPE_MOON_WORLD.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class CommandSpellVisualClient {
   private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();
   private static volatile boolean localCommandSpellPoseActive = false;

   private CommandSpellVisualClient() {
   }

   public static void apply(UUID playerId, boolean masterActive, int commandSpells, boolean poseActive) {
      if (playerId == null) {
         return;
      }
      if (!masterActive && !poseActive) {
         STATES.remove(playerId);
      } else {
         STATES.put(playerId, new State(masterActive, Math.max(0, commandSpells), poseActive));
      }
   }

   public static boolean shouldRenderMark(AbstractClientPlayer player) {
      return getCommandSpellCount(player) >= 0;
   }

   public static int getCommandSpellCount(AbstractClientPlayer player) {
      if (player == null) {
         return -1;
      }
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player != null && minecraft.player.getId() == player.getId()) {
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         return vars != null && vars.master_active ? Math.max(0, vars.master_command_spells) : -1;
      }
      State state = STATES.get(player.getUUID());
      return state != null && state.masterActive ? Math.max(0, state.commandSpells) : -1;
   }

   public static void setLocalCommandSpellPoseActive(boolean active) {
      localCommandSpellPoseActive = active;
   }

   public static boolean isCommandSpellPoseActive(Player player) {
      if (player == null) {
         return false;
      }
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player != null && minecraft.player.getId() == player.getId()) {
         return localCommandSpellPoseActive || minecraft.screen instanceof net.xxxjk.TYPE_MOON_WORLD.client.gui.MasterCommandSpellScreen;
      }
      State state = STATES.get(player.getUUID());
      return state != null && state.poseActive;
   }

   private record State(boolean masterActive, int commandSpells, boolean poseActive) {
   }
}
