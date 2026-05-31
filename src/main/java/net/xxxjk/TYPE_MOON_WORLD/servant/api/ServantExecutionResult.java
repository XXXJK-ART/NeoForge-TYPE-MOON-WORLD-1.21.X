package net.xxxjk.TYPE_MOON_WORLD.servant.api;

public record ServantExecutionResult(boolean handled, boolean success, double mpCost) {
   public static final ServantExecutionResult SUCCESS = new ServantExecutionResult(true, true, 0.0);
   public static final ServantExecutionResult FAILED = new ServantExecutionResult(true, false, 0.0);
   public static final ServantExecutionResult NOT_HANDLED = new ServantExecutionResult(false, false, 0.0);

   public ServantExecutionResult withMpCost(double cost) {
      return new ServantExecutionResult(this.handled, this.success, cost);
   }
}
