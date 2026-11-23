package net.xxxjk.TYPE_MOON_WORLD.servant.heracles;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.network.HeraclesActionAnimationMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ModNetwork;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.joml.Vector3f;

/** Presentation only: no movement, health, hit timing, resource or invulnerability changes. */
public final class HeraclesPresentationService {
   public static final int REVIVE = 9;
   public static final int ADAPT = 10;
   public static final int IMPACT = 11;
   private static final Map<LivingEntity, Stride> STRIDES = new WeakHashMap<>();
   private static final DustParticleOptions REVIVAL_DUST = new DustParticleOptions(new Vector3f(.58F,.13F,.08F),1.0F);
   private static final DustParticleOptions ADAPTATION_DUST = new DustParticleOptions(new Vector3f(.83F,.61F,.25F),.8F);

   private static final class Stride {
      Vec3 previous;
      double distance;
      long nextStep;
      Stride(Vec3 position) { previous = position; }
   }

   private HeraclesPresentationService() { }

   public static void playerAnimation(ServerPlayer player, int action) {
      var message = new HeraclesActionAnimationMessage(player.getUUID(), action);
      for (var observer : player.serverLevel().players()) {
         if (observer == player || observer.distanceToSqr(player) <= 128 * 128) ModNetwork.sendToPlayer(observer, message);
      }
   }

   public static void revive(LivingEntity entity) {
      if (!(entity.level() instanceof ServerLevel level)) return;
      if (entity instanceof HeraclesEntity npc) npc.triggerGodHandReaction(false);
      if (entity instanceof ServerPlayer player) playerAnimation(player, REVIVE);
      level.sendParticles(REVIVAL_DUST,entity.getX(),entity.getY()+entity.getBbHeight()*.6,entity.getZ(),20,.42,.5,.42,.01);
      level.sendParticles(ParticleTypes.CLOUD,entity.getX(),entity.getY()+.12,entity.getZ(),8,.55,.06,.55,.025);
      level.playSound(null,entity.blockPosition(),SoundEvents.IRON_GOLEM_REPAIR,SoundSource.HOSTILE,1F,.65F);
      level.playSound(null,entity.blockPosition(),SoundEvents.WARDEN_HEARTBEAT,SoundSource.HOSTILE,.85F,.7F);
   }

   public static void adapted(LivingEntity entity) {
      if (!(entity.level() instanceof ServerLevel level)) return;
      if (entity instanceof HeraclesEntity npc) npc.triggerGodHandReaction(true);
      if (entity instanceof ServerPlayer player) playerAnimation(player, ADAPT);
      level.sendParticles(ADAPTATION_DUST,entity.getX(),entity.getY()+entity.getBbHeight()*.7,entity.getZ(),10,.36,.3,.36,.005);
      level.playSound(null,entity.blockPosition(),SoundEvents.BEACON_ACTIVATE,SoundSource.HOSTILE,.55F,.65F);
   }

   public static void impact(LivingEntity entity) {
      if (entity instanceof HeraclesEntity npc) npc.triggerNamedActionAnimation("frenzy_impact");
      if (entity instanceof ServerPlayer player) playerAnimation(player, IMPACT);
   }

   public static void tick(LivingEntity entity) {
      if (!(entity.level() instanceof ServerLevel level)) return;
      if (!entity.isAlive() || !HeraclesAbilityService.isHeracles(entity)) { clear(entity); return; }
      Stride stride = STRIDES.computeIfAbsent(entity, ignored -> new Stride(entity.position()));
      double moved = entity.position().subtract(stride.previous).horizontalDistance();
      stride.previous = entity.position();
      if (!entity.onGround() || entity.isPassenger() || entity.isInWater() || HeraclesAbilityService.busy(entity)
         || HeraclesAbilityService.controlled(entity) || ServantCombatSystem.cannotAct(entity)
         || EntityUtils.isUntargetableServantTransition(entity) || moved < .01 || moved > 1.25) { stride.distance = 0; return; }
      stride.distance += moved;
      if (stride.distance < 1.35 || level.getGameTime() < stride.nextStep) return;
      stride.distance %= 1.35;
      stride.nextStep = level.getGameTime()+4;
      level.playSound(null,entity.blockPosition(),SoundEvents.IRON_GOLEM_STEP,SoundSource.HOSTILE,.8F,.72F);
      var ground = level.getBlockState(BlockPos.containing(entity.position().add(0,-.15,0)));
      if (!ground.isAir() && ground.getFluidState().isEmpty()) {
         level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground),entity.getX(),entity.getY()+.08,entity.getZ(),4,.25,.03,.25,.015);
      }
   }

   public static void clear(LivingEntity entity) { STRIDES.remove(entity); }
}
