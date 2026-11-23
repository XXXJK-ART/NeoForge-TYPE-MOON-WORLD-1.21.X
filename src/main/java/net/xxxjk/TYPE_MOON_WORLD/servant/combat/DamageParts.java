package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

/** Attacker output, before any defender mitigation. Only explicit anti-divine increments are exempt. */
public record DamageParts(float normal, float antiDivine) {
   public float total() { return normal + antiDivine; }
   public float godHand(boolean adapted) { return (normal < 15 ? 0 : normal * (adapted ? .2F : .8F)) + antiDivine; }
}
