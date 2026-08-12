package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.Comparator;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.EmiyaThrownWeaponEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshGateWeaponProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.LancelotWeaponItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantSprintCollisionHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.lancelot.LancelotCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactService;
import org.joml.Vector3f;

public final class ServantCardLancelotBerserkerSkills {
   private static final String LAST_INTERCEPT_TAG = "ServantCardLancelotLastProjectileIntercept";
   private static final String LAST_SPRINT_COLLISION_TAG = "ServantCardLancelotLastSprintCollisionBreak";
   private static final String MANA_REVERSAL_UNTIL_TAG = "ServantCardLancelotManaReversalUntil";
   private static final String LAST_DRAIN_TICK_TAG = "ServantCardLancelotAroundightDrainTick";
   private static final ResourceLocation AROUNDIGHT_DAMAGE_ID = id("servant_card_lancelot_aroundight_damage");
   private static final ResourceLocation AROUNDIGHT_HEALTH_ID = id("servant_card_lancelot_aroundight_health");
   private static final ResourceLocation AROUNDIGHT_ARMOR_ID = id("servant_card_lancelot_aroundight_armor");
   private static final ResourceLocation AROUNDIGHT_SPEED_ID = id("servant_card_lancelot_aroundight_speed");
   private static final ResourceLocation AROUNDIGHT_ATTACK_SPEED_ID = id("servant_card_lancelot_aroundight_attack_speed");
   private static final ResourceLocation FAIRY_ARMOR_ID = id("servant_card_lancelot_fairy_armor");
   private static final ResourceLocation FAIRY_TOUGHNESS_ID = id("servant_card_lancelot_fairy_toughness");
   private static final ResourceLocation MANA_REVERSAL_DAMAGE_ID = id("servant_card_lancelot_mana_reversal_damage");
   private static final DustParticleOptions DARK_DUST = new DustParticleOptions(new Vector3f(0.03F, 0.02F, 0.04F), 1.35F);
   private static final DustParticleOptions PURPLE_DUST = new DustParticleOptions(new Vector3f(0.28F, 0.08F, 0.48F), 1.05F);

   private ServantCardLancelotBerserkerSkills() {
   }

   public static boolean isActiveCard(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.servant_card_transformed && "lancelot_berserker".equals(vars.servant_card_id);
   }

   public static boolean isAroundightActive(ServerPlayer player) {
      return isActiveCard(player) && LancelotCombatHelper.isAroundightMode(player);
   }

   public static void initialize(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      clear(player, vars);
      vars.servant_card_action_mode = 0;
      vars.syncPlayerVariables(player);
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!"lancelot_berserker".equals(vars.servant_card_id)) {
         clear(player, vars);
         return;
      }
      long now = player.level().getGameTime();
      if (LancelotCombatHelper.isUnable(player, now)) {
         player.setDeltaMovement(player.getDeltaMovement().scale(0.35));
         return;
      }
      tickLancelotSprintCollisionBreak(player);
      tickAroundight(player, vars, now);
      tickFairyBlessing(player);
      tickManaReversal(player, now);
      if (!LancelotCombatHelper.isAroundightMode(player)) {
         maybeInterceptProjectile(player, now);
      }
      if (player.level() instanceof ServerLevel level) {
         if (LancelotCombatHelper.isAroundightMode(player)) {
            if ((player.tickCount & 3) == 0) level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(), 4, 0.45, 0.62, 0.45, 0.03);
         } else if ((player.tickCount & 3) == 0) {
            level.sendParticles(DARK_DUST, player.getX(), player.getY() + 0.9, player.getZ(), 14, 0.62, 0.8, 0.62, 0.055);
            level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 0.85, player.getZ(), 4, 0.5, 0.62, 0.5, 0.035);
         }
      }
   }

   public static void clear(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      CompoundTag data = player.getPersistentData();
      data.remove(LAST_INTERCEPT_TAG);
      data.remove(LAST_SPRINT_COLLISION_TAG);
      data.remove(MANA_REVERSAL_UNTIL_TAG);
      data.remove(LAST_DRAIN_TICK_TAG);
      data.remove(LancelotCombatHelper.AROUNDIGHT_MODE_TAG);
      data.remove(LancelotCombatHelper.AROUNDIGHT_DRAIN_TICK_TAG);
      data.remove(LancelotCombatHelper.UNABLE_UNTIL_TAG);
      removeAroundightModifiers(player);
      clearFairyBlessing(player);
      ServantCardSkillUtils.remove(player.getAttribute(Attributes.ATTACK_DAMAGE), MANA_REVERSAL_DAMAGE_ID);
      if (vars != null && "lancelot_berserker".equals(vars.servant_card_id)) {
         vars.servant_card_action_mode = 0;
      }
   }

   private static void tickLancelotSprintCollisionBreak(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      ServantSprintCollisionHelper.tryPlayerSprintCollision(
         player,
         level,
         player.getPersistentData(),
         LAST_SPRINT_COLLISION_TAG,
         false,
         10.0F,
         1.25,
         0.26,
         32,
         45.0F
      );
   }

   public static void performMaulCombo(ServerPlayer player) {
      Vec3 dir = PlayerNoblePhantasmHelper.horizontalLook(player);
      player.setDeltaMovement(player.getDeltaMovement().add(dir.x * 0.55, 0.04, dir.z * 0.55));
      player.hurtMarked = true;
      strikeArc(player, 4.1, 22.0F, 0.20, 3, 0.75);
      TYPE_MOON_WORLD.queueServerWork(5, () -> {
         if (player.isAlive()) strikeArc(player, 3.8, 19.0F, -0.15, 3, 0.55);
      });
      swingAndFx(player, dir, true);
   }

   public static void performFeralRush(ServerPlayer player) {
      Vec3 dir = PlayerNoblePhantasmHelper.horizontalLook(player);
      player.setSprinting(true);
      player.setDeltaMovement(player.getDeltaMovement().add(dir.x * 1.85, 0.12, dir.z * 1.85));
      player.hurtMarked = true;
      player.fallDistance = 0.0F;
      strikeArc(player, 4.8, 28.0F, 0.35, 2, 1.2);
      if (player.level() instanceof ServerLevel level) {
         tryFeralRushCollision(player, level, dir);
         for (int delay : new int[]{1, 2, 3, 4}) {
            TYPE_MOON_WORLD.queueServerWork(delay, () -> {
               if (player.isAlive() && isActiveCard(player) && player.level() instanceof ServerLevel delayedLevel) {
                  tryFeralRushCollision(player, delayedLevel, dir);
               }
            });
         }
         level.sendParticles(DARK_DUST, player.getX(), player.getY() + 0.55, player.getZ(), 34, 0.4, 0.35, 0.4, 0.11);
         level.playSound(null, player.blockPosition(), SoundEvents.RAVAGER_ATTACK, SoundSource.PLAYERS, 0.85F, 0.72F);
      }
   }

   public static void performGroundSlam(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return;
      double radius = LancelotCombatHelper.isAroundightMode(player) ? 5.6 : 4.6;
      damageRadius(player, radius, LancelotCombatHelper.isAroundightMode(player) ? 38.0F : 30.0F, 1.25, 0.44);
      TerrainImpactService.impact(level, player, player.position().add(0.0, 0.18, 0.0),
         TerrainImpactProfile.of(TerrainImpactProfile.Tier.MEDIUM), TerrainImpactService.Shape.UPPER_SURFACE_CRATER);
      level.sendParticles(DARK_DUST, player.getX(), player.getY() + 0.35, player.getZ(), 72, radius * 0.35, 0.2, radius * 0.35, 0.1);
      level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.25F, 0.48F);
      player.swing(InteractionHand.MAIN_HAND, true);
   }

   public static void performHuntStep(ServerPlayer player) {
      LivingEntity target = nearestTarget(player, 10.0);
      Vec3 dir = PlayerNoblePhantasmHelper.horizontalLook(player);
      if (target != null) {
         Vec3 fromTarget = player.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
         if (fromTarget.lengthSqr() < 1.0E-4) fromTarget = dir.scale(-1.0);
         Vec3 side = new Vec3(-fromTarget.z, 0.0, fromTarget.x).normalize().scale(player.getRandom().nextBoolean() ? 1.7 : -1.7);
         Vec3 destination = target.position().add(fromTarget.normalize().scale(1.65)).add(side).add(0.0, 0.1, 0.0);
         ServantCardSkillUtils.trySafeHorizontalTeleport(player, destination);
         dir = target.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
         if (dir.lengthSqr() > 1.0E-4) dir = dir.normalize();
      } else {
         ServantCardSkillUtils.trySafeHorizontalTeleport(player, player.position().add(dir.scale(4.2)).add(0.0, 0.1, 0.0));
      }
      strikeArc(player, 3.6, 18.0F, 0.05, 2, 0.65);
      swingAndFx(player, dir, false);
   }

   public static void performManaReversal(ServerPlayer player) {
      long until = player.level().getGameTime() + 12L * 20L;
      player.getPersistentData().putLong(MANA_REVERSAL_UNTIL_TAG, until);
      ServantCardSkillUtils.addOrReplaceMultiplied(player.getAttribute(Attributes.ATTACK_DAMAGE), MANA_REVERSAL_DAMAGE_ID, 0.20);
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 12 * 20, 0, false, true, true));
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(DARK_DUST, player.getX(), player.getY() + 1.0, player.getZ(), 46, 0.58, 0.72, 0.58, 0.09);
         level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.75F, 0.58F);
      }
   }

   public static void performBerserkRoar(ServerPlayer player) {
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 8 * 20, 1, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 8 * 20, 0, false, true, true));
      damageRadius(player, 5.8, 12.0F, 1.6, 0.28);
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 32, 0.95, 0.75, 0.95, 0.08);
         level.sendParticles(DARK_DUST, player.getX(), player.getY() + 0.85, player.getZ(), 58, 0.9, 0.72, 0.9, 0.1);
         level.playSound(null, player.blockPosition(), ModSounds.LANCELOT_BERSERKER_VOICE_ROAR.get(), SoundSource.VOICE, 1.25F, 0.86F);
      }
   }

   public static boolean performKnightOfOwner(ServerPlayer player) {
      if (LancelotCombatHelper.isAroundightMode(player)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.lancelot.aroundight_seals_owner"), true);
         return false;
      }
      ItemStack main = player.getMainHandItem();
      if (main.isEmpty()) {
         equipKnightRod(player);
      } else if (LancelotCombatHelper.canOwnerize(main)) {
         player.setItemInHand(InteractionHand.MAIN_HAND, LancelotCombatHelper.knightOfOwnerStack(main, player));
      } else {
         return false;
      }
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(DARK_DUST, player.getX(), player.getY() + 0.95, player.getZ(), 36, 0.5, 0.6, 0.5, 0.08);
         level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.65F, 0.55F);
      }
      return true;
   }

   public static boolean tryThrowKnightOfOwnerItem(ServerPlayer player, InteractionHand hand) {
      if (hand == null || !isActiveCard(player) || !LancelotCombatHelper.canUseKnightOfOwner(player)) return false;
      if (!(player.level() instanceof ServerLevel level)) return false;
      ItemStack held = player.getItemInHand(hand);
      if (held.isEmpty() || !LancelotCombatHelper.isKnightOfOwner(held)) return false;

      ItemStack thrown = held.copy();
      thrown.setCount(1);
      LancelotCombatHelper.knightOfOwnerStack(thrown, player);
      Vec3 start = player.getEyePosition().add(player.getLookAngle().scale(0.55));
      Vec3 dir = aimedThrowDirection(player, start);

      EmiyaThrownWeaponEntity projectile = new EmiyaThrownWeaponEntity(level, player, thrown);
      projectile.setPos(start);
      projectile.setFixedDamage((float)Math.max(18.0, player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.45));
      projectile.setNoGravity(true);
      projectile.setPiercingImpact(true);
      projectile.shoot(dir.x, dir.y + 0.03, dir.z, 2.45F, 0.0F);
      projectile.alignPoseToMotion();
      level.addFreshEntity(projectile);

      held.shrink(1);
      player.setItemInHand(hand, held.isEmpty() ? ItemStack.EMPTY : held);
      player.swing(hand, true);
      level.sendParticles(DARK_DUST, player.getX(), player.getY() + 0.95, player.getZ(), 24, 0.45, 0.55, 0.45, 0.07);
      level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 0.9F, 0.72F);
      return true;
   }

   private static Vec3 aimedThrowDirection(ServerPlayer player, Vec3 start) {
      HitResult hit = EntityUtils.getRayTraceTarget(player, 64.0);
      if (hit != null && hit.getType() != HitResult.Type.MISS) {
         Vec3 aimed = hit.getLocation().subtract(start);
         if (aimed.lengthSqr() > 1.0E-4) {
            return aimed;
         }
      }
      Vec3 look = player.getLookAngle();
      return look.lengthSqr() > 1.0E-4 ? look : new Vec3(0.0, 0.0, 1.0);
   }

   public static boolean performAroundight(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      CompoundTag data = player.getPersistentData();
      if (LancelotCombatHelper.isAroundightMode(player)) {
         stopAroundight(player, false);
         return true;
      }
      long now = player.level().getGameTime();
      if (LancelotCombatHelper.isUnable(player, now)) return false;
      data.putBoolean(LancelotCombatHelper.AROUNDIGHT_MODE_TAG, true);
      data.putLong(LAST_DRAIN_TICK_TAG, now + LancelotCombatHelper.AROUNDIGHT_DRAIN_INTERVAL);
      ItemStack sword = LancelotCombatHelper.knightOfOwnerStack(new ItemStack(ModItems.AROUNDIGHT.get()), player);
      ServantCardTransformManager.markGeneratedItem(sword, true, false);
      player.setItemInHand(InteractionHand.MAIN_HAND, sword);
      player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
      applyAroundightModifiers(player);
      vars.servant_card_action_mode = 1;
      if (player.level() instanceof ServerLevel level) {
         level.playSound(null, player.blockPosition(), ModSounds.LANCELOT_BERSERKER_VOICE_NP.get(), SoundSource.VOICE, 1.25F, 0.95F);
         level.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 1.0, player.getZ(), 1, 0.2, 0.2, 0.2, 0.0);
         level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(), 46, 0.72, 0.82, 0.72, 0.18);
      }
      return true;
   }

   private static void tickAroundight(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, long now) {
      if (!LancelotCombatHelper.isAroundightMode(player)) {
         removeAroundightModifiers(player);
         return;
      }
      if (!isHoldingAroundight(player)) {
         stopAroundight(player, false);
         return;
      }
      applyAroundightModifiers(player);
      CompoundTag data = player.getPersistentData();
      if (now < data.getLong(LAST_DRAIN_TICK_TAG)) return;
      data.putLong(LAST_DRAIN_TICK_TAG, now + LancelotCombatHelper.AROUNDIGHT_DRAIN_INTERVAL);
      if (!ServantCardManaService.consumeSilently(player, vars, LancelotCombatHelper.AROUNDIGHT_DRAIN_MP)) {
         stopAroundight(player, true);
         return;
      }
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(PURPLE_DUST, player.getX(), player.getY() + 1.05, player.getZ(), 12, 0.55, 0.65, 0.55, 0.04);
      }
   }

   private static void stopAroundight(ServerPlayer player, boolean unable) {
      CompoundTag data = player.getPersistentData();
      data.remove(LancelotCombatHelper.AROUNDIGHT_MODE_TAG);
      data.remove(LAST_DRAIN_TICK_TAG);
      removeAroundightModifiers(player);
      if (isHoldingAroundight(player)) {
         player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
      }
      if (unable) {
         data.putLong(LancelotCombatHelper.UNABLE_UNTIL_TAG, player.level().getGameTime() + LancelotCombatHelper.UNABLE_DURATION_TICKS);
         if (player.level() instanceof ServerLevel level) {
            level.playSound(null, player.blockPosition(), ModSounds.LANCELOT_BERSERKER_VOICE_FAIL.get(), SoundSource.VOICE, 1.0F, 0.82F);
         }
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.servant_card_action_mode = 0;
      vars.syncPlayerVariables(player);
   }

   private static void strikeArc(ServerPlayer player, double range, float damage, double minDot, int maxHits, double knockback) {
      if (!(player.level() instanceof ServerLevel level)) return;
      Vec3 origin = player.position().add(0.0, player.getBbHeight() * 0.52, 0.0);
      Vec3 forward = PlayerNoblePhantasmHelper.horizontalLook(player);
      int[] hits = {0};
      level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range, 2.5, range), target -> validTarget(player, target)).stream()
         .sorted(Comparator.comparingDouble(player::distanceToSqr))
         .forEach(target -> {
            if (hits[0] >= maxHits) return;
            Vec3 to = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(origin);
            if (to.lengthSqr() > range * range) return;
            Vec3 flat = new Vec3(to.x, 0.0, to.z);
            if (flat.lengthSqr() > 1.0E-4 && flat.normalize().dot(forward) < minDot) return;
            target.invulnerableTime = 0;
            boolean hurt = target.hurt(player.damageSources().playerAttack(player), damage);
            target.invulnerableTime = 0;
            if (hurt || target.hurtTime > 0 || !target.isAlive()) {
               LancelotCombatHelper.applyWeaponHit(player, target, player.getMainHandItem());
               target.push(forward.x * knockback, 0.12, forward.z * knockback);
               target.hurtMarked = true;
               hits[0]++;
            }
         });
   }

   private static void damageRadius(ServerPlayer player, double radius, float damage, double knockback, double vertical) {
      if (!(player.level() instanceof ServerLevel level)) return;
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius, 2.1, radius), target -> validTarget(player, target))) {
         target.invulnerableTime = 0;
         target.hurt(player.damageSources().playerAttack(player), damage);
         target.invulnerableTime = 0;
         LancelotCombatHelper.applyWeaponHit(player, target, player.getMainHandItem());
         Vec3 away = target.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
         if (away.lengthSqr() < 1.0E-4) away = PlayerNoblePhantasmHelper.horizontalLook(player);
         away = away.normalize();
         target.push(away.x * knockback, vertical, away.z * knockback);
         target.hurtMarked = true;
      }
   }

   private static void maybeInterceptProjectile(ServerPlayer player, long now) {
      if (!(player.level() instanceof ServerLevel level) || now - player.getPersistentData().getLong(LAST_INTERCEPT_TAG) < LancelotCombatHelper.PROJECTILE_INTERCEPT_COOLDOWN) return;
      Entity projectile = level.getEntitiesOfClass(Entity.class, player.getBoundingBox().inflate(7.0), candidate -> isInterceptableProjectile(player, candidate)).stream()
         .min(Comparator.comparingDouble(player::distanceToSqr))
         .orElse(null);
      if (projectile == null) return;
      player.getPersistentData().putLong(LAST_INTERCEPT_TAG, now);
      LivingEntity target = nearestTarget(player, 24.0);
      GilgameshGateWeaponProjectileEntity gateProjectile = projectile instanceof GilgameshGateWeaponProjectileEntity gate ? gate : null;
      ItemStack stack = stackForProjectile(projectile, player);
      projectile.discard();
      if (target != null) {
         Vec3 start = player.getEyePosition().add(player.getLookAngle().scale(0.5));
         Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
         Vec3 dir = aim.subtract(start);
         if (gateProjectile != null) {
            LancelotCombatHelper.spawnKnightOfOwnerGateCounter(level, player, target, start, dir, gateProjectile);
         } else {
            EmiyaThrownWeaponEntity counter = new EmiyaThrownWeaponEntity(level, player, stack);
            counter.setPos(start);
            counter.setFixedDamage(28.0F);
            counter.setNoGravity(true);
            counter.setPiercingImpact(true);
            counter.shoot(dir.x, dir.y + 0.03, dir.z, 2.45F, 0.0F);
            counter.alignPoseToMotion();
            level.addFreshEntity(counter);
         }
      }
      level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.1, player.getZ(), 22, 0.55, 0.65, 0.55, 0.08);
      level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 0.55F);
   }

   private static boolean isInterceptableProjectile(ServerPlayer player, Entity candidate) {
      if (candidate == player || candidate == null || !candidate.isAlive()) return false;
      if (candidate instanceof GilgameshGateWeaponProjectileEntity gate) {
         LivingEntity owner = gate.getOwnerEntity();
         return owner != null && validProjectileOwner(player, owner);
      }
      if (candidate instanceof Projectile projectile) {
         Entity owner = projectile.getOwner();
         return owner instanceof LivingEntity living && validProjectileOwner(player, living);
      }
      return candidate instanceof ThrowableItemProjectile;
   }

   private static boolean validProjectileOwner(ServerPlayer player, LivingEntity owner) {
      return owner != player && owner.isAlive() && !owner.isAlliedTo(player) && !player.isAlliedTo(owner)
         && !ServantMasterTargeting.isContractMaster(player, owner) && !ServantMasterProtection.isProtectedMaster(player, owner);
   }

   private static ItemStack stackForProjectile(Entity projectile, ServerPlayer owner) {
      if (projectile instanceof ThrownTrident trident) {
         return LancelotCombatHelper.knightOfOwnerStack(trident.getPickupItemStackOrigin().copy(), owner);
      }
      if (projectile instanceof AbstractArrow arrow) {
         return LancelotCombatHelper.knightOfOwnerStack(arrow.getPickupItemStackOrigin().copy(), owner);
      }
      if (projectile instanceof ThrowableItemProjectile thrown) {
         ItemStack stack = thrown.getItem().copy();
         if (!stack.isEmpty()) return LancelotCombatHelper.knightOfOwnerStack(stack, owner);
      }
      if (projectile instanceof GilgameshGateWeaponProjectileEntity gate) {
         return LancelotCombatHelper.knightOfOwnerStack(LancelotCombatHelper.gilgameshWeaponStack(gate.getWeaponId()), owner);
      }
      return LancelotCombatHelper.knightOfOwnerStack(new ItemStack(ModItems.LANCELOT_IRON_ROD.get()), owner);
   }

   private static LivingEntity nearestTarget(ServerPlayer player, double range) {
      if (!(player.level() instanceof ServerLevel level)) return null;
      AABB area = player.getBoundingBox().inflate(range);
      return level.getEntitiesOfClass(LivingEntity.class, area, target -> validTarget(player, target)).stream()
         .min(Comparator.comparingDouble(player::distanceToSqr))
         .orElse(null);
   }

   private static boolean validTarget(ServerPlayer player, LivingEntity target) {
      return target != null && target != player && target.isAlive()
         && !EntityUtils.isImmunePlayerTarget(target)
         && !ServantMasterTargeting.isContractMaster(player, target)
         && !ServantMasterProtection.isProtectedMaster(player, target)
         && !player.isAlliedTo(target) && !target.isAlliedTo(player)
         && EntityUtils.isValidCombatTarget(player, target);
   }

   private static void tickFairyBlessing(ServerPlayer player) {
      boolean low = player.getHealth() <= player.getMaxHealth() * 0.5F;
      ServantCardSkillUtils.addOrReplaceMultiplied(player.getAttribute(Attributes.ARMOR), FAIRY_ARMOR_ID, low ? 0.24 : 0.12);
      ServantCardSkillUtils.addOrReplaceMultiplied(player.getAttribute(Attributes.ARMOR_TOUGHNESS), FAIRY_TOUGHNESS_ID, low ? 0.30 : 0.14);
      if (low && player.tickCount % 40 == 0) {
         player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 45, 0, false, true, true));
      }
   }

   private static void clearFairyBlessing(ServerPlayer player) {
      ServantCardSkillUtils.remove(player.getAttribute(Attributes.ARMOR), FAIRY_ARMOR_ID);
      ServantCardSkillUtils.remove(player.getAttribute(Attributes.ARMOR_TOUGHNESS), FAIRY_TOUGHNESS_ID);
   }

   private static void tickManaReversal(ServerPlayer player, long now) {
      if (player.getPersistentData().getLong(MANA_REVERSAL_UNTIL_TAG) > now) return;
      player.getPersistentData().remove(MANA_REVERSAL_UNTIL_TAG);
      ServantCardSkillUtils.remove(player.getAttribute(Attributes.ATTACK_DAMAGE), MANA_REVERSAL_DAMAGE_ID);
   }

   private static boolean isHoldingAroundight(ServerPlayer player) {
      ItemStack main = player.getMainHandItem();
      return main.getItem() instanceof LancelotWeaponItem weapon && weapon.weaponType() == LancelotWeaponItem.WeaponType.AROUNDIGHT;
   }

   private static void equipKnightRod(ServerPlayer player) {
      ItemStack rod = LancelotCombatHelper.knightOfOwnerStack(new ItemStack(ModItems.LANCELOT_IRON_ROD.get()), player);
      ServantCardTransformManager.markGeneratedItem(rod, true, false);
      player.setItemInHand(InteractionHand.MAIN_HAND, rod);
   }

   private static void tryFeralRushCollision(ServerPlayer player, ServerLevel level, Vec3 dir) {
      ServantSprintCollisionHelper.tryPlayerSprintCollision(
         player,
         level,
         player.getPersistentData(),
         LAST_SPRINT_COLLISION_TAG,
         false,
         26.0F,
         1.35,
         0.28,
         32,
         45.0F,
         dir
      );
   }

   private static void applyAroundightModifiers(ServerPlayer player) {
      syncAroundightHealthBeforeApply(player);
      double bonus = LancelotCombatHelper.AROUNDIGHT_CORE_ATTRIBUTE_MULTIPLIER - 1.0;
      ServantCardSkillUtils.addOrReplaceMultiplied(player.getAttribute(Attributes.ATTACK_DAMAGE), AROUNDIGHT_DAMAGE_ID, bonus);
      ServantCardSkillUtils.addOrReplaceMultiplied(player.getAttribute(Attributes.MAX_HEALTH), AROUNDIGHT_HEALTH_ID, bonus);
      ServantCardSkillUtils.addOrReplaceMultiplied(player.getAttribute(Attributes.ARMOR), AROUNDIGHT_ARMOR_ID, bonus);
      ServantCardSkillUtils.addOrReplaceMultiplied(player.getAttribute(Attributes.MOVEMENT_SPEED), AROUNDIGHT_SPEED_ID, bonus);
      ServantCardSkillUtils.addOrReplaceMultiplied(player.getAttribute(Attributes.ATTACK_SPEED), AROUNDIGHT_ATTACK_SPEED_ID, 0.75);
   }

   private static void removeAroundightModifiers(ServerPlayer player) {
      ServantCardSkillUtils.remove(player.getAttribute(Attributes.ATTACK_DAMAGE), AROUNDIGHT_DAMAGE_ID);
      ServantCardSkillUtils.remove(player.getAttribute(Attributes.MAX_HEALTH), AROUNDIGHT_HEALTH_ID);
      ServantCardSkillUtils.remove(player.getAttribute(Attributes.ARMOR), AROUNDIGHT_ARMOR_ID);
      ServantCardSkillUtils.remove(player.getAttribute(Attributes.MOVEMENT_SPEED), AROUNDIGHT_SPEED_ID);
      ServantCardSkillUtils.remove(player.getAttribute(Attributes.ATTACK_SPEED), AROUNDIGHT_ATTACK_SPEED_ID);
      player.getPersistentData().remove(LancelotCombatHelper.AROUNDIGHT_HEALTH_SYNCED_TAG);
      if (player.getHealth() > player.getMaxHealth()) {
         player.setHealth(player.getMaxHealth());
      }
   }

   private static void syncAroundightHealthBeforeApply(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      if (data.getBoolean(LancelotCombatHelper.AROUNDIGHT_HEALTH_SYNCED_TAG)) return;
      float oldHealth = player.getHealth();
      double oldMaxHealth = player.getMaxHealth();
      double bonus = LancelotCombatHelper.AROUNDIGHT_CORE_ATTRIBUTE_MULTIPLIER - 1.0;
      ServantCardSkillUtils.addOrReplaceMultiplied(player.getAttribute(Attributes.MAX_HEALTH), AROUNDIGHT_HEALTH_ID, bonus);
      if (oldMaxHealth > 0.0) {
         player.setHealth(Math.min(player.getMaxHealth(), (float)(oldHealth * player.getMaxHealth() / oldMaxHealth)));
      }
      data.putBoolean(LancelotCombatHelper.AROUNDIGHT_HEALTH_SYNCED_TAG, true);
   }

   private static void swingAndFx(ServerPlayer player, Vec3 dir, boolean heavy) {
      player.swing(InteractionHand.MAIN_HAND, true);
      if (!(player.level() instanceof ServerLevel level)) return;
      Vec3 forward = dir.lengthSqr() < 1.0E-4 ? PlayerNoblePhantasmHelper.horizontalLook(player) : dir.normalize();
      Vec3 fx = player.position().add(forward.scale(2.0)).add(0.0, player.getBbHeight() * 0.55, 0.0);
      level.sendParticles(ParticleTypes.SWEEP_ATTACK, fx.x, fx.y, fx.z, heavy ? 4 : 2, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(DARK_DUST, fx.x, fx.y - 0.15, fx.z, heavy ? 18 : 10, 0.45, 0.22, 0.45, 0.07);
      level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, heavy ? 0.9F : 0.7F, heavy ? 0.72F : 0.95F);
   }

   private static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, path);
   }
}
