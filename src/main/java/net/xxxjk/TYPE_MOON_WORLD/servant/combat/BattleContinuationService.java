package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.servant.heracles.GodHandService;

@EventBusSubscriber(modid="typemoonworld")
public final class BattleContinuationService {
   private BattleContinuationService() {}
   private static int rank(LivingEntity e) { return CommonSkillRules.highest(ServantIdentityHelper.skillIdsOf(e), "battle_continuation_"); }
   public static boolean trigger(LivingEntity e) {
      var d=e.getPersistentData(); int rank=rank(e); long now=e.level().getGameTime();
      if (rank == 0 || d.getBoolean("CausalSevered") || d.getLong("CommonContinuationReady") > now
         || d.getBoolean("GodHandActive") && d.getInt("GodHandLives") > 0) return false;
      var rule=CommonSkillRules.continuation(rank);
      e.setHealth(Math.max(1,(float)(e.getMaxHealth()*rule.healthRatio())));
      d.putLong("CommonContinuationReady",now+rule.cooldownTicks());
      d.putLong("CommonContinuationProtection",now+rule.protectionTicks());
      d.putBoolean("CommonContinuationRecovering",true); d.putLong("CommonContinuationHealAt",now+20);
      return true;
   }
   @SubscribeEvent(priority=EventPriority.LOWEST)
   public static void afterDefense(LivingDamageEvent.Pre event) {
      var e=event.getEntity(); var d=e.getPersistentData();
      if (e.level().isClientSide()) return;
      if (d.getLong("CommonContinuationProtection") > e.level().getGameTime() && rank(e)>0) event.setNewDamage(event.getNewDamage()*.2F);
      if (event.getNewDamage()>0 && event.getNewDamage() - e.getAbsorptionAmount() >= e.getHealth()) {
         var hit=DamageContexts.get(event.getContainer());
         if (GodHandService.revive(e,event.getSource(),hit==null?AttackOrigin.of(event.getSource()):hit.origin()) || trigger(e)) { e.setAbsorptionAmount(0); event.setNewDamage(0); }
      }
   }
   @SubscribeEvent(priority=EventPriority.HIGHEST)
   public static void death(LivingDeathEvent event) {
      if (event.getEntity().level().isClientSide()) return;
      var e=event.getEntity();
      if (GodHandService.revive(e,event.getSource(),AttackOrigin.of(event.getSource())) || trigger(e)) event.setCanceled(true);
   }
   @SubscribeEvent
   public static void clonePlayer(net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone event) {
      var old=event.getOriginal().getPersistentData();
      if(old.contains("CommonContinuationReady"))event.getEntity().getPersistentData().putLong("CommonContinuationReady",old.getLong("CommonContinuationReady"));
   }
   @SubscribeEvent
   public static void tick(EntityTickEvent.Post event) {
      if (!(event.getEntity() instanceof LivingEntity e) || e.level().isClientSide()) return;
      GodHandService.tick(e);
      if (e.tickCount % 20 == 0 && (e instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity || e instanceof net.minecraft.server.level.ServerPlayer)) CommonSkillService.reconcile(e);
      var d=e.getPersistentData();
      if (!d.getBoolean("CommonContinuationRecovering")) return;
      if (!e.isAlive() || rank(e)==0 || e.getHealth()>=e.getMaxHealth()) { d.remove("CommonContinuationRecovering"); return; }
      if (e.level().getGameTime() >= d.getLong("CommonContinuationHealAt")) {
         e.heal(CommonSkillRules.continuation(rank(e)).heal()); d.putLong("CommonContinuationHealAt",e.level().getGameTime()+20);
      }
   }
}
