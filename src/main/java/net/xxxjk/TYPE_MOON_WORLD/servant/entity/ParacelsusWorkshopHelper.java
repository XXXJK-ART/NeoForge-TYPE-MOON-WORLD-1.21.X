package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

public final class ParacelsusWorkshopHelper {
   public static final String TAG_INSIDE_WORKSHOP = "ParacelsusInsideWorkshop";
   public static final String TAG_WORKSHOP_TYPE = "ParacelsusWorkshopType";
   public static final String TAG_CENTER_X = "ParacelsusWorkshopCenterX";
   public static final String TAG_CENTER_Y = "ParacelsusWorkshopCenterY";
   public static final String TAG_CENTER_Z = "ParacelsusWorkshopCenterZ";
   public static final String TAG_RADIUS = "ParacelsusWorkshopRadius";
   private static final String TAG_LAST_WORKSHOP_SCAN = "ParacelsusLastWorkshopScan";
   public static final int WORKSHOP_NONE = 0;
   public static final int WORKSHOP_SPHERE = 1;
   private static final double SIMPLE_WORKSHOP_RADIUS = 15.0;
   private static final long WORKSHOP_RETRY_INTERVAL = 200L;
   private static final ResourceLocation WORKSHOP_SPEED_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "paracelsus_workshop_speed");
   private static final ResourceLocation WORKSHOP_MANA_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "paracelsus_workshop_mana");

   private ParacelsusWorkshopHelper() {
   }

   public static void tickEnvironment(ParacelsusEntity entity) {
      if (!(entity.level() instanceof ServerLevel level) || !entity.isAlive()) {
         return;
      }

      long now = level.getGameTime();
      if (!hasWorkshop(entity) && now - entity.getPersistentData().getLong(TAG_LAST_WORKSHOP_SCAN) >= WORKSHOP_RETRY_INTERVAL) {
         entity.getPersistentData().putLong(TAG_LAST_WORKSHOP_SCAN, now);
         establishWorkshop(entity, level);
      }

      boolean inside = refreshWorkshopState(entity);
      applyWorkshopSpeedModifier(entity, inside);
      regenerateMana(entity, inside);
   }

   public static boolean hasWorkshop(ParacelsusEntity entity) {
      return entity.getPersistentData().getInt(TAG_WORKSHOP_TYPE) != WORKSHOP_NONE;
   }

   public static boolean refreshWorkshopState(ParacelsusEntity entity) {
      boolean inside = isInsideWorkshop(entity, entity.position());
      entity.getPersistentData().putBoolean(TAG_INSIDE_WORKSHOP, inside);
      return inside;
   }

   public static boolean isInsideWorkshop(ParacelsusEntity entity, Vec3 position) {
      if (entity.getPersistentData().getInt(TAG_WORKSHOP_TYPE) != WORKSHOP_SPHERE) {
         return false;
      }
      Vec3 center = getWorkshopCenter(entity);
      double radius = getWorkshopRadius(entity);
      return center.distanceToSqr(position) <= radius * radius;
   }

   public static Vec3 getWorkshopCenter(ParacelsusEntity entity) {
      return new Vec3(
         entity.getPersistentData().getDouble(TAG_CENTER_X),
         entity.getPersistentData().getDouble(TAG_CENTER_Y),
         entity.getPersistentData().getDouble(TAG_CENTER_Z)
      );
   }

   public static double getWorkshopRadius(ParacelsusEntity entity) {
      return Math.max(1.0, entity.getPersistentData().getDouble(TAG_RADIUS));
   }

   public static float applyWorkshopDamageBonus(ParacelsusEntity entity, float baseDamage) {
      return isInsideWorkshop(entity, entity.position()) ? baseDamage * 1.18F : baseDamage;
   }

   public static double adjustedManaCost(ParacelsusEntity entity, double baseCost) {
      return isInsideWorkshop(entity, entity.position()) ? baseCost * 0.85 : baseCost;
   }

   private static void establishWorkshop(ParacelsusEntity entity, ServerLevel level) {
      BlockPos origin = entity.blockPosition();
      BlockPos center = findNearbyWorkshopCenter(entity, level, origin, 28);
      if (center == null) {
         entity.getPersistentData().putInt(TAG_WORKSHOP_TYPE, WORKSHOP_NONE);
         return;
      }
      applySphereWorkshop(entity, center);
   }

   private static BlockPos findNearbyWorkshopCenter(ParacelsusEntity entity, ServerLevel level, BlockPos origin, int radius) {
      BlockPos best = null;
      double bestScore = Double.NEGATIVE_INFINITY;
      for (int dx = -radius; dx <= radius; dx += 2) {
         for (int dz = -radius; dz <= radius; dz += 2) {
            int x = origin.getX() + dx;
            int z = origin.getZ() + dz;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos candidate = new BlockPos(x, y, z);
            if (!isSafeFeetPosition(level, candidate)) {
               continue;
            }
            Vec3 center = new Vec3(candidate.getX() + 0.5, candidate.getY(), candidate.getZ() + 0.5);
            if (wouldOverlapExistingWorkshop(level, center, SIMPLE_WORKSHOP_RADIUS, entity)) {
               continue;
            }
            double score = candidate.getY() * 4.0 - candidate.distSqr(origin);
            if (score > bestScore) {
               bestScore = score;
               best = candidate;
            }
         }
      }
      return best;
   }

   private static boolean wouldOverlapExistingWorkshop(ServerLevel level, Vec3 center, double radius, @Nullable ParacelsusEntity self) {
      AABB search = new AABB(center, center).inflate(radius + 32.0);
      for (MedeaEntity medea : level.getEntitiesOfClass(MedeaEntity.class, search, MedeaWorkshopHelper::hasWorkshop)) {
         if (overlapsMedeaWorkshop(medea, center, radius)) {
            return true;
         }
      }
      for (ParacelsusEntity other : level.getEntitiesOfClass(ParacelsusEntity.class, search, e -> e != self && hasWorkshop(e))) {
         Vec3 otherCenter = getWorkshopCenter(other);
         double otherRadius = getWorkshopRadius(other);
         double maxDistance = radius + otherRadius;
         if (center.distanceToSqr(otherCenter) <= maxDistance * maxDistance) {
            return true;
         }
      }
      return false;
   }

   private static boolean overlapsMedeaWorkshop(MedeaEntity medea, Vec3 center, double radius) {
      int type = medea.getPersistentData().getInt(MedeaWorkshopHelper.TAG_WORKSHOP_TYPE);
      if (type == MedeaWorkshopHelper.WORKSHOP_NONE) {
         return false;
      }
      if (type == MedeaWorkshopHelper.WORKSHOP_SPHERE) {
         Vec3 otherCenter = MedeaWorkshopHelper.getWorkshopCenter(medea);
         double otherRadius = medea.getPersistentData().getDouble(MedeaWorkshopHelper.TAG_RADIUS);
         double maxDistance = radius + otherRadius;
         return center.distanceToSqr(otherCenter) <= maxDistance * maxDistance;
      }
      AABB bounds = MedeaWorkshopHelper.getWorkshopBounds(medea);
      return bounds != null && distanceToAabbSqr(center, bounds) <= radius * radius;
   }

   private static double distanceToAabbSqr(Vec3 point, AABB box) {
      double dx = Mth.clamp(point.x, box.minX, box.maxX) - point.x;
      double dy = Mth.clamp(point.y, box.minY, box.maxY) - point.y;
      double dz = Mth.clamp(point.z, box.minZ, box.maxZ) - point.z;
      return dx * dx + dy * dy + dz * dz;
   }

   private static void applySphereWorkshop(ParacelsusEntity entity, BlockPos center) {
      entity.getPersistentData().putInt(TAG_WORKSHOP_TYPE, WORKSHOP_SPHERE);
      entity.getPersistentData().putDouble(TAG_CENTER_X, center.getX() + 0.5);
      entity.getPersistentData().putDouble(TAG_CENTER_Y, center.getY());
      entity.getPersistentData().putDouble(TAG_CENTER_Z, center.getZ() + 0.5);
      entity.getPersistentData().putDouble(TAG_RADIUS, SIMPLE_WORKSHOP_RADIUS);
   }

   private static void applyWorkshopSpeedModifier(ParacelsusEntity entity, boolean inside) {
      var speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
      if (speed == null) {
         return;
      }
      if (inside) {
         if (speed.getModifier(WORKSHOP_SPEED_MODIFIER_ID) == null) {
            speed.addTransientModifier(new AttributeModifier(WORKSHOP_SPEED_MODIFIER_ID, 0.08, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
         }
      } else {
         speed.removeModifier(WORKSHOP_SPEED_MODIFIER_ID);
      }
   }

   private static void regenerateMana(ParacelsusEntity entity, boolean inside) {
      if (entity.tickCount % 10 != 0) {
         return;
      }
      double regen = inside ? 5.5 : 1.2;
      entity.setCurrentMp(Math.min(entity.getMaxMp(), entity.getCurrentMp() + regen));
   }

   private static boolean isSafeFeetPosition(ServerLevel level, BlockPos feetPos) {
      BlockPos below = feetPos.below();
      return level.getBlockState(below).isSolidRender(level, below)
         && level.getBlockState(feetPos).getCollisionShape(level, feetPos).isEmpty()
         && level.getBlockState(feetPos.above()).getCollisionShape(level, feetPos.above()).isEmpty();
   }
}
