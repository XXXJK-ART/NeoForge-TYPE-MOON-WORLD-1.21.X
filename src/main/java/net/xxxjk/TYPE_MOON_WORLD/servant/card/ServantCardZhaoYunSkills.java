package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.ZhaoYunHakuryuEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.zhaoyun.ZhaoYunDamageTypes;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

/**
 * Player-side Zhao Yun (Rider) servant-card implementation.
 *
 * <p>The NPC implementation deliberately remains in ZhaoYunRiderEntity. This
 * class owns only the transformed-player runtime state, so releasing or
 * switching cards can remove every transient modifier and mount cleanly.</p>
 */
public final class ServantCardZhaoYunSkills {
   public static final String SERVANT_ID = "zhao_yun_rider";
   public static final String TAG_BREAKTHROUGH = "ServantCardZhaoYunBreakthrough";
   public static final String TAG_QINGGANG_UNTIL = "ServantCardZhaoYunQinggangUntil";
   public static final String TAG_QINGGANG_TARGETS = "ServantCardZhaoYunQinggangTargets";
   public static final String TAG_RESCUE_UNTIL = "ServantCardZhaoYunRescueUntil";
   public static final String TAG_RESCUE_TARGET = "ServantCardZhaoYunRescueTarget";
   public static final String TAG_RESCUE_DEFENSE_UNTIL = "ServantCardZhaoYunRescueDefenseUntil";
   public static final String TAG_NP_CHANT_UNTIL = "ServantCardZhaoYunNpChantUntil";
   public static final String TAG_NP_MOUNT_UUID = "ServantCardZhaoYunNpMount";
   public static final String TAG_NP_UNTIL = "ServantCardZhaoYunNpUntil";
   public static final String TAG_NP_INITIAL_DISTANCE = "ServantCardZhaoYunNpInitialDistance";
   public static final String TAG_NP_INITIAL_DONE = "ServantCardZhaoYunNpInitialDone";
   public static final String TAG_NP_INITIAL_DIR_X = "ServantCardZhaoYunNpInitialDirX";
   public static final String TAG_NP_INITIAL_DIR_Z = "ServantCardZhaoYunNpInitialDirZ";
   public static final String TAG_NP_SAFE_X = "ServantCardZhaoYunNpSafeX";
   public static final String TAG_NP_SAFE_Y = "ServantCardZhaoYunNpSafeY";
   public static final String TAG_NP_SAFE_Z = "ServantCardZhaoYunNpSafeZ";
   public static final String TAG_SKILL_MOUNT_UUID = "ServantCardZhaoYunSkillMount";
   public static final String TAG_SKILL_MOUNT_COOLDOWN = "ServantCardZhaoYunSkillMountCooldown";
   public static final String TAG_NORMAL_THRUST_UNTIL = "ServantCardZhaoYunNormalThrustUntil";
   public static final String TAG_LAST_COMBAT = "ServantCardZhaoYunLastCombat";
   private static final String TAG_DRAGON_GALL_ENEMY_COUNT = "ServantCardZhaoYunDragonGallEnemyCount";
   private static final String TAG_DRAGON_GALL_SCAN_TICK = "ServantCardZhaoYunDragonGallScanTick";
   private static final int DRAGON_GALL_SCAN_INTERVAL = 10;

   private static final net.minecraft.resources.ResourceLocation DRAGON_GALL_ATTACK =
      net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_zhao_yun_dragon_gall_attack");
   private static final net.minecraft.resources.ResourceLocation DRAGON_GALL_SPEED =
      net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_zhao_yun_dragon_gall_speed");
   private static final net.minecraft.resources.ResourceLocation DRAGON_GALL_A_PLUS_ATTACK =
      net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_zhao_yun_dragon_gall_a_plus_attack");
   private static final net.minecraft.resources.ResourceLocation DRAGON_GALL_A_PLUS_SPEED =
      net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_zhao_yun_dragon_gall_a_plus_speed");
   private static final net.minecraft.resources.ResourceLocation SURROUNDED_ATTACK =
      net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_zhao_yun_surrounded_attack");
   private static final net.minecraft.resources.ResourceLocation DESPERATION_SPEED =
      net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_zhao_yun_desperation_speed");
   private static final net.minecraft.resources.ResourceLocation DESPERATION_ATTACK =
      net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_zhao_yun_desperation_attack");

   private ServantCardZhaoYunSkills() {}

   public static void initialize(ServerPlayer player) {
      clear(player);
      player.getPersistentData().putInt(TAG_BREAKTHROUGH, 0);
      player.getPersistentData().putLong(TAG_LAST_COMBAT, player.level().getGameTime());
      tick(player, player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES));
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!player.isAlive() || !vars.servant_card_transformed || !SERVANT_ID.equals(vars.servant_card_id)) {
         clear(player);
         return;
      }
      CompoundTag data = player.getPersistentData();
      long now = player.level().getGameTime();
      if (player.hurtTime > 0) {
         data.putLong(TAG_LAST_COMBAT, now);
      }
      if (now - data.getLong(TAG_LAST_COMBAT) >= 15L * 20L) {
         data.putInt(TAG_BREAKTHROUGH, 0);
      }
      int enemies = cachedDragonGallEnemyCount(player, now, 25.0);
      boolean low = player.getHealth() <= player.getMaxHealth() * 0.5F;

      // 龙胆 EX: permanent, non-slot passive.
      if (low) {
         ServantCardSkillUtils.addOrReplaceMultiplied(player.getAttribute(Attributes.ATTACK_DAMAGE), DRAGON_GALL_ATTACK, 0.20);
         ServantCardSkillUtils.addOrReplaceMultiplied(player.getAttribute(Attributes.MOVEMENT_SPEED), DRAGON_GALL_SPEED, 0.15);
         // Existing A-state bonuses remain active; these additional modifiers
         // represent the A+ breakthrough once Zhao Yun is below half health.
         ServantCardSkillUtils.addOrReplaceMultiplied(player.getAttribute(Attributes.ATTACK_DAMAGE), DRAGON_GALL_A_PLUS_ATTACK, 0.10);
         ServantCardSkillUtils.addOrReplaceMultiplied(player.getAttribute(Attributes.MOVEMENT_SPEED), DRAGON_GALL_A_PLUS_SPEED, 0.10);
      } else {
         ServantCardSkillUtils.remove(player.getAttribute(Attributes.ATTACK_DAMAGE), DRAGON_GALL_ATTACK);
         ServantCardSkillUtils.remove(player.getAttribute(Attributes.MOVEMENT_SPEED), DRAGON_GALL_SPEED);
         ServantCardSkillUtils.remove(player.getAttribute(Attributes.ATTACK_DAMAGE), DRAGON_GALL_A_PLUS_ATTACK);
         ServantCardSkillUtils.remove(player.getAttribute(Attributes.MOVEMENT_SPEED), DRAGON_GALL_A_PLUS_SPEED);
      }
      if (enemies >= 2) {
         double surroundedBonus = Math.min(0.50, 0.25 + Math.max(0, enemies - 2) * 0.05);
         ServantCardSkillUtils.addOrReplaceMultiplied(player.getAttribute(Attributes.ATTACK_DAMAGE), SURROUNDED_ATTACK, surroundedBonus);
      } else {
         ServantCardSkillUtils.remove(player.getAttribute(Attributes.ATTACK_DAMAGE), SURROUNDED_ATTACK);
      }
      // Mental interference is removed continuously while the passive is active.
      player.removeEffect(MobEffects.CONFUSION);
      player.removeEffect(MobEffects.DARKNESS);
      player.removeEffect(ModMobEffects.PALE_RIDER_FEAR);
      player.removeEffect(ModMobEffects.SUGGESTION);
      player.removeEffect(ModMobEffects.REVERSE_MOVEMENT);

      // A successful guard is represented by the short hurt window while blocking.
      if (player.isBlocking() && player.hurtTime > 0 && data.getLong("ServantCardZhaoYunLastGuard") != now) {
         data.putLong("ServantCardZhaoYunLastGuard", now);
         data.putInt(TAG_BREAKTHROUGH, Math.min(14, data.getInt(TAG_BREAKTHROUGH) + 1));
         data.putLong(TAG_LAST_COMBAT, now);
      }
      int stacks = Math.max(0, Math.min(14, data.getInt(TAG_BREAKTHROUGH)));
      if (stacks > 0) {
         ServantCardSkillUtils.addOrReplaceMultiplied(player.getAttribute(Attributes.MOVEMENT_SPEED), id("servant_card_zhao_yun_breakthrough_speed"), stacks * 0.03);
      } else {
         ServantCardSkillUtils.remove(player.getAttribute(Attributes.MOVEMENT_SPEED), id("servant_card_zhao_yun_breakthrough_speed"));
      }
      if (stacks >= 7) {
         player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
         player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 4, 1, false, false, false));
      }
      if (stacks >= 8) {
         player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
         player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 4, 2, false, false, false));
      }

      // 绝境突围: automatic and slotless.
      boolean desperation = low && enemies >= 2;
      if (desperation) {
         data.putLong("ServantCardZhaoYunDesperationUntil", now + 20L);
         ServantCardSkillUtils.addOrReplaceMultiplied(player.getAttribute(Attributes.MOVEMENT_SPEED), DESPERATION_SPEED, 0.25);
         ServantCardSkillUtils.addOrReplaceMultiplied(player.getAttribute(Attributes.ATTACK_DAMAGE), DESPERATION_ATTACK, 0.20);
         player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20, 1, false, true, true));
      } else if (data.getLong("ServantCardZhaoYunDesperationUntil") <= now) {
         ServantCardSkillUtils.remove(player.getAttribute(Attributes.MOVEMENT_SPEED), DESPERATION_SPEED);
         ServantCardSkillUtils.remove(player.getAttribute(Attributes.ATTACK_DAMAGE), DESPERATION_ATTACK);
      }

      tickRescue(player, now);
      tickQinggang(player, now);
      tickSkillMount(player);
      tickNp(player, vars, now);
      if (player.tickCount % 8 == 0 && player.level() instanceof ServerLevel level
         && (desperation || stacks >= 7 || isNpActive(player))) {
         level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.8, player.getZ(), 8, 0.35, 0.65, 0.35, 0.04);
      }
   }

   public static void clear(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      ServantCardSkillUtils.remove(player.getAttribute(Attributes.ATTACK_DAMAGE), DRAGON_GALL_ATTACK);
      ServantCardSkillUtils.remove(player.getAttribute(Attributes.MOVEMENT_SPEED), DRAGON_GALL_SPEED);
      ServantCardSkillUtils.remove(player.getAttribute(Attributes.ATTACK_DAMAGE), DRAGON_GALL_A_PLUS_ATTACK);
      ServantCardSkillUtils.remove(player.getAttribute(Attributes.MOVEMENT_SPEED), DRAGON_GALL_A_PLUS_SPEED);
      ServantCardSkillUtils.remove(player.getAttribute(Attributes.ATTACK_DAMAGE), SURROUNDED_ATTACK);
      ServantCardSkillUtils.remove(player.getAttribute(Attributes.MOVEMENT_SPEED), DESPERATION_SPEED);
      ServantCardSkillUtils.remove(player.getAttribute(Attributes.ATTACK_DAMAGE), DESPERATION_ATTACK);
      ServantCardSkillUtils.remove(player.getAttribute(Attributes.MOVEMENT_SPEED), id("servant_card_zhao_yun_breakthrough_speed"));
      data.remove(TAG_BREAKTHROUGH);
      data.remove(TAG_LAST_COMBAT);
      data.remove(TAG_DRAGON_GALL_ENEMY_COUNT);
      data.remove(TAG_DRAGON_GALL_SCAN_TICK);
      data.remove("ServantCardZhaoYunLastGuard");
      data.remove("ServantCardZhaoYunDesperationUntil");
      data.remove(TAG_RESCUE_UNTIL);
      data.remove(TAG_RESCUE_TARGET);
      data.remove(TAG_RESCUE_DEFENSE_UNTIL);
      data.remove(TAG_QINGGANG_UNTIL);
      data.remove(TAG_QINGGANG_TARGETS);
      data.remove(TAG_NP_CHANT_UNTIL);
      data.remove(TAG_NP_UNTIL);
      data.remove(TAG_NP_INITIAL_DISTANCE);
      data.remove(TAG_NP_INITIAL_DONE);
      data.remove(TAG_NP_INITIAL_DIR_X);
      data.remove(TAG_NP_INITIAL_DIR_Z);
      data.remove(TAG_NP_SAFE_X);
      data.remove(TAG_NP_SAFE_Y);
      data.remove(TAG_NP_SAFE_Z);
      clearNpHitTags(data);
      discardMount(player, TAG_SKILL_MOUNT_UUID);
      discardMount(player, TAG_NP_MOUNT_UUID);
   }

   public static boolean performBasicSpearCombo(ServerPlayer player) {
      if (!player.isCrouching()) return false;
      Vec3 dir = PlayerNoblePhantasmHelper.horizontalLook(player);
      for (int i = 0; i < 3; i++) {
         final int hit = i;
         TYPE_MOON_WORLD.queueServerWork(i * 4, () -> {
            if (!player.isAlive() || !SERVANT_ID.equals(player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servant_card_id)) return;
            ServantCardSkillUtils.hitForwardArc(player, dir, 4.6, (float)(player.getAttributeValue(Attributes.ATTACK_DAMAGE) * (hit == 2 ? 1.25 : 0.75) + (hit == 2 ? 7 : 3)));
            if (hit == 2) {
               for (LivingEntity target : nearbyEnemies(player, 4.6)) target.push(dir.x * 0.9, 0.2, dir.z * 0.9);
            }
            if (player.level() instanceof ServerLevel level) {
               Vec3 at = player.position().add(dir.scale(1.8)).add(0.0, 0.8, 0.0);
               spawnSkillBurst(level, at, 0.7, hit == 2 ? 18 : 12);
            }
         });
      }
      return true;
   }

   /**
    * Ordinary right-click spear thrust. This is intentionally separate from
    * skill slot 1: it has no MP cost or cooldown, deals modest damage, and is
    * primarily a short mobility burst. While mounted, the horse's current
    * horizontal speed contributes a small capped damage bonus.
    */
   public static boolean performNormalSpearThrust(ServerPlayer player) {
      CompoundTag playerData = player.getPersistentData();
      long now = player.level().getGameTime();
      if (playerData.getLong(TAG_NORMAL_THRUST_UNTIL) > now) {
         return false;
      }
      playerData.putLong(TAG_NORMAL_THRUST_UNTIL, now + 10L);

      Vec3 dir = PlayerNoblePhantasmHelper.horizontalLook(player);
      ZhaoYunHakuryuEntity mount = player.getVehicle() instanceof ZhaoYunHakuryuEntity value
         && value.isAlive() ? value : null;
      double currentSpeed = mount == null ? 0.0 : mount.getDeltaMovement().horizontalDistance();
      double speedBonus = Math.min(7.0, currentSpeed * 7.0);
      float damage = (float)(4.0 + player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.25 + speedBonus);
      double thrustDistance = mount == null ? 0.72 : 1.15;

      if (mount != null) {
         float yaw = (float)(Math.atan2(-dir.x, dir.z) * 180.0 / Math.PI);
         mount.setYRot(yaw);
         mount.setYBodyRot(yaw);
         mount.setYHeadRot(yaw);
         mount.move(net.minecraft.world.entity.MoverType.SELF, dir.scale(thrustDistance));
         mount.setDeltaMovement(dir.scale(Math.min(1.0, Math.max(0.45, currentSpeed + 0.35)))
            .add(0.0, mount.getDeltaMovement().y, 0.0));
         mount.hasImpulse = true;
      } else {
         player.setDeltaMovement(dir.scale(thrustDistance)
            .add(0.0, Math.max(0.0, player.getDeltaMovement().y), 0.0));
         player.hurtMarked = true;
      }

      ServantCardSkillUtils.hitForwardArc(player, dir, mount == null ? 3.6 : 4.2, damage);
      if (player.level() instanceof ServerLevel level) {
         Vec3 origin = mount == null ? player.position() : mount.position();
         Vec3 tip = origin.add(dir.scale(mount == null ? 1.4 : 1.9))
            .add(0.0, mount == null ? 0.9 : 1.15, 0.0);
         ServantCardSkillUtils.spawnLineParticles(level, origin.add(0.0, 0.8, 0.0), tip, ParticleTypes.CRIT);
         level.sendParticles(ParticleTypes.CLOUD, tip.x, tip.y, tip.z, mount == null ? 5 : 8,
            0.25, 0.18, 0.25, 0.03);
         level.playSound(null, BlockPos.containing(origin), SoundEvents.PLAYER_ATTACK_SWEEP,
            SoundSource.PLAYERS, 0.45F, mount == null ? 1.15F : 0.95F);
      }
      return true;
   }

   public static boolean summonSkillHakuryu(ServerPlayer player) {
      if (getMount(player, TAG_SKILL_MOUNT_UUID) != null
         || player.getPersistentData().getLong(TAG_SKILL_MOUNT_COOLDOWN) > player.level().getGameTime()) return false;
      if (!(player.level() instanceof ServerLevel level)) return false;
      ZhaoYunHakuryuEntity mount = ModEntities.ZHAO_YUN_HAKURYU.get().create(level);
      if (mount == null) return false;
      mount.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
      mount.setHealth(mount.getMaxHealth());
      mount.bindSkillOwner(player);
      if (!level.addFreshEntity(mount)) return false;
      player.getPersistentData().putUUID(TAG_SKILL_MOUNT_UUID, mount.getUUID());
      player.startRiding(mount, true);
      return true;
   }

   public static boolean performSpearBreakthrough(ServerPlayer player) {
      Vec3 dir = PlayerNoblePhantasmHelper.horizontalLook(player);
      ZhaoYunHakuryuEntity mount = player.getVehicle() instanceof ZhaoYunHakuryuEntity value
         && value.isAlive() ? value : null;
      if (mount != null) {
         // When mounted, the spear thrust belongs to the horse as well as the
         // rider.  Keep the horse aligned to the player's horizontal facing
         // and propel the mount so the rider does not appear to slide in place.
         float yaw = (float)(Math.atan2(-dir.x, dir.z) * 180.0 / Math.PI);
         mount.setYRot(yaw);
         mount.setYBodyRot(yaw);
         mount.setYHeadRot(yaw);
         mount.setDeltaMovement(dir.scale(1.15).add(0.0, mount.getDeltaMovement().y, 0.0));
         // Apply the lunge immediately as well as setting velocity. The
         // controlled-mount tick normally follows player input and would
         // otherwise clear a one-tick impulse when the rider is not holding
         // forward.
         mount.move(net.minecraft.world.entity.MoverType.SELF, dir.scale(1.15));
         mount.hasImpulse = true;
      } else {
         player.setDeltaMovement(dir.scale(0.9).add(0.0, Math.max(0.0, player.getDeltaMovement().y), 0.0));
         player.hurtMarked = true;
      }
      ServantCardSkillUtils.hitForwardArc(player, dir, 5.0, (float)(player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.3 + 8.0));
      knockForwardEnemies(player, dir, 5.0, 0.9);
      spawnSlash(player, dir, ParticleTypes.CRIT);
      if (player.level() instanceof ServerLevel level) {
         Vec3 origin = mount == null ? player.position() : mount.position();
         spawnSkillBurst(level, origin.add(dir.scale(2.0)).add(0.0, 0.9, 0.0), 0.9, 26);
      }
      return true;
   }

   public static boolean performRescue(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars =
         player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      ServerPlayer master = MasterServantLinkService.getLinkedMaster(player, vars);
      if (master == null || !master.isAlive() || master.level() != player.level()
         || master == player
         || player.distanceToSqr(master) > 12.0 * 12.0) {
         return false;
      }
      LivingEntity target = master;
      long until = player.level().getGameTime() + 12L * 20L;
      CompoundTag data = player.getPersistentData();
      data.putLong(TAG_RESCUE_UNTIL, until);
      data.putUUID(TAG_RESCUE_TARGET, target.getUUID());
      // Resistance I supplies a visible baseline; CommonEvents applies the
      // remaining factor so the final rescue defense is exactly 30% rather
      // than the old 50%-class protection.
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 12 * 20, 0, false, true, true));
      target.getPersistentData().putLong(TAG_RESCUE_DEFENSE_UNTIL, until);
      player.getPersistentData().putLong(TAG_RESCUE_DEFENSE_UNTIL, until);
      if (player.level() instanceof ServerLevel level) {
         VFXServerEffects.spawn(level, "servant_zhao_yun_rescue", player, 64.0);
         level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, target.getX(), target.getY() + 0.8, target.getZ(), 12, 0.35, 0.5, 0.35, 0.04);
      }
      return true;
   }

   public static boolean performDragonSweep(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return false;
      Vec3 origin = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
      AABB area = player.getBoundingBox().inflate(4.5, 1.8, 4.5);
      float damage = (float)(player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.15 + 10.0);
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, e -> isEnemy(player, e))) {
         target.invulnerableTime = 0;
         target.hurt(player.damageSources().playerAttack(player), damage);
         target.invulnerableTime = 0;
         Vec3 away = target.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
         if (away.lengthSqr() < 1.0E-4) away = PlayerNoblePhantasmHelper.horizontalLook(player);
         away = away.normalize();
         target.push(away.x * 1.0, 0.25, away.z * 1.0);
      }
      VFXServerEffects.spawn(level, "servant_zhao_yun_dragon_gall", player, 64.0);
      level.sendParticles(ParticleTypes.SWEEP_ATTACK, origin.x, origin.y, origin.z, 18, 2.8, 0.5, 2.8, 0.0);
      spawnSkillBurst(level, origin, 2.8, 30);
      return true;
   }

   public static boolean performMountedRush(ServerPlayer player) {
      Vec3 dir = PlayerNoblePhantasmHelper.horizontalLook(player);
      ZhaoYunHakuryuEntity mount = getMount(player, TAG_SKILL_MOUNT_UUID);
      if (mount != null) {
         mount.setDeltaMovement(dir.scale(1.4).add(0.0, mount.getDeltaMovement().y, 0.0));
         mount.hurtMarked = true;
         hitMountTargets(player, mount, 12.0, 35.0F);
      } else {
         player.setDeltaMovement(dir.scale(1.2).add(0.0, player.getDeltaMovement().y, 0.0));
         ServantCardSkillUtils.hitForwardArc(player, dir, 4.5, 25.0F);
      }
      spawnSlash(player, dir, ParticleTypes.CLOUD);
      if (player.level() instanceof ServerLevel level) {
         spawnSkillBurst(level, player.position().add(0.0, 0.7, 0.0), 1.2, 28);
      }
      return true;
   }

   public static boolean performDragonFlash(ServerPlayer player) {
      Vec3 dir = PlayerNoblePhantasmHelper.horizontalLook(player);
      ServantCardSkillUtils.hitForwardArc(player, dir, 7.0, (float)(player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 2.0 + 18.0));
      spawnSlash(player, dir, ParticleTypes.END_ROD);
      if (player.level() instanceof ServerLevel level) {
         spawnSkillBurst(level, player.position().add(dir.scale(2.5)).add(0.0, 1.0, 0.0), 1.0, 34);
      }
      return true;
   }

   public static boolean performSevenProbe(ServerPlayer player) {
      for (int i = 0; i < 3; i++) {
         final int step = i;
         TYPE_MOON_WORLD.queueServerWork(i * 5, () -> {
            if (!player.isAlive()) return;
            LivingEntity target = ServantCardSkillUtils.findLookTarget(player, 12.0, 1.8);
            Vec3 dir = target == null ? PlayerNoblePhantasmHelper.horizontalLook(player) : target.position().subtract(player.position()).normalize();
            player.setDeltaMovement(new Vec3(dir.x * 0.95, Math.max(player.getDeltaMovement().y, 0.08), dir.z * 0.95));
            ServantCardSkillUtils.hitForwardArc(player, dir, 4.5, 24.0F);
            if (step == 2) ServantCardSkillUtils.hitForwardArc(player, dir, 6.0, 40.0F);
            if (player.level() instanceof ServerLevel level) {
               Vec3 at = player.position().add(dir.scale(1.8)).add(0.0, 0.9, 0.0);
               spawnSkillBurst(level, at, step == 2 ? 1.2 : 0.85, step == 2 ? 32 : 24);
               level.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z, 8, 0.45, 0.35, 0.45, 0.02);
            }
         });
      }
      return true;
   }

   public static boolean performHakuryuTrample(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return false;
      ZhaoYunHakuryuEntity mount = getMount(player, TAG_SKILL_MOUNT_UUID);
      LivingEntity source = mount == null ? player : mount;
      AABB area = source.getBoundingBox().inflate(4.5, 1.6, 4.5);
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, e -> isEnemy(player, e))) {
         target.hurt(player.damageSources().playerAttack(player), 28.0F);
         target.push(0.0, 0.6, 0.0);
         target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1, false, true, true));
      }
      level.sendParticles(ParticleTypes.EXPLOSION, source.getX(), source.getY() + 0.2, source.getZ(), 16, 1.4, 0.25, 1.4, 0.0);
      spawnSkillBurst(level, source.position().add(0.0, 0.8, 0.0), 2.6, 28);
      return true;
   }

   public static boolean performQinggang(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      data.putLong(TAG_QINGGANG_UNTIL, player.level().getGameTime() + 15L * 20L);
      data.remove(TAG_QINGGANG_TARGETS);
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 0.7, player.getZ(), 40, 0.7, 0.9, 0.7, 0.05);
         level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.8, player.getZ(), 18, 0.55, 0.8, 0.55, 0.03);
      }
      return true;
   }

   public static boolean performChangbanpo(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      long now = player.level().getGameTime();
      if (data.getLong(TAG_NP_CHANT_UNTIL) > now || data.getLong(TAG_NP_UNTIL) > now) return false;
      // A stale temporary horse must not be allowed to coexist with a new
      // Noble Phantasm horse after a disconnect, failed dismount, or death.
      discardMount(player, TAG_NP_MOUNT_UUID);
      data.putLong(TAG_NP_CHANT_UNTIL, now + 3L * 20L);
      data.remove(TAG_NP_UNTIL);
      data.remove(TAG_NP_INITIAL_DONE);
      data.putDouble(TAG_NP_INITIAL_DISTANCE, 0.0);
      data.remove(TAG_NP_SAFE_X);
      data.remove(TAG_NP_SAFE_Y);
      data.remove(TAG_NP_SAFE_Z);
      clearNpHitTags(data);
      player.level().playSound(null, player.blockPosition(), ModSounds.ZHAO_YUN_VOICE_NP.get(), SoundSource.VOICE, 1.0F, 1.0F);
      return true;
   }

   public static void onZhaoYunAttack(ServerPlayer player, LivingEntity target) {
      if (!isQinggangActive(player) || target == null || !target.isAlive() || !isEnemy(player, target)) return;
      TYPE_MOON_WORLD.queueServerWork(10, () -> {
         if (!player.isAlive() || !target.isAlive() || !isQinggangActive(player)) return;
         // This is a real second attack: normal dodge/block/i-frame handling
         // still applies. The damage type only bypasses armor.
         boolean hit = target.hurt(
            player.damageSources().source(ZhaoYunDamageTypes.QINGGANG_SECOND_HIT, player),
            20.0F);
         if (hit && player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.55,
               target.getZ(), 8, 0.2, 0.25, 0.2, 0.02);
         }
      });
   }

   public static boolean isQinggangActive(ServerPlayer player) {
      return player.getPersistentData().getLong(TAG_QINGGANG_UNTIL) > player.level().getGameTime();
   }

   /** Records one successful dodge or guard for Seven In Seven Out. */
   public static void recordBreakthroughDefense(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed || !SERVANT_ID.equals(vars.servant_card_id)) return;
      CompoundTag data = player.getPersistentData();
      data.putInt(TAG_BREAKTHROUGH, Math.min(14, Math.max(0, data.getInt(TAG_BREAKTHROUGH)) + 1));
      data.putLong(TAG_LAST_COMBAT, player.level().getGameTime());
   }

   public static void markCombatActivity(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars.servant_card_transformed && SERVANT_ID.equals(vars.servant_card_id)) {
         player.getPersistentData().putLong(TAG_LAST_COMBAT, player.level().getGameTime());
      }
   }

   public static boolean hasSevenInSevenOutDiscount(ServerPlayer player) {
      return player.getPersistentData().getInt(TAG_BREAKTHROUGH) >= 8;
   }

   public static boolean isMeleeSmallSkill(String effectId) {
      return switch (effectId) {
         case "zhao_yun_spear_combo", "zhao_yun_spear_breakthrough",
            "zhao_yun_dragon_sweep", "zhao_yun_mounted_rush",
            "zhao_yun_dragon_flash", "zhao_yun_seven_probe",
            "zhao_yun_hakuryu_trample" -> true;
         default -> false;
      };
   }

   private static void tickRescue(ServerPlayer player, long now) {
      CompoundTag data = player.getPersistentData();
      if (data.getLong(TAG_RESCUE_UNTIL) <= now) {
         data.remove(TAG_RESCUE_UNTIL);
         data.remove(TAG_RESCUE_TARGET);
         data.remove(TAG_RESCUE_DEFENSE_UNTIL);
      }
   }

   private static void tickQinggang(ServerPlayer player, long now) {
      CompoundTag data = data(player);
      if (data.getLong(TAG_QINGGANG_UNTIL) <= now) {
         data.remove(TAG_QINGGANG_UNTIL);
         data.remove(TAG_QINGGANG_TARGETS);
      }
   }

   private static void tickSkillMount(ServerPlayer player) {
      ZhaoYunHakuryuEntity mount = getMount(player, TAG_SKILL_MOUNT_UUID);
      if (mount == null && data(player).hasUUID(TAG_SKILL_MOUNT_UUID)) {
         data(player).remove(TAG_SKILL_MOUNT_UUID);
      } else if (mount != null && (!player.isAlive() || !player.isPassenger() || player.getVehicle() != mount)) {
         discardMount(player, TAG_SKILL_MOUNT_UUID);
      }
   }

   public static int summonCooldownRemaining(ServerPlayer player) {
      return (int)Math.max(0L, player.getPersistentData().getLong(TAG_SKILL_MOUNT_COOLDOWN) - player.level().getGameTime());
   }

   private static void tickNp(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, long now) {
      CompoundTag data = player.getPersistentData();
      long chant = data.getLong(TAG_NP_CHANT_UNTIL);
      if (chant > 0L && now >= chant) {
         data.remove(TAG_NP_CHANT_UNTIL);
         ZhaoYunHakuryuEntity mount = spawnNpMount(player);
         if (mount != null) {
            data.putLong(TAG_NP_UNTIL, now + 15L * 20L);
            data.putDouble(TAG_NP_INITIAL_DISTANCE, 0.0);
            data.remove(TAG_NP_INITIAL_DONE);
            rememberNpSafePosition(data, mount);
            Vec3 initialDirection = PlayerNoblePhantasmHelper.horizontalLook(player);
            data.putDouble(TAG_NP_INITIAL_DIR_X, initialDirection.x);
            data.putDouble(TAG_NP_INITIAL_DIR_Z, initialDirection.z);
            mount.setNpActive(true);
            if (!player.startRiding(mount, true)) {
               mount.discard();
               data.remove(TAG_NP_MOUNT_UUID);
               data.remove(TAG_NP_UNTIL);
               data.remove(TAG_NP_INITIAL_DISTANCE);
               data.remove(TAG_NP_INITIAL_DONE);
               data.remove(TAG_NP_INITIAL_DIR_X);
               data.remove(TAG_NP_INITIAL_DIR_Z);
               clearNpHitTags(data);
               return;
            }
            if (player.level() instanceof ServerLevel level) {
            VFXServerEffects.spawn(level, "servant_zhao_yun_changbanpo", mount, 96.0);
            }
         }
      }
      if (data.getLong(TAG_NP_UNTIL) > now) {
         ZhaoYunHakuryuEntity mount = getMount(player, TAG_NP_MOUNT_UUID);
         if (mount != null) {
            // Dismounting the temporary NP horse cancels that horse instead
            // of leaving an ownerless entity moving through the world.
            if (player.getVehicle() != mount) {
               discardMount(player, TAG_NP_MOUNT_UUID);
               data.remove(TAG_NP_UNTIL);
               data.remove(TAG_NP_INITIAL_DISTANCE);
               data.remove(TAG_NP_INITIAL_DONE);
               data.remove(TAG_NP_INITIAL_DIR_X);
               data.remove(TAG_NP_INITIAL_DIR_Z);
               data.remove(TAG_NP_SAFE_X);
               data.remove(TAG_NP_SAFE_Y);
               data.remove(TAG_NP_SAFE_Z);
               clearNpHitTags(data);
               return;
            }
            double distance = data.getDouble(TAG_NP_INITIAL_DISTANCE);
            boolean opening = !data.getBoolean(TAG_NP_INITIAL_DONE);
            Vec3 dir = opening ? initialChargeDirection(data)
               : PlayerNoblePhantasmHelper.horizontalLook(player);
            double speed = (opening ? 1.0 : 0.65) * cardMovementSpeedRatio(player);
            double step = Math.min(speed, opening ? 50.0 - distance : speed);
            boolean moved = moveMountHorizontally(player, mount, dir, step);
            if (opening) {
               if (moved) data.putDouble(TAG_NP_INITIAL_DISTANCE, distance + step);
               // An opening-dash bedrock hit stops the fixed dash safely and
               // hands control to the 15-second free-movement phase.
               if (!moved || distance + step >= 50.0) data.putBoolean(TAG_NP_INITIAL_DONE, true);
            }
            hitMountTargets(player, mount, opening ? 3.8 : 3.2, opening ? 200.0F : 100.0F);
            if (player.tickCount % 3 == 0 && player.level() instanceof ServerLevel level) {
               level.sendParticles(ParticleTypes.END_ROD, mount.getX(), mount.getY() + 1.0, mount.getZ(), 20, 1.0, 0.5, 1.0, 0.03);
               level.sendParticles(ParticleTypes.CLOUD, mount.getX(), mount.getY() + 0.2, mount.getZ(), 12, 0.8, 0.1, 0.8, 0.03);
            }
         } else {
            data.remove(TAG_NP_UNTIL);
            data.remove(TAG_NP_INITIAL_DISTANCE);
            data.remove(TAG_NP_INITIAL_DONE);
            data.remove(TAG_NP_INITIAL_DIR_X);
            data.remove(TAG_NP_INITIAL_DIR_Z);
            clearNpHitTags(data);
            data.remove(TAG_NP_SAFE_X);
            data.remove(TAG_NP_SAFE_Y);
            data.remove(TAG_NP_SAFE_Z);
            data.remove(TAG_NP_MOUNT_UUID);
         }
      } else if (data.getLong(TAG_NP_UNTIL) > 0L) {
         ZhaoYunHakuryuEntity mount = getMount(player, TAG_NP_MOUNT_UUID);
         if (mount != null) {
            mount.setNpActive(false);
            mount.promoteToSkillMount(player);
            if (data.hasUUID(TAG_SKILL_MOUNT_UUID)
               && !data.getUUID(TAG_SKILL_MOUNT_UUID).equals(mount.getUUID())
               && player.level() instanceof ServerLevel level) {
               Entity previous = level.getEntity(data.getUUID(TAG_SKILL_MOUNT_UUID));
               if (previous != null) {
                  previous.ejectPassengers();
                  previous.discard();
               }
            }
            data.putUUID(TAG_SKILL_MOUNT_UUID, mount.getUUID());
         }
         data.remove(TAG_NP_MOUNT_UUID);
         data.remove(TAG_NP_UNTIL);
         data.remove(TAG_NP_INITIAL_DISTANCE);
         data.remove(TAG_NP_INITIAL_DONE);
         data.remove(TAG_NP_INITIAL_DIR_X);
         data.remove(TAG_NP_INITIAL_DIR_Z);
         data.remove(TAG_NP_SAFE_X);
         data.remove(TAG_NP_SAFE_Y);
         data.remove(TAG_NP_SAFE_Z);
         clearNpHitTags(data);
      }
   }

   private static ZhaoYunHakuryuEntity spawnNpMount(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return null;
      ZhaoYunHakuryuEntity mount = ModEntities.ZHAO_YUN_HAKURYU.get().create(level);
      if (mount == null) return null;
      // If the player is already on the persistent skill horse, use that
      // horse's feet as the spawn height. Spawning at the player's passenger
      // eye/seat height made the NP horse begin in mid-air and fight gravity
      // for several ticks.
      double spawnY = player.getVehicle() instanceof ZhaoYunHakuryuEntity vehicle
         ? vehicle.getY() : player.getY();
      mount.moveTo(player.getX(), spawnY, player.getZ(), player.getYRot(), 0.0F);
      mount.setHealth(mount.getMaxHealth());
      mount.bindNoblePhantasmOwner(player);
      if (!level.addFreshEntity(mount)) return null;
      player.getPersistentData().putUUID(TAG_NP_MOUNT_UUID, mount.getUUID());
      return mount;
   }

   private static boolean moveMountHorizontally(ServerPlayer owner, ZhaoYunHakuryuEntity mount, Vec3 dir, double distance) {
      if (!(mount.level() instanceof ServerLevel level)) return false;
      Vec3 flat = new Vec3(dir.x, 0.0, dir.z);
      if (flat.lengthSqr() < 1.0E-6 || distance <= 0.0) return true;
      flat = flat.normalize();
      Vec3 delta = flat.scale(distance);
      AABB startBox = mount.getBoundingBox();
      if (containsBedrock(level, startBox, delta)) return false;

      if (mount.onGround() || hasSolidSupport(level, mount)) {
         rememberNpSafePosition(owner.getPersistentData(), mount);
      }
      breakNpBlocks(level, mount, delta);

      float yaw = (float)(Math.atan2(-flat.x, flat.z) * 180.0 / Math.PI);
      mount.setYRot(yaw);
      mount.setYBodyRot(yaw);
      mount.setYHeadRot(yaw);
      double verticalVelocity = mount.getDeltaMovement().y;
      mount.move(net.minecraft.world.entity.MoverType.SELF, delta);
      mount.setDeltaMovement(0.0, verticalVelocity, 0.0);
      mount.fallDistance = 0.0F;

      // Gravity stays enabled; pits and ledges are part of the charge path.
      // Only void-level failures are treated as invalid movement.
      if (mount.getY() < level.getMinBuildHeight() - 4) {
         restoreNpSafePosition(owner.getPersistentData(), mount);
         return false;
      }
      return true;
   }

   private static boolean containsBedrock(ServerLevel level, AABB startBox, Vec3 delta) {
      int steps = Math.max(1, (int)Math.ceil(delta.length() / 0.4));
      for (int i = 0; i <= steps; i++) {
         double progress = (double)i / steps;
         AABB box = startBox.move(delta.scale(progress)).inflate(0.04);
         BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
         BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
         for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (level.getBlockState(pos).is(Blocks.BEDROCK)) return true;
         }
      }
      return false;
   }

   private static void breakNpBlocks(ServerLevel level, ZhaoYunHakuryuEntity mount, Vec3 desired) {
      Vec3 forward = new Vec3(desired.x, 0.0, desired.z);
      if (forward.lengthSqr() < 1.0E-6) return;
      forward = forward.normalize();
      BlockPos base = mount.blockPosition();
      int broken = 0;
      for (int distance = 0; distance < 7 && broken < 96; distance++) {
         BlockPos check = base.offset((int)Math.round(forward.x * (distance + 1)), 0,
            (int)Math.round(forward.z * (distance + 1)));
         for (BlockPos pos : BlockPos.betweenClosed(check.offset(-2, -1, -2), check.offset(2, 4, 2))) {
            // The support below a ground mount is never destroyed by the NP.
            if (pos.getY() < base.getY()) continue;
            var state = level.getBlockState(pos);
            if (!state.isAir() && !state.is(Blocks.BEDROCK)
               && state.getDestroySpeed(level, pos) >= 0.0F && state.getDestroySpeed(level, pos) < 75.0F) {
               level.removeBlock(pos, false);
               if (++broken >= 96) break;
            }
         }
      }
   }

   private static void rememberNpSafePosition(CompoundTag data, ZhaoYunHakuryuEntity mount) {
      data.putDouble(TAG_NP_SAFE_X, mount.getX());
      data.putDouble(TAG_NP_SAFE_Y, mount.getY());
      data.putDouble(TAG_NP_SAFE_Z, mount.getZ());
   }

   private static void restoreNpSafePosition(CompoundTag data, ZhaoYunHakuryuEntity mount) {
      if (!data.contains(TAG_NP_SAFE_X) || !data.contains(TAG_NP_SAFE_Y) || !data.contains(TAG_NP_SAFE_Z)) {
         mount.setDeltaMovement(Vec3.ZERO);
         return;
      }
      mount.teleportTo(data.getDouble(TAG_NP_SAFE_X), data.getDouble(TAG_NP_SAFE_Y), data.getDouble(TAG_NP_SAFE_Z));
      mount.setDeltaMovement(Vec3.ZERO);
      mount.fallDistance = 0.0F;
   }

   private static boolean hasSolidSupport(ServerLevel level, ZhaoYunHakuryuEntity mount) {
      double y = mount.getY() - 0.08;
      for (double xOffset : new double[]{-0.75, 0.0, 0.75}) {
         for (double zOffset : new double[]{-0.75, 0.0, 0.75}) {
            BlockPos below = BlockPos.containing(mount.getX() + xOffset, y, mount.getZ() + zOffset);
            if (level.getBlockState(below).isSolidRender(level, below)) return true;
         }
      }
      return false;
   }

   private static Vec3 initialChargeDirection(CompoundTag data) {
      Vec3 direction = new Vec3(data.getDouble(TAG_NP_INITIAL_DIR_X), 0.0,
         data.getDouble(TAG_NP_INITIAL_DIR_Z));
      return direction.lengthSqr() < 1.0E-4
         ? new Vec3(0.0, 0.0, 1.0)
         : direction.normalize();
   }

   private static double cardMovementSpeedRatio(ServerPlayer player) {
      double base = Math.max(0.1, player.getAttributeBaseValue(Attributes.MOVEMENT_SPEED));
      return Math.max(0.35, player.getAttributeValue(Attributes.MOVEMENT_SPEED) / base);
   }

   private static void hitMountTargets(ServerPlayer player, ZhaoYunHakuryuEntity mount, double inflate, float damage) {
      if (!(player.level() instanceof ServerLevel level)) return;
      AABB area = mount.getBoundingBox().inflate(inflate, 1.4, inflate);
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
         e -> e != player && e != mount && isEnemy(player, e))) {
         String key = "ZhaoYunNpHit_" + target.getUUID();
         if (player.getPersistentData().getBoolean(key)) continue;
         player.getPersistentData().putBoolean(key, true);
         target.invulnerableTime = 0;
         // Use the regular damage pipeline so Heracles' Twelve Trials,
         // Battle Continuation, servant dodge, and damage immunities all
         // resolve correctly instead of being bypassed by setHealth().
         target.hurt(player.damageSources().mobAttack(player), damage);
         target.invulnerableTime = 0;
         level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.55,
            target.getZ(), damage >= 200.0F ? 28 : 14, 0.35, 0.45, 0.35, 0.04);
         if (damage >= 200.0F) {
            level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 0.5, target.getZ(),
               3, 0.2, 0.25, 0.2, 0.0);
         }
      }
      for (String key : List.copyOf(player.getPersistentData().getAllKeys())) {
         if (key.startsWith("ZhaoYunNpHit_")) {
            UUID uuid;
            try { uuid = UUID.fromString(key.substring("ZhaoYunNpHit_".length())); } catch (Exception ignored) { continue; }
            Entity target = level.getEntity(uuid);
            if (target == null || !area.intersects(target.getBoundingBox().inflate(1.0))) player.getPersistentData().remove(key);
         }
      }
   }

   private static void clearNpHitTags(CompoundTag data) {
      for (String key : List.copyOf(data.getAllKeys())) {
         if (key.startsWith("ZhaoYunNpHit_")) data.remove(key);
      }
   }

   private static void knockForwardEnemies(ServerPlayer player, Vec3 dir, double range, double power) {
      for (LivingEntity target : nearbyEnemies(player, range)) target.push(dir.x * power, 0.15, dir.z * power);
   }

   private static void spawnSlash(ServerPlayer player, Vec3 dir, net.minecraft.core.particles.SimpleParticleType particle) {
      if (player.level() instanceof ServerLevel level) {
         Vec3 at = player.position().add(dir.scale(2.0)).add(0.0, player.getBbHeight() * 0.55, 0.0);
         level.sendParticles(particle, at.x, at.y, at.z, 18, 0.4, 0.35, 0.4, 0.04);
      }
   }

   private static void spawnSkillBurst(ServerLevel level, Vec3 center, double radius, int count) {
      level.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z,
         count, radius, radius * 0.55, radius, 0.08);
      level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z,
         Math.max(6, count / 2), radius * 0.7, radius * 0.75, radius * 0.7, 0.04);
      level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z,
         Math.max(3, count / 4), radius * 0.55, radius * 0.35, radius * 0.55, 0.01);
   }

   private static List<LivingEntity> nearbyEnemies(ServerPlayer player, double radius) {
      return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
         e -> isEnemy(player, e));
   }

   /**
    * Dragon Gall uses a wider battlefield scan than ordinary skill targeting.
    * NPCs count when they are actually targeting Zhao Yun (or have exchanged
    * a hit); players count only after a combat exchange has occurred.
    */
   private static int dragonGallEnemyCount(ServerPlayer player, double radius) {
      return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
         entity -> isDragonGallEnemy(player, entity)).size();
   }

   private static int cachedDragonGallEnemyCount(ServerPlayer player, long now, double radius) {
      CompoundTag data = player.getPersistentData();
      long lastScan = data.getLong(TAG_DRAGON_GALL_SCAN_TICK);
      if (lastScan > 0L && now - lastScan < DRAGON_GALL_SCAN_INTERVAL) {
         return Math.max(0, data.getInt(TAG_DRAGON_GALL_ENEMY_COUNT));
      }
      int count = dragonGallEnemyCount(player, radius);
      data.putLong(TAG_DRAGON_GALL_SCAN_TICK, now);
      data.putInt(TAG_DRAGON_GALL_ENEMY_COUNT, count);
      return count;
   }

   private static boolean isDragonGallEnemy(ServerPlayer player, LivingEntity entity) {
      if (!isEnemy(player, entity)) return false;
      if (entity instanceof Player other) {
         return player.getLastHurtMob() == other
            || player.getLastHurtByMob() == other
            || other.getLastHurtMob() == player
            || other.getLastHurtByMob() == player;
      }
      if (entity instanceof net.minecraft.world.entity.Mob mob) {
         return mob.getTarget() == player
            || player.getLastHurtMob() == entity
            || player.getLastHurtByMob() == entity;
      }
      return player.getLastHurtMob() == entity || player.getLastHurtByMob() == entity;
   }

   private static boolean isEnemy(ServerPlayer player, LivingEntity entity) {
      return entity != player && entity.isAlive() && !EntityUtils.isImmunePlayerTarget(entity)
         && entity.getVehicle() != player.getVehicle()
         && !ServantMasterTargeting.isContractMaster(player, entity)
         && !player.isAlliedTo(entity) && !entity.isAlliedTo(player);
   }

   private static CompoundTag data(ServerPlayer player) { return player.getPersistentData(); }
   private static net.minecraft.resources.ResourceLocation id(String path) {
      return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, path);
   }

   private static ZhaoYunHakuryuEntity getMount(ServerPlayer player, String tag) {
      CompoundTag data = player.getPersistentData();
      if (!data.hasUUID(tag) || !(player.level() instanceof ServerLevel level)) return null;
      Entity entity = level.getEntity(data.getUUID(tag));
      return entity instanceof ZhaoYunHakuryuEntity mount && mount.isAlive() ? mount : null;
   }

   private static void discardMount(ServerPlayer player, String tag) {
      CompoundTag data = player.getPersistentData();
      if (data.hasUUID(tag) && player.level() instanceof ServerLevel level) {
         Entity entity = level.getEntity(data.getUUID(tag));
         if (entity != null) {
            entity.ejectPassengers();
            entity.discard();
         }
      }
      data.remove(tag);
   }

   public static boolean isNpActive(ServerPlayer player) {
      return player.getPersistentData().getLong(TAG_NP_UNTIL) > player.level().getGameTime();
   }
}
