package net.xxxjk.typemoonworld.api;

@FunctionalInterface
public interface SkillExecutor {
   ExecutionResult execute(ServantContext context);
}
