package net.xxxjk.typemoonworld.api;

import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;

/** Optional addon boundary for external infinite-mana sources. */
public final class AddonManaSource {
   private static volatile Predicate<ServerPlayer> provider = player -> false;

   private AddonManaSource() {
   }

   public static void register(Predicate<ServerPlayer> predicate) {
      provider = Objects.requireNonNullElse(predicate, player -> false);
   }

   public static boolean hasInfiniteSupply(ServerPlayer player) {
      return player != null && provider.test(player);
   }
}
