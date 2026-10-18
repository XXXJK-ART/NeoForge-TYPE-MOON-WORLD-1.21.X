package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import java.util.Set;

/** Shared release policy for all NP entry points. */
public final class NoblePhantasmCastRules {
   private static final Set<String> INSTANT = Set.of(
      "okita_mumyoudan_zuki", "cursed_arm_hassan_zabaniya", "li_shuwen_wu_er_da", "lancelot_arondight"
   );
   private static final Set<String> INTERRUPTIBLE = Set.of(
      "artoria_excalibur", "gilgamesh_ea", "cu_gae_bolg", "gawain_gallatin", "muramasa_tsumukari",
      "medusa_bellerophon", "paracelsus_elemental_sword", "jeanne_alter_np", "nightingale_pledge",
      "baobhan_sith_failnaught",
      "zhao_yun_changbanpo", "oda_three_thousand", "oda_hajun", "muramasa_summon"
   );
   private static final int INTERRUPT_WINDOW_START_TICKS = 80;
   private static final int FINAL_IMMUNE_TICKS = 40;
   private static final int RELEASE_LEAD_TICKS = FINAL_IMMUNE_TICKS + 20;

   private NoblePhantasmCastRules() { }
   public static boolean instant(String id) { return id != null && INSTANT.contains(id); }
   public static boolean interruptible(String id) { return id != null && INTERRUPTIBLE.contains(id); }
   public static boolean immune(String id) { return !interruptible(id); }
   /** Number of ticks before the voice ends when the interrupt window opens. */
   public static int interruptWindowStartTicks() { return INTERRUPT_WINDOW_START_TICKS; }
   /** Number of ticks before the voice ends when the NP effect is released. */
   public static int releaseLeadTicks() { return RELEASE_LEAD_TICKS; }
   /** Number of ticks at the end of the voice that cannot be interrupted. */
   public static int finalImmuneTicks() { return FINAL_IMMUNE_TICKS; }
   public static int windowTicks() { return INTERRUPT_WINDOW_START_TICKS - RELEASE_LEAD_TICKS; }
   public static long releaseAt(long voiceStart, long voiceEnd) {
      return Math.max(voiceStart + 1L, voiceEnd - RELEASE_LEAD_TICKS);
   }
   public static boolean interruptibleAt(long now, long voiceStart, long voiceEnd) {
      if (voiceEnd - voiceStart <= INTERRUPT_WINDOW_START_TICKS) return false;
      long windowStart = Math.max(voiceStart, voiceEnd - INTERRUPT_WINDOW_START_TICKS);
      return now >= windowStart && now < releaseAt(voiceStart, voiceEnd);
   }
   public static boolean effectReleased(long now, long voiceEnd) {
      return voiceEnd > 0 && now >= releaseAt(0, voiceEnd);
   }
   public static double interruptedCost(double fullCost) { return Math.max(0, fullCost) * 0.5; }
   public static int interruptedCooldown(int fullTicks) { return Math.max(0, fullTicks / 2); }
}
