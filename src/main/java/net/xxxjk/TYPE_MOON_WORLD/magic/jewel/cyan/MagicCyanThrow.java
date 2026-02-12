package net.xxxjk.TYPE_MOON_WORLD.magic.jewel.cyan;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.entity.RubyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.constants.MagicConstants;
import net.xxxjk.TYPE_MOON_WORLD.utils.GemUtils;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;

public class MagicCyanThrow {
    public static void execute(Entity entity) {
        if (entity == null)
            return;
        
        if (entity instanceof Player player) {
            ItemStack gemStack = GemUtils.consumeGem(player, GemType.CYAN);
            
            if (!gemStack.isEmpty()) {
                Level level = player.level();
                RubyProjectileEntity projectile = new RubyProjectileEntity(level, player);
                projectile.setItem(gemStack);
                projectile.setGemType(4); // 4 = Cyan
                projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, MagicConstants.RUBY_THROW_VELOCITY, MagicConstants.RUBY_THROW_INACCURACY);
                level.addFreshEntity(projectile);
            } else {
                player.displayClientMessage(Component.translatable("message.typemoonworld.magic.cyan_throw.need_gem"), true);
            }
        }
    }
}
