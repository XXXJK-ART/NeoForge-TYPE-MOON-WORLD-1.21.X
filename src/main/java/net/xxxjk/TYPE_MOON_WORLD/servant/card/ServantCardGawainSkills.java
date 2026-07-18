package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantSprintCollisionHelper;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

import static net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSkillUtils.hitForwardArc;
import static net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSkillUtils.spawnLineParticles;

public final class ServantCardGawainSkills {
   private static final String TAG_INITIALIZED = "ServantCardGawainInitialized";
   private static final String TAG_SUN_BLESSING = "ServantCardGawainSunBlessingActive";
   private static final String TAG_LAST_SUN_VFX = "ServantCardGawainLastSunBlessingVfx";
   private static final String TAG_LAST_SUN_COLLISION_BREAK = "ServantCardGawainLastSunCollisionBreak";
   private static final String TAG_BELT_READY = "ServantCardGawainBeltReady";
   private static final String TAG_BELT_SOLAR = "ServantCardGawainBeltSolar";
   private static final ResourceLocation SUN_HEALTH_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_gawain_sun_health");
   private static final ResourceLocation SUN_ATTACK_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_gawain_sun_attack");
   private static final ResourceLocation SUN_SPEED_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_gawain_sun_speed");
   private static final ResourceLocation SUN_ARMOR_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_gawain_sun_armor");

   private ServantCardGawainSkills() {
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!vars.servant_card_transformed || !"gawain".equals(vars.servant_card_id)) {
         clear(player);
         return;
      }
      CompoundTag data = player.getPersistentData();
      if (!data.getBoolean(TAG_INITIALIZED)) {
         data.putBoolean(TAG_INITIALIZED, true);
         data.putBoolean(TAG_BELT_READY, true);
      }
      if (player.level() instanceof ServerLevel level) {
         tickSunBlessing(player, level, data);
         tickSunCollisionBreak(player, level, data);
      }
   }

   public static void clear(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      data.remove(TAG_INITIALIZED);
      data.remove(TAG_SUN_BLESSING);
      data.remove(TAG_LAST_SUN_VFX);
      data.remove(TAG_LAST_SUN_COLLISION_BREAK);
      data.remove(TAG_BELT_READY);
      data.remove(TAG_BELT_SOLAR);
      removeModifier(player.getAttribute(Attributes.MAX_HEALTH), SUN_HEALTH_ID);
      removeModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), SUN_ATTACK_ID);
      removeModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), SUN_SPEED_ID);
      removeModifier(player.getAttribute(Attributes.ARMOR), SUN_ARMOR_ID);
      if (player.getHealth() > player.getMaxHealth()) {
         player.setHealth(player.getMaxHealth());
      }
   }

   public static boolean tryConsumeBeltGuts(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, LivingIncomingDamageEvent event) {
      if (!vars.servant_card_transformed || !"gawain".equals(vars.servant_card_id)) {
         return false;
      }
      CompoundTag data = player.getPersistentData();
      if (!data.getBoolean(TAG_BELT_READY)) {
         return false;
      }
      float projectedHealth = (float)(player.getHealth() - event.getAmount());
      if (projectedHealth > player.getMaxHealth() * 0.35F) {
         return false;
      }
      data.putBoolean(TAG_BELT_READY, false);
      data.putBoolean(TAG_BELT_SOLAR, hasSunBlessing(player));
      event.setCanceled(true);
      event.setAmount(0.0F);
      player.setHealth(player.getMaxHealth());
      player.clearFire();
      player.invulnerableTime = Math.max(player.invulnerableTime, data.getBoolean(TAG_BELT_SOLAR) ? 80 : 50);
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, data.getBoolean(TAG_BELT_SOLAR) ? 100 : 70, 2, false, true, true));
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + player.getBbHeight() * 0.55, player.getZ(), 44, 0.5, 0.7, 0.5, 0.14);
         level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + player.getBbHeight() * 0.45, player.getZ(), 34, 0.42, 0.45, 0.42, 0.08);
         level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + player.getBbHeight() * 0.62, player.getZ(), 40, 0.45, 0.55, 0.45, 0.08);
         level.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.1F, 0.85F);
      }
      vars.syncPlayerVariables(player);
      return true;
   }

   public static boolean hasSunBlessing(ServerPlayer player) {
      return player.getPersistentData().getBoolean(TAG_SUN_BLESSING);
   }

   public static void performGuard(ServerPlayer player) {
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 180, 1, false, true, true));
   }

   public static void performCharisma(ServerPlayer player) {
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 180, 0, false, true, true));
      if (player.level() instanceof ServerLevel level) {
         for (ServerPlayer ally : level.players()) {
            if (ally != player && ally.distanceToSqr(player) <= 10.0 * 10.0) {
               ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 180, 0, false, true, true));
               ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 180, 0, false, true, true));
            }
         }
      }
   }

   public static void performGawainGallatinSpark(ServerPlayer player) {
      Vec3 dir = PlayerNoblePhantasmHelper.horizontalLook(player);
      boolean solar = hasSunBlessing(player);
      hitForwardArc(player, dir, 8.0, solar ? 48.0F : 34.0F);
      if (player.level() instanceof ServerLevel level) {
         spawnLineParticles(level, player.getEyePosition(), player.getEyePosition().add(dir.scale(9.0)), ParticleTypes.FLAME);
         level.sendParticles(ParticleTypes.END_ROD, player.getX() + dir.x * 3.0, player.getY() + 1.0, player.getZ() + dir.z * 3.0, 22, 0.4, 0.25, 0.4, 0.04);
      }
   }

   public static void performGawainSolarRebuke(ServerPlayer player) {
      if (player.level() instanceof ServerLevel level) {
         boolean solar = hasSunBlessing(player);
         for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(6.0, 2.0, 6.0), e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e))) {
            living.setRemainingFireTicks(solar ? 100 : 80);
            living.invulnerableTime = 0;
            living.hurt(player.damageSources().playerAttack(player), solar ? 42.0F : 28.0F);
            living.invulnerableTime = 0;
            Vec3 away = living.position().subtract(player.position()).normalize();
            living.push(away.x * 0.8, 0.22, away.z * 0.8);
            living.hurtMarked = true;
         }
         level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 0.8, player.getZ(), 46, 2.6, 0.55, 2.6, 0.06);
      }
   }

   public static void performGawainFlameTornado(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 dir = PlayerNoblePhantasmHelper.horizontalLook(player);
      for (int step = 1; step <= 14; step++) {
         final int index = step;
         TYPE_MOON_WORLD.queueServerWork(step * 2, () -> {
            if (!player.isAlive() || !(player.level() instanceof ServerLevel delayedLevel)) {
               return;
            }
            Vec3 center = player.position().add(dir.scale(1.6 + index * 1.15)).add(0.0, 0.7, 0.0);
            delayedLevel.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 18, 0.55, 0.75, 0.55, 0.08);
            delayedLevel.sendParticles(ParticleTypes.SMOKE, center.x, center.y + 0.15, center.z, 10, 0.45, 0.6, 0.45, 0.04);
            delayedLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z, 1, 0.0, 0.0, 0.0, 0.0);
            for (LivingEntity living : delayedLevel.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().move(dir.scale(index * 1.15)).inflate(1.8, 1.4, 1.8), e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e))) {
               living.setRemainingFireTicks(100);
               living.invulnerableTime = 0;
               living.hurt(player.damageSources().playerAttack(player), hasSunBlessing(player) ? 18.0F : 12.0F);
               living.invulnerableTime = 0;
               living.push(dir.x * 0.75, 0.18, dir.z * 0.75);
               living.hurtMarked = true;
            }
            if (index == 1) {
               delayedLevel.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.8F);
            }
         });
      }
   }

   public static void performGawainRadiantField(ServerPlayer player) {
      boolean solar = hasSunBlessing(player);
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 260, solar ? 2 : 1, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 360, 0, false, true, true));
      if (solar) {
         player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 260, 1, false, true, true));
      }
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 36, 1.2, 0.6, 1.2, 0.035);
      }
   }

   public static void performGawainSolarCombo(ServerPlayer player) {
      Vec3 dir = PlayerNoblePhantasmHelper.horizontalLook(player);
      player.setDeltaMovement(player.getDeltaMovement().add(dir.x * 1.4, 0.18, dir.z * 1.4));
      player.hurtMarked = true;
      hitForwardArc(player, dir, 5.0, hasSunBlessing(player) ? 32.0F : 22.0F);
   }

   private static void tickSunBlessing(ServerPlayer player, ServerLevel level, CompoundTag data) {
      boolean active = isUnderSun(level, player.blockPosition());
      boolean wasActive = data.getBoolean(TAG_SUN_BLESSING);
      if (active != wasActive) {
         float ratio = player.getMaxHealth() > 0.0F ? player.getHealth() / player.getMaxHealth() : 1.0F;
         data.putBoolean(TAG_SUN_BLESSING, active);
         updateModifier(player.getAttribute(Attributes.MAX_HEALTH), SUN_HEALTH_ID, active ? 2.0 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
         updateModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), SUN_ATTACK_ID, active ? 2.0 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
         updateModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), SUN_SPEED_ID, active ? 2.0 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
         updateModifier(player.getAttribute(Attributes.ARMOR), SUN_ARMOR_ID, active ? 2.0 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
         player.setHealth(Math.max(1.0F, Math.min(player.getMaxHealth(), player.getMaxHealth() * ratio)));
         spawnSunTransitionFx(player, level, active);
      }
      if (active && level.getGameTime() - data.getLong(TAG_LAST_SUN_VFX) >= 38L) {
         data.putLong(TAG_LAST_SUN_VFX, level.getGameTime());
         VFXServerEffects.spawn(level, "servant_gawain_sun_blessing", player, 64.0);
      }
   }

   private static void tickSunCollisionBreak(ServerPlayer player, ServerLevel level, CompoundTag data) {
      boolean solar = data.getBoolean(TAG_SUN_BLESSING);
      ServantSprintCollisionHelper.tryPlayerSprintCollision(player, level, data, TAG_LAST_SUN_COLLISION_BREAK, solar, solar ? 8.0F : 6.0F, solar ? 1.0 : 0.85, solar ? 0.2 : 0.16, 27, 42.0F);
   }

   private static boolean isUnderSun(ServerLevel level, BlockPos pos) {
      long dayTime = level.getDayTime() % 24000L;
      return level.dimensionType().hasSkyLight()
         && dayTime >= 0L && dayTime < 12000L
         && !level.isRaining() && !level.isThundering()
         && level.canSeeSky(pos.above());
   }

   private static void spawnSunTransitionFx(ServerPlayer player, ServerLevel level, boolean active) {
      VFXServerEffects.spawn(level, "servant_gawain_sun_blessing", player, 64.0);
      level.sendParticles(active ? ParticleTypes.END_ROD : ParticleTypes.SMOKE, player.getX(), player.getY() + player.getBbHeight() * 0.55, player.getZ(), active ? 46 : 18, 0.45, 0.55, 0.45, active ? 0.08 : 0.03);
      level.playSound(null, player.blockPosition(), active ? SoundEvents.BEACON_POWER_SELECT : SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.9F, active ? 1.35F : 0.85F);
   }

   private static boolean hasSunBreakableBlockAhead(ServerLevel level, ServerPlayer player, Vec3 dir) {
      if (dir.lengthSqr() < 1.0E-4) {
         return false;
      }
      dir = dir.normalize();
      Vec3 right = new Vec3(-dir.z, 0.0, dir.x);
      for (double step = 0.62; step <= 1.18; step += 0.28) {
         Vec3 center = player.position().add(dir.scale(step));
         for (int y = 0; y <= 2; y++) {
            for (int w = -1; w <= 1; w++) {
               BlockPos pos = BlockPos.containing(center.add(right.scale(w * 0.42)).add(0.0, y, 0.0));
               if (canBreakSunBlock(level, pos, 42.0F)) {
                  return true;
               }
            }
         }
      }
      return false;
   }

   private static int breakSunForwardBlocks(ServerLevel level, ServerPlayer player, Vec3 dir, double distance, int halfWidth, int limit, float hardnessCap) {
      if (dir.lengthSqr() < 1.0E-4) {
         return 0;
      }
      dir = dir.normalize();
      Vec3 right = new Vec3(-dir.z, 0.0, dir.x);
      int broken = 0;
      for (double step = 0.8; step <= distance && broken < limit; step += 0.8) {
         Vec3 center = player.position().add(dir.scale(step));
         for (int y = 0; y <= 2 && broken < limit; y++) {
            for (int w = -halfWidth; w <= halfWidth && broken < limit; w++) {
               if (Math.abs(w) == halfWidth && blockNoise(level, BlockPos.containing(center)) > 0.7) {
                  continue;
               }
               BlockPos pos = BlockPos.containing(center.add(right.scale(w * 0.65)).add(0.0, y, 0.0));
               if (breakSunBlock(level, pos, hardnessCap, broken % 4 == 0)) {
                  broken++;
               }
            }
         }
      }
      return broken;
   }

   private static boolean breakSunBlock(ServerLevel level, BlockPos pos, float hardnessCap, boolean debris) {
      BlockState state = level.getBlockState(pos);
      if (!canBreakSunBlock(level, pos, hardnessCap)) {
         return false;
      }
      if (!level.removeBlock(pos, false)) {
         return false;
      }
      if (debris) {
         level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.45, pos.getZ() + 0.5, 4, 0.2, 0.16, 0.2, 0.04);
         level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.48, pos.getZ() + 0.5, 3, 0.18, 0.14, 0.18, 0.03);
         level.levelEvent(2001, pos, net.minecraft.world.level.block.Block.getId(state));
      }
      return true;
   }

   private static boolean canBreakSunBlock(ServerLevel level, BlockPos pos, float hardnessCap) {
      BlockState state = level.getBlockState(pos);
      float hardness = state.getDestroySpeed(level, pos);
      return !state.isAir()
         && !state.is(Blocks.BEDROCK)
         && hardness >= 0.0F
         && hardness <= hardnessCap
         && state.getExplosionResistance(level, pos, null) < 1200.0F;
   }

   private static double blockNoise(ServerLevel level, BlockPos pos) {
      long seed = pos.asLong() ^ level.getSeed();
      seed ^= seed >>> 33;
      seed *= 0xff51afd7ed558ccdL;
      seed ^= seed >>> 33;
      return (seed & 0xffff) / 65535.0;
   }

   private static void updateModifier(AttributeInstance attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
      if (attribute == null) {
         return;
      }
      attribute.removeModifier(id);
      if (amount != 0.0) {
         attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
      }
   }

   private static void removeModifier(AttributeInstance attribute, ResourceLocation id) {
      if (attribute != null) {
         attribute.removeModifier(id);
      }
   }
}
