package net.xxxjk.TYPE_MOON_WORLD.martial;

public enum KendoMove {
   HIGH_JUMP(0.5),
   PRIMARY_ONE(1.0), PRIMARY_TWO(5.0), PRIMARY_THREE(20.0),
   DOWN_ONE(20.0), DOWN_TWO(40.0), DOWN_THREE(40.0),
   UP_ATTACK(1.0), PARRY(20.0), DRAW_SLASH(50.0),
   ARC(20.0), CUT(50.0), EXECUTION(50.0), TELEPORT(70.0),
   STANCE(80.0), THREE_THRUST(80.0), CLOUD_DRAGON(80.0),
   RAY(50.0), RUNNING_RAY(70.0), PERFECT_SWORD(100.0),
   UKEMI(0.0);

   private final double required;
   KendoMove(double required) { this.required = required; }
   public double requiredProficiency() { return required; }
}
