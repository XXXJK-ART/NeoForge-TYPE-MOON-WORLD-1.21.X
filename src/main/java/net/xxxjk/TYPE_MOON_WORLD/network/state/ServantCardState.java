package net.xxxjk.TYPE_MOON_WORLD.network.state;

import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

/**
 * Domain view for the active servant card runtime.
 *
 * <p>The public fields on {@code PlayerVariables} remain as a compatibility
 * mirror for older skills and saved data. New code should use this view. The
 * hash based bridge detects legacy writes without requiring every old caller
 * to be migrated in one release.</p>
 */
public final class ServantCardState {
   public static final class Data {
      private boolean transformed;
      private String id = "";
      private double mana;
      private double maxMana;
      private double manaRegen;
      private int jumpCharges;
      private int jumpRecoveryTicks;
      private long jumpRecoveryEnd;
      private String skillCooldowns = "";
      private String skillCooldownEnds = "";
      private int npCooldown;
      private long npCooldownEnd;
      private boolean flying;
      private int flightMode;
      private long highFlightUntil;
      private long highFlightCooldownUntil;
      private int odaFlightTicks;
      private long odaFlightCooldownUntil;
      private long odaFlightRechargeAt;
      private double flightForward;
      private double flightStrafe;
      private double flightVertical;
      private int flightToggleCooldown;
      private int actionMode;
      private int transformCooldown;
      private int releaseCooldown;
      private boolean deathRelease;
      private int medeaDragonfangStock;
      private int medeaManaCharmStock;
      private int medeaHealCharmStock;
      private int paracelsusStoneStock;
      private int paracelsusDiamondShieldStock;
      private int arashArrowStock;
      private int royalCannonAmmo;
      private double gillesSpellbookMana;
      private double gillesSpellbookMaxMana;
      private int heraclesGodHandLives;
      private String enkiduPoints = "";
      private boolean medusaMysticEyesActive;
      private boolean hassanCloakBroken;
      private int hassanZabaniyaAnimationUntil;
      private int observedLegacyHash;
      private boolean initialized;
      private boolean changed;
   }

   private final TypeMoonWorldModVariables.PlayerVariables vars;

   public ServantCardState(TypeMoonWorldModVariables.PlayerVariables vars) { this.vars = vars; }

   private Data data() { return vars.servantCardData(); }

   private int legacyHash() {
      int hash = 1;
      hash = mix(hash, vars.servant_card_transformed);
      hash = mix(hash, vars.servant_card_id);
      hash = mix(hash, vars.servant_card_mana);
      hash = mix(hash, vars.servant_card_max_mana);
      hash = mix(hash, vars.servant_card_mana_regen);
      hash = mix(hash, vars.servant_card_jump_charges);
      hash = mix(hash, vars.servant_card_jump_recovery_ticks);
      hash = mix(hash, vars.servant_card_jump_recovery_end);
      hash = mix(hash, vars.servant_card_skill_cooldowns);
      hash = mix(hash, vars.servant_card_skill_cooldown_ends);
      hash = mix(hash, vars.servant_card_np_cooldown);
      hash = mix(hash, vars.servant_card_np_cooldown_end);
      hash = mix(hash, vars.servant_card_flying);
      hash = mix(hash, vars.servant_card_flight_mode);
      hash = mix(hash, vars.servant_card_high_flight_until);
      hash = mix(hash, vars.servant_card_high_flight_cooldown_until);
      hash = mix(hash, vars.servant_card_oda_flight_ticks);
      hash = mix(hash, vars.servant_card_oda_flight_cooldown_until);
      hash = mix(hash, vars.servant_card_oda_flight_recharge_at);
      hash = mix(hash, vars.servant_card_flight_forward);
      hash = mix(hash, vars.servant_card_flight_strafe);
      hash = mix(hash, vars.servant_card_flight_vertical);
      hash = mix(hash, vars.servant_card_flight_toggle_cooldown);
      hash = mix(hash, vars.servant_card_action_mode);
      hash = mix(hash, vars.servant_card_transform_cooldown);
      hash = mix(hash, vars.servant_card_release_cooldown);
      hash = mix(hash, vars.servant_card_death_release);
      hash = mix(hash, vars.servant_card_medea_dragonfang_stock);
      hash = mix(hash, vars.servant_card_medea_mana_charm_stock);
      hash = mix(hash, vars.servant_card_medea_heal_charm_stock);
      hash = mix(hash, vars.servant_card_paracelsus_stone_stock);
      hash = mix(hash, vars.servant_card_paracelsus_diamond_shield_stock);
      hash = mix(hash, vars.servant_card_arash_arrow_stock);
      hash = mix(hash, vars.servant_card_royal_cannon_ammo);
      hash = mix(hash, vars.servant_card_gilles_spellbook_mana);
      hash = mix(hash, vars.servant_card_gilles_spellbook_max_mana);
      hash = mix(hash, vars.servant_card_heracles_god_hand_lives);
      hash = mix(hash, vars.servant_card_enkidu_transfiguration_points);
      hash = mix(hash, vars.servant_card_medusa_mystic_eyes_active);
      hash = mix(hash, vars.servant_card_hassan_cloak_broken);
      return mix(hash, vars.servant_card_hassan_zabaniya_animation_until);
   }

   private static int mix(int hash, boolean value) { return 31 * hash + Boolean.hashCode(value); }
   private static int mix(int hash, int value) { return 31 * hash + value; }
   private static int mix(int hash, long value) { return 31 * hash + Long.hashCode(value); }
   private static int mix(int hash, double value) { return 31 * hash + Double.hashCode(value); }
   private static int mix(int hash, String value) { return 31 * hash + (value == null ? 0 : value.hashCode()); }

   private void syncLegacy() {
      Data data = data();
      int hash = legacyHash();
      if (data.initialized && data.observedLegacyHash == hash) return;
      data.transformed = vars.servant_card_transformed;
      data.id = safe(vars.servant_card_id);
      data.mana = finiteNonNegative(vars.servant_card_mana);
      data.maxMana = finiteNonNegative(vars.servant_card_max_mana);
      data.manaRegen = finiteNonNegative(vars.servant_card_mana_regen);
      data.jumpCharges = Math.max(0, vars.servant_card_jump_charges);
      data.jumpRecoveryTicks = Math.max(0, vars.servant_card_jump_recovery_ticks);
      data.jumpRecoveryEnd = Math.max(0L, vars.servant_card_jump_recovery_end);
      data.skillCooldowns = safe(vars.servant_card_skill_cooldowns);
      data.skillCooldownEnds = safe(vars.servant_card_skill_cooldown_ends);
      data.npCooldown = Math.max(0, vars.servant_card_np_cooldown);
      data.npCooldownEnd = Math.max(0L, vars.servant_card_np_cooldown_end);
      data.flying = vars.servant_card_flying;
      data.flightMode = clamp(vars.servant_card_flight_mode, 0, 2);
      data.highFlightUntil = Math.max(0L, vars.servant_card_high_flight_until);
      data.highFlightCooldownUntil = Math.max(0L, vars.servant_card_high_flight_cooldown_until);
      data.odaFlightTicks = clamp(vars.servant_card_oda_flight_ticks, 0, 100);
      data.odaFlightCooldownUntil = Math.max(0L, vars.servant_card_oda_flight_cooldown_until);
      data.odaFlightRechargeAt = Math.max(0L, vars.servant_card_oda_flight_recharge_at);
      data.flightForward = clamp(vars.servant_card_flight_forward, -1.0, 1.0);
      data.flightStrafe = clamp(vars.servant_card_flight_strafe, -1.0, 1.0);
      data.flightVertical = clamp(vars.servant_card_flight_vertical, -1.0, 1.0);
      data.flightToggleCooldown = Math.max(0, vars.servant_card_flight_toggle_cooldown);
      data.actionMode = Math.max(0, vars.servant_card_action_mode);
      data.transformCooldown = Math.max(0, vars.servant_card_transform_cooldown);
      data.releaseCooldown = Math.max(0, vars.servant_card_release_cooldown);
      data.deathRelease = vars.servant_card_death_release;
      data.medeaDragonfangStock = Math.max(0, vars.servant_card_medea_dragonfang_stock);
      data.medeaManaCharmStock = Math.max(0, vars.servant_card_medea_mana_charm_stock);
      data.medeaHealCharmStock = Math.max(0, vars.servant_card_medea_heal_charm_stock);
      data.paracelsusStoneStock = Math.max(0, vars.servant_card_paracelsus_stone_stock);
      data.paracelsusDiamondShieldStock = Math.max(0, vars.servant_card_paracelsus_diamond_shield_stock);
      data.arashArrowStock = Math.max(0, vars.servant_card_arash_arrow_stock);
      data.royalCannonAmmo = Math.max(0, vars.servant_card_royal_cannon_ammo);
      data.gillesSpellbookMana = finiteNonNegative(vars.servant_card_gilles_spellbook_mana);
      data.gillesSpellbookMaxMana = finiteNonNegative(vars.servant_card_gilles_spellbook_max_mana);
      data.heraclesGodHandLives = Math.max(0, vars.servant_card_heracles_god_hand_lives);
      data.enkiduPoints = safe(vars.servant_card_enkidu_transfiguration_points);
      data.medusaMysticEyesActive = vars.servant_card_medusa_mystic_eyes_active;
      data.hassanCloakBroken = vars.servant_card_hassan_cloak_broken;
      data.hassanZabaniyaAnimationUntil = Math.max(0, vars.servant_card_hassan_zabaniya_animation_until);
      data.observedLegacyHash = hash;
      data.initialized = true;
   }

   private void publish() {
      Data data = data();
      int before = legacyHash();
      vars.servant_card_transformed = data.transformed;
      vars.servant_card_id = safe(data.id);
      vars.servant_card_mana = finiteNonNegative(data.mana);
      vars.servant_card_max_mana = finiteNonNegative(data.maxMana);
      vars.servant_card_mana_regen = finiteNonNegative(data.manaRegen);
      vars.servant_card_jump_charges = Math.max(0, data.jumpCharges);
      vars.servant_card_jump_recovery_ticks = Math.max(0, data.jumpRecoveryTicks);
      vars.servant_card_jump_recovery_end = Math.max(0L, data.jumpRecoveryEnd);
      vars.servant_card_skill_cooldowns = safe(data.skillCooldowns);
      vars.servant_card_skill_cooldown_ends = safe(data.skillCooldownEnds);
      vars.servant_card_np_cooldown = Math.max(0, data.npCooldown);
      vars.servant_card_np_cooldown_end = Math.max(0L, data.npCooldownEnd);
      vars.servant_card_flying = data.flying;
      vars.servant_card_flight_mode = clamp(data.flightMode, 0, 2);
      vars.servant_card_high_flight_until = Math.max(0L, data.highFlightUntil);
      vars.servant_card_high_flight_cooldown_until = Math.max(0L, data.highFlightCooldownUntil);
      vars.servant_card_oda_flight_ticks = clamp(data.odaFlightTicks, 0, 100);
      vars.servant_card_oda_flight_cooldown_until = Math.max(0L, data.odaFlightCooldownUntil);
      vars.servant_card_oda_flight_recharge_at = Math.max(0L, data.odaFlightRechargeAt);
      vars.servant_card_flight_forward = clamp(data.flightForward, -1.0, 1.0);
      vars.servant_card_flight_strafe = clamp(data.flightStrafe, -1.0, 1.0);
      vars.servant_card_flight_vertical = clamp(data.flightVertical, -1.0, 1.0);
      vars.servant_card_flight_toggle_cooldown = Math.max(0, data.flightToggleCooldown);
      vars.servant_card_action_mode = Math.max(0, data.actionMode);
      vars.servant_card_transform_cooldown = Math.max(0, data.transformCooldown);
      vars.servant_card_release_cooldown = Math.max(0, data.releaseCooldown);
      vars.servant_card_death_release = data.deathRelease;
      vars.servant_card_medea_dragonfang_stock = Math.max(0, data.medeaDragonfangStock);
      vars.servant_card_medea_mana_charm_stock = Math.max(0, data.medeaManaCharmStock);
      vars.servant_card_medea_heal_charm_stock = Math.max(0, data.medeaHealCharmStock);
      vars.servant_card_paracelsus_stone_stock = Math.max(0, data.paracelsusStoneStock);
      vars.servant_card_paracelsus_diamond_shield_stock = Math.max(0, data.paracelsusDiamondShieldStock);
      vars.servant_card_arash_arrow_stock = Math.max(0, data.arashArrowStock);
      vars.servant_card_royal_cannon_ammo = Math.max(0, data.royalCannonAmmo);
      vars.servant_card_gilles_spellbook_mana = finiteNonNegative(data.gillesSpellbookMana);
      vars.servant_card_gilles_spellbook_max_mana = finiteNonNegative(data.gillesSpellbookMaxMana);
      vars.servant_card_heracles_god_hand_lives = Math.max(0, data.heraclesGodHandLives);
      vars.servant_card_enkidu_transfiguration_points = safe(data.enkiduPoints);
      vars.servant_card_medusa_mystic_eyes_active = data.medusaMysticEyesActive;
      vars.servant_card_hassan_cloak_broken = data.hassanCloakBroken;
      vars.servant_card_hassan_zabaniya_animation_until = Math.max(0, data.hassanZabaniyaAnimationUntil);
      data.observedLegacyHash = legacyHash();
      data.initialized = true;
      data.changed = before != data.observedLegacyHash;
   }

   private void dirty() { if (data().changed) vars.markDirty(TypeMoonWorldModVariables.PlayerVariables.StateDomain.SERVANT_CARD); }
   private static String safe(String value) { return value == null ? "" : value; }
   private static double finiteNonNegative(double value) { return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D; }
   private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
   private static double clamp(double value, double min, double max) { return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : 0.0D; }

   public void refreshFromLegacy() { syncLegacy(); }
   public boolean transformed() { syncLegacy(); return data().transformed; }
   public String id() { syncLegacy(); return data().id; }
   public double mana() { syncLegacy(); return data().mana; }
   public double maxMana() { syncLegacy(); return data().maxMana; }
   public double manaRegen() { syncLegacy(); return data().manaRegen; }
   public int jumpCharges() { syncLegacy(); return data().jumpCharges; }
   public int jumpRecoveryTicks() { syncLegacy(); return data().jumpRecoveryTicks; }
   public long jumpRecoveryEnd() { syncLegacy(); return data().jumpRecoveryEnd; }
   public String skillCooldowns() { syncLegacy(); return data().skillCooldowns; }
   public String skillCooldownEnds() { syncLegacy(); return data().skillCooldownEnds; }
   public int npCooldown() { syncLegacy(); return data().npCooldown; }
   public long npCooldownEnd() { syncLegacy(); return data().npCooldownEnd; }
   public boolean flying() { syncLegacy(); return data().flying; }
   public int flightMode() { syncLegacy(); return data().flightMode; }
   public long highFlightUntil() { syncLegacy(); return data().highFlightUntil; }
   public long highFlightCooldownUntil() { syncLegacy(); return data().highFlightCooldownUntil; }
   public int odaFlightTicks() { syncLegacy(); return data().odaFlightTicks; }
   public long odaFlightCooldownUntil() { syncLegacy(); return data().odaFlightCooldownUntil; }
   public long odaFlightRechargeAt() { syncLegacy(); return data().odaFlightRechargeAt; }
   public double flightForward() { syncLegacy(); return data().flightForward; }
   public double flightStrafe() { syncLegacy(); return data().flightStrafe; }
   public double flightVertical() { syncLegacy(); return data().flightVertical; }
   public int flightToggleCooldown() { syncLegacy(); return data().flightToggleCooldown; }
   public int actionMode() { syncLegacy(); return data().actionMode; }
   public int transformCooldown() { syncLegacy(); return data().transformCooldown; }
   public int releaseCooldown() { syncLegacy(); return data().releaseCooldown; }
   public boolean deathRelease() { syncLegacy(); return data().deathRelease; }
   public int medeaDragonfangStock() { syncLegacy(); return data().medeaDragonfangStock; }
   public int medeaManaCharmStock() { syncLegacy(); return data().medeaManaCharmStock; }
   public int medeaHealCharmStock() { syncLegacy(); return data().medeaHealCharmStock; }
   public int paracelsusStoneStock() { syncLegacy(); return data().paracelsusStoneStock; }
   public int paracelsusDiamondShieldStock() { syncLegacy(); return data().paracelsusDiamondShieldStock; }
   public int arashArrowStock() { syncLegacy(); return data().arashArrowStock; }
   public int royalCannonAmmo() { syncLegacy(); return data().royalCannonAmmo; }
   public double gillesSpellbookMana() { syncLegacy(); return data().gillesSpellbookMana; }
   public double gillesSpellbookMaxMana() { syncLegacy(); return data().gillesSpellbookMaxMana; }
   public int heraclesGodHandLives() { syncLegacy(); return data().heraclesGodHandLives; }
   public String enkiduPoints() { syncLegacy(); return data().enkiduPoints; }
   public boolean medusaMysticEyesActive() { syncLegacy(); return data().medusaMysticEyesActive; }
   public boolean hassanCloakBroken() { syncLegacy(); return data().hassanCloakBroken; }
   public int hassanZabaniyaAnimationUntil() { syncLegacy(); return data().hassanZabaniyaAnimationUntil; }

   public void setTransformed(boolean value) { syncLegacy(); data().transformed = value; publish(); dirty(); }
   public void setId(String value) { syncLegacy(); data().id = safe(value); publish(); dirty(); }
   public void setMana(double value) { syncLegacy(); data().mana = value; publish(); dirty(); }
   public boolean hasMana(double amount) { return mana() + 1.0E-6D >= Math.max(0.0D, amount); }
   public void addMana(double amount) { setMana(Math.min(maxMana(), mana() + Math.max(0.0D, amount))); }
   public void setMaxMana(double value) { syncLegacy(); data().maxMana = value; publish(); dirty(); }
   public void setManaRegen(double value) { syncLegacy(); data().manaRegen = value; publish(); dirty(); }
   public void setJumpCharges(int value) { syncLegacy(); data().jumpCharges = value; publish(); dirty(); }
   public void setJumpRecoveryTicks(int value) { syncLegacy(); data().jumpRecoveryTicks = value; publish(); dirty(); }
   public void setJumpRecoveryEnd(long value) { syncLegacy(); data().jumpRecoveryEnd = value; publish(); dirty(); }
   public void setSkillCooldowns(String value) { syncLegacy(); data().skillCooldowns = safe(value); publish(); dirty(); }
   public void setSkillCooldownEnds(String value) { syncLegacy(); data().skillCooldownEnds = safe(value); publish(); dirty(); }
   public void setNpCooldown(int value) { syncLegacy(); data().npCooldown = value; publish(); dirty(); }
   public void setNpCooldownEnd(long value) { syncLegacy(); data().npCooldownEnd = value; publish(); dirty(); }
   public void setFlying(boolean value) { syncLegacy(); data().flying = value; publish(); dirty(); }
   public void setFlightMode(int value) { syncLegacy(); data().flightMode = value; publish(); dirty(); }
   public void setHighFlightUntil(long value) { syncLegacy(); data().highFlightUntil = value; publish(); dirty(); }
   public void setHighFlightCooldownUntil(long value) { syncLegacy(); data().highFlightCooldownUntil = value; publish(); dirty(); }
   public void setOdaFlightTicks(int value) { syncLegacy(); data().odaFlightTicks = value; publish(); dirty(); }
   public void setOdaFlightCooldownUntil(long value) { syncLegacy(); data().odaFlightCooldownUntil = value; publish(); dirty(); }
   public void setOdaFlightRechargeAt(long value) { syncLegacy(); data().odaFlightRechargeAt = value; publish(); dirty(); }
   public void setFlightInput(double forward, double strafe, double vertical) { syncLegacy(); data().flightForward = forward; data().flightStrafe = strafe; data().flightVertical = vertical; publish(); dirty(); }
   public void setFlightForward(double value) { syncLegacy(); data().flightForward = value; publish(); dirty(); }
   public void setFlightStrafe(double value) { syncLegacy(); data().flightStrafe = value; publish(); dirty(); }
   public void setFlightVertical(double value) { syncLegacy(); data().flightVertical = value; publish(); dirty(); }
   public void setFlightToggleCooldown(int value) { syncLegacy(); data().flightToggleCooldown = value; publish(); dirty(); }
   public void setActionMode(int value) { syncLegacy(); data().actionMode = value; publish(); dirty(); }
   public void setTransformCooldown(int value) { syncLegacy(); data().transformCooldown = value; publish(); dirty(); }
   public void setReleaseCooldown(int value) { syncLegacy(); data().releaseCooldown = value; publish(); dirty(); }
   public void setDeathRelease(boolean value) { syncLegacy(); data().deathRelease = value; publish(); dirty(); }
   public void setMedeaDragonfangStock(int value) { syncLegacy(); data().medeaDragonfangStock = value; publish(); dirty(); }
   public void setMedeaManaCharmStock(int value) { syncLegacy(); data().medeaManaCharmStock = value; publish(); dirty(); }
   public void setMedeaHealCharmStock(int value) { syncLegacy(); data().medeaHealCharmStock = value; publish(); dirty(); }
   public void setParacelsusStoneStock(int value) { syncLegacy(); data().paracelsusStoneStock = value; publish(); dirty(); }
   public void setParacelsusDiamondShieldStock(int value) { syncLegacy(); data().paracelsusDiamondShieldStock = value; publish(); dirty(); }
   public void setArashArrowStock(int value) { syncLegacy(); data().arashArrowStock = value; publish(); dirty(); }
   public void setRoyalCannonAmmo(int value) { syncLegacy(); data().royalCannonAmmo = value; publish(); dirty(); }
   public void setGillesSpellbookMana(double value) { syncLegacy(); data().gillesSpellbookMana = value; publish(); dirty(); }
   public void setGillesSpellbookMaxMana(double value) { syncLegacy(); data().gillesSpellbookMaxMana = value; publish(); dirty(); }
   public void setHeraclesGodHandLives(int value) { syncLegacy(); data().heraclesGodHandLives = value; publish(); dirty(); }
   public void setEnkiduPoints(String value) { syncLegacy(); data().enkiduPoints = safe(value); publish(); dirty(); }
   public void setMedusaMysticEyesActive(boolean value) { syncLegacy(); data().medusaMysticEyesActive = value; publish(); dirty(); }
   public void setHassanCloakBroken(boolean value) { syncLegacy(); data().hassanCloakBroken = value; publish(); dirty(); }
   public void setHassanZabaniyaAnimationUntil(int value) { syncLegacy(); data().hassanZabaniyaAnimationUntil = value; publish(); dirty(); }
}
