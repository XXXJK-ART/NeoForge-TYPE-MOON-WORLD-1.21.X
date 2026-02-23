package net.xxxjk.TYPE_MOON_WORLD.magic.jewel.cyan;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
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
                ItemStack gem1 = GemUtils.consumeGem(player, GemType.CYAN);
                ItemStack gem2 = GemUtils.consumeGem(player, GemType.CYAN);
                ItemStack gem3 = GemUtils.consumeGem(player, GemType.CYAN);

                Level level = player.level();

                float multiplier1 = 1.0f;
                if (gem1.getItem() instanceof FullManaCarvedGemItem fullGem) {
                    multiplier1 = fullGem.getQuality().getEffectMultiplier();
                }
                float multiplier2 = 1.0f;
                if (gem2.getItem() instanceof FullManaCarvedGemItem fullGem2) {
                    multiplier2 = fullGem2.getQuality().getEffectMultiplier();
                }
                float multiplier3 = 1.0f;
                if (gem3.getItem() instanceof FullManaCarvedGemItem fullGem3) {
                    multiplier3 = fullGem3.getQuality().getEffectMultiplier();
                }
                float avgMultiplier = (multiplier1 + multiplier2 + multiplier3) / 3.0f;

                float baseRadius = MagicConstants.CYAN_WIND_RADIUS;
                float radius = baseRadius * 1.5f * avgMultiplier;
                int baseDuration = MagicConstants.CYAN_WIND_DURATION;
                int duration = (int)(baseDuration * (1.0f + 0.5f * avgMultiplier));

                ItemStack projectileStack = gem1.copy();
                projectileStack.setCount(1);
                CompoundTag tag = new CompoundTag();
                CustomData existing = projectileStack.get(DataComponents.CUSTOM_DATA);
                if (existing != null) {
                    tag = existing.copyTag();
                }
                tag.putBoolean("IsCyanTornado", true);
                tag.putFloat("CyanRadius", radius);
                tag.putInt("CyanDuration", duration);
                projectileStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

                RubyProjectileEntity projectile = new RubyProjectileEntity(level, player);
                projectile.setItem(projectileStack);
                projectile.setGemType(4);
                projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, MagicConstants.RUBY_THROW_VELOCITY, MagicConstants.RUBY_THROW_INACCURACY);
                level.addFreshEntity(projectile);
                
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getEyeY(), player.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
                }

            } else {
                player.displayClientMessage(Component.translatable("message.typemoonworld.magic.cyan_wind.need_gem"), true);
            }
        }
    }
}
