package net.xxxjk.typemoonworld.api;

@FunctionalInterface
public interface CommandSpellExecutor { ExecutionResult execute(CommandSpellContext context); }
