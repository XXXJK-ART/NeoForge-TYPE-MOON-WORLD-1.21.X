package net.xxxjk.TYPE_MOON_WORLD.magic.jewel;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class MagicJewelRelease {
   public static void execute(Entity entity) {
      if (entity instanceof ServerPlayer player) {
         if (!MagicJewelShoot.tryUseHeldFullGem(player)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.any_gem.need"), true);
         }
      }
   }
}
