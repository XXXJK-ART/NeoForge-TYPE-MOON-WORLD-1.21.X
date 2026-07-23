package net.xxxjk.typemoonworld.api;

@FunctionalInterface
public interface CardActionExecutor {
   ExecutionResult execute(CardActionContext context);
}
