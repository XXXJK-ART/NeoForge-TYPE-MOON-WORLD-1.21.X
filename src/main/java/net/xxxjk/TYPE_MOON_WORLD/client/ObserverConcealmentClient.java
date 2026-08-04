package net.xxxjk.TYPE_MOON_WORLD.client;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ObserverConcealmentClient {
   private static final Set<UUID> CONCEALED = ConcurrentHashMap.newKeySet();

   private ObserverConcealmentClient() {
   }

   public static void apply(UUID entityId, boolean concealed) {
      if (entityId == null) return;
      if (concealed) CONCEALED.add(entityId);
      else CONCEALED.remove(entityId);
   }

   public static boolean isConcealed(UUID entityId) {
      return entityId != null && CONCEALED.contains(entityId);
   }

   public static void clear() {
      CONCEALED.clear();
   }
}
