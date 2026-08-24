package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantCombatTempoService;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterProtection;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesCombatRules;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public final class ServantTrueSweepService {
   private static final String PLAYER_NEXT_SWEEP = "ServantCardTrueSweepNextTick";
   private static final String NPC_NEXT_SWEEP = "ServantTrueSweepNextTick";
   private static final double MIN_SWEEP_RANGE = 3.0;
   private static final double MAX_SWEEP_RANGE = 6.0;

   private ServantTrueSweepService() {
   }

   public static void clearPlayer(ServerPlayer player) {
      if (player != null) {
         player.getPersistentData().remove(PLAYER_NEXT_SWEEP);
      }
   }

   public static int intervalTicks(double attackSpeed) {
      double speed = Math.max(0.1, attackSpeed);
      return Math.max(1, Mth.ceil(20.0 / speed));
   }

   public static boolean tryPlayerSweep(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (player == null || vars == null || !vars.servant_card_transformed || !(player.level() instanceof ServerLevel level)) {
         return false;
      }
      long now = level.getGameTime();
      int interval = intervalTicks(player.getAttributeValue(Attributes.ATTACK_SPEED));
      CompoundTag data = player.getPersistentData();
      long next = data.getLong(PLAYER_NEXT_SWEEP);
      data.putLong(PLAYER_NEXT_SWEEP, now + interval);
      if (now < next) {
         return false;
      }
      Vec3 look = PlayerNoblePhantasmHelper.horizontalLook(player);
      double range = sweepRange(player);
      float damage = (float)player.getAttributeValue(Attributes.ATTACK_DAMAGE);
      int hits = sweep(level, player, look, range, damage, true);
      spawnSweepFx(level, player, look, range, hits);
      return true;
   }

   public static boolean tryNpcSweep(ServantEntity servant, LivingEntity primaryTarget, long now) {
      if (servant == null || primaryTarget == null || !primaryTarget.isAlive() || !(servant.level() instanceof ServerLevel level)) {
         return false;
      }
      int interval = intervalTicks(servant.getAttributeValue(Attributes.ATTACK_SPEED));
      CompoundTag data = servant.getPersistentData();
      long next = data.getLong(NPC_NEXT_SWEEP);
      data.putLong(NPC_NEXT_SWEEP, now + interval);
      if (now < next) {
         return false;
      }
      Vec3 forward = primaryTarget.position().subtract(servant.position()).multiply(1.0, 0.0, 1.0);
      if (forward.lengthSqr() < 1.0E-4) {
         forward = servant.getLookAngle().multiply(1.0, 0.0, 1.0);
      }
      if (forward.lengthSqr() < 1.0E-4) {
         forward = new Vec3(0.0, 0.0, 1.0);
      }
      double range = Math.max(sweepRange(servant), ServantCombatTempoService.basicAttackReach(servant, primaryTarget));
      float damage = (float)servant.getAttributeValue(Attributes.ATTACK_DAMAGE);
      int hits = sweep(level, servant, forward.normalize(), range, damage, false);
      spawnSweepFx(level, servant, forward.normalize(), range, hits);
      return true;
   }

   private static double sweepRange(LivingEntity attacker) {
      double range = attacker.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
      return Mth.clamp(range, MIN_SWEEP_RANGE, MAX_SWEEP_RANGE);
   }

   private static int sweep(ServerLevel level, LivingEntity attacker, Vec3 forward, double range, float damage, boolean playerSource) {
      AABB area = attacker.getBoundingBox().inflate(range, 1.6, range).move(forward.scale(Math.min(0.8, range * 0.16)));
      int hits = 0;
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
         entity -> isSweepTarget(attacker, entity))) {
         Vec3 offset = target.position().subtract(attacker.position());
         if (offset.horizontalDistanceSqr() > range * range) continue;
         if (!HeraclesCombatRules.isInsideBasicSweepArc(forward.x, forward.z, offset.x, offset.z)) continue;
         if (!attacker.hasLineOfSight(target) && attacker.distanceTo(target) > 1.8F) continue;
         target.invulnerableTime = 0;
         boolean hurt = target.hurt(playerSource && attacker instanceof ServerPlayer player
            ? attacker.damageSources().playerAttack(player)
            : attacker.damageSources().mobAttack(attacker), damage);
         target.invulnerableTime = 0;
         if (hurt) {
            hits++;
         }
         Vec3 push = new Vec3(offset.x, 0.0, offset.z);
         if (push.lengthSqr() > 1.0E-4) {
            push = push.normalize();
            target.push(push.x * 0.45, 0.08, push.z * 0.45);
            target.hurtMarked = true;
         }
      }
      return hits;
   }

   private static boolean isSweepTarget(LivingEntity attacker, LivingEntity target) {
      return target != null
         && target != attacker
         && target.isAlive()
         && !EntityUtils.isImmunePlayerTarget(target)
         && !attacker.isAlliedTo(target)
         && !target.isAlliedTo(attacker)
         && !ServantMasterProtection.isProtectedMaster(attacker, target);
   }

   private static void spawnSweepFx(ServerLevel level, LivingEntity attacker, Vec3 forward, double range, int hits) {
      Vec3 center = attacker.position().add(forward.scale(Math.min(range * 0.45, 2.2))).add(0.0, attacker.getBbHeight() * 0.55, 0.0);
      level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z, hits > 0 ? 5 : 2, 0.65, 0.28, 0.65, 0.0);
      level.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z, hits > 0 ? 14 : 6, 0.95, 0.32, 0.95, 0.08);
      level.playSound(null, BlockPos.containing(center), SoundEvents.PLAYER_ATTACK_SWEEP, attacker instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, 0.85F, 0.88F);
   }
}
