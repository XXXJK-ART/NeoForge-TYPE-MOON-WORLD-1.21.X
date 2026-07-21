package net.xxxjk.TYPE_MOON_WORLD.martial;

import java.util.EnumSet;
import java.util.function.DoubleSupplier;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysteriousSwordsmanEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public final class GanryuNpcCombatController {
   private static final String TAG_NEXT_SKILL = "TypeMoonGanryuNpcNextSkill";
   private static final String TAG_NEXT_PATH = "TypeMoonGanryuNpcNextPath";
   private static final String TAG_NEXT_DASH = "TypeMoonGanryuNpcNextDash";
   private static final String TAG_STRAFE_SIDE = "TypeMoonGanryuNpcStrafeSide";
   private static final String TAG_NEXT_STRAFE_SWITCH = "TypeMoonGanryuNpcNextStrafeSwitch";
   private static final String TAG_STANCE_MOVE = "TypeMoonGanryuNpcStanceMove";
   private static final String TAG_STANCE_READY = "TypeMoonGanryuNpcStanceReady";
   private static final String TAG_STANCE_EXPIRE = "TypeMoonGanryuNpcStanceExpire";
   private static final String TAG_PENDING_STRIKES = "TypeMoonGanryuNpcPendingStrikes";
   private static final String TAG_PENDING_NEXT = "TypeMoonGanryuNpcPendingNext";
   private static final String TAG_PENDING_EXPIRE = "TypeMoonGanryuNpcPendingExpire";
   private static final String TAG_PENDING_DAMAGE = "TypeMoonGanryuNpcPendingDamage";

   private GanryuNpcCombatController() {}

   public static Goal combatGoal(PathfinderMob npc, DoubleSupplier proficiency) {
      return new Goal() {
         {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
         }

         @Override public boolean canUse() { return validTarget(npc, npc.getTarget()); }
         @Override public boolean canContinueToUse() { return validTarget(npc, npc.getTarget()); }
         @Override public void tick() { GanryuNpcCombatController.tick(npc, proficiency == null ? 0.0 : proficiency.getAsDouble()); }
          @Override public void stop() {
             npc.getNavigation().stop();
             clearQueuedActions(npc);
          }
      };
   }

   private static void tick(PathfinderMob npc, double proficiency) {
      LivingEntity target = npc.getTarget();
      if (!validTarget(npc, target)) {
         if (target != null) npc.setTarget(null);
         return;
      }
      if (npc.hasEffect(ModMobEffects.STAGGER) || npc.hasEffect(ModMobEffects.OFF_BALANCE)) return;
      long now = npc.level().getGameTime();
      double distance = Math.sqrt(npc.distanceToSqr(target));
      double verticalGap = Math.abs(target.getY() - npc.getY());
      double attackReach = 2.9 + (npc.getBbWidth() + target.getBbWidth()) * 0.42;
      boolean lineOfSight = npc.hasLineOfSight(target);
      npc.getLookControl().setLookAt(target, 70.0F, 55.0F);
      if (tickPendingStrikes(npc, target, proficiency, now, distance, attackReach, lineOfSight)) return;
      if (tickStance(npc, target, proficiency, now, distance, attackReach, lineOfSight)) return;

      if (distance > attackReach - 0.25) {
         approach(npc, target, proficiency, now);
         if (lineOfSight && distance <= 10.0) tryGapClose(npc, target, proficiency, now, distance, attackReach);
      }

      boolean skillReady = now >= npc.getPersistentData().getLong(TAG_NEXT_SKILL);
      if (!skillReady || distance > attackReach || verticalGap > 2.5 || (!lineOfSight && distance > 2.2)) {
         if (distance <= attackReach + 1.0 && verticalGap <= 2.5) circleTarget(npc, target, distance, attackReach, now);
         return;
      }
      if (proficiency < 0.5) {
         npc.getPersistentData().putLong(TAG_NEXT_SKILL, now + 10L);
         return;
      }
      int roll = npc.getRandom().nextInt(100);
      GanryuMove move;
      if (proficiency >= 80.0 && roll < 5) move = GanryuMove.TSUBAME_GAESHI;
      else if (proficiency >= 60.0 && roll < 12) move = GanryuMove.FLOWER_BUD;
      else if (proficiency >= 55.0 && roll < 22) move = GanryuMove.SPARROW_SLASH;
      else if (proficiency >= 25.0 && roll < 62) move = npc.getRandom().nextBoolean() ? GanryuMove.SPRING_BUD : GanryuMove.SPRING_BUD_SECOND;
      else if (proficiency >= 5.0 && roll < 86) move = npc.getRandom().nextBoolean() ? GanryuMove.SPARROW_THRUST : GanryuMove.SPARROW_THRUST_SECOND;
      else move = GanryuMove.STONE_FLOWER;
      if (isStanceMove(move)) {
         npc.getPersistentData().putString(TAG_STANCE_MOVE, move.name());
         npc.getPersistentData().putLong(TAG_STANCE_READY, now + 60L);
         npc.getPersistentData().putLong(TAG_STANCE_EXPIRE, now + 100L);
         npc.getNavigation().stop();
         if (npc instanceof MysteriousSwordsmanEntity swordsman) swordsman.triggerMove(GanryuMove.STANCE);
      } else {
         perform(npc, target, move);
         int minimumRecovery = proficiency >= 80.0 ? 6 : 8;
         int recovery = Math.max(minimumRecovery,
            move.recoveryTicks() + 1 - (proficiency >= 80.0 ? 5 : proficiency >= 40.0 ? 2 : 0));
         npc.getPersistentData().putLong(TAG_NEXT_SKILL, now + recovery);
      }
   }

   private static boolean tickStance(PathfinderMob npc, LivingEntity target, double proficiency, long now,
      double distance, double attackReach, boolean lineOfSight) {
      String queued = npc.getPersistentData().getString(TAG_STANCE_MOVE);
      if (queued.isEmpty()) return false;
      if (now < npc.getPersistentData().getLong(TAG_STANCE_READY)) {
         if (distance > attackReach - 0.2 && (npc.getNavigation().isDone() || now % 6L == 0L)) {
            npc.getNavigation().moveTo(target, proficiency >= 80.0 ? 0.62 : 0.52);
         } else if (distance <= attackReach - 0.4) {
            circleTarget(npc, target, distance, attackReach, now);
         }
         return true;
      }
      if (distance > attackReach || !lineOfSight) {
         if (now > npc.getPersistentData().getLong(TAG_STANCE_EXPIRE) || distance > 12.0) {
            clearStance(npc);
            npc.getPersistentData().putLong(TAG_NEXT_SKILL, now + 6L);
            return false;
         }
         approach(npc, target, proficiency, now);
         if (lineOfSight && distance <= 10.0) tryGapClose(npc, target, proficiency, now, distance, attackReach);
         return true;
      }
      npc.getPersistentData().remove(TAG_STANCE_MOVE);
      npc.getPersistentData().remove(TAG_STANCE_READY);
      npc.getPersistentData().remove(TAG_STANCE_EXPIRE);
      GanryuMove move;
      try {
         move = GanryuMove.valueOf(queued);
      } catch (IllegalArgumentException ignored) {
         return false;
      }
      perform(npc, target, move);
      int recovery = move == GanryuMove.TSUBAME_GAESHI ? 32 : proficiency >= 80.0 ? 12 : 15;
      npc.getPersistentData().putLong(TAG_NEXT_SKILL, now + recovery);
      return true;
   }

   private static boolean tickPendingStrikes(PathfinderMob npc, LivingEntity target, double proficiency, long now,
      double distance, double attackReach, boolean lineOfSight) {
      int remaining = npc.getPersistentData().getInt(TAG_PENDING_STRIKES);
      if (remaining <= 0) return false;
      if (now > npc.getPersistentData().getLong(TAG_PENDING_EXPIRE) || distance > 12.0) {
         clearPendingStrikes(npc);
         npc.getPersistentData().putLong(TAG_NEXT_SKILL, now + 6L);
         return false;
      }
      if (now < npc.getPersistentData().getLong(TAG_PENDING_NEXT)) return true;
      if (distance > attackReach + 0.8 || !lineOfSight) {
         approach(npc, target, proficiency, now);
         if (lineOfSight && distance <= 8.0) tryGapClose(npc, target, proficiency, now, distance, attackReach);
         return true;
      }
      if (npc instanceof MysteriousSwordsmanEntity swordsman) swordsman.triggerMove(GanryuMove.SPARROW_THRUST);
      hurtTarget(npc, target, npc.getPersistentData().getFloat(TAG_PENDING_DAMAGE), false);
      remaining--;
      npc.getPersistentData().putInt(TAG_PENDING_STRIKES, remaining);
      npc.getPersistentData().putLong(TAG_PENDING_NEXT, now + 3L);
      npc.setDeltaMovement(npc.getDeltaMovement().add(horizontalDirection(npc, target).scale(0.15)));
      npc.hurtMarked = true;
      if (remaining <= 0) {
         clearPendingStrikes(npc);
      }
      return remaining > 0;
   }

   private static boolean isStanceMove(GanryuMove move) {
      return move == GanryuMove.SPARROW_SLASH || move == GanryuMove.FLOWER_BUD || move == GanryuMove.TSUBAME_GAESHI;
   }

   private static void clearQueuedActions(PathfinderMob npc) {
      clearStance(npc);
      clearPendingStrikes(npc);
   }

   private static void clearPendingStrikes(PathfinderMob npc) {
      npc.getPersistentData().remove(TAG_PENDING_STRIKES);
      npc.getPersistentData().remove(TAG_PENDING_NEXT);
      npc.getPersistentData().remove(TAG_PENDING_EXPIRE);
      npc.getPersistentData().remove(TAG_PENDING_DAMAGE);
   }

   private static void clearStance(PathfinderMob npc) {
      npc.getPersistentData().remove(TAG_STANCE_MOVE);
      npc.getPersistentData().remove(TAG_STANCE_READY);
      npc.getPersistentData().remove(TAG_STANCE_EXPIRE);
   }

   private static void approach(PathfinderMob npc, LivingEntity target, double proficiency, long now) {
      if (!npc.getNavigation().isDone() && now < npc.getPersistentData().getLong(TAG_NEXT_PATH)) return;
      double speed = proficiency >= 80.0 ? 1.52 : proficiency >= 25.0 ? 1.40 : 1.30;
      boolean started = npc.getNavigation().moveTo(target, speed);
      npc.getPersistentData().putLong(TAG_NEXT_PATH, now + (started ? 4L : 2L));
   }

   private static void tryGapClose(PathfinderMob npc, LivingEntity target, double proficiency, long now,
      double distance, double attackReach) {
      if (!npc.onGround() || distance <= attackReach - 0.1 || now < npc.getPersistentData().getLong(TAG_NEXT_DASH)) return;
      Vec3 direction = horizontalDirection(npc, target);
      double speed = proficiency >= 80.0 ? 1.05 : proficiency >= 25.0 ? 0.88 : 0.72;
      if (proficiency >= 55.0 && distance <= 6.5
         && npc.getRandom().nextInt(100) < (proficiency >= 80.0 ? 55 : 35)) {
         int side = npc.getPersistentData().getInt(TAG_STRAFE_SIDE);
         if (side == 0) side = npc.getRandom().nextBoolean() ? 1 : -1;
         double lateral = proficiency >= 80.0 ? 0.34 : 0.24;
         direction = direction.add(-direction.z * side * lateral, 0.0, direction.x * side * lateral).normalize();
         npc.getPersistentData().putInt(TAG_STRAFE_SIDE, -side);
      }
      npc.getNavigation().stop();
      npc.setDeltaMovement(direction.x * speed, Math.max(0.05, npc.getDeltaMovement().y), direction.z * speed);
      npc.hurtMarked = true;
      npc.getPersistentData().putLong(TAG_NEXT_DASH, now + (proficiency >= 80.0 ? 11L : 15L));
   }

   private static void circleTarget(PathfinderMob npc, LivingEntity target, double distance, double attackReach, long now) {
      if (now >= npc.getPersistentData().getLong(TAG_NEXT_STRAFE_SWITCH)) {
         npc.getPersistentData().putInt(TAG_STRAFE_SIDE, npc.getRandom().nextBoolean() ? 1 : -1);
         npc.getPersistentData().putLong(TAG_NEXT_STRAFE_SWITCH, now + 10L + npc.getRandom().nextInt(10));
      }
      int side = npc.getPersistentData().getInt(TAG_STRAFE_SIDE);
      if (side == 0) side = 1;
      float forward = distance > attackReach - 0.25 ? 0.58F : distance < 1.55 ? -0.32F : 0.16F;
      npc.getMoveControl().strafe(forward, side * 0.68F);
      npc.getLookControl().setLookAt(target, 75.0F, 60.0F);
   }

   private static void perform(PathfinderMob npc, LivingEntity target, GanryuMove move) {
      if (npc instanceof MysteriousSwordsmanEntity swordsman) swordsman.triggerMove(move);
      npc.getNavigation().stop();
      Vec3 direction = horizontalDirection(npc, target);
      if (move == GanryuMove.SPRING_BUD || move == GanryuMove.SPRING_BUD_SECOND || move == GanryuMove.SPARROW_SLASH) {
         npc.setDeltaMovement(npc.getDeltaMovement().add(direction.scale(move == GanryuMove.SPRING_BUD_SECOND ? 0.65 : 0.45)).add(0.0, 0.04, 0.0));
         npc.hurtMarked = true;
      }
      if (move == GanryuMove.SPARROW_THRUST_SECOND) {
         npc.setDeltaMovement(npc.getDeltaMovement().add(direction.scale(-0.38)).add(0.0, 0.03, 0.0));
         npc.hurtMarked = true;
      }
      if (move == GanryuMove.TSUBAME_GAESHI && npc.getRandom().nextFloat() >= 0.8F) {
         fx(npc, target, true);
         return;
      }
      float damage = switch (move) {
         case TSUBAME_GAESHI -> 120.0F;
         case FLOWER_BUD -> 20.0F;
         case SPARROW_SLASH -> 8.0F;
         case SPRING_BUD, SPRING_BUD_SECOND -> 9.0F;
         case SPARROW_THRUST, SPARROW_THRUST_SECOND -> 7.0F;
         default -> 6.0F;
      };
      if (move == GanryuMove.SPARROW_SLASH) {
         npc.getPersistentData().putInt(TAG_PENDING_STRIKES, 3);
         npc.getPersistentData().putLong(TAG_PENDING_NEXT, npc.level().getGameTime());
         npc.getPersistentData().putLong(TAG_PENDING_EXPIRE, npc.level().getGameTime() + 35L);
         npc.getPersistentData().putFloat(TAG_PENDING_DAMAGE, damage);
      } else {
         if (move == GanryuMove.TSUBAME_GAESHI) target.stopUsingItem();
         hurtTarget(npc, target, damage, move == GanryuMove.TSUBAME_GAESHI);
      }
      if (move == GanryuMove.FLOWER_BUD || move == GanryuMove.SPARROW_THRUST || move == GanryuMove.SPARROW_THRUST_SECOND) {
         target.push(direction.x * 0.15, 0.35, direction.z * 0.15);
         target.hurtMarked = true;
      }
      fx(npc, target, false);
   }

   private static void hurtTarget(PathfinderMob npc, LivingEntity target, float damage, boolean tsubame) {
      target.invulnerableTime = 0;
      target.hurt(tsubame
         ? npc.damageSources().source(GanryuCombatService.TSUBAME_DAMAGE, npc)
         : npc.damageSources().mobAttack(npc), damage);
      target.invulnerableTime = 0;
   }

   private static void fx(PathfinderMob npc, LivingEntity target, boolean missed) {
      if (!(npc.level() instanceof ServerLevel level)) return;
      level.sendParticles(missed ? ParticleTypes.SMOKE : ParticleTypes.SWEEP_ATTACK,
         target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), missed ? 14 : 2, 0.35, 0.4, 0.35, 0.04);
      level.playSound(null, target.blockPosition(), missed ? SoundEvents.PLAYER_ATTACK_WEAK : SoundEvents.PLAYER_ATTACK_SWEEP,
         SoundSource.HOSTILE, 0.8F, missed ? 1.2F : 0.85F);
   }

   private static boolean validTarget(PathfinderMob npc, LivingEntity target) {
      if (target == null || !target.isAlive() || target == npc || npc.isAlliedTo(target) || EntityUtils.isImmunePlayerTarget(target)) return false;
      return !(target instanceof Player player) || !player.isCreative() && !player.isSpectator();
   }

   private static Vec3 horizontalDirection(PathfinderMob npc, LivingEntity target) {
      Vec3 direction = target.position().subtract(npc.position()).multiply(1.0, 0.0, 1.0);
      return direction.lengthSqr() < 1.0E-5 ? new Vec3(0.0, 0.0, 1.0) : direction.normalize();
   }
}
