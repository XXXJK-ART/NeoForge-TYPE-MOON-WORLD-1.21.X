package net.xxxjk.typemoonworld.api;

@FunctionalInterface
public interface LifecycleHook {
   ExecutionResult tick(ServantContext context);
}
