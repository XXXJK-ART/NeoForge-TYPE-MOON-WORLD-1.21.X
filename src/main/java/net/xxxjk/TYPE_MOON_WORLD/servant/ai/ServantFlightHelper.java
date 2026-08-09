package net.xxxjk.TYPE_MOON_WORLD.servant.ai;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

public final class ServantFlightHelper {
   public static final double CRUISE_HEIGHT_ABOVE_GROUND = 4.35;
   public static final double MAX_HEIGHT_ABOVE_GROUND = 5.0;
   public static final double TARGET_CLIMB_THRESHOLD = 2.45;
   public static final double COMBAT_BAND_BELOW = 2.0;
   public static final double COMBAT_BAND_ABOVE = 6.0;
   public static final double MAX_VERTICAL_SPEED_UP = 0.24;
   public static final double MAX_VERTICAL_SPEED_DOWN = 0.30;
   private static final double ANCHOR_REBASE_THRESHOLD = 3.0;
   private static final double ANCHOR_REBASE_STEP = 0.24;
   private static final String COMBAT_ANCHOR_Y = "TypeMoonCombatFlightAnchorY";
   private static final String COMBAT_ANCHOR_TARGET = "TypeMoonCombatFlightAnchorTarget";
   private static final String COMBAT_ANCHOR_UPDATE = "TypeMoonCombatFlightAnchorUpdate";
   private static final String IDLE_ANCHOR_Y = "TypeMoonIdleFlightAnchorY";
   private static final String IDLE_ANCHOR_UPDATE = "TypeMoonIdleFlightAnchorUpdate";

   private ServantFlightHelper() {
   }

   public static double groundY(ServantEntity entity) {
      if (entity.level() instanceof ServerLevel level) {
         double support = supportSurfaceY(level, entity.getX(), entity.getY(), entity.getZ(), 32);
         if (Double.isFinite(support)) return support;
         BlockPos column = BlockPos.containing(entity.getX(), entity.getY(), entity.getZ());
         if (level.hasChunkAt(column)) {
            BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
            return surface.getY();
         }
      }
      return Math.floor(entity.getY() - CRUISE_HEIGHT_ABOVE_GROUND);
   }

   public static double desiredHoverY(ServantEntity entity, @Nullable LivingEntity target) {
      if (target != null && target.isAlive()) {
         double anchor = combatAnchorY(entity, target);
         long disconnected = ServantCombatTempoService.disconnectedTicks(entity, entity.level().getGameTime());
         return clampToWorld(entity, desiredCombatY(anchor, target.getY(), target.getBbHeight(), disconnected));
      }
      double groundY = idleAnchorY(entity);
      double cruiseY = groundY + CRUISE_HEIGHT_ABOVE_GROUND;
      return clampToWorld(entity, cruiseY);
   }

   public static double clampFlyingY(ServantEntity entity, double wantedY, @Nullable LivingEntity target) {
      if (target != null && target.isAlive()) {
         double anchor = combatAnchorY(entity, target);
         return clampToWorld(entity, Mth.clamp(wantedY, anchor - COMBAT_BAND_BELOW, anchor + COMBAT_BAND_ABOVE));
      }
      double groundY = idleAnchorY(entity);
      double cruiseY = groundY + CRUISE_HEIGHT_ABOVE_GROUND;
      return clampToWorld(entity, Mth.clamp(wantedY, groundY + 3.7, cruiseY));
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
      entity.getPersistentData().remove(COMBAT_ANCHOR_UPDATE);
   }

   public static void clearAllAnchors(ServantEntity entity) {
      if (entity == null) return;
      clearCombatAnchor(entity);
      entity.getPersistentData().remove(IDLE_ANCHOR_Y);
      entity.getPersistentData().remove(IDLE_ANCHOR_UPDATE);
   }

   public static double desiredCombatY(double anchorY, double targetY, double targetHeight, long disconnectedTicks) {
      double offset = disconnectedTicks >= 100L ? 0.35
         : disconnectedTicks >= 40L ? 0.9 : Math.min(1.8, targetHeight + 0.5);
      return Mth.clamp(targetY + offset, anchorY - COMBAT_BAND_BELOW, anchorY + COMBAT_BAND_ABOVE);
   }

   public static double clampVerticalSpeed(double velocityY) {
      return Mth.clamp(velocityY, -MAX_VERTICAL_SPEED_DOWN, MAX_VERTICAL_SPEED_UP);
   }

   public static double advanceAnchor(double currentAnchor, double desiredAnchor) {
      if (Math.abs(desiredAnchor - currentAnchor) < ANCHOR_REBASE_THRESHOLD) return currentAnchor;
      return currentAnchor + Mth.clamp(desiredAnchor - currentAnchor, -ANCHOR_REBASE_STEP, ANCHOR_REBASE_STEP);
   }

   private static double combatAnchorY(ServantEntity entity, LivingEntity target) {
      CompoundTag data = entity.getPersistentData();
      boolean sameTarget = data.hasUUID(COMBAT_ANCHOR_TARGET)
         && target.getUUID().equals(data.getUUID(COMBAT_ANCHOR_TARGET));
      double desiredAnchor = combatGroundAnchor(entity, target);
      if (!sameTarget || !data.contains(COMBAT_ANCHOR_Y)) {
         data.putUUID(COMBAT_ANCHOR_TARGET, target.getUUID());
         data.putDouble(COMBAT_ANCHOR_Y, desiredAnchor);
         data.putLong(COMBAT_ANCHOR_UPDATE, entity.level().getGameTime());
      } else if (data.getLong(COMBAT_ANCHOR_UPDATE) != entity.level().getGameTime()) {
         data.putDouble(COMBAT_ANCHOR_Y, advanceAnchor(data.getDouble(COMBAT_ANCHOR_Y), desiredAnchor));
         data.putLong(COMBAT_ANCHOR_UPDATE, entity.level().getGameTime());
      }
      return data.getDouble(COMBAT_ANCHOR_Y);
   }

   private static double combatGroundAnchor(ServantEntity entity, LivingEntity target) {
      if (entity.level() instanceof ServerLevel level) {
         double targetSupport = supportSurfaceY(level, target.getX(), target.getY(), target.getZ(), 24);
         if (Double.isFinite(targetSupport)) return targetSupport;
         double entitySupport = supportSurfaceY(level, entity.getX(), entity.getY(), entity.getZ(), 24);
         if (Double.isFinite(entitySupport)) return Math.min(entitySupport, target.getY());
      }
      return Math.min(entity.getY(), target.getY()) - CRUISE_HEIGHT_ABOVE_GROUND;
   }

   private static double idleAnchorY(ServantEntity entity) {
      CompoundTag data = entity.getPersistentData();
      double desired = groundY(entity);
      if (!data.contains(IDLE_ANCHOR_Y)) {
         data.putDouble(IDLE_ANCHOR_Y, desired);
         data.putLong(IDLE_ANCHOR_UPDATE, entity.level().getGameTime());
      } else if (data.getLong(IDLE_ANCHOR_UPDATE) != entity.level().getGameTime()) {
         double current = data.getDouble(IDLE_ANCHOR_Y);
         boolean needsLowerLayer = desired < current - ANCHOR_REBASE_THRESHOLD;
         boolean terrainImmediatelyAhead = desired > current + ANCHOR_REBASE_THRESHOLD
            && entity.getY() - desired < 1.75;
         if (needsLowerLayer || terrainImmediatelyAhead) {
            data.putDouble(IDLE_ANCHOR_Y, advanceAnchor(current, desired));
         }
         data.putLong(IDLE_ANCHOR_UPDATE, entity.level().getGameTime());
      }
      return data.getDouble(IDLE_ANCHOR_Y);
   }

   private static double clampToWorld(ServantEntity entity, double wantedY) {
      double minimum = entity.level().getMinBuildHeight() + 1.0;
      double maximum = entity.level().getMaxBuildHeight() - entity.getBbHeight() - 1.0;
      return Mth.clamp(wantedY, minimum, Math.max(minimum, maximum));
   }

   private static double supportSurfaceY(ServerLevel level, double x, double y, double z, int scanDepth) {
      int top = Math.min(Mth.floor(y - 0.05), level.getMaxBuildHeight() - 1);
      int bottom = Math.max(level.getMinBuildHeight(), top - Math.max(1, scanDepth));
      BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(Mth.floor(x), top, Mth.floor(z));
      if (!level.hasChunkAt(cursor)) return Double.NaN;
      for (int blockY = top; blockY >= bottom; blockY--) {
         cursor.setY(blockY);
         VoxelShape shape = level.getBlockState(cursor).getCollisionShape(level, cursor);
         if (!shape.isEmpty()) return blockY + shape.max(Direction.Axis.Y);
      }
      return Double.NaN;
   }
}
