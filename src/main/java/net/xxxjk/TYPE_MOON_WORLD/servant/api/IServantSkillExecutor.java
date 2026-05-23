package net.xxxjk.TYPE_MOON_WORLD.servant.api;

@FunctionalInterface
public interface IServantSkillExecutor {
   ServantExecutionResult execute(ServantExecutionContext context);
}
