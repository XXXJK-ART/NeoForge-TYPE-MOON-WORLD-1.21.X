package net.xxxjk.TYPE_MOON_WORLD.event;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

@EventBusSubscriber(
   modid = "typemoonworld"
)
public class ModPlayerEventHandler {
   private static boolean isModItem(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else {
         ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
         return id != null && "typemoonworld".equals(id.getNamespace());
      }
   }

   private static boolean checkMagus(Player player) {
      if (player.level().isClientSide()) {
         return true;
      } else if (player.isSpectator()) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.spectator_no_mod_item"), true);
         return false;
      } else {
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (!vars.is_magus) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.not_magus_interaction"), true);
            return false;
         } else {
            return true;
         }
      }
   }

   @SubscribeEvent
   public static void onRightClickItem(RightClickItem event) {
      if (!event.getLevel().isClientSide()) {
         if (isModItem(event.getItemStack()) && !checkMagus(event.getEntity())) {
            event.setCanceled(true);
         }
      }
   }

   @SubscribeEvent
   public static void onRightClickBlock(RightClickBlock event) {
      if (!event.getLevel().isClientSide()) {
         if (isModItem(event.getItemStack()) && !checkMagus(event.getEntity())) {
            event.setCanceled(true);
         }
      }
   }

   @SubscribeEvent
   public static void onEntityInteract(EntityInteract event) {
      if (!event.getLevel().isClientSide()) {
         if (isModItem(event.getItemStack()) && !checkMagus(event.getEntity())) {
            event.setCanceled(true);
         }
      }
   }

   @SubscribeEvent
   public static void onLeftClickBlock(LeftClickBlock event) {
      if (!event.getLevel().isClientSide()) {
         if (isModItem(event.getItemStack()) && !checkMagus(event.getEntity())) {
            event.setCanceled(true);
         }
      }
   }
}
