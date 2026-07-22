package net.xxxjk.TYPE_MOON_WORLD.martial;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

public final class MartialHighJumpService {
   private static final String TAG_USED = "TypeMoonMartialHighJumpUsed";
   private static final String TAG_AIRBORNE = "TypeMoonMartialHighJumpAirborne";
   private static final String TAG_LAST_SUPPORT = "TypeMoonMartialHighJumpLastSupport";
   private static final double SUPPORT_DEPTH = 0.08;
   private static final long INPUT_GRACE_TICKS = 2L;

   private MartialHighJumpService() {}

   public static boolean tryConsume(ServerPlayer player) {
      if (player == null || player.getPersistentData().getBoolean(TAG_USED)) return false;
      long now = player.level().getGameTime();
      boolean supportedNow = player.onGround() && hasBlockSupport(player);
      boolean recentlySupported = player.getPersistentData().contains(TAG_LAST_SUPPORT)
         && now - player.getPersistentData().getLong(TAG_LAST_SUPPORT) <= INPUT_GRACE_TICKS
         && player.getDeltaMovement().y > 0.0;
      if (!supportedNow && !recentlySupported) return false;
      player.getPersistentData().putBoolean(TAG_USED, true);
      player.getPersistentData().remove(TAG_AIRBORNE);
      return true;
   }

   public static void tick(ServerPlayer player) {
      if (player == null) return;
      if (player.onGround() && hasBlockSupport(player)) {
         player.getPersistentData().putLong(TAG_LAST_SUPPORT, player.level().getGameTime());
      }
      if (!player.getPersistentData().getBoolean(TAG_USED)) return;
      if (!player.onGround()) {
         player.getPersistentData().putBoolean(TAG_AIRBORNE, true);
      } else if (player.getPersistentData().getBoolean(TAG_AIRBORNE) && hasBlockSupport(player)) {
         player.getPersistentData().remove(TAG_USED);
         player.getPersistentData().remove(TAG_AIRBORNE);
      }
   }

   static AABB supportBox(AABB playerBox) {
      double horizontalInset = Math.min(0.05, playerBox.getXsize() * 0.2);
      return new AABB(
         playerBox.minX + horizontalInset, playerBox.minY - SUPPORT_DEPTH,
         playerBox.minZ + horizontalInset, playerBox.maxX - horizontalInset,
         playerBox.minY + 0.01, playerBox.maxZ - horizontalInset
      );
   }

   private static boolean hasBlockSupport(ServerPlayer player) {
      return player.level().getBlockCollisions(player, supportBox(player.getBoundingBox())).iterator().hasNext();
   }
}
