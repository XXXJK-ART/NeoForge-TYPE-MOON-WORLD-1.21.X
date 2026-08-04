package net.xxxjk.TYPE_MOON_WORLD.servant.skill;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.IServantAddonEntrypoint;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.IServantSkillExecutor;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.IServantSkillRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionResult;

public final class ServantSkillRegistry implements IServantSkillRegistry {
   private static final Map<String, RegisteredSkill> REGISTRY = new ConcurrentHashMap<>();
   private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
   private static final AtomicBoolean FROZEN = new AtomicBoolean(false);
   private static final ServantSkillRegistry INSTANCE = new ServantSkillRegistry();

   private ServantSkillRegistry() {
   }

   public static void ensureInitialized() {
      if (INITIALIZED.compareAndSet(false, true)) {
         CommonServantSkills.registerBuiltin(INSTANCE);
         CuChulainnServantSkills.registerBuiltin(INSTANCE);
         HeraclesServantSkills.registerBuiltin(INSTANCE);
         CasterGilgameshServantSkills.registerBuiltin(INSTANCE);
         SasakiKojiroServantSkills.registerBuiltin(INSTANCE);
         ParacelsusServantSkills.registerBuiltin(INSTANCE);
         MuramasaServantSkills.registerBuiltin(INSTANCE);
         loadAddonEntrypoints();
      }
   }

   public static void freeze() {
      ensureInitialized();
      FROZEN.set(true);
   }

   public static boolean registerExternal(String skillId, IServantSkillExecutor executor, String providerId) {
      ensureInitialized();
      return INSTANCE.register(skillId, executor, providerId);
   }

   public static ServantExecutionResult execute(String skillId, ServantExecutionContext context) {
      if (context == null || skillId == null || skillId.isEmpty()) {
         return ServantExecutionResult.NOT_HANDLED;
      }

      RegisteredSkill registered = REGISTRY.get(skillId);
      if (registered == null || registered.executor == null) {
         return ServantExecutionResult.NOT_HANDLED;
      }

      try {
         ServantExecutionResult result = registered.executor.execute(context);
         return result == null ? ServantExecutionResult.FAILED : result;
      } catch (Exception e) {
         TYPE_MOON_WORLD.LOGGER.error("Servant skill executor failed. skillId={}, provider={}", skillId, registered.providerId, e);
         return ServantExecutionResult.FAILED;
      }
   }

   public static Set<String> registeredSkillIds() {
      return Set.copyOf(REGISTRY.keySet());
   }

   @Override
   public boolean register(String skillId, IServantSkillExecutor executor, String providerId) {
      if (FROZEN.get() || !isValidSkillId(skillId) || executor == null) {
         return false;
      }

      String normalizedProvider = providerId != null && !providerId.isBlank() ? providerId : "unknown";
      RegisteredSkill previous = REGISTRY.putIfAbsent(skillId, new RegisteredSkill(executor, normalizedProvider));
      if (previous != null) {
         TYPE_MOON_WORLD.LOGGER.warn(
            "Duplicate servant skill registration ignored. skillId={}, existingProvider={}, newProvider={}",
            skillId, previous.providerId, normalizedProvider
         );
         return false;
      }

      TYPE_MOON_WORLD.LOGGER.debug("Registered servant skill executor: {} ({})", skillId, normalizedProvider);
      return true;
   }

   @Override
   public boolean unregister(String skillId, String providerId) {
      if (!isValidSkillId(skillId)) {
         return false;
      }

      RegisteredSkill current = REGISTRY.get(skillId);
      if (current == null) {
         return false;
      }

      if (providerId != null && !providerId.isBlank() && !providerId.equals(current.providerId)) {
         return false;
      }

      return REGISTRY.remove(skillId, current);
   }

   private static boolean isValidSkillId(String skillId) {
      return skillId != null && !skillId.isEmpty() && (skillId.matches("[a-z0-9_]+") || ResourceLocation.tryParse(skillId) != null);
   }

   private static void loadAddonEntrypoints() {
      for (IServantAddonEntrypoint entrypoint : ServiceLoader.load(IServantAddonEntrypoint.class)) {
         if (entrypoint != null) {
            String provider = entrypoint.providerId();
            try {
               entrypoint.registerSkills(INSTANCE);
               TYPE_MOON_WORLD.LOGGER.info("Loaded servant addon entrypoint: {}", provider);
            } catch (Exception e) {
               TYPE_MOON_WORLD.LOGGER.error("Failed to load servant addon entrypoint: {}", provider, e);
            }
         }
      }
   }

   private record RegisteredSkill(IServantSkillExecutor executor, String providerId) {
   }
}
