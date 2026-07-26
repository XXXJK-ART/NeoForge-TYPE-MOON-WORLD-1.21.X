package net.xxxjk.TYPE_MOON_WORLD.magic.church;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.BlackKeyItem;

public final class MagicBlackKeyFireEngraving {
   private MagicBlackKeyFireEngraving() {}

   public static boolean execute(Entity caster) {
      if (!(caster instanceof ServerPlayer player)) return false;
      ItemStack stack = player.getMainHandItem();
      if (!(stack.getItem() instanceof BlackKeyItem)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.black_key.not_held"), true);
         return false;
      }
      if (BlackKeyItem.isFireEngraved(stack)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.black_key.already_engraved"), true);
         return false;
      }
      return BlackKeyItem.engraveWithFire(player, stack);
   }
}
