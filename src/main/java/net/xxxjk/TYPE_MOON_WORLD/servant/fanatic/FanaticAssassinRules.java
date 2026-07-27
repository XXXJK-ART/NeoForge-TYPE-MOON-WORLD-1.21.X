package net.xxxjk.TYPE_MOON_WORLD.servant.fanatic;

import net.xxxjk.TYPE_MOON_WORLD.servant.model.StatRank;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceRank;

public final class FanaticAssassinRules {
   public enum TechniqueDecision {
      COMPUTER, TEMPERATURE, MARROW, HEARTBEAT, NERVES, JINN, HAIR, TOXIN, BASIC
   }
   public static final double HEARTBEAT_RANGE = 5.0;
   public static final float HEARTBEAT_DAMAGE = 250.0F;
   public static final double MARROW_RADIUS = 15.0;
   public static final float MARROW_DAMAGE = 150.0F;
   public static final double HAIR_RANGE = 8.0;
   public static final float HAIR_DAMAGE = 100.0F;
   public static final double COMPUTER_RANGE = 1.8;
   public static final float COMPUTER_DAMAGE = 150.0F;
   public static final double COMPUTER_SPLASH_RADIUS = 2.0;
   public static final float COMPUTER_BACKLASH = 20.0F;
   public static final float TOXIN_DAMAGE_PER_SECOND = 10.0F;
   public static final int JINN_MAX_HEALTH = 200;
   public static final float JINN_DAMAGE = 30.0F;

   public static final int HEARTBEAT_MP = 50;
   public static final int MARROW_MP = 40;
   public static final int HAIR_MP = 30;
   public static final int TEMPERATURE_MP = 30;
   public static final int NERVES_MP = 20;
   public static final int COMPUTER_MP = 40;
   public static final int TOXIN_MP = 30;
   public static final int JINN_MP = 60;

   public static final int TECHNIQUE_COOLDOWN = 600;
   public static final int HEARTBEAT_COOLDOWN = TECHNIQUE_COOLDOWN;
   public static final int MARROW_COOLDOWN = TECHNIQUE_COOLDOWN;
   public static final int HAIR_COOLDOWN = TECHNIQUE_COOLDOWN;
   public static final int TEMPERATURE_COOLDOWN = TECHNIQUE_COOLDOWN;
   public static final int NERVES_COOLDOWN = TECHNIQUE_COOLDOWN;
   public static final int COMPUTER_COOLDOWN = TECHNIQUE_COOLDOWN;
   public static final int TOXIN_COOLDOWN = TECHNIQUE_COOLDOWN;
   public static final int JINN_COOLDOWN = TECHNIQUE_COOLDOWN;
   public static final float COMBO_CHANCE = 0.30F;
   public static final int MAX_COMBO_TECHNIQUES = 6;

   public static final int WOUNDED_DURATION = 200;
   public static final int CONFUSION_DURATION = 100;
   public static final int CIRCUIT_DISRUPTION_DURATION = 160;
   public static final int TEMPERATURE_DURATION = 300;
   public static final int NERVES_DURATION = 300;
   public static final int STUN_DURATION = 40;
   public static final int TOXIN_STANCE_DURATION = 240;
   public static final int TOXIN_DURATION = 160;
   public static final int JINN_LIFETIME = 600;
   public static final int RECONCEAL_DELAY = 100;
   public static final int MENTAL_CLEANSE_INTERVAL = 1200;
   public static final float MENTAL_ATTACK_CANCEL_CHANCE = 0.70F;

   private FanaticAssassinRules() {
   }

   public static float heartbeatDamage(MagicResistanceRank magicResistance, StatRank luck) {
      boolean resistant = magicResistance != null && magicResistance.isAtLeast(MagicResistanceRank.B);
      return resistant || isAtLeastB(luck) ? HEARTBEAT_DAMAGE * 0.5F : HEARTBEAT_DAMAGE;
   }

   public static boolean isAtLeastB(StatRank rank) {
      return rank != null && rank.coefficient() >= StatRank.B.coefficient();
   }

   public static boolean shouldRetreat(double currentMp, double maxMp) {
      return maxMp > 0.0 && currentMp < maxMp * 0.20;
   }

   public static boolean recoveredFromRetreat(double currentMp, double maxMp) {
      return maxMp <= 0.0 || currentMp >= maxMp * 0.30;
   }

   public static float computerSplashDamage(double distance) {
      if (distance < 0.0 || distance > COMPUTER_SPLASH_RADIUS) return 0.0F;
      return (float)(COMPUTER_DAMAGE * (1.0 - distance / COMPUTER_SPLASH_RADIUS));
   }

   public static int jinnForm(double distance) {
      return distance <= 4.0 ? 0 : 1;
   }

   public static boolean cooldownReady(long now, boolean hasPreviousUse, long previousUse, int cooldown) {
      return !hasPreviousUse || now - previousUse >= cooldown;
   }

   public static boolean shouldCancelMentalAttack(float roll) {
      return roll >= 0.0F && roll < MENTAL_ATTACK_CANCEL_CHANCE;
   }

   public static boolean shouldAttemptCombo(float roll) {
      return roll >= 0.0F && roll < COMBO_CHANCE;
   }

   public static boolean canContinueCombo(int techniquesCast) {
      return techniquesCast >= 1 && techniquesCast < MAX_COMBO_TECHNIQUES;
   }

   public static TechniqueDecision chooseTechnique(boolean computer, boolean temperature, boolean marrow,
                                                    boolean heartbeat, boolean nerves, boolean jinn,
                                                    boolean hair, boolean toxin) {
      if (computer) return TechniqueDecision.COMPUTER;
      if (temperature) return TechniqueDecision.TEMPERATURE;
      if (marrow) return TechniqueDecision.MARROW;
      if (heartbeat) return TechniqueDecision.HEARTBEAT;
      if (nerves) return TechniqueDecision.NERVES;
      if (jinn) return TechniqueDecision.JINN;
      if (hair) return TechniqueDecision.HAIR;
      if (toxin) return TechniqueDecision.TOXIN;
      return TechniqueDecision.BASIC;
   }
}
