package net.xxxjk.TYPE_MOON_WORLD.servant.api;

@FunctionalInterface
public interface IServantLifecycleHandler {
   ServantExecutionResult tick(ServantLifecycleContext context);
}
