package net.xxxjk.typemoonworld.api;

@FunctionalInterface
public interface ServantActionExecutor {
   ExecutionResult execute(ServantContext context);
}
