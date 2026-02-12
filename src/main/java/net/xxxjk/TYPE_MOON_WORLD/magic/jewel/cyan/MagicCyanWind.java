package net.xxxjk.TYPE_MOON_WORLD.magic.jewel.cyan;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.constants.MagicConstants;
import net.xxxjk.TYPE_MOON_WORLD.entity.RubyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.utils.GemUtils;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.FullManaCarvedGemItem;

public class MagicCyanWind {
    public static void execute(Entity entity) {
        if (entity == null)
            return;
        
        if (entity instanceof Player player) {
            // Count total cyan gems
            int count = 0;
            for (int i = 0; i < player.getInventory().items.size(); i++) {
                ItemStack stack = player.getInventory().items.get(i);
                if (stack.getItem() instanceof FullManaCarvedGemItem gemItem && gemItem.getType() == GemType.CYAN) {
                    count += stack.getCount();
                }
            }
            ItemStack offhand = player.getOffhandItem();
            if (offhand.getItem() instanceof FullManaCarvedGemItem gemItem && gemItem.getType() == GemType.CYAN) {
                count += offhand.getCount();
            }

            if (count >= 3) {
                // Consume 3 gems
                ItemStack gem1 = GemUtils.consumeGem(player, GemType.CYAN);
                ItemStack gem2 = GemUtils.consumeGem(player, GemType.CYAN);
                ItemStack gem3 = GemUtils.consumeGem(player, GemType.CYAN);

                Level level = player.level();

                // Fire 3 projectiles in a fan (Storm/Wind)
                // Center
                spawnProjectile(player, gem1, 0.0f);
                // Left
                spawnProjectile(player, gem2, -15.0f);
                // Right
                spawnProjectile(player, gem3, 15.0f);
                
                // Wind Particles
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getEyeY(), player.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
                }

            } else {
                player.displayClientMessage(Component.translatable("message.typemoonworld.magic.cyan_wind.need_gem"), true);
            }
        }
    }

    private static void spawnProjectile(Player player, ItemStack gemStack, float yRotOffset) {
        if (gemStack.isEmpty()) return;
        
        Level level = player.level();
        RubyProjectileEntity projectile = new RubyProjectileEntity(level, player);
        projectile.setItem(gemStack);
        projectile.setGemType(4); // 4 = Cyan
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot() + yRotOffset, 0.0F, MagicConstants.RUBY_THROW_VELOCITY, MagicConstants.RUBY_THROW_INACCURACY);
        level.addFreshEntity(projectile);
    }
}
