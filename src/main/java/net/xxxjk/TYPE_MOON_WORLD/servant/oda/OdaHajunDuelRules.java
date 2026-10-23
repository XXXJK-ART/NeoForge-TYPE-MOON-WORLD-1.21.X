package net.xxxjk.TYPE_MOON_WORLD.servant.oda;

import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantEra;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;

/** NPC-only approximation of an unobserved Hajun battle; these are probability weights, not damage. */
public final class OdaHajunDuelRules {
   public static final int MIN_DURATION = 60;
   public static final int MAX_DURATION = 100;
   private OdaHajunDuelRules() {}

   public enum Outcome { ODA_WINS, OPPONENTS_WIN, NEITHER_WINS, BOTH_DIE }

   public record Weights(double odaWins, double opponentsWin, double neitherWins, double bothDie) {
      public Outcome roll(double unitRoll) {
         double roll = Math.clamp(unitRoll, 0, Math.nextDown(1.0));
         if (roll < odaWins) return Outcome.ODA_WINS;
         if (roll < odaWins + opponentsWin) return Outcome.OPPONENTS_WIN;
         if (roll < odaWins + opponentsWin + neitherWins) return Outcome.NEITHER_WINS;
         return Outcome.BOTH_DIE;
      }
   }

   /** Uses the common parameter conversions, including A-, + and A++, without a second rank table. */
   public static double panelScore(ServantParams params) {
      return params.attackDamage() * 2 * .25 + params.maxHealth() / 10 * .25
         + (params.movementSpeed() - .16) / .004 * .20 + params.manaPool() / 20 * .20
         + params.critRatePercent() / .2 * .10;
   }

   public static double healthFactor(double healthFraction) {
      return .25 + .75 * Math.clamp(healthFraction, 0, 1);
   }

   /** Reuses Tenka Fubu once; Hajun has no Three Thousand Worlds riding or Strategy multiplier. */
   public static double matchupMultiplier(int divinity, ServantEra era) {
      return OdaRules.special(divinity, era, false) * OdaRules.eraFinal(era);
   }

   public static Weights weights(double odaScore, double opponentsScore) {
      double oda = Math.max(1.0e-6, odaScore), opponents = Math.max(1.0e-6, opponentsScore);
      double share = oda / (oda + opponents);
      double closeness = 1 - Math.abs(2 * share - 1);
      double neither = .05 + .10 * closeness;
      double both = .01 + .04 * closeness;
      double decisive = 1 - neither - both;
      return new Weights(decisive * share, decisive * (1 - share), neither, both);
   }
}
