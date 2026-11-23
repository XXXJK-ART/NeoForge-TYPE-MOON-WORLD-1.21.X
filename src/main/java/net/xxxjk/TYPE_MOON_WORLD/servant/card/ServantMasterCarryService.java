package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatDefenseService;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.StatRank;

/** Actual passengers authorize protection; saved tags only recover interrupted dismounts. */
@EventBusSubscriber(modid = "typemoonworld")
public final class ServantMasterCarryService {
   private static final String CARRYING_TAG = "TypeMoonCarryingMaster", CARRIED_BY_TAG = "TypeMoonCarriedByServant";
   private static final Set<UUID> WAIT_FOR_SHIFT_RELEASE = new HashSet<>();
   private static final Map<UUID, Long> CONSUMED_INTERACTIONS = new HashMap<>();
   private static final Map<UUID, UUID> CLIENT_PAIRS = new HashMap<>();
   private ServantMasterCarryService() {}

   public static boolean isContractPair(Player servant, Entity target) {
      if (!(target instanceof Player master) || servant == master || !servant.isAlive() || !master.isAlive()
         || servant.isRemoved() || master.isRemoved() || servant.level() != master.level()) return false;
      var sv = servant.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      var mv = master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return sv.servant_card_transformed && mv.master_active
         && master.getUUID().toString().equals(sv.servant_card_master_uuid)
         && servant.getUUID().toString().equals(mv.master_servant_uuid)
         && !sv.servant_card_contract_id.isBlank() && sv.servant_card_contract_id.equals(mv.master_servant_contract_id);
   }
   public static boolean isCarryPair(Player servant, Entity master) {
      if (servant.level().isClientSide) return master instanceof Player && master.getUUID().equals(CLIENT_PAIRS.get(servant.getUUID()));
      return isContractPair(servant, master) && canCarry(servant);
   }
   public static void applyClientPair(UUID servant, UUID master, boolean carrying) {
      CLIENT_PAIRS.remove(servant);
      if (carrying) CLIENT_PAIRS.put(servant, master);
   }
   public static void clearClientPairs() { CLIENT_PAIRS.clear(); }
   public static boolean canCarry(Player servant) {
      if (servant == null) return false;
      var vars = servant.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.servant_card_transformed && canCarry(vars.servant_card_id);
   }
   public static boolean canCarry(String servantId) {
      var definition = ServantDataRegistry.get(servantId == null ? "" : servantId);
      return definition != null && definition.parameters().strength().coefficient() > StatRank.E.coefficient();
   }
   public static boolean isCarryingMaster(Player servant) {
      return servant.getFirstPassenger() instanceof Player master && master.getVehicle() == servant && isCarryPair(servant, master);
   }
   public static boolean isCarriedMaster(Player master) {
      return master.getVehicle() instanceof Player servant && servant.hasPassenger(master) && isCarryPair(servant, master);
   }
   /** The two-block guard does not require carrying strength. */
   public static ServerPlayer protector(ServerPlayer master) {
      if (isCarriedMaster(master)) return (ServerPlayer)master.getVehicle();
      var vars = master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.master_active) return null;
      ServerPlayer servant = MasterServantLinkService.getLinkedServant(master, vars);
      return servant != null && isContractPair(servant, master) && servant.distanceToSqr(master) <= 4.0
         && ServantCombatDefenseService.isActiveGuard(servant) ? servant : null;
   }
   public static boolean attacksBlocked(Player player) { return isCarryingMaster(player) || isCarriedMaster(player); }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
      if (interact(event.getEntity(), event.getTarget())) {
         event.setCanceled(true); event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
      }
   }
   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
      if (interact(event.getEntity(), event.getTarget())) {
         event.setCanceled(true); event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
      }
   }
   private static boolean interact(Player actor, Entity target) {
      if (actor instanceof ServerPlayer sp && consumed(sp)) return true;
      if (isCarryingMaster(actor)) {
         if (actor.isShiftKeyDown() && actor instanceof ServerPlayer sp) { consume(sp); dismount(sp); }
         return true;
      }
      if (!(target instanceof Player other) || !actor.isShiftKeyDown()) return false;
      Player servant = isContractPair(actor, other) ? actor : isContractPair(other, actor) ? other : null;
      if (servant == null) return false;
      Player master = servant == actor ? other : actor;
      if (isCarriedMaster(master)) return true;
      if (!canCarry(servant)) {
         if (actor instanceof ServerPlayer sp) sp.displayClientMessage(Component.translatable("message.typemoonworld.master_carry.too_weak"), true);
         return true;
      }
      if (servant instanceof ServerPlayer sp && master instanceof ServerPlayer mp && tryCarry(sp, mp)) consume(actor);
      return true;
   }
   public static boolean tryCarry(ServerPlayer servant, ServerPlayer master) {
      if (!isCarryPair(servant, master) || servant.isPassenger() || master.isPassenger() || !servant.getPassengers().isEmpty()
         || !servant.canInteractWithEntity(master, 0.0) || !master.canInteractWithEntity(servant, 0.0)
         || !servant.hasLineOfSight(master) || !master.hasLineOfSight(servant)) return false;
      ServantCarryActionService.interrupt(servant);
      if (!isCarryPair(servant, master) || servant.isPassenger() || master.isPassenger() || !servant.getPassengers().isEmpty()
         || !servant.canInteractWithEntity(master, 0.0)) return false;
      if (!master.startRiding(servant, true)) return false;
      servant.getPersistentData().putUUID(CARRYING_TAG, master.getUUID());
      master.getPersistentData().putUUID(CARRIED_BY_TAG, servant.getUUID());
      if (master.isShiftKeyDown()) WAIT_FOR_SHIFT_RELEASE.add(master.getUUID());
      master.fallDistance = 0.0F;
      syncPassengers(servant, master);
      servant.displayClientMessage(Component.translatable("message.typemoonworld.master_carry.picked_up"), true);
      master.displayClientMessage(Component.translatable("message.typemoonworld.master_carry.carried"), true);
      return true;
   }
   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
      if (carryingUse(event.getEntity())) { event.setCanceled(true); event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide)); }
   }
   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
      if (carryingUse(event.getEntity())) { event.setCanceled(true); event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide)); }
   }
   private static boolean carryingUse(Player player) {
      if (player instanceof ServerPlayer sp && consumed(sp)) return true;
      if (!isCarryingMaster(player)) return false;
      if (player.isShiftKeyDown() && player instanceof ServerPlayer sp) { consume(sp); dismount(sp); }
      return true;
   }
   private static boolean consumed(ServerPlayer player) {
      return CONSUMED_INTERACTIONS.getOrDefault(player.getUUID(), Long.MIN_VALUE) == player.level().getGameTime();
   }
   private static void consume(Player player) { CONSUMED_INTERACTIONS.put(player.getUUID(), player.level().getGameTime()); }
   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onAttackEntity(AttackEntityEvent event) { if (attacksBlocked(event.getEntity())) event.setCanceled(true); }
   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onCarriedFall(LivingFallEvent event) {
      if (event.getEntity() instanceof Player master && isCarriedMaster(master)) { master.fallDistance = 0.0F; event.setCanceled(true); }
   }
   public static void onPlayerTick(PlayerTickEvent.Post event) {
      if (!(event.getEntity() instanceof ServerPlayer player)) return;
      if (isCarriedMaster(player)) {
         player.fallDistance = 0.0F;
         if (!player.isShiftKeyDown()) WAIT_FOR_SHIFT_RELEASE.remove(player.getUUID());
         else if (!WAIT_FOR_SHIFT_RELEASE.contains(player.getUUID())) dismount(player);
      } else if (player.getPersistentData().hasUUID(CARRIED_BY_TAG)) dismount(player);
      if (player.getPersistentData().hasUUID(CARRYING_TAG) && !isCarryingMaster(player)) dismount(player);
      if (!consumed(player)) CONSUMED_INTERACTIONS.remove(player.getUUID());
   }
   public static void dismount(ServerPlayer player) {
      ServerPlayer servant = null, master = null;
      if (player.getVehicle() instanceof ServerPlayer vehicle && (isCarryPair(vehicle, player) || player.getPersistentData().hasUUID(CARRIED_BY_TAG))) {
         servant = vehicle; master = player;
      } else if (player.getFirstPassenger() instanceof ServerPlayer passenger && (isCarryPair(player, passenger) || player.getPersistentData().hasUUID(CARRYING_TAG))) {
         servant = player; master = passenger;
      } else if (player.getPersistentData().hasUUID(CARRIED_BY_TAG)) {
         servant = player.getServer().getPlayerList().getPlayer(player.getPersistentData().getUUID(CARRIED_BY_TAG)); master = player;
      } else if (player.getPersistentData().hasUUID(CARRYING_TAG)) {
         servant = player; master = player.getServer().getPlayerList().getPlayer(player.getPersistentData().getUUID(CARRYING_TAG));
      }
      if (servant != null) servant.getPersistentData().remove(CARRYING_TAG);
      if (master == null) return;
      master.getPersistentData().remove(CARRIED_BY_TAG); WAIT_FOR_SHIFT_RELEASE.remove(master.getUUID());
      if (master.getVehicle() == servant) master.stopRiding();
      if (servant != null) {
         if (master.isAlive() && servant.level() == master.level() && !master.isPassenger()) {
            Vec3 destination = dismountPosition(servant, master);
            master.teleportTo(destination.x, destination.y, destination.z); master.fallDistance = 0.0F;
         }
         syncPassengers(servant, master);
      }
   }
   private static Vec3 dismountPosition(ServerPlayer servant, ServerPlayer master) {
      if (!servant.onGround()) return master.position();
      double yaw = Math.toRadians(servant.getYRot());
      for (double radius : new double[]{1.35, 2.0, 0.65}) for (int step = 0; step < 8; step++) {
         double angle = yaw + step * Math.PI / 4;
         BlockPos pos = BlockPos.containing(servant.position().add(-Math.cos(angle) * radius, 0, -Math.sin(angle) * radius));
         Vec3 safe = DismountHelper.findSafeDismountLocation(master.getType(), master.level(), pos, true);
         if (safe != null && master.level().noCollision(master, master.getBoundingBox().move(safe.subtract(master.position())))) return safe;
      }
      return servant.position();
   }
   private static void syncPassengers(ServerPlayer servant, ServerPlayer master) {
      var packet = new ClientboundSetPassengersPacket(servant);
      var state = new net.xxxjk.TYPE_MOON_WORLD.network.MasterCarryStateMessage(servant.getUUID(), master.getUUID(), master.getVehicle() == servant);
      Set<ServerPlayer> recipients = new HashSet<>(servant.serverLevel().players()); recipients.add(servant); recipients.add(master);
      for (ServerPlayer observer : recipients) {
         net.xxxjk.TYPE_MOON_WORLD.network.ModNetwork.sendToPlayer(observer, state);
         observer.connection.send(packet);
      }
   }
   @SubscribeEvent public static void onTracking(PlayerEvent.StartTracking event) {
      if (!(event.getEntity() instanceof ServerPlayer observer) || !(event.getTarget() instanceof ServerPlayer target)) return;
      ServerPlayer servant = isCarryingMaster(target) ? target : isCarriedMaster(target) ? (ServerPlayer)target.getVehicle() : null;
      if (servant != null && servant.getFirstPassenger() instanceof ServerPlayer master)
         net.xxxjk.TYPE_MOON_WORLD.network.ModNetwork.sendToPlayer(observer,
            new net.xxxjk.TYPE_MOON_WORLD.network.MasterCarryStateMessage(servant.getUUID(), master.getUUID(), true));
   }
   @SubscribeEvent public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) { dismount(player); CONSUMED_INTERACTIONS.remove(player.getUUID()); }
   }
   @SubscribeEvent public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) { if (event.getEntity() instanceof ServerPlayer player) dismount(player); }
   @SubscribeEvent(priority = EventPriority.LOWEST) public static void onDeath(LivingDeathEvent event) {
      if (!event.isCanceled() && event.getEntity() instanceof ServerPlayer player) dismount(player);
   }
   @SubscribeEvent public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) { if (event.getEntity() instanceof ServerPlayer player) dismount(player); }
   @SubscribeEvent public static void onReload(net.neoforged.neoforge.event.OnDatapackSyncEvent event) {
      if (event.getPlayer() == null) for (ServerPlayer player : event.getPlayerList().getPlayers()) dismount(player);
   }
   @SubscribeEvent public static void onStopping(ServerStoppingEvent event) {
      for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) dismount(player);
      WAIT_FOR_SHIFT_RELEASE.clear(); CONSUMED_INTERACTIONS.clear();
   }
}
