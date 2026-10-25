package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaStateService;
import net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaAbilityService;
import net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper;
import net.xxxjk.TYPE_MOON_WORLD.magic.basic.ManaBurstService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonCombatHelper;

public final class ServantCardArtoriaSkills {
   public static final int WIND_HAMMER_SLOT = 1;
   public static final int WIND_DEFENSE_SLOT = 6;
   public static final int WIND_RELEASE_SLOT = 3;

   private ServantCardArtoriaSkills() {
   }

   public static boolean isWindAction(ServantCardSkillAction action) {
      return action != null && ("invisible_air_hammer".equals(action.effectId())
         || ArtoriaRules.WIND_DEFENSE.equals(action.effectId())
         || "invisible_air_release".equals(action.effectId()));
   }

   public static boolean isWindToggle(ServantCardSkillAction action) {
      return action != null && net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaRules.WIND_BARRIER_TOGGLE.equals(action.effectId());
   }

   public static boolean isManaBurstActive(ServerPlayer player) {
      return ArtoriaPendragonCombatHelper.isManaBurstActive(player);
   }

   public static boolean isWindLockedByExcalibur(ServerPlayer player) {
      return PlayerNoblePhantasmHelper.isArtoriaExcaliburWindLocked(player);
   }

   public static void performManaBurst(ServerPlayer player) {
      if (ArtoriaPendragonCombatHelper.isManaBurstActive(player)) ArtoriaStateService.stopBurst(player);
      else { ArtoriaStateService.startBurst(player); spawnManaBurstActivationFx(player); }
   }

   public static void clear(ServerPlayer player) {
      ArtoriaStateService.clearTemporary(player); ManaBurstService.clearExternalJetMovement(player);
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!"artoria_pendragon".equals(vars.servant_card_id)) return;
      ArtoriaStateService.tick(player);
      if (ArtoriaPendragonCombatHelper.isManaBurstActive(player)) ManaBurstService.tickExternalJetMovement(player, 5);
   }

    public static boolean performInvisibleAirHammer(ServerPlayer player, int revealTicks) {
       return ArtoriaAbilityService.hammer(player);
   }

    public static boolean performInvisibleAirRelease(ServerPlayer player, int revealTicks) {
       return ArtoriaAbilityService.windRelease(player);
   }

   public static boolean toggleWindBarrier(ServerPlayer player) {
      return ArtoriaStateService.toggleWindBarrier(player);
   }

   public static boolean startWindDefense(ServerPlayer player) {
      return ArtoriaStateService.startWindDefense(player, true);
   }

   public static void stopWindDefense(ServerPlayer player) {
      ArtoriaStateService.stopWindDefense(player);
   }

   public static void performSmallCombo(ServerPlayer player) {
      ArtoriaAbilityService.combo(player);
   }

   private static void spawnManaBurstActivationFx(ServerPlayer player) {
      if (player.level() instanceof ServerLevel level) {
         net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects.spawn(level, "artoria_mana_burst_aura", player, 96);
         level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.25F, 1.28F);
         level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.35F);
      }
   }

}
