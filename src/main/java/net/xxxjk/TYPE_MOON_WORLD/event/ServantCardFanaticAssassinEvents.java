package net.xxxjk.TYPE_MOON_WORLD.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardFanaticAssassinSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.fanatic.FanaticAssassinCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.fanatic.FanaticAssassinRules;
import net.xxxjk.TYPE_MOON_WORLD.servant.fanatic.FanaticDamageTypes;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public final class ServantCardFanaticAssassinEvents {
   private ServantCardFanaticAssassinEvents() {
   }

   @SubscribeEvent
   public static void rejectMentalEffects(MobEffectEvent.Applicable event) {
      if (event.getEntity() instanceof ServerPlayer player
         && ServantCardFanaticAssassinSkills.isFanatic(player)
         && event.getEffectInstance().getEffect().is(FanaticAssassinCombatHelper.MENTAL_EFFECTS)) {
         event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void cancelMentalDamage(LivingIncomingDamageEvent event) {
      if (event.getEntity() instanceof ServerPlayer player
         && ServantCardFanaticAssassinSkills.isFanatic(player)
         && event.getSource().is(FanaticDamageTypes.MENTAL_ATTACKS)
         && FanaticAssassinRules.shouldCancelMentalAttack(player.getRandom().nextFloat())) {
         event.setAmount(0.0F);
         event.setCanceled(true);
      }
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public static void revealWhenDamaged(LivingIncomingDamageEvent event) {
      if (!event.isCanceled() && event.getAmount() > 0.0F && event.getEntity() instanceof ServerPlayer player
         && ServantCardFanaticAssassinSkills.isConcealed(player)) {
         ServantCardFanaticAssassinSkills.revealForAttack(player);
      }
   }

   @SubscribeEvent
   public static void revealWhenAttacking(AttackEntityEvent event) {
      if (event.getEntity() instanceof ServerPlayer player
         && ServantCardFanaticAssassinSkills.isFanatic(player)
         && event.getTarget() instanceof LivingEntity) {
         ServantCardFanaticAssassinSkills.revealForAttack(player);
      }
   }

   @SubscribeEvent
   public static void applyMeleeToxin(LivingDamageEvent.Post event) {
      if (event.getSource().is(DamageTypes.PLAYER_ATTACK)
         && event.getSource().getEntity() instanceof ServerPlayer player) {
         ServantCardFanaticAssassinSkills.applyToxinOnMelee(player, event.getEntity());
      }
   }

   @SubscribeEvent
   public static void discardJinnOnDeath(LivingDeathEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         ServantCardFanaticAssassinSkills.discardOwnedJinn(player);
      }
   }

   @SubscribeEvent
   public static void discardJinnOnLogout(PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         ServantCardFanaticAssassinSkills.discardOwnedJinn(player);
      }
   }

   @SubscribeEvent
   public static void discardJinnOnDimensionChange(PlayerChangedDimensionEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         ServantCardFanaticAssassinSkills.discardOwnedJinn(player);
      }
   }
}
