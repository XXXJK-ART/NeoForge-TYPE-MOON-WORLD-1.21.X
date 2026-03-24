package net.xxxjk.TYPE_MOON_WORLD.procedures;

import net.minecraft.world.entity.Entity;
import net.xxxjk.TYPE_MOON_WORLD.magic.player.PlayerMagicCastService;

@SuppressWarnings("null")
public class CastMagic {
   public static void execute(Entity entity) {
      PlayerMagicCastService.execute(entity);
   }
}
