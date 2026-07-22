package net.xxxjk.TYPE_MOON_WORLD.martial;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class MartialUkemiService {
   private static final String TAG_UKEMI_UNTIL = "TypeMoonMartialUkemiUntil";
   private static final String TAG_REDUCTION_UNTIL = "TypeMoonMartialUkemiReductionUntil";
   private static final String TAG_LAST_GROUNDED = "TypeMoonMartialLastGrounded";

   private MartialUkemiService() {}

   public static void migrate(TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars != null) vars.martial_ukemi_learned = shouldBeLearned(
         vars.martial_ukemi_learned, vars.bajiquan_proficiency, vars.ganryu_proficiency);
   }

   static boolean shouldBeLearned(boolean alreadyLearned, double bajiquanProficiency, double ganryuProficiency) {
      return alreadyLearned || bajiquanProficiency >= 30.0 || ganryuProficiency >= 50.0;
   }

   public static boolean isLearned(TypeMoonWorldModVariables.PlayerVariables vars) {
      migrate(vars);
      return vars != null && vars.martial_ukemi_learned;
   }

   public static void tick(ServerPlayer player) {
      long now = player.level().getGameTime();
      boolean grounded = player.onGround();
      boolean wasGrounded = player.getPersistentData().getBoolean(TAG_LAST_GROUNDED);
      if (!grounded && player.fallDistance > 2.0F) player.getPersistentData().putLong(TAG_UKEMI_UNTIL, now + 4L);
      if (grounded && !wasGrounded && player.getPersistentData().contains(TAG_UKEMI_UNTIL)
         && player.getPersistentData().getLong(TAG_UKEMI_UNTIL) >= now - 1L) {
         player.getPersistentData().putLong(TAG_UKEMI_UNTIL, now + 4L);
      }
      player.getPersistentData().putBoolean(TAG_LAST_GROUNDED, grounded);
   }

   public static boolean tryUse(ServerPlayer player, boolean styleActive) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      long now = player.level().getGameTime();
      if (!styleActive || !isLearned(vars) || !player.getPersistentData().contains(TAG_UKEMI_UNTIL)
         || player.getPersistentData().getLong(TAG_UKEMI_UNTIL) < now) return false;
      Vec3 dir = horizontalLook(player);
      player.setDeltaMovement(dir.x * 0.9, Math.max(0.12, player.getDeltaMovement().y), dir.z * 0.9);
      player.fallDistance *= 0.3F;
      player.getPersistentData().putLong(TAG_UKEMI_UNTIL, 0L);
      player.getPersistentData().putLong(TAG_REDUCTION_UNTIL, now + 4L);
      player.hurtMarked = true;
      return true;
   }

   public static boolean consumeReduction(ServerPlayer player) {
      if (!player.getPersistentData().contains(TAG_REDUCTION_UNTIL)
         || player.getPersistentData().getLong(TAG_REDUCTION_UNTIL) < player.level().getGameTime()) return false;
      player.getPersistentData().remove(TAG_REDUCTION_UNTIL);
      return true;
   }

   private static Vec3 horizontalLook(ServerPlayer player) {
      Vec3 dir = player.getLookAngle().multiply(1.0, 0.0, 1.0);
      return dir.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : dir.normalize();
   }
}
