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
import net.xxxjk.TYPE_MOON_WORLD.Config;
import net.xxxjk.TYPE_MOON_WORLD.network.ModNetwork;
import net.xxxjk.TYPE_MOON_WORLD.network.TerrainDebrisMessage;
import net.xxxjk.typemoonworld.api.event.TerrainImpactBlockEvent;

public final class TerrainImpactService {
   public enum Shape { GROUND_LOWER_HEMISPHERE, SURFACE_HEMISPHERE, AIR_SPHERE, UPPER_SURFACE_CRATER }
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

      if (isHeracles(source)) return impactGroundLayer(level, source, center.add(0,-.2,0), profile, permission);

      List<TerrainDebrisMessage.Sample> debris = new ArrayList<>(Math.min(48, profile.debrisCount()));
      int[] physicalDebris = {0};
      long debrisSeed = level.random.nextLong();
      Integer minimumSelfFootY = selfFootMinimumY(source, center, profile, shape);
      DeferredTerrainDestruction.BlockPredicate predicate = (serverLevel, pos, distanceSqr, radius, origin) -> {
         if (!CombatTerrainProtection.canRemove(serverLevel, pos)) return false;
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
      } else if (shape == Shape.UPPER_SURFACE_CRATER) {
         Vec3 shapedCenter = center.add(0.0, Math.max(0.08, profile.radius() * 0.16), 0.0);
         DeferredTerrainDestruction.queueHemisphere(level, shapedCenter, profile.radius(), false, profile.maximumHardness(),
            targetTicks, predicate, callback, completion);
      } else {
         Vec3 shapedCenter = shape == Shape.SURFACE_HEMISPHERE ? center.add(0.0, profile.radius() * 0.3, 0.0) : center;
         DeferredTerrainDestruction.queueHemisphere(level, shapedCenter, profile.radius(), true, profile.maximumHardness(),
            targetTicks, predicate, callback, completion);
      }
      return true;
   }

   public static boolean impactWallTunnel(ServerLevel level, @Nullable LivingEntity source, Vec3 center,
                                          Vec3 direction, TerrainImpactProfile profile,
                                          double length, int width, int height) {
      return impactWallTunnel(level, source, center, direction, profile, permission(source), length, width, height);
   }

   public static boolean impactWallTunnel(ServerLevel level, @Nullable LivingEntity source, Vec3 center,
                                          Vec3 direction, TerrainImpactProfile profile, Permission permission,
                                          double length, int width, int height) {
      return impactDirectional(level, source, center, direction, profile, permission, length, width, height, true);
   }

   public static boolean impactForwardBreakthrough(ServerLevel level, @Nullable LivingEntity source, Vec3 center,
                                                   Vec3 direction, TerrainImpactProfile profile,
                                                   double length, int width, int height) {
      return impactForwardBreakthrough(level, source, center, direction, profile, permission(source), length, width, height);
   }

   public static boolean impactForwardBreakthrough(ServerLevel level, @Nullable LivingEntity source, Vec3 center,
                                                   Vec3 direction, TerrainImpactProfile profile, Permission permission,
                                                   double length, int width, int height) {
      return impactDirectional(level, source, center, direction, profile, permission, length, width, height, false);
   }

   private static boolean impactDirectional(ServerLevel level, @Nullable LivingEntity source, Vec3 center,
                                            Vec3 direction, TerrainImpactProfile profile,
                                            Permission permission, double length, int width, int height, boolean wallTunnel) {
      if (level == null || center == null || profile == null || direction == null
         || profile.tier() == TerrainImpactProfile.Tier.NONE || !allowed(level, permission)) return false;
      Vec3 horizontal = direction.multiply(1.0, 0.0, 1.0);
      if (horizontal.lengthSqr() < 1.0E-4) return false;
      horizontal = horizontal.normalize();
      if (isHeracles(source)) return impactHeraclesWall(level, source, center, horizontal, profile, permission);
      double cutLength = Math.max(1.0, length);
      double cutWidth = Math.max(1.0, width);
      double cutHeight = Math.max(1.0, height);
      Vec3 origin = center.add(horizontal.scale(wallTunnel ? 0.35 : 0.15))
         .add(0.0, wallTunnel ? 0.15 : 0.0, 0.0);
      DeferredTerrainDestruction.protectNextCombatJob();
      DeferredTerrainDestruction.queueDirectionalCut(level, origin, horizontal,
         cutLength, cutWidth, cutHeight, false, profile.maximumHardness());
      return impact(level, source, center, profile, permission, Shape.AIR_SPHERE);
   }

   private static boolean isHeracles(@Nullable LivingEntity source) {
      var definition = source == null ? null : net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper.definitionOf(source);
      return definition != null && "heracles".equals(definition.id());
   }

   /** Snapshot exactly one contact layer; delayed jobs and visual debris cannot dig below it. */
   public static boolean impactGroundLayer(ServerLevel level, LivingEntity source, Vec3 feet, TerrainImpactProfile profile, Permission permission) {
      if (!allowed(level, permission)) return false;
      int y = net.minecraft.util.Mth.floor(feet.y - .01);
      java.util.Map<BlockPos, BlockState> snapshot = new java.util.HashMap<>();
      int radius = (int)Math.ceil(profile.radius());
      BlockPos origin = BlockPos.containing(feet);
      for (int x=-radius;x<=radius;x++) for (int z=-radius;z<=radius;z++) {
         BlockPos pos=new BlockPos(origin.getX()+x,y,origin.getZ()+z);
         if (Vec3.atCenterOf(pos).subtract(feet).horizontalDistanceSqr() <= profile.radius()*profile.radius()) snapshot.put(pos,level.getBlockState(pos));
      }
      return queueSnapshot(level,source,new Vec3(feet.x,y+.5,feet.z),profile,snapshot,profile.radius()+1,true);
   }

   private static boolean impactHeraclesWall(ServerLevel level, LivingEntity source, Vec3 center, Vec3 forward,
                                             TerrainImpactProfile profile, Permission permission) {
      return impactHeraclesWallAt(level,source,source.position(),source.getBbHeight(),forward,profile,permission);
   }

   public static boolean impactHeraclesWallAt(ServerLevel level, LivingEntity source, Vec3 feet, double height, Vec3 direction,
                                                TerrainImpactProfile profile, Permission permission) {
      if (!allowed(level,permission)) return false;
      Vec3 forward=direction.multiply(1,0,1);
      if(forward.lengthSqr()<1e-6)return false;
      forward=forward.normalize();
      double footY=feet.y;
      Vec3 origin=feet;
      Vec3 side=new Vec3(-forward.z,0,forward.x);
      java.util.Map<BlockPos,BlockState> snapshot=new java.util.HashMap<>();
      // Bounds are captured now and enforced again by the queued snapshot predicate.
      for(int x=-5;x<=5;x++) for(int z=-5;z<=5;z++) for(int y=0;y<=Math.ceil(height);y++) {
         BlockPos pos=BlockPos.containing(origin).offset(x,y,z);
         Vec3 rel=Vec3.atCenterOf(pos).subtract(origin);
         double along=rel.dot(forward), across=rel.dot(side);
         if(along<0 || along>3 || Math.abs(across)>1.5 || pos.getY()<footY-1.0E-5 || pos.getY()>=footY+height) continue;
         snapshot.put(pos,level.getBlockState(pos));
      }
      return queueSnapshot(level,source,origin.add(forward.scale(1.5)).add(0,height*.5,0),profile,snapshot,6,false);
   }

   private static boolean queueSnapshot(ServerLevel level, LivingEntity source, Vec3 center, TerrainImpactProfile profile,
                                         java.util.Map<BlockPos,BlockState> snapshot, double scanRadius, boolean originalGroundLimit) {
      List<TerrainDebrisMessage.Sample> debris=new ArrayList<>();
      // Horizontal snapshots already exclude all blocks below the captured feet. A wall's
      // top must not be mistaken for original ground by the heightmap floor cache.
      if(originalGroundLimit)DeferredTerrainDestruction.protectNextCombatJob();
      DeferredTerrainDestruction.queueSphereDetailed(level,center,scanRadius,profile.maximumHardness(),3,
         (world,pos,distance,radius,origin)->snapshot.containsKey(pos) && world.getBlockState(pos).equals(snapshot.get(pos))
            && (!originalGroundLimit || CombatTerrainProtection.canRemove(world,pos))
            && !NeoForge.EVENT_BUS.post(new TerrainImpactBlockEvent(world,source,pos,world.getBlockState(pos),profile,"HERACLES_SNAPSHOT")).isCanceled(),
         (world,pos,state,removed)->addVisualSample(debris,profile,pos,state),
         ()->finishVisuals(level,center,profile,debris));
      return true;
   }

   @Nullable
   private static Integer selfFootMinimumY(@Nullable LivingEntity source, Vec3 center, TerrainImpactProfile profile, Shape shape) {
      if (source == null || shape == Shape.AIR_SPHERE || !profile.limitsSelfFootDepth()) return null;
      if (Config.protectCombatFooting) {
         int feetY = net.minecraft.util.Mth.floor(source.getY());
         // Surface impacts are body/wall contacts. Keep the block directly below
         // the actor intact; ground impacts use GROUND_LOWER_HEMISPHERE and retain
         // their footing-breaking behavior.
         return shape == Shape.UPPER_SURFACE_CRATER || shape == Shape.SURFACE_HEMISPHERE ? feetY : feetY - 1;
      }
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
      if (debris.size() < Math.min(96, profile.debrisCount())) {
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
      // Keep terrain impacts readable after the block shards fade, without
      // bringing back the former high-volume particle burst.
      int smoke = Math.min(8, Math.max(2, debris.size() / 6));
      level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, center.x, center.y + 0.2, center.z,
         smoke, Math.min(1.2, profile.radius() * 0.22), 0.1,
         Math.min(1.2, profile.radius() * 0.22), 0.028);

      TerrainDebrisMessage payload = new TerrainDebrisMessage(center, level.random.nextLong(), debris);
      double rangeSqr = 96.0 * 96.0;
      for (ServerPlayer player : level.players()) {
         if (player.distanceToSqr(center) <= rangeSqr) {
            ModNetwork.sendToPlayer(player, payload);
         }
      }
   }
}
