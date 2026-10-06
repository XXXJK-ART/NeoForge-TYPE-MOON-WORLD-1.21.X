package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** Lightweight card action timeline shared by validation, interruption and HUD state. */
public final class ServantCardActionTimeline {
   public enum Stage { APPROACH, WINDUP, ACTIVE, RECOVERY }
   public static final String STAGE_TAG = "ServantCardActionStage";
   public static final String ACTION_ID_TAG = "ServantCardActionId";
   public static final String UNTIL_TAG = "ServantCardActionUntil";
   public static final String INTERRUPTIBLE_TAG = "ServantCardActionInterruptible";

   private ServantCardActionTimeline() { }

   public static boolean isActive(ServerPlayer player) {
      return player != null && !stage(player).isBlank()
         && player.level().getGameTime() < player.getPersistentData().getLong(UNTIL_TAG);
   }

   public static String stage(ServerPlayer player) {
      return player == null ? "" : player.getPersistentData().getString(STAGE_TAG);
   }

   public static void begin(ServerPlayer player, String actionId, int windupTicks, int activeTicks, int recoveryTicks) {
      if (player == null) return;
      long now = player.level().getGameTime();
      CompoundTag data = player.getPersistentData();
      data.putString(ACTION_ID_TAG, actionId == null ? "" : actionId);
      data.putString(STAGE_TAG, Stage.WINDUP.name());
      data.putLong(UNTIL_TAG, now + Math.max(1, windupTicks + activeTicks + recoveryTicks));
      data.putBoolean(INTERRUPTIBLE_TAG, true);
   }

   public static void active(ServerPlayer player) {
      if (player != null) player.getPersistentData().putString(STAGE_TAG, Stage.ACTIVE.name());
   }

   public static void recovery(ServerPlayer player) {
      if (player != null) player.getPersistentData().putString(STAGE_TAG, Stage.RECOVERY.name());
   }

   public static void clear(ServerPlayer player) {
      if (player == null) return;
      CompoundTag data = player.getPersistentData();
      data.remove(STAGE_TAG); data.remove(ACTION_ID_TAG); data.remove(UNTIL_TAG); data.remove(INTERRUPTIBLE_TAG);
   }
}
