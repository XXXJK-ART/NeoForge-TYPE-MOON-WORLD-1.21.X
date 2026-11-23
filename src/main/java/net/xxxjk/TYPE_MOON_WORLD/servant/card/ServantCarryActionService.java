package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService;

/** Cancel the operator, leaving released projectiles, summons and timed effects alive. */
public final class ServantCarryActionService {
   private ServantCarryActionService() {}
   public static void interrupt(ServerPlayer player) {
      NoblePhantasmCastService.cancel(player, true);
      net.xxxjk.TYPE_MOON_WORLD.magic.nordic.MagicGander.clearCharge(player);
      net.xxxjk.TYPE_MOON_WORLD.magic.nordic.MagicGandrMachineGun.forceCleanup(player);
      net.xxxjk.TYPE_MOON_WORLD.magic.special.ElementalArrayService.interruptForCarry(player);
      net.xxxjk.TYPE_MOON_WORLD.magic.basic.ManaBurstService.clear(player);
      if (net.xxxjk.TYPE_MOON_WORLD.chain.service.EnumaChainService.isCasting(player))
         net.xxxjk.TYPE_MOON_WORLD.chain.service.EnumaChainService.cleanupOwner(player);
      if (ServantCardArashSkills.isPlayerChanting(player)) ServantCardArashSkills.abortPlayerChantTechnical(player);
      ServantCardNightingaleSkills.interrupt(player);
      net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works.ChantHandler.interruptForCarry(player);
      ServantCardActionTimeline.clear(player);
      net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidAbilityService.cancel(player);
      ServantCardHundredFacesHassanSkills.cancelPendingSummon(player, true);
      ServantCardEmiyaSkills.stopRhoAias(player);
      ServantCardArtoriaSkills.stopWindDefense(player);
      PlayerNoblePhantasmHelper.clearGaeBulgUseState(player);
      PlayerNoblePhantasmHelper.clearGaeBulgMeleeState(player);
      PlayerNoblePhantasmHelper.releaseExcalibur(player);
      PlayerNoblePhantasmHelper.releaseShortExcalibur(player);
      player.stopUsingItem();
      var data = player.getPersistentData();
      data.remove("ServantCardLiCounterUntil");
      data.remove("ServantCardOdaChargedMatchlockActive");
      data.remove("ServantCardOdaChargedMatchlockStart");
      data.remove("ServantCardOdaThreeThousandActive");
      data.remove("ServantCardOdaThreeThousandStart");
      data.remove("TypeMoonGoldenExcaliburCharge");
      data.remove("TypeMoonGallatinCharge");
      net.xxxjk.TYPE_MOON_WORLD.martial.GanryuCombatService.clearRuntime(player);
      net.xxxjk.TYPE_MOON_WORLD.martial.BajiquanCombatService.interruptForCarry(player);
      net.xxxjk.TYPE_MOON_WORLD.martial.KendoCombatService.interruptForCarry(player);
      ServantCardMedusaSkills.clearMedusaEyes(player);
      player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).syncPlayerVariables(player);
   }
}
