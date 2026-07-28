package net.xxxjk.TYPE_MOON_WORLD.combat.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works.UBWInstanceManager;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public final class BattlefieldAreaService {
   public enum Type { REALITY_MARBLE, WORKSHOP, BOUNDED_FIELD, CONTINUOUS_NOBLE_PHANTASM }
   private static final Map<ResourceKey<Level>, List<Area>> AREAS = new HashMap<>();

   private BattlefieldAreaService() { }

   public static void register(ServerLevel level, UUID owner, Type type, Vec3 center, double radius, long expiresAt) {
      if (level == null || owner == null || type == null || center == null || radius <= 0.0) return;
      List<Area> areas = AREAS.computeIfAbsent(level.dimension(), ignored -> new ArrayList<>());
      areas.removeIf(area -> area.owner().equals(owner) && area.type() == type);
      areas.add(new Area(owner, type, center, radius, expiresAt));
   }

   public static void unregister(ServerLevel level, UUID owner, Type type) {
      List<Area> areas = level == null ? null : AREAS.get(level.dimension());
      if (areas != null) areas.removeIf(area -> area.owner().equals(owner) && area.type() == type);
   }

   public static List<Area> at(ServerLevel level, Vec3 point) {
      long now = level.getGameTime();
      List<Area> result = new ArrayList<>();
      List<Area> areas = AREAS.get(level.dimension());
      if (areas != null) for (Area area : areas) if (area.expiresAt() >= now && area.contains(point)) result.add(area);
      if (UBWInstanceManager.isUbwDimension(level)) {
         UUID owner = UBWInstanceManager.getOwnerId(level.dimension());
         if (owner != null) result.add(new Area(owner, Type.REALITY_MARBLE, point, Double.MAX_VALUE, Long.MAX_VALUE));
      }
      return result;
   }

   public static boolean isHostileArea(LivingEntity observer, Vec3 point) {
      if (!(observer.level() instanceof ServerLevel level)) return false;
      for (Area area : at(level, point)) {
         Entity owner = level.getEntity(area.owner());
         if (!(owner instanceof LivingEntity living) || !observer.isAlliedTo(living)) return true;
      }
      return false;
   }

   @SubscribeEvent
   public static void tick(LevelTickEvent.Post event) {
      if (!(event.getLevel() instanceof ServerLevel level)) return;
      List<Area> areas = AREAS.get(level.dimension());
      if (areas != null) {
         long now = level.getGameTime();
         areas.removeIf(area -> area.expiresAt() < now);
         if (areas.isEmpty()) AREAS.remove(level.dimension());
      }
   }

   @SubscribeEvent
   public static void unload(LevelEvent.Unload event) {
      if (event.getLevel() instanceof Level level && !level.isClientSide()) AREAS.remove(level.dimension());
   }

   public record Area(UUID owner, Type type, Vec3 center, double radius, long expiresAt) {
      public boolean contains(Vec3 point) { return radius == Double.MAX_VALUE || center.distanceToSqr(point) <= radius * radius; }
   }
}
