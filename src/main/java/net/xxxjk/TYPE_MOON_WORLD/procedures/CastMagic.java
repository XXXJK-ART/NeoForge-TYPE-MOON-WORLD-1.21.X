package net.xxxjk.TYPE_MOON_WORLD.procedures;

import net.minecraft.world.entity.Entity;
import net.xxxjk.TYPE_MOON_WORLD.magic.player.PlayerMagicCastService;

@SuppressWarnings("null")
public class CastMagic {
   public static void execute(Entity entity) {
      if (entity instanceof net.minecraft.world.entity.player.Player player
         && net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterCarryService.isCarryingMaster(player)) return;
      PlayerMagicCastService.execute(entity);
   }
}
