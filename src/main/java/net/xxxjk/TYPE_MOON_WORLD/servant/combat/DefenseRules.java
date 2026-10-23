package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

public final class DefenseRules {
   /** NPC preference only: stronger ranged hits still use the existing dodge/guard rules. */
   public static final float NPC_LIGHT_RANGED_DAMAGE_LIMIT = 40.0F;
   private DefenseRules() { }
   public static boolean npcGuardCandidate(float amount, boolean melee, boolean bypassesDefense,
                                           boolean noblePhantasm, boolean explosion) {
      return Float.isFinite(amount) && amount > 0 && !bypassesDefense && !noblePhantasm && !explosion
         && (melee || amount <= NPC_LIGHT_RANGED_DAMAGE_LIMIT);
   }

   public enum NpcReaction { NONE, GUARD, DODGE }

   /** Guard is a conditional chance after evasion fails or is unavailable. */
   public record NpcReactionChances(double guard, double dodge) {
      public NpcReaction choose(double dodgeRoll, double guardRoll) {
         return dodgeRoll < dodge ? NpcReaction.DODGE
            : guardRoll < guard ? NpcReaction.GUARD : NpcReaction.NONE;
      }
   }

   /** Absolute damage, the defender's health pool, and telegraph danger all matter. */
   public static NpcReactionChances npcReactionChances(int defenseStep, int agilityStep,
       double damage, double maxHealth, int danger, boolean canGuard, boolean canDodge) {
      double severity = Math.max(clamp((damage - 40.0) / 120.0),
         Math.max(clamp((damage / Math.max(1.0, maxHealth) - 0.08) / 0.22),
            clamp((danger - 1.0) / 4.0)));
      double reaction = Math.min(0.85, 0.30 + Math.max(0, Math.max(defenseStep, agilityStep)) * 0.025
         + severity * 0.30);
      double dodgeShare = Math.max(0.20, Math.min(0.90,
         0.65 + (agilityStep - defenseStep) * 0.025 + severity * 0.45));
      // Evasion is checked first; its failure still leaves guard probabilistic.
      return new NpcReactionChances(canGuard ? reaction * (1.0 - dodgeShare) : 0.0,
         canDodge ? reaction * dodgeShare : 0.0);
   }

   private static double clamp(double value) { return Math.max(0.0, Math.min(1.0, value)); }
   public static double poiseLoss(double actualDamage) { return Math.max(10.0, actualDamage * 0.5); }
   public static int safeDodgeCooldown(int proposedTicks, int longestInvulnerability) {
      return Math.max(proposedTicks, longestInvulnerability + 10);
   }
   public static double guardCost(double base, double bonus, boolean active, boolean perfect) {
      return guardCost(base, bonus, active, perfect, 0.0);
   }
   public static double guardCost(double base, double defenderReduction, boolean active,
                                  boolean perfect, double attackerSouwaBonus) {
      return net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SasakiKojiroRules.guardStaminaCost(
         base, defenderReduction, active, perfect, attackerSouwaBonus);
   }
   public static float guardedDamage(float amount, boolean active, boolean perfect) {
      return perfect ? 0 : amount * (active ? 0.2F : 0.5F);
   }
}
