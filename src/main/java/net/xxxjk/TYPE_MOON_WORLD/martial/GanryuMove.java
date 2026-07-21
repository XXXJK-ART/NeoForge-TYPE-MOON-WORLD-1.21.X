package net.xxxjk.TYPE_MOON_WORLD.martial;

public enum GanryuMove {
   HIGH_JUMP("high_jump", 0.5, 0.0F, 8, 0.0),
   STONE_FLOWER("stone_flower", 0.5, 5.0F, 10, 3.5),
   SPARROW_THRUST("sparrow_thrust", 5.0, 5.0F, 10, 3.6),
   SPRING_BUD("spring_bud", 25.0, 7.0F, 12, 4.2),
   SPRING_BUD_SECOND("spring_bud_second", 25.0, 8.0F, 14, 4.5),
   SPARROW_THRUST_SECOND("sparrow_thrust_second", 5.0, 7.0F, 14, 4.0),
   STONE_FLOWER_SECOND("stone_flower_second", 0.5, 9.0F, 14, 4.0),
   STANCE("stance", 10.0, 0.0F, 0, 0.0),
   SPARROW_SLASH("sparrow_slash", 55.0, 0.0F, 18, 5.0),
   FLOWER_BUD("flower_bud", 60.0, 0.0F, 18, 5.0),
   TSUBAME_GAESHI("tsubame_gaeshi", 80.0, 0.0F, 40, 8.0),
   UKEMI("ukemi", 50.0, 0.0F, 6, 0.0);

   private final String id;
   private final double proficiency;
   private final float damage;
   private final int recoveryTicks;
   private final double range;

   GanryuMove(String id, double proficiency, float damage, int recoveryTicks, double range) {
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
