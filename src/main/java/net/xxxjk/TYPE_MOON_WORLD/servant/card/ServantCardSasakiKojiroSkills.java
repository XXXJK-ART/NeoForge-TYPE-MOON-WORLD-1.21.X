package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SasakiKojiroCombatService;
import net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SasakiKojiroRules.Technique;

public final class ServantCardSasakiKojiroSkills {
   private ServantCardSasakiKojiroSkills() {
   }

   public static void tick(ServerPlayer player) {
      SasakiKojiroCombatService.tick(player);
   }

   public static boolean canPerform(ServerPlayer player, Technique technique) {
      return SasakiKojiroCombatService.canPerform(player, technique);
   }

   public static boolean perform(ServerPlayer player, Technique technique) {
      return SasakiKojiroCombatService.perform(player, technique);
   }

   public static boolean enablePresenceConcealment(ServerPlayer player) {
      if (!SasakiKojiroCombatService.setConcealed(player, true)) return false;
      net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SasakiKojiroActionAnimationService.play(player,
         net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SasakiKojiroAction.PRESENCE_CONCEALMENT);
      return true;
   }

   public static boolean performTransparency(ServerPlayer player) {
      return SasakiKojiroCombatService.performTransparency(player);
   }
}
