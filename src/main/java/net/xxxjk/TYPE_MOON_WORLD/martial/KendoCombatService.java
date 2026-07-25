package net.xxxjk.TYPE_MOON_WORLD.martial;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

/** Shared server-side input and progression logic for both kendo schools. */
public final class KendoCombatService {
   public static final String HOKUSHIN_ID = "hokushin_ittoryu";
   public static final String TENNEN_ID = "tennen_rishin_ryu";
   public static final int INPUT_A = 0, INPUT_B_START = 1, INPUT_B_END = 2, INPUT_JUMP = 3, INPUT_DOWN = 4;
   private static final String TAG_RECOVERY = "TypeMoonKendoRecovery";
   private static final String TAG_A_COUNT = "TypeMoonKendoACount";
   private static final String TAG_DOWN_A_COUNT = "TypeMoonKendoDownACount";
   private static final String TAG_DOWN_UNTIL = "TypeMoonKendoDownUntil";
   private static final String TAG_JUMP_UNTIL = "TypeMoonKendoJumpUntil";
   private static final String TAG_STANCE = "TypeMoonKendoStance";
   private static final String TAG_STANCE_UNTIL = "TypeMoonKendoStanceUntil";
   private static final String TAG_TELEPORT_COOLDOWN = "TypeMoonKendoTeleportCooldown";
   private static final String TAG_THREE_THRUST_COOLDOWN = "TypeMoonKendoThreeThrustCooldown";
   private static final String TAG_THREE_THRUST_TELEPORTS = "TypeMoonKendoThreeThrustTeleports";
   private static final String TAG_THREE_THRUST_UNTIL = "TypeMoonKendoThreeThrustUntil";
   private static final String TAG_INVULNERABLE_UNTIL = "TypeMoonKendoInvulnerableUntil";
   private static final String TAG_ATTACK_ACTIVE_UNTIL = "TypeMoonKendoAttackActiveUntil";
   private static final String TAG_RUN_STREAK = "TypeMoonKendoRunStreak";
   private static final String TAG_LAST_RUN_TICK = "TypeMoonKendoLastRunTick";
   private static final String TAG_MARTIAL_DAMAGE = "TypeMoonKendoMartialDamage";

   private KendoCombatService() {}

   public static boolean isActive(ServerPlayer player, KendoSchool school) {
      if (player == null || school == null) return false;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      String id = PlayerMagicSelectionService.getCurrentMagicId(vars);
      return school.learned(vars) && school.id().equals(id) && vars.is_magic_circuit_open && !vars.servant_card_transformed
         && GanryuCombatService.hasValidHands(player);
   }

   public static boolean isReadyWithInvitation(ServerPlayer player, KendoSchool school) {
      if (player == null || school == null) return false;
      boolean invitationAndBlade = player.getMainHandItem().is(net.xxxjk.TYPE_MOON_WORLD.item.ModItems.SPARRING_INVITATION.get())
         && GanryuCombatService.isAllowedBlade(player.getOffhandItem())
         || player.getOffhandItem().is(net.xxxjk.TYPE_MOON_WORLD.item.ModItems.SPARRING_INVITATION.get())
         && GanryuCombatService.isAllowedBlade(player.getMainHandItem());
      return invitationAndBlade && isActive(player, school);
   }

   public static boolean isMartialDamage(ServerPlayer player) {
      return player != null && player.getPersistentData().getBoolean(TAG_MARTIAL_DAMAGE);
   }

   public static boolean learn(ServerPlayer player, KendoSchool school) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (school.learned(vars)) return false;
      if (school == KendoSchool.HOKUSHIN) vars.hokushin_learned = true;
      else vars.tennen_learned = true;
      if (!vars.learned_magics.contains(school.id())) vars.learned_magics.add(school.id());
      vars.syncPlayerVariables(player);
      player.displayClientMessage(Component.translatable("message.typemoonworld." + school.id() + ".learned"), false);
      return true;
   }

   public static void forget(ServerPlayer player, KendoSchool school) {
      if (player == null || school == null) return;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (school == KendoSchool.HOKUSHIN) {
         vars.hokushin_learned = false;
         vars.hokushin_proficiency = 0.0;
         vars.hokushin_master_defeated = false;
      } else {
         vars.tennen_learned = false;
         vars.tennen_proficiency = 0.0;
         vars.tennen_master_defeated = false;
      }
      vars.learned_magics.remove(school.id());
      vars.rebuildSelectedMagicsFromActiveWheel();
      vars.syncPlayerVariables(player);
   }

   public static boolean isUnlocked(TypeMoonWorldModVariables.PlayerVariables vars, KendoSchool school, KendoMove move) {
      if (vars == null || school == null || move == null || !school.learned(vars)) return false;
      if (move == KendoMove.UKEMI) return MartialUkemiService.isLearned(vars);
      return school.proficiency(vars) + 1.0E-6 >= requiredProficiency(school, move)
         && (move != KendoMove.STANCE && move != KendoMove.THREE_THRUST && move != KendoMove.CLOUD_DRAGON
             || school.masterDefeated(vars));
   }

   static double requiredProficiency(KendoSchool school, KendoMove move) {
      if (school == KendoSchool.HOKUSHIN) return move.requiredProficiency();
      return switch (move) {
         case HIGH_JUMP -> 0.0;
         case PRIMARY_ONE, UP_ATTACK -> 10.0;
         case PRIMARY_TWO, ARC -> 20.0;
         case PRIMARY_THREE, DOWN_ONE, DOWN_TWO, CUT, EXECUTION, RAY -> 50.0;
         case TELEPORT, RUNNING_RAY -> 70.0;
         case STANCE, THREE_THRUST, CLOUD_DRAGON -> 80.0;
         case PERFECT_SWORD -> 100.0;
         default -> 0.0;
      };
   }

   public static void handleInput(ServerPlayer player, KendoSchool school, int input, boolean down, boolean up) {
      if (input == INPUT_B_END) {
         if (player.getPersistentData().getBoolean(TAG_STANCE)) {
            player.getPersistentData().remove(TAG_STANCE);
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
         }
         return;
      }
      if (!isActive(player, school)) return;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      long now = player.level().getGameTime();
      if (player.getPersistentData().getBoolean(TAG_STANCE) && now > player.getPersistentData().getLong(TAG_STANCE_UNTIL))
         player.getPersistentData().remove(TAG_STANCE);
      if (input == INPUT_DOWN) {
         if (school == KendoSchool.TENNEN && now <= player.getPersistentData().getLong(TAG_JUMP_UNTIL)
            && now >= player.getPersistentData().getLong(TAG_RECOVERY)) {
            player.getPersistentData().remove(TAG_JUMP_UNTIL);
            perform(player, school, vars, KendoMove.HIGH_JUMP);
            return;
         }
         player.getPersistentData().putLong(TAG_DOWN_UNTIL, now + 8L);
         return;
      }
      if (input == INPUT_JUMP) {
         boolean combo = player.getPersistentData().getLong(TAG_DOWN_UNTIL) >= now;
         if (school == KendoSchool.TENNEN && !down && !combo) {
            player.getPersistentData().putLong(TAG_JUMP_UNTIL, now + 8L);
         } else if ((combo || down) && now >= player.getPersistentData().getLong(TAG_RECOVERY)) {
            perform(player, school, vars, KendoMove.HIGH_JUMP);
         }
         return;
      }
      if (input == INPUT_B_START) {
         if (MartialUkemiService.tryUse(player, true)) return;
         if (now < player.getPersistentData().getLong(TAG_RECOVERY)
            && !(school == KendoSchool.TENNEN && !down)) return;
         if (school == KendoSchool.TENNEN) {
            KendoMove bMove = down ? KendoMove.STANCE : KendoMove.TELEPORT;
            if (isUnlocked(vars, school, bMove)) {
               if (bMove == KendoMove.TELEPORT) {
                  int freeTeleports = player.getPersistentData().getInt(TAG_THREE_THRUST_TELEPORTS);
                  if (freeTeleports <= 0 && now < player.getPersistentData().getLong(TAG_TELEPORT_COOLDOWN)) return;
                  perform(player, school, vars, bMove);
                  return;
               }
               player.getPersistentData().putBoolean(TAG_STANCE, true);
               player.getPersistentData().putLong(TAG_STANCE_UNTIL, now + 100L);
               player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 105, 1, false, false, true));
               player.getPersistentData().putLong(TAG_RECOVERY, now + 4L);
            }
         } else {
            KendoMove bMove = down ? KendoMove.DRAW_SLASH : KendoMove.PARRY;
            if (isUnlocked(vars, school, bMove)) perform(player, school, vars, bMove);
         }
         return;
      }
      if (input != INPUT_A || now < player.getPersistentData().getLong(TAG_RECOVERY)) return;
      if (school == KendoSchool.TENNEN && player.getPersistentData().getBoolean(TAG_STANCE) && down) {
         if (now < player.getPersistentData().getLong(TAG_THREE_THRUST_COOLDOWN)) return;
         player.getPersistentData().remove(TAG_STANCE);
         perform(player, school, vars, KendoMove.THREE_THRUST);
         return;
      }
      boolean stance = player.getPersistentData().getBoolean(TAG_STANCE);
      int count = down ? next(player, TAG_DOWN_A_COUNT, 3) : next(player, TAG_A_COUNT, 3);
      KendoMove move;
      if (school == KendoSchool.HOKUSHIN) {
         move = down ? (count == 1 ? KendoMove.DOWN_ONE : count == 2 ? KendoMove.DOWN_TWO : KendoMove.DOWN_THREE)
            : up ? KendoMove.UP_ATTACK : (count == 1 ? KendoMove.PRIMARY_ONE : count == 2 ? KendoMove.PRIMARY_TWO : KendoMove.PRIMARY_THREE);
         if (stance) move = KendoMove.DRAW_SLASH;
      } else {
         move = down ? (count == 1 ? KendoMove.DOWN_ONE : KendoMove.DOWN_TWO)
            : up ? KendoMove.UP_ATTACK : (count == 1 ? KendoMove.PRIMARY_ONE : count == 2 ? KendoMove.ARC : KendoMove.CUT);
         if (!down && !up && count == 3 && vars.tennen_proficiency >= 100.0) move = KendoMove.PERFECT_SWORD;
      }
      if (isUnlocked(vars, school, move)) perform(player, school, vars, move);
      else player.displayClientMessage(Component.translatable("message.typemoonworld.kendo.move_locked"), true);
   }

   private static int next(ServerPlayer player, String key, int max) {
      int value = player.getPersistentData().getInt(key) % max + 1;
      player.getPersistentData().putInt(key, value);
      return value;
   }

   private static void perform(ServerPlayer player, KendoSchool school, TypeMoonWorldModVariables.PlayerVariables vars, KendoMove move) {
      long now = player.level().getGameTime();
      int recoveryTicks = recovery(move, vars.body_technique);
      if (school == KendoSchool.TENNEN && isUnlocked(vars, school, KendoMove.RAY)) recoveryTicks = Math.max(3, recoveryTicks - 3);
      player.getPersistentData().putLong(TAG_RECOVERY, now + recoveryTicks);
      player.getPersistentData().putLong(TAG_ATTACK_ACTIVE_UNTIL, now + 8L);
      if (move == KendoMove.HIGH_JUMP) {
         player.setDeltaMovement(player.getDeltaMovement().x, 1.05 + BodyTrainingService.stagedPercent(vars.body_technique) * 0.4, player.getDeltaMovement().z);
         player.hurtMarked = true;
         spawnMoveFx(player, school, move);
         return;
      }
      LivingEntity target = target(player, move);
      if (move == KendoMove.TELEPORT) {
         Vec3 dir = horizontalLook(player);
         player.teleportTo(player.getX() + dir.x * 2.0, player.getY(), player.getZ() + dir.z * 2.0);
         int freeTeleports = player.getPersistentData().getInt(TAG_THREE_THRUST_TELEPORTS);
         if (freeTeleports > 0 && now <= player.getPersistentData().getLong(TAG_THREE_THRUST_UNTIL)) {
            player.getPersistentData().putInt(TAG_THREE_THRUST_TELEPORTS, freeTeleports - 1);
         } else {
            player.getPersistentData().putInt(TAG_THREE_THRUST_TELEPORTS, 0);
            player.getPersistentData().putLong(TAG_TELEPORT_COOLDOWN, now + 60L);
         }
         player.hurtMarked = true;
      } else if (school == KendoSchool.TENNEN && move == KendoMove.ARC) {
         dashBackward(player, 2.2);
         player.getPersistentData().putLong(TAG_INVULNERABLE_UNTIL, now + 12L);
         if (target != null) hurtTarget(player, target, baseDamage(player, school, move) * 0.9F, move);
      } else if (school == KendoSchool.TENNEN && move == KendoMove.PRIMARY_THREE) {
         if (target != null) dashToTarget(player, target, 0.8);
         else dashForward(player, 2.4);
         if (target != null) hurtTarget(player, target, baseDamage(player, school, move), move);
      } else if (school == KendoSchool.TENNEN && move == KendoMove.DOWN_ONE) {
         if (target != null) dashToTarget(player, target, 0.8);
         else dashForward(player, 1.8);
         if (target != null) {
            float damage = baseDamage(player, school, move) * 0.72F;
            hurtTarget(player, target, damage, move);
            hurtTarget(player, target, damage, move);
         }
      } else if (school == KendoSchool.TENNEN && move == KendoMove.DOWN_TWO) {
         if (target != null) {
            dashToTarget(player, target, 0.65);
            target.stopUsingItem();
            hurtTarget(player, target, baseDamage(player, school, move) * 2.5F, move);
         } else dashForward(player, 1.2);
      } else if (school == KendoSchool.TENNEN && move == KendoMove.THREE_THRUST) {
         player.getPersistentData().putLong(TAG_THREE_THRUST_COOLDOWN, now + 200L);
         player.getPersistentData().putLong(TAG_THREE_THRUST_UNTIL, now + 100L);
         player.getPersistentData().putInt(TAG_THREE_THRUST_TELEPORTS, 3);
         if (target != null) {
            for (int i = 0; i < 3; i++) hurtTarget(player, target, 15.0F, move);
            target.addEffect(new MobEffectInstance(ModMobEffects.STAGGER, 16, 0, false, true, true));
         } else dashForward(player, 1.5);
      } else if (school == KendoSchool.HOKUSHIN && move == KendoMove.PRIMARY_THREE) {
         dashForward(player, 0.7);
         areaStrike(player, move, 3.35, baseDamage(player, school, move) * 1.15F);
      } else if (school == KendoSchool.HOKUSHIN && move == KendoMove.DOWN_ONE) {
         if (target != null) dashToTarget(player, target, 0.5);
         else dashForward(player, 4.2);
         if (target != null) hurtTarget(player, target, baseDamage(player, school, move) * 1.25F, move);
      } else if (school == KendoSchool.HOKUSHIN && move == KendoMove.DOWN_TWO) {
         if (target != null) {
            boolean countered = target instanceof Mob mob && mob.getTarget() == player;
            dashToTarget(player, target, 0.55);
            hurtTarget(player, target, baseDamage(player, school, move) * (countered ? 3.0F : 1.25F), move);
         } else dashForward(player, 1.4);
      } else if (school == KendoSchool.HOKUSHIN && move == KendoMove.DOWN_THREE) {
         if (target != null) {
            boolean countered = target instanceof Mob mob && mob.getTarget() == player;
            dashToTarget(player, target, 0.6);
            hurtTarget(player, target, baseDamage(player, school, move) * (countered ? 1.5F : 1.0F), move);
         } else dashForward(player, 2.0);
      } else if (school == KendoSchool.HOKUSHIN && move == KendoMove.PARRY) {
         if (target != null) {
            disarmTarget(target);
            dashToTarget(player, target, 0.5);
            hurtTarget(player, target, baseDamage(player, school, move) * 1.35F, move);
         }
      } else if (move == KendoMove.DRAW_SLASH) {
         if (target != null) dashToTarget(player, target, 0.55);
         else dashForward(player, 3.4);
         if (target != null) hurtTarget(player, target, baseDamage(player, school, move), move);
      } else if (target != null) {
         if (move == KendoMove.PRIMARY_TWO) dashToTarget(player, target, 0.8);
         else if (move == KendoMove.PRIMARY_ONE || move == KendoMove.UP_ATTACK) dashForward(player, 0.35);
         hurtTarget(player, target, baseDamage(player, school, move), move);
      } else if (move == KendoMove.PRIMARY_TWO) {
         dashForward(player, 3.8);
      } else if (move == KendoMove.PRIMARY_ONE || move == KendoMove.UP_ATTACK) {
         dashForward(player, 0.6);
      }
      if (move == KendoMove.PRIMARY_ONE) blockNearbyProjectiles(player);
      spawnMoveFx(player, school, move);
   }

   private static void hurtTarget(ServerPlayer player, LivingEntity target, float damage, KendoMove move) {
      if (!target.isAlive()) return;
      Vec3 look = player.getLookAngle();
      if (look.y > 0.3 && player.getRandom().nextFloat() < 0.30F) damage *= 2.0F;
      target.invulnerableTime = 0;
      player.getPersistentData().putBoolean(TAG_MARTIAL_DAMAGE, true);
      try {
         target.hurt(player.damageSources().playerAttack(player), damage);
      } finally {
         player.getPersistentData().remove(TAG_MARTIAL_DAMAGE);
      }
      target.invulnerableTime = 0;
      applyMoveEffects(player, target, move);
   }

   private static void areaStrike(ServerPlayer player, KendoMove move, double radius, float damage) {
      AABB box = player.getBoundingBox().inflate(radius, 1.6, radius);
      Vec3 origin = player.position().add(0.0, player.getBbHeight() * 0.55, 0.0);
      Vec3 look = horizontalLook(player);
      for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, box,
         entity -> {
            if (entity == player || !entity.isAlive() || !player.hasLineOfSight(entity)) return false;
            Vec3 toTarget = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0)
               .subtract(origin).multiply(1.0, 0.0, 1.0);
            double distance = toTarget.length();
            return distance > 0.2 && distance <= radius && look.dot(toTarget.normalize()) >= 0.05;
         })) {
         hurtTarget(player, entity, damage, move);
         entity.stopUsingItem();
      }
   }

   private static void applyMoveEffects(ServerPlayer player, LivingEntity target, KendoMove move) {
      if (move == KendoMove.PRIMARY_THREE || move == KendoMove.CUT || move == KendoMove.EXECUTION
         || move == KendoMove.THREE_THRUST || move == KendoMove.DOWN_TWO || move == KendoMove.DOWN_THREE) {
         target.addEffect(new MobEffectInstance(ModMobEffects.STAGGER, move == KendoMove.PRIMARY_THREE ? 20 : 12, 0, false, true, true));
      }
      Vec3 look = player.getLookAngle();
      if (look.y < -0.35) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, true, true));
      else if (Math.abs(look.y) <= 0.3) {
         target.addEffect(new MobEffectInstance(ModMobEffects.OFF_BALANCE, 60, 0, false, true, true));
         if (player.getRandom().nextFloat() < 0.35F)
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0, false, true, true));
      }
   }

   private static void disarmTarget(LivingEntity target) {
      ItemStack held = target.getMainHandItem();
      if (held.isEmpty()) return;
      target.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
      target.spawnAtLocation(held.copy(), 0.1F);
   }

   /** Lightweight sword trails shared by both schools; this only adds particles and sound, never a player pose. */
   private static void spawnMoveFx(ServerPlayer player, KendoSchool school, KendoMove move) {
      ServerLevel level = player.serverLevel();
      Vec3 direction = player.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (direction.lengthSqr() < 1.0E-4) direction = new Vec3(0.0, 0.0, 1.0);
      direction = direction.normalize();
      Vec3 pos = player.position().add(direction.scale(move == KendoMove.TELEPORT ? 0.5 : 1.15)).add(0.0, 1.0, 0.0);
      ParticleOptions particle = switch (move) {
         case HIGH_JUMP -> ParticleTypes.CLOUD;
         case TELEPORT -> ParticleTypes.PORTAL;
         case PERFECT_SWORD, EXECUTION -> ParticleTypes.END_ROD;
         case PRIMARY_TWO, DOWN_TWO, DOWN_THREE, THREE_THRUST, RAY, RUNNING_RAY -> ParticleTypes.CRIT;
         default -> ParticleTypes.SWEEP_ATTACK;
      };
      int count = move == KendoMove.THREE_THRUST || move == KendoMove.PERFECT_SWORD ? 3 : move == KendoMove.TELEPORT ? 10 : 1;
      level.sendParticles(particle, pos.x, pos.y, pos.z, count, 0.28, 0.22, 0.28, 0.035);
      if (school == KendoSchool.TENNEN && (move == KendoMove.TELEPORT || move == KendoMove.RUNNING_RAY)) {
         level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 2, 0.18, 0.18, 0.18, 0.01);
      }
      SoundEvent sound = move == KendoMove.TELEPORT ? SoundEvents.ENDERMAN_TELEPORT
         : move == KendoMove.PERFECT_SWORD || move == KendoMove.EXECUTION ? SoundEvents.PLAYER_ATTACK_STRONG
         : SoundEvents.PLAYER_ATTACK_SWEEP;
      level.playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS,
         move == KendoMove.TELEPORT ? 0.55F : 0.8F, school == KendoSchool.HOKUSHIN ? 1.05F : 1.15F);
   }

   private static LivingEntity target(ServerPlayer player, KendoMove move) {
      double range = strikeRange(move);
      Vec3 look = horizontalLook(player);
      Vec3 origin = player.position().add(0.0, player.getBbHeight() * 0.55, 0.0);
      double cone = move == KendoMove.PRIMARY_TWO ? 0.05 : move == KendoMove.PRIMARY_THREE || move == KendoMove.EXECUTION ? -0.05 : 0.22;
      AABB box = player.getBoundingBox().inflate(range, 1.5, range);
      return player.level().getEntitiesOfClass(LivingEntity.class, box, entity -> {
         if (entity == player || !entity.isAlive() || !player.hasLineOfSight(entity)) return false;
         Vec3 toTarget = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0).subtract(origin).multiply(1.0, 0.0, 1.0);
         double distance = toTarget.length();
         return distance > 0.01 && distance <= range && look.dot(toTarget.normalize()) >= cone;
      }).stream().min((a, b) -> Double.compare(player.distanceToSqr(a), player.distanceToSqr(b))).orElse(null);
   }

   private static double strikeRange(KendoMove move) {
      return switch (move) {
         case PRIMARY_ONE, UP_ATTACK, PARRY -> 2.8;
         case PRIMARY_TWO -> 4.6;
         case DOWN_ONE -> 5.8;
         case PRIMARY_THREE, DOWN_TWO, DOWN_THREE, CUT, EXECUTION -> 3.4;
         case THREE_THRUST, RAY, RUNNING_RAY -> 4.0;
         default -> 3.2;
      };
   }

   private static Vec3 horizontalLook(ServerPlayer player) {
      Vec3 look = player.getLookAngle().multiply(1.0, 0.0, 1.0);
      return look.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : look.normalize();
   }

   private static void dashToTarget(ServerPlayer player, LivingEntity target, double stoppingDistance) {
      Vec3 direction = target.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
      if (direction.lengthSqr() < 1.0E-4) return;
      direction = direction.normalize();
      double distance = Math.sqrt(player.distanceToSqr(target));
      if (distance <= stoppingDistance) return;
      double travel = Math.min(distance - stoppingDistance, 5.5);
      player.teleportTo(player.getX() + direction.x * travel, player.getY(), player.getZ() + direction.z * travel);
      player.setDeltaMovement(direction.scale(0.18));
      player.hurtMarked = true;
   }

   private static void dashForward(ServerPlayer player, double distance) {
      Vec3 direction = horizontalLook(player);
      player.teleportTo(player.getX() + direction.x * distance, player.getY(), player.getZ() + direction.z * distance);
      player.setDeltaMovement(direction.scale(0.18));
      player.hurtMarked = true;
   }

   private static void dashBackward(ServerPlayer player, double distance) {
      Vec3 direction = horizontalLook(player).scale(-1.0);
      player.teleportTo(player.getX() + direction.x * distance, player.getY(), player.getZ() + direction.z * distance);
      player.setDeltaMovement(direction.scale(0.18));
      player.hurtMarked = true;
   }

   /** Maintains stance slowdown and the escalating Raiko running bonus. */
   public static void tickPlayer(ServerPlayer player) {
      if (player == null) return;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      KendoSchool school = activeSchool(vars);
      if (school == null) {
         if (player.getPersistentData().getBoolean(TAG_STANCE)) {
            player.getPersistentData().remove(TAG_STANCE);
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
         }
         player.getPersistentData().remove(TAG_RUN_STREAK);
         return;
      }
      long now = player.level().getGameTime();
      if (player.getPersistentData().getBoolean(TAG_STANCE)) {
         if (now > player.getPersistentData().getLong(TAG_STANCE_UNTIL)) {
            player.getPersistentData().remove(TAG_STANCE);
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
         } else {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 12, 1, false, false, true));
            player.setDeltaMovement(0.0, player.getDeltaMovement().y, 0.0);
         }
      }
      if (school == KendoSchool.TENNEN && isUnlocked(vars, school, KendoMove.RUNNING_RAY) && player.isSprinting()) {
         long last = player.getPersistentData().getLong(TAG_LAST_RUN_TICK);
         int streak = last + 12L >= now ? Math.min(3, player.getPersistentData().getInt(TAG_RUN_STREAK) + 1) : 1;
         player.getPersistentData().putInt(TAG_RUN_STREAK, streak);
         player.getPersistentData().putLong(TAG_LAST_RUN_TICK, now);
         player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 12, streak - 1, false, false, true));
      } else if (now - player.getPersistentData().getLong(TAG_LAST_RUN_TICK) > 12L) {
         player.getPersistentData().remove(TAG_RUN_STREAK);
      }
   }

   private static KendoSchool activeSchool(TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars == null || !vars.is_magic_circuit_open || vars.servant_card_transformed) return null;
      String id = PlayerMagicSelectionService.getCurrentMagicId(vars);
      if (KendoSchool.HOKUSHIN.id().equals(id) && vars.hokushin_learned) return KendoSchool.HOKUSHIN;
      if (KendoSchool.TENNEN.id().equals(id) && vars.tennen_learned) return KendoSchool.TENNEN;
      return null;
   }

   public static boolean isKendoInvulnerable(ServerPlayer player) {
      return player != null && player.getPersistentData().getLong(TAG_INVULNERABLE_UNTIL) > player.level().getGameTime();
   }

   public static boolean tryCloudDragonEvasion(ServerPlayer player) {
      if (player == null || !isKendoInvulnerableWindow(player)) return false;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!isActive(player, KendoSchool.TENNEN) || !isUnlocked(vars, KendoSchool.TENNEN, KendoMove.CLOUD_DRAGON)) return false;
      return player.getRandom().nextFloat() < 0.50F;
   }

   private static boolean isKendoInvulnerableWindow(ServerPlayer player) {
      return player.getPersistentData().getLong(TAG_ATTACK_ACTIVE_UNTIL) > player.level().getGameTime();
   }

   private static void blockNearbyProjectiles(ServerPlayer player) {
      Vec3 look = horizontalLook(player);
      Vec3 origin = player.position().add(0.0, player.getBbHeight() * 0.55, 0.0);
      AABB box = player.getBoundingBox().inflate(1.35, 1.0, 1.35);
      for (Projectile projectile : player.level().getEntitiesOfClass(Projectile.class, box,
         projectile -> projectile.isAlive() && projectile.getOwner() != player)) {
         Vec3 toProjectile = projectile.position().subtract(origin).multiply(1.0, 0.0, 1.0);
         if (toProjectile.lengthSqr() > 0.01 && look.dot(toProjectile.normalize()) >= -0.15) {
            projectile.discard();
         }
      }
   }

   private static float baseDamage(ServerPlayer player, KendoSchool school, KendoMove move) {
      float base = (float)Math.max(3.0, player.getAttributeValue(Attributes.ATTACK_DAMAGE));
      if (move == KendoMove.PRIMARY_THREE || move == KendoMove.DRAW_SLASH || move == KendoMove.EXECUTION) base *= 1.8F;
      if (school == KendoSchool.TENNEN && move == KendoMove.PERFECT_SWORD) base *= 2.0F;
      return base;
   }

   private static int recovery(KendoMove move, int technique) {
      int base = switch (move) { case HIGH_JUMP -> 8; case PRIMARY_THREE, CUT, EXECUTION, THREE_THRUST -> 18; default -> 10; };
      return Math.max(3, (int)Math.ceil(base * (1.0 - BodyTrainingService.stagedPercent(technique))));
   }

   public static void addProficiency(ServerPlayer player, KendoSchool school, double amount) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      double before = school.proficiency(vars);
      double cap = school.masterDefeated(vars) ? 100.0 : school.preMasterCap();
      double value = Mth.clamp(Math.round((before + amount) * 100.0) / 100.0, 0.0, cap);
      if (school == KendoSchool.HOKUSHIN) vars.hokushin_proficiency = value;
      else vars.tennen_proficiency = value;
      if ((int)(before * 20.0) != (int)(value * 20.0)) vars.syncPlayerVariables(player);
   }
}
