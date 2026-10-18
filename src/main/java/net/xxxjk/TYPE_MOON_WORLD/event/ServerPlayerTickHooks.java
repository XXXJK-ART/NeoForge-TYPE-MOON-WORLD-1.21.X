package net.xxxjk.TYPE_MOON_WORLD.event;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * Small extension point for server-side player ticks.
 *
 * Addons register one callback instead of subscribing one event handler per
 * service.  The list is copy-on-write because registrations happen during
 * mod construction while ticks may already be running in an integrated
 * server during development reloads.
 */
public final class ServerPlayerTickHooks {
   private static final CopyOnWriteArrayList<Consumer<ServerPlayer>> HOOKS = new CopyOnWriteArrayList<>();
   private static final Logger LOGGER = LogUtils.getLogger();

   private ServerPlayerTickHooks() {
   }

   public static void register(Consumer<ServerPlayer> hook) {
      if (hook != null && !HOOKS.contains(hook)) {
         HOOKS.add(hook);
      }
   }

   public static void run(ServerPlayer player) {
      for (Consumer<ServerPlayer> hook : HOOKS) {
         try {
            hook.accept(player);
         } catch (RuntimeException exception) {
            LOGGER.error("Server player tick hook {} failed for {}", hook, player.getGameProfile().getName(), exception);
         }
      }
   }
}
