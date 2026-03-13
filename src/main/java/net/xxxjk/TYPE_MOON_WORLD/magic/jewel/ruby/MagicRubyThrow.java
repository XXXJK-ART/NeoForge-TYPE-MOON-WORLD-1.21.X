package net.xxxjk.TYPE_MOON_WORLD.magic.jewel.ruby;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.RubyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.utils.GemUtils;

public class MagicRubyThrow {
   private static final String RANDOM_COOLDOWN_TAG = "TypeMoonRandomJewelNextTick";
   private static final int RANDOM_COOLDOWN_TICKS = 10;

   public static boolean executeRandom(Entity entity, int type) {
      if (entity == null) {
         return false;
      } else {
         if (entity instanceof ServerPlayer player) {
            long now = player.level().getGameTime();
            long nextAllowed = player.getPersistentData().getLong("TypeMoonRandomJewelNextTick");
            if (now < nextAllowed) {
               return false;
            }

            GemType[] types = GemType.values();
            int startIndex = player.getRandom().nextInt(types.length);

            for (int i = 0; i < types.length; i++) {
               int index = (startIndex + i) % types.length;
               GemType t = types[index];
               ItemStack stack = GemUtils.consumeGem(player, t);
               if (!stack.isEmpty()) {
                  Level level = player.level();
                  RubyProjectileEntity projectile = new RubyProjectileEntity(level, player);
                  int typeId = 0;
                  byte var20;
                  if (t == GemType.RUBY) {
                     var20 = 0;
                  } else if (t == GemType.SAPPHIRE) {
                     var20 = 1;
                  } else if (t == GemType.EMERALD) {
                     var20 = 2;
                  } else if (t == GemType.TOPAZ) {
                     var20 = 3;
                  } else if (t == GemType.CYAN) {
                     var20 = 4;
                  } else if (t == GemType.BLACK_SHARD) {
                     var20 = 6;
                  } else {
                     var20 = 5;
                  }

                  projectile.setGemType(var20);
                  float powerMultiplier = t == GemType.RUBY ? 1.0F : 0.5F;
                  CompoundTag tag = new CompoundTag();
                  CustomData existingData = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
                  if (existingData != null) {
                     tag = existingData.copyTag();
                  }

                  tag.putFloat("ExplosionPowerMultiplier", powerMultiplier);
                  tag.putBoolean("IsRandomMode", true);
                  stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                  projectile.setItem(stack);
                  Vec3 direction = EntityUtils.getAutoAimDirection(player, 48.0, 55.0);
                  projectile.shoot(direction.x, direction.y, direction.z, 1.5F, 1.0F);
                  level.addFreshEntity(projectile);
                  player.getPersistentData().putLong("TypeMoonRandomJewelNextTick", now + 10L);
                  return true;
               }
            }

            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.any_gem.need"), true);
         }

         return false;
      }
   }

   public static void execute(Entity entity) {
      if (entity != null) {
         if (entity instanceof ServerPlayer player) {
            ItemStack gemStack = GemUtils.consumeGem(player, GemType.RUBY);
            if (!gemStack.isEmpty()) {
               Level level = player.level();
               RubyProjectileEntity projectile = new RubyProjectileEntity(level, player);
               projectile.setItem(gemStack);
               Vec3 direction = EntityUtils.getAutoAimDirection(player, 48.0, 55.0);
               projectile.shoot(direction.x, direction.y, direction.z, 1.5F, 1.0F);
               level.addFreshEntity(projectile);
            } else {
               player.displayClientMessage(Component.translatable("message.typemoonworld.magic.ruby_throw.need_gem"), true);
            }
         }
      }
   }
}
