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
   TSUBAME_WINDUP(12, "tsubame_windup", 10),
   OKITA_MUMYOUDAN_ZUKI(13, "okita_souji_saber_mumyoudan_zuki", 15, "okita_mumyoudan_zuki"),
   OKITA_THRUST(14, "okita_souji_saber_thrust", 19, "sasaki_kojiro_thrust");

   private final int networkId;
   private final String animationName;
   private final int durationTicks;
   private final String animationPath;

   SasakiKojiroAction(int networkId, String animationName, int durationTicks) {
      this(networkId, animationName, durationTicks, "sasaki_kojiro_" + animationName);
   }

   SasakiKojiroAction(int networkId, String animationName, int durationTicks, String animationPath) {
      this.networkId = networkId;
      this.animationName = animationName;
      this.durationTicks = durationTicks;
      this.animationPath = animationPath;
   }

   public int networkId() { return networkId; }
   public String animationName() { return animationName; }
   public String playerAnimationPath() { return animationPath; }
   public int durationTicks() { return durationTicks; }

   public static SasakiKojiroAction byNetworkId(int networkId) {
      for (SasakiKojiroAction action : values()) if (action.networkId == networkId) return action;
      return null;
   }
}
