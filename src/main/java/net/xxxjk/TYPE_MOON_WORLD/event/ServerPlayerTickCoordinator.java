package net.xxxjk.TYPE_MOON_WORLD.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.magic.church.BaptismRiteEventHandler;
import net.xxxjk.TYPE_MOON_WORLD.magic.projection.ProjectionTickHandler;
import net.xxxjk.TYPE_MOON_WORLD.magic.reinforcement.MagicReinforcementEventHandler;
import net.xxxjk.TYPE_MOON_WORLD.magic.special.TimeAlterEventHandler;
import net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works.ChantHandler;
import net.xxxjk.TYPE_MOON_WORLD.martial.BajiquanEvents;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardArashSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterCarryService;
import net.xxxjk.TYPE_MOON_WORLD.servant.summon.SummoningRitualService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.magic.sanctification.MagicSanctification;

/**
 * Single server-side entry point for player tick work.
 *
 * Event handlers that only advance a service belong in this coordinator.  A
 * service still owns its state and may cheaply return when it is inactive.
 */
@EventBusSubscriber(modid = "typemoonworld")
public final class ServerPlayerTickCoordinator {
   private ServerPlayerTickCoordinator() {
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent.Post event) {
      if (!(event.getEntity() instanceof ServerPlayer player)
         || player.level().isClientSide()
         || !player.isAlive()
         || player.isRemoved()) {
         return;
      }

      CommonEvents.tickGoldenFleece(player);
      CommonEvents.tickPlayer(player);
      // Keep the event bus surface small. These handlers are simulation phases,
      // so they are invoked once here after the attachment has been fetched by
      // CommonEvents. Each phase still owns its active-state checks.
      runPhase("ubw_chant", () -> ChantHandler.onPlayerTick(new PlayerTickEvent.Post(player)), player);
      runPhase("time_alter", () -> TimeAlterEventHandler.onPlayerTick(new PlayerTickEvent.Post(player)), player);
      runPhase("baptism_rite", () -> BaptismRiteEventHandler.onPlayerTick(new PlayerTickEvent.Post(player)), player);
      runPhase("sanctification", () -> MagicSanctification.tick(player), player);
      runPhase("projection", () -> ProjectionTickHandler.onPlayerTick(new PlayerTickEvent.Post(player)), player);
      runPhase("reinforcement", () -> MagicReinforcementEventHandler.onPlayerTick(new PlayerTickEvent.Post(player)), player);
      runPhase("bajiquan", () -> BajiquanEvents.onPlayerTick(new PlayerTickEvent.Post(player)), player);
      runPhase("summoning", () -> SummoningRitualService.onPlayerTick(new PlayerTickEvent.Post(player)), player);
      runPhase("gem_terrain", () -> GemTerrainEventHandler.onPlayerTick(new PlayerTickEvent.Post(player)), player);
      runPhase("rune_release", () -> RuneReleaseEvents.onPlayerTick(new PlayerTickEvent.Post(player)), player);
      runPhase("servant_master_carry", () -> ServantMasterCarryService.onPlayerTick(new PlayerTickEvent.Post(player)), player);
      runPhase("arash", () -> ServantCardArashSkills.onPlayerTick(new PlayerTickEvent.Post(player)), player);
      ServerPlayerTickHooks.run(player);
      if (player.tickCount % 10 == 0) {
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         vars.syncDirtyDomains(player);
      }
   }

   private static void runPhase(String id, Runnable phase, ServerPlayer player) {
      try {
         phase.run();
      } catch (RuntimeException exception) {
         net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.LOGGER.error("Player tick phase {} failed for {}", id, player.getGameProfile().getName(), exception);
      }
   }
}
