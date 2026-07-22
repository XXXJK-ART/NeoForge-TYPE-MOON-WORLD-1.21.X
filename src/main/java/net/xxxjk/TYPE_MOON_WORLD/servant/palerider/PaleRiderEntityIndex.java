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

public final class PaleRiderEntityIndex {
   private static final Map<UUID, Set<UUID>> CONTROLLED_BY_OWNER = new HashMap<>();
   private static final Map<UUID, Set<UUID>> OWNED_BY_OWNER = new HashMap<>();

   private PaleRiderEntityIndex() {
   }

   public static void registerOwned(UUID owner, Entity entity) {
      register(OWNED_BY_OWNER, owner, entity);
      registerControlled(owner, entity);
   }

   public static void unregisterOwned(UUID owner, Entity entity) {
      unregister(OWNED_BY_OWNER, owner, entity == null ? null : entity.getUUID());
      unregisterControlled(owner, entity);
   }

   public static void registerControlled(UUID owner, Entity entity) {
      register(CONTROLLED_BY_OWNER, owner, entity);
   }

   public static void unregisterControlled(UUID owner, Entity entity) {
      unregister(CONTROLLED_BY_OWNER, owner, entity == null ? null : entity.getUUID());
   }

   public static <T extends Entity> List<T> owned(ServerLevel level, UUID owner, Class<T> type, Predicate<T> predicate) {
      return resolve(level, owner, type, OWNED_BY_OWNER, predicate);
   }

   public static List<LivingEntity> controlled(ServerLevel level, UUID owner, Predicate<LivingEntity> predicate) {
      return resolve(level, owner, LivingEntity.class, CONTROLLED_BY_OWNER, predicate);
   }

   public static int controlledCount(ServerLevel level, UUID owner, Predicate<LivingEntity> predicate) {
      return controlled(level, owner, predicate).size();
   }

   private static void register(Map<UUID, Set<UUID>> index, UUID owner, Entity entity) {
      if (owner == null || entity == null) {
         return;
      }
      index.computeIfAbsent(owner, ignored -> new LinkedHashSet<>()).add(entity.getUUID());
   }

   private static void unregister(Map<UUID, Set<UUID>> index, UUID owner, UUID entityId) {
      if (owner == null || entityId == null) {
         return;
      }
      Set<UUID> entities = index.get(owner);
      if (entities == null) {
         return;
      }
      entities.remove(entityId);
      if (entities.isEmpty()) {
         index.remove(owner);
      }
   }

   private static <T extends Entity> List<T> resolve(
      ServerLevel level,
      UUID owner,
      Class<T> type,
      Map<UUID, Set<UUID>> index,
      Predicate<T> predicate
   ) {
      Set<UUID> entityIds = index.get(owner);
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
         index.remove(owner);
      }
      return result;
   }
}
