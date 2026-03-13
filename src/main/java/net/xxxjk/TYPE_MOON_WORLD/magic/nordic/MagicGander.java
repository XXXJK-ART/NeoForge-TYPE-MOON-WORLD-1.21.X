package net.xxxjk.TYPE_MOON_WORLD.magic.nordic;

import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.GanderProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;
import org.joml.Vector3f;

public final class MagicGander {
   private static final double MANA_PER_SECOND = 5.0;
   private static final int BASE_MAX_CHARGE_SECONDS = 4;
   private static final int ADVANCED_MAX_CHARGE_SECONDS = 5;
   private static final double ADVANCED_CHARGE_PROFICIENCY_THRESHOLD = 50.0;
   private static final int BASE_CHARGE_STEP_TICKS = 20;
   private static final int MIN_CHARGE_STEP_TICKS = 2;
   private static final double RELEASE_FORWARD_FROM_ANCHOR = 0.08;
   private static final double CHARGE_ANCHOR_FORWARD = 1.32;
   private static final double CHARGE_ANCHOR_DOWN = -0.12;
   private static final double CHARGE_ANCHOR_RIGHT = 0.3;
   private static final double CHARGE_ANCHOR_FORWARD_FROM_HAND = 0.54;
   private static final double CHARGE_ANCHOR_UP_FROM_HAND = 0.1;
   private static final float CHARGE_PREVIEW_SCALE_MIN = 0.207F;
   private static final float CHARGE_PREVIEW_SCALE_MAX = 0.37F;
   private static final DustParticleOptions BLACK_DUST = new DustParticleOptions(new Vector3f(0.05F, 0.05F, 0.05F), 1.0F);
   private static final DustParticleOptions RED_DUST = new DustParticleOptions(new Vector3f(0.95F, 0.08F, 0.12F), 1.1F);
   private static final String TAG_CHARGING = "TypeMoonGanderCharging";
   private static final String TAG_CHARGE_START_TICK = "TypeMoonGanderChargeStartTick";
   private static final String TAG_CHARGE_SECONDS = "TypeMoonGanderChargeSeconds";
   private static final String TAG_CHARGE_PREVIEW_UUID = "TypeMoonGanderChargePreviewUUID";

   private MagicGander() {
   }

   public static boolean execute(Entity entity) {
      if (entity instanceof ServerPlayer player) {
         if (isCharging(player)) {
            return releaseCharge(player);
         } else {
            beginCharge(player);
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean beginCharge(ServerPlayer player) {
      if (!canUse(player)) {
         return false;
      } else if (!EntityUtils.hasAnyEmptyHand(player)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.magic.need_empty_hand"), true);
         return false;
      } else if (isCharging(player)) {
         return true;
      } else {
         CompoundTag tag = player.getPersistentData();
         long now = player.level().getGameTime();
         tag.putBoolean("TypeMoonGanderCharging", true);
         tag.putLong("TypeMoonGanderChargeStartTick", now);
         tag.putInt("TypeMoonGanderChargeSeconds", 0);
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         int maxChargeSeconds = getMaxChargeSeconds(getEffectiveGanderProficiency(vars));
         updateChargingPreview(player, now, now, 20, 0, maxChargeSeconds);
         player.displayClientMessage(Component.translatable("message.typemoonworld.magic.gander.charge.start"), true);
         return true;
      }
   }

   public static boolean releaseCharge(ServerPlayer player) {
      if (!isCharging(player)) {
         return false;
      } else if (!EntityUtils.hasAnyEmptyHand(player)) {
         clearCharge(player);
         player.displayClientMessage(Component.translatable("message.typemoonworld.magic.need_empty_hand"), true);
         return false;
      } else {
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         boolean crestCast = vars.isCurrentSelectionFromCrest("gander");
         int maxChargeSeconds = getMaxChargeSeconds(getEffectiveGanderProficiency(vars));
         int chargeSeconds = Math.max(0, Math.min(maxChargeSeconds, player.getPersistentData().getInt("TypeMoonGanderChargeSeconds")));
         clearCharge(player);
         if (chargeSeconds <= 0) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.gander.charge.too_low"), true);
            return false;
         } else {
            GanderProjectileEntity projectile = new GanderProjectileEntity(player.level(), player);
            projectile.setNoGravity(true);
            projectile.setChargeSeconds(chargeSeconds);
            projectile.setItem(new ItemStack((ItemLike)ModItems.GANDER.get()));
            Vec3 direction = EntityUtils.getAutoAimDirection(player, 48.0, 55.0);
            Vec3 spawnPos = getChargeAnchor(player).add(direction.scale(0.08));
            projectile.setPos(spawnPos);
            projectile.setVisualScale(getChargeVisualScaleBySeconds(chargeSeconds, maxChargeSeconds));
            projectile.shoot(direction.x, direction.y, direction.z, 3.8F, 0.0F);
            player.level().addFreshEntity(projectile);
            player.level()
               .playSound(
                  null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS, 0.7F, 1.1F + chargeSeconds * 0.05F
               );
            if (!crestCast) {
               vars.proficiency_gander = Math.min(100.0, vars.proficiency_gander + 0.2 * chargeSeconds);
               vars.syncProficiency(player);
            }

            player.displayClientMessage(
               Component.translatable("message.typemoonworld.magic.gander.cast", new Object[]{chargeSeconds, (int)(chargeSeconds * 5.0)}), true
            );
            return true;
         }
      }
   }

   public static void tick(ServerPlayer player) {
      if (isCharging(player)) {
         if (!canUse(player)) {
            clearCharge(player);
         } else {
            CompoundTag tag = player.getPersistentData();
            long now = player.level().getGameTime();
            long startTick = tag.getLong("TypeMoonGanderChargeStartTick");
            TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(
               TypeMoonWorldModVariables.PLAYER_VARIABLES
            );
            double proficiency = getEffectiveGanderProficiency(vars);
            int maxChargeSeconds = getMaxChargeSeconds(proficiency);
            int chargedSeconds = Math.max(0, Math.min(maxChargeSeconds, tag.getInt("TypeMoonGanderChargeSeconds")));
            int chargeStepTicks = getChargeStepTicks(proficiency);
            int shouldReachSeconds = Math.max(0, Math.min(maxChargeSeconds, (int)((now - startTick) / chargeStepTicks)));
            updateChargingPreview(player, now, startTick, chargeStepTicks, chargedSeconds, maxChargeSeconds);
            if (chargedSeconds < shouldReachSeconds) {
               while (chargedSeconds < shouldReachSeconds) {
                  if (!ManaHelper.consumeManaOrHealth(player, 5.0)) {
                     releaseCharge(player);
                     return;
                  }

                  tag.putInt("TypeMoonGanderChargeSeconds", ++chargedSeconds);
                  vars.syncMana(player);
                  player.displayClientMessage(
                     Component.translatable("message.typemoonworld.magic.gander.charge.progress", new Object[]{chargedSeconds, maxChargeSeconds}), true
                  );
                  applySelfChargeFeedback(player, chargedSeconds);
                  if (chargedSeconds >= maxChargeSeconds) {
                     player.displayClientMessage(Component.translatable("message.typemoonworld.magic.gander.charge.max"), true);
                     break;
                  }
               }
            }
         }
      }
   }

   private static int getChargeStepTicks(double proficiency) {
      double clamped = Math.max(0.0, Math.min(100.0, proficiency));
      double t = clamped / 100.0;
      int stepTicks = (int)Math.round(20.0 - 18.0 * t);
      return Math.max(2, Math.min(20, stepTicks));
   }

   private static double getEffectiveGanderProficiency(TypeMoonWorldModVariables.PlayerVariables vars) {
      return vars.isCurrentSelectionFromCrest("gander") ? 100.0 : vars.proficiency_gander;
   }

   private static int getMaxChargeSeconds(double proficiency) {
      return proficiency >= 50.0 ? 5 : 4;
   }

   private static float getChargeVisualScaleBySeconds(int chargeSeconds, int maxChargeSeconds) {
      int clampedMax = Math.max(1, maxChargeSeconds);
      int clamped = Math.max(1, Math.min(clampedMax, chargeSeconds));
      double ratio = (double)clamped / clampedMax;
      return getChargeVisualScaleByRatio(ratio);
   }

   private static float getChargeVisualScaleByRatio(double ratio) {
      double progressRatio = Math.max(0.0, Math.min(1.0, ratio));
      return (float)(0.207F + 0.163F * progressRatio);
   }

   private static void updateChargingPreview(ServerPlayer player, long now, long startTick, int chargeStepTicks, int chargedSeconds, int maxChargeSeconds) {
      if (player.level() instanceof ServerLevel) {
         int clampedMaxCharge = Math.max(1, maxChargeSeconds);
         double rawProgress = Math.max(0.0, (double)(now - startTick) / chargeStepTicks);
         double chargeProgress = Math.max((double)chargedSeconds, Math.min((double)clampedMaxCharge, rawProgress));
         double progressRatio = Math.max(0.0, Math.min(1.0, chargeProgress / clampedMaxCharge));
         float scale = getChargeVisualScaleByRatio(progressRatio);
         GanderProjectileEntity preview = getOrCreateChargingPreview(player);
         if (preview != null) {
            Vec3 anchor = getChargeAnchor(player);
            preview.setNoGravity(true);
            preview.setChargingPreview(true);
            preview.setVisualScale(scale);
            preview.setPos(anchor);
            preview.setDeltaMovement(Vec3.ZERO);
            preview.setYRot(player.getYRot());
            preview.setXRot(player.getXRot());
            preview.setYHeadRot(player.getYHeadRot());
            preview.hasImpulse = true;
            if (progressRatio < 0.999) {
               spawnGatheringParticles((ServerLevel)player.level(), anchor, progressRatio);
            }
         }
      }
   }

   private static void applySelfChargeFeedback(ServerPlayer player, int chargedSeconds) {
      int amplifier = Math.max(0, chargedSeconds - 1);
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, amplifier, false, false, true));
      player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 25, amplifier, false, false, true));
   }

   private static boolean canUse(ServerPlayer player) {
      if (!player.isSpectator() && player.isAlive()) {
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (!vars.is_magus || !vars.is_magic_circuit_open) {
            return false;
         } else if (vars.selected_magics.isEmpty()) {
            return false;
         } else {
            int index = vars.current_magic_index;
            if (index >= 0 && index < vars.selected_magics.size()) {
               boolean crestCast = vars.isCurrentSelectionFromCrest("gander");
               return "gander".equals(vars.selected_magics.get(index)) && (vars.learned_magics.contains("gander") || crestCast)
                  ? EntityUtils.hasAnyEmptyHand(player)
                  : false;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   private static boolean isCharging(ServerPlayer player) {
      return player.getPersistentData().getBoolean("TypeMoonGanderCharging");
   }

   private static void clearCharge(ServerPlayer player) {
      discardChargingPreview(player);
      CompoundTag tag = player.getPersistentData();
      tag.putBoolean("TypeMoonGanderCharging", false);
      tag.putLong("TypeMoonGanderChargeStartTick", 0L);
      tag.putInt("TypeMoonGanderChargeSeconds", 0);
   }

   public static void forceCleanup(ServerPlayer player) {
      clearCharge(player);
   }

   public static Vec3 getChargeAnchor(LivingEntity caster) {
      float yawRad = caster.getYRot() * (float) (Math.PI / 180.0);
      Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0, Math.cos(yawRad));
      Vec3 worldUp = new Vec3(0.0, 1.0, 0.0);
      if (caster instanceof Player player) {
         Vec3 handAnchor = EntityUtils.getCurrentEmptyHandCastAnchor(player);
         return handAnchor.add(forward.scale(0.54)).add(worldUp.scale(0.1));
      } else {
         Vec3 sideBase = forward.cross(worldUp);
         if (sideBase.lengthSqr() < 1.0E-6) {
            sideBase = new Vec3(1.0, 0.0, 0.0);
         } else {
            sideBase = sideBase.normalize();
         }

         return caster.getEyePosition().add(forward.scale(1.32)).add(sideBase.scale(0.3)).add(worldUp.scale(-0.12));
      }
   }

   public static float getVisualScaleForChargeSeconds(int chargeSeconds) {
      return getChargeVisualScaleBySeconds(chargeSeconds, 5);
   }

   private static GanderProjectileEntity getOrCreateChargingPreview(ServerPlayer player) {
      GanderProjectileEntity preview = getChargingPreview(player);
      if (preview != null && preview.isAlive()) {
         return preview;
      } else if (!(player.level() instanceof ServerLevel)) {
         return null;
      } else {
         preview = new GanderProjectileEntity(player.level(), player);
         preview.setNoGravity(true);
         preview.setChargingPreview(true);
         preview.setVisualScale(0.207F);
         preview.setItem(new ItemStack((ItemLike)ModItems.GANDER.get()));
         preview.setPos(getChargeAnchor(player));
         player.level().addFreshEntity(preview);
         player.getPersistentData().putUUID("TypeMoonGanderChargePreviewUUID", preview.getUUID());
         return preview;
      }
   }

   private static GanderProjectileEntity getChargingPreview(ServerPlayer player) {
      if (player.level() instanceof ServerLevel serverLevel) {
         CompoundTag tag = player.getPersistentData();
         if (!tag.hasUUID("TypeMoonGanderChargePreviewUUID")) {
            return null;
         } else {
            UUID uuid = tag.getUUID("TypeMoonGanderChargePreviewUUID");
            return serverLevel.getEntity(uuid) instanceof GanderProjectileEntity preview && preview.isChargingPreview() ? preview : null;
         }
      } else {
         return null;
      }
   }

   private static void discardChargingPreview(ServerPlayer player) {
      if (player.level() instanceof ServerLevel serverLevel) {
         CompoundTag tag = player.getPersistentData();
         if (tag.hasUUID("TypeMoonGanderChargePreviewUUID")) {
            UUID uuid = tag.getUUID("TypeMoonGanderChargePreviewUUID");
            if (serverLevel.getEntity(uuid) instanceof GanderProjectileEntity preview) {
               preview.discard();
            }

            tag.remove("TypeMoonGanderChargePreviewUUID");
         }
      }
   }

   private static void spawnGatheringParticles(ServerLevel level, Vec3 center, double progressRatio) {
      double t = Math.max(0.0, Math.min(1.0, progressRatio));
      double outerRadius = 0.34 - 0.2 * t;
      double innerRadius = 0.22 - 0.14 * t;

      for (int i = 0; i < 4; i++) {
         double yaw = level.random.nextDouble() * (Math.PI * 2);
         double pitch = (level.random.nextDouble() - 0.5) * 0.9;
         Vec3 dir = new Vec3(Math.cos(yaw) * Math.cos(pitch), Math.sin(pitch), Math.sin(yaw) * Math.cos(pitch));
         Vec3 from = center.add(dir.scale(outerRadius));
         Vec3 vel = center.subtract(from).scale(0.3);
         level.sendParticles(BLACK_DUST, from.x, from.y, from.z, 0, vel.x, vel.y, vel.z, 1.0);
      }

      for (int i = 0; i < 3; i++) {
         double yaw = level.random.nextDouble() * (Math.PI * 2);
         double pitch = (level.random.nextDouble() - 0.5) * 0.7;
         Vec3 dir = new Vec3(Math.cos(yaw) * Math.cos(pitch), Math.sin(pitch), Math.sin(yaw) * Math.cos(pitch));
         Vec3 from = center.add(dir.scale(innerRadius));
         Vec3 vel = center.subtract(from).scale(0.36);
         level.sendParticles(RED_DUST, from.x, from.y, from.z, 0, vel.x, vel.y, vel.z, 1.0);
      }
   }
}
