package net.xxxjk.TYPE_MOON_WORLD.servant.api;

@FunctionalInterface
public interface IServantCombatActionExecutor {
   ServantExecutionResult execute(ServantCombatActionContext context);
}
