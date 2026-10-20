package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.world.damagesource.DamageSource;
import net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SasakiKojiroCombatService;

/** Rank-based Souwa defense bypass shared by player cards and NPC defenders. */
public final class SowaExpertiseHelper {
   private SowaExpertiseHelper() { }

   public static boolean rollBypass(DamageSource source) {
      return SasakiKojiroCombatService.rollDefenseBypass(source);
   }
}
