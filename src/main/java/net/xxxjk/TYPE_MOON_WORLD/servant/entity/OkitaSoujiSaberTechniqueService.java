package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterDamageRouter;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterTargeting;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SasakiKojiroAction;
import net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SwordActionAnimationService;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.network.OkitaMumyoudanImpactMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ModNetwork;
import org.joml.Vector3f;

public final class OkitaSoujiSaberTechniqueService {
   public static final int MUMYOUDAN_WINDUP_TICKS = 10;
   public static final double MUMYOUDAN_RANGE = 3.0;
   // Source peaks: 0.2083, 0.3333, 0.4583 seconds, baked into 14 motion ticks.
   // Round up so server damage never precedes the corresponding sword extension.
   private static final int[] MUMYOUDAN_STRIKE_TICKS = {5, 7, 10};
   private static final float MUMYOUDAN_DAMAGE = 150.0F;
   private static final String EXECUTION_UNTIL = "OkitaMumyoudanExecutionUntil";
   private static final String EXECUTION_TOKEN = "OkitaMumyoudanExecutionToken";
   private static final DustParticleOptions ASAGI_IMPACT = new DustParticleOptions(new Vector3f(0.22F, 0.73F, 0.76F), 0.85F);

   private OkitaSoujiSaberTechniqueService() { }

   public static int[] mumyoudanStrikeTicks() {
      return MUMYOUDAN_STRIKE_TICKS.clone();
   }

   public static boolean isExecuting(LivingEntity attacker) {
      return attacker.getPersistentData().getLong(EXECUTION_UNTIL) > attacker.level().getGameTime();
   }

   public static void cancel(LivingEntity attacker) {
      attacker.getPersistentData().remove(EXECUTION_UNTIL);
      attacker.getPersistentData().remove(EXECUTION_TOKEN);
   }

   public static boolean beginMumyoudan(LivingEntity attacker, LivingEntity target, Runnable afterTechnique) {
      if (attacker == null || !(attacker.level() instanceof ServerLevel level) || isExecuting(attacker)
         || !canLandMumyoudanStrike(attacker, target, level)) return false;
      String token = java.util.UUID.randomUUID().toString();
      attacker.getPersistentData().putString(EXECUTION_TOKEN, token);
      attacker.getPersistentData().putLong(EXECUTION_UNTIL,
         level.getGameTime() + MUMYOUDAN_WINDUP_TICKS + SasakiKojiroAction.OKITA_MUMYOUDAN_ZUKI.durationTicks());
      SwordActionAnimationService.play(attacker, SasakiKojiroAction.TSUBAME_WINDUP);
      TYPE_MOON_WORLD.queueServerWork(MUMYOUDAN_WINDUP_TICKS, () -> {
         if (!canContinue(attacker, level, token)) return;
         SwordActionAnimationService.play(attacker, SasakiKojiroAction.OKITA_MUMYOUDAN_ZUKI);
         level.playSound(null, attacker.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.25F, 1.55F);
      });
      for (int i = 0; i < MUMYOUDAN_STRIKE_TICKS.length; i++) {
         int strike = i;
         TYPE_MOON_WORLD.queueServerWork(MUMYOUDAN_WINDUP_TICKS + MUMYOUDAN_STRIKE_TICKS[i], () -> {
            if (canContinue(attacker, level, token)) strikeMumyoudanPoint(attacker, target, level, strike);
         });
      }
      TYPE_MOON_WORLD.queueServerWork(MUMYOUDAN_WINDUP_TICKS + SasakiKojiroAction.OKITA_MUMYOUDAN_ZUKI.durationTicks(), () -> {
         if (!canContinue(attacker, level, token)) return;
         cancel(attacker);
         afterTechnique.run();
      });
      return true;
   }

   private static boolean canContinue(LivingEntity attacker, ServerLevel level, String token) {
      if (!attacker.isAlive() || attacker.isRemoved() || attacker.level() != level
         || !token.equals(attacker.getPersistentData().getString(EXECUTION_TOKEN))) return false;
      if (attacker instanceof ServerPlayer player) {
         var state = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState();
         return !player.hasDisconnected() && state.transformed() && OkitaSoujiSaberEntity.SERVANT_KEY.equals(state.id());
      }
      return true;
   }

   public static boolean canLandMumyoudanStrike(LivingEntity attacker, LivingEntity target, ServerLevel level) {
      return attacker != null && target != null && attacker.isAlive() && !attacker.isRemoved()
         && target.isAlive() && !target.isRemoved() && attacker.level() == level && target.level() == level
         && target != attacker && !attacker.isAlliedTo(target) && !target.isAlliedTo(attacker)
         && (!(attacker instanceof ServerPlayer player) || !ServantMasterTargeting.isContractMaster(player, target))
         && attacker.distanceTo(target) <= MUMYOUDAN_RANGE && attacker.hasLineOfSight(target);
   }

   public static boolean strikeMumyoudanPoint(LivingEntity attacker, LivingEntity target, ServerLevel level, int strikeIndex) {
      if (!canLandMumyoudanStrike(attacker, target, level) || strikeIndex < 0 || strikeIndex >= MUMYOUDAN_STRIKE_TICKS.length) return false;
      Vec3 point = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      target.stopUsingItem();
      target.removeEffect(MobEffects.DAMAGE_RESISTANCE);
      target.removeEffect(MobEffects.ABSORPTION);
      target.invulnerableTime = 0;
      boolean damaged;
      try (var segment = ServantMasterDamageRouter.attackSegment()) {
         damaged = target.hurt(attacker.damageSources().source(OkitaSoujiSaberDamageTypes.MUMYOUDAN_ZUKI, attacker), MUMYOUDAN_DAMAGE);
      }
      target.invulnerableTime = 0;
      if (damaged) {
         target.getPersistentData().putLong("OkitaMumyoudanDefenseDisabledUntil", level.getGameTime() + 100L);
         playMumyoudanImpact(attacker, target, level, point, strikeIndex);
      }
      return damaged;
   }

   private static void playMumyoudanImpact(LivingEntity attacker, LivingEntity target, ServerLevel level, Vec3 point, int strike) {
      boolean finisher = strike == 2;
      SoundSource category = attacker instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE;
      float pitch = finisher ? 0.95F : 1.25F + strike * 0.12F;
      // A sharp piercing layer and a short low impact keep all three contacts distinct.
      level.playSound(null, point.x, point.y, point.z, SoundEvents.TRIDENT_HIT, category, finisher ? 1.35F : 1.0F, pitch);
      level.playSound(null, point.x, point.y, point.z, SoundEvents.PLAYER_ATTACK_STRONG, category, finisher ? 0.85F : 0.5F, finisher ? 0.65F : 0.9F);
      level.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, finisher ? 24 : 14, 0.18, 0.22, 0.18, finisher ? 0.18 : 0.10);
      level.sendParticles(ASAGI_IMPACT, point.x, point.y, point.z, finisher ? 22 : 12, 0.24, 0.28, 0.24, 0.03);
      // Only a confirmed hit produces feedback; a dodged/out-of-range segment stays silent.
      var message = new OkitaMumyoudanImpactMessage(attacker.getId(), target.getId(), strike);
      for (ServerPlayer viewer : level.players()) {
         if (viewer.distanceToSqr(target) <= 64.0 * 64.0) ModNetwork.sendToPlayer(viewer, message);
      }
   }
}
