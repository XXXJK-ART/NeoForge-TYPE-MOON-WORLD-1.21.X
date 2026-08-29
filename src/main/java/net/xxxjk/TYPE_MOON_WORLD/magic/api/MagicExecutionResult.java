package net.xxxjk.TYPE_MOON_WORLD.magic.api;

public record MagicExecutionResult(boolean handled, boolean success, double manaCost, int cooldownTicks, String failureReason) {
   public static final MagicExecutionResult SUCCESS = new MagicExecutionResult(true, true, 0.0, -1, "");
   public static final MagicExecutionResult FAILED = new MagicExecutionResult(true, false, 0.0, -1, "failed");
   public static final MagicExecutionResult NOT_HANDLED = new MagicExecutionResult(false, false, 0.0, -1, "not_handled");

   public MagicExecutionResult(boolean handled, boolean success, double manaCost, int cooldownTicks) {
      this(handled, success, manaCost, cooldownTicks, success ? "" : "failed");
   }

   public MagicExecutionResult(boolean handled, boolean success) {
      this(handled, success, 0.0, -1, success ? "" : "failed");
   }

   public String reason() { return failureReason; }
   public boolean failed() { return handled && !success; }
   public int cooldown() { return cooldownTicks; }
}
