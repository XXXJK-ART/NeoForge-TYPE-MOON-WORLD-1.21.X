package net.xxxjk.TYPE_MOON_WORLD.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.DiarmuidUaDuibhneEntity;

@EventBusSubscriber(modid = "typemoonworld")
public final class DiarmuidEvents {
   private DiarmuidEvents() {
   }

   @SubscribeEvent
   public static void onEntityTick(EntityTickEvent.Post event) {
      if (event.getEntity() instanceof LivingEntity living && living.level() instanceof ServerLevel level) {
         DiarmuidCombatHelper.tickCurse(level, living);
      }
   }

   @SubscribeEvent
   public static void onLivingDeath(LivingDeathEvent event) {
      if (event.getEntity() instanceof DiarmuidUaDuibhneEntity diarmuid) {
         DiarmuidCombatHelper.clearCursesFromOwner(diarmuid);
      }
   }

   @SubscribeEvent
   public static void onAttackEntity(AttackEntityEvent event) {
      Player player = event.getEntity();
      if (DiarmuidCombatHelper.hasDisabledRightHand(player)) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
      if (handDisabled(event.getEntity(), event.getHand())) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
      if (handDisabled(event.getEntity(), event.getHand())) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
      if (handDisabled(event.getEntity(), event.getHand())) {
         event.setCanceled(true);
      }
   }

   private static boolean handDisabled(Player player, InteractionHand hand) {
      return hand == InteractionHand.MAIN_HAND
         ? DiarmuidCombatHelper.hasDisabledRightHand(player)
         : DiarmuidCombatHelper.hasDisabledLeftHand(player);
   }
}
