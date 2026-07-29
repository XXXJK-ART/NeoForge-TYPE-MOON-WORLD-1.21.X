package net.xxxjk.TYPE_MOON_WORLD.world.terrain;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.xxxjk.TYPE_MOON_WORLD.Config;
import net.xxxjk.TYPE_MOON_WORLD.network.TerrainDebrisMessage;
import net.xxxjk.typemoonworld.api.event.TerrainImpactBlockEvent;

public final class TerrainImpactService {
   public enum Shape { GROUND_LOWER_HEMISPHERE, SURFACE_HEMISPHERE, AIR_SPHERE }
   public enum Permission { PLAYER, NPC }

   private TerrainImpactService() { }

   public static boolean impact(ServerLevel level, @Nullable LivingEntity source, Vec3 center,
                                TerrainImpactProfile profile, Shape shape) {
      return impact(level, source, center, profile, permission(source), shape);
   }

   public static boolean impact(ServerLevel level, @Nullable LivingEntity source, Vec3 center,
                                TerrainImpactProfile profile, Permission permission, Shape shape) {
      if (level == null || center == null || profile == null || profile.tier() == TerrainImpactProfile.Tier.NONE
         || profile.radius() <= 0.0 || !allowed(level, permission)) return false;

      List<TerrainDebrisMessage.Sample> debris = new ArrayList<>(Math.min(48, profile.debrisCount()));
      int[] physicalDebris = {0};
      long debrisSeed = level.random.nextLong();
      Integer minimumSelfFootY = selfFootMinimumY(source, center, profile, shape);
      DeferredTerrainDestruction.BlockPredicate predicate = (serverLevel, pos, distanceSqr, radius, origin) -> {
         BlockState state = serverLevel.getBlockState(pos);
         if (minimumSelfFootY != null && pos.getY() < minimumSelfFootY) return false;
         if (NeoForge.EVENT_BUS.post(new TerrainImpactBlockEvent(serverLevel, source, pos, state, profile, shape.name())).isCanceled()) return false;
         if (physicalDebris[0] < profile.physicalDebrisCount()
            && shouldBecomePhysicalDebris(serverLevel, pos, state, profile.tier(), debrisSeed)
            && PhysicalTerrainDebrisService.launch(serverLevel, pos, state, center, profile.tier())) {
            physicalDebris[0]++;
            addVisualSample(debris, profile, pos, state);
            return false;
         }
         return true;
      };
      DeferredTerrainDestruction.DetailedRemovalCallback callback = (serverLevel, pos, previous, removed) -> {
         addVisualSample(debris, profile, pos, previous);
      };
      Runnable completion = () -> finishVisuals(level, center, profile, debris);
      int targetTicks = Math.max(2, (int)Math.ceil(profile.radius()));

      if (shape == Shape.AIR_SPHERE) {
         DeferredTerrainDestruction.queueSphereDetailed(level, center, profile.radius(), profile.maximumHardness(),
            targetTicks, predicate, callback, completion);
      } else {
         Vec3 shapedCenter = shape == Shape.SURFACE_HEMISPHERE ? center.add(0.0, profile.radius() * 0.3, 0.0) : center;
         DeferredTerrainDestruction.queueHemisphere(level, shapedCenter, profile.radius(), true, profile.maximumHardness(),
            targetTicks, predicate, callback, completion);
      }
      return true;
   }

   @Nullable
   private static Integer selfFootMinimumY(@Nullable LivingEntity source, Vec3 center, TerrainImpactProfile profile, Shape shape) {
      if (source == null || shape == Shape.AIR_SPHERE || !profile.limitsSelfFootDepth()) return null;
      double dx = source.getX() - center.x;
      double dz = source.getZ() - center.z;
      if (dx * dx + dz * dz > 9.0 || Math.abs(source.getY() - center.y) > 3.0) return null;
      return net.minecraft.util.Mth.floor(source.getY()) - 1;
   }

   private static boolean shouldBecomePhysicalDebris(ServerLevel level, BlockPos pos, BlockState state,
                                                      TerrainImpactProfile.Tier tier, long seed) {
      if (tier == TerrainImpactProfile.Tier.NONE || tier == TerrainImpactProfile.Tier.CHIP
         || tier == TerrainImpactProfile.Tier.SMALL || state.getRenderShape() == RenderShape.INVISIBLE) return false;
      boolean exposed = false;
      for (Direction direction : Direction.values()) {
         BlockPos adjacent = pos.relative(direction);
         if (level.getBlockState(adjacent).getCollisionShape(level, adjacent).isEmpty()) {
            exposed = true;
            break;
         }
      }
      if (!exposed) return false;
      int divisor = tier == TerrainImpactProfile.Tier.MEDIUM ? 3 : tier == TerrainImpactProfile.Tier.HEAVY ? 2 : 1;
      long mixed = pos.asLong() ^ seed;
      mixed ^= mixed >>> 33;
      mixed *= 0xff51afd7ed558ccdL;
      mixed ^= mixed >>> 33;
      return Math.floorMod((int)(mixed ^ mixed >>> 32), divisor) == 0;
   }

   private static void addVisualSample(List<TerrainDebrisMessage.Sample> debris, TerrainImpactProfile profile,
                                       BlockPos pos, BlockState state) {
      if (debris.size() < Math.min(48, profile.debrisCount())) {
         debris.add(new TerrainDebrisMessage.Sample(pos.immutable(), Block.getId(state)));
      }
   }

   private static Permission permission(@Nullable LivingEntity source) {
      return source instanceof Player ? Permission.PLAYER : Permission.NPC;
   }

   private static boolean allowed(ServerLevel level, Permission permission) {
      if (!Config.terrainDestructionEnabled) return false;
      if (permission == Permission.PLAYER) return Config.playerTerrainDestructionEnabled;
      return Config.npcTerrainDestructionEnabled && level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
   }

   private static void finishVisuals(ServerLevel level, Vec3 center, TerrainImpactProfile profile,
                                     List<TerrainDebrisMessage.Sample> debris) {
      if (debris.isEmpty()) return;
      int dustRemaining = Math.min(64, profile.dustCount());
      for (int i = 0; i < debris.size() && dustRemaining > 0; i++) {
         TerrainDebrisMessage.Sample sample = debris.get(i);
         BlockState state = Block.stateById(sample.stateId());
         int count = Math.min(dustRemaining, Math.max(1, 64 / debris.size()));
         BlockPos pos = sample.pos();
         level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5,
            pos.getY() + 0.5, pos.getZ() + 0.5, count, 0.2, 0.15, 0.2, 0.08);
         dustRemaining -= count;
      }

      TerrainDebrisMessage payload = new TerrainDebrisMessage(center, level.random.nextLong(), debris);
      double rangeSqr = 96.0 * 96.0;
      for (ServerPlayer player : level.players()) {
         if (player.distanceToSqr(center) <= rangeSqr && NetworkRegistry.hasChannel(player.connection, payload.type().id())) {
            PacketDistributor.sendToPlayer(player, payload);
         }
      }
   }
}
