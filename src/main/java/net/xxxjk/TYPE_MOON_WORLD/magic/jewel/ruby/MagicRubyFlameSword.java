package net.xxxjk.TYPE_MOON_WORLD.magic.jewel.ruby;

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
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;

public class MagicRubyFlameSword {
    public static void execute(Entity entity) {
        if (entity == null)
            return;
        
        if (entity instanceof Player player) {
            ItemStack requiredItem = new ItemStack(ModItems.CARVED_RUBY_FULL.get());
            int requiredCount = 3;
            int count = 0;
            
            // Count items
            for (int i = 0; i < player.getInventory().items.size(); i++) {
                ItemStack stack = player.getInventory().items.get(i);
                if (stack.getItem() == requiredItem.getItem()) {
                    count += stack.getCount();
                }
            }
            if (player.getOffhandItem().getItem() == requiredItem.getItem()) {
                count += player.getOffhandItem().getCount();
            }

            if (count >= requiredCount) {
                // Consume items
                int toRemove = requiredCount;
                
                // Remove from main inventory
                for (int i = 0; i < player.getInventory().items.size(); i++) {
                    if (toRemove <= 0) break;
                    ItemStack stack = player.getInventory().items.get(i);
                    if (stack.getItem() == requiredItem.getItem()) {
                        int remove = Math.min(toRemove, stack.getCount());
                        stack.shrink(remove);
                        toRemove -= remove;
                        if (stack.isEmpty()) {
                             player.getInventory().removeItem(stack);
                        }
                    }
                }
                
                // Remove from offhand
                if (toRemove > 0) {
                     ItemStack stack = player.getOffhandItem();
                     if (stack.getItem() == requiredItem.getItem()) {
                         int remove = Math.min(toRemove, stack.getCount());
                         stack.shrink(remove);
                     }
                }

                // Fire 3 projectiles
                spawnProjectile(player);
                TYPE_MOON_WORLD.queueServerWork(5, () -> spawnProjectile(player));
                TYPE_MOON_WORLD.queueServerWork(10, () -> spawnProjectile(player));
                
            } else {
                player.displayClientMessage(Component.literal("需要3个红宝石"), true);
            }
        }
    }

    private static void spawnProjectile(Player player) {
        Level level = player.level();
        
        // Particles
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLAME, player.getX(), player.getEyeY(), player.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
        }

        RubyProjectileEntity projectile = new RubyProjectileEntity(level, player);
        projectile.setItem(new ItemStack(ModItems.CARVED_RUBY_FULL.get()));
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, MagicConstants.RUBY_THROW_VELOCITY, MagicConstants.RUBY_THROW_INACCURACY);
        level.addFreshEntity(projectile);
    }
}
