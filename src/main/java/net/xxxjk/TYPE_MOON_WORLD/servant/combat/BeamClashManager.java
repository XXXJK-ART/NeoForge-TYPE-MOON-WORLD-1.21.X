package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

public final class BeamClashManager {
   private static final String PARTNER_TAG = "TypeMoonBeamClashPartner";
   private static final String STARTED_TAG = "TypeMoonBeamClashStarted";
   private static final double MAX_DIRECTION_DOT = -Math.cos(Math.toRadians(12.0));

   private BeamClashManager() {
   }

   public static void tick(ServerLevel level, BeamClashParticipant beam) {
      Entity entity = beam.clashEntity();
      if (!entity.isAlive() || !beam.isBeamDamageActive()) {
         return;
      }
      BeamClashParticipant partner = resolvePartner(level, beam);
      if (partner == null) {
         if (!beam.isClashing()) {
            findAndStart(level, beam);
         }
         return;
      }
      if (entity.getId() > partner.clashEntity().getId()) {
         return;
      }
      LivingEntity ownerA = beam.beamOwner(level);
      LivingEntity ownerB = partner.beamOwner(level);
      if (ownerA == null || ownerB == null || !ownerA.isAlive() || !ownerB.isAlive()) {
         endDraw(beam, partner);
         return;
      }
      Vec3 midpoint = clashPoint(beam, partner);
      if (entity.tickCount % 5 == 0) {
         drain(ownerA, maxMp(ownerA) * 0.04);
         drain(ownerB, maxMp(ownerB) * 0.04);
         spawnClashFx(level, midpoint);
         boolean emptyA = currentMp(ownerA) <= 0.001;
         boolean emptyB = currentMp(ownerB) <= 0.001;
         if (emptyA && emptyB) {
            endDraw(beam, partner);
         } else if (emptyA) {
            endWithWinner(partner, beam);
         } else if (emptyB) {
            endWithWinner(beam, partner);
         }
      }
   }

   private static void findAndStart(ServerLevel level, BeamClashParticipant beam) {
      for (Entity candidate : level.getEntities().getAll()) {
         if (!(candidate instanceof BeamClashParticipant other)
            || candidate == beam.clashEntity()
            || !candidate.isAlive()
            || !other.isBeamDamageActive()
            || other.isClashing()
            || !canClash(beam, other)) {
            continue;
         }
         start(beam, other);
         Vec3 midpoint = clashPoint(beam, other);
         VFXServerEffects.spawn(level, "beam_clash", midpoint, 192.0);
         level.playSound(null, BlockPos.containing(midpoint), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.HOSTILE, 2.0F, 0.75F);
         return;
      }
   }

   private static boolean canClash(BeamClashParticipant a, BeamClashParticipant b) {
      Vec3 da = a.beamEnd().subtract(a.beamStart());
      Vec3 db = b.beamEnd().subtract(b.beamStart());
      if (da.lengthSqr() < 1.0E-4 || db.lengthSqr() < 1.0E-4 || da.normalize().dot(db.normalize()) > MAX_DIRECTION_DOT) {
         return false;
      }
      return segmentDistanceSqr(a.beamStart(), a.beamEnd(), b.beamStart(), b.beamEnd())
         <= Math.pow(a.beamHalfWidth() + b.beamHalfWidth(), 2.0);
   }

   private static void start(BeamClashParticipant a, BeamClashParticipant b) {
      a.clashEntity().getPersistentData().putUUID(PARTNER_TAG, b.clashEntity().getUUID());
      b.clashEntity().getPersistentData().putUUID(PARTNER_TAG, a.clashEntity().getUUID());
      a.clashEntity().getPersistentData().putBoolean(STARTED_TAG, true);
      b.clashEntity().getPersistentData().putBoolean(STARTED_TAG, true);
      a.setClashing(true);
      b.setClashing(true);
   }

   private static BeamClashParticipant resolvePartner(ServerLevel level, BeamClashParticipant beam) {
      if (!beam.clashEntity().getPersistentData().hasUUID(PARTNER_TAG)) {
         return null;
      }
      UUID id = beam.clashEntity().getPersistentData().getUUID(PARTNER_TAG);
      Entity partner = level.getEntity(id);
      if (partner instanceof BeamClashParticipant participant && partner.isAlive()) {
         return participant;
      }
      clear(beam);
      return null;
   }

   private static void endWithWinner(BeamClashParticipant winner, BeamClashParticipant loser) {
      clear(winner);
      clear(loser);
      loser.cancelClashBeam();
      winner.setClashDamageScale(0.1F);
      winner.continueAfterClash();
   }

   private static void endDraw(BeamClashParticipant a, BeamClashParticipant b) {
      clear(a);
      clear(b);
      a.cancelClashBeam();
      b.cancelClashBeam();
   }

   private static void clear(BeamClashParticipant participant) {
      participant.clashEntity().getPersistentData().remove(PARTNER_TAG);
      participant.clashEntity().getPersistentData().remove(STARTED_TAG);
      participant.setClashing(false);
   }

   private static double currentMp(LivingEntity owner) {
      if (owner instanceof ServantEntity servant) {
         return servant.getCurrentMp();
      }
      if (owner instanceof ServerPlayer player) {
         return player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servant_card_mana;
      }
      return 0.0;
   }

   private static double maxMp(LivingEntity owner) {
      if (owner instanceof ServantEntity servant) {
         return servant.getMaxMp();
      }
      if (owner instanceof ServerPlayer player) {
         return Math.max(1.0, player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servant_card_max_mana);
      }
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

   private static void spawnClashFx(ServerLevel level, Vec3 center) {
      level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 1, 0.08, 0.08, 0.08, 0.0);
      level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 36, 2.2, 2.2, 2.2, 0.18);
      level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y, center.z, 28, 1.8, 1.8, 1.8, 0.14);
   }

   private static Vec3 clashPoint(BeamClashParticipant a, BeamClashParticipant b) {
      return a.beamStart().lerp(a.beamEnd(), 0.5).lerp(b.beamStart().lerp(b.beamEnd(), 0.5), 0.5);
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
         t = clamp(f / e);
      } else {
         double c = d1.dot(r);
         if (e <= 1.0E-8) {
            t = 0.0;
            s = clamp(-c / a);
         } else {
            double b = d1.dot(d2);
            double denom = a * e - b * b;
            s = denom == 0.0 ? 0.0 : clamp((b * f - c * e) / denom);
            t = (b * s + f) / e;
            if (t < 0.0) {
               t = 0.0;
               s = clamp(-c / a);
            } else if (t > 1.0) {
               t = 1.0;
               s = clamp((b - c) / a);
            }
         }
      }
      return p1.add(d1.scale(s)).distanceToSqr(p2.add(d2.scale(t)));
   }

   private static double clamp(double value) {
      return Math.max(0.0, Math.min(1.0, value));
   }
}
