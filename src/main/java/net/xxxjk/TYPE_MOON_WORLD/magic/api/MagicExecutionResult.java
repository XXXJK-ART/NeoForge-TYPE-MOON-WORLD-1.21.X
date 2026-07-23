package net.xxxjk.TYPE_MOON_WORLD.magic.api;

public record MagicExecutionResult(boolean handled, boolean success, double manaCost, int cooldownTicks) {
   public static final MagicExecutionResult SUCCESS = new MagicExecutionResult(true, true, 0.0, -1);
   public static final MagicExecutionResult FAILED = new MagicExecutionResult(true, false, 0.0, -1);
   public static final MagicExecutionResult NOT_HANDLED = new MagicExecutionResult(false, false, 0.0, -1);

   public MagicExecutionResult(boolean handled, boolean success) {
      this(handled, success, 0.0, -1);
   }
}
