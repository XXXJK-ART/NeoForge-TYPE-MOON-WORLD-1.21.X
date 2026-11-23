package net.xxxjk.TYPE_MOON_WORLD.world.terrain;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.EventHooks;
import net.xxxjk.TYPE_MOON_WORLD.Config;
import net.xxxjk.TYPE_MOON_WORLD.util.ModTags;

/** Shared terrain limits for large Noble Phantasm blast and endpoint phases. */
public final class NoblePhantasmTerrainLimits {
   private NoblePhantasmTerrainLimits() { }

   public static double radius(double requested) {
      return Math.max(0.0, requested);
   }

   public static boolean contains(Vec3 center, BlockPos pos, double radius) {
      if (center == null || pos == null || radius < 0.0) return false;
      return center.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= radius * radius;
   }

   public static boolean isAtOrAboveCenter(Vec3 center, BlockPos pos) {
      return center != null && pos != null && pos.getY() >= Math.ceil(center.y);
   }

   public static boolean canDestroy(ServerLevel level, Entity source, BlockPos pos, Vec3 center,
                                    double radius, float maximumHardness) {
      if (level == null || source == null || pos == null || !Config.terrainDestructionEnabled
         || !level.getWorldBorder().isWithinBounds(pos) || !level.hasChunkAt(pos)
         || !contains(center, pos, radius) || level.getBlockEntity(pos) != null) return false;
      if (source instanceof Player player) {
         if (!Config.playerTerrainDestructionEnabled || !level.mayInteract(player, pos)) return false;
      } else if (!(source instanceof LivingEntity living) || !Config.npcTerrainDestructionEnabled
         || !level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING)
         || !EventHooks.canEntityGrief(level, source)) return false;
      var state = level.getBlockState(pos);
      float hardness = state.getDestroySpeed(level, pos);
      return !state.isAir() && !state.is(ModTags.Blocks.TERRAIN_IMMUNE) && !state.is(Blocks.BEDROCK)
         && hardness >= 0.0F && hardness <= maximumHardness
         && state.getExplosionResistance(level, pos, null) < 1200.0F
         && state.canEntityDestroy(level, pos, source)
         && (!(source instanceof LivingEntity living) || EventHooks.onEntityDestroyBlock(living, pos, state));
   }
}
