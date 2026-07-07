package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantSprintCollisionHelper;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

public final class ServantCardHeraclesSkills {
   private static final String HERACLES_AIRBORNE_TAG = "ServantCardHeraclesAirborne";
   private static final String HERACLES_AIRBORNE_TICKS_TAG = "ServantCardHeraclesAirborneTicks";
   private static final String HERACLES_AIRBORNE_MAX_Y_TAG = "ServantCardHeraclesAirborneMaxY";
   private static final String HERACLES_FORCE_LANDING_IMPACT_TAG = "ServantCardHeraclesForceLandingImpact";
   private static final String HERACLES_LAST_SPRINT_COLLISION_BREAK_TAG = "ServantCardHeraclesLastSprintCollisionBreak";
   private static final double HERACLES_LANDING_IMPACT_MIN_DROP = 5.0;
   private static final String GOD_HAND_REVIVE_LOCK_TAG = "GodHandReviveLockUntil";
   private static final String GOD_HAND_HIGH_DAMAGE_REVIVE_UNTIL_TAG = "GodHandHighDamageReviveUntil";

   private ServantCardHeraclesSkills() {
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!"heracles".equals(vars.servant_card_id)) {
         clear(player);
         return;
      }
      initializeHeraclesGodHand(player);
      CompoundTag data = player.getPersistentData();
      tickHeraclesSprintCollisionBreak(player, data);
      boolean onGround = player.onGround();
      if (!onGround) {
         if (!data.getBoolean(HERACLES_AIRBORNE_TAG)) {
            beginHeraclesAirborne(player, false);
         }
         data.putDouble(HERACLES_AIRBORNE_MAX_Y_TAG, Math.max(data.getDouble(HERACLES_AIRBORNE_MAX_Y_TAG), player.getY()));
         data.putInt(HERACLES_AIRBORNE_TICKS_TAG, data.getInt(HERACLES_AIRBORNE_TICKS_TAG) + 1);
      } else if (data.getBoolean(HERACLES_AIRBORNE_TAG)) {
         int airborneTicks = data.getInt(HERACLES_AIRBORNE_TICKS_TAG);
         double drop = Math.max(0.0, data.getDouble(HERACLES_AIRBORNE_MAX_Y_TAG) - player.getY());
         boolean forceImpact = data.getBoolean(HERACLES_FORCE_LANDING_IMPACT_TAG);
         data.remove(HERACLES_AIRBORNE_TAG);
         data.remove(HERACLES_AIRBORNE_TICKS_TAG);
         data.remove(HERACLES_AIRBORNE_MAX_Y_TAG);
         data.remove(HERACLES_FORCE_LANDING_IMPACT_TAG);
         if (airborneTicks >= 8 && (forceImpact || drop >= HERACLES_LANDING_IMPACT_MIN_DROP)) {
            performHeraclesLandingImpact(player);
         }
      }
   }

   public static void clear(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      data.remove(HERACLES_AIRBORNE_TAG);
      data.remove(HERACLES_AIRBORNE_TICKS_TAG);
      data.remove(HERACLES_AIRBORNE_MAX_Y_TAG);
      data.remove(HERACLES_FORCE_LANDING_IMPACT_TAG);
      data.remove(HERACLES_LAST_SPRINT_COLLISION_BREAK_TAG);
      data.remove("GodHandActive");
      data.remove("GodHandThreshold");
      data.remove("GodHandLives");
      data.remove("GodHandAdaptiveReduction");
      data.remove("GodHandAdaptiveMax");
      data.remove("GodHandStrongCost");
      data.remove("GodHandExtraStrongCost");
      data.remove("GodHandCurrentResistance");
      data.remove("GodHandAdaptedMedusaCybele");
      data.remove("GodHandAdaptedCursedArmZabaniya");
      data.remove("CausalSevered");
      data.remove(GOD_HAND_REVIVE_LOCK_TAG);
      data.remove(GOD_HAND_HIGH_DAMAGE_REVIVE_UNTIL_TAG);
   }

   public static void initializeHeraclesGodHand(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      data.putBoolean("GodHandActive", true);
      data.putFloat("GodHandThreshold", 3.0F);
      if (!data.contains("GodHandLives")) {
         data.putInt("GodHandLives", 11);
      }
      data.putFloat("GodHandAdaptiveReduction", 0.25F);
      data.putFloat("GodHandAdaptiveMax", 0.75F);
      data.putInt("GodHandStrongCost", 2);
      data.putInt("GodHandExtraStrongCost", 3);
   }

   private static void tickHeraclesSprintCollisionBreak(ServerPlayer player, CompoundTag data) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      ServantSprintCollisionHelper.tryPlayerSprintCollision(player, level, data, HERACLES_LAST_SPRINT_COLLISION_BREAK_TAG, false, 10.0F, 1.25, 0.26, 32, 45.0F);
   }

   private static boolean hasHeraclesBreakableBlockAhead(ServerLevel level, ServerPlayer player, Vec3 dir) {
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
               if (canBreakHeraclesBlock(level, pos, 42.0F)) {
                  return true;
               }
            }
         }
      }
      return false;
   }

   public static void beginHeraclesAirborne(ServerPlayer player, boolean forceLandingImpact) {
      CompoundTag data = player.getPersistentData();
      data.putBoolean(HERACLES_AIRBORNE_TAG, true);
      data.putInt(HERACLES_AIRBORNE_TICKS_TAG, 0);
      data.putDouble(HERACLES_AIRBORNE_MAX_Y_TAG, player.getY());
      if (forceLandingImpact) {
         data.putBoolean(HERACLES_FORCE_LANDING_IMPACT_TAG, true);
      }
   }

   public static void performBigJump(ServerPlayer player) {
      Vec3 look = PlayerNoblePhantasmHelper.horizontalLook(player);
      player.setDeltaMovement(look.scale(1.05).add(0.0, 1.52, 0.0));
      player.hurtMarked = true;
      player.fallDistance = 0.0F;
      beginHeraclesAirborne(player, true);
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.2, player.getZ(), 28, 0.75, 0.16, 0.75, 0.11);
         level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 0.45, player.getZ(), 16, 0.35, 0.22, 0.35, 0.06);
         level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.62F);
      }
   }

   private static void performHeraclesLandingImpact(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      BlockPos center = player.blockPosition();
      int broken = breakHeraclesRadius(level, center, 2.4, 1, 2, 22, 38.0F);
      damageHeraclesRadius(player, 3.2, 26.0F, 1.15, 0.42);
      if (broken > 0 || player.tickCount % 2 == 0) {
         level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.18, player.getZ(), 26, 1.1, 0.18, 1.1, 0.12);
         level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 0.15, player.getZ(), 12, 0.9, 0.16, 0.9, 0.06);
         level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.65F, 0.72F);
      }
   }

   public static void performGroundSlam(ServerPlayer player, boolean heavy) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      VFXServerEffects.spawn(level, "servant_heracles_slam", player.position(), 128.0);
      double radius = heavy ? 5.4 : 4.0;
      float damage = heavy ? 58.0F : 36.0F;
      damageHeraclesRadius(player, radius, damage, heavy ? 1.85 : 1.25, heavy ? 0.72 : 0.46);
      breakHeraclesRadius(level, player.blockPosition(), heavy ? 3.8 : 2.6, 1, 2, heavy ? 70 : 28, heavy ? 75.0F : 42.0F);
      spawnHeraclesSlamFx(level, player.position(), heavy);
   }

   public static void performRoar(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      VFXServerEffects.spawn(level, "servant_heracles_roar", player, 128.0);
      for (LivingEntity target : level.getEntitiesOfClass(
         LivingEntity.class,
         player.getBoundingBox().inflate(7.0),
         e -> e != player && e.isAlive() && !EntityUtils.isImmunePlayerTarget(e)
      )) {
         Vec3 away = target.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
         if (away.lengthSqr() < 1.0E-4) {
            away = PlayerNoblePhantasmHelper.horizontalLook(player);
         }
         away = away.normalize();
         target.invulnerableTime = 0;
         target.hurt(player.damageSources().mobAttack(player), 14.0F);
         target.invulnerableTime = 0;
         target.push(away.x * 1.35, 0.28, away.z * 1.35);
         target.hurtMarked = true;
         target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1, false, true, true));
      }
      level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + player.getBbHeight(), player.getZ(), 28, 0.9, 0.65, 0.9, 0.16);
      level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 1.2, player.getZ(), 14, 0.55, 0.5, 0.55, 0.1);
      level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 0.5, player.getZ(), 18, 1.05, 0.3, 1.05, 0.05);
      level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, player.getX(), player.getY() + 0.1, player.getZ(), 24, 1.25, 0.1, 1.25, 0.08);
      level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.2F, 0.7F);
   }

   public static void showGodHandStatus(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      int lives = Math.max(1, data.getInt("GodHandLives") + 1);
      player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.god_hand_lives", lives), true);
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + player.getBbHeight() * 0.62, player.getZ(), 18, 0.5, 0.55, 0.5, 0.035);
         level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + player.getBbHeight() * 0.52, player.getZ(), 16, 0.45, 0.45, 0.45, 0.04);
         level.playSound(null, player.blockPosition(), SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.55F, 0.72F);
      }
   }

   public static void triggerHeraclesAttackImpact(ServerPlayer player, LivingEntity target) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed || !"heracles".equals(vars.servant_card_id) || !(player.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.35, 0.0);
      breakHeraclesRadius(level, BlockPos.containing(center), 2.0, 1, 2, 14, 45.0F);
      level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, center.x, center.y - 0.25, center.z, 16, 0.7, 0.18, 0.7, 0.05);
      level.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z, 10, 0.45, 0.35, 0.45, 0.12);
   }

   public static void triggerHeraclesBlockAttack(ServerPlayer player, BlockPos pos) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed || !"heracles".equals(vars.servant_card_id) || !(player.level() instanceof ServerLevel level)) {
         return;
      }
      breakHeraclesRadius(level, pos, 1.85, 1, 1, 12, 45.0F);
      Vec3 center = Vec3.atCenterOf(pos);
      level.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z, 14, 0.55, 0.28, 0.55, 0.08);
      level.playSound(null, pos, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.PLAYERS, 0.75F, 0.68F);
   }

   private static void damageHeraclesRadius(ServerPlayer player, double radius, float damage, double horizontalPower, double verticalPower) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      for (LivingEntity target : level.getEntitiesOfClass(
         LivingEntity.class,
         player.getBoundingBox().inflate(radius),
         e -> e != player && e.isAlive() && !EntityUtils.isImmunePlayerTarget(e)
      )) {
         Vec3 away = target.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
         if (away.lengthSqr() < 1.0E-4) {
            away = PlayerNoblePhantasmHelper.horizontalLook(player);
         }
         away = away.normalize();
         double distance = Math.max(0.8, target.distanceTo(player));
         float scaledDamage = (float)(damage * Mth.clamp(1.15 - distance / (radius * 1.35), 0.45, 1.0));
         target.invulnerableTime = 0;
         target.hurt(player.damageSources().mobAttack(player), scaledDamage);
         target.invulnerableTime = 0;
         target.push(away.x * horizontalPower, verticalPower, away.z * horizontalPower);
         target.hurtMarked = true;
      }
   }

   private static int breakHeraclesRadius(ServerLevel level, BlockPos center, double radius, int down, int up, int limit, float hardnessCap) {
      int broken = 0;
      int ceil = Mth.ceil(radius);
      for (BlockPos pos : BlockPos.betweenClosed(center.offset(-ceil, -down, -ceil), center.offset(ceil, up, ceil))) {
         if (broken >= limit) {
            break;
         }
         double dx = pos.getX() + 0.5 - (center.getX() + 0.5);
         double dy = (pos.getY() - center.getY()) / 0.75;
         double dz = pos.getZ() + 0.5 - (center.getZ() + 0.5);
         if (dx * dx + dy * dy + dz * dz > radius * radius || blockNoise(level, pos) > 0.86 && radius > 2.0) {
            continue;
         }
         if (breakHeraclesBlock(level, pos, hardnessCap, broken % 5 == 0)) {
            broken++;
         }
      }
      return broken;
   }

   private static int breakHeraclesForwardBlocks(ServerLevel level, ServerPlayer player, Vec3 dir, double distance, int halfWidth, int limit, float hardnessCap) {
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
               if (breakHeraclesBlock(level, pos, hardnessCap, broken % 4 == 0)) {
                  broken++;
               }
            }
         }
      }
      return broken;
   }

   private static boolean breakHeraclesBlock(ServerLevel level, BlockPos pos, float hardnessCap, boolean debris) {
      BlockState state = level.getBlockState(pos);
      if (!canBreakHeraclesBlock(level, pos, hardnessCap)) {
         return false;
      }
      if (debris) {
         spawnHeraclesDebris(level, pos, state);
      }
      if (!level.removeBlock(pos, false)) {
         return false;
      }
      if (debris) {
         level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.45, pos.getZ() + 0.5, 4, 0.2, 0.16, 0.2, 0.04);
      }
      return true;
   }

   private static boolean canBreakHeraclesBlock(ServerLevel level, BlockPos pos, float hardnessCap) {
      BlockState state = level.getBlockState(pos);
      float hardness = state.getDestroySpeed(level, pos);
      return !state.isAir()
         && !state.is(Blocks.BEDROCK)
         && hardness >= 0.0F
         && hardness <= hardnessCap
         && state.getExplosionResistance(level, pos, null) < 1200.0F;
   }

   private static void spawnHeraclesDebris(ServerLevel level, BlockPos pos, BlockState state) {
      if (!level.getBlockState(pos.above()).isAir() || blockNoise(level, pos) > 0.35) {
         return;
      }
      FallingBlockEntity falling = FallingBlockEntity.fall(level, pos.above(), state);
      falling.disableDrop();
      double angle = blockNoise(level, pos) * Math.PI * 2.0;
      falling.setDeltaMovement(Math.cos(angle) * 0.08, 0.24 + blockNoise(level, pos.above()) * 0.16, Math.sin(angle) * 0.08);
      falling.time = 1;
   }

   private static void spawnHeraclesSlamFx(ServerLevel level, Vec3 center, boolean heavy) {
      double y = center.y;
      level.sendParticles(ParticleTypes.CLOUD, center.x, y + 0.3, center.z, heavy ? 36 : 26, 1.5, 0.3, 1.5, 0.3);
      level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, y + 0.2, center.z, heavy ? 18 : 12, 1.2, 0.4, 1.2, 0.1);
      level.sendParticles(ParticleTypes.POOF, center.x, y + 0.5, center.z, heavy ? 14 : 10, 1.0, 0.3, 1.0, 0.15);
      int rings = heavy ? 3 : 2;
      for (int ring = 1; ring <= rings; ring++) {
         double radius = ring * 1.27;
         for (int i = 0; i < 16; i++) {
            double angle = Math.PI * 2.0 * i / 16.0;
            double px = center.x + Math.cos(angle) * radius;
            double pz = center.z + Math.sin(angle) * radius;
            level.sendParticles(ParticleTypes.CLOUD, px, y + 0.2, pz, 1, 0.0, 0.0, 0.0, 0.03);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, px, y + 0.15, pz, 1, 0.0, 0.0, 0.0, 0.05);
         }
      }
      level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, heavy ? 1.35F : 0.85F, 0.52F);
      level.playSound(null, BlockPos.containing(center), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, heavy ? 1.0F : 0.7F, 0.6F);
   }

   private static double blockNoise(ServerLevel level, BlockPos pos) {
      long seed = pos.asLong() ^ (level.getGameTime() * 341873128712L);
      seed ^= seed >>> 33;
      seed *= 0xff51afd7ed558ccdL;
      seed ^= seed >>> 33;
      seed *= 0xc4ceb9fe1a85ec53L;
      seed ^= seed >>> 33;
      return (double)(seed & 0xFFFFFFL) / (double)0x1000000;
   }


}
