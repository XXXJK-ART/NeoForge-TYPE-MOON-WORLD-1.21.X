
package net.xxxjk.TYPE_MOON_WORLD.magic.jewel.sapphire;

import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.constants.MagicConstants;
import net.xxxjk.TYPE_MOON_WORLD.entity.SapphireProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.utils.GemUtils;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;

public class MagicSapphireThrow {
    public static void execute(Entity entity) {
        if (entity == null)
            return;
        
        if (entity instanceof Player player) {
            ItemStack gemStack = GemUtils.consumeGem(player, GemType.SAPPHIRE);
            
            if (!gemStack.isEmpty()) {
                Level level = player.level();
                SapphireProjectileEntity projectile = new SapphireProjectileEntity(level, player);
                projectile.setItem(gemStack);
                projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, MagicConstants.SAPPHIRE_THROW_VELOCITY, MagicConstants.SAPPHIRE_THROW_INACCURACY);
                level.addFreshEntity(projectile);
            } else {
                player.displayClientMessage(Component.translatable(MagicConstants.MSG_MAGIC_SAPPHIRE_THROW_NEED_GEM), true);
            }
        }
    }
}
