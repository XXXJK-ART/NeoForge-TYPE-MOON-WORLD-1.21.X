package net.xxxjk.TYPE_MOON_WORLD.client;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ObserverConcealmentClient {
   private static final Set<UUID> CONCEALED = ConcurrentHashMap.newKeySet();
   private static final Set<UUID> PERFECTLY_CONCEALED = ConcurrentHashMap.newKeySet();

   private ObserverConcealmentClient() {
   }

   public static void apply(UUID entityId, boolean concealed, boolean perfect) {
      if (entityId == null) return;
      if (concealed) {
         CONCEALED.add(entityId);
         if (perfect) PERFECTLY_CONCEALED.add(entityId);
         else PERFECTLY_CONCEALED.remove(entityId);
      } else {
         CONCEALED.remove(entityId);
         PERFECTLY_CONCEALED.remove(entityId);
      }
   }

   public static boolean isConcealed(UUID entityId) {
      return entityId != null && CONCEALED.contains(entityId);
   }

   public static boolean isPerfectlyConcealed(UUID entityId) {
      return entityId != null && PERFECTLY_CONCEALED.contains(entityId);
   }

   public static void clear() {
      CONCEALED.clear();
      PERFECTLY_CONCEALED.clear();
   }
}
