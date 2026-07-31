package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.StatRank;

@EventBusSubscriber(modid = "typemoonworld")
public final class ServantMasterCarryService {
   private static final String CARRYING_TAG = "TypeMoonCarryingMaster";
   private static final String CARRIED_BY_TAG = "TypeMoonCarriedByServant";

   private ServantMasterCarryService() {}

   public static boolean isCarryPair(Player servant, Entity master) {
      if (!(master instanceof Player masterPlayer) || servant == masterPlayer) return false;
      TypeMoonWorldModVariables.PlayerVariables servantVars = servant.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      TypeMoonWorldModVariables.PlayerVariables masterVars = masterPlayer.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return servantVars.servant_card_transformed
         && canCarry(servantVars.servant_card_id)
         && masterVars.master_active
         && masterPlayer.getUUID().toString().equals(servantVars.servant_card_master_uuid)
         && servant.getUUID().toString().equals(masterVars.master_servant_uuid);
   }

   public static boolean canCarry(Player servant) {
      if (servant == null) return false;
      TypeMoonWorldModVariables.PlayerVariables vars = servant.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.servant_card_transformed && canCarry(vars.servant_card_id);
   }

   public static boolean canCarry(String servantId) {
      ServantDefinition definition = ServantDataRegistry.get(servantId == null ? "" : servantId);
      return definition == null || definition.parameters().strength().coefficient() > StatRank.E.coefficient();
   }

   public static boolean isCarryingMaster(Player servant) {
      return servant.getFirstPassenger() instanceof Player master && isCarryPair(servant, master);
   }

   public static boolean isCarriedMaster(Player master) {
      return master.getVehicle() instanceof Player servant && isCarryPair(servant, master);
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
      if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer servant)
         || !(event.getTarget() instanceof ServerPlayer master) || !servant.isCrouching()) return;
      if (!isCarryPair(servant, master) || servant.isPassenger() || !servant.getPassengers().isEmpty() || master.isPassenger()) return;
      if (master.startRiding(servant, true)) {
         syncPassengers(servant, master);
         servant.getPersistentData().putUUID(CARRYING_TAG, master.getUUID());
         master.getPersistentData().putUUID(CARRIED_BY_TAG, servant.getUUID());
         master.fallDistance = 0.0F;
         servant.displayClientMessage(Component.translatable("message.typemoonworld.master_carry.picked_up"), true);
         master.displayClientMessage(Component.translatable("message.typemoonworld.master_carry.carried"), true);
         event.setCanceled(true);
         event.setCancellationResult(InteractionResult.SUCCESS);
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
      if (event.getHand() == InteractionHand.MAIN_HAND && event.getEntity() instanceof ServerPlayer servant && handleCarryingUse(servant)) {
         event.setCanceled(true);
         event.setCancellationResult(InteractionResult.SUCCESS);
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
      if (event.getHand() == InteractionHand.MAIN_HAND && event.getEntity() instanceof ServerPlayer servant && handleCarryingUse(servant)) {
         event.setCanceled(true);
         event.setCancellationResult(InteractionResult.SUCCESS);
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onAttackEntity(AttackEntityEvent event) {
      if (event.getEntity() instanceof ServerPlayer servant && isCarryingMaster(servant)) event.setCanceled(true);
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onCarriedDamage(LivingIncomingDamageEvent event) {
      if (!(event.getEntity() instanceof ServerPlayer master) || !isCarriedMaster(master)) return;
      if (event.getSource().is(DamageTypes.FALL) || event.getSource().is(DamageTypes.IN_WALL)) {
         event.setCanceled(true);
         event.setAmount(0.0F);
         master.fallDistance = 0.0F;
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onCarriedFall(LivingFallEvent event) {
      if (event.getEntity() instanceof ServerPlayer master && isCarriedMaster(master)) {
         master.fallDistance = 0.0F;
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent.Post event) {
      if (!(event.getEntity() instanceof ServerPlayer player)) return;
      if (isCarriedMaster(player)) {
         player.fallDistance = 0.0F;
         return;
      }
      if (player.getPersistentData().hasUUID(CARRIED_BY_TAG)) {
         ServerPlayer servant = player.getServer().getPlayerList().getPlayer(player.getPersistentData().getUUID(CARRIED_BY_TAG));
         finishDismount(servant, player);
      }
      if (player.getPersistentData().hasUUID(CARRYING_TAG) && !isCarryingMaster(player)) player.getPersistentData().remove(CARRYING_TAG);
   }

   private static boolean handleCarryingUse(ServerPlayer servant) {
      if (!isCarryingMaster(servant)) return false;
      if (servant.isCrouching() && servant.onGround() && servant.getFirstPassenger() instanceof ServerPlayer master) {
         master.stopRiding();
         finishDismount(servant, master);
      }
      return true;
   }

   private static void finishDismount(ServerPlayer servant, ServerPlayer master) {
      if (master == null) return;
      if (master.isPassenger()) master.stopRiding();
      if (servant != null) syncPassengers(servant, master);
      if (servant != null && servant.level() == master.level()) {
         double yaw = Math.toRadians(servant.getYRot());
         Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
         Vec3 preferred = servant.position().add(right.scale(1.35));
         Vec3 safe = DismountHelper.findSafeDismountLocation(master.getType(), master.level(), net.minecraft.core.BlockPos.containing(preferred), true);
         Vec3 destination = safe == null ? servant.position().add(right.scale(1.1)) : safe;
         master.teleportTo(destination.x, destination.y, destination.z);
         servant.getPersistentData().remove(CARRYING_TAG);
      }
      master.getPersistentData().remove(CARRIED_BY_TAG);
      master.fallDistance = 0.0F;
   }

   private static void syncPassengers(ServerPlayer servant, ServerPlayer master) {
      ClientboundSetPassengersPacket packet = new ClientboundSetPassengersPacket(servant);
      servant.connection.send(packet);
      master.connection.send(packet);
   }
}
