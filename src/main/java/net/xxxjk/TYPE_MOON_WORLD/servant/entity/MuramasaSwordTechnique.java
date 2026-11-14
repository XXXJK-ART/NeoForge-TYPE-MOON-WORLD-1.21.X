package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

public enum MuramasaSwordTechnique {
   DOWNWARD_SLASH("muramasa_downward_slash", 8.0, 60, 3.0, 1.15, false),
   THRUST("muramasa_thrust", 14.0, 100, 4.5, 1.35, false),
   SPINNING_SLASH("muramasa_spinning_slash", 16.0, 140, 4.0, 0.75, true);

   private final String id;
   private final double manaCost;
   private final int cooldownTicks;
   private final double range;
   private final double damageMultiplier;
   private final boolean area;

   MuramasaSwordTechnique(String id, double manaCost, int cooldownTicks, double range, double damageMultiplier, boolean area) {
      this.id = id;
      this.manaCost = manaCost;
      this.cooldownTicks = cooldownTicks;
      this.range = range;
      this.damageMultiplier = damageMultiplier;
      this.area = area;
   }

   public String id() { return id; }
   public double manaCost() { return manaCost; }
   public int cooldownTicks() { return cooldownTicks; }
   public double range() { return range; }
   public double damageMultiplier() { return damageMultiplier; }
   public boolean isArea() { return area; }
}
