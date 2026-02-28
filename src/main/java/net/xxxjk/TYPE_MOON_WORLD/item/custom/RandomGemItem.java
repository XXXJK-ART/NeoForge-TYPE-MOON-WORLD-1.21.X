package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.constants.MagicConstants;
import net.xxxjk.TYPE_MOON_WORLD.entity.RubyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;

public class RandomGemItem extends Item {
    public RandomGemItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            // Pick random type
            GemType[] types = GemType.values();
            GemType t = types[level.random.nextInt(types.length)];
            
            // Spawn projectile
            RubyProjectileEntity projectile = new RubyProjectileEntity(level, player);
            
            // Set visual item
            Item visualItem;
            int typeId;
            float powerMultiplier;

            if (t == GemType.RUBY) {
                visualItem = ModItems.CARVED_RUBY.get();
                typeId = 0;
                powerMultiplier = 1.0f;
            } else if (t == GemType.SAPPHIRE) {
                visualItem = ModItems.CARVED_SAPPHIRE.get();
                typeId = 1;
                powerMultiplier = 0.5f;
            } else if (t == GemType.EMERALD) {
                visualItem = ModItems.CARVED_EMERALD.get();
                typeId = 2;
                powerMultiplier = 0.5f;
            } else if (t == GemType.TOPAZ) {
                visualItem = ModItems.CARVED_TOPAZ.get();
                typeId = 3;
                powerMultiplier = 0.5f;
            } else if (t == GemType.CYAN) {
                visualItem = ModItems.CARVED_CYAN_GEMSTONE.get();
                typeId = 4;
                powerMultiplier = 0.5f;
            } else if (t == GemType.BLACK_SHARD) {
                visualItem = ModItems.CARVED_BLACK_SHARD.get();
                typeId = 6;
                powerMultiplier = 0.5f;
            } else { // WHITE_GEMSTONE
                visualItem = ModItems.CARVED_WHITE_GEMSTONE.get();
                typeId = 5;
                powerMultiplier = 0.5f;
            }
            
            ItemStack visualStack = new ItemStack(visualItem);
            
            projectile.setGemType(typeId);
            
            // Set NBT
            CompoundTag tag = new CompoundTag();
            tag.putFloat("ExplosionPowerMultiplier", powerMultiplier);
            tag.putBoolean("IsRandomMode", true);
            visualStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            
            projectile.setItem(visualStack);
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, MagicConstants.RUBY_THROW_VELOCITY, MagicConstants.RUBY_THROW_INACCURACY);
            level.addFreshEntity(projectile);
            
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }
}
