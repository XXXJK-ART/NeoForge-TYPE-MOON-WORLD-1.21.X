package net.xxxjk.TYPE_MOON_WORLD.magic.jewel.topaz;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;

public class MagicTopazReinforcement {
    public static void execute(Entity entity) {
        if (entity == null)
            return;

        if (entity instanceof Player player) {
            ItemStack requiredItem = new ItemStack(ModItems.CARVED_TOPAZ_FULL.get());
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
                if (toRemove > 0) {
                    ItemStack stack = player.getOffhandItem();
                    if (stack.getItem() == requiredItem.getItem()) {
                        int remove = Math.min(toRemove, stack.getCount());
                        stack.shrink(remove);
                    }
                }

                // Apply Buffs (60s = 1200 ticks)
                // "Three Reinforcement" - Strength, Speed, Resistance
                if (player instanceof LivingEntity living) {
                    living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 1)); // Strength II
                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 1)); // Speed II
                    living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 1)); // Resistance II
                    living.addEffect(new MobEffectInstance(MobEffects.JUMP, 1200, 1)); // Jump Boost II (Bonus)
                    
                    // Particles
                    Level level = player.level();
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1, player.getZ(), 30, 0.5, 1, 0.5, 0.1);
                        serverLevel.sendParticles(ParticleTypes.WAX_ON, player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, 1, 0.5, 0.1);
                    }
                }

            } else {
                player.displayClientMessage(Component.literal("需要3个黄宝石"), true);
            }
        }
    }
}
