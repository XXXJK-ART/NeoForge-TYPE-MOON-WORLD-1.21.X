package net.xxxjk.TYPE_MOON_WORLD.servant.ai;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

public final class ServantFlightHelper {
   public static final double CRUISE_HEIGHT_ABOVE_GROUND = 4.35;
   public static final double MAX_HEIGHT_ABOVE_GROUND = 5.0;
   public static final double TARGET_CLIMB_THRESHOLD = 2.45;

   private ServantFlightHelper() {
   }

   public static double groundY(ServantEntity entity) {
      if (entity.level() instanceof ServerLevel level) {
         BlockPos column = BlockPos.containing(entity.getX(), entity.getY(), entity.getZ());
         BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
         return surface.getY();
      }
      return Math.floor(entity.getY() - CRUISE_HEIGHT_ABOVE_GROUND);
   }

   public static double desiredHoverY(ServantEntity entity, @Nullable LivingEntity target) {
      double groundY = groundY(entity);
      double cruiseY = groundY + CRUISE_HEIGHT_ABOVE_GROUND;
      double maxY = groundY + MAX_HEIGHT_ABOVE_GROUND;
      if (target != null && target.isAlive() && target.getY() - entity.getY() >= TARGET_CLIMB_THRESHOLD) {
         return Mth.clamp(target.getY() + Math.min(0.8, target.getBbHeight() * 0.35), cruiseY, maxY);
      }
      return cruiseY;
   }

   public static double clampFlyingY(ServantEntity entity, double wantedY, @Nullable LivingEntity target) {
      double groundY = groundY(entity);
      double cruiseY = groundY + CRUISE_HEIGHT_ABOVE_GROUND;
      double maxY = groundY + MAX_HEIGHT_ABOVE_GROUND;
      if (target != null && target.isAlive() && target.getY() - entity.getY() >= TARGET_CLIMB_THRESHOLD) {
         return Mth.clamp(wantedY, cruiseY, maxY);
      }
      return Mth.clamp(wantedY, groundY + 3.7, cruiseY);
   }

   public static double verticalVelocityToward(double currentY, double desiredY, double gain, double minSpeed, double maxUpSpeed, double maxDownSpeed) {
      double delta = (desiredY - currentY) * gain;
      if (Math.abs(desiredY - currentY) < 0.08) {
         return 0.0;
      }
      if (delta > 0.0) {
         return Mth.clamp(delta, minSpeed, maxUpSpeed);
      }
      return Mth.clamp(delta, -maxDownSpeed, -minSpeed);
   }
}
