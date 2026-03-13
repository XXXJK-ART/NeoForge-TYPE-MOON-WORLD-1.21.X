package net.xxxjk.TYPE_MOON_WORLD.magic.broken_phantasm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.BrokenPhantasmProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.AvalonItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.NoblePhantasmItem;
import net.xxxjk.TYPE_MOON_WORLD.magic.projection.MagicProjection;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public class MagicBrokenPhantasm {
   private static void executeMode(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      ItemStack heldItem = player.getMainHandItem();
      boolean isMainHand = true;
      if (!isValidBPItem(heldItem)) {
         ItemStack offhandItem = player.getOffhandItem();
         if (!isValidBPItem(offhandItem)) {
            if (!heldItem.isEmpty() && !offhandItem.isEmpty()) {
               player.displayClientMessage(Component.translatable("message.typemoonworld.broken_phantasm.no_valid_item"), true);
            } else {
               MagicProjection.execute(player);
            }

            return;
         }

         heldItem = offhandItem;
         isMainHand = false;
      }

      double cost = calculateCost(heldItem, false);
      float explosionPower = (float)(cost / 20.0);
      BrokenPhantasmProjectileEntity projectile = new BrokenPhantasmProjectileEntity(player.level(), player, heldItem.copy());
      projectile.setItem(heldItem.copy());
      projectile.setExplosionPower(explosionPower);
      Vec3 direction = EntityUtils.getAutoAimDirection(player, 48.0, 60.0);
      projectile.shoot(direction.x, direction.y, direction.z, 3.0F, 1.0F);
      player.level().addFreshEntity(projectile);
      heldItem.shrink(1);
      player.displayClientMessage(
         Component.translatable("message.typemoonworld.broken_phantasm.cast", new Object[]{String.format("%.1f", explosionPower)}), true
      );
   }

   public static void execute(Entity entity) {
      if (entity instanceof ServerPlayer player) {
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         executeMode(player, vars);
      }
   }

   private static boolean isValidBPItem(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else {
         if (stack.has(DataComponents.CUSTOM_DATA)) {
            CustomData customData = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
               CompoundTag tag = customData.copyTag();
               if (tag.contains("is_projected")) {
                  return true;
               }
            }
         }

         return stack.getItem() instanceof NoblePhantasmItem && !(stack.getItem() instanceof AvalonItem);
      }
   }

   private static boolean isProjectedItem(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else {
         if (stack.has(DataComponents.CUSTOM_DATA)) {
            CustomData customData = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
               CompoundTag tag = customData.copyTag();
               return tag.contains("is_projected");
            }
         }

         return false;
      }
   }

   public static double calculateCost(ItemStack stack, boolean hasSwordAttribute) {
      double baseCost = 10.0;
      if (stack.getItem() instanceof NoblePhantasmItem) {
         baseCost = 500.0;
      } else {
         Rarity rarity = stack.getRarity();
         if (rarity == Rarity.UNCOMMON) {
            baseCost = 50.0;
         } else if (rarity == Rarity.RARE) {
            baseCost = 100.0;
         } else if (rarity == Rarity.EPIC) {
            baseCost = 300.0;
         }
      }

      int enchantCount = stack.getEnchantments().size();
      if (enchantCount > 0) {
         baseCost *= 1.0 + enchantCount * 0.2;
      }

      if (stack.getMaxDamage() > 0) {
         baseCost *= 1.0 + stack.getMaxDamage() / 1000.0;
      }

      if (stack.getItem() instanceof BlockItem blockItem) {
         BlockState state = blockItem.getBlock().defaultBlockState();
         float hardness = state.getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
         if (hardness > 0.0F) {
            baseCost += hardness * 5.0F;
         }
      }

      ItemAttributeModifiers modifiers = (ItemAttributeModifiers)stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
      double damage = modifiers.compute(0.0, EquipmentSlot.MAINHAND);
      if (damage > 0.0) {
         baseCost += damage * 5.0;
      }

      if (hasSwordAttribute) {
         boolean isSword = stack.getItem() instanceof SwordItem;
         if (isSword) {
            baseCost *= 0.1;
         } else {
            baseCost *= 0.5;
         }
      }

      return baseCost;
   }
}
