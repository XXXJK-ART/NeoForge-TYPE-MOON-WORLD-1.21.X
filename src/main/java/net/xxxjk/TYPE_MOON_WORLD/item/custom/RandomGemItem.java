package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.entity.RubyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;

public class RandomGemItem extends Item {
   public RandomGemItem(Properties properties) {
      super(properties);
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack itemstack = player.getItemInHand(hand);
      if (!level.isClientSide) {
         GemType[] types = GemType.values();
         GemType t = types[level.random.nextInt(types.length)];
         RubyProjectileEntity projectile = new RubyProjectileEntity(level, player);
         Item visualItem;
         int typeId;
         float powerMultiplier;
         if (t == GemType.RUBY) {
            visualItem = (Item)ModItems.CARVED_RUBY.get();
            typeId = 0;
            powerMultiplier = 1.0F;
         } else if (t == GemType.SAPPHIRE) {
            visualItem = (Item)ModItems.CARVED_SAPPHIRE.get();
            typeId = 1;
            powerMultiplier = 0.5F;
         } else if (t == GemType.EMERALD) {
            visualItem = (Item)ModItems.CARVED_EMERALD.get();
            typeId = 2;
            powerMultiplier = 0.5F;
         } else if (t == GemType.TOPAZ) {
            visualItem = (Item)ModItems.CARVED_TOPAZ.get();
            typeId = 3;
            powerMultiplier = 0.5F;
         } else if (t == GemType.CYAN) {
            visualItem = (Item)ModItems.CARVED_CYAN_GEMSTONE.get();
            typeId = 4;
            powerMultiplier = 0.5F;
         } else if (t == GemType.BLACK_SHARD) {
            visualItem = (Item)ModItems.CARVED_BLACK_SHARD.get();
            typeId = 6;
            powerMultiplier = 0.5F;
         } else {
            visualItem = (Item)ModItems.CARVED_WHITE_GEMSTONE.get();
            typeId = 5;
            powerMultiplier = 0.5F;
         }

         ItemStack visualStack = new ItemStack(visualItem);
         projectile.setGemType(typeId);
         CompoundTag tag = new CompoundTag();
         tag.putFloat("ExplosionPowerMultiplier", powerMultiplier);
         tag.putBoolean("IsRandomMode", true);
         visualStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
         projectile.setItem(visualStack);
         projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
         level.addFreshEntity(projectile);
         if (!player.getAbilities().instabuild) {
            itemstack.shrink(1);
         }
      }

      return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
   }
}
