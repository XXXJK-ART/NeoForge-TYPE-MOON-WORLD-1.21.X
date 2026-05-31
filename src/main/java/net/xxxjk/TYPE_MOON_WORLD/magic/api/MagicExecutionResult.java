package net.xxxjk.TYPE_MOON_WORLD.magic.api;

public record MagicExecutionResult(boolean handled, boolean success) {
   public static final MagicExecutionResult SUCCESS = new MagicExecutionResult(true, true);
   public static final MagicExecutionResult FAILED = new MagicExecutionResult(true, false);
   public static final MagicExecutionResult NOT_HANDLED = new MagicExecutionResult(false, false);
}
