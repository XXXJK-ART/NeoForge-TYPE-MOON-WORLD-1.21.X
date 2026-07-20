package net.xxxjk.TYPE_MOON_WORLD.martial;

public enum BajiquanMove {
   PUNCH("punch", 0.0, 3.0F, 7, 3.0),
   RIGHT_KICK("right_kick", 0.5, 4.0F, 8, 3.2),
   LEFT_KICK("left_kick", 0.5, 4.0F, 8, 3.2),
   ELBOW("elbow", 1.0, 4.0F, 8, 3.0),
   DOWN_KICK("down_kick", 5.0, 5.0F, 10, 3.4),
   CHOP("chop", 5.0, 5.0F, 9, 3.2),
   PALM("palm", 10.0, 5.0F, 10, 5.0),
   SHOULDER("shoulder", 10.0, 6.0F, 10, 5.0),
   STOMP("stomp", 10.0, 0.0F, 7, 0.0),
   CLAMP("clamp", 10.0, 0.0F, 8, 0.0),
   FLURRY("flurry", 10.0, 6.0F, 12, 4.0),
   FINISHER_KICK("finisher_kick", 10.0, 7.0F, 14, 4.0),
   HIGH_JUMP("high_jump", 25.0, 0.0F, 8, 0.0),
   TREMOR("tremor", 25.0, 3.0F, 14, 3.0),
   KNEE("knee", 25.0, 5.0F, 10, 4.0),
   PARRY("parry", 30.0, 0.0F, 8, 0.0),
   UKEMI("ukemi", 30.0, 0.0F, 6, 0.0),
   PUSH("push", 40.0, 4.0F, 10, 5.0),
   FA_JIN("fa_jin", 50.0, 8.0F, 12, 4.0),
   CHARGED_TREMOR("charged_tremor", 80.0, 5.0F, 18, 5.0),
   DOUBLE_PALM("double_palm", 80.0, 20.0F, 24, 5.0),
   FIERCE_TIGER("fierce_tiger", 80.0, 32.0F, 24, 8.0),
   CIRCLE_REALM("circle_realm", 100.0, 0.0F, 8, 0.0);

   private final String id;
   private final double proficiency;
   private final float damage;
   private final int recoveryTicks;
   private final double range;

   BajiquanMove(String id, double proficiency, float damage, int recoveryTicks, double range) {
      this.id = id;
      this.proficiency = proficiency;
      this.damage = damage;
      this.recoveryTicks = recoveryTicks;
      this.range = range;
   }

   public String id() { return this.id; }
   public double requiredProficiency() { return this.proficiency; }
   public float damage() { return this.damage; }
   public int recoveryTicks() { return this.recoveryTicks; }
   public double range() { return this.range; }
}
