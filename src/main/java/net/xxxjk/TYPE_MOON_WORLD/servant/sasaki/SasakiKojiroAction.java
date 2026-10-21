package net.xxxjk.TYPE_MOON_WORLD.servant.sasaki;

public enum SasakiKojiroAction {
   CROUCH_CUTBACK(1, "crouch_cutback", 20),
   PRESENCE_CONCEALMENT(2, "presence_concealment", 10),
   VERTICAL_SLASH(3, "vertical_slash", 17),
   DOWNWARD_SLASH(4, "downward_slash", 13),
   UPWARD_SLASH(5, "upward_slash", 12),
   HORIZONTAL_SLASH(6, "horizontal_slash", 14),
   TRANSPARENCY(7, "transparency", 18),
   THRUST(8, "thrust", 19),
   SPINNING_SLASH(9, "spinning_slash", 23),
   COUNTER_STANCE(10, "counter_stance", 20),
   TSUBAME_GAESHI(11, "tsubame_gaeshi", 18),
   TSUBAME_WINDUP(12, "tsubame_windup", 10);

   private final int networkId;
   private final String animationName;
   private final int durationTicks;

   SasakiKojiroAction(int networkId, String animationName, int durationTicks) {
      this.networkId = networkId;
      this.animationName = animationName;
      this.durationTicks = durationTicks;
   }

   public int networkId() { return networkId; }
   public String animationName() { return animationName; }
   public String playerAnimationPath() { return "sasaki_kojiro_" + animationName; }
   public int durationTicks() { return durationTicks; }

   public static SasakiKojiroAction byNetworkId(int networkId) {
      for (SasakiKojiroAction action : values()) if (action.networkId == networkId) return action;
      return null;
   }
}
