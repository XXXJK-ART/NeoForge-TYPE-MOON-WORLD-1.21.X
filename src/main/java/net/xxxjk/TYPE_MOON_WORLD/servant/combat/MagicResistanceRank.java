package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

public enum MagicResistanceRank {
   NONE(0, "none"),
   E(1, "e"),
   D(2, "d"),
   C(3, "c"),
   B(4, "b"),
   A(5, "a");

   private final int level;
   private final String key;

   MagicResistanceRank(int level, String key) {
      this.level = level;
      this.key = key;
   }

   public int level() {
      return this.level;
   }

   public String key() {
      return this.key;
   }

   public boolean isAtLeast(MagicResistanceRank other) {
      return other != null && this.level >= other.level;
   }

   public static MagicResistanceRank fromLevel(int level) {
      for (MagicResistanceRank rank : values()) {
         if (rank.level == level) {
            return rank;
         }
      }
      return level <= 0 ? NONE : A;
   }
}
