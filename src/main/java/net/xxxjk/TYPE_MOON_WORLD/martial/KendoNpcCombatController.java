package net.xxxjk.TYPE_MOON_WORLD.martial;

import java.util.EnumSet;
import java.util.function.DoubleSupplier;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.RoninEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.magic.npc.NpcCombatTemperament;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

/** Distance-aware sword fighting for all kendo NPCs. */
public final class KendoNpcCombatController {
   public static final int POSE_SLASH = 1;
   public static final int POSE_DRAW = 2;
   public static final int POSE_THRUST = 3;
   public static final int POSE_GUARD = 4;
   public static final int POSE_RETREAT = 5;
   private static final String NEXT_SKILL = "TypeMoonKendoNpcNextSkill";
   private static final String NEXT_PATH = "TypeMoonKendoNpcNextPath";
   private static final String NEXT_DASH = "TypeMoonKendoNpcNextDash";
   private static final String STRAFE_SIDE = "TypeMoonKendoNpcStrafeSide";
   private static final String NEXT_SWITCH = "TypeMoonKendoNpcNextStrafeSwitch";

   private KendoNpcCombatController() {}

   public static Goal combatGoal(PathfinderMob npc, KendoSchool school, DoubleSupplier proficiency) {
      return new Goal() {
         { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
         @Override public boolean canUse() { return valid(npc, npc.getTarget()); }
         @Override public boolean canContinueToUse() { return valid(npc, npc.getTarget()); }
         @Override public void tick() {
            LivingEntity target = npc.getTarget();
            if (target != null) tickCombat(npc, target, school, proficiency == null ? 0.0 : proficiency.getAsDouble());
         }
         @Override public void stop() { npc.getNavigation().stop(); }
      };
   }

   private static void tickCombat(PathfinderMob npc, LivingEntity target, KendoSchool school, double proficiency) {
      if (npc.level().isClientSide() || npc.hasEffect(ModMobEffects.STAGGER) || npc.hasEffect(ModMobEffects.OFF_BALANCE)) return;
      long now = npc.level().getGameTime();
      double distance = Math.sqrt(npc.distanceToSqr(target));
      double reach = 2.75 + (npc.getBbWidth() + target.getBbWidth()) * 0.42;
      boolean visible = npc.hasLineOfSight(target);
      boolean timid = npc instanceof RoninEntity ronin && ronin.getCombatTemperament() == NpcCombatTemperament.TIMID;
      npc.getLookControl().setLookAt(target, 75.0F, 60.0F);

      if (distance > reach || !visible) {
         approach(npc, target, proficiency, now);
         if (visible && distance <= 8.0 && now >= npc.getPersistentData().getLong(NEXT_DASH)
            && npc.getRandom().nextInt(100) < (proficiency >= 70.0 ? 58 : 34)) {
            Vec3 direction = horizontalDirection(npc, target);
            int side = strafeSide(npc, now);
            if (npc.getRandom().nextBoolean()) direction = direction.add(-direction.z * side * 0.25, 0.0, direction.x * side * 0.25).normalize();
            npc.getNavigation().stop();
            double dash = proficiency >= 80.0 ? 0.95 : proficiency >= 40.0 ? 0.78 : 0.62;
            npc.setDeltaMovement(direction.x * dash, Math.max(0.05, npc.getDeltaMovement().y), direction.z * dash);
            npc.hurtMarked = true;
            npc.getPersistentData().putLong(NEXT_DASH, now + (proficiency >= 80.0 ? 12L : 18L));
            pose(npc, POSE_THRUST, 8);
         }
         return;
      }

      if (now < npc.getPersistentData().getLong(NEXT_SKILL)) {
         circleTarget(npc, target, distance, reach, now, timid);
         return;
      }
      npc.getNavigation().stop();
      Move move = chooseMove(npc, school, proficiency, timid, distance);
      perform(npc, target, move);
      int recovery = switch (move) {
         case DRAW -> 13;
         case THRUST -> 11;
         case GUARD -> 9;
         case RETREAT -> 15;
         default -> 10;
      };
      if (proficiency >= 80.0) recovery = Math.max(7, recovery - 2);
      npc.getPersistentData().putLong(NEXT_SKILL, now + recovery);
   }

   private static Move chooseMove(PathfinderMob npc, KendoSchool school, double proficiency, boolean timid, double distance) {
      int roll = npc.getRandom().nextInt(100);
      if (distance < 1.65 && (timid && roll < 34 || !timid && roll < 10)) return Move.RETREAT;
      if (proficiency < 20.0) return roll < 65 ? Move.SLASH : Move.THRUST;
      if (school == KendoSchool.TENNEN) {
         if (roll < 27) return Move.DRAW;
         if (roll < 58) return Move.THRUST;
         if (roll < 78) return Move.SLASH;
         return timid ? Move.RETREAT : Move.GUARD;
      }
      if (roll < 28) return Move.DRAW;
      if (roll < 55) return Move.THRUST;
      if (roll < 82) return Move.SLASH;
      return timid ? Move.RETREAT : Move.GUARD;
   }

   private static void perform(PathfinderMob npc, LivingEntity target, Move move) {
      Vec3 direction = horizontalDirection(npc, target);
      if (move == Move.RETREAT) {
         pose(npc, POSE_RETREAT, 10);
         npc.setDeltaMovement(-direction.x * 0.55, Math.max(0.04, npc.getDeltaMovement().y), -direction.z * 0.55);
         npc.hurtMarked = true;
         return;
      }
      int pose = switch (move) {
         case DRAW -> POSE_DRAW;
         case THRUST -> POSE_THRUST;
         case GUARD -> POSE_GUARD;
         default -> POSE_SLASH;
      };
      pose(npc, pose, move == Move.GUARD ? 8 : 10);
      double lunge = move == Move.THRUST ? 0.42 : move == Move.DRAW ? 0.28 : 0.12;
      npc.setDeltaMovement(direction.x * lunge, Math.max(0.04, npc.getDeltaMovement().y), direction.z * lunge);
      npc.hurtMarked = true;
      float base = (float)Math.max(2.0, npc.getAttributeValue(Attributes.ATTACK_DAMAGE));
      float damage = base * switch (move) { case DRAW -> 1.25F; case THRUST -> 1.12F; case GUARD -> 0.95F; default -> 1.0F; };
      target.invulnerableTime = 0;
      boolean hit = target.hurt(npc.damageSources().mobAttack(npc), damage);
      target.invulnerableTime = 0;
      if (hit) {
         target.addEffect(new MobEffectInstance(ModMobEffects.STAGGER, move == Move.GUARD ? 5 : 8, 0, false, true, true));
         if (move == Move.DRAW) target.addEffect(new MobEffectInstance(ModMobEffects.OFF_BALANCE, 18, 0, false, true, true));
         if (move == Move.THRUST) target.push(direction.x * 0.16, 0.08, direction.z * 0.16);
      }
      if (npc.level() instanceof ServerLevel level) {
         level.sendParticles(hit ? ParticleTypes.SWEEP_ATTACK : ParticleTypes.SMOKE,
            target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), hit ? 2 : 5, 0.3, 0.25, 0.3, 0.03);
         level.playSound(null, target.blockPosition(), hit ? SoundEvents.PLAYER_ATTACK_SWEEP : SoundEvents.PLAYER_ATTACK_WEAK,
            SoundSource.HOSTILE, 0.7F, move == Move.THRUST ? 1.1F : 0.9F);
      }
   }

   private static void pose(PathfinderMob npc, int pose, int ticks) {
      npc.swing(InteractionHand.MAIN_HAND, true);
      if (npc instanceof NpcActionPose action) action.triggerNpcActionPose(pose, ticks);
   }

   private static void approach(PathfinderMob npc, LivingEntity target, double proficiency, long now) {
      if (!npc.getNavigation().isDone() && now < npc.getPersistentData().getLong(NEXT_PATH)) return;
      double speed = proficiency >= 80.0 ? 1.5 : proficiency >= 40.0 ? 1.36 : 1.23;
      npc.getNavigation().moveTo(target, speed);
      npc.getPersistentData().putLong(NEXT_PATH, now + 4L);
   }

   private static void circleTarget(PathfinderMob npc, LivingEntity target, double distance, double reach, long now, boolean timid) {
      int side = strafeSide(npc, now);
      float forward = distance > reach - 0.2 ? 0.52F : distance < 1.55 ? -0.35F : 0.14F;
      if (timid) forward -= 0.12F;
      npc.getMoveControl().strafe(forward, side * 0.7F);
      npc.getLookControl().setLookAt(target, 75.0F, 60.0F);
      if (distance < 1.45 && now % 14L == 0L) pose(npc, POSE_GUARD, 6);
   }

   private static int strafeSide(PathfinderMob npc, long now) {
      if (now >= npc.getPersistentData().getLong(NEXT_SWITCH)) {
         npc.getPersistentData().putInt(STRAFE_SIDE, npc.getRandom().nextBoolean() ? 1 : -1);
         npc.getPersistentData().putLong(NEXT_SWITCH, now + 10L + npc.getRandom().nextInt(12));
      }
      int side = npc.getPersistentData().getInt(STRAFE_SIDE);
      return side == 0 ? 1 : side;
   }

   private static Vec3 horizontalDirection(PathfinderMob npc, LivingEntity target) {
      Vec3 direction = target.position().subtract(npc.position()).multiply(1.0, 0.0, 1.0);
      return direction.lengthSqr() < 1.0E-5 ? new Vec3(0.0, 0.0, 1.0) : direction.normalize();
   }

   private static boolean valid(PathfinderMob npc, LivingEntity target) {
      if (target == null || !target.isAlive() || target == npc || npc.isAlliedTo(target) || EntityUtils.isImmunePlayerTarget(target)) return false;
      return !(target instanceof Player player) || !player.isCreative() && !player.isSpectator();
   }

   private enum Move { SLASH, DRAW, THRUST, GUARD, RETREAT }
}
