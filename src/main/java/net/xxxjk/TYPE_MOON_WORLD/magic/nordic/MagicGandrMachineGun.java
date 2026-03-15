package net.xxxjk.TYPE_MOON_WORLD.magic.nordic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.GanderProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.joml.Vector3f;

public final class MagicGandrMachineGun {
   private static final int MODE_RAPID_BURST = 0;
   private static final int MODE_GATE_BARRAGE = 1;
   private static final int BURST_COUNT = 3;
   private static final int CHANT_TICKS = 20;
   private static final int BURST_INTERVAL_TICKS = 2;
   private static final int BARRAGE_INTERVAL_TICKS = 5;
   private static final int BARRAGE_INTERVAL_JITTER_TICKS = 2;
   private static final double MANA_COST_PER_SHOT = 20.0;
   private static final float PROJECTILE_SCALE_MIN = 0.207F;
   private static final float PROJECTILE_SCALE_MAX = 0.37F;
   private static final double RIGHT_HAND_FORWARD_OFFSET = 0.08;
   private static final double RANDOM_SIDE_JITTER = 0.06;
   private static final double RANDOM_UP_JITTER = 0.05;
   private static final double RANDOM_FORWARD_JITTER = 0.03;
   private static final float RANDOM_YAW_JITTER = 0.45F;
   private static final float RANDOM_PITCH_JITTER = 0.35F;
   private static final int BARRAGE_MIN_SHOTS = 12;
   private static final int BARRAGE_MAX_SHOTS = 15;
   private static final int BARRAGE_COLUMNS = 8;
   private static final double BARRAGE_BACK_OFFSET = 2.2;
   private static final double BARRAGE_UP_OFFSET = 0.72;
   private static final double BARRAGE_ROW_SPACING = 0.44;
   private static final double BARRAGE_COL_SPACING = 0.52;
   private static final double BARRAGE_SIDE_JITTER = 0.2;
   private static final double BARRAGE_HEIGHT_JITTER = 0.24;
   private static final double BARRAGE_DEPTH_JITTER = 0.58;
   private static final double BARRAGE_DEPTH_LAYER_SPREAD = 0.42;
   private static final double BARRAGE_AIM_DISTANCE = 36.0;
   private static final double BARRAGE_AIM_JITTER = 0.85;
   private static final float BARRAGE_PROJECTILE_SPEED_MIN = 3.4F;
   private static final float BARRAGE_PROJECTILE_SPEED_MAX = 4.2F;
   private static final float BARRAGE_PROJECTILE_INACCURACY_MIN = 0.05F;
   private static final float BARRAGE_PROJECTILE_INACCURACY_MAX = 0.17F;
   private static final double CHARGE_ANCHOR_FORWARD_FROM_HAND = 0.54;
   private static final double CHARGE_ANCHOR_UP_FROM_HAND = 0.1;
   private static final DustParticleOptions BLACK_DUST = new DustParticleOptions(new Vector3f(0.05F, 0.05F, 0.05F), 1.0F);
   private static final DustParticleOptions RED_DUST = new DustParticleOptions(new Vector3f(0.95F, 0.08F, 0.12F), 1.1F);
   private static final String TAG_ACTIVE = "TypeMoonGandrMachineGunActive";
   private static final String TAG_CHANTING = "TypeMoonGandrMachineGunChanting";
   private static final String TAG_CHANT_END_TICK = "TypeMoonGandrMachineGunChantEndTick";
   private static final String TAG_NEXT_BURST_TICK = "TypeMoonGandrMachineGunNextBurstTick";
   private static final String TAG_MODE = "TypeMoonGandrMachineGunMode";
   private static final String TAG_CHARGE_PREVIEW_UUIDS = "TypeMoonGandrMachineGunChargePreviewUUIDs";
   private static final String TAG_CHARGE_PREVIEW_UUID = "TypeMoonGandrMachineGunChargePreviewUUID";
   private static final String TAG_LAST_MANA_SYNC_TICK = "TypeMoonGandrMachineGunLastManaSyncTick";
   private static final String TAG_LAST_PROF_SYNC_TICK = "TypeMoonGandrMachineGunLastProfSyncTick";
   private static final int MANA_SYNC_INTERVAL_TICKS = 4;
   private static final int PROF_SYNC_INTERVAL_TICKS = 20;
   private static final float[] SHOT_YAW_OFFSETS = new float[]{0.0F, -1.1F, 1.1F};
   private static final float[] SHOT_PITCH_OFFSETS = new float[]{0.0F, -0.7F, 0.7F};
   private static final double[] SHOT_SIDE_OFFSETS = new double[]{0.0, -0.12, 0.12};
   private static final double[] SHOT_UP_OFFSETS = new double[]{0.0, 0.06, -0.06};

   private MagicGandrMachineGun() {
   }

   public static boolean execute(Entity entity) {
      if (!(entity instanceof ServerPlayer player)) {
         return false;
      } else {
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         boolean crestCast = vars.isCurrentSelectionFromCrest("gandr_machine_gun");
         if (!crestCast && !vars.learned_magics.contains("gandr_machine_gun")) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.not_learned"), true);
            return false;
         } else if (!crestCast && !vars.learned_magics.contains("gander")) {
            player.displayClientMessage(
               Component.translatable(
                  "message.typemoonworld.scroll.requirement_not_met", Component.translatable("magic.typemoonworld.gander.name")
               ),
               true
            );
            return false;
         } else {
            CompoundTag tag = player.getPersistentData();
            if (tag.getBoolean(TAG_ACTIVE) || tag.getBoolean(TAG_CHANTING)) {
               clearState(player);
               return true;
            } else if (!EntityUtils.hasAnyEmptyHand(player)) {
               player.displayClientMessage(Component.translatable("message.typemoonworld.magic.need_empty_hand"), true);
               return false;
            } else {
               int mode = clampMode(vars.gandr_machine_gun_mode);
               double effectiveProficiency = getEffectiveGanderProficiency(vars);
               if (!hasEnoughManaForMode(vars, mode, effectiveProficiency)) {
                  player.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), true);
                  return false;
               } else {
                  long now = player.level().getGameTime();
                  if (crestCast) {
                     tag.putBoolean(TAG_CHANTING, false);
                     tag.putBoolean(TAG_ACTIVE, true);
                     tag.putLong(TAG_CHANT_END_TICK, now);
                     tag.putLong(TAG_NEXT_BURST_TICK, now);
                  } else {
                     tag.putBoolean(TAG_CHANTING, true);
                     tag.putBoolean(TAG_ACTIVE, false);
                     tag.putLong(TAG_CHANT_END_TICK, now + CHANT_TICKS);
                     tag.putLong(TAG_NEXT_BURST_TICK, now + CHANT_TICKS);
                  }

                  tag.putInt(TAG_MODE, mode);
                  if (!crestCast && mode == MODE_GATE_BARRAGE) {
                     player.displayClientMessage(getGateBarrageChantComponent(), true);
                  } else {
                     player.displayClientMessage(Component.translatable("message.typemoonworld.magic.gandr_machine_gun.start"), true);
                  }

                  return true;
               }
            }
         }
      }
   }

   public static void tick(ServerPlayer player) {
      if (!player.level().isClientSide()) {
         CompoundTag tag = player.getPersistentData();
         boolean chanting = tag.getBoolean(TAG_CHANTING);
         boolean active = tag.getBoolean(TAG_ACTIVE);
         if (chanting || active) {
            TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(
               TypeMoonWorldModVariables.PLAYER_VARIABLES
            );
            if (!canMaintain(player, vars)) {
               clearState(player);
            } else {
               boolean crestCast = vars.isCurrentSelectionFromCrest("gandr_machine_gun");
               double effectiveProficiency = getEffectiveGanderProficiency(vars);
               int mode = clampMode(tag.getInt(TAG_MODE));
               long now = player.level().getGameTime();
               if (chanting) {
                  long chantEndTick = tag.getLong(TAG_CHANT_END_TICK);
                  if (now < chantEndTick) {
                     double progress = 1.0 - (chantEndTick - now) / (double)CHANT_TICKS;
                     discardChargingPreview(player);
                     spawnChantParticles(player, progress, mode, effectiveProficiency);
                     return;
                  }

                  discardChargingPreview(player);
                  tag.putBoolean(TAG_CHANTING, false);
                  tag.putBoolean(TAG_ACTIVE, true);
                  tag.putLong(TAG_NEXT_BURST_TICK, now);
               }

               if (tag.getBoolean(TAG_ACTIVE)) {
                  long nextBurstTick = tag.getLong(TAG_NEXT_BURST_TICK);
                  if (now >= nextBurstTick) {
                     if (mode == MODE_GATE_BARRAGE) {
                        boolean fired = fireGateBarrage(player, vars, effectiveProficiency);
                        if (!fired) {
                           clearState(player);
                        } else {
                           vars.magic_cooldown = BARRAGE_INTERVAL_TICKS;
                           if (!crestCast) {
                              vars.proficiency_gander = Math.min(100.0, vars.proficiency_gander + getBarrageShotCount(effectiveProficiency) * 0.02);
                           }

                           syncBurstState(player, vars, now, !crestCast);
                            int jitter = player.getRandom().nextInt(BARRAGE_INTERVAL_JITTER_TICKS * 2 + 1) - BARRAGE_INTERVAL_JITTER_TICKS;
                            int nextInterval = Math.max(BURST_INTERVAL_TICKS, BARRAGE_INTERVAL_TICKS + jitter);
                            tag.putLong(TAG_NEXT_BURST_TICK, now + nextInterval);
                         }
                      } else {
                        boolean fired = fireBurst(player, vars, effectiveProficiency);
                        if (!fired) {
                           clearState(player);
                        } else {
                           vars.magic_cooldown = BURST_INTERVAL_TICKS;
                           if (!crestCast) {
                              vars.proficiency_gander = Math.min(100.0, vars.proficiency_gander + 0.05);
                           }

                           syncBurstState(player, vars, now, !crestCast);
                            tag.putLong(TAG_NEXT_BURST_TICK, now + BURST_INTERVAL_TICKS);
                         }
                      }
                  }
               }
            }
         }
      }
   }

   private static boolean canMaintain(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!player.isAlive() || player.isSpectator()) {
         return false;
      } else if (!vars.is_magus || !vars.is_magic_circuit_open) {
         return false;
      } else if (!vars.learned_magics.contains("gandr_machine_gun") && !vars.isCurrentSelectionFromCrest("gandr_machine_gun")) {
         return false;
      } else if (!EntityUtils.hasAnyEmptyHand(player)) {
         return false;
      } else if (vars.selected_magics.isEmpty()) {
         return false;
      } else {
         int index = vars.current_magic_index;
         return index >= 0 && index < vars.selected_magics.size() ? "gandr_machine_gun".equals(vars.selected_magics.get(index)) : false;
      }
   }

   private static boolean fireBurst(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, double effectiveProficiency) {
      if (!hasEnoughManaForRapidBurst(vars)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), true);
         return false;
      } else {
         int chargeSeconds = getEquivalentChargeSeconds(effectiveProficiency);
         vars.player_mana = Math.max(0.0, vars.player_mana - getRapidBurstManaCost());
         Level level = player.level();

         for (int i = 0; i < BURST_COUNT; i++) {
            shootRapidBullet(level, player, i, chargeSeconds);
         }

         level.playSound(
            null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.5F, 1.15F + level.random.nextFloat() * 0.2F
         );
         return true;
      }
   }

   private static void shootRapidBullet(Level level, ServerPlayer player, int shotIndex, int chargeSeconds) {
      int pattern = Math.floorMod(shotIndex, 3);
      GanderProjectileEntity projectile = new GanderProjectileEntity(level, player);
      projectile.setNoGravity(true);
      projectile.setChargeSeconds(chargeSeconds);
      projectile.setVisualScale(getProjectileScaleForCharge(chargeSeconds));
      projectile.setItem(new ItemStack(ModItems.GANDER.get()));
      Vec3 forward = EntityUtils.getAutoAimDirection(player, 48.0, 58.0);
      Vec3 right = forward.cross(new Vec3(0.0, 1.0, 0.0));
      if (right.lengthSqr() < 1.0E-6) {
         right = new Vec3(1.0, 0.0, 0.0);
      } else {
         right = right.normalize();
      }

      Vec3 up = right.cross(forward).normalize();
      Vec3 handAnchor = EntityUtils.getCurrentEmptyHandCastAnchor(player);
      double randomSide = (level.random.nextDouble() * 2.0 - 1.0) * RANDOM_SIDE_JITTER;
      double randomUp = (level.random.nextDouble() * 2.0 - 1.0) * RANDOM_UP_JITTER;
      double randomForward = level.random.nextDouble() * RANDOM_FORWARD_JITTER;
      Vec3 spawnPos = handAnchor.add(forward.scale(RIGHT_HAND_FORWARD_OFFSET + randomForward))
         .add(right.scale(SHOT_SIDE_OFFSETS[pattern]))
         .add(up.scale(SHOT_UP_OFFSETS[pattern]))
         .add(right.scale(randomSide))
         .add(up.scale(randomUp));
      projectile.setPos(spawnPos);
      float yawOffset = SHOT_YAW_OFFSETS[pattern] + (float)level.random.nextGaussian() * RANDOM_YAW_JITTER;
      float pitchOffset = SHOT_PITCH_OFFSETS[pattern] + (float)level.random.nextGaussian() * RANDOM_PITCH_JITTER;
      projectile.shootFromRotation(player, player.getXRot() + pitchOffset, player.getYRot() + yawOffset, 0.0F, 3.8F, 0.1F);
      level.addFreshEntity(projectile);
      spawnShotParticles(level, spawnPos, forward);
   }

   private static boolean fireGateBarrage(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, double effectiveProficiency) {
      int shotCount = getBarrageShotCount(effectiveProficiency);
      double manaCost = shotCount * MANA_COST_PER_SHOT;
      if (vars.player_mana < manaCost) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), true);
         return false;
      } else {
         vars.player_mana = Math.max(0.0, vars.player_mana - manaCost);
         Level level = player.level();
         int chargeSeconds = getEquivalentChargeSeconds(effectiveProficiency);
         Vec3 forward = player.getLookAngle().normalize();
         Vec3 right = forward.cross(new Vec3(0.0, 1.0, 0.0));
         if (right.lengthSqr() < 1.0E-6) {
            right = new Vec3(1.0, 0.0, 0.0);
         } else {
            right = right.normalize();
         }

         Vec3 up = right.cross(forward).normalize();
         Vec3 center = player.getEyePosition().add(forward.scale(-BARRAGE_BACK_OFFSET)).add(up.scale(BARRAGE_UP_OFFSET));
         LivingEntity lockTarget = EntityUtils.findAutoAimTarget(player, 52.0, 75.0);
         Vec3 targetCenter = lockTarget != null ? lockTarget.getEyePosition() : player.getEyePosition().add(forward.scale(BARRAGE_AIM_DISTANCE));
         int columns = Math.min(BARRAGE_COLUMNS + level.random.nextInt(3), Math.max(1, shotCount));
         columns = Math.max(5, columns);
         int rows = (int)Math.ceil((double)shotCount / columns);
         int[] order = buildShuffledOrder(shotCount, level);

         for (int i = 0; i < shotCount; i++) {
            int slot = order[i];
            int row = slot / columns;
            int col = slot % columns;
            double x = (col - (columns - 1) * 0.5) * BARRAGE_COL_SPACING + (level.random.nextDouble() - 0.5) * BARRAGE_SIDE_JITTER;
            double y = ((rows - 1) * 0.5 - row) * BARRAGE_ROW_SPACING + (level.random.nextDouble() - 0.5) * BARRAGE_HEIGHT_JITTER;
            double rowDepthOffset = ((rows - 1) * 0.5 - row) * BARRAGE_DEPTH_LAYER_SPREAD;
            Vec3 spawnPos = center.add(right.scale(x)).add(up.scale(y)).add(forward.scale(rowDepthOffset + (level.random.nextDouble() - 0.5) * BARRAGE_DEPTH_JITTER));
            Vec3 jitter = right.scale((level.random.nextDouble() - 0.5) * BARRAGE_AIM_JITTER)
               .add(up.scale((level.random.nextDouble() - 0.5) * BARRAGE_AIM_JITTER * 0.65));
            Vec3 direction = targetCenter.add(jitter).subtract(spawnPos).normalize();
            float speed = BARRAGE_PROJECTILE_SPEED_MIN + level.random.nextFloat() * (BARRAGE_PROJECTILE_SPEED_MAX - BARRAGE_PROJECTILE_SPEED_MIN);
            float inaccuracy = BARRAGE_PROJECTILE_INACCURACY_MIN
               + level.random.nextFloat() * (BARRAGE_PROJECTILE_INACCURACY_MAX - BARRAGE_PROJECTILE_INACCURACY_MIN);
            shootBarrageBullet(level, player, spawnPos, direction, chargeSeconds, speed, inaccuracy);
         }

         level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.PLAYERS, 0.65F, 1.25F);
         return true;
      }
   }

   private static void shootBarrageBullet(Level level, ServerPlayer player, Vec3 spawnPos, Vec3 direction, int chargeSeconds, float speed, float inaccuracy) {
      GanderProjectileEntity projectile = new GanderProjectileEntity(level, player);
      projectile.setNoGravity(true);
      projectile.setChargeSeconds(chargeSeconds);
      projectile.setVisualScale(getProjectileScaleForCharge(chargeSeconds));
      projectile.setItem(new ItemStack(ModItems.GANDER.get()));
      projectile.setPos(spawnPos);
      projectile.shoot(direction.x, direction.y, direction.z, speed, inaccuracy);
      level.addFreshEntity(projectile);
      spawnShotParticles(level, spawnPos, direction);
   }

   private static int[] buildShuffledOrder(int size, Level level) {
      int[] order = new int[size];
      int i = 0;

      while (i < size) {
         order[i] = i++;
      }

      for (int ix = size - 1; ix > 0; ix--) {
         int j = level.random.nextInt(ix + 1);
         int tmp = order[ix];
         order[ix] = order[j];
         order[j] = tmp;
      }

      return order;
   }

   private static void spawnShotParticles(Level level, Vec3 spawnPos, Vec3 direction) {
      if (level instanceof ServerLevel serverLevel) {
         Vec3 dir = direction.normalize();
         Vec3 right = dir.cross(new Vec3(0.0, 1.0, 0.0));
         if (right.lengthSqr() < 1.0E-6) {
            right = new Vec3(1.0, 0.0, 0.0);
         } else {
            right = right.normalize();
         }

         Vec3 up = right.cross(dir).normalize();
         Vec3 mouth = spawnPos.add(dir.scale(0.04));
         serverLevel.sendParticles(BLACK_DUST, mouth.x, mouth.y, mouth.z, 5, 0.05, 0.05, 0.05, 0.02);
         serverLevel.sendParticles(RED_DUST, mouth.x, mouth.y, mouth.z, 4, 0.04, 0.04, 0.04, 0.02);

         for (int i = 0; i < 2; i++) {
            double side = (serverLevel.random.nextDouble() - 0.5) * 0.08;
            double vertical = (serverLevel.random.nextDouble() - 0.5) * 0.08;
            Vec3 start = mouth.add(right.scale(side)).add(up.scale(vertical));
            Vec3 velBlack = dir.scale(0.16 + serverLevel.random.nextDouble() * 0.06);
            Vec3 velRed = dir.scale(0.2 + serverLevel.random.nextDouble() * 0.06);
            serverLevel.sendParticles(BLACK_DUST, start.x, start.y, start.z, 0, velBlack.x, velBlack.y, velBlack.z, 1.0);
            serverLevel.sendParticles(RED_DUST, start.x, start.y, start.z, 0, velRed.x, velRed.y, velRed.z, 1.0);
         }
      }
   }

   private static boolean hasEnoughManaForMode(TypeMoonWorldModVariables.PlayerVariables vars, int mode, double effectiveProficiency) {
      return mode == 1 ? vars.player_mana >= getBarrageManaCost(effectiveProficiency) : hasEnoughManaForRapidBurst(vars);
   }

   private static boolean hasEnoughManaForRapidBurst(TypeMoonWorldModVariables.PlayerVariables vars) {
      return vars.player_mana >= getRapidBurstManaCost();
   }

   private static double getRapidBurstManaCost() {
      return BURST_COUNT * MANA_COST_PER_SHOT;
   }

   private static double getBarrageManaCost(double proficiency) {
      return getBarrageShotCount(proficiency) * MANA_COST_PER_SHOT;
   }

   private static int getBarrageShotCount(double proficiency) {
      double t = Math.max(0.0, Math.min(100.0, proficiency)) / 100.0;
      return (int)Math.round(BARRAGE_MIN_SHOTS + (BARRAGE_MAX_SHOTS - BARRAGE_MIN_SHOTS) * t);
   }

   private static int getEquivalentChargeSeconds(double proficiency) {
      double p = Math.max(0.0, Math.min(100.0, proficiency));
      return 1 + (int)Math.floor(p / 25.0);
   }

   private static double getEffectiveGanderProficiency(TypeMoonWorldModVariables.PlayerVariables vars) {
      return vars.isCurrentSelectionFromCrest("gandr_machine_gun") ? 100.0 : vars.proficiency_gander;
   }

   private static float getProjectileScaleForCharge(int chargeSeconds) {
      int clamped = Math.max(1, Math.min(5, chargeSeconds));
      double ratio = (clamped - 1) / 4.0;
      return (float)(PROJECTILE_SCALE_MIN + (PROJECTILE_SCALE_MAX - PROJECTILE_SCALE_MIN) * ratio);
   }

   private static int clampMode(int mode) {
      return mode == MODE_GATE_BARRAGE ? MODE_GATE_BARRAGE : MODE_RAPID_BURST;
   }

   private static void clearState(ServerPlayer player) {
      discardChargingPreview(player);
      CompoundTag tag = player.getPersistentData();
      tag.putBoolean(TAG_ACTIVE, false);
      tag.putBoolean(TAG_CHANTING, false);
      tag.putLong(TAG_CHANT_END_TICK, 0L);
      tag.putLong(TAG_NEXT_BURST_TICK, 0L);
      tag.putInt(TAG_MODE, MODE_RAPID_BURST);
      tag.remove(TAG_LAST_MANA_SYNC_TICK);
      tag.remove(TAG_LAST_PROF_SYNC_TICK);
   }

   private static void syncBurstState(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, long now, boolean syncProficiency) {
      CompoundTag tag = player.getPersistentData();
      long lastManaSync = tag.getLong(TAG_LAST_MANA_SYNC_TICK);
      if (now - lastManaSync >= MANA_SYNC_INTERVAL_TICKS || vars.player_mana <= 0.0) {
         vars.syncMana(player);
         tag.putLong(TAG_LAST_MANA_SYNC_TICK, now);
      }

      if (syncProficiency) {
         long lastProfSync = tag.getLong(TAG_LAST_PROF_SYNC_TICK);
         if (now - lastProfSync >= PROF_SYNC_INTERVAL_TICKS) {
            vars.syncProficiency(player);
            tag.putLong(TAG_LAST_PROF_SYNC_TICK, now);
         }
      }
   }

   public static void forceCleanup(ServerPlayer player) {
      clearState(player);
   }

   private static void updateChargingPreview(ServerPlayer player, double progressRatio, double proficiency, int mode) {
      List<Vec3> anchors = getChargeAnchors(player, proficiency, mode);
      if (anchors.isEmpty()) {
         discardChargingPreview(player);
      } else {
         List<GanderProjectileEntity> previews = getOrCreateChargingPreviews(player, anchors.size());
         if (previews.size() == anchors.size()) {
            int chargeSeconds = getEquivalentChargeSeconds(proficiency);
            float targetScale = getProjectileScaleForCharge(chargeSeconds);
            double t = Math.max(0.0, Math.min(1.0, progressRatio));
            float scale = (float)(PROJECTILE_SCALE_MIN + (targetScale - PROJECTILE_SCALE_MIN) * t);

            for (int i = 0; i < previews.size(); i++) {
               GanderProjectileEntity preview = previews.get(i);
               Vec3 anchor = anchors.get(i);
               preview.setNoGravity(true);
               preview.setChargingPreview(true);
               preview.setChargeSeconds(chargeSeconds);
               preview.setVisualScale(scale);
               preview.setPos(anchor);
               preview.setDeltaMovement(Vec3.ZERO);
               preview.setYRot(player.getYRot());
               preview.setXRot(player.getXRot());
               preview.setYHeadRot(player.getYHeadRot());
               preview.hasImpulse = true;
            }
         }
      }
   }

   private static List<Vec3> getChargeAnchors(ServerPlayer player, double proficiency, int mode) {
      if (mode == 1) {
         int shotCount = getBarrageShotCount(proficiency);
         return getBarrageChargeAnchors(player, shotCount);
      } else {
         return List.of(getRapidChargeAnchor(player));
      }
   }

   private static Vec3 getRapidChargeAnchor(ServerPlayer player) {
      Vec3 handAnchor = EntityUtils.getCurrentEmptyHandCastAnchor(player);
      Vec3 forward = player.getLookAngle().normalize();
      return handAnchor.add(forward.scale(CHARGE_ANCHOR_FORWARD_FROM_HAND)).add(new Vec3(0.0, CHARGE_ANCHOR_UP_FROM_HAND, 0.0));
   }

   private static Vec3 getBarrageFormationCenter(ServerPlayer player) {
      Vec3 forward = player.getLookAngle().normalize();
      Vec3 right = forward.cross(new Vec3(0.0, 1.0, 0.0));
      if (right.lengthSqr() < 1.0E-6) {
         right = new Vec3(1.0, 0.0, 0.0);
      } else {
         right = right.normalize();
      }

      Vec3 up = right.cross(forward).normalize();
      return player.getEyePosition().add(forward.scale(-BARRAGE_BACK_OFFSET)).add(up.scale(BARRAGE_UP_OFFSET));
   }

   private static List<Vec3> getBarrageChargeAnchors(ServerPlayer player, int shotCount) {
      List<Vec3> anchors = new ArrayList<>();
      Vec3 forward = player.getLookAngle().normalize();
      Vec3 right = forward.cross(new Vec3(0.0, 1.0, 0.0));
      if (right.lengthSqr() < 1.0E-6) {
         right = new Vec3(1.0, 0.0, 0.0);
      } else {
         right = right.normalize();
      }

      Vec3 up = right.cross(forward).normalize();
      Vec3 center = getBarrageFormationCenter(player);
      int columns = Math.min(BARRAGE_COLUMNS, Math.max(1, shotCount));
      int rows = (int)Math.ceil((double)shotCount / columns);

      for (int i = 0; i < shotCount; i++) {
         int row = i / columns;
         int col = i % columns;
         double x = (col - (columns - 1) * 0.5) * BARRAGE_COL_SPACING + signedSlotNoise(i, 1) * BARRAGE_SIDE_JITTER;
         double y = ((rows - 1) * 0.5 - row) * BARRAGE_ROW_SPACING + signedSlotNoise(i, 2) * BARRAGE_HEIGHT_JITTER;
         double rowDepthOffset = ((rows - 1) * 0.5 - row) * BARRAGE_DEPTH_LAYER_SPREAD;
         double z = rowDepthOffset + signedSlotNoise(i, 3) * BARRAGE_DEPTH_JITTER;
         anchors.add(center.add(right.scale(x)).add(up.scale(y)).add(forward.scale(z)));
      }

      return anchors;
   }

   private static double signedSlotNoise(int slot, int salt) {
      long n = slot * 1103515245L ^ salt * 214013L + 2531011L;
      n = n << 13 ^ n;
      double unit = 1.0 - (n * (n * n * 15731L + 789221L) + 1376312589L & 2147483647L) / 1.0737418E9F;
      return Math.max(-1.0, Math.min(1.0, unit));
   }

   private static List<GanderProjectileEntity> getOrCreateChargingPreviews(ServerPlayer player, int count) {
      List<GanderProjectileEntity> previews = getChargingPreviews(player);
      if (previews.size() == count) {
         return previews;
      } else {
         discardChargingPreview(player);
         if (!(player.level() instanceof ServerLevel)) {
            return List.of();
         } else {
            List<GanderProjectileEntity> var6 = new ArrayList<>(count);
            Vec3 fallbackPos = player.position();

             for (int i = 0; i < count; i++) {
               GanderProjectileEntity preview = new GanderProjectileEntity(player.level(), player);
               preview.setNoGravity(true);
               preview.setChargingPreview(true);
               preview.setVisualScale(PROJECTILE_SCALE_MIN);
               preview.setItem(new ItemStack(ModItems.GANDER.get()));
               preview.setPos(fallbackPos);
               player.level().addFreshEntity(preview);
               var6.add(preview);
            }

            saveChargingPreviewIds(player, var6);
            return var6;
         }
      }
   }

   private static void saveChargingPreviewIds(ServerPlayer player, List<GanderProjectileEntity> previews) {
      CompoundTag tag = player.getPersistentData();
      ListTag list = new ListTag();

      for (GanderProjectileEntity preview : previews) {
         list.add(StringTag.valueOf(preview.getUUID().toString()));
      }

      tag.put(TAG_CHARGE_PREVIEW_UUIDS, list);
      if (previews.size() == 1) {
         tag.putUUID(TAG_CHARGE_PREVIEW_UUID, previews.get(0).getUUID());
      } else {
         tag.remove(TAG_CHARGE_PREVIEW_UUID);
      }
   }

   private static List<GanderProjectileEntity> getChargingPreviews(ServerPlayer player) {
      List<GanderProjectileEntity> previews = new ArrayList<>();
      if (!(player.level() instanceof ServerLevel serverLevel)) {
         return previews;
      } else {
         CompoundTag tag = player.getPersistentData();
         if (tag.contains(TAG_CHARGE_PREVIEW_UUIDS, 9)) {
            ListTag list = tag.getList(TAG_CHARGE_PREVIEW_UUIDS, 8);

            for (int i = 0; i < list.size(); i++) {
               String raw = list.getString(i);

               try {
                  UUID uuid = UUID.fromString(raw);
                  if (serverLevel.getEntity(uuid) instanceof GanderProjectileEntity preview && preview.isChargingPreview()) {
                     previews.add(preview);
                  }
               } catch (IllegalArgumentException var10) {
               }
            }

            if (!previews.isEmpty()) {
               return previews;
            }
         }

         if (tag.hasUUID(TAG_CHARGE_PREVIEW_UUID)) {
            UUID legacy = tag.getUUID(TAG_CHARGE_PREVIEW_UUID);
            if (serverLevel.getEntity(legacy) instanceof GanderProjectileEntity preview && preview.isChargingPreview()) {
               previews.add(preview);
            }
         }

         return previews;
      }
   }

   private static void discardChargingPreview(ServerPlayer player) {
      if (player.level() instanceof ServerLevel serverLevel) {
         CompoundTag var7 = player.getPersistentData();

         for (GanderProjectileEntity preview : getChargingPreviews(player)) {
            preview.discard();
         }

         if (var7.hasUUID(TAG_CHARGE_PREVIEW_UUID)) {
            UUID legacy = var7.getUUID(TAG_CHARGE_PREVIEW_UUID);
            if (serverLevel.getEntity(legacy) instanceof GanderProjectileEntity preview) {
               preview.discard();
            }
         }

         var7.remove(TAG_CHARGE_PREVIEW_UUIDS);
         var7.remove(TAG_CHARGE_PREVIEW_UUID);
      }
   }

   private static void spawnChantParticles(ServerPlayer player, double progressRatio, int mode, double proficiency) {
      if (player.level() instanceof ServerLevel serverLevel) {
         double var14 = Math.max(0.0, Math.min(1.0, progressRatio));
         Vec3 handCenter = getRapidChargeAnchor(player);
         Vec3 backCenter = getBarrageFormationCenter(player);
         if (mode == 1) {
            for (Vec3 anchor : getBarrageChargeAnchors(player, getBarrageShotCount(proficiency))) {
               spawnChargeGatheringAtCenter(serverLevel, anchor, var14, 1, 1, 0.46);
            }

            spawnChargeGatheringAtCenter(serverLevel, backCenter, var14, 3, 2, 0.84);
            spawnChargeGatheringAtCenter(serverLevel, handCenter, var14, 2, 1, 0.72);
         } else {
            spawnChargeGatheringAtCenter(serverLevel, handCenter, var14, 3, 2, 0.92);
            spawnChargeGatheringAtCenter(serverLevel, backCenter, var14, 2, 1, 0.72);
         }
      }
   }

   private static void spawnChargeGatheringAtCenter(ServerLevel serverLevel, Vec3 center, double progress, int blackCount, int redCount, double radiusScale) {
      double blackRadius = (0.22 - 0.1 * progress) * radiusScale;
      double redRadius = (0.16 - 0.08 * progress) * radiusScale;

      for (int i = 0; i < blackCount; i++) {
         double yaw = serverLevel.random.nextDouble() * (Math.PI * 2);
         double pitch = (serverLevel.random.nextDouble() - 0.5) * 0.8;
         Vec3 dir = new Vec3(Math.cos(yaw) * Math.cos(pitch), Math.sin(pitch), Math.sin(yaw) * Math.cos(pitch));
         Vec3 from = center.add(dir.scale(blackRadius));
         Vec3 vel = center.subtract(from).scale(0.28);
         serverLevel.sendParticles(BLACK_DUST, from.x, from.y, from.z, 0, vel.x, vel.y, vel.z, 1.0);
      }

      for (int i = 0; i < redCount; i++) {
         double yaw = serverLevel.random.nextDouble() * (Math.PI * 2);
         double pitch = (serverLevel.random.nextDouble() - 0.5) * 0.7;
         Vec3 dir = new Vec3(Math.cos(yaw) * Math.cos(pitch), Math.sin(pitch), Math.sin(yaw) * Math.cos(pitch));
         Vec3 from = center.add(dir.scale(redRadius));
         Vec3 vel = center.subtract(from).scale(0.34);
         serverLevel.sendParticles(RED_DUST, from.x, from.y, from.z, 0, vel.x, vel.y, vel.z, 1.0);
      }
   }

   private static Component getGateBarrageChantComponent() {
      return Component.literal("Fixierung ")
         .withStyle(ChatFormatting.GRAY)
         .append(Component.literal("EileSalve").withStyle(new ChatFormatting[]{ChatFormatting.DARK_RED, ChatFormatting.ITALIC}));
   }
}
