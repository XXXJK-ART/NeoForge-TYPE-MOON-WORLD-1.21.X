package net.xxxjk.typemoonworld.api;

public record ExecutionResult(boolean handled, boolean success, double resourceCost, int cooldownTicks) {
   public static final ExecutionResult NOT_HANDLED = new ExecutionResult(false, false, 0.0, -1);
   public static final ExecutionResult SUCCESS = new ExecutionResult(true, true, 0.0, -1);
   public static final ExecutionResult FAILED = new ExecutionResult(true, false, 0.0, -1);

   public ExecutionResult withCost(double cost) {
      return new ExecutionResult(this.handled, this.success, Math.max(0.0, cost), this.cooldownTicks);
   }

   public ExecutionResult withCooldown(int ticks) {
      return new ExecutionResult(this.handled, this.success, this.resourceCost, Math.max(0, ticks));
   }
}
