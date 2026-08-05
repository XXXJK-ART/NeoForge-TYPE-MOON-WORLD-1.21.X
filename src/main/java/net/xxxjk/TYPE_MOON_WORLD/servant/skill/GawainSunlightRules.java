package net.xxxjk.TYPE_MOON_WORLD.servant.skill;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Shared Numeral of the Saint environment check for NPC and transformed-player Gawain. */
public final class GawainSunlightRules {
   private GawainSunlightRules() { }

   public static boolean isActive(ServerLevel level, BlockPos pos) {
      return level != null && pos != null && isActive(
         level.dimensionType().hasSkyLight(), level.getDayTime(),
         level.isRaining(), level.isThundering(), level.canSeeSky(pos.above()));
   }

   public static boolean isActive(boolean hasSkyLight, long dayTime, boolean raining,
                                  boolean thundering, boolean canSeeSky) {
      long timeOfDay = Math.floorMod(dayTime, 24000L);
      return hasSkyLight && timeOfDay < 12000L && !raining && !thundering && canSeeSky;
   }
}
