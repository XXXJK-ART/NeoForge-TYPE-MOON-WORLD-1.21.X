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
import net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidAbilityService;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardDiarmuidSkills;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.DiarmuidSpearItem;

@EventBusSubscriber(modid = "typemoonworld")
public final class DiarmuidEvents {
   private DiarmuidEvents() {
   }

   @SubscribeEvent
   public static void onEntityTick(EntityTickEvent.Post event) {
      if (event.getEntity() instanceof LivingEntity living && living.level() instanceof ServerLevel level) {
         DiarmuidCombatHelper.tickCurse(level, living);
         if (DiarmuidAbilityService.isOwner(living) || DiarmuidAbilityService.active(living)) DiarmuidAbilityService.tick(living);
      }
   }

   @SubscribeEvent
   public static void onLivingDeath(LivingDeathEvent event) {
      DiarmuidAbilityService.cancel(event.getEntity());
      if (event.getEntity() instanceof DiarmuidUaDuibhneEntity diarmuid) {
         DiarmuidCombatHelper.clearCursesFromOwner(diarmuid);
      }
   }

   @SubscribeEvent
   public static void onAttackEntity(AttackEntityEvent event) {
      Player player = event.getEntity();
      if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer && ServantCardDiarmuidSkills.isActiveCard(serverPlayer)) {
         event.setCanceled(true);
         if (player.isShiftKeyDown()) net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager.triggerAction(serverPlayer, -1);
         else ServantCardDiarmuidSkills.performOffhandSpearThrust(serverPlayer);
         return;
      }
      if (player.getMainHandItem().getItem() instanceof DiarmuidSpearItem spear && event.getTarget() instanceof LivingEntity target
         && !DiarmuidAbilityService.canHit(player, target, spear.spearType() == DiarmuidSpearItem.SpearType.GAE_DEARG ? 5 : 3)) {
         event.setCanceled(true);
      }
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

   @SubscribeEvent
   public static void onLeaveLevel(net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent event) {
      if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity owner) DiarmuidAbilityService.cancel(owner);
   }

   @SubscribeEvent
   public static void onFall(net.neoforged.neoforge.event.entity.living.LivingFallEvent event) {
      if (!event.getEntity().level().isClientSide() && DiarmuidAbilityService.protectFall(event.getEntity())) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
   public static void onVanillaSpearDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
      var source = event.getSource();
      if (source instanceof net.xxxjk.TYPE_MOON_WORLD.servant.combat.CombatDamageSource) return;
      if (source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker
         && attacker.getMainHandItem().getItem() instanceof DiarmuidSpearItem spear
         && (DiarmuidAbilityService.isOwner(attacker) || !DiarmuidAbilityService.canHit(attacker, event.getEntity(),
            spear.spearType() == DiarmuidSpearItem.SpearType.GAE_DEARG ? 5 : 3))) event.setCanceled(true);
   }
}
