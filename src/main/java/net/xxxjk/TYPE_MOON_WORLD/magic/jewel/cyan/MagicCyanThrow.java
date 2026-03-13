package net.xxxjk.TYPE_MOON_WORLD.magic.jewel.cyan;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.RubyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.utils.GemUtils;

public class MagicCyanThrow {
   public static void execute(Entity entity) {
      if (entity != null) {
         if (entity instanceof ServerPlayer player) {
            ItemStack gemStack = GemUtils.consumeGem(player, GemType.CYAN);
            if (!gemStack.isEmpty()) {
               Level level = player.level();
               RubyProjectileEntity projectile = new RubyProjectileEntity(level, player);
               projectile.setItem(gemStack);
               projectile.setGemType(4);
               Vec3 direction = EntityUtils.getAutoAimDirection(player, 48.0, 55.0);
               projectile.shoot(direction.x, direction.y, direction.z, 1.5F, 1.0F);
               level.addFreshEntity(projectile);
            } else {
               player.displayClientMessage(Component.translatable("message.typemoonworld.magic.cyan_throw.need_gem"), true);
            }
         }
      }
   }
}
