package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.DragonfangSoldierEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MedeaBeamEffectEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MedeaMagicBoltEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiContext;
import org.joml.Vector3f;

public final class MedeaCombatHelper {
   private static final String TAG_LAST_BOLT_TICK = "MedeaLastBoltTick";
   private static final String TAG_LAST_LIGHTNING_TICK = "MedeaLastLightningTick";
   private static final String TAG_LAST_BEAM_TICK = "MedeaLastBeamTick";
   private static final String TAG_LAST_BARRIER_TICK = "MedeaLastBarrierTick";
   private static final String TAG_LAST_TELEPORT_TICK = "MedeaLastTeleportTick";
   private static final String TAG_LAST_FLEECE_TICK = "MedeaLastFleeceTick";
   private static final String TAG_LAST_RULE_BREAKER_TICK = "MedeaLastRuleBreakerTick";
   private static final String TAG_LAST_COMBAT_SUMMON_TICK = "MedeaLastCombatSummonTick";
   private static final DustParticleOptions CIRCLE_PRIMARY = new DustParticleOptions(new Vector3f(0.45F, 0.65F, 1.0F), 1.2F);
   private static final DustParticleOptions CIRCLE_ACCENT = new DustParticleOptions(new Vector3f(0.72F, 0.28F, 1.0F), 1.0F);

   private MedeaCombatHelper() {
   }

   public static void tick(MedeaEntity entity, ServantAiContext context) {
      LivingEntity target = context.target();
      if (target == null || target.isDeadOrDying()) {
         entity.setFlyingMode(false);
         entity.clearTemporaryFocusItem();
         return;
      }

      long now = context.gameTick();
      consumeUtilityItems(entity);
      maybeUseGoldenFleece(entity, now);
      maybeCombatSummon(entity, now);

      double distance = entity.distanceTo(target);
      if (distance > 8.0) {
         entity.setFlyingMode(true);
      } else if (distance < 6.0) {
         entity.setFlyingMode(false);
      }

      entity.getLookControl().setLookAt(target, 30.0F, 30.0F);

      if (distance <= 4.0) {
         if (!entity.isBarrierActive() && now - entity.getPersistentData().getLong(TAG_LAST_BARRIER_TICK) >= 200L && castBarrier(entity, now)) {
            return;
         }
         if (MedeaWorkshopHelper.canTeleportNow(entity, now) && castTeleport(entity, target, now)) {
            return;
         }
         if (shouldUseRuleBreaker(entity, target, now) && castRuleBreaker(entity, target, now)) {
            return;
         }
         if (canCastBolt(entity, now)) {
            castBolt(entity, target, now);
         }
         return;
      }

      if (distance <= 8.0) {
         if (shouldUseRuleBreaker(entity, target, now) && castRuleBreaker(entity, target, now)) {
            return;
         }
         if (canCastLightning(entity, now)) {
            castLightning(entity, target, now);
            return;
         }
         if (canCastBolt(entity, now)) {
            castBolt(entity, target, now);
         }
         return;
      }

      if (canCastBeam(entity, now) && castBeamVolley(entity, target, now)) {
         return;
      }
      if (canCastBolt(entity, now)) {
         castBolt(entity, target, now);
      }
   }

   public static void applyRuleBreakerHit(LivingEntity target, LivingEntity attacker) {
      if (target == null || !target.isAlive()) {
         return;
      }

      Collection<MobEffectInstance> activeEffects = List.copyOf(target.getActiveEffects());
      for (MobEffectInstance effect : activeEffects) {
         if (effect.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) {
            target.removeEffect(effect.getEffect());
         }
      }

      target.removeEffect(ModMobEffects.REINFORCEMENT_SELF_AGILITY);
      target.removeEffect(ModMobEffects.REINFORCEMENT_SELF_DEFENSE);
      target.removeEffect(ModMobEffects.REINFORCEMENT_SELF_SIGHT);
      target.removeEffect(ModMobEffects.REINFORCEMENT_SELF_STRENGTH);
      target.removeEffect(ModMobEffects.REINFORCEMENT_OTHER_AGILITY);
      target.removeEffect(ModMobEffects.REINFORCEMENT_OTHER_DEFENSE);
      target.removeEffect(ModMobEffects.REINFORCEMENT_OTHER_SIGHT);
      target.removeEffect(ModMobEffects.REINFORCEMENT_OTHER_STRENGTH);

      if (target instanceof ServantEntity servant) {
         servant.getPersistentData().remove(CuChulainnCombatHelper.PROTECTION_FROM_ARROWS_TAG);
         servant.getPersistentData().remove(CuChulainnCombatHelper.ALGIZ_SHIELD_TAG);
         servant.getPersistentData().remove(CuChulainnCombatHelper.GAE_BOLG_WINDUP_UNTIL_TAG);
         CuChulainnCombatHelper.clearRune(servant);
      }

      if (target.getPersistentData().getBoolean(MedeaWorkshopHelper.TAG_MAGIC_SUMMON)) {
         target.invulnerableTime = 0;
         target.hurt(attacker != null ? attacker.damageSources().magic() : target.damageSources().magic(), Float.MAX_VALUE);
         if (target.isAlive()) {
            target.discard();
         }
      }

      purgeOwnedSummons(target);
   }

   private static boolean canCastBolt(MedeaEntity entity, long now) {
      return now - entity.getPersistentData().getLong(TAG_LAST_BOLT_TICK) >= 8L && entity.getCurrentMp() >= MedeaWorkshopHelper.adjustedManaCost(entity, 5.0);
   }

   private static boolean canCastLightning(MedeaEntity entity, long now) {
      return now - entity.getPersistentData().getLong(TAG_LAST_LIGHTNING_TICK) >= 18L && entity.getCurrentMp() >= MedeaWorkshopHelper.adjustedManaCost(entity, 15.0);
   }

   private static boolean canCastBeam(MedeaEntity entity, long now) {
      return now - entity.getPersistentData().getLong(TAG_LAST_BEAM_TICK) >= 400L && entity.getCurrentMp() >= MedeaWorkshopHelper.adjustedManaCost(entity, 80.0);
   }

   private static void castBolt(MedeaEntity entity, LivingEntity target, long now) {
      if (!(entity.level() instanceof ServerLevel level) || !MedeaWorkshopHelper.consumeMana(entity, 5.0)) {
         return;
      }
      entity.getPersistentData().putLong(TAG_LAST_BOLT_TICK, now);
      Vec3 direction = getAimDirection(entity, target);
      Vec3 spawnPos = entity.getEyePosition().add(direction.scale(0.75));
      spawnMagicCircle(level, spawnPos.subtract(direction.scale(0.85)), direction, 1.3F);
      MedeaMagicBoltEntity projectile = new MedeaMagicBoltEntity(level, entity);
      projectile.setMode(MedeaMagicBoltEntity.Mode.BOLT);
      projectile.setMagicDamage(MedeaWorkshopHelper.applyWorkshopDamageBonus(entity, 25.0F));
      projectile.setPos(spawnPos);
      projectile.shoot(direction.x, direction.y, direction.z, 2.9F, 0.04F);
      level.addFreshEntity(projectile);
      level.playSound(null, entity.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 0.4F, 1.5F);
   }

   private static void castLightning(MedeaEntity entity, LivingEntity target, long now) {
      if (!(entity.level() instanceof ServerLevel level) || !MedeaWorkshopHelper.consumeMana(entity, 15.0)) {
         return;
      }
      entity.getPersistentData().putLong(TAG_LAST_LIGHTNING_TICK, now);
      ServantVoiceHelper.tryPlaySpell(entity);
      Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
      spawnMagicCircle(level, entity.position().add(0.0, entity.getBbHeight() * 0.45, 0.0), getAimDirection(entity, target), 1.4F);
      for (int i = 0; i < 5; i++) {
         double y = center.y + 4.0 - i * 1.1;
         level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, y, center.z, 8, 0.08, 0.12, 0.08, 0.02);
      }
      level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 12, 0.3, 0.5, 0.3, 0.04);
      target.hurt(entity.damageSources().magic(), MedeaWorkshopHelper.applyWorkshopDamageBonus(entity, 60.0F));
      target.invulnerableTime = 0;
      level.playSound(null, BlockPos.containing(center), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.HOSTILE, 0.9F, 1.2F);
   }

   private static boolean castBeamVolley(MedeaEntity entity, LivingEntity target, long now) {
      if (!(entity.level() instanceof ServerLevel level) || !MedeaWorkshopHelper.consumeMana(entity, 80.0)) {
         return false;
      }
      entity.getPersistentData().putLong(TAG_LAST_BEAM_TICK, now);
      entity.setTemporaryFocusItem(MedeaEntity.FocusItem.HECATES_STAFF);
      ServantVoiceHelper.tryPlaySpell(entity);
      int shotCount = entity.isInsideWorkshop() ? 5 : 3;
      float damage = MedeaWorkshopHelper.applyWorkshopDamageBonus(entity, 70.0F);

      for (int i = 0; i < shotCount; i++) {
         TYPE_MOON_WORLD.queueServerWork(i * 6 + 1, () -> {
            if (!(entity.level() instanceof ServerLevel serverLevel) || !entity.isAlive()) {
               return;
            }
            LivingEntity resolvedTarget = target.isAlive() ? target : entity.getTarget();
            if (resolvedTarget == null || !resolvedTarget.isAlive()) {
               return;
            }
            Vec3 start = entity.getEyePosition().add(entity.getLookAngle().scale(0.8));
            Vec3 end = resolvedTarget.position().add(0.0, resolvedTarget.getBbHeight() * 0.5, 0.0);
            Vec3 direction = end.subtract(start).normalize();
            spawnMagicCircle(serverLevel, start.subtract(direction.scale(0.95)), direction, 1.8F);
            MedeaBeamEffectEntity beam = new MedeaBeamEffectEntity(serverLevel, entity, start, end, damage, 10);
            serverLevel.addFreshEntity(beam);
            serverLevel.playSound(null, BlockPos.containing(start), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 0.65F, 1.4F);
         });
      }
      TYPE_MOON_WORLD.queueServerWork(shotCount * 6 + 10, () -> {
         if (entity.isAlive() && entity.getTemporaryFocusItem() == MedeaEntity.FocusItem.HECATES_STAFF) {
            entity.clearTemporaryFocusItem();
         }
      });
      return true;
   }

   private static boolean castBarrier(MedeaEntity entity, long now) {
      if (!(entity.level() instanceof ServerLevel level) || !MedeaWorkshopHelper.consumeMana(entity, 25.0)) {
         return false;
      }
      entity.getPersistentData().putLong(TAG_LAST_BARRIER_TICK, now);
      entity.setBarrierStrength(100.0F);
      spawnMagicCircle(level, entity.position().add(0.0, 0.1, 0.0), entity.getLookAngle(), 2.2F);
      for (LivingEntity living : level.getEntitiesOfClass(
         LivingEntity.class,
         entity.getBoundingBox().inflate(3.2),
         target -> target != entity && target.isAlive() && !target.isAlliedTo(entity) && !net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils.isImmunePlayerTarget(target)
      )) {
         Vec3 push = living.position().subtract(entity.position()).normalize().scale(1.2);
         living.push(push.x, 0.35, push.z);
         living.hurtMarked = true;
      }
      level.playSound(null, entity.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 0.8F, 1.2F);
      return true;
   }

   private static boolean castTeleport(MedeaEntity entity, LivingEntity target, long now) {
      if (!(entity.level() instanceof ServerLevel level) || !MedeaWorkshopHelper.consumeMana(entity, 20.0)) {
         return false;
      }
      BlockPos destination = MedeaWorkshopHelper.findTeleportPosition(entity);
      Vec3 from = entity.position();
      entity.teleportTo(destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5);
      entity.setDeltaMovement(Vec3.ZERO);
      entity.fallDistance = 0.0F;
      entity.setFlyingMode(false);
      MedeaWorkshopHelper.markTeleportUsed(entity, now);
      level.sendParticles(ParticleTypes.REVERSE_PORTAL, from.x, from.y + 0.6, from.z, 18, 0.3, 0.4, 0.3, 0.05);
      level.sendParticles(ParticleTypes.REVERSE_PORTAL, entity.getX(), entity.getY() + 0.6, entity.getZ(), 18, 0.3, 0.4, 0.3, 0.05);
      level.playSound(null, destination, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.1F);
      return true;
   }

   private static boolean castRuleBreaker(MedeaEntity entity, LivingEntity target, long now) {
      if (!(entity.level() instanceof ServerLevel level) || !MedeaWorkshopHelper.consumeMana(entity, 40.0)) {
         return false;
      }
      entity.getPersistentData().putLong(TAG_LAST_RULE_BREAKER_TICK, now);
      entity.setTemporaryFocusItem(MedeaEntity.FocusItem.RULE_BREAKER);
      ServantVoiceHelper.tryPlayRuleBreaker(entity);
      Vec3 direction = getAimDirection(entity, target);
      if (entity.distanceToSqr(target) <= 12.25) {
         applyRuleBreakerHit(target, entity);
         target.hurt(entity.damageSources().mobAttack(entity), 4.0F);
      } else {
         Vec3 spawnPos = entity.getEyePosition().add(direction.scale(0.65));
         spawnMagicCircle(level, spawnPos.subtract(direction.scale(0.65)), direction, 1.2F);
         MedeaMagicBoltEntity projectile = new MedeaMagicBoltEntity(level, entity);
         projectile.setMode(MedeaMagicBoltEntity.Mode.RULE_BREAKER);
         projectile.setMagicDamage(4.0F);
         projectile.setPos(spawnPos);
         projectile.shoot(direction.x, direction.y, direction.z, 2.0F, 0.0F);
         level.addFreshEntity(projectile);
      }
      TYPE_MOON_WORLD.queueServerWork(18, () -> {
         if (entity.isAlive() && entity.getTemporaryFocusItem() == MedeaEntity.FocusItem.RULE_BREAKER) {
            entity.clearTemporaryFocusItem();
         }
      });
      return true;
   }

   private static boolean shouldUseRuleBreaker(MedeaEntity entity, LivingEntity target, long now) {
      if (now - entity.getPersistentData().getLong(TAG_LAST_RULE_BREAKER_TICK) < 200L) {
         return false;
      }
      if (entity.getCurrentMp() < MedeaWorkshopHelper.adjustedManaCost(entity, 40.0)) {
         return false;
      }
      if (target.getPersistentData().getBoolean(MedeaWorkshopHelper.TAG_MAGIC_SUMMON)) {
         return true;
      }
      for (MobEffectInstance effect : target.getActiveEffects()) {
         if (effect.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) {
            return true;
         }
      }
      return target instanceof ServantEntity;
   }

   private static void maybeUseGoldenFleece(MedeaEntity entity, long now) {
      if (entity.getHealth() >= entity.getMaxHealth() * 0.3F) {
         return;
      }
      if (now - entity.getPersistentData().getLong(TAG_LAST_FLEECE_TICK) < 400L) {
         return;
      }
      entity.getPersistentData().putLong(TAG_LAST_FLEECE_TICK, now);
      entity.setHealth(entity.getMaxHealth());
      if (entity.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(), 24, 0.4, 0.6, 0.4, 0.05);
         level.playSound(null, entity.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.HOSTILE, 1.0F, 1.1F);
      }
   }

   private static void consumeUtilityItems(MedeaEntity entity) {
      if (entity.getHealth() < entity.getMaxHealth() * 0.3F && MedeaWorkshopHelper.getHealCharmStock(entity) > 0) {
         MedeaWorkshopHelper.setHealCharmStock(entity, MedeaWorkshopHelper.getHealCharmStock(entity) - 1);
         entity.heal(40.0F);
      }
      if (entity.getCurrentMp() < entity.getMaxMp() * 0.2 && MedeaWorkshopHelper.getManaCharmStock(entity) > 0) {
         MedeaWorkshopHelper.setManaCharmStock(entity, MedeaWorkshopHelper.getManaCharmStock(entity) - 1);
         entity.setCurrentMp(Math.min(entity.getMaxMp(), entity.getCurrentMp() + 50.0));
      }
   }

   private static void maybeCombatSummon(MedeaEntity entity, long now) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }
      if (now - entity.getPersistentData().getLong(TAG_LAST_COMBAT_SUMMON_TICK) < 80L) {
         return;
      }
      if (MedeaWorkshopHelper.getDragonfangStock(entity) <= 0) {
         return;
      }
      int active = MedeaWorkshopHelper.countOwnedDragonfangs(level, entity.getUUID(), entity.position());
      if (active >= 6) {
         return;
      }
      if (entity.getCurrentMp() < 60.0) {
         return;
      }
      if (MedeaWorkshopHelper.summonDragonfang(entity, level)) {
         entity.getPersistentData().putLong(TAG_LAST_COMBAT_SUMMON_TICK, now);
      }
   }

   private static Vec3 getAimDirection(MedeaEntity entity, LivingEntity target) {
      Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(entity.getEyePosition());
      return aim.lengthSqr() < 1.0E-6 ? entity.getLookAngle() : aim.normalize();
   }

   private static void spawnMagicCircle(ServerLevel level, Vec3 center, Vec3 direction, float scale) {
      Vec3 forward = direction.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : direction.normalize();
      Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
      if (right.lengthSqr() < 1.0E-6) {
         right = new Vec3(1.0, 0.0, 0.0);
      } else {
         right = right.normalize();
      }
      Vec3 planeForward = right.cross(new Vec3(0.0, 1.0, 0.0)).normalize();
      List<Vec3> vertices = new ArrayList<>();
      for (int i = 0; i < 5; i++) {
         double angle = -Math.PI / 2.0 + i * (Math.PI * 2.0 / 5.0);
         vertices.add(center.add(right.scale(Math.cos(angle) * scale)).add(planeForward.scale(Math.sin(angle) * scale)));
      }
      drawRing(level, center, right, planeForward, scale * 1.15, 24, CIRCLE_PRIMARY);
      drawStar(level, vertices, CIRCLE_ACCENT);
   }

   private static void drawRing(ServerLevel level, Vec3 center, Vec3 right, Vec3 forward, double radius, int samples, DustParticleOptions particle) {
      for (int i = 0; i < samples; i++) {
         double angle = i * (Math.PI * 2.0 / samples);
         Vec3 pos = center.add(right.scale(Math.cos(angle) * radius)).add(forward.scale(Math.sin(angle) * radius));
         level.sendParticles(particle, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
      }
   }

   private static void drawStar(ServerLevel level, List<Vec3> vertices, DustParticleOptions particle) {
      int[][] edges = new int[][]{{0, 2}, {2, 4}, {4, 1}, {1, 3}, {3, 0}};
      for (int[] edge : edges) {
         Vec3 start = vertices.get(edge[0]);
         Vec3 end = vertices.get(edge[1]);
         for (double t = 0.0; t <= 1.0; t += 0.1) {
            Vec3 pos = start.lerp(end, t);
            level.sendParticles(particle, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
         }
      }
   }

   private static void purgeOwnedSummons(LivingEntity target) {
      if (!(target.level() instanceof ServerLevel serverLevel)) {
         return;
      }
      String ownerId = target.getUUID().toString();
      AABB purgeBox = target.getBoundingBox().inflate(192.0);
      for (DragonfangSoldierEntity summon : serverLevel.getEntitiesOfClass(
         DragonfangSoldierEntity.class,
         purgeBox,
         dragonfang -> dragonfang.isAlive() && ownerId.equals(dragonfang.getPersistentData().getString(MedeaWorkshopHelper.TAG_MAGIC_SUMMON_OWNER))
      )) {
         summon.discard();
      }
   }
}
