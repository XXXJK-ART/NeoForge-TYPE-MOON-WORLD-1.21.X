package net.xxxjk.TYPE_MOON_WORLD.magic.npc;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public enum MysticMagicianRank {
   GRAND("grand", 10, 20, 90.0, 100.0, 500.0, 1000.0, 1.0),
   BRAND("brand", 5, 10, 60.0, 100.0, 400.0, 1000.0, 0.07),
   PRIDE("pride", 3, 6, 50.0, 80.0, 300.0, 1000.0, 0.14),
   FES("fes", 3, 6, 40.0, 80.0, 100.0, 1000.0, 0.16),
   ADEPT("adept", 2, 5, 30.0, 80.0, 100.0, 1000.0, 0.24),
   UMNOS("umnos", 2, 3, 20.0, 60.0, 100.0, 1000.0, 0.28),
   FRAME("frame", 1, 3, 10.0, 40.0, 100.0, 1000.0, 0.30);

   private final String id;
   private final int minMagicCount;
   private final int maxMagicCount;
   private final double minProficiency;
   private final double maxProficiency;
   private final double minMana;
   private final double maxMana;
   private final double naturalSpawnWeight;

   MysticMagicianRank(
      String id, int minMagicCount, int maxMagicCount,
      double minProficiency, double maxProficiency,
      double minMana, double maxMana, double naturalSpawnWeight
   ) {
      this.id = id;
      this.minMagicCount = minMagicCount;
      this.maxMagicCount = maxMagicCount;
      this.minProficiency = minProficiency;
      this.maxProficiency = maxProficiency;
      this.minMana = minMana;
      this.maxMana = maxMana;
      this.naturalSpawnWeight = naturalSpawnWeight;
   }

   public String id() {
      return this.id;
   }

   public int minMagicCount() {
      return this.minMagicCount;
   }

   public int maxMagicCount() {
      return this.maxMagicCount;
   }

   public double minProficiency() {
      return this.minProficiency;
   }

   public double maxProficiency() {
      return this.maxProficiency;
   }

   public double minSpecialtyProficiency() {
      return this == FES ? 90.0 : this.minProficiency;
   }

   public double maxSpecialtyProficiency() {
      return this == FES ? 100.0 : this.maxProficiency;
   }

   public double minMana() {
      return this.minMana;
   }

   public double maxMana() {
      return this.maxMana;
   }

   public double naturalSpawnWeight() {
      return this.naturalSpawnWeight;
   }

   public int rollMagicCount(RandomSource random) {
      return Mth.nextInt(random, this.minMagicCount, this.maxMagicCount);
   }

   public double rollProficiency(RandomSource random) {
      return this.minProficiency + random.nextDouble() * (this.maxProficiency - this.minProficiency);
   }

   public double rollMana(RandomSource random) {
      return this.minMana + random.nextDouble() * (this.maxMana - this.minMana);
   }

   public boolean hasEliteSpecialty() {
      return this == GRAND || this == BRAND || this == FES;
   }

   public boolean prefersAdvancedMagic() {
      return this == GRAND || this == BRAND || this == PRIDE || this == FES;
   }

   public static MysticMagicianRank fromId(String id) {
      if (id != null) {
         for (MysticMagicianRank rank : values()) {
            if (rank.id.equalsIgnoreCase(id.trim())) {
               return rank;
            }
         }
      }
      return ADEPT;
   }

   public static MysticMagicianRank fromIndex(int index) {
      MysticMagicianRank[] ranks = values();
      return index >= 0 && index < ranks.length ? ranks[index] : ADEPT;
   }

   public enum BrandColor {
      RED("red"),
      BLUE("blue"),
      YELLOW("yellow"),
      ORANGE("orange"),
      PURPLE("purple"),
      GREEN("green"),
      BLACK("black");

      private final String id;

      BrandColor(String id) {
         this.id = id;
      }

      public String id() {
         return this.id;
      }

      public static BrandColor random(RandomSource random) {
         BrandColor[] colors = values();
         return colors[random.nextInt(colors.length)];
      }

      public static BrandColor fromId(String id) {
         if (id != null) {
            for (BrandColor color : values()) {
               if (color.id.equalsIgnoreCase(id.trim())) {
                  return color;
               }
            }
         }
         return RED;
      }
   }
}
