package net.xxxjk.TYPE_MOON_WORLD.martial;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
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
   private static final String TAG_STANCE = "TypeMoonKendoStance";
   private static final String TAG_STANCE_UNTIL = "TypeMoonKendoStanceUntil";
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
         player.getPersistentData().remove(TAG_STANCE);
         return;
      }
      if (!isActive(player, school)) return;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      long now = player.level().getGameTime();
      if (player.getPersistentData().getBoolean(TAG_STANCE) && now > player.getPersistentData().getLong(TAG_STANCE_UNTIL))
         player.getPersistentData().remove(TAG_STANCE);
      if (input == INPUT_DOWN) {
         player.getPersistentData().putLong(TAG_DOWN_UNTIL, now + 8L);
         return;
      }
      if (input == INPUT_JUMP) {
         boolean combo = player.getPersistentData().getLong(TAG_DOWN_UNTIL) >= now;
         if (combo && now >= player.getPersistentData().getLong(TAG_RECOVERY)) perform(player, school, vars, KendoMove.HIGH_JUMP);
         return;
      }
      if (input == INPUT_B_START) {
         if (MartialUkemiService.tryUse(player, true)) return;
         if (now < player.getPersistentData().getLong(TAG_RECOVERY)) return;
         if (school == KendoSchool.TENNEN) {
            KendoMove bMove = down ? KendoMove.STANCE : KendoMove.TELEPORT;
            if (isUnlocked(vars, school, bMove)) {
               if (bMove == KendoMove.TELEPORT) {
                  perform(player, school, vars, bMove);
                  return;
               }
               player.getPersistentData().putBoolean(TAG_STANCE, true);
               player.getPersistentData().putLong(TAG_STANCE_UNTIL, now + 100L);
            }
         } else {
            KendoMove bMove = down ? KendoMove.DRAW_SLASH : KendoMove.PARRY;
            if (isUnlocked(vars, school, bMove)) perform(player, school, vars, bMove);
         }
         return;
      }
      if (input != INPUT_A || now < player.getPersistentData().getLong(TAG_RECOVERY)) return;
      if (school == KendoSchool.TENNEN && player.getPersistentData().getBoolean(TAG_STANCE) && down) {
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
      player.getPersistentData().putLong(TAG_RECOVERY, now + recovery(move, vars.body_technique));
      if (move == KendoMove.HIGH_JUMP) {
         player.setDeltaMovement(player.getDeltaMovement().x, 1.05 + BodyTrainingService.stagedPercent(vars.body_technique) * 0.4, player.getDeltaMovement().z);
         player.hurtMarked = true;
         return;
      }
      LivingEntity target = target(player, move == KendoMove.TELEPORT ? 8.0 : 4.5);
      if (move == KendoMove.TELEPORT) {
         Vec3 dir = player.getLookAngle().normalize();
         player.teleportTo(player.getX() + dir.x * 2.0, player.getY(), player.getZ() + dir.z * 2.0);
      } else if (target != null) {
         float damage = baseDamage(player, school, move);
         if (school == KendoSchool.TENNEN && move == KendoMove.THREE_THRUST) damage = 15.0F;
         if (school == KendoSchool.HOKUSHIN && move == KendoMove.DOWN_TWO) damage *= 1.5F;
         target.invulnerableTime = 0;
         player.getPersistentData().putBoolean(TAG_MARTIAL_DAMAGE, true);
         try {
            target.hurt(player.damageSources().playerAttack(player), damage);
         } finally {
            player.getPersistentData().remove(TAG_MARTIAL_DAMAGE);
         }
         target.invulnerableTime = 0;
         if (move == KendoMove.PRIMARY_THREE || move == KendoMove.CUT || move == KendoMove.THREE_THRUST)
            target.addEffect(new MobEffectInstance(ModMobEffects.STAGGER, 12, 0, false, true, true));
      }
      player.swing(InteractionHand.MAIN_HAND, true);
   }

   private static LivingEntity target(ServerPlayer player, double range) {
      AABB box = player.getBoundingBox().inflate(range, 2.0, range);
      return player.level().getEntitiesOfClass(LivingEntity.class, box, entity -> entity != player && entity.isAlive() && player.hasLineOfSight(entity))
         .stream().min((a, b) -> Double.compare(player.distanceToSqr(a), player.distanceToSqr(b))).orElse(null);
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
