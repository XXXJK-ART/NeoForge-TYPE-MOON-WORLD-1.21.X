package net.xxxjk.TYPE_MOON_WORLD.magic.api;

public record MagicExecutionResult(boolean handled, boolean success, double manaCost, int cooldownTicks, String failureReason, Status status) {
   public enum Status {
      SUCCESS,
      STARTED,
      STOPPED,
      REJECTED,
      NOT_HANDLED
   }

   public static final MagicExecutionResult SUCCESS = new MagicExecutionResult(true, true, 0.0, -1, "", Status.SUCCESS);
   public static final MagicExecutionResult STARTED = new MagicExecutionResult(true, true, 0.0, -1, "", Status.STARTED);
   public static final MagicExecutionResult STOPPED = new MagicExecutionResult(true, true, 0.0, -1, "", Status.STOPPED);
   public static final MagicExecutionResult FAILED = new MagicExecutionResult(true, false, 0.0, -1, "failed", Status.REJECTED);
   public static final MagicExecutionResult REJECTED = FAILED;
   public static final MagicExecutionResult NOT_HANDLED = new MagicExecutionResult(false, false, 0.0, -1, "not_handled", Status.NOT_HANDLED);

   public MagicExecutionResult(boolean handled, boolean success, double manaCost, int cooldownTicks, String failureReason) {
      this(handled, success, manaCost, cooldownTicks, failureReason, inferStatus(handled, success, failureReason));
   }

   public MagicExecutionResult(boolean handled, boolean success, double manaCost, int cooldownTicks) {
      this(handled, success, manaCost, cooldownTicks, success ? "" : "failed");
   }

   public MagicExecutionResult(boolean handled, boolean success) {
      this(handled, success, 0.0, -1, success ? "" : "failed");
   }

   public String reason() { return failureReason; }
   public boolean failed() { return handled && !success; }
   public int cooldown() { return cooldownTicks; }
   public boolean accepted() { return handled && status != Status.REJECTED && status != Status.NOT_HANDLED; }
   public boolean startsContinuous() { return status == Status.STARTED; }
   public boolean stopsContinuous() { return status == Status.STOPPED; }
   public boolean shouldApplyCooldown() {
      return accepted() && status != Status.STOPPED && (cooldownTicks >= 0 || status == Status.SUCCESS);
   }

   public static MagicExecutionResult started(double manaCost, int cooldownTicks) {
      return new MagicExecutionResult(true, true, Math.max(0.0, manaCost), cooldownTicks, "", Status.STARTED);
   }

   public static MagicExecutionResult stopped() {
      return STOPPED;
   }

   public static MagicExecutionResult rejected(String reason) {
      return new MagicExecutionResult(true, false, 0.0, -1, reason == null || reason.isBlank() ? "rejected" : reason, Status.REJECTED);
   }

   private static Status inferStatus(boolean handled, boolean success, String reason) {
      if (!handled) return Status.NOT_HANDLED;
      return success ? Status.SUCCESS : Status.REJECTED;
   }
}
