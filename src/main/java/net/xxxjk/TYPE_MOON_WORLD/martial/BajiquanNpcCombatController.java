package net.xxxjk.TYPE_MOON_WORLD.martial;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

/** Distance-aware martial decisions shared by the master, apprentices, and Rin's close defense. */
public final class BajiquanNpcCombatController {
   private static final String TAG_NEXT_SKILL = "TypeMoonBajiquanNpcNextSkill";
   private static final String TAG_NEXT_DASH = "TypeMoonBajiquanNpcNextDash";

   private BajiquanNpcCombatController() {}

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
      npc.lookAt(target, 50.0F, 45.0F);
      if (!closeDefenseOnly && distance > 2.7) {
         npc.getNavigation().moveTo(target, proficiency >= 80.0 ? 1.3 : 1.12);
      }
      if (closeDefenseOnly && distance > 4.5) return;

      if (distance >= 3.0 && distance <= 7.5 && proficiency >= 10.0
         && now >= npc.getPersistentData().getLong(TAG_NEXT_DASH) && npc.hasLineOfSight(target)) {
         Vec3 direction = horizontalDirection(npc, target);
         npc.setDeltaMovement(npc.getDeltaMovement().add(direction.scale(proficiency >= 40.0 ? 0.72 : 0.5)).add(0.0, 0.06, 0.0));
         npc.hurtMarked = true;
         npc.getPersistentData().putLong(TAG_NEXT_DASH, now + 28L);
      }

      if (distance > 4.8 || !npc.hasLineOfSight(target) || now < npc.getPersistentData().getLong(TAG_NEXT_SKILL)) return;
      int roll = npc.getRandom().nextInt(100);
      MoveType move = proficiency >= 40.0 && roll < 20 ? MoveType.PUSH
         : proficiency >= 25.0 && roll < 40 ? MoveType.TREMOR
         : proficiency >= 10.0 && roll < 72 ? MoveType.FLURRY
         : proficiency >= 0.5 ? MoveType.KICK : MoveType.PUNCH;
      perform(npc, target, move);
      int recovery = switch (move) {
         case TREMOR -> 34;
         case PUSH -> 28;
         case FLURRY -> 22;
         default -> 18;
      };
      npc.getPersistentData().putLong(TAG_NEXT_SKILL, now + recovery);
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
