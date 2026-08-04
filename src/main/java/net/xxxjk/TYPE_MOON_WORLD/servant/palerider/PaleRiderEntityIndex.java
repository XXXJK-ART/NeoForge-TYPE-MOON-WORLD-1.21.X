package net.xxxjk.TYPE_MOON_WORLD.servant.palerider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;

public final class PaleRiderEntityIndex {
   private static final Map<ResourceKey<Level>, Map<UUID, Set<UUID>>> CONTROLLED_BY_DIMENSION = new HashMap<>();
   private static final Map<ResourceKey<Level>, Map<UUID, Set<UUID>>> OWNED_BY_DIMENSION = new HashMap<>();

   private PaleRiderEntityIndex() {
   }

   public static void registerOwned(UUID owner, Entity entity) {
      register(OWNED_BY_DIMENSION, owner, entity);
      registerControlled(owner, entity);
   }

   public static void unregisterOwned(UUID owner, Entity entity) {
      unregister(OWNED_BY_DIMENSION, owner, entity);
      unregisterControlled(owner, entity);
   }

   public static void registerControlled(UUID owner, Entity entity) {
      register(CONTROLLED_BY_DIMENSION, owner, entity);
   }

   public static void unregisterControlled(UUID owner, Entity entity) {
      unregister(CONTROLLED_BY_DIMENSION, owner, entity);
   }

   public static void clearLevel(ServerLevel level) {
      if (level == null) return;
      CONTROLLED_BY_DIMENSION.remove(level.dimension());
      OWNED_BY_DIMENSION.remove(level.dimension());
   }

   public static <T extends Entity> List<T> owned(ServerLevel level, UUID owner, Class<T> type, Predicate<T> predicate) {
      return resolve(level, owner, type, OWNED_BY_DIMENSION, predicate);
   }

   public static List<LivingEntity> controlled(ServerLevel level, UUID owner, Predicate<LivingEntity> predicate) {
      return resolve(level, owner, LivingEntity.class, CONTROLLED_BY_DIMENSION, predicate);
   }

   public static int controlledCount(ServerLevel level, UUID owner, Predicate<LivingEntity> predicate) {
      Map<UUID, Set<UUID>> index = CONTROLLED_BY_DIMENSION.get(level.dimension());
      Set<UUID> entityIds = index == null ? null : index.get(owner);
      if (entityIds == null || entityIds.isEmpty()) {
         return 0;
      }
      int count = 0;
      Iterator<UUID> iterator = entityIds.iterator();
      while (iterator.hasNext()) {
         Entity entity = level.getEntity(iterator.next());
         if (entity == null || !entity.isAlive()) {
            iterator.remove();
         } else if (entity instanceof LivingEntity living && (predicate == null || predicate.test(living))) {
            count++;
         }
      }
      if (entityIds.isEmpty()) {
         if (index != null) index.remove(owner);
         if (index != null && index.isEmpty()) CONTROLLED_BY_DIMENSION.remove(level.dimension());
      }
      return count;
   }

   private static void register(Map<ResourceKey<Level>, Map<UUID, Set<UUID>>> index, UUID owner, Entity entity) {
      if (owner == null || entity == null) {
         return;
      }
      index.computeIfAbsent(entity.level().dimension(), ignored -> new HashMap<>())
         .computeIfAbsent(owner, ignored -> new LinkedHashSet<>()).add(entity.getUUID());
   }

   private static void unregister(Map<ResourceKey<Level>, Map<UUID, Set<UUID>>> index, UUID owner, Entity entity) {
      if (owner == null || entity == null) {
         return;
      }
      Map<UUID, Set<UUID>> dimensionIndex = index.get(entity.level().dimension());
      if (dimensionIndex == null) return;
      Set<UUID> entities = dimensionIndex.get(owner);
      if (entities == null) {
         return;
      }
      entities.remove(entity.getUUID());
      if (entities.isEmpty()) {
         dimensionIndex.remove(owner);
         if (dimensionIndex.isEmpty()) index.remove(entity.level().dimension());
      }
   }

   private static <T extends Entity> List<T> resolve(
      ServerLevel level,
      UUID owner,
      Class<T> type,
       Map<ResourceKey<Level>, Map<UUID, Set<UUID>>> index,
       Predicate<T> predicate
   ) {
      Map<UUID, Set<UUID>> dimensionIndex = index.get(level.dimension());
      Set<UUID> entityIds = dimensionIndex == null ? null : dimensionIndex.get(owner);
      if (entityIds == null || entityIds.isEmpty()) {
         return List.of();
      }
      List<T> result = new ArrayList<>();
      Iterator<UUID> iterator = entityIds.iterator();
      while (iterator.hasNext()) {
         Entity entity = level.getEntity(iterator.next());
         if (entity == null || !entity.isAlive()) {
            iterator.remove();
            continue;
         }
         if (type.isInstance(entity)) {
            T typed = type.cast(entity);
            if (predicate == null || predicate.test(typed)) {
               result.add(typed);
            }
         }
      }
      if (entityIds.isEmpty()) {
         if (dimensionIndex != null) dimensionIndex.remove(owner);
         if (dimensionIndex != null && dimensionIndex.isEmpty()) index.remove(level.dimension());
      }
      return result;
   }
}
