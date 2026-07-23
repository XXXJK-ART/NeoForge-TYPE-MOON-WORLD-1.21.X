package net.xxxjk.typemoonworld.api;

@FunctionalInterface
public interface MagicExecutor {
   ExecutionResult execute(MagicCastContext context);
}
