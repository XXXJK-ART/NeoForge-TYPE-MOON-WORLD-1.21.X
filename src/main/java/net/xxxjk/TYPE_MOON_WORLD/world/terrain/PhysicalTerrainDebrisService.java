package net.xxxjk.TYPE_MOON_WORLD.world.terrain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.Config;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

/** Server-side, bounded falling blocks created from terrain that was actually removed. */
@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public final class PhysicalTerrainDebrisService {
   private static final String DEBRIS_TAG = "TypeMoonPhysicalTerrainDebris";
   private static final String EXPIRES_TAG = "TypeMoonPhysicalTerrainDebrisExpires";
   private static final int MAX_LIFETIME_TICKS = 80;
   private static final Map<ResourceKey<Level>, Set<UUID>> ACTIVE = new HashMap<>();

   private PhysicalTerrainDebrisService() { }

   public static boolean launch(ServerLevel level, net.minecraft.core.BlockPos pos, BlockState state, Vec3 center,
                                TerrainImpactProfile.Tier tier) {
      if (level == null || pos == null || state == null || center == null || !Config.physicalTerrainDebrisEnabled
         || Config.maxPhysicalTerrainDebris <= 0 || state.isAir() || level.getBlockEntity(pos) != null) return false;
      Set<UUID> active = ACTIVE.computeIfAbsent(level.dimension(), ignored -> new HashSet<>());
      active.removeIf(id -> level.getEntity(id) == null);
      if (active.size() >= Config.maxPhysicalTerrainDebris) return false;

      FallingBlockEntity debris = FallingBlockEntity.fall(level, pos, state);
      debris.dropItem = false;
      float hurtAmount = tier == TerrainImpactProfile.Tier.NP ? 1.0F
         : tier == TerrainImpactProfile.Tier.HEAVY ? 0.65F : 0.35F;
      int hurtMaximum = tier == TerrainImpactProfile.Tier.NP ? 20
         : tier == TerrainImpactProfile.Tier.HEAVY ? 14 : 8;
      debris.setHurtsEntities(hurtAmount, hurtMaximum);
      debris.getPersistentData().putBoolean(DEBRIS_TAG, true);
      debris.getPersistentData().putLong(EXPIRES_TAG, level.getGameTime() + MAX_LIFETIME_TICKS);

      Vec3 outward = Vec3.atCenterOf(pos).subtract(center);
      Vec3 horizontal = new Vec3(outward.x, 0.0, outward.z);
      if (horizontal.lengthSqr() < 1.0E-4) {
         horizontal = new Vec3(level.random.nextDouble() - 0.5, 0.0, level.random.nextDouble() - 0.5);
      }
      horizontal = horizontal.normalize();
      double force = tier == TerrainImpactProfile.Tier.NP ? 1.0
         : tier == TerrainImpactProfile.Tier.HEAVY ? 0.82 : 0.62;
      double sideways = (0.16 + level.random.nextDouble() * 0.24) * force;
      double upward = (0.42 + level.random.nextDouble() * 0.34) * force;
      debris.setDeltaMovement(horizontal.scale(sideways).add(
         (level.random.nextDouble() - 0.5) * 0.12, upward, (level.random.nextDouble() - 0.5) * 0.12));
      debris.hurtMarked = true;
      active.add(debris.getUUID());
      return true;
   }

   public static boolean isPhysicalDebris(Entity entity) {
      return entity instanceof FallingBlockEntity && entity.getPersistentData().getBoolean(DEBRIS_TAG);
   }

   public static int maxActivePerDimension() {
      return Math.max(0, Config.maxPhysicalTerrainDebris);
   }

   @SubscribeEvent
   public static void tick(EntityTickEvent.Post event) {
      Entity entity = event.getEntity();
      if (!isPhysicalDebris(entity) || !(entity.level() instanceof ServerLevel level)) return;
      Set<UUID> active = ACTIVE.computeIfAbsent(level.dimension(), ignored -> new HashSet<>());
      if (!entity.isAlive()) {
         active.remove(entity.getUUID());
         return;
      }
      if (!active.contains(entity.getUUID()) && active.size() >= Config.maxPhysicalTerrainDebris) {
         entity.discard();
         return;
      }
      active.add(entity.getUUID());
      if (level.getGameTime() >= entity.getPersistentData().getLong(EXPIRES_TAG)) entity.discard();
   }

   @SubscribeEvent
   public static void leave(EntityLeaveLevelEvent event) {
      if (!isPhysicalDebris(event.getEntity())) return;
      Set<UUID> active = ACTIVE.get(event.getLevel().dimension());
      if (active != null) active.remove(event.getEntity().getUUID());
   }

   @SubscribeEvent
   public static void unload(LevelEvent.Unload event) {
      if (event.getLevel() instanceof Level level && !level.isClientSide()) ACTIVE.remove(level.dimension());
   }
}
