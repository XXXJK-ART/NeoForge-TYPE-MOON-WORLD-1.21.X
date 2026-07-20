package net.xxxjk.TYPE_MOON_WORLD.martial;

import java.util.Comparator;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.advancement.TypeMoonAdvancementHelper;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.network.BajiquanPoseMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.CircleRealmStateMessage;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.LiShuwenEntity;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public final class BajiquanCombatService {
   public static final String MAGIC_ID = "bajiquan";
   public static final int INPUT_A = 0;
   public static final int INPUT_B = 1;
   public static final int INPUT_JUMP = 2;
   public static final int INPUT_CIRCLE_REALM = 3;
   public static final int INPUT_DOWN = 4;
   public static final String TAG_CIRCLE_UNTIL = "TypeMoonBajiquanCircleUntil";
   private static final String TAG_LAST_MOVE = "TypeMoonBajiquanLastMove";
   private static final String TAG_LAST_MOVE_TICK = "TypeMoonBajiquanLastMoveTick";
   private static final String TAG_RECOVERY_UNTIL = "TypeMoonBajiquanRecoveryUntil";
   private static final String TAG_A_COUNT = "TypeMoonBajiquanACount";
   private static final String TAG_DOWN_A_COUNT = "TypeMoonBajiquanDownACount";
   private static final String TAG_B_COUNT = "TypeMoonBajiquanBCount";
   private static final String TAG_DOWN_B_COUNT = "TypeMoonBajiquanDownBCount";
   private static final String TAG_PUNCH_FROM_KICK = "TypeMoonBajiquanPunchFromKick";
   private static final String TAG_SHOULDER_PURSUIT = "TypeMoonBajiquanShoulderPursuit";
   private static final String TAG_HISTORY = "TypeMoonBajiquanHistory";
   private static final String TAG_HISTORY_TICK = "TypeMoonBajiquanHistoryTick";
   private static final String TAG_PARRY_UNTIL = "TypeMoonBajiquanParryUntil";
   private static final String TAG_REACTIVE_PARRY_UNTIL = "TypeMoonBajiquanReactiveParryUntil";
   private static final String TAG_CLAMP_UNTIL = "TypeMoonBajiquanClampUntil";
   private static final String TAG_OFF_BALANCE_TARGET = "TypeMoonBajiquanOffBalanceTarget";
   private static final String TAG_LAST_COMBAT = "TypeMoonBajiquanLastCombat";
   private static final String TAG_LAST_QI_HEAL = "TypeMoonBajiquanLastQiHeal";
   private static final String TAG_UKEMI_UNTIL = "TypeMoonBajiquanUkemiUntil";
   private static final String TAG_UKEMI_REDUCTION_UNTIL = "TypeMoonBajiquanUkemiReductionUntil";
   private static final String TAG_LAST_GROUNDED = "TypeMoonBajiquanLastGrounded";
   private static final String TAG_DOWN_UNTIL = "TypeMoonBajiquanDownUntil";
   private static final String TAG_LAST_INPUT_TICK = "TypeMoonBajiquanLastInputTick";
   private static final String TAG_PURSUIT_STAGE = "TypeMoonBajiquanPursuitStage";
   private static final String TAG_FA_JIN_FOLLOWUPS = "TypeMoonBajiquanFaJinFollowups";
   private static final String TAG_PALM_FROM_FLURRY = "TypeMoonBajiquanPalmFromFlurry";
   private static final String TAG_MARTIAL_DAMAGE = "TypeMoonBajiquanDamage";
   private static final String TAG_KNEE_AIR_UNTIL = "TypeMoonBajiquanKneeAirUntil";
   private static final String TAG_KNEE_LANDING_UNTIL = "TypeMoonBajiquanKneeLandingUntil";
   private static final String TAG_KNEE_PENDING = "TypeMoonBajiquanKneePending";
   private static final String[] COMBO_STATE_KEYS = {
      TAG_HISTORY, TAG_HISTORY_TICK, TAG_A_COUNT, TAG_DOWN_A_COUNT, TAG_B_COUNT, TAG_DOWN_B_COUNT,
      TAG_PUNCH_FROM_KICK, TAG_SHOULDER_PURSUIT, TAG_PURSUIT_STAGE, TAG_FA_JIN_FOLLOWUPS, TAG_PALM_FROM_FLURRY
   };
   private static final int COMBO_WINDOW = 12;

   private BajiquanCombatService() {}

   public static boolean isActive(ServerPlayer player) {
      if (player == null) return false;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.bajiquan_learned && vars.is_magic_circuit_open && !vars.servant_card_transformed
         && (player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty()
            || isSparring(player) && player.getMainHandItem().is(ModItems.SPARRING_INVITATION.get()) && player.getOffhandItem().isEmpty())
         && PlayerMagicSelectionService.isCurrentSelection(vars, MAGIC_ID);
   }

   /** Allows the invitation to occupy the main hand only for the master interaction. */
   public static boolean isActiveWithInvitation(ServerPlayer player) {
      if (player == null || !player.getMainHandItem().is(ModItems.SPARRING_INVITATION.get()) || !player.getOffhandItem().isEmpty()) return false;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.bajiquan_learned && vars.is_magic_circuit_open && !vars.servant_card_transformed
         && PlayerMagicSelectionService.isCurrentSelection(vars, MAGIC_ID);
   }

   public static boolean learn(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars.bajiquan_learned) return false;
      vars.bajiquan_learned = true;
      if (!vars.learned_magics.contains(MAGIC_ID)) vars.learned_magics.add(MAGIC_ID);
      vars.syncPlayerVariables(player);
      TypeMoonAdvancementHelper.grant(player, TypeMoonAdvancementHelper.BAJIQUAN);
      player.displayClientMessage(Component.translatable("message.typemoonworld.bajiquan.learned"), false);
      return true;
   }

   public static void forget(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.bajiquan_learned = false;
      vars.bajiquan_proficiency = 0.0;
      vars.bajiquan_tiger_unlocked = false;
      vars.learned_magics.remove(MAGIC_ID);
      vars.rebuildSelectedMagicsFromActiveWheel();
      vars.syncPlayerVariables(player);
   }

   public static boolean isUnlocked(TypeMoonWorldModVariables.PlayerVariables vars, BajiquanMove move) {
      if (vars == null || !vars.bajiquan_learned || vars.bajiquan_proficiency + 1.0E-6 < move.requiredProficiency()) return false;
      return switch (move) {
         case CHARGED_TREMOR, DOUBLE_PALM, FIERCE_TIGER -> vars.bajiquan_tiger_unlocked;
         default -> true;
      };
   }

   public static void handleInput(ServerPlayer player, int input, boolean down, boolean up) {
      if (!isActive(player)) return;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (player.hasEffect(ModMobEffects.STAGGER) || player.hasEffect(ModMobEffects.OFF_BALANCE)) return;
      if (input == INPUT_CIRCLE_REALM) {
         activateCircleRealm(player, vars);
         return;
      }
      CompoundTag data = player.getPersistentData();
      long now = player.level().getGameTime();
      if (input == INPUT_DOWN) {
         data.putLong(TAG_DOWN_UNTIL, now + 8L);
         return;
      }
      if (input == INPUT_JUMP) {
         if (down || data.getLong(TAG_DOWN_UNTIL) >= now) {
            if (now >= data.getLong(TAG_RECOVERY_UNTIL)) perform(player, vars, BajiquanMove.HIGH_JUMP, false);
         }
         return;
      }
      if (input == INPUT_B && tryUkemi(player, vars)) return;

      if (data.contains(TAG_LAST_INPUT_TICK) && data.getLong(TAG_LAST_INPUT_TICK) == now) return;
      data.putLong(TAG_LAST_INPUT_TICK, now);
      if (now - data.getLong(TAG_LAST_MOVE_TICK) > COMBO_WINDOW) resetCounters(data);
      CompoundTag comboSnapshot = saveComboState(data);
      String token = (down ? "D" : up ? "U" : "") + (input == INPUT_A ? "A" : "B");
      appendHistory(data, token, now);
      if (input == INPUT_B && down && isUnlocked(vars, BajiquanMove.PARRY)) data.putLong(TAG_REACTIVE_PARRY_UNTIL, now + 6L);

      BajiquanMove move;
      if (vars.bajiquan_tiger_unlocked && historyEnds(data, "A,DA,A,DA,DB,DA")) {
         move = BajiquanMove.FIERCE_TIGER;
      } else if (historyEnds(data, "DA,DB,DB")) {
         move = BajiquanMove.CLAMP;
      } else {
         move = selectMove(player, vars, input, down, up, now);
      }
      boolean clampPrefix = historyEnds(data, "DA,DB") && isUnlocked(vars, BajiquanMove.CLAMP);
      if (move == null) {
         if (clampPrefix) return;
         restoreComboState(data, comboSnapshot);
         return;
      }
      if (!isUnlocked(vars, move)) {
         if (clampPrefix) return;
         restoreComboState(data, comboSnapshot);
         player.displayClientMessage(Component.translatable("message.typemoonworld.bajiquan.move_locked"), true);
         return;
      }
      boolean comboCancel = now - data.getLong(TAG_LAST_MOVE_TICK) <= COMBO_WINDOW
         && BajiquanComboRules.isLegalRecoveryCancel(
            moveById(data.getString(TAG_LAST_MOVE)), move, data.getInt(TAG_PURSUIT_STAGE) > 0
         );
      if (now < data.getLong(TAG_RECOVERY_UNTIL) && !comboCancel) {
         restoreComboState(data, comboSnapshot);
         return;
      }
      perform(player, vars, move, comboCancel);
   }

   private static BajiquanMove selectMove(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, int input, boolean down, boolean up, long now) {
      CompoundTag data = player.getPersistentData();
      String last = data.getString(TAG_LAST_MOVE);
      if (input == INPUT_A) {
         if (up) return BajiquanMove.CHOP;
         int faJinFollowups = data.getInt(TAG_FA_JIN_FOLLOWUPS);
         if (last.equals(BajiquanMove.PARRY.id())) return findStoredTarget(player, 5.0, true) != null ? BajiquanMove.FA_JIN : null;
         if (last.equals(BajiquanMove.FA_JIN.id()) || faJinFollowups > 0) {
            if (faJinFollowups >= 2) return null;
            data.putInt(TAG_FA_JIN_FOLLOWUPS, faJinFollowups + 1);
            return BajiquanMove.PUNCH;
         }
         if (down) {
            if (last.equals(BajiquanMove.FLURRY.id())) return BajiquanMove.PALM;
            if (last.equals(BajiquanMove.CHARGED_TREMOR.id())) return BajiquanMove.DOUBLE_PALM;
            if (last.equals(BajiquanMove.PALM.id()) && data.getBoolean(TAG_PALM_FROM_FLURRY)) return null;
            if (last.equals(BajiquanMove.PUNCH.id()) || last.equals(BajiquanMove.CLAMP.id())) return BajiquanMove.SHOULDER;
            int count = cycle(data, TAG_DOWN_A_COUNT, 3);
            return count == 1 ? BajiquanMove.PALM : count == 2 ? BajiquanMove.SHOULDER : BajiquanMove.PUSH;
         }
         int pursuit = data.getInt(TAG_PURSUIT_STAGE);
         if (pursuit > 0) {
            long gap = now - data.getLong(TAG_LAST_MOVE_TICK);
            if (gap >= 4L && gap <= 10L && pursuit < 4) {
               data.putInt(TAG_PURSUIT_STAGE, pursuit + 1);
               return pursuit + 1 < 4 ? BajiquanMove.PUNCH : BajiquanMove.PUSH;
            }
            if (pursuit >= 4 || gap > 10L) data.putInt(TAG_PURSUIT_STAGE, 0);
         }
         if (last.equals(BajiquanMove.SHOULDER.id())) {
            long gap = now - data.getLong(TAG_LAST_MOVE_TICK);
            if (gap >= 4L && gap <= 10L) {
               data.putInt(TAG_PURSUIT_STAGE, 1);
               return BajiquanMove.PUNCH;
            }
            return BajiquanMove.FLURRY;
         }
         if (last.equals(BajiquanMove.RIGHT_KICK.id()) || last.equals(BajiquanMove.LEFT_KICK.id())) {
            data.putBoolean(TAG_PUNCH_FROM_KICK, true);
            return BajiquanMove.PUNCH;
         }
         if (data.getLong(TAG_KNEE_LANDING_UNTIL) >= now || last.equals(BajiquanMove.KNEE.id()) || last.equals(BajiquanMove.DOWN_KICK.id())) return BajiquanMove.FLURRY;
         if (last.equals(BajiquanMove.CHOP.id())) return BajiquanMove.ELBOW;
         int count = cycle(data, TAG_A_COUNT, 3);
         return count == 1 ? BajiquanMove.PUNCH : count == 2 ? BajiquanMove.ELBOW : BajiquanMove.FLURRY;
      }
      if (up || (!player.onGround() && data.getLong(TAG_KNEE_AIR_UNTIL) >= now)) return BajiquanMove.DOWN_KICK;
      if (down) {
         if (last.equals(BajiquanMove.PALM.id())) return vars.bajiquan_tiger_unlocked ? BajiquanMove.CHARGED_TREMOR : BajiquanMove.TREMOR;
         int count = cycle(data, TAG_DOWN_B_COUNT, 3);
         return count == 1 ? BajiquanMove.STOMP : count == 2 ? BajiquanMove.TREMOR : BajiquanMove.PARRY;
      }
      if (last.equals(BajiquanMove.FLURRY.id()) || last.equals(BajiquanMove.PALM.id()) || data.getBoolean(TAG_PUNCH_FROM_KICK)) {
         data.putBoolean(TAG_PUNCH_FROM_KICK, false);
         return BajiquanMove.FINISHER_KICK;
      }
      if (last.equals(BajiquanMove.PUNCH.id()) || last.equals(BajiquanMove.PUSH.id())) return BajiquanMove.RIGHT_KICK;
      int count = cycle(data, TAG_B_COUNT, 3);
      return count == 1 ? BajiquanMove.RIGHT_KICK : count == 2 ? BajiquanMove.LEFT_KICK : BajiquanMove.KNEE;
   }

   private static int cycle(CompoundTag data, String key, int max) {
      int next = data.getInt(key) % max + 1;
      data.putInt(key, next);
      return next;
   }

   private static void perform(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, BajiquanMove move, boolean comboCancel) {
      if (!isUnlocked(vars, move)) return;
      ServerLevel level = player.serverLevel();
      CompoundTag data = player.getPersistentData();
      long now = level.getGameTime();
      String previous = data.getString(TAG_LAST_MOVE);
      int recovery = Math.max(1, (int)Math.ceil(move.recoveryTicks() * (1.0 - BodyTrainingService.stagedPercent(vars.body_technique))));
      data.putLong(TAG_RECOVERY_UNTIL, now + recovery);
      data.putString(TAG_LAST_MOVE, move.id());
      data.putLong(TAG_LAST_MOVE_TICK, now);
      data.putLong(TAG_LAST_COMBAT, now);
      if (move == BajiquanMove.PALM) data.putBoolean(TAG_PALM_FROM_FLURRY, previous.equals(BajiquanMove.FLURRY.id()));
      if (move == BajiquanMove.FA_JIN) data.putInt(TAG_FA_JIN_FOLLOWUPS, 0);
      if (move == BajiquanMove.SHOULDER) data.putInt(TAG_PURSUIT_STAGE, 0);
      if (move == BajiquanMove.KNEE) {
         data.putLong(TAG_KNEE_AIR_UNTIL, now + 40L);
         data.putBoolean(TAG_KNEE_PENDING, true);
      }
      if (move == BajiquanMove.FLURRY) data.remove(TAG_KNEE_LANDING_UNTIL);

      if (move == BajiquanMove.PARRY) {
         data.putLong(TAG_PARRY_UNTIL, now + 6L);
      } else if (move == BajiquanMove.CLAMP) {
         data.putLong(TAG_CLAMP_UNTIL, now + 12L);
      } else if (move == BajiquanMove.HIGH_JUMP) {
         player.setDeltaMovement(player.getDeltaMovement().x, 1.05 + BodyTrainingService.stagedPercent(vars.body_technique) * 0.4, player.getDeltaMovement().z);
         player.hurtMarked = true;
      } else if (move == BajiquanMove.STOMP) {
         player.setDeltaMovement(player.getDeltaMovement().add(look(player).scale(0.55)));
         player.hurtMarked = true;
      } else if (move == BajiquanMove.TREMOR || move == BajiquanMove.CHARGED_TREMOR) {
         tremor(player, move == BajiquanMove.CHARGED_TREMOR ? 5.0 : 3.0, move == BajiquanMove.CHARGED_TREMOR ? 60 : 30, move.damage());
      } else if (move == BajiquanMove.FIERCE_TIGER) {
         LivingEntity target = findTarget(player, 8.0);
         if (target != null) {
            dashToward(player, target, 1.35);
            hit(player, target, move.damage(), 2.2, 0.25, 12);
         }
      } else {
         LivingEntity target = move == BajiquanMove.FA_JIN
            ? findStoredTarget(player, move.range(), true)
            : move == BajiquanMove.PUNCH && data.getInt(TAG_FA_JIN_FOLLOWUPS) > 0
               ? findStoredTarget(player, 8.0, false)
               : findTarget(player, move.range());
         if (target != null) {
            if (move == BajiquanMove.SHOULDER || move == BajiquanMove.KNEE || move == BajiquanMove.PALM || move == BajiquanMove.PUSH) {
               dashToward(player, target, move == BajiquanMove.SHOULDER ? 1.0 : 0.55);
            }
            if (move == BajiquanMove.FA_JIN || move == BajiquanMove.PUNCH && data.getInt(TAG_FA_JIN_FOLLOWUPS) > 0) dashToward(player, target, 0.75);
            float damage = move.damage();
            if (move == BajiquanMove.FA_JIN) damage = vars.bajiquan_proficiency >= 80.0 ? 14.0F : vars.bajiquan_proficiency >= 60.0 ? 11.0F : 8.0F;
            double knockback = switch (move) {
               case FINISHER_KICK -> 1.4;
               case DOUBLE_PALM -> 2.2;
               case PUSH -> 0.8;
               case RIGHT_KICK, LEFT_KICK, FA_JIN -> 0.35;
               default -> 0.12;
            };
            double lift = move == BajiquanMove.KNEE ? 0.38 : move == BajiquanMove.DOWN_KICK ? -0.1 : 0.08;
            int stagger = move == BajiquanMove.FLURRY ? 12 : move == BajiquanMove.FINISHER_KICK ? 10 : 6;
            if (move == BajiquanMove.FLURRY) {
               hit(player, target, 3.0F, knockback, lift, stagger, false);
               hit(player, target, 3.0F, knockback, lift, stagger, true);
            } else {
               hit(player, target, damage, knockback, lift, stagger, true);
            }
            if (move == BajiquanMove.KNEE) {
               player.setDeltaMovement(player.getDeltaMovement().add(0.0, 0.28, 0.0));
               player.hurtMarked = true;
            }
            if (move == BajiquanMove.FA_JIN) data.putString(TAG_OFF_BALANCE_TARGET, target.getUUID().toString());
         }
      }
      if (move == BajiquanMove.DOUBLE_PALM) {
         player.addEffect(new MobEffectInstance(ModMobEffects.STAGGER, 12, 0, false, false, true));
      }
      spawnMoveFx(level, player, move);
      PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new BajiquanPoseMessage(player.getUUID(), move, Math.min(20, move.recoveryTicks())), new net.minecraft.network.protocol.common.custom.CustomPacketPayload[0]);
   }

   private static void hit(ServerPlayer player, LivingEntity target, float damage, double knockback, double lift, int stagger) {
      hit(player, target, damage, knockback, lift, stagger, true);
   }

   private static void hit(ServerPlayer player, LivingEntity target, float damage, double knockback, double lift, int stagger, boolean awardProficiency) {
      if (!canHit(player, target)) return;
      breakCircleRealm(player);
      target.invulnerableTime = 0;
      player.getPersistentData().putBoolean(TAG_MARTIAL_DAMAGE, true);
      boolean hit;
      try {
         hit = target.hurt(player.damageSources().playerAttack(player), damage);
      } finally {
         player.getPersistentData().remove(TAG_MARTIAL_DAMAGE);
      }
      target.invulnerableTime = 0;
      if (!hit) return;
      if (stagger > 0) target.addEffect(new MobEffectInstance(ModMobEffects.STAGGER, stagger, 0, false, true, true));
      Vec3 direction = target.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
      if (direction.lengthSqr() < 1.0E-4) direction = look(player);
      direction = direction.normalize();
      target.push(direction.x * knockback, lift, direction.z * knockback);
      target.hurtMarked = true;
      if (awardProficiency) addProficiency(player, isSparring(player) ? 0.25 : 0.05);
   }

   private static void tremor(ServerPlayer player, double radius, int imbalanceTicks, float damage) {
      AABB area = player.getBoundingBox().inflate(radius, 1.5, radius);
      boolean landed = false;
      for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, area, e -> canHit(player, e))) {
         target.addEffect(new MobEffectInstance(ModMobEffects.OFF_BALANCE, imbalanceTicks, 0, false, true, true));
         if (damage > 0.0F) {
            target.invulnerableTime = 0;
            player.getPersistentData().putBoolean(TAG_MARTIAL_DAMAGE, true);
            try {
               landed |= target.hurt(player.damageSources().playerAttack(player), damage);
            } finally {
               player.getPersistentData().remove(TAG_MARTIAL_DAMAGE);
            }
            target.invulnerableTime = 0;
         }
      }
      if (landed) addProficiency(player, isSparring(player) ? 0.25 : 0.05);
   }

   public static void tickPlayer(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      BodyTrainingService.applyAttributes(player, vars);
      CompoundTag data = player.getPersistentData();
      long now = player.level().getGameTime();
      boolean grounded = player.onGround();
      boolean wasGrounded = data.getBoolean(TAG_LAST_GROUNDED);
      if (!grounded && player.fallDistance > 2.0F) data.putLong(TAG_UKEMI_UNTIL, now + 4L);
      if (grounded && !wasGrounded && data.getLong(TAG_UKEMI_UNTIL) >= now - 1L) data.putLong(TAG_UKEMI_UNTIL, now + 4L);
      if (grounded && !wasGrounded && data.getBoolean(TAG_KNEE_PENDING)) {
         data.putBoolean(TAG_KNEE_PENDING, false);
         data.remove(TAG_KNEE_AIR_UNTIL);
         data.putLong(TAG_KNEE_LANDING_UNTIL, now + COMBO_WINDOW);
      }
      data.putBoolean(TAG_LAST_GROUNDED, grounded);

      long circleUntil = data.getLong(TAG_CIRCLE_UNTIL);
      if (circleUntil > now) {
         clearAggro(player);
         if (now % 40L == 0L) syncCircleRealm(player, true);
         if (!player.hasEffect(MobEffects.INVISIBILITY)) player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, (int)(circleUntil - now), 0, false, false, false));
      } else if (circleUntil != 0L) {
         breakCircleRealm(player);
      }

      if (!isActive(player) || vars.bajiquan_proficiency < 50.0 || player.getHealth() >= player.getMaxHealth() || now - data.getLong(TAG_LAST_COMBAT) < 100L) return;
      int interval = vars.bajiquan_proficiency >= 80.0 ? 60 : vars.bajiquan_proficiency >= 60.0 ? 100 : 160;
      if (now - data.getLong(TAG_LAST_QI_HEAL) >= interval) {
         data.putLong(TAG_LAST_QI_HEAL, now);
         player.heal(1.0F);
      }
   }

   public static boolean tryDefend(ServerPlayer player, LivingEntity attacker, float[] damage) {
      if (!isActive(player) || attacker == null) return false;
      CompoundTag data = player.getPersistentData();
      long now = player.level().getGameTime();
      if (data.getLong(TAG_CLAMP_UNTIL) >= now) {
         attacker.addEffect(new MobEffectInstance(ModMobEffects.STAGGER, 12, 0, false, true, true));
         damage[0] = 0.0F;
         addProficiency(player, isSparring(player) ? 0.25 : 0.05);
         return true;
      }
      if (Math.max(data.getLong(TAG_PARRY_UNTIL), data.getLong(TAG_REACTIVE_PARRY_UNTIL)) < now
         || player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).bajiquan_proficiency < 30.0) return false;
      double p = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).bajiquan_proficiency;
      boolean perfect = p >= 50.0 || p >= 40.0 && player.getRandom().nextBoolean();
      damage[0] = perfect ? 0.0F : damage[0] * 0.25F;
      attacker.addEffect(new MobEffectInstance(ModMobEffects.OFF_BALANCE, 25, 0, false, true, true));
      data.putString(TAG_OFF_BALANCE_TARGET, attacker.getUUID().toString());
      addProficiency(player, isSparring(player) ? 0.25 : 0.05);
      return perfect;
   }

   public static boolean tryUkemi(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      long now = player.level().getGameTime();
      if (!isUnlocked(vars, BajiquanMove.UKEMI) || player.getPersistentData().getLong(TAG_UKEMI_UNTIL) < now) return false;
      Vec3 dir = look(player);
      player.setDeltaMovement(dir.x * 0.9, Math.max(0.12, player.getDeltaMovement().y), dir.z * 0.9);
      player.fallDistance *= 0.3F;
      player.getPersistentData().putLong(TAG_UKEMI_UNTIL, 0L);
      player.getPersistentData().putLong(TAG_UKEMI_REDUCTION_UNTIL, now + 4L);
      player.hurtMarked = true;
      return true;
   }

   public static void activateCircleRealm(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      long now = player.level().getGameTime();
      if (!isActive(player) || !isUnlocked(vars, BajiquanMove.CIRCLE_REALM) || vars.bajiquan_circle_realm_cooldown_until > now) return;
      vars.bajiquan_circle_realm_cooldown_until = now + 2400L;
      player.getPersistentData().putLong(TAG_CIRCLE_UNTIL, now + 600L);
      player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 600, 0, false, false, false));
      clearAggro(player);
      syncCircleRealm(player, true);
      vars.syncPlayerVariables(player);
   }

   public static void breakCircleRealm(ServerPlayer player) {
      if (player.getPersistentData().getLong(TAG_CIRCLE_UNTIL) == 0L) return;
      player.getPersistentData().remove(TAG_CIRCLE_UNTIL);
      player.removeEffect(MobEffects.INVISIBILITY);
      syncCircleRealm(player, false);
   }

   private static void syncCircleRealm(ServerPlayer player, boolean active) {
      PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new CircleRealmStateMessage(player.getUUID(), active), new net.minecraft.network.protocol.common.custom.CustomPacketPayload[0]);
   }

   public static boolean canDetectCircleRealm(LivingEntity observer) {
      if (observer instanceof LiShuwenEntity) return true;
      if (observer instanceof ServerPlayer player) {
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         return vars.servant_card_transformed && "li_shuwen".equals(vars.servant_card_id);
      }
      return false;
   }

   public static void addProficiency(ServerPlayer player, double amount) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      double before = vars.bajiquan_proficiency;
      vars.bajiquan_proficiency = Mth.clamp(Math.round((before + amount) * 100.0) / 100.0, 0.0, 100.0);
      notifyUnlocks(player, before, vars.bajiquan_proficiency);
      if (before < 40.0 && vars.bajiquan_proficiency >= 40.0) TypeMoonAdvancementHelper.grant(player, TypeMoonAdvancementHelper.BAJIQUAN_GRADUATE);
      if (before < 100.0 && vars.bajiquan_proficiency >= 100.0) TypeMoonAdvancementHelper.grant(player, TypeMoonAdvancementHelper.BAJIQUAN_CIRCLE_REALM);
      if ((int)(before * 20.0) != (int)(vars.bajiquan_proficiency * 20.0)) vars.syncPlayerVariables(player);
   }

   private static void notifyUnlocks(ServerPlayer player, double before, double after) {
      double[] thresholds = {0.5, 1.0, 5.0, 10.0, 25.0, 30.0, 40.0, 50.0, 60.0, 80.0, 100.0};
      String[] keys = {"05", "10", "50", "100", "250", "300", "400", "500", "600", "800", "1000"};
      for (int i = 0; i < thresholds.length; i++) {
         if (before < thresholds[i] && after >= thresholds[i]) {
            player.displayClientMessage(
               Component.translatable(
                  "message.typemoonworld.bajiquan.moves_unlocked",
                  Component.translatable("message.typemoonworld.bajiquan.unlock." + keys[i])
               ), false
            );
         }
      }
   }

   public static boolean isSparring(ServerPlayer player) {
      return player.getPersistentData().getBoolean("TypeMoonBajiquanSparring");
   }

   public static boolean isMartialDamage(ServerPlayer player) {
      return player != null && player.getPersistentData().getBoolean(TAG_MARTIAL_DAMAGE);
   }

   public static boolean consumeUkemiReduction(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      if (data.getLong(TAG_UKEMI_REDUCTION_UNTIL) < player.level().getGameTime()) return false;
      data.remove(TAG_UKEMI_REDUCTION_UNTIL);
      return true;
   }

   private static BajiquanMove moveById(String id) {
      if (id == null || id.isEmpty()) return null;
      for (BajiquanMove move : BajiquanMove.values()) if (move.id().equals(id)) return move;
      return null;
   }

   private static LivingEntity findTarget(ServerPlayer player, double range) {
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle().normalize();
      return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().expandTowards(look.scale(range)).inflate(2.0), e -> canHit(player, e))
         .stream().filter(e -> {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            return to.lengthSqr() <= range * range && to.normalize().dot(look) > 0.72 && player.hasLineOfSight(e);
         }).min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
   }

   private static LivingEntity findStoredTarget(ServerPlayer player, double range, boolean requireOffBalance) {
      String rawId = player.getPersistentData().getString(TAG_OFF_BALANCE_TARGET);
      if (rawId.isEmpty()) return null;
      try {
         if (!(player.serverLevel().getEntity(java.util.UUID.fromString(rawId)) instanceof LivingEntity target)) return null;
         if (!canHit(player, target) || player.distanceToSqr(target) > range * range || !player.hasLineOfSight(target)) return null;
         if (requireOffBalance && !target.hasEffect(ModMobEffects.OFF_BALANCE)) return null;
         return target;
      } catch (IllegalArgumentException ignored) {
         return null;
      }
   }

   private static boolean canHit(ServerPlayer player, LivingEntity target) {
      return target != player && target.isAlive() && !target.isAlliedTo(player) && !EntityUtils.isImmunePlayerTarget(target);
   }

   private static void dashToward(ServerPlayer player, LivingEntity target, double speed) {
      Vec3 dir = target.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
      if (dir.lengthSqr() > 1.0E-4) {
         dir = dir.normalize();
         player.setDeltaMovement(player.getDeltaMovement().add(dir.x * speed, 0.08, dir.z * speed));
         player.hurtMarked = true;
      }
   }

   private static Vec3 look(ServerPlayer player) {
      Vec3 dir = player.getLookAngle().multiply(1.0, 0.0, 1.0);
      return dir.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : dir.normalize();
   }

   private static void clearAggro(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return;
      for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(48.0), m -> m.getTarget() == player && !canDetectCircleRealm(m))) {
         mob.setTarget(null);
         mob.getNavigation().stop();
      }
   }

   private static void spawnMoveFx(ServerLevel level, ServerPlayer player, BajiquanMove move) {
      Vec3 pos = player.position().add(look(player).scale(1.2)).add(0.0, 0.9, 0.0);
      level.sendParticles(move == BajiquanMove.TREMOR || move == BajiquanMove.CHARGED_TREMOR ? ParticleTypes.CLOUD : ParticleTypes.SWEEP_ATTACK,
         pos.x, pos.y, pos.z, move == BajiquanMove.TREMOR || move == BajiquanMove.CHARGED_TREMOR ? 18 : 1, 0.45, 0.18, 0.45, 0.03);
      level.playSound(null, player.blockPosition(), move.damage() >= 10.0F ? SoundEvents.PLAYER_ATTACK_CRIT : SoundEvents.PLAYER_ATTACK_STRONG,
         SoundSource.PLAYERS, 0.75F, move.damage() >= 10.0F ? 0.65F : 0.9F);
   }

   private static void appendHistory(CompoundTag data, String token, long now) {
      String history = now - data.getLong(TAG_HISTORY_TICK) <= COMBO_WINDOW ? data.getString(TAG_HISTORY) : "";
      history = history.isEmpty() ? token : history + "," + token;
      String[] parts = history.split(",");
      if (parts.length > 6) history = String.join(",", List.of(parts).subList(parts.length - 6, parts.length));
      data.putString(TAG_HISTORY, history);
      data.putLong(TAG_HISTORY_TICK, now);
   }

   private static boolean historyEnds(CompoundTag data, String sequence) {
      return data.getString(TAG_HISTORY).endsWith(sequence);
   }

   private static void resetCounters(CompoundTag data) {
      data.putInt(TAG_A_COUNT, 0);
      data.putInt(TAG_DOWN_A_COUNT, 0);
      data.putInt(TAG_B_COUNT, 0);
      data.putInt(TAG_DOWN_B_COUNT, 0);
      data.putInt(TAG_SHOULDER_PURSUIT, 0);
      data.putInt(TAG_PURSUIT_STAGE, 0);
      data.putInt(TAG_FA_JIN_FOLLOWUPS, 0);
      data.putBoolean(TAG_PALM_FROM_FLURRY, false);
      data.putBoolean(TAG_PUNCH_FROM_KICK, false);
      data.remove(TAG_LAST_MOVE);
      data.remove(TAG_OFF_BALANCE_TARGET);
   }

   private static CompoundTag saveComboState(CompoundTag data) {
      CompoundTag snapshot = new CompoundTag();
      for (String key : COMBO_STATE_KEYS) {
         Tag value = data.get(key);
         if (value != null) snapshot.put(key, value.copy());
      }
      return snapshot;
   }

   private static void restoreComboState(CompoundTag data, CompoundTag snapshot) {
      for (String key : COMBO_STATE_KEYS) {
         data.remove(key);
         Tag value = snapshot.get(key);
         if (value != null) data.put(key, value.copy());
      }
   }
}
