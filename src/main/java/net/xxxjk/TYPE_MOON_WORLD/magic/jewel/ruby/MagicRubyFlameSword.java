
package net.xxxjk.TYPE_MOON_WORLD.magic.jewel.ruby;

import java.util.ArrayList;
import java.util.List;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.constants.MagicConstants;
import net.xxxjk.TYPE_MOON_WORLD.entity.RubyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.utils.GemUtils;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.FullManaCarvedGemItem;

public class MagicRubyFlameSword {
    public static void execute(Entity entity) {
        if (entity == null)
            return;
        
        if (entity instanceof Player player) {
            // Count total rubies
            int count = 0;
            for (int i = 0; i < player.getInventory().items.size(); i++) {
                ItemStack stack = player.getInventory().items.get(i);
                if (stack.getItem() instanceof FullManaCarvedGemItem gemItem && gemItem.getType() == GemType.RUBY) {
                    count += stack.getCount();
                }
            }
            ItemStack offhand = player.getOffhandItem();
            if (offhand.getItem() instanceof FullManaCarvedGemItem gemItem && gemItem.getType() == GemType.RUBY) {
                count += offhand.getCount();
            }

            if (count >= 3) {
                // Consume 3 gems
                ItemStack gem1 = GemUtils.consumeGem(player, GemType.RUBY);
                ItemStack gem2 = GemUtils.consumeGem(player, GemType.RUBY);
                ItemStack gem3 = GemUtils.consumeGem(player, GemType.RUBY);

                // Fire 3 projectiles
                spawnProjectile(player, gem1);
                TYPE_MOON_WORLD.queueServerWork(5, () -> spawnProjectile(player, gem2));
                TYPE_MOON_WORLD.queueServerWork(10, () -> spawnProjectile(player, gem3));
                
            } else {
                player.displayClientMessage(Component.translatable(MagicConstants.MSG_MAGIC_RUBY_FLAME_SWORD_NEED_GEM), true);
            }
        }
    }

    private static void spawnProjectile(Player player, ItemStack gemStack) {
        if (gemStack.isEmpty()) return;
        
        Level level = player.level();
        
        // Particles
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLAME, player.getX(), player.getEyeY(), player.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
        }

        RubyProjectileEntity projectile = new RubyProjectileEntity(level, player);
        projectile.setItem(gemStack);
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, MagicConstants.RUBY_THROW_VELOCITY, MagicConstants.RUBY_THROW_INACCURACY);
        level.addFreshEntity(projectile);
    }
}
