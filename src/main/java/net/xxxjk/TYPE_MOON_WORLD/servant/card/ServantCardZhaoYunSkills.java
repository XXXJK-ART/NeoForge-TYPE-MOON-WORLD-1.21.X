package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
   public static final String TAG_NP_CHANT_UNTIL = "ServantCardZhaoYunNpChantUntil";
   public static final String TAG_NP_MOUNT_UUID = "ServantCardZhaoYunNpMount";
   public static final String TAG_NP_UNTIL = "ServantCardZhaoYunNpUntil";
   public static final String TAG_NP_INITIAL_DISTANCE = "ServantCardZhaoYunNpInitialDistance";
   public static final String TAG_NP_INITIAL_DONE = "ServantCardZhaoYunNpInitialDone";
   public static final String TAG_SKILL_MOUNT_UUID = "ServantCardZhaoYunSkillMount";
   public static final String TAG_SKILL_MOUNT_COOLDOWN = "ServantCardZhaoYunSkillMountCooldown";

   private static final net.minecraft.resources.ResourceLocation DRAGON_GALL_ATTACK =
      net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_zhao_yun_dragon_gall_attack");
   private static final net.minecraft.resources.ResourceLocation DRAGON_GALL_SPEED =
      net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_zhao_yun_dragon_gall_speed");
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
      tick(player, player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES));
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!player.isAlive() || !vars.servant_card_transformed || !SERVANT_ID.equals(vars.servant_card_id)) {
         clear(player);
         return;
      }
      CompoundTag data = player.getPersistentData();
      long now = player.level().getGameTime();
      int enemies = nearbyEnemies(player, 8.0).size();
      boolean low = player.getHealth() <= player.getMaxHealth() * 0.5F;

      // 龙胆 EX: permanent, non-slot passive.
      if (low) {
         ServantCardSkillUtils.addOrReplaceMultiplied(player.getAttribute(Attributes.ATTACK_DAMAGE), DRAGON_GALL_ATTACK, 0.20);
         ServantCardSkillUtils.addOrReplaceMultiplied(player.getAttribute(Attributes.MOVEMENT_SPEED), DRAGON_GALL_SPEED, 0.15);
      } else {
         ServantCardSkillUtils.remove(player.getAttribute(Attributes.ATTACK_DAMAGE), DRAGON_GALL_ATTACK);
         ServantCardSkillUtils.remove(player.getAttribute(Attributes.MOVEMENT_SPEED), DRAGON_GALL_SPEED);
      }
      if (enemies >= 2) {
         ServantCardSkillUtils.addOrReplaceMultiplied(player.getAttribute(Attributes.ATTACK_DAMAGE), SURROUNDED_ATTACK, 0.25);
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
         data.putInt(TAG_BREAKTHROUGH, Math.min(7, data.getInt(TAG_BREAKTHROUGH) + 1));
      }
      int stacks = Math.max(0, Math.min(7, data.getInt(TAG_BREAKTHROUGH)));
      if (stacks > 0) {
         ServantCardSkillUtils.addOrReplaceMultiplied(player.getAttribute(Attributes.MOVEMENT_SPEED), id("servant_card_zhao_yun_breakthrough_speed"), stacks * 0.03);
      } else {
         ServantCardSkillUtils.remove(player.getAttribute(Attributes.MOVEMENT_SPEED), id("servant_card_zhao_yun_breakthrough_speed"));
      }
      if (stacks >= 7) {
         player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
         player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 4, 1, false, false, false));
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
      ServantCardSkillUtils.remove(player.getAttribute(Attributes.ATTACK_DAMAGE), SURROUNDED_ATTACK);
      ServantCardSkillUtils.remove(player.getAttribute(Attributes.MOVEMENT_SPEED), DESPERATION_SPEED);
      ServantCardSkillUtils.remove(player.getAttribute(Attributes.ATTACK_DAMAGE), DESPERATION_ATTACK);
      ServantCardSkillUtils.remove(player.getAttribute(Attributes.MOVEMENT_SPEED), id("servant_card_zhao_yun_breakthrough_speed"));
      data.remove(TAG_BREAKTHROUGH);
      data.remove("ServantCardZhaoYunLastGuard");
      data.remove("ServantCardZhaoYunDesperationUntil");
      data.remove(TAG_RESCUE_UNTIL);
      data.remove(TAG_RESCUE_TARGET);
      data.remove(TAG_QINGGANG_UNTIL);
      data.remove(TAG_QINGGANG_TARGETS);
      data.remove(TAG_NP_CHANT_UNTIL);
      data.remove(TAG_NP_UNTIL);
      data.remove(TAG_NP_INITIAL_DISTANCE);
      data.remove(TAG_NP_INITIAL_DONE);
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
               level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + dir.x * 1.8, player.getY() + 0.8, player.getZ() + dir.z * 1.8, 4, 0, 0, 0, 0);
            }
         });
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
      player.setDeltaMovement(dir.scale(0.9).add(0.0, Math.max(0.0, player.getDeltaMovement().y), 0.0));
      player.hurtMarked = true;
      ServantCardSkillUtils.hitForwardArc(player, dir, 5.0, (float)(player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.3 + 8.0));
      knockForwardEnemies(player, dir, 5.0, 0.9);
      spawnSlash(player, dir, ParticleTypes.CRIT);
      return true;
   }

   public static boolean performRescue(ServerPlayer player) {
      LivingEntity target = findFriendlyLookTarget(player, 12.0);
      if (target == null) return false;
      long until = player.level().getGameTime() + 12L * 20L;
      CompoundTag data = player.getPersistentData();
      data.putLong(TAG_RESCUE_UNTIL, until);
      data.putUUID(TAG_RESCUE_TARGET, target.getUUID());
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 12 * 20, 1, false, true, true));
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
      return true;
   }

   public static boolean performDragonFlash(ServerPlayer player) {
      Vec3 dir = PlayerNoblePhantasmHelper.horizontalLook(player);
      ServantCardSkillUtils.hitForwardArc(player, dir, 7.0, (float)(player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 2.0 + 18.0));
      spawnSlash(player, dir, ParticleTypes.END_ROD);
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
      level.sendParticles(ParticleTypes.EXPLOSION, source.getX(), source.getY() + 0.2, source.getZ(), 6, 1.4, 0.15, 1.4, 0.0);
      return true;
   }

   public static boolean performQinggang(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      data.putLong(TAG_QINGGANG_UNTIL, player.level().getGameTime() + 15L * 20L);
      data.remove(TAG_QINGGANG_TARGETS);
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 0.7, player.getZ(), 20, 0.5, 0.7, 0.5, 0.05);
      }
      return true;
   }

   public static boolean performChangbanpo(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      long now = player.level().getGameTime();
      data.putLong(TAG_NP_CHANT_UNTIL, now + 3L * 20L);
      data.remove(TAG_NP_UNTIL);
      data.remove(TAG_NP_INITIAL_DONE);
      data.putDouble(TAG_NP_INITIAL_DISTANCE, 0.0);
      player.level().playSound(null, player.blockPosition(), ModSounds.ZHAO_YUN_VOICE_NP.get(), SoundSource.VOICE, 1.0F, 1.0F);
      return true;
   }

   public static void onZhaoYunAttack(ServerPlayer player, LivingEntity target) {
      if (!isQinggangActive(player) || target == null || !target.isAlive() || !isEnemy(player, target)) return;
      TYPE_MOON_WORLD.queueServerWork(10, () -> {
         if (!player.isAlive() || !target.isAlive() || !isQinggangActive(player)) return;
         target.invulnerableTime = 0;
         double currentAttack = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
         double baseAttack = Math.max(1.0, player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE));
         target.hurt(player.damageSources().magic(), (float)(15.0 * Math.max(1.0, currentAttack / baseAttack)));
         target.invulnerableTime = 0;
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
      data.putInt(TAG_BREAKTHROUGH, Math.min(7, Math.max(0, data.getInt(TAG_BREAKTHROUGH)) + 1));
   }

   private static void tickRescue(ServerPlayer player, long now) {
      CompoundTag data = player.getPersistentData();
      if (data.getLong(TAG_RESCUE_UNTIL) <= now) {
         data.remove(TAG_RESCUE_UNTIL);
         data.remove(TAG_RESCUE_TARGET);
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
            mount.setNpActive(true);
            player.startRiding(mount, true);
            if (player.level() instanceof ServerLevel level) {
               VFXServerEffects.spawn(level, "servant_zhao_yun_changbanpo", player, 96.0);
            }
         }
      }
      if (data.getLong(TAG_NP_UNTIL) > now) {
         ZhaoYunHakuryuEntity mount = getMount(player, TAG_NP_MOUNT_UUID);
         if (mount != null) {
            double distance = data.getDouble(TAG_NP_INITIAL_DISTANCE);
            boolean opening = !data.getBoolean(TAG_NP_INITIAL_DONE);
            Vec3 dir = PlayerNoblePhantasmHelper.horizontalLook(player);
            double speed = opening ? 1.0 : 0.65;
            double step = Math.min(speed, opening ? 50.0 - distance : speed);
            moveMountHorizontally(mount, dir, step);
            if (opening) {
               data.putDouble(TAG_NP_INITIAL_DISTANCE, distance + step);
               if (distance + step >= 50.0) data.putBoolean(TAG_NP_INITIAL_DONE, true);
            }
            hitMountTargets(player, mount, opening ? 2.6 : 2.3, opening ? 500.0F : 100.0F);
            if (player.tickCount % 3 == 0 && player.level() instanceof ServerLevel level) {
               level.sendParticles(ParticleTypes.END_ROD, mount.getX(), mount.getY() + 1.0, mount.getZ(), 20, 1.0, 0.5, 1.0, 0.03);
               level.sendParticles(ParticleTypes.CLOUD, mount.getX(), mount.getY() + 0.2, mount.getZ(), 12, 0.8, 0.1, 0.8, 0.03);
            }
         } else {
            data.remove(TAG_NP_UNTIL);
            data.remove(TAG_NP_INITIAL_DISTANCE);
            data.remove(TAG_NP_INITIAL_DONE);
            clearNpHitTags(data);
            data.remove(TAG_NP_MOUNT_UUID);
         }
      } else if (data.getLong(TAG_NP_UNTIL) > 0L) {
         data.remove(TAG_NP_UNTIL);
         data.remove(TAG_NP_INITIAL_DISTANCE);
         data.remove(TAG_NP_INITIAL_DONE);
         clearNpHitTags(data);
         discardMount(player, TAG_NP_MOUNT_UUID);
      }
   }

   private static ZhaoYunHakuryuEntity spawnNpMount(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return null;
      ZhaoYunHakuryuEntity mount = ModEntities.ZHAO_YUN_HAKURYU.get().create(level);
      if (mount == null) return null;
      mount.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
      mount.setHealth(mount.getMaxHealth());
      mount.bindSkillOwner(player);
      if (!level.addFreshEntity(mount)) return null;
      player.getPersistentData().putUUID(TAG_NP_MOUNT_UUID, mount.getUUID());
      return mount;
   }

   private static void moveMountHorizontally(ZhaoYunHakuryuEntity mount, Vec3 dir, double distance) {
      if (!(mount.level() instanceof ServerLevel level)) return;
      Vec3 delta = new Vec3(dir.x, 0.0, dir.z).normalize().scale(distance);
      BlockPos check = mount.blockPosition().offset((int)Math.round(delta.x), 0, (int)Math.round(delta.z));
      if (level.getBlockState(check).is(Blocks.BEDROCK)) return;
      BlockPos min = BlockPos.containing(mount.getX() + Math.min(0.0, delta.x) - 1.5, mount.getY(), mount.getZ() + Math.min(0.0, delta.z) - 1.5);
      BlockPos max = BlockPos.containing(mount.getX() + Math.max(0.0, delta.x) + 1.5, mount.getY() + 2.4, mount.getZ() + Math.max(0.0, delta.z) + 1.5);
      for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
         var state = level.getBlockState(pos);
         if (!state.isAir() && !state.is(Blocks.BEDROCK) && state.getDestroySpeed(level, pos) >= 0.0F) {
            level.removeBlock(pos, false);
         }
      }
      mount.setYRot((float)(Math.atan2(-dir.x, dir.z) * 180.0 / Math.PI));
      mount.move(net.minecraft.world.entity.MoverType.SELF, delta);
      mount.setDeltaMovement(0.0, mount.getDeltaMovement().y, 0.0);
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
         target.hurt(player.damageSources().playerAttack(player), damage);
         target.invulnerableTime = 0;
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

   private static LivingEntity findFriendlyLookTarget(ServerPlayer player, double range) {
      LivingEntity best = null;
      double bestDistance = Double.MAX_VALUE;
      AABB box = player.getBoundingBox().expandTowards(player.getLookAngle().scale(range)).inflate(1.8);
      for (LivingEntity candidate : player.level().getEntitiesOfClass(LivingEntity.class, box,
         e -> e != player && e.isAlive() && !isEnemy(player, e))) {
         double distance = player.distanceToSqr(candidate);
         if (distance < bestDistance && player.getLookAngle().dot(candidate.position().subtract(player.getEyePosition()).normalize()) > 0.35) {
            best = candidate;
            bestDistance = distance;
         }
      }
      return best;
   }

   private static List<LivingEntity> nearbyEnemies(ServerPlayer player, double radius) {
      return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
         e -> isEnemy(player, e));
   }

   private static boolean isEnemy(ServerPlayer player, LivingEntity entity) {
      return entity != player && entity.isAlive() && !EntityUtils.isImmunePlayerTarget(entity)
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
         if (entity != null) entity.discard();
      }
      data.remove(tag);
   }

   public static boolean isNpActive(ServerPlayer player) {
      return player.getPersistentData().getLong(TAG_NP_UNTIL) > player.level().getGameTime();
   }
}
