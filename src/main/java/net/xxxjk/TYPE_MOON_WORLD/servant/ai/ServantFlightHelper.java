package net.xxxjk.TYPE_MOON_WORLD.servant.ai;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.nbt.CompoundTag;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

public final class ServantFlightHelper {
   public static final double CRUISE_HEIGHT_ABOVE_GROUND = 4.35;
   public static final double MAX_HEIGHT_ABOVE_GROUND = 5.0;
   public static final double TARGET_CLIMB_THRESHOLD = 2.45;
   public static final double COMBAT_BAND_BELOW = 2.0;
   public static final double COMBAT_BAND_ABOVE = 6.0;
   private static final String COMBAT_ANCHOR_Y = "TypeMoonCombatFlightAnchorY";
   private static final String COMBAT_ANCHOR_TARGET = "TypeMoonCombatFlightAnchorTarget";

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
      if (target != null && target.isAlive()) {
         double anchor = combatAnchorY(entity, target);
         long disconnected = ServantCombatTempoService.disconnectedTicks(entity, entity.level().getGameTime());
         return desiredCombatY(anchor, target.getY(), target.getBbHeight(), disconnected);
      }
      double groundY = groundY(entity);
      double cruiseY = groundY + CRUISE_HEIGHT_ABOVE_GROUND;
      double maxY = groundY + MAX_HEIGHT_ABOVE_GROUND;
      if (target != null && target.isAlive() && target.getY() - entity.getY() >= TARGET_CLIMB_THRESHOLD) {
         return Mth.clamp(target.getY() + Math.min(0.8, target.getBbHeight() * 0.35), cruiseY, maxY);
      }
      return cruiseY;
   }

   public static double clampFlyingY(ServantEntity entity, double wantedY, @Nullable LivingEntity target) {
      if (target != null && target.isAlive()) {
         double anchor = combatAnchorY(entity, target);
         return Mth.clamp(wantedY, anchor - COMBAT_BAND_BELOW, anchor + COMBAT_BAND_ABOVE);
      }
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

   public static void clearCombatAnchor(ServantEntity entity) {
      if (entity == null) return;
      entity.getPersistentData().remove(COMBAT_ANCHOR_Y);
      entity.getPersistentData().remove(COMBAT_ANCHOR_TARGET);
   }

   public static double desiredCombatY(double anchorY, double targetY, double targetHeight, long disconnectedTicks) {
      double offset = disconnectedTicks >= 100L ? 0.35
         : disconnectedTicks >= 40L ? 0.9 : Math.min(1.8, targetHeight + 0.5);
      return Mth.clamp(targetY + offset, anchorY - COMBAT_BAND_BELOW, anchorY + COMBAT_BAND_ABOVE);
   }

   private static double combatAnchorY(ServantEntity entity, LivingEntity target) {
      CompoundTag data = entity.getPersistentData();
      boolean sameTarget = data.hasUUID(COMBAT_ANCHOR_TARGET)
         && target.getUUID().equals(data.getUUID(COMBAT_ANCHOR_TARGET));
      if (!sameTarget || !data.contains(COMBAT_ANCHOR_Y)) {
         data.putUUID(COMBAT_ANCHOR_TARGET, target.getUUID());
         // A stale flyer can already be far above the target.  Anchor the band
         // to the target encounter instead of the roof currently beneath it.
         double encounterY = Math.min(entity.getY() - CRUISE_HEIGHT_ABOVE_GROUND, target.getY() + 1.0);
         data.putDouble(COMBAT_ANCHOR_Y, encounterY);
      }
      return data.getDouble(COMBAT_ANCHOR_Y);
   }
}
