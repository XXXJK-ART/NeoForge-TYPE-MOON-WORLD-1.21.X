package net.xxxjk.TYPE_MOON_WORLD.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.servant.cu.*;

@EventBusSubscriber(modid="typemoonworld")
public final class CuChulainnEvents {
   private CuChulainnEvents() { }
   @SubscribeEvent public static void tick(EntityTickEvent.Post event) {
      if(event.getEntity() instanceof LivingEntity owner && owner.level() instanceof ServerLevel) {
         if(CuChulainnAbilityService.isOwner(owner) || CuChulainnAbilityService.active(owner) || CuChulainnWeaponService.busy(owner) || CuSpearLedger.outstanding(owner)) CuChulainnAbilityService.tick(owner);
         else if(owner.getPersistentData().contains("TypeMoonDisengageUntil"))net.xxxjk.TYPE_MOON_WORLD.servant.combat.DisengageService.tick(owner);
      }
   }
   @SubscribeEvent public static void tracking(net.neoforged.neoforge.event.entity.player.PlayerEvent.StartTracking event) {
      if(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer viewer && event.getTarget() instanceof LivingEntity owner
         && CuChulainnAbilityService.isOwner(owner))CuChulainnVisualService.syncTo(owner,viewer);
   }
   @SubscribeEvent(priority=EventPriority.HIGH) public static void damage(LivingIncomingDamageEvent event) {
      if(event.getSource().getEntity() instanceof LivingEntity owner && CuChulainnAbilityService.isOwner(owner))
         event.setAmount((float)(event.getAmount()*CuChulainnRules.fourBranches(owner.getHealth(),owner.getMaxHealth())));
   }
   @SubscribeEvent public static void actualDamage(LivingDamageEvent.Post event) {
      if(event.getNewDamage()<=0)return;
      CuChulainnAbilityService.actualDamage(event.getEntity());
      if(event.getSource().getEntity() instanceof LivingEntity owner)CuChulainnAbilityService.actualDamage(owner);
   }
   @SubscribeEvent public static void leave(EntityLeaveLevelEvent event) {
      if(!event.getLevel().isClientSide() && event.getEntity() instanceof net.xxxjk.TYPE_MOON_WORLD.entity.CuSpearEntity spear) {CuSpearLedger.pending(spear);return;}
      if(event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity owner))return;
      CuSpearLedger.ownerLeft(owner);CuChulainnWeaponService.clear(owner);
   }
   @SubscribeEvent public static void dimension(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
      if(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer owner){CuSpearLedger.ownerLeft(owner);CuChulainnWeaponService.clear(owner);}
   }
   @SubscribeEvent public static void death(LivingDeathEvent event) {
      if(event.getEntity().level().isClientSide())return;
      if(CuChulainnAbilityService.isOwner(event.getEntity()))CuChulainnAbilityService.clear(event.getEntity());
   }
   @SubscribeEvent public static void attack(net.neoforged.neoforge.event.entity.player.AttackEntityEvent event){
      if(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer owner && CuChulainnAbilityService.isOwner(owner)
         && (owner.isShiftKeyDown() || CuChulainnAbilityService.active(owner) || CuChulainnWeaponService.busy(owner))){
         event.setCanceled(true);if(owner.isShiftKeyDown())CuChulainnAbilityService.start(owner,-1);
      }
   }
   @SubscribeEvent public static void fall(LivingFallEvent event) {
      if(event.getEntity().getPersistentData().getBoolean("CuLeapFall")) {event.setCanceled(true);event.getEntity().getPersistentData().remove("CuLeapFall");}
   }
}
