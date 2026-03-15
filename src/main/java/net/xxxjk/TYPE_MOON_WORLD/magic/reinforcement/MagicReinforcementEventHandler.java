package net.xxxjk.TYPE_MOON_WORLD.magic.reinforcement;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemAttributeModifiers.Builder;
import net.minecraft.world.item.component.ItemAttributeModifiers.Entry;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;

@EventBusSubscriber(
   modid = "typemoonworld"
)
public class MagicReinforcementEventHandler {
   @SubscribeEvent
   public static void onPlayerTick(Post event) {
      Player player = event.getEntity();
      if (!player.level().isClientSide()) {
         for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.has(DataComponents.CUSTOM_DATA)) {
               checkReinforcementExpiry(player, stack);
            }
         }
      }
   }

   @SubscribeEvent
   public static void onLivingDamage(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
      if (event.getSource().getDirectEntity() instanceof Player player && !player.level().isClientSide()) {
         ItemStack stack = player.getMainHandItem();
         if (!stack.isEmpty() && stack.has(DataComponents.CUSTOM_DATA)) {
            CompoundTag tag = ((CustomData)stack.get(DataComponents.CUSTOM_DATA)).copyTag();
            if (tag.getBoolean("ReinforcementTemporary")) {
               int hits = tag.getInt("ReinforcementHitsLeft");
               if (--hits <= 0) {
                  stack.shrink(1);
                  player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
                  player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.item.destroyed"), true);
               } else {
                  tag.putInt("ReinforcementHitsLeft", hits);
                  stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                  player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.item.hits_left", hits), true);
               }
            }
         }
      }
   }

   private static void checkReinforcementExpiry(Player player, ItemStack stack) {
      CompoundTag tag = ((CustomData)stack.get(DataComponents.CUSTOM_DATA)).copyTag();
      if (tag.getBoolean("Reinforced")) {
         long startTime = tag.getLong("ReinforcementTime");
         int expiry = tag.getInt("ReinforcementExpiry");
         long currentTime = player.level().getGameTime();
         if (currentTime - startTime >= expiry) {
            removeReinforcement(player, stack, tag);
         }
      }
   }

   public static void removeReinforcement(Player player, ItemStack stack, CompoundTag tag) {
      removeStoredReinforcementEnchantment(player, stack, tag, "ReinforcedEnchantment", "ReinforcedEnchantmentLevel");
      removeStoredReinforcementEnchantment(player, stack, tag, "ReinforcedEnchantmentExtra", "ReinforcedEnchantmentExtraLevel");
      if (stack.has(DataComponents.ATTRIBUTE_MODIFIERS)) {
         ItemAttributeModifiers modifiers = (ItemAttributeModifiers)stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
         Builder builder = ItemAttributeModifiers.builder();
         boolean found = false;

         for (Entry entry : modifiers.modifiers()) {
            if (!entry.modifier().id().equals(ResourceLocation.fromNamespaceAndPath("typemoonworld", "reinforcement_item_damage"))) {
               builder.add(entry.attribute(), entry.modifier(), entry.slot());
            } else {
               found = true;
            }
         }

         if (found) {
            stack.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
         }
      }

      boolean injectedGlint = tag.getBoolean("ReinforcementInjectedGlint");
      tag.remove("Reinforced");
      tag.remove("ReinforcedLevel");
      tag.remove("ReinforcementTime");
      tag.remove("ReinforcementExpiry");
      tag.remove("ReinforcementTemporary");
      tag.remove("ReinforcementHitsLeft");
      tag.remove("ReinforcedEnchantment");
      tag.remove("ReinforcedEnchantmentLevel");
      tag.remove("ReinforcedEnchantmentExtra");
      tag.remove("ReinforcedEnchantmentExtraLevel");
      tag.remove("ReinforcementInjectedGlint");
      tag.remove("CasterUUID");
      if (injectedGlint && !shouldKeepForcedGlint(stack, tag)) {
         stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
      }

      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.item.expired"), true);
   }

   private static void removeStoredReinforcementEnchantment(Player player, ItemStack stack, CompoundTag tag, String enchantmentIdKey, String levelKey) {
      if (tag.contains(enchantmentIdKey)) {
         String enchantmentId = tag.getString(enchantmentIdKey);
         int levelToRemove = tag.getInt(levelKey);
         if (levelToRemove > 0) {
            player.registryAccess().registry(Registries.ENCHANTMENT).ifPresent(registry -> {
               ResourceLocation rl;
               try {
                  rl = ResourceLocation.parse(enchantmentId);
               } catch (Exception var6x) {
                  return;
               }

               registry.getHolder(rl).ifPresent(holder -> EnchantmentHelper.updateEnchantments(stack, mutable -> {
                  int currentLevel = mutable.getLevel(holder);
                  if (currentLevel <= levelToRemove) {
                     mutable.set(holder, 0);
                  } else {
                     mutable.set(holder, currentLevel - levelToRemove);
                  }
               }));
            });
         }
      }
   }

   private static boolean shouldKeepForcedGlint(ItemStack stack, CompoundTag tag) {
      if (!tag.contains("is_projected") && !tag.contains("is_infinite_projection")) {
         ItemEnchantments enchantments = (ItemEnchantments)stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
         return enchantments.size() > 0;
      } else {
         return true;
      }
   }
}
