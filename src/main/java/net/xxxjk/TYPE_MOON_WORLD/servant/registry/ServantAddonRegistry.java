package net.xxxjk.TYPE_MOON_WORLD.servant.registry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.IServantAddonEntrypoint;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.IServantAddonRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.IServantCombatActionExecutor;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.IServantLifecycleHandler;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.IServantNoblePhantasmExecutor;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantCombatActionContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionResult;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantLifecycleContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantNoblePhantasmContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;

public final class ServantAddonRegistry implements IServantAddonRegistry {
   private static final ServantAddonRegistry INSTANCE = new ServantAddonRegistry();
   private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
   private static final Map<String, RegisteredDefinition> DEFINITIONS = new ConcurrentHashMap<>();
   private static final Map<String, RegisteredCombatAction> COMBAT_ACTIONS = new ConcurrentHashMap<>();
   private static final Map<String, RegisteredNoblePhantasm> NOBLE_PHANTASMS = new ConcurrentHashMap<>();
   private static final Map<String, RegisteredLifecycleHandler> LIFECYCLE_HANDLERS = new ConcurrentHashMap<>();

   private ServantAddonRegistry() {
   }

   public static void ensureInitialized() {
      if (INITIALIZED.compareAndSet(false, true)) {
         loadAddonEntrypoints();
      }
   }

   public static Map<String, ServantDefinition> addonDefinitions() {
      ensureInitialized();
      Map<String, ServantDefinition> copy = new LinkedHashMap<>();
      DEFINITIONS.entrySet().stream()
         .sorted(Map.Entry.comparingByKey())
         .forEach(entry -> copy.put(entry.getKey(), entry.getValue().definition));
      return copy;
   }

   public static void addAddonDefinitions(Map<String, ServantDefinition> target) {
      ensureInitialized();
      for (RegisteredDefinition registered : DEFINITIONS.values()) {
         ServantDefinition previous = target.putIfAbsent(registered.definition.id(), registered.definition);
         if (previous != null) {
            TYPE_MOON_WORLD.LOGGER.warn(
               "Addon servant definition ignored because a datapack/core definition already exists. servantId={}, provider={}",
               registered.definition.id(),
               registered.providerId
            );
         }
      }
   }

   public static ServantExecutionResult executeCombatAction(ServantCombatActionContext context) {
      ensureInitialized();
      if (context == null || context.actionId() == null || context.actionId().isBlank()) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      RegisteredCombatAction registered = COMBAT_ACTIONS.get(context.actionId());
      if (registered == null || registered.executor == null) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      try {
         ServantExecutionResult result = registered.executor.execute(context);
         return result == null ? ServantExecutionResult.FAILED : result;
      } catch (Exception e) {
         TYPE_MOON_WORLD.LOGGER.error(
            "Servant combat action failed. actionId={}, provider={}, servant={}",
            context.actionId(),
            registered.providerId,
            context.caster() != null ? context.caster().getServantId() : "unknown",
            e
         );
         return ServantExecutionResult.FAILED;
      }
   }

   public static ServantExecutionResult runFirstHandledCombatAction(
      Set<String> actionIds,
      java.util.function.Function<String, ServantCombatActionContext> contextFactory
   ) {
      ensureInitialized();
      if (actionIds == null || actionIds.isEmpty() || contextFactory == null) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      for (String actionId : actionIds) {
         if (!COMBAT_ACTIONS.containsKey(actionId)) {
            continue;
         }
         ServantExecutionResult result = executeCombatAction(contextFactory.apply(actionId));
         if (result.handled()) {
            return result;
         }
      }
      return ServantExecutionResult.NOT_HANDLED;
   }

   public static ServantExecutionResult runLifecycleHandlers(ServantLifecycleContext context) {
      ensureInitialized();
      if (context == null || LIFECYCLE_HANDLERS.isEmpty()) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      List<RegisteredLifecycleHandler> handlers = new ArrayList<>(LIFECYCLE_HANDLERS.values());
      handlers.sort(Comparator.comparing(RegisteredLifecycleHandler::handlerId));
      for (RegisteredLifecycleHandler registered : handlers) {
         try {
            ServantExecutionResult result = registered.handler.tick(context);
            if (result != null && result.handled()) {
               return result;
            }
         } catch (Exception e) {
            TYPE_MOON_WORLD.LOGGER.error(
               "Servant lifecycle handler failed. handlerId={}, provider={}, servant={}",
               registered.handlerId,
               registered.providerId,
               context.entity() != null ? context.entity().getServantId() : "unknown",
               e
            );
            return ServantExecutionResult.FAILED;
         }
      }
      return ServantExecutionResult.NOT_HANDLED;
   }

   public static ServantExecutionResult executeNoblePhantasm(ServantNoblePhantasmContext context) {
      ensureInitialized();
      if (context == null || context.noblePhantasmDefinition() == null || context.noblePhantasmDefinition().id() == null) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      RegisteredNoblePhantasm registered = NOBLE_PHANTASMS.get(context.noblePhantasmDefinition().id());
      if (registered == null || registered.executor == null) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      try {
         ServantExecutionResult result = registered.executor.execute(context);
         return result == null ? ServantExecutionResult.FAILED : result;
      } catch (Exception e) {
         TYPE_MOON_WORLD.LOGGER.error(
            "Servant noble phantasm failed. noblePhantasmId={}, provider={}, servant={}",
            context.noblePhantasmDefinition().id(),
            registered.providerId,
            context.caster() != null ? context.caster().getServantId() : "unknown",
            e
         );
         return ServantExecutionResult.FAILED;
      }
   }

   public static Set<String> registeredCombatActions() {
      ensureInitialized();
      return Set.copyOf(COMBAT_ACTIONS.keySet());
   }

   public static Set<String> registeredLifecycleHandlers() {
      ensureInitialized();
      return Set.copyOf(LIFECYCLE_HANDLERS.keySet());
   }

   public static Set<String> registeredNoblePhantasms() {
      ensureInitialized();
      return Set.copyOf(NOBLE_PHANTASMS.keySet());
   }

   @Override
   public boolean registerDefinition(ServantDefinition definition, String providerId) {
      if (definition == null || !isValidId(definition.id())) {
         return false;
      }
      String provider = normalizeProvider(providerId);
      RegisteredDefinition previous = DEFINITIONS.putIfAbsent(definition.id(), new RegisteredDefinition(definition, provider));
      if (previous != null) {
         TYPE_MOON_WORLD.LOGGER.warn(
            "Duplicate addon servant definition ignored. servantId={}, existingProvider={}, newProvider={}",
            definition.id(),
            previous.providerId,
            provider
         );
         return false;
      }
      TYPE_MOON_WORLD.LOGGER.debug("Registered addon servant definition: {} ({})", definition.id(), provider);
      return true;
   }

   @Override
   public boolean registerCombatAction(String actionId, IServantCombatActionExecutor executor, String providerId) {
      if (!isValidId(actionId) || executor == null) {
         return false;
      }
      String provider = normalizeProvider(providerId);
      RegisteredCombatAction previous = COMBAT_ACTIONS.putIfAbsent(actionId, new RegisteredCombatAction(actionId, executor, provider));
      if (previous != null) {
         TYPE_MOON_WORLD.LOGGER.warn(
            "Duplicate servant combat action ignored. actionId={}, existingProvider={}, newProvider={}",
            actionId,
            previous.providerId,
            provider
         );
         return false;
      }
      TYPE_MOON_WORLD.LOGGER.debug("Registered servant combat action: {} ({})", actionId, provider);
      return true;
   }

   @Override
   public boolean registerNoblePhantasm(String noblePhantasmId, IServantNoblePhantasmExecutor executor, String providerId) {
      if (!isValidId(noblePhantasmId) || executor == null) {
         return false;
      }
      String provider = normalizeProvider(providerId);
      RegisteredNoblePhantasm previous = NOBLE_PHANTASMS.putIfAbsent(noblePhantasmId, new RegisteredNoblePhantasm(noblePhantasmId, executor, provider));
      if (previous != null) {
         TYPE_MOON_WORLD.LOGGER.warn(
            "Duplicate servant noble phantasm ignored. noblePhantasmId={}, existingProvider={}, newProvider={}",
            noblePhantasmId,
            previous.providerId,
            provider
         );
         return false;
      }
      TYPE_MOON_WORLD.LOGGER.debug("Registered servant noble phantasm: {} ({})", noblePhantasmId, provider);
      return true;
   }

   @Override
   public boolean registerLifecycleHandler(String handlerId, IServantLifecycleHandler handler, String providerId) {
      if (!isValidId(handlerId) || handler == null) {
         return false;
      }
      String provider = normalizeProvider(providerId);
      RegisteredLifecycleHandler previous = LIFECYCLE_HANDLERS.putIfAbsent(handlerId, new RegisteredLifecycleHandler(handlerId, handler, provider));
      if (previous != null) {
         TYPE_MOON_WORLD.LOGGER.warn(
            "Duplicate servant lifecycle handler ignored. handlerId={}, existingProvider={}, newProvider={}",
            handlerId,
            previous.providerId,
            provider
         );
         return false;
      }
      TYPE_MOON_WORLD.LOGGER.debug("Registered servant lifecycle handler: {} ({})", handlerId, provider);
      return true;
   }

   private static void loadAddonEntrypoints() {
      for (IServantAddonEntrypoint entrypoint : ServiceLoader.load(IServantAddonEntrypoint.class)) {
         if (entrypoint == null) {
            continue;
         }
         String provider = entrypoint.providerId();
         try {
            entrypoint.registerServants(INSTANCE);
            TYPE_MOON_WORLD.LOGGER.info("Loaded servant addon extension entrypoint: {}", provider);
         } catch (Exception e) {
            TYPE_MOON_WORLD.LOGGER.error("Failed to load servant addon extension entrypoint: {}", provider, e);
         }
      }
   }

   private static boolean isValidId(String id) {
      return id != null && !id.isBlank() && id.matches("[a-z0-9_]+");
   }

   private static String normalizeProvider(String providerId) {
      return providerId != null && !providerId.isBlank() ? providerId : "unknown";
   }

   private record RegisteredDefinition(ServantDefinition definition, String providerId) {
   }

   private record RegisteredCombatAction(String actionId, IServantCombatActionExecutor executor, String providerId) {
   }

   private record RegisteredNoblePhantasm(String noblePhantasmId, IServantNoblePhantasmExecutor executor, String providerId) {
   }

   private record RegisteredLifecycleHandler(String handlerId, IServantLifecycleHandler handler, String providerId) {
   }
}
