package net.xxxjk.TYPE_MOON_WORLD.magic.projection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.ExpandingRingEffectEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ProjectionCircuitEffectEntity;

public final class ProjectionEffectHelper {
   public static final int PROJECTION_BLUE = 0x39D8C8;

   private ProjectionEffectHelper() {
   }

   public static void spawnStructureStart(ServerLevel level, BlockPos anchorPos) {
      if (level != null && anchorPos != null) {
         Vec3 center = Vec3.atCenterOf(anchorPos);
         level.addFreshEntity(new ProjectionCircuitEffectEntity(level, center.x, center.y + 0.05, center.z, 0.8F, 4.8F, 0.78F, 18, PROJECTION_BLUE));
         level.addFreshEntity(new ExpandingRingEffectEntity(level, center.x, center.y + 0.05, center.z, 0.4F, 3.6F, 0.12F, 16, PROJECTION_BLUE, 0.52F, 0.02F));
         level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 0.2, center.z, 12, 0.22, 0.08, 0.22, 0.03);
      }
   }

   public static void spawnBlockPlace(ServerLevel level, BlockPos pos) {
      if (level != null && pos != null) {
         Vec3 center = Vec3.atCenterOf(pos);
         level.addFreshEntity(new ProjectionCircuitEffectEntity(level, center.x, center.y + 0.04, center.z, 0.18F, 0.9F, 0.72F, 10, PROJECTION_BLUE));
         level.addFreshEntity(new ExpandingRingEffectEntity(level, center.x, center.y + 0.04, center.z, 0.1F, 0.72F, 0.08F, 10, PROJECTION_BLUE, 0.44F, 0.01F));
         level.sendParticles(ParticleTypes.WAX_ON, center.x, center.y + 0.08, center.z, 4, 0.08, 0.05, 0.08, 0.01);
      }
   }

   public static void spawnBlockBreak(ServerLevel level, BlockPos pos) {
      if (level != null && pos != null) {
         Vec3 center = Vec3.atCenterOf(pos);
         level.addFreshEntity(new ProjectionCircuitEffectEntity(level, center.x, center.y + 0.04, center.z, 0.22F, 1.05F, 0.64F, 12, PROJECTION_BLUE));
         level.addFreshEntity(new ExpandingRingEffectEntity(level, center.x, center.y + 0.04, center.z, 0.14F, 0.86F, 0.08F, 12, PROJECTION_BLUE, 0.42F, 0.0F));
         level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 0.08, center.z, 6, 0.12, 0.08, 0.12, 0.02);
      }
   }
}
