package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper;

public final class ServantCardSasakiKojiroSkills {
   private ServantCardSasakiKojiroSkills() {
   }

   public static boolean performTsubameGaeshi(ServerPlayer player) {
      return PlayerNoblePhantasmHelper.useTsubameGaeshi(player);
   }
}
