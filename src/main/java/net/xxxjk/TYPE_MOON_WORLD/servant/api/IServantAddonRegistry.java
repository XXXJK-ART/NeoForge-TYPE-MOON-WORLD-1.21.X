package net.xxxjk.TYPE_MOON_WORLD.servant.api;

import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;

public interface IServantAddonRegistry {
   boolean registerDefinition(ServantDefinition definition, String providerId);

   boolean registerCombatAction(String actionId, IServantCombatActionExecutor executor, String providerId);

   boolean registerNoblePhantasm(String noblePhantasmId, IServantNoblePhantasmExecutor executor, String providerId);

   boolean registerLifecycleHandler(String handlerId, IServantLifecycleHandler handler, String providerId);

   boolean registerEntityFactory(String servantId, IServantEntityFactory factory, String providerId);

   default boolean registerDefinition(ServantDefinition definition) {
      return this.registerDefinition(definition, "unknown");
   }

   default boolean registerCombatAction(String actionId, IServantCombatActionExecutor executor) {
      return this.registerCombatAction(actionId, executor, "unknown");
   }

   default boolean registerNoblePhantasm(String noblePhantasmId, IServantNoblePhantasmExecutor executor) {
      return this.registerNoblePhantasm(noblePhantasmId, executor, "unknown");
   }

   default boolean registerLifecycleHandler(String handlerId, IServantLifecycleHandler handler) {
      return this.registerLifecycleHandler(handlerId, handler, "unknown");
   }

   default boolean registerEntityFactory(String servantId, IServantEntityFactory factory) {
      return this.registerEntityFactory(servantId, factory, "unknown");
   }
}
