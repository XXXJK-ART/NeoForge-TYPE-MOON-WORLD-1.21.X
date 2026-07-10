package net.xxxjk.TYPE_MOON_WORLD.magic.basic;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public final class BasicMagecraftHelper {
   public static final double LOW_MANA_MAX_THRESHOLD = 300.0;

   private BasicMagecraftHelper() {
   }

   public static double clampProficiency(double proficiency) {
      return Mth.clamp(proficiency, 0.0, 100.0);
   }

   public static LivingEntity rayTarget(ServerPlayer player, double range) {
      HitResult hit = EntityUtils.getRayTraceTarget(player, range);
      return hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living ? living : null;
   }

   public static boolean isLowManaTarget(LivingEntity target) {
      if (target instanceof Player player) {
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         return vars.is_magus && vars.player_max_mana < LOW_MANA_MAX_THRESHOLD;
      }

      if (target instanceof net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity) {
         TypeMoonWorldModVariables.PlayerVariables vars = target.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         return vars.player_max_mana < LOW_MANA_MAX_THRESHOLD;
      }

      return false;
   }

   public static boolean isCreativeOrSpectator(Entity entity) {
      return entity instanceof Player player && (player.isCreative() || player.isSpectator());
   }
}
