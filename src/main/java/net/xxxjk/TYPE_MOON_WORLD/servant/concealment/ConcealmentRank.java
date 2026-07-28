package net.xxxjk.TYPE_MOON_WORLD.servant.concealment;

import java.util.Locale;

/** Visibility leakage profile for ranked servant concealment. */
public enum ConcealmentRank {
   NONE(0, 0, 0),
   E(5, 30, 65),
   D(4, 45, 85),
   C(3, 60, 105),
   B(2, 80, 130),
   A_MINUS(2, 110, 170),
   A(1, 140, 220),
   A_PLUS(0, 0, 0),
   EX(0, 0, 0);

   private final int maxParticles;
   private final int minimumDelay;
   private final int maximumDelay;

   ConcealmentRank(int maxParticles, int minimumDelay, int maximumDelay) {
      this.maxParticles = maxParticles;
      this.minimumDelay = minimumDelay;
      this.maximumDelay = maximumDelay;
   }

   public int maxParticles() {
      return this.maxParticles;
   }

   public int minimumDelay() {
      return this.minimumDelay;
   }

   public int maximumDelay() {
      return this.maximumDelay;
   }

   public boolean isPerfect() {
      return this == A_PLUS || this == EX;
   }

   public static ConcealmentRank fromSkillId(String skillId) {
      if (skillId == null || skillId.isBlank()) return NONE;
      String id = skillId.toLowerCase(Locale.ROOT);
      if (!id.contains("presence_concealment") && !id.startsWith("stealth_")) return NONE;
      if (id.contains("_ex")) return EX;
      if (id.contains("_a_plus")) return A_PLUS;
      if (id.contains("_a_minus")) return A_MINUS;
      if (id.matches(".*_a(?:_|$).*")) return A;
      if (id.matches(".*_b(?:_|$).*")) return B;
      if (id.matches(".*_c(?:_|$).*")) return C;
      if (id.matches(".*_d(?:_|$).*")) return D;
      if (id.matches(".*_e(?:_|$).*")) return E;
      return NONE;
   }

   public static ConcealmentRank stronger(ConcealmentRank first, ConcealmentRank second) {
      ConcealmentRank left = first == null ? NONE : first;
      ConcealmentRank right = second == null ? NONE : second;
      return left.ordinal() >= right.ordinal() ? left : right;
   }
}
