package net.xxxjk.TYPE_MOON_WORLD.martial;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.entity.BajiquanApprenticeEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

@EventBusSubscriber(modid = "typemoonworld")
public final class BajiquanEvents {
   private BajiquanEvents() {}

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent.Post event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         BajiquanCombatService.tickPlayer(player);
         GanryuCombatService.tickPlayer(player);
      }
   }

   @SubscribeEvent
   public static void onAttack(AttackEntityEvent event) {
      if (event.getEntity().hasEffect(ModMobEffects.STAGGER) || event.getEntity().hasEffect(ModMobEffects.OFF_BALANCE)) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onRightClickBlock(RightClickBlock event) {
      if (shouldCancelInteraction(event)) {
         event.setCanceled(true);
         event.setCancellationResult(InteractionResult.SUCCESS);
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onRightClickItem(RightClickItem event) {
      if (shouldCancelInteraction(event)) {
         event.setCanceled(true);
         event.setCancellationResult(InteractionResult.SUCCESS);
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onEntityInteract(EntityInteract event) {
      if (shouldCancelInteraction(event)) {
         event.setCanceled(true);
         event.setCancellationResult(InteractionResult.SUCCESS);
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onEntityInteractSpecific(EntityInteractSpecific event) {
      if (shouldCancelInteraction(event)) {
         event.setCanceled(true);
         event.setCancellationResult(InteractionResult.SUCCESS);
      }
   }

   private static boolean shouldCancelInteraction(PlayerInteractEvent event) {
      boolean controlled = event.getEntity().hasEffect(ModMobEffects.STAGGER) || event.getEntity().hasEffect(ModMobEffects.OFF_BALANCE);
      boolean bajiquanInput = event.getEntity() instanceof ServerPlayer player && BajiquanCombatService.isActive(player);
      boolean ganryuInput = event.getEntity() instanceof ServerPlayer player && GanryuCombatService.isActive(player);
      return controlled || bajiquanInput || ganryuInput;
   }

   @SubscribeEvent
   public static void onLogin(PlayerLoggedInEvent event) {
      event.getEntity().getPersistentData().remove("TypeMoonBajiquanSparring");
      event.getEntity().getPersistentData().remove("TypeMoonBajiquanSparringMaster");
      event.getEntity().getPersistentData().remove("TypeMoonGanryuSparring");
      event.getEntity().getPersistentData().remove("TypeMoonGanryuSparringMaster");
      if (event.getEntity() instanceof ServerPlayer player) {
         MartialUkemiService.migrate(player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES));
      }
   }

   @SubscribeEvent
   public static void onLogout(PlayerLoggedOutEvent event) {
      if (!(event.getEntity() instanceof ServerPlayer player)) return;
      if (player.getPersistentData().hasUUID("TypeMoonBajiquanSparringMaster")) {
         java.util.UUID masterId = player.getPersistentData().getUUID("TypeMoonBajiquanSparringMaster");
         for (net.minecraft.server.level.ServerLevel level : player.getServer().getAllLevels()) {
            if (level.getEntity(masterId) instanceof net.xxxjk.TYPE_MOON_WORLD.entity.BajiquanMasterEntity master) {
               master.endDuel(player, false);
               break;
            }
         }
      }
      player.getPersistentData().remove("TypeMoonBajiquanSparring");
      player.getPersistentData().remove("TypeMoonBajiquanSparringMaster");
      if (player.getPersistentData().hasUUID("TypeMoonGanryuSparringMaster")) {
         java.util.UUID masterId = player.getPersistentData().getUUID("TypeMoonGanryuSparringMaster");
         for (net.minecraft.server.level.ServerLevel level : player.getServer().getAllLevels()) {
            if (level.getEntity(masterId) instanceof net.xxxjk.TYPE_MOON_WORLD.entity.MysteriousSwordsmanEntity master) {
               master.endDuel(player, false);
               break;
            }
         }
      }
      player.getPersistentData().remove("TypeMoonGanryuSparring");
      player.getPersistentData().remove("TypeMoonGanryuSparringMaster");
      GanryuCombatService.clearRuntime(player);
   }

   @SubscribeEvent
   public static void onChangedDimension(PlayerChangedDimensionEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) GanryuCombatService.clearRuntime(player);
   }

   @SubscribeEvent
   public static void onRespawn(PlayerRespawnEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) GanryuCombatService.clearRuntime(player);
   }

   @SubscribeEvent
   public static void onDamage(LivingIncomingDamageEvent event) {
      if (event.getSource().getEntity() instanceof LivingEntity controlledAttacker
         && event.getSource().getDirectEntity() == controlledAttacker
         && (controlledAttacker.hasEffect(ModMobEffects.STAGGER) || controlledAttacker.hasEffect(ModMobEffects.OFF_BALANCE))) {
         event.setCanceled(true);
         return;
      }
      if (event.getSource().getEntity() instanceof ServerPlayer sparringAttacker
         && sparringAttacker.getPersistentData().getBoolean("TypeMoonBajiquanSparring")) {
         boolean allowed = sparringAttacker.getPersistentData().hasUUID("TypeMoonBajiquanSparringMaster")
            && event.getEntity().getUUID().equals(sparringAttacker.getPersistentData().getUUID("TypeMoonBajiquanSparringMaster"))
            && BajiquanCombatService.isMartialDamage(sparringAttacker);
         if (!allowed) { event.setCanceled(true); return; }
      }
      if (event.getSource().getEntity() instanceof ServerPlayer sparringAttacker
         && sparringAttacker.getPersistentData().getBoolean("TypeMoonGanryuSparring")) {
         boolean allowed = sparringAttacker.getPersistentData().hasUUID("TypeMoonGanryuSparringMaster")
            && event.getEntity().getUUID().equals(sparringAttacker.getPersistentData().getUUID("TypeMoonGanryuSparringMaster"))
            && GanryuCombatService.isMartialDamage(sparringAttacker);
         if (!allowed) { event.setCanceled(true); return; }
      }
      if (event.getEntity() instanceof ServerPlayer sparringDefender
         && sparringDefender.getPersistentData().getBoolean("TypeMoonBajiquanSparring")) {
         boolean allowed = sparringDefender.getPersistentData().hasUUID("TypeMoonBajiquanSparringMaster")
            && event.getSource().getEntity() != null
            && event.getSource().getEntity().getUUID().equals(sparringDefender.getPersistentData().getUUID("TypeMoonBajiquanSparringMaster"));
         if (!allowed) { event.setCanceled(true); return; }
         TypeMoonWorldModVariables.PlayerVariables duelVars = sparringDefender.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (duelVars.bajiquan_proficiency < 80.0 && sparringDefender.getHealth() - event.getAmount() <= 0.0F) {
            event.setCanceled(true);
            sparringDefender.setHealth(1.0F);
            if (sparringDefender.serverLevel().getEntity(sparringDefender.getPersistentData().getUUID("TypeMoonBajiquanSparringMaster")) instanceof net.xxxjk.TYPE_MOON_WORLD.entity.BajiquanMasterEntity master) {
               master.endDuel(sparringDefender, false);
            }
            return;
         }
      }
      if (event.getEntity() instanceof ServerPlayer sparringDefender
         && sparringDefender.getPersistentData().getBoolean("TypeMoonGanryuSparring")) {
         boolean allowed = sparringDefender.getPersistentData().hasUUID("TypeMoonGanryuSparringMaster")
            && event.getSource().getEntity() != null
            && event.getSource().getEntity().getUUID().equals(sparringDefender.getPersistentData().getUUID("TypeMoonGanryuSparringMaster"));
         if (!allowed) { event.setCanceled(true); return; }
         TypeMoonWorldModVariables.PlayerVariables duelVars = sparringDefender.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (duelVars.ganryu_proficiency < 80.0 && sparringDefender.getHealth() - event.getAmount() <= 0.0F) {
            event.setCanceled(true);
            sparringDefender.setHealth(1.0F);
            if (sparringDefender.serverLevel().getEntity(sparringDefender.getPersistentData().getUUID("TypeMoonGanryuSparringMaster"))
               instanceof net.xxxjk.TYPE_MOON_WORLD.entity.MysteriousSwordsmanEntity master) {
               master.endDuel(sparringDefender, false);
            }
            return;
         }
      }
      boolean melee = event.getSource().getEntity() instanceof LivingEntity attacker
         && event.getSource().getDirectEntity() == attacker
         && (event.getSource().is(DamageTypes.PLAYER_ATTACK) || event.getSource().is(DamageTypes.MOB_ATTACK))
         && !(event.getSource().getDirectEntity() instanceof Projectile);

      if (melee && event.getEntity() instanceof ServerPlayer defender && event.getSource().getEntity() instanceof LivingEntity attacker) {
         float[] adjusted = new float[]{event.getAmount()};
         boolean canceled = BajiquanCombatService.tryDefend(defender, attacker, adjusted);
         if (canceled && adjusted[0] <= 0.0F) {
            event.setCanceled(true);
            return;
         }
         event.setAmount(adjusted[0]);
      }

      if (event.getEntity() instanceof ServerPlayer player && !event.getSource().is(DamageTypeTags.BYPASSES_RESISTANCE)) {
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         double reduction = vars.servant_card_transformed || vars.master_card_active
            ? 0.0
            : BodyTrainingService.resistanceReduction(vars.body_resistance);
         event.setAmount((float)(event.getAmount() * (1.0 - reduction)));
      }

      if (event.getEntity() instanceof ServerPlayer player && event.getSource().is(DamageTypes.FALL)) {
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         boolean liShuwen = vars.servant_card_transformed && "li_shuwen".equals(vars.servant_card_id);
         if (liShuwen || BajiquanCombatService.consumeUkemiReduction(player)) event.setAmount(event.getAmount() * 0.3F);
      }

      if (melee && event.getSource().is(DamageTypes.PLAYER_ATTACK) && event.getSource().getEntity() instanceof ServerPlayer attacker && event.getAmount() > 0.0F) {
         if (!GanryuCombatService.isMartialDamage(attacker)
            && attacker.getMainHandItem().is(net.xxxjk.TYPE_MOON_WORLD.item.ModItems.NODACHI.get())) {
            int bonus = GanryuCombatService.souwaBonus(attacker.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES));
            if (bonus > 0) event.setAmount(event.getAmount() + bonus);
         }
         long now = attacker.level().getGameTime();
         long last = attacker.getPersistentData().getLong("TypeMoonBodyTrainingLastAwardTick");
         if (last != now) {
            attacker.getPersistentData().putLong("TypeMoonBodyTrainingLastAwardTick", now);
            BodyTrainingService.award(attacker, event.getEntity() instanceof BajiquanApprenticeEntity ? 2 : 1);
         }
         BajiquanCombatService.breakCircleRealm(attacker);
      }
   }
}
