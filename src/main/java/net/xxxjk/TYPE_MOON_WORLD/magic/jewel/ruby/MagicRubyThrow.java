package net.xxxjk.TYPE_MOON_WORLD.magic.jewel.ruby;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.entity.RubyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.constants.MagicConstants;

import net.xxxjk.TYPE_MOON_WORLD.utils.GemUtils;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;

public class MagicRubyThrow {
    public static void executeRandom(Entity entity, int type) {
        if (entity == null)
            return;
        
        if (entity instanceof Player player) {
            // Include WHITE_GEMSTONE (which is likely the 5th value if it exists, or index 4)
            // GemType.values() probably has RUBY, SAPPHIRE, EMERALD, TOPAZ, WHITE_GEMSTONE
            // type passed here is 4 for Random mode.
            // We want to pick a random gem from available types to consume.
            // Or just try to consume ANY gem.
            
            // Randomly shuffle types to try
            GemType[] types = GemType.values();
            // Shuffle or just iterate randomly?
            // Let's pick a random starting index and loop
            int startIndex = player.getRandom().nextInt(types.length);
            
            for (int i = 0; i < types.length; i++) {
                int index = (startIndex + i) % types.length;
                GemType t = types[index];
                
                ItemStack stack = GemUtils.consumeGem(player, t);
                if (!stack.isEmpty()) {
                    // Found a gem!
                    Level level = player.level();
                    RubyProjectileEntity projectile = new RubyProjectileEntity(level, player);
                    
                    // Determine internal type ID for renderer
                    // 0: Ruby, 1: Sapphire, 2: Emerald, 3: Topaz, 4: Cyan, 5: White
                    int typeId = 0;
                    if (t == GemType.RUBY) typeId = 0;
                    else if (t == GemType.SAPPHIRE) typeId = 1;
                    else if (t == GemType.EMERALD) typeId = 2;
                    else if (t == GemType.TOPAZ) typeId = 3;
                    else if (t == GemType.CYAN) typeId = 4;
                    else typeId = 5; // White
                    
                    projectile.setGemType(typeId);
                    
                    // Power: Ruby is 1.0, others 0.5. White is also 0.5?
                    float powerMultiplier = (t == GemType.RUBY) ? 1.0f : 0.5f;
                    
                    net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
                    net.minecraft.world.item.component.CustomData existingData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
                    if (existingData != null) {
                        tag = existingData.copyTag();
                    }
                    
                    tag.putFloat("ExplosionPowerMultiplier", powerMultiplier);
                    tag.putBoolean("IsRandomMode", true);
                    stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
                    
                    projectile.setItem(stack);
                    projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, MagicConstants.RUBY_THROW_VELOCITY, MagicConstants.RUBY_THROW_INACCURACY);
                    level.addFreshEntity(projectile);
                    return;
                }
            }
            
            player.displayClientMessage(Component.translatable(MagicConstants.MSG_MAGIC_ANY_GEM_NEED), true);
        }
    }

    public static void execute(Entity entity) {
        if (entity == null)
            return;
        
        if (entity instanceof Player player) {
            ItemStack gemStack = GemUtils.consumeGem(player, GemType.RUBY);
            
            if (!gemStack.isEmpty()) {
                Level level = player.level();
                RubyProjectileEntity projectile = new RubyProjectileEntity(level, player);
                projectile.setItem(gemStack);
                // 提高投掷速度
                projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, MagicConstants.RUBY_THROW_VELOCITY, MagicConstants.RUBY_THROW_INACCURACY);
                level.addFreshEntity(projectile);
            } else {
                player.displayClientMessage(Component.translatable(MagicConstants.MSG_MAGIC_RUBY_THROW_NEED_GEM), true);
            }
        }
    }
}
