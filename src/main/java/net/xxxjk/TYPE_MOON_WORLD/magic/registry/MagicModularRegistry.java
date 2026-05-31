package net.xxxjk.TYPE_MOON_WORLD.magic.registry;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.IMagicAddonEntrypoint;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.IMagicExecutor;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.IMagicRegistry;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionResult;

public final class MagicModularRegistry implements IMagicRegistry {
   private static final Map<String, MagicModularRegistry.RegisteredMagic> REGISTRY = new ConcurrentHashMap<>();
   private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
   private static final MagicModularRegistry INSTANCE = new MagicModularRegistry();

   private MagicModularRegistry() {
   }

   public static void ensureInitialized() {
      if (INITIALIZED.compareAndSet(false, true)) {
         BuiltinMagicExecutors.registerBuiltin(INSTANCE);
         loadAddonEntrypoints();
      }
   }

   public static MagicExecutionResult execute(MagicExecutionContext context) {
      if (context != null && context.magicId() != null && !context.magicId().isEmpty()) {
         MagicModularRegistry.RegisteredMagic registered = REGISTRY.get(context.magicId());
         if (registered != null && registered.executor != null) {
            try {
               MagicExecutionResult result = registered.executor.execute(context);
               return result == null ? MagicExecutionResult.FAILED : result;
            } catch (Exception var3) {
               TYPE_MOON_WORLD.LOGGER.error("Magic executor failed. magicId={}, provider={}", context.magicId(), registered.providerId, var3);
               return MagicExecutionResult.FAILED;
            }
         } else {
            return MagicExecutionResult.NOT_HANDLED;
         }
      } else {
         return MagicExecutionResult.NOT_HANDLED;
      }
   }

   public static Set<String> registeredMagicIds() {
      return Set.copyOf(REGISTRY.keySet());
   }

   public static boolean registerExternal(String magicId, IMagicExecutor executor, String providerId) {
      ensureInitialized();
      return INSTANCE.register(magicId, executor, providerId);
   }

   public static boolean unregisterExternal(String magicId, String providerId) {
      ensureInitialized();
      return INSTANCE.unregister(magicId, providerId);
   }

   @Override
   public boolean register(String magicId, IMagicExecutor executor, String providerId) {
      if (isValidMagicId(magicId) && executor != null) {
         String normalizedProvider = providerId != null && !providerId.isBlank() ? providerId : "unknown";
         MagicModularRegistry.RegisteredMagic previous = REGISTRY.putIfAbsent(magicId, new MagicModularRegistry.RegisteredMagic(executor, normalizedProvider));
         if (previous != null) {
            TYPE_MOON_WORLD.LOGGER
               .warn(
                  "Duplicate magic registration ignored. magicId={}, existingProvider={}, newProvider={}",
                  magicId, previous.providerId, normalizedProvider
               );
            return false;
         } else {
            TYPE_MOON_WORLD.LOGGER.debug("Registered magic executor: {} ({})", magicId, normalizedProvider);
            return true;
         }
      } else {
         return false;
      }
   }

   @Override
   public boolean unregister(String magicId, String providerId) {
      if (!isValidMagicId(magicId)) {
         return false;
      } else {
         MagicModularRegistry.RegisteredMagic current = REGISTRY.get(magicId);
         if (current == null) {
            return false;
         } else {
            return providerId != null && !providerId.isBlank() && !providerId.equals(current.providerId) ? false : REGISTRY.remove(magicId, current);
         }
      }
   }

   private static boolean isValidMagicId(String magicId) {
      return magicId != null && !magicId.isEmpty() && magicId.matches("[a-z0-9_]+");
   }

   private static void loadAddonEntrypoints() {
      for (IMagicAddonEntrypoint entrypoint : ServiceLoader.load(IMagicAddonEntrypoint.class)) {
         if (entrypoint != null) {
            String provider = entrypoint.providerId();

            try {
               entrypoint.registerMagics(INSTANCE);
               TYPE_MOON_WORLD.LOGGER.info("Loaded magic addon entrypoint: {}", provider);
            } catch (Exception var5) {
               TYPE_MOON_WORLD.LOGGER.error("Failed to load magic addon entrypoint: {}", provider, var5);
            }
         }
      }
   }

   private record RegisteredMagic(IMagicExecutor executor, String providerId) {
   }
}
