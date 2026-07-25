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
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

/** Distance-aware martial decisions shared by the master, apprentices, and Rin's close defense. */
public final class BajiquanNpcCombatController {
   public static final int POSE_PUNCH = 1;
   public static final int POSE_KICK = 2;
   public static final int POSE_ELBOW = 3;
   public static final int POSE_SHOULDER = 4;
   public static final int POSE_TREMOR = 5;
   public static final int POSE_PUSH = 6;
   private static final String TAG_NEXT_SKILL = "TypeMoonBajiquanNpcNextSkill";
   private static final String TAG_NEXT_DASH = "TypeMoonBajiquanNpcNextDash";
   private static final String TAG_NEXT_PATH = "TypeMoonBajiquanNpcNextPath";
   private static final String TAG_STRAFE_SIDE = "TypeMoonBajiquanNpcStrafeSide";
   private static final String TAG_NEXT_STRAFE_SWITCH = "TypeMoonBajiquanNpcNextStrafeSwitch";

   private BajiquanNpcCombatController() {}

   public static Goal combatGoal(PathfinderMob npc, DoubleSupplier proficiency, boolean closeDefenseOnly) {
      return new Goal() {
         {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
         }

         @Override public boolean canUse() { return validTarget(npc, npc.getTarget()); }
         @Override public boolean canContinueToUse() { return validTarget(npc, npc.getTarget()); }
         @Override public void tick() {
            LivingEntity target = npc.getTarget();
            if (target != null) {
               npc.getLookControl().setLookAt(target, 60.0F, 50.0F);
               BajiquanNpcCombatController.tick(npc, proficiency == null ? 0.0 : proficiency.getAsDouble(), closeDefenseOnly);
            }
         }
         @Override public void stop() { npc.getNavigation().stop(); }
      };
   }

   public static void tick(PathfinderMob npc, double proficiency, boolean closeDefenseOnly) {
      if (npc == null || npc.level().isClientSide() || !npc.isAlive()) return;
      LivingEntity target = npc.getTarget();
      if (!validTarget(npc, target)) {
         if (target != null) npc.setTarget(null);
         return;
      }
      if (npc.hasEffect(ModMobEffects.STAGGER) || npc.hasEffect(ModMobEffects.OFF_BALANCE)) return;

      long now = npc.level().getGameTime();
      double distance = Math.sqrt(npc.distanceToSqr(target));
      double verticalGap = Math.abs(target.getY() - npc.getY());
      double attackReach = 2.65 + (npc.getBbWidth() + target.getBbWidth()) * 0.45;
      boolean lineOfSight = npc.hasLineOfSight(target);
      boolean skillReady = now >= npc.getPersistentData().getLong(TAG_NEXT_SKILL);
      npc.getLookControl().setLookAt(target, 65.0F, 55.0F);

      if (closeDefenseOnly && distance > 5.4) return;
      if (!closeDefenseOnly && distance > attackReach - 0.2
         && (npc.getNavigation().isDone() || now >= npc.getPersistentData().getLong(TAG_NEXT_PATH))) {
         npc.getNavigation().moveTo(target, proficiency >= 80.0 ? 1.38 : proficiency >= 25.0 ? 1.24 : 1.16);
         npc.getPersistentData().putLong(TAG_NEXT_PATH, now + (npc.getNavigation().isDone() ? 3L : 6L));
      }

      if (skillReady && distance > attackReach && distance <= 7.0 && proficiency >= 10.0
         && now >= npc.getPersistentData().getLong(TAG_NEXT_DASH) && npc.hasLineOfSight(target)) {
         Vec3 direction = horizontalDirection(npc, target);
         npc.getNavigation().stop();
         npc.setDeltaMovement(npc.getDeltaMovement().add(direction.scale(proficiency >= 80.0 ? 0.82 : proficiency >= 40.0 ? 0.68 : 0.52)).add(0.0, 0.05, 0.0));
         npc.hurtMarked = true;
         npc.getPersistentData().putLong(TAG_NEXT_DASH, now + (proficiency >= 80.0 ? 16L : 22L));
      }

      if (!skillReady || distance > attackReach || verticalGap > 2.5 || (!lineOfSight && distance > 2.25)) {
         if (distance <= attackReach + 0.9 && verticalGap <= 2.5) circleTarget(npc, target, distance, attackReach, now);
         return;
      }
      npc.getNavigation().stop();
      int roll = npc.getRandom().nextInt(100);
      MoveType move = proficiency >= 80.0 && roll < 22 ? MoveType.PUSH
         : proficiency >= 40.0 && roll < 42 ? MoveType.TREMOR
         : proficiency >= 10.0 && roll < 76 ? MoveType.FLURRY
         : roll < 88 ? MoveType.KICK : MoveType.PUNCH;
      perform(npc, target, move);
      int recovery = switch (move) {
         case TREMOR -> 24;
         case PUSH -> 20;
         case FLURRY -> 14;
         default -> 11;
      };
      if (proficiency >= 80.0) recovery = Math.max(8, recovery - 3);
      else if (proficiency >= 40.0) recovery = Math.max(9, recovery - 1);
      npc.getPersistentData().putLong(TAG_NEXT_SKILL, now + recovery);
   }

   private static void circleTarget(PathfinderMob npc, LivingEntity target, double distance, double attackReach, long now) {
      if (now >= npc.getPersistentData().getLong(TAG_NEXT_STRAFE_SWITCH)) {
         npc.getPersistentData().putInt(TAG_STRAFE_SIDE, npc.getRandom().nextBoolean() ? 1 : -1);
         npc.getPersistentData().putLong(TAG_NEXT_STRAFE_SWITCH, now + 12L + npc.getRandom().nextInt(12));
      }
      int side = npc.getPersistentData().getInt(TAG_STRAFE_SIDE);
      if (side == 0) side = 1;
      float forward = distance > attackReach - 0.25 ? 0.5F : distance < 1.7 ? -0.2F : 0.12F;
      npc.getMoveControl().strafe(forward, side * 0.62F);
      npc.getLookControl().setLookAt(target, 70.0F, 55.0F);
   }

   private static void perform(PathfinderMob npc, LivingEntity target, MoveType move) {
      float baseDamage = (float)Math.max(2.0, npc.getAttributeValue(Attributes.ATTACK_DAMAGE));
      float damage = switch (move) {
         case PUNCH -> baseDamage;
         case KICK -> baseDamage * 1.1F;
         case FLURRY -> baseDamage * 1.2F;
         case TREMOR -> baseDamage * 0.8F;
         case PUSH -> baseDamage * 1.35F;
      };
      triggerPose(npc, move);
      target.invulnerableTime = 0;
      boolean hit = target.hurt(npc.damageSources().mobAttack(npc), damage);
      target.invulnerableTime = 0;
      if (!hit) return;

      Vec3 direction = horizontalDirection(npc, target);
      switch (move) {
         case KICK -> target.push(direction.x * 0.45, 0.12, direction.z * 0.45);
         case FLURRY -> target.addEffect(new MobEffectInstance(ModMobEffects.STAGGER, 10, 0, false, true, true));
         case TREMOR -> target.addEffect(new MobEffectInstance(ModMobEffects.OFF_BALANCE, 30, 0, false, true, true));
         case PUSH -> target.push(direction.x * 1.05, 0.18, direction.z * 1.05);
         default -> target.addEffect(new MobEffectInstance(ModMobEffects.STAGGER, 6, 0, false, true, true));
      }
      target.hurtMarked = true;
      if (npc.level() instanceof ServerLevel level) {
         level.sendParticles(move == MoveType.TREMOR ? ParticleTypes.CLOUD : ParticleTypes.SWEEP_ATTACK,
            target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), move == MoveType.TREMOR ? 10 : 1, 0.3, 0.2, 0.3, 0.02);
         level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 0.65F, move == MoveType.TREMOR ? 0.7F : 0.9F);
      }
   }

   private static void triggerPose(PathfinderMob npc, MoveType move) {
      npc.swing(InteractionHand.MAIN_HAND, true);
      if (npc instanceof NpcActionPose action) {
         int pose = switch (move) {
            case KICK -> POSE_KICK;
            case TREMOR -> POSE_TREMOR;
            case PUSH -> POSE_PUSH;
            case FLURRY -> POSE_ELBOW;
            default -> POSE_PUNCH;
         };
         action.triggerNpcActionPose(pose, move == MoveType.TREMOR ? 12 : 9);
      }
      if (npc instanceof MysticMagicianEntity magician) {
         int pose = switch (move) {
            case KICK -> MysticMagicianEntity.MELEE_POSE_WHIP_KICK;
            case TREMOR, PUSH -> MysticMagicianEntity.MELEE_POSE_SLAM;
            default -> MysticMagicianEntity.MELEE_POSE_PUNCH;
         };
         magician.triggerMeleeSkillPose(pose, move == MoveType.TREMOR ? 12 : 9);
      }
   }

   private static boolean validTarget(PathfinderMob npc, LivingEntity target) {
      if (target == null || !target.isAlive() || target == npc || npc.isAlliedTo(target) || EntityUtils.isImmunePlayerTarget(target)) return false;
      return !(target instanceof Player player) || !player.isCreative() && !player.isSpectator();
   }

   private static Vec3 horizontalDirection(PathfinderMob npc, LivingEntity target) {
      Vec3 direction = target.position().subtract(npc.position()).multiply(1.0, 0.0, 1.0);
      return direction.lengthSqr() < 1.0E-5 ? new Vec3(0.0, 0.0, 1.0) : direction.normalize();
   }

   private enum MoveType { PUNCH, KICK, FLURRY, TREMOR, PUSH }
}
