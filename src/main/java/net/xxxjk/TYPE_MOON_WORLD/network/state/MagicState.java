package net.xxxjk.TYPE_MOON_WORLD.network.state;

import java.util.List;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

/** Domain view for magic/circuit state stored in the player attachment. */
public final class MagicState {
   /** Persisted sub-state moved out of PlayerVariables' public field set. */
   public static final class Data {
      private boolean analysisActive;
      private double mana;
      private double maxMana;
      private double manaRegen;
      private double restoreMagicMoment;
      private double cooldown;
   private int observedLegacyHash;
      private boolean initialized;
      public boolean analysisActive() { return analysisActive; }
      public void setAnalysisActive(boolean value) { analysisActive = value; }
   }
   private final TypeMoonWorldModVariables.PlayerVariables vars;

   public MagicState(TypeMoonWorldModVariables.PlayerVariables vars) {
      this.vars = vars;
   }

   private void syncLegacy() {
      Data data = vars.magicData();
      int legacyHash = legacyHash(data.analysisActive);
      if (!data.initialized || data.observedLegacyHash != legacyHash) {
         data.mana = finiteNonNegative(vars.player_mana);
         data.maxMana = finiteNonNegative(vars.player_max_mana);
         data.manaRegen = finiteNonNegative(vars.player_mana_egenerated_every_moment);
         data.restoreMagicMoment = finiteNonNegative(vars.player_restore_magic_moment);
         data.cooldown = finiteNonNegative(vars.magic_cooldown);
         data.observedLegacyHash = legacyHash;
         data.initialized = true;
      }
   }

   private void publish(Data data) {
      vars.player_mana = finiteNonNegative(data.mana);
      vars.player_max_mana = finiteNonNegative(data.maxMana);
      vars.player_mana_egenerated_every_moment = finiteNonNegative(data.manaRegen);
      vars.player_restore_magic_moment = finiteNonNegative(data.restoreMagicMoment);
      vars.magic_cooldown = finiteNonNegative(data.cooldown);
      data.observedLegacyHash = legacyHash(data.analysisActive);
      data.initialized = true;
   }

   private static double finiteNonNegative(double value) {
      return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
   }

   private int legacyHash(boolean analysisActive) {
      int hash = 1;
      hash = 31 * hash + Double.hashCode(vars.player_mana);
      hash = 31 * hash + Double.hashCode(vars.player_max_mana);
      hash = 31 * hash + Double.hashCode(vars.player_mana_egenerated_every_moment);
      hash = 31 * hash + Double.hashCode(vars.player_restore_magic_moment);
      hash = 31 * hash + Double.hashCode(vars.magic_cooldown);
      return 31 * hash + Boolean.hashCode(analysisActive);
   }

   public void refreshFromLegacy() { syncLegacy(); }
   public double mana() { syncLegacy(); return vars.magicData().mana; }
   public double maxMana() { syncLegacy(); return vars.magicData().maxMana; }
   public double manaRegen() { syncLegacy(); return vars.magicData().manaRegen; }
   public double restoreMagicMoment() { syncLegacy(); return vars.magicData().restoreMagicMoment; }
   public double cooldown() { syncLegacy(); return vars.magicData().cooldown; }
   public boolean circuitOpen() { return vars.is_magic_circuit_open; }
   public boolean analysisActive() { return vars.magicData().analysisActive(); }
   public List<String> learnedMagics() { return vars.learned_magics; }
   public void setMana(double value) { syncLegacy(); vars.magicData().mana = value; publish(vars.magicData()); vars.markDirty(TypeMoonWorldModVariables.PlayerVariables.StateDomain.MAGIC); }
   public void setMaxMana(double value) { syncLegacy(); vars.magicData().maxMana = value; publish(vars.magicData()); vars.markDirty(TypeMoonWorldModVariables.PlayerVariables.StateDomain.MAGIC); }
   public void setManaRegen(double value) { syncLegacy(); vars.magicData().manaRegen = value; publish(vars.magicData()); vars.markDirty(TypeMoonWorldModVariables.PlayerVariables.StateDomain.MAGIC); }
   public void setRestoreMagicMoment(double value) { syncLegacy(); vars.magicData().restoreMagicMoment = value; publish(vars.magicData()); vars.markDirty(TypeMoonWorldModVariables.PlayerVariables.StateDomain.MAGIC); }
   public void setCooldown(double value) { syncLegacy(); vars.magicData().cooldown = value; publish(vars.magicData()); vars.markDirty(TypeMoonWorldModVariables.PlayerVariables.StateDomain.MAGIC); }
   public boolean hasMana(double amount) { return mana() + 1.0E-6D >= Math.max(0.0D, amount); }
   public void addMana(double amount) { setMana(Math.min(maxMana(), mana() + Math.max(0.0D, amount))); }
   public boolean tryConsume(double amount) {
      if (amount <= 0.0D) return true;
      syncLegacy();
      if (vars.magicData().mana + 1.0E-6D < amount) return false;
      vars.magicData().mana -= amount;
      publish(vars.magicData());
      vars.markDirty(TypeMoonWorldModVariables.PlayerVariables.StateDomain.MAGIC);
      return true;
   }
   public void setCooldownAtLeast(double ticks) { setCooldown(Math.max(cooldown(), ticks)); }
   public void tickCooldown() { if (cooldown() > 0.0D) setCooldown(Math.max(0.0D, cooldown() - 1.0D)); }
   public void setCircuitOpen(boolean value) { vars.is_magic_circuit_open = value; vars.markDirty(TypeMoonWorldModVariables.PlayerVariables.StateDomain.MAGIC); }
   public void setAnalysisActive(boolean value) { vars.magicData().setAnalysisActive(value); vars.markDirty(TypeMoonWorldModVariables.PlayerVariables.StateDomain.MAGIC); }
}
