package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.CombatThreat;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.CombatThreatService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactService;

/** Resolves opposed continuous Noble Phantasm beams as one shared combat action. */
public final class BeamClashManager {
   private static final String PARTNER_TAG = "TypeMoonBeamClashPartner";
   private static final String BALANCE_TAG = "TypeMoonBeamClashBalance";
   private static final String AGE_TAG = "TypeMoonBeamClashAge";
   private static final double MAX_DIRECTION_DOT = -Math.cos(Math.toRadians(12.0));
   private static final float WIN_BALANCE = 0.72F;
   private static final int MAX_CLASH_TICKS = 80;
   private static final int CONTEST_INTERVAL = 5;
   private static final ResourceLocation CLASH_THREAT = ResourceLocation.fromNamespaceAndPath(
      TYPE_MOON_WORLD.MOD_ID, "combat/beam_clash");

   private BeamClashManager() { }

   public static void tick(ServerLevel level, BeamClashParticipant beam) {
      Entity entity = beam.clashEntity();
      if (!entity.isAlive() || !beam.isBeamDamageActive()) return;

      BeamClashParticipant partner = resolvePartner(level, beam);
      if (partner == null) {
         if (!beam.isClashing()) findAndStart(level, beam);
         return;
      }
      beam.setClashing(true);
      partner.setClashing(true);
      if (entity.getId() > partner.clashEntity().getId()) return;

      LivingEntity ownerA = beam.beamOwner(level);
      LivingEntity ownerB = partner.beamOwner(level);
      if (ownerA == null || ownerB == null || !ownerA.isAlive() || !ownerB.isAlive()) {
         finishDraw(level, beam, partner, clashPoint(beam, partner, 0.0F), ownerA);
         return;
      }

      int age = entity.getPersistentData().getInt(AGE_TAG) + 1;
      setAge(beam, partner, age);
      float balance = entity.getPersistentData().getFloat(BALANCE_TAG);
      Vec3 center = clashPoint(beam, partner, balance);
      if (age % 2 == 0) spawnSustainFx(level, center, balance);
      if (age % CONTEST_INTERVAL != 0) return;

      float powerA = effectiveStrength(beam.clashPower(), manaFraction(ownerA));
      float powerB = effectiveStrength(partner.clashPower(), manaFraction(ownerB));
      balance = clamp(balance + pressureDelta(powerA, powerB), -1.0F, 1.0F);
      setBalance(beam, partner, balance);
      center = clashPoint(beam, partner, balance);
      publishMovingThreat(level, beam, center, age);

      drain(ownerA, maxMp(ownerA) * clashDrainFraction(powerB, powerA));
      drain(ownerB, maxMp(ownerB) * clashDrainFraction(powerA, powerB));
      boolean emptyA = currentMp(ownerA) <= 0.001;
      boolean emptyB = currentMp(ownerB) <= 0.001;
      if (emptyA && emptyB) {
         finishDraw(level, beam, partner, center, ownerA);
      } else if (emptyA || balance <= -WIN_BALANCE) {
         finishWithWinner(level, partner, beam, center, powerB, powerA, -balance, ownerB);
      } else if (emptyB || balance >= WIN_BALANCE) {
         finishWithWinner(level, beam, partner, center, powerA, powerB, balance, ownerA);
      } else if (age >= MAX_CLASH_TICKS) {
         if (powerA > powerB * 1.08F) {
            finishWithWinner(level, beam, partner, center, powerA, powerB, balance, ownerA);
         } else if (powerB > powerA * 1.08F) {
            finishWithWinner(level, partner, beam, center, powerB, powerA, -balance, ownerB);
         } else {
            finishDraw(level, beam, partner, center, ownerA);
         }
      }
   }

   private static void findAndStart(ServerLevel level, BeamClashParticipant beam) {
      Entity sourceEntity = beam.clashEntity();
      double inflate = beam.beamHalfWidth() + 18.0;
      AABB search = new AABB(beam.beamStart(), beam.beamEnd()).inflate(inflate);
      for (Entity candidate : level.getEntities(sourceEntity, search, entity -> entity instanceof BeamClashParticipant)) {
         BeamClashParticipant other = (BeamClashParticipant)candidate;
         if (!candidate.isAlive() || !other.isBeamDamageActive() || other.isClashing() || !canClash(level, beam, other)) continue;
         start(level, beam, other);
         return;
      }
   }

   private static boolean canClash(ServerLevel level, BeamClashParticipant a, BeamClashParticipant b) {
      if (a.beamType() == b.beamType()) return false;
      LivingEntity ownerA = a.beamOwner(level);
      LivingEntity ownerB = b.beamOwner(level);
      if (ownerA == null || ownerB == null || ownerA == ownerB || ownerA.isAlliedTo(ownerB) || ownerB.isAlliedTo(ownerA)) return false;
      return canClashGeometry(a.beamStart(), a.beamEnd(), a.beamHalfWidth(), b.beamStart(), b.beamEnd(), b.beamHalfWidth());
   }

   public static boolean canClashGeometry(Vec3 aStart, Vec3 aEnd, double aHalfWidth,
                                          Vec3 bStart, Vec3 bEnd, double bHalfWidth) {
      Vec3 da = aEnd.subtract(aStart);
      Vec3 db = bEnd.subtract(bStart);
      if (da.lengthSqr() < 1.0E-4 || db.lengthSqr() < 1.0E-4 || da.normalize().dot(db.normalize()) > MAX_DIRECTION_DOT) return false;
      double combinedWidth = Math.max(0.0, aHalfWidth) + Math.max(0.0, bHalfWidth);
      return segmentDistanceSqr(aStart, aEnd, bStart, bEnd) <= combinedWidth * combinedWidth;
   }

   private static void start(ServerLevel level, BeamClashParticipant a, BeamClashParticipant b) {
      link(a, b, 0.0F);
      link(b, a, 0.0F);
      a.setClashing(true);
      b.setClashing(true);
      Vec3 center = clashPoint(a, b, 0.0F);
      long now = level.getGameTime();
      LivingEntity owner = a.beamOwner(level);
      UUID source = owner == null ? a.clashEntity().getUUID() : owner.getUUID();
      CombatThreatService.publish(level, new CombatThreat(CLASH_THREAT, source, null, center, Vec3.ZERO,
         CombatThreat.Shape.SPHERE, 20.0, 0.0, 5, now, now + 5L, now + CONTEST_INTERVAL + 2L,
         false, true, false));
      VFXServerEffects.spawn(level, "beam_clash_contact", center, 192.0);
      VFXServerEffects.screenFlash(level, center, 96.0, 5, 0.45F);
      spawnSustainFx(level, center, 0.0F);
      level.playSound(null, BlockPos.containing(center), SoundEvents.RESPAWN_ANCHOR_CHARGE,
         SoundSource.HOSTILE, 2.4F, 0.72F);
   }

   private static void link(BeamClashParticipant beam, BeamClashParticipant partner, float balance) {
      beam.clashEntity().getPersistentData().putUUID(PARTNER_TAG, partner.clashEntity().getUUID());
      beam.clashEntity().getPersistentData().putFloat(BALANCE_TAG, balance);
      beam.clashEntity().getPersistentData().putInt(AGE_TAG, 0);
   }

   private static void publishMovingThreat(ServerLevel level, BeamClashParticipant beam, Vec3 center, int age) {
      LivingEntity owner = beam.beamOwner(level);
      UUID source = owner == null ? beam.clashEntity().getUUID() : owner.getUUID();
      long now = level.getGameTime();
      long remaining = Math.max(CONTEST_INTERVAL, MAX_CLASH_TICKS - age);
      CombatThreatService.publish(level, new CombatThreat(CLASH_THREAT, source, null, center, Vec3.ZERO,
         CombatThreat.Shape.SPHERE, 20.0, 0.0, 5, now, now + Math.min(remaining, CONTEST_INTERVAL),
         now + CONTEST_INTERVAL + 2L, false, true, false));
   }

   private static BeamClashParticipant resolvePartner(ServerLevel level, BeamClashParticipant beam) {
      if (!beam.clashEntity().getPersistentData().hasUUID(PARTNER_TAG)) return null;
      UUID id = beam.clashEntity().getPersistentData().getUUID(PARTNER_TAG);
      Entity entity = level.getEntity(id);
      if (entity instanceof BeamClashParticipant participant && entity.isAlive()
         && entity.getPersistentData().hasUUID(PARTNER_TAG)
         && beam.clashEntity().getUUID().equals(entity.getPersistentData().getUUID(PARTNER_TAG))) {
         return participant;
      }
      clear(beam);
      return null;
   }

   private static void finishWithWinner(ServerLevel level, BeamClashParticipant winner, BeamClashParticipant loser,
                                        Vec3 center, float winnerPower, float loserPower, float balance,
                                        LivingEntity winnerOwner) {
      float residual = residualScale(winnerPower, loserPower, balance);
      clear(winner);
      clear(loser);
      loser.cancelClashBeam();
      winner.setClashDamageScale(residual);
      winner.continueAfterClash();
      resolveBurst(level, winner, winnerOwner, center, winnerPower + loserPower, false, residual);
   }

   private static void finishDraw(ServerLevel level, BeamClashParticipant a, BeamClashParticipant b,
                                  Vec3 center, LivingEntity sourceOwner) {
      float combinedPower = effectiveStrength(a.clashPower(), sourceOwner == null ? 1.0F : manaFraction(sourceOwner))
         + b.clashPower();
      clear(a);
      clear(b);
      a.cancelClashBeam();
      b.cancelClashBeam();
      resolveBurst(level, a, sourceOwner, center, combinedPower, true, 0.0F);
   }

   private static void resolveBurst(ServerLevel level, BeamClashParticipant sourceBeam, LivingEntity sourceOwner,
                                    Vec3 center, float combinedPower, boolean draw, float residual) {
      double radius = clamp((draw ? 10.0 : 7.0) + combinedPower * (draw ? 3.0 : 2.0), 8.0, 18.0);
      float maximumDamage = (draw ? 100.0F : 70.0F) * Math.max(0.5F, combinedPower) * (draw ? 1.0F : 0.75F + residual * 0.25F);
      DamageSource source = level.damageSources().explosion(sourceBeam.clashEntity(), sourceOwner);
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
         entity -> entity.isAlive() && entity != sourceOwner && !EntityUtils.isImmunePlayerTarget(entity))) {
         double distance = Math.sqrt(target.position().distanceToSqr(center));
         if (distance > radius) continue;
         float falloff = (float)Math.max(0.3, 1.0 - distance / radius);
         hurtWithoutIFrames(target, source, maximumDamage * falloff);
         Vec3 push = target.position().subtract(center);
         if (push.lengthSqr() > 1.0E-4) {
            push = push.normalize().scale(draw ? 1.15 : 0.8);
            target.push(push.x, Math.max(0.25, push.y + 0.35), push.z);
            target.hurtMarked = true;
         }
      }

      long now = level.getGameTime();
      UUID sourceUuid = sourceOwner == null ? sourceBeam.clashEntity().getUUID() : sourceOwner.getUUID();
      CombatThreatService.publish(level, new CombatThreat(CLASH_THREAT, sourceUuid, null, center, Vec3.ZERO,
         CombatThreat.Shape.SPHERE, radius, 0.0, 5, now, now, now + 8L, false, true, false));
      TerrainImpactProfile profile = TerrainImpactProfile.of(draw ? TerrainImpactProfile.Tier.HEAVY : TerrainImpactProfile.Tier.MEDIUM);
      TerrainImpactService.impact(level, sourceOwner, center, profile, TerrainImpactService.Shape.AIR_SPHERE);
      VFXServerEffects.spawn(level, "beam_clash_impact", center, 224.0);
      VFXServerEffects.screenFlash(level, center, 128.0, draw ? 10 : 7, draw ? 0.85F : 0.65F);
      level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, draw ? 8 : 4, 1.2, 1.2, 1.2, 0.0);
      level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, draw ? 5 : 3, 0.4, 0.4, 0.4, 0.0);
      level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, draw ? 120 : 72, radius * 0.4, radius * 0.4, radius * 0.4, 0.2);
      level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y, center.z, draw ? 90 : 54, radius * 0.35, radius * 0.35, radius * 0.35, 0.16);
      level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, draw ? 5.5F : 4.0F, draw ? 0.48F : 0.62F);
   }

   private static void clear(BeamClashParticipant participant) {
      participant.clashEntity().getPersistentData().remove(PARTNER_TAG);
      participant.clashEntity().getPersistentData().remove(BALANCE_TAG);
      participant.clashEntity().getPersistentData().remove(AGE_TAG);
      participant.setClashing(false);
   }

   private static void setBalance(BeamClashParticipant a, BeamClashParticipant b, float valueForA) {
      a.clashEntity().getPersistentData().putFloat(BALANCE_TAG, valueForA);
      b.clashEntity().getPersistentData().putFloat(BALANCE_TAG, -valueForA);
   }

   private static void setAge(BeamClashParticipant a, BeamClashParticipant b, int age) {
      a.clashEntity().getPersistentData().putInt(AGE_TAG, age);
      b.clashEntity().getPersistentData().putInt(AGE_TAG, age);
   }

   private static float manaFraction(LivingEntity owner) {
      return (float)clamp(currentMp(owner) / Math.max(1.0, maxMp(owner)), 0.0, 1.0);
   }

   public static float effectiveStrength(float beamPower, float manaFraction) {
      return Math.max(0.05F, beamPower) * (0.25F + 0.75F * clamp(manaFraction, 0.0F, 1.0F));
   }

   public static float pressureDelta(float powerA, float powerB) {
      float total = Math.max(0.1F, powerA + powerB);
      return clamp((powerA - powerB) / total * 0.18F, -0.12F, 0.12F);
   }

   public static float residualScale(float winnerPower, float loserPower, float balance) {
      float share = winnerPower / Math.max(0.1F, winnerPower + loserPower);
      return clamp(0.18F + share * 0.72F + Math.abs(balance) * 0.2F, 0.25F, 0.9F);
   }

   private static double clashDrainFraction(float opposingPower, float ownPower) {
      double pressure = opposingPower / Math.max(0.1F, opposingPower + ownPower);
      return clamp(0.018 + pressure * 0.014, 0.018, 0.032);
   }

   private static double currentMp(LivingEntity owner) {
      if (owner instanceof ServantEntity servant) return servant.getCurrentMp();
      if (owner instanceof ServerPlayer player) return player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servant_card_mana;
      return 0.0;
   }

   private static double maxMp(LivingEntity owner) {
      if (owner instanceof ServantEntity servant) return servant.getMaxMp();
      if (owner instanceof ServerPlayer player) return Math.max(1.0, player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servant_card_max_mana);
      return 1.0;
   }

   private static void drain(LivingEntity owner, double amount) {
      if (owner instanceof ServantEntity servant) {
         servant.setCurrentMp(Math.max(0.0, servant.getCurrentMp() - amount));
      } else if (owner instanceof ServerPlayer player) {
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         vars.servant_card_mana = Math.max(0.0, vars.servant_card_mana - amount);
         vars.syncPlayerVariables(player);
      }
   }

   private static void spawnSustainFx(ServerLevel level, Vec3 center, float balance) {
      double spread = 1.8 + Math.abs(balance) * 1.2;
      if (level.getGameTime() % 6 == 0) level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 1, 0.1, 0.1, 0.1, 0.0);
      level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 22, spread, spread, spread, 0.18);
      level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 16, spread * 0.75, spread * 0.75, spread * 0.75, 0.12);
      level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y, center.z, 14, spread * 0.7, spread * 0.7, spread * 0.7, 0.1);
      if (level.getGameTime() % 10 == 0) {
         level.playSound(null, BlockPos.containing(center), SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 1.5F, 0.55F + Math.abs(balance) * 0.25F);
      }
   }

   private static Vec3 clashPoint(BeamClashParticipant a, BeamClashParticipant b, float balance) {
      Vec3 between = b.beamStart().subtract(a.beamStart());
      Vec3 midpoint = a.beamStart().lerp(b.beamStart(), 0.5);
      if (between.lengthSqr() < 1.0E-4) return midpoint;
      double maxShift = Math.min(32.0, between.length() * 0.28);
      return midpoint.add(between.normalize().scale(balance * maxShift));
   }

   private static void hurtWithoutIFrames(LivingEntity target, DamageSource source, float damage) {
      target.invulnerableTime = 0;
      target.hurtTime = 0;
      target.hurtDuration = 0;
      target.hurt(source, damage);
      target.invulnerableTime = 0;
      target.hurtTime = 0;
      target.hurtDuration = 0;
   }

   private static double segmentDistanceSqr(Vec3 p1, Vec3 q1, Vec3 p2, Vec3 q2) {
      Vec3 d1 = q1.subtract(p1);
      Vec3 d2 = q2.subtract(p2);
      Vec3 r = p1.subtract(p2);
      double a = d1.dot(d1);
      double e = d2.dot(d2);
      double f = d2.dot(r);
      double s;
      double t;
      if (a <= 1.0E-8 && e <= 1.0E-8) return p1.distanceToSqr(p2);
      if (a <= 1.0E-8) {
         s = 0.0;
         t = clamp(f / e, 0.0, 1.0);
      } else {
         double c = d1.dot(r);
         if (e <= 1.0E-8) {
            t = 0.0;
            s = clamp(-c / a, 0.0, 1.0);
         } else {
            double b = d1.dot(d2);
            double denominator = a * e - b * b;
            s = denominator == 0.0 ? 0.0 : clamp((b * f - c * e) / denominator, 0.0, 1.0);
            t = (b * s + f) / e;
            if (t < 0.0) {
               t = 0.0;
               s = clamp(-c / a, 0.0, 1.0);
            } else if (t > 1.0) {
               t = 1.0;
               s = clamp((b - c) / a, 0.0, 1.0);
            }
         }
      }
      return p1.add(d1.scale(s)).distanceToSqr(p2.add(d2.scale(t)));
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   private static double clamp(double value, double min, double max) {
      return Math.max(min, Math.min(max, value));
   }
}
