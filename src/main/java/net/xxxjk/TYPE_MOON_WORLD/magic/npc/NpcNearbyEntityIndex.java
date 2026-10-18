package net.xxxjk.TYPE_MOON_WORLD.magic.npc;

import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;

/**
 * Shared, short lived nearby-mob index for NPC combat decisions.
 *
 * The old implementation performed one entity query per magician. Entries
 * are grouped into 32x32 block regions and are valid for five server ticks,
 * which keeps the result suitable for threat scoring while sharing the scan
 * between all magicians in the same region.
 */
public final class NpcNearbyEntityIndex {
   private static final Map<Level, Cache> CACHES = new WeakHashMap<>();

   private NpcNearbyEntityIndex() {
   }

   public static Iterable<Mob> mobs(MysticMagicianEntity npc, double radius) {
      if (npc == null || npc.level() == null) {
         return java.util.List.of();
      }
      Level level = npc.level();
      long now = level.getGameTime();
      Cache cache = CACHES.computeIfAbsent(level, ignored -> new Cache());
      if (cache.tick < 0L || now - cache.tick >= 5L) {
         cache.tick = now;
         cache.regions.clear();
      }
      int regionX = Mth.floor(npc.getX() / 32.0D);
      int regionZ = Mth.floor(npc.getZ() / 32.0D);
      long key = (regionX * 341873128712L) ^ (regionZ * 132897987541L);
      return cache.regions.computeIfAbsent(key, ignored -> {
         double cx = regionX * 32.0D + 16.0D;
         double cz = regionZ * 32.0D + 16.0D;
         double scanRadius = Math.max(32.0D, radius + 16.0D);
         AABB box = new AABB(cx - scanRadius, npc.getY() - 24.0D, cz - scanRadius,
            cx + scanRadius, npc.getY() + 24.0D, cz + scanRadius);
         return new ArrayList<>(level.getEntitiesOfClass(Mob.class, box,
            candidate -> candidate.isAlive()));
      });
   }

   private static final class Cache {
      private long tick = -1L;
      private final Map<Long, ArrayList<Mob>> regions = new java.util.HashMap<>();
   }
}
