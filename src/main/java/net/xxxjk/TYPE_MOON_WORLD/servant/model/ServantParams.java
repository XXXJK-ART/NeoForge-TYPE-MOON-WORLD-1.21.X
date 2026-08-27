package net.xxxjk.TYPE_MOON_WORLD.servant.model;

public record ServantParams(
   StatRank endurance,
   boolean endurancePlus,
   StatRank strength,
   boolean strengthPlus,
   StatRank agility,
   boolean agilityPlus,
   StatRank magic,
   boolean magicPlus,
   StatRank luck,
   boolean luckPlus
) {
   public ServantParams {
   }

   private int effectiveCoefficient(StatRank rank, boolean plus) {
      if (rank == null) {
         return StatRank.E.coefficient();
      }
      // A++ is encoded as its own rank and is exactly 3x the base value.
      if (rank == StatRank.A_PLUS_PLUS) {
         return (int)Math.round(StatRank.A.coefficient() * rank.parameterMultiplier());
      }
      return plus ? rank.plusCoefficient() : rank.coefficient();
   }

   public double maxHealth() {
      return effectiveCoefficient(this.endurance, this.endurancePlus) * 10.0;
   }

   public double attackDamage() {
      return effectiveCoefficient(this.strength, this.strengthPlus) * 0.5;
   }

   public double movementSpeed() {
      return 0.16 + effectiveCoefficient(this.agility, this.agilityPlus) * 0.004;
   }

   public double armor() {
      return effectiveCoefficient(this.strength, this.strengthPlus) * 0.15;
   }

   public double manaPool() {
      return effectiveCoefficient(this.magic, this.magicPlus) * 20.0;
   }

   public double critRatePercent() {
      return effectiveCoefficient(this.luck, this.luckPlus) * 0.2;
   }

   public static ServantParams of(String endurance, boolean ePlus, String strength, boolean sPlus,
                                  String agility, boolean aPlus, String magic, boolean mPlus,
                                  String luck, boolean lPlus) {
      return new ServantParams(
         StatRank.fromKey(endurance), ePlus,
         StatRank.fromKey(strength), sPlus,
         StatRank.fromKey(agility), aPlus,
         StatRank.fromKey(magic), mPlus,
         StatRank.fromKey(luck), lPlus
      );
   }
}
