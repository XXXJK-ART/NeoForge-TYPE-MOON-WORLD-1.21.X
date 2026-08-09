package net.xxxjk.TYPE_MOON_WORLD.talent;

import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.network.ClairvoyanceStateMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.passive.PassiveService;
import net.neoforged.neoforge.network.PacketDistributor;

public final class TalentService {
   public static final String MONSTROUS_STRENGTH = "monstrous_strength";
   public static final String CLAIRVOYANCE = "clairvoyance";
   public static final Set<String> IDS = Set.of(MONSTROUS_STRENGTH, CLAIRVOYANCE);
   public static final String STRENGTH_UNTIL_TAG = "TypeMoonMonstrousStrengthUntil";
   public static final String STRENGTH_AMPLIFIER_TAG = "TypeMoonMonstrousStrengthAmplifier";
   public static final String STRENGTH_SUSPENDED_REMAINING_TAG = "TypeMoonMonstrousStrengthSuspendedRemaining";
   private static final String EFFECTS_SUSPENDED_TAG = "TypeMoonTalentEffectsSuspended";

   private TalentService() {
   }

   public static boolean isTalent(String id) {
      return id != null && IDS.contains(id);
   }

   public static boolean owns(TypeMoonWorldModVariables.PlayerVariables vars, String id) {
      if (vars == null || !isTalent(id)) return false;
      if (vars.talent_proficiencies.containsKey(id)) return true;
      return CLAIRVOYANCE.equals(id) && PassiveService.has(vars, PassiveService.CLAIRVOYANCE);
   }

   public static boolean hasAny(TypeMoonWorldModVariables.PlayerVariables vars) {
      return owns(vars, MONSTROUS_STRENGTH) || owns(vars, CLAIRVOYANCE);
   }

   public static double proficiency(TypeMoonWorldModVariables.PlayerVariables vars, String id) {
      if (vars == null || !isTalent(id)) return 0.0;
      double granted = Mth.clamp(vars.talent_proficiencies.getOrDefault(id, 0.0), 0.0, 100.0);
      if (CLAIRVOYANCE.equals(id)) {
         var rank = PassiveService.rank(vars, PassiveService.CLAIRVOYANCE);
         granted = effectiveClairvoyanceProficiency(granted, rank);
      }
      return granted;
   }

   public static double effectiveClairvoyanceProficiency(double independentlyGranted, net.xxxjk.TYPE_MOON_WORLD.passive.PassiveRank rank) {
      return Math.max(Mth.clamp(independentlyGranted, 0.0, 100.0), rank == null ? 0.0 : rank.clairvoyanceProficiency());
   }

   public static boolean hasClairvoyanceSource(boolean independentlyGranted, net.xxxjk.TYPE_MOON_WORLD.passive.PassiveRank rank) {
      return independentlyGranted || rank != null;
   }

   public static int monstrousStrengthLevel(double proficiency) {
      return Mth.clamp(1 + (int)Math.floor(Mth.clamp(proficiency, 0.0, 100.0) / 20.0), 1, 6);
   }

   public static int monstrousStrengthDurationTicks(double proficiency) {
      double seconds = 60.0 + Mth.clamp(proficiency, 0.0, 100.0) * 2.4;
      return Math.max(1, (int)Math.round(seconds * 20.0));
   }

   public static int maxZoom(double proficiency) {
      return Mth.clamp(2 + (int)Math.floor(Mth.clamp(proficiency, 0.0, 100.0) / 20.0) * 2, 2, 12);
   }

   public static boolean cast(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, String id) {
      if (player == null || vars == null || PassiveService.effectsSuppressed(vars) || !owns(vars, id)) return false;
      if (MONSTROUS_STRENGTH.equals(id)) {
         double proficiency = proficiency(vars, id);
         int level = monstrousStrengthLevel(proficiency);
         int duration = monstrousStrengthDurationTicks(proficiency);
         player.getPersistentData().remove(STRENGTH_UNTIL_TAG);
         player.getPersistentData().remove(STRENGTH_AMPLIFIER_TAG);
         player.removeEffect(ModMobEffects.MONSTROUS_STRENGTH);
         player.getPersistentData().putLong(STRENGTH_UNTIL_TAG, player.level().getGameTime() + duration);
         player.getPersistentData().putInt(STRENGTH_AMPLIFIER_TAG, level - 1);
         player.addEffect(new MobEffectInstance(ModMobEffects.MONSTROUS_STRENGTH, duration, level - 1, false, true, true));
         return true;
      }
      if (CLAIRVOYANCE.equals(id)) {
         PacketDistributor.sendToPlayer(player, new ClairvoyanceStateMessage(true, maxZoom(proficiency(vars, id))));
         return true;
      }
      return false;
   }

   public static void tick(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (PassiveService.effectsSuppressed(vars)) {
         suspendActiveEffects(player);
         return;
      }
      if (player.getPersistentData().getBoolean(EFFECTS_SUSPENDED_TAG)) {
         resumeActiveEffects(player);
         return;
      }
      long until = player.getPersistentData().getLong(STRENGTH_UNTIL_TAG);
      long now = player.level().getGameTime();
      if (until <= now) {
         player.getPersistentData().remove(STRENGTH_UNTIL_TAG);
         player.getPersistentData().remove(STRENGTH_AMPLIFIER_TAG);
         return;
      }
      if (!player.hasEffect(ModMobEffects.MONSTROUS_STRENGTH)) {
         int amplifier = Mth.clamp(player.getPersistentData().getInt(STRENGTH_AMPLIFIER_TAG), 0, 5);
         player.addEffect(new MobEffectInstance(ModMobEffects.MONSTROUS_STRENGTH, (int)Math.min(Integer.MAX_VALUE, until - now), amplifier, false, true, true));
      }
   }

   public static boolean shouldPreventRemoval(ServerPlayer player) {
      if (player == null) return false;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return !PassiveService.effectsSuppressed(vars)
         && player.getPersistentData().getLong(STRENGTH_UNTIL_TAG) > player.level().getGameTime();
   }

   public static void suspendActiveEffects(ServerPlayer player) {
      if (player == null) return;
      var data = player.getPersistentData();
      if (data.getBoolean(EFFECTS_SUSPENDED_TAG)) return;
      data.putBoolean(EFFECTS_SUSPENDED_TAG, true);
      long remaining = data.getLong(STRENGTH_UNTIL_TAG) - player.level().getGameTime();
      if (remaining > 0L) {
         data.putLong(STRENGTH_SUSPENDED_REMAINING_TAG, remaining);
      } else {
         data.remove(STRENGTH_SUSPENDED_REMAINING_TAG);
         data.remove(STRENGTH_AMPLIFIER_TAG);
      }
      data.remove(STRENGTH_UNTIL_TAG);
      player.removeEffect(ModMobEffects.MONSTROUS_STRENGTH);
      resetClairvoyance(player);
   }

   public static void resumeActiveEffects(ServerPlayer player) {
      if (player == null) return;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (PassiveService.effectsSuppressed(vars)) return;
      var data = player.getPersistentData();
      data.remove(EFFECTS_SUSPENDED_TAG);
      long remaining = data.getLong(STRENGTH_SUSPENDED_REMAINING_TAG);
      data.remove(STRENGTH_SUSPENDED_REMAINING_TAG);
      if (remaining > 0L) {
         data.putLong(STRENGTH_UNTIL_TAG, player.level().getGameTime() + remaining);
      }
      tick(player);
   }

   public static void clearMonstrousStrength(ServerPlayer player) {
      if (player == null) return;
      player.getPersistentData().remove(STRENGTH_UNTIL_TAG);
      player.getPersistentData().remove(STRENGTH_AMPLIFIER_TAG);
      player.getPersistentData().remove(STRENGTH_SUSPENDED_REMAINING_TAG);
      player.getPersistentData().remove(EFFECTS_SUSPENDED_TAG);
      player.removeEffect(ModMobEffects.MONSTROUS_STRENGTH);
   }

   public static void resetClairvoyance(ServerPlayer player) {
      if (player != null) PacketDistributor.sendToPlayer(player, new ClairvoyanceStateMessage(false, 2));
   }

   public static void clearActiveState(ServerPlayer player) {
      clearMonstrousStrength(player);
      resetClairvoyance(player);
   }
}
