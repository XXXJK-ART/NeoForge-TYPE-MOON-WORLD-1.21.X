package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import java.util.Set;

/** Shared release policy for all NP entry points. */
public final class NoblePhantasmCastRules {
   private static final Set<String> INTERRUPTIBLE = Set.of(
      "artoria_excalibur", "gilgamesh_ea", "cu_gae_bolg", "gawain_gallatin", "muramasa_tsumukari",
      "medusa_bellerophon", "paracelsus_elemental_sword", "jeanne_alter_np", "nightingale_pledge",
      "okita_mumyoudan_zuki", "li_shuwen_wu_er_da", "cursed_arm_hassan_zabaniya", "baobhan_sith_failnaught",
      "zhao_yun_changbanpo", "oda_three_thousand", "oda_hajun", "muramasa_summon", "lancelot_arondight"
   );
   private NoblePhantasmCastRules() { }
   public static boolean interruptible(String id) { return id != null && INTERRUPTIBLE.contains(id); }
   public static boolean immune(String id) { return !interruptible(id); }
   public static int windowTicks() { return 20; }
   public static boolean effectReleased(long now, long voiceEnd) { return voiceEnd > 0 && now >= voiceEnd; }
   public static double interruptedCost(double fullCost) { return Math.max(0, fullCost) * 0.5; }
   public static int interruptedCooldown(int fullTicks) { return Math.max(0, fullTicks / 2); }
}
