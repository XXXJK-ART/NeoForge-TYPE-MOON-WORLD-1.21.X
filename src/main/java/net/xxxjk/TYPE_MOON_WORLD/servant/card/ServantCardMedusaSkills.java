package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.GravityFieldShellEffectEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MedusaPegasusEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper;
import net.xxxjk.TYPE_MOON_WORLD.mixin.LivingEntityInputAccessor;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceRank;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesGodHandHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import org.joml.Vector3f;

public final class ServantCardMedusaSkills {
   private static final ResourceLocation MEDUSA_MONSTER_STRENGTH_ATTACK_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_medusa_monster_strength_attack");
   private static final String MEDUSA_EYES_ACTIVE_TAG = "ServantCardMedusaEyesActive";
   private static final String MEDUSA_LAST_CYBELE_TICK_TAG = "ServantCardMedusaLastCybeleTick";
   private static final String MEDUSA_LAST_BLOODFORT_TICK_TAG = "ServantCardMedusaLastBloodfortTick";
   private static final String MEDUSA_BLOODFORT_UNTIL_TAG = "ServantCardMedusaBloodfortUntil";
   private static final String MEDUSA_BLOODFORT_RADIUS_TAG = "ServantCardMedusaBloodfortRadius";
   private static final String MEDUSA_BLOODFORT_X_TAG = "ServantCardMedusaBloodfortX";
   private static final String MEDUSA_BLOODFORT_Y_TAG = "ServantCardMedusaBloodfortY";
   private static final String MEDUSA_BLOODFORT_Z_TAG = "ServantCardMedusaBloodfortZ";
   private static final String MEDUSA_BLOODFORT_NP_ACTIVE_TAG = "ServantCardMedusaBloodfortNpActive";
   private static final String MEDUSA_BELLEROPHON_PEGASUS_UUID_TAG = "ServantCardMedusaPegasusUuid";
   private static final String MEDUSA_BELLEROPHON_RIDE_UNTIL_TAG = "ServantCardMedusaBellerophonRideUntil";
   private static final String MEDUSA_BELLEROPHON_CHARGE_UNTIL_TAG = "ServantCardMedusaBellerophonChargeUntil";
   private static final String MEDUSA_BELLEROPHON_LAUNCH_TICK_TAG = "ServantCardMedusaBellerophonLaunchTick";
   private static final String MEDUSA_BELLEROPHON_TARGET_X_TAG = "ServantCardMedusaBellerophonTargetX";
   private static final String MEDUSA_BELLEROPHON_TARGET_Y_TAG = "ServantCardMedusaBellerophonTargetY";
   private static final String MEDUSA_BELLEROPHON_TARGET_Z_TAG = "ServantCardMedusaBellerophonTargetZ";
   private static final String MEDUSA_BELLEROPHON_SHOCKWAVE_DONE_TAG = "ServantCardMedusaBellerophonShockwaveDone";
   private static final String MEDUSA_BELLEROPHON_HIT_UNTIL_TAG = "ServantCardMedusaBellerophonHitUntil";
   private static final String MEDUSA_BELLEROPHON_COLLISION_HIT_UNTIL_TAG = "ServantCardMedusaPegasusCollisionHitUntil";
   private static final int MEDUSA_BELLEROPHON_WINDUP_TICKS = 20;
   private static final int MEDUSA_BELLEROPHON_CHARGE_TICKS = 18;
   private static final int MEDUSA_BELLEROPHON_RIDE_EXTENSION_TICKS = 400;
   private static final double MEDUSA_BELLEROPHON_CHARGE_DISTANCE = 10.0;
   private static final double MEDUSA_BELLEROPHON_RIDE_SPEED = 1.9;
   static final double MEDUSA_BLOODFORT_RADIUS = 25.0;
   private static final DustParticleOptions MEDUSA_BLOODFORT_PARTICLE = new DustParticleOptions(new Vector3f(0.95F, 0.22F, 0.35F), 1.1F);
   private static final DustParticleOptions MEDUSA_BLOODFORT_SIGIL_PARTICLE = new DustParticleOptions(new Vector3f(0.86F, 0.08F, 0.12F), 1.25F);
   private static final DustParticleOptions MEDUSA_BLOODFORT_NODE_PARTICLE = new DustParticleOptions(new Vector3f(1.0F, 0.2F, 0.24F), 1.45F);
   private static final DustParticleOptions MEDUSA_BLOODFORT_LINK_PARTICLE = new DustParticleOptions(new Vector3f(0.72F, 0.02F, 0.08F), 1.05F);
   private static final DustParticleOptions MEDUSA_SUMMON_LIGHT_PARTICLE = new DustParticleOptions(new Vector3f(1.0F, 0.96F, 0.82F), 1.25F);
   private static final DustParticleOptions MEDUSA_SUMMON_GOLD_PARTICLE = new DustParticleOptions(new Vector3f(0.98F, 0.84F, 0.32F), 1.2F);
   private ServantCardMedusaSkills() {
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!"medusa".equals(vars.servant_card_id)) {
         clear(player);
         return;
      }
      applyMedusaMonsterStrengthPassive(player);
      CompoundTag data = player.getPersistentData();
      boolean eyesActive = data.getBoolean(MEDUSA_EYES_ACTIVE_TAG);
      if (vars.servant_card_medusa_mystic_eyes_active != eyesActive) {
         vars.servant_card_medusa_mystic_eyes_active = eyesActive;
         vars.syncPlayerVariables(player);
      }
      long now = player.level().getGameTime();
      if (eyesActive) {
         tickMedusaMysticEyes(player, now);
      }
      tickMedusaBloodfort(player, vars, now);
      tickMedusaBellerophon(player, now);
      if (player.tickCount % 14 == 0 && player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + player.getBbHeight() * 0.55, player.getZ(), 2, 0.22, 0.28, 0.22, 0.012);
      }
   }

   private static void applyMedusaMonsterStrengthPassive(ServerPlayer player) {
      AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
      if (attack != null && attack.getModifier(MEDUSA_MONSTER_STRENGTH_ATTACK_ID) == null) {
         attack.addTransientModifier(new AttributeModifier(MEDUSA_MONSTER_STRENGTH_ATTACK_ID, 5.0, AttributeModifier.Operation.ADD_VALUE));
      }
   }

   public static void clear(ServerPlayer player) {
      remove(player.getAttribute(Attributes.ATTACK_DAMAGE), MEDUSA_MONSTER_STRENGTH_ATTACK_ID);
      CompoundTag data = player.getPersistentData();
      MedusaPegasusEntity pegasus = getMedusaPegasus(player);
      if (pegasus != null && pegasus.isAlive()) {
         player.stopRiding();
         pegasus.discard();
      }
      data.remove(MEDUSA_EYES_ACTIVE_TAG);
      data.remove(MEDUSA_LAST_CYBELE_TICK_TAG);
      data.remove(MEDUSA_LAST_BLOODFORT_TICK_TAG);
      data.remove(MEDUSA_BLOODFORT_UNTIL_TAG);
      data.remove(MEDUSA_BLOODFORT_RADIUS_TAG);
      data.remove(MEDUSA_BLOODFORT_X_TAG);
      data.remove(MEDUSA_BLOODFORT_Y_TAG);
      data.remove(MEDUSA_BLOODFORT_Z_TAG);
      data.remove(MEDUSA_BLOODFORT_NP_ACTIVE_TAG);
      data.remove(MEDUSA_BELLEROPHON_PEGASUS_UUID_TAG);
      data.remove(MEDUSA_BELLEROPHON_RIDE_UNTIL_TAG);
      data.remove(MEDUSA_BELLEROPHON_CHARGE_UNTIL_TAG);
      data.remove(MEDUSA_BELLEROPHON_LAUNCH_TICK_TAG);
      data.remove(MEDUSA_BELLEROPHON_TARGET_X_TAG);
      data.remove(MEDUSA_BELLEROPHON_TARGET_Y_TAG);
      data.remove(MEDUSA_BELLEROPHON_TARGET_Z_TAG);
      data.remove(MEDUSA_BELLEROPHON_SHOCKWAVE_DONE_TAG);
      data.remove(MEDUSA_BELLEROPHON_HIT_UNTIL_TAG);
      data.remove(MEDUSA_BELLEROPHON_COLLISION_HIT_UNTIL_TAG);
   }

   private static void tickMedusaMysticEyes(ServerPlayer player, long now) {
      if (!(player.level() instanceof ServerLevel level) || now - player.getPersistentData().getLong(MEDUSA_LAST_CYBELE_TICK_TAG) < 10L) {
         return;
      }
      player.getPersistentData().putLong(MEDUSA_LAST_CYBELE_TICK_TAG, now);
      level.sendParticles(ParticleTypes.GLOW, player.getX(), player.getEyeY() - 0.08, player.getZ(), 4, 0.16, 0.08, 0.16, 0.01);
      for (LivingEntity target : level.getEntitiesOfClass(
         LivingEntity.class,
         player.getBoundingBox().inflate(18.0, 7.0, 18.0),
         target -> isMedusaMysticEyesTarget(player, target)
      )) {
         applyMedusaCardCybele(player, target);
      }
   }

   private static boolean isMedusaMysticEyesTarget(ServerPlayer player, LivingEntity target) {
      if (target == player || !target.isAlive() || player.isAlliedTo(target) || EntityUtils.isImmunePlayerTarget(target)) {
         return false;
      }
      Vec3 playerEye = player.getEyePosition();
      Vec3 targetEye = target.getEyePosition();
      Vec3 toTarget = targetEye.subtract(playerEye);
      double distance = toTarget.length();
      if (distance <= 0.01 || distance > 18.0 || player.getLookAngle().normalize().dot(toTarget.normalize()) < 0.72) {
         return false;
      }
      Vec3 toPlayer = playerEye.subtract(targetEye);
      if (toPlayer.lengthSqr() <= 1.0E-4 || target.getLookAngle().normalize().dot(toPlayer.normalize()) < 0.42) {
         return false;
      }
      return player.hasLineOfSight(target);
   }

   private static void applyMedusaCardCybele(ServerPlayer player, LivingEntity target) {
      if (HeraclesGodHandHelper.isAdaptedToCybele(target)) {
         target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2, false, true, true));
         return;
      }
      if (HeraclesGodHandHelper.hasGodHand(target)) {
         HeraclesGodHandHelper.consumeLifeForCybele(target);
         return;
      }

      MagicResistanceRank rank = MagicResistanceHelper.getMagicResistanceRank(target);
      boolean petrify = !rank.isAtLeast(MagicResistanceRank.B);
      if (rank == MagicResistanceRank.B) {
         float failChance = 0.84F - MagicResistanceHelper.getDebuffResistance(target) * 0.35F;
         petrify = player.getRandom().nextFloat() <= Mth.clamp(failChance, 0.2F, 0.92F);
      } else if (rank == MagicResistanceRank.A) {
         float failChance = 0.46F - MagicResistanceHelper.getDebuffResistance(target) * 0.45F;
         petrify = player.getRandom().nextFloat() <= Mth.clamp(failChance, 0.12F, 0.72F);
      }

      if (petrify) {
         target.addEffect(new MobEffectInstance(ModMobEffects.PETRIFIED, MagicResistanceHelper.applyDebuffResistance(target, 120), 0, false, true, true));
      } else {
         int duration = MagicResistanceHelper.applyDebuffResistance(target, rank == MagicResistanceRank.A ? 120 : 90);
         target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 2, false, true, true));
         target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 1, false, true, true));
         target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, 1, false, true, true));
      }
      target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, false, true, true));
      if (target instanceof net.minecraft.world.entity.Mob mob) {
         mob.setTarget(null);
      }
      if (player.level() instanceof ServerLevel level) {
         VFXServerEffects.spawn(level, "servant_medusa_cybele", target.position(), 96.0);
         level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 8, 0.25, 0.35, 0.25, 0.01);
         level.sendParticles(ParticleTypes.GLOW, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 10, 0.3, 0.45, 0.3, 0.01);
         level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.75F, 0.85F);
      }
   }

   public static boolean hasBloodfortTargets(ServerPlayer player, double radius) {
      Vec3 center = player.position().add(0.0, 0.1, 0.0);
      return !player.level().getEntitiesOfClass(
         LivingEntity.class,
         player.getBoundingBox().inflate(radius, radius, radius),
         target -> isMedusaBloodfortTarget(player, center, radius, target)
      ).isEmpty();
   }

   private static void tickMedusaBloodfort(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, long now) {
      CompoundTag data = player.getPersistentData();
      long until = data.getLong(MEDUSA_BLOODFORT_UNTIL_TAG);
      if (until <= now) {
         data.remove(MEDUSA_BLOODFORT_UNTIL_TAG);
         data.remove(MEDUSA_BLOODFORT_RADIUS_TAG);
         data.remove(MEDUSA_BLOODFORT_NP_ACTIVE_TAG);
         return;
      }
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 center = new Vec3(data.getDouble(MEDUSA_BLOODFORT_X_TAG), data.getDouble(MEDUSA_BLOODFORT_Y_TAG), data.getDouble(MEDUSA_BLOODFORT_Z_TAG));
      double radius = data.getDouble(MEDUSA_BLOODFORT_RADIUS_TAG);
      boolean noble = data.getBoolean(MEDUSA_BLOODFORT_NP_ACTIVE_TAG);
      if (now % 10L == 0L) {
         spawnMedusaBloodfortGroundSigil(level, center, radius, noble);
      }
      if (now % 5L == 0L) {
         spawnMedusaBloodfortShell(level, center, radius);
      }
      if (now % 4L == 0L) {
         spawnMedusaBloodfortInteriorHaze(level, center, radius, noble);
      }
      if (now % 20L == 0L) {
         spawnMedusaBloodfortRisingShell(level, center, radius, noble);
      }
      if (now % 20L != 0L) {
         return;
      }
      float damage = noble ? 24.0F + player.getRandom().nextInt(7) : 15.0F;
      for (LivingEntity victim : level.getEntitiesOfClass(
         LivingEntity.class,
         new AABB(center, center).inflate(radius, radius, radius),
         target -> isMedusaBloodfortTarget(player, center, radius, target)
      )) {
         applyMedusaBloodfortDebuffs(victim, noble);
         drainMedusaBloodfortMana(victim, noble);
         float before = victim.getHealth();
         victim.invulnerableTime = 0;
         victim.hurt(player.damageSources().playerAttack(player), damage);
         victim.invulnerableTime = 0;
         float dealt = Math.max(0.0F, before - victim.getHealth());
         if (dealt > 0.0F) {
            vars.servant_card_mana = Math.min(vars.servant_card_max_mana, vars.servant_card_mana + dealt);
            spawnMedusaBloodfortVictimAura(level, victim, dealt);
         }
      }
      vars.syncPlayerVariables(player);
   }

   private static boolean isMedusaBloodfortTarget(ServerPlayer player, Vec3 center, double radius, LivingEntity target) {
      if (target == null || target == player || !target.isAlive() || player.isAlliedTo(target) || EntityUtils.isImmunePlayerTarget(target)) {
         return false;
      }
      double sampleY = Mth.clamp(target.getY() + target.getBbHeight() * 0.35, center.y, center.y + radius);
      if (target.getY() + target.getBbHeight() < center.y - 0.25) {
         return false;
      }
      double dx = target.getX() - center.x;
      double dy = sampleY - center.y;
      double dz = target.getZ() - center.z;
      return dx * dx + dy * dy + dz * dz <= radius * radius;
   }

   private static void applyMedusaBloodfortDebuffs(LivingEntity victim, boolean noble) {
      victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, noble ? 60 : 40, noble ? 2 : 1, false, true, true));
      victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, noble ? 60 : 40, noble ? 1 : 0, false, true, true));
      victim.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, noble ? 60 : 40, noble ? 1 : 0, false, true, true));
   }

   private static void drainMedusaBloodfortMana(LivingEntity victim, boolean noble) {
      double manaDrain = noble ? 18.0 : 10.0;
      if (victim instanceof ServantEntity servant) {
         servant.setCurrentMp(Math.max(0.0, servant.getCurrentMp() - manaDrain));
         return;
      }
      if (victim instanceof ServerPlayer targetPlayer) {
         TypeMoonWorldModVariables.PlayerVariables targetVars = targetPlayer.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (targetVars.servant_card_transformed) {
            targetVars.servant_card_mana = Math.max(0.0, targetVars.servant_card_mana - manaDrain);
            targetVars.syncPlayerVariables(targetPlayer);
         } else if (targetVars.is_magus || targetVars.player_max_mana > 0.0) {
            targetVars.player_mana = Math.max(0.0, targetVars.player_mana - manaDrain);
            targetVars.syncMana(targetPlayer);
         }
      }
   }

   private static void tickMedusaBellerophon(ServerPlayer player, long now) {
      CompoundTag data = player.getPersistentData();
      long launchTick = data.getLong(MEDUSA_BELLEROPHON_LAUNCH_TICK_TAG);
      boolean activeBellerophon = launchTick > 0L
         || data.hasUUID(MEDUSA_BELLEROPHON_PEGASUS_UUID_TAG)
         || data.getLong(MEDUSA_BELLEROPHON_RIDE_UNTIL_TAG) > now;
      if (!activeBellerophon) {
         return;
      }
      if (launchTick > now) {
         player.setDeltaMovement(player.getDeltaMovement().multiply(0.35, 1.0, 0.35));
         return;
      }
      if (launchTick == now) {
         launchMedusaBellerophon(player);
      }
      MedusaPegasusEntity pegasus = getMedusaPegasus(player);
      if (pegasus == null || !pegasus.isAlive()) {
         clearMedusaEyes(player);
         clearMedusaBellerophonData(player);
         return;
      }
      if (player.getVehicle() != pegasus) {
         player.startRiding(pegasus, true);
         if (player.getVehicle() != pegasus) {
            return;
         }
      }
      pegasus.setFlyingMode(true);
      long chargeUntil = data.getLong(MEDUSA_BELLEROPHON_CHARGE_UNTIL_TAG);
      boolean charging = now < chargeUntil;
      Vec3 desired = computeMedusaControlledPegasusVelocity(player, pegasus, charging);
      if (charging) {
         orientMedusaPegasus(player, pegasus, desired);
      } else {
         orientMedusaControlledPegasus(player, pegasus, desired);
      }
      pegasus.setDeltaMovement(desired);
      pegasus.hasImpulse = true;
      if (charging || desired.horizontalDistanceSqr() > 0.01) {
         breakMedusaRideBlocks(pegasus, desired);
      }
      if (charging) {
         applyMedusaChargeHits(player, pegasus, now);
         return;
      }
      if (!data.getBoolean(MEDUSA_BELLEROPHON_SHOCKWAVE_DONE_TAG)) {
         data.putBoolean(MEDUSA_BELLEROPHON_SHOCKWAVE_DONE_TAG, true);
         emitMedusaBellerophonShockwave(player, pegasus);
      }
      if (desired.horizontalDistanceSqr() > 0.01) {
         applyMedusaRideCollisionHits(player, pegasus, now);
      }
      if (now >= data.getLong(MEDUSA_BELLEROPHON_RIDE_UNTIL_TAG)) {
         endMedusaBellerophon(player, pegasus);
      }
   }

   private static void launchMedusaBellerophon(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level) || !player.isAlive()) {
         return;
      }
      CompoundTag data = player.getPersistentData();
      MedusaPegasusEntity pegasus = ModEntities.MEDUSA_PEGASUS.get().create(level);
      if (pegasus == null) {
         clearMedusaBellerophonData(player);
         return;
      }
      pegasus.moveTo(player.getX(), player.getY() + 0.2, player.getZ(), player.getYRot(), player.getXRot());
      pegasus.setSummoner(player);
      pegasus.setFlyingMode(true);
      level.addFreshEntity(pegasus);
      player.startRiding(pegasus, true);
      data.putUUID(MEDUSA_BELLEROPHON_PEGASUS_UUID_TAG, pegasus.getUUID());
      long now = level.getGameTime();
      Vec3 chargeTarget = computeMedusaInitialChargeTarget(player);
      data.putDouble(MEDUSA_BELLEROPHON_TARGET_X_TAG, chargeTarget.x);
      data.putDouble(MEDUSA_BELLEROPHON_TARGET_Y_TAG, chargeTarget.y);
      data.putDouble(MEDUSA_BELLEROPHON_TARGET_Z_TAG, chargeTarget.z);
      data.putLong(MEDUSA_BELLEROPHON_CHARGE_UNTIL_TAG, now + MEDUSA_BELLEROPHON_CHARGE_TICKS);
      data.putLong(MEDUSA_BELLEROPHON_RIDE_UNTIL_TAG, now + MEDUSA_BELLEROPHON_CHARGE_TICKS + MEDUSA_BELLEROPHON_RIDE_EXTENSION_TICKS);
      data.putBoolean(MEDUSA_BELLEROPHON_SHOCKWAVE_DONE_TAG, false);
      spawnMedusaPegasusArrivalFx(level, pegasus);
   }

   private static MedusaPegasusEntity getMedusaPegasus(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level) || !player.getPersistentData().hasUUID(MEDUSA_BELLEROPHON_PEGASUS_UUID_TAG)) {
         return null;
      }
      Entity entity = level.getEntity(player.getPersistentData().getUUID(MEDUSA_BELLEROPHON_PEGASUS_UUID_TAG));
      return entity instanceof MedusaPegasusEntity pegasus ? pegasus : null;
   }

   private static Vec3 computeMedusaInitialChargeTarget(ServerPlayer player) {
      LivingEntity target = findLookTarget(player, 22.0, 2.4);
      Vec3 origin = player.position();
      Vec3 aim = target != null && target.isAlive()
         ? target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(origin)
         : player.getLookAngle();
      if (aim.lengthSqr() < 1.0E-4) {
         aim = new Vec3(0.0, 0.0, 1.0);
      }
      return origin.add(aim.normalize().scale(MEDUSA_BELLEROPHON_CHARGE_DISTANCE));
   }

   private static Vec3 computeMedusaPegasusVelocity(ServerPlayer player, MedusaPegasusEntity pegasus, long now) {
      CompoundTag data = player.getPersistentData();
      Vec3 aim;
      double speed;
      if (now < data.getLong(MEDUSA_BELLEROPHON_CHARGE_UNTIL_TAG)) {
         aim = new Vec3(data.getDouble(MEDUSA_BELLEROPHON_TARGET_X_TAG), data.getDouble(MEDUSA_BELLEROPHON_TARGET_Y_TAG), data.getDouble(MEDUSA_BELLEROPHON_TARGET_Z_TAG)).subtract(pegasus.position());
         speed = 1.15;
      } else {
         LivingEntity target = findLookTarget(player, 24.0, 2.6);
         if (target == null || !target.isAlive()) {
            aim = player.getLookAngle();
            speed = 0.55;
         } else {
            Vec3 targetCenter = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
            Vec3 toTarget = targetCenter.subtract(pegasus.position());
            double distance = Math.max(0.001, toTarget.length());
            Vec3 passPoint = targetCenter.add(toTarget.normalize().scale(Math.max(1.0, MEDUSA_BELLEROPHON_CHARGE_DISTANCE - Math.min(distance, MEDUSA_BELLEROPHON_CHARGE_DISTANCE))));
            aim = passPoint.subtract(pegasus.position());
            speed = 0.95;
         }
      }
      if (aim.lengthSqr() < 1.0E-4) {
         aim = player.getLookAngle();
      }
      aim = aim.normalize();
      return new Vec3(aim.x * speed, Mth.clamp(aim.y * speed, -0.25, 0.35), aim.z * speed);
   }

   private static Vec3 computeMedusaControlledPegasusVelocity(ServerPlayer player, MedusaPegasusEntity pegasus, boolean charging) {
      double strafe = Math.abs(player.xxa) < 0.05F ? 0.0 : player.xxa;
      double forwardInput = Math.abs(player.zza) < 0.05F ? 0.0 : player.zza;
      boolean ascending = ((LivingEntityInputAccessor)player).typemoonworld$isJumping();
      boolean descending = player.isShiftKeyDown();
      if (strafe == 0.0 && forwardInput == 0.0 && !ascending && !descending) {
         return Vec3.ZERO;
      }

      float yaw = player.getYRot() * (float)(Math.PI / 180.0F);
      double sin = Mth.sin(yaw);
      double cos = Mth.cos(yaw);
      double x = strafe * cos - forwardInput * sin;
      double z = forwardInput * cos + strafe * sin;
      Vec3 horizontal = new Vec3(x, 0.0, z);
      if (horizontal.lengthSqr() > 1.0) {
         horizontal = horizontal.normalize();
      }

      double speed = MEDUSA_BELLEROPHON_RIDE_SPEED;
      double vertical = 0.0;
      if (ascending && !descending) {
         vertical = 0.8;
      } else if (descending && !ascending) {
         vertical = -0.8;
      } else if (forwardInput > 0.0) {
         vertical = Mth.clamp(-Math.sin(player.getXRot() * Math.PI / 180.0) * Math.abs(forwardInput) * 0.46, -0.28, 0.34);
      } else {
         vertical = Mth.clamp(pegasus.getDeltaMovement().y * 0.45, -0.12, 0.12);
      }
      return new Vec3(horizontal.x * speed, vertical, horizontal.z * speed);
   }

   private static void applyMedusaChargeHits(ServerPlayer player, MedusaPegasusEntity pegasus, long now) {
      AABB hitBox = pegasus.getBoundingBox().inflate(1.5, 0.8, 1.5);
      for (LivingEntity victim : pegasus.level().getEntitiesOfClass(LivingEntity.class, hitBox, target -> isMedusaChargeVictim(player, pegasus, target, MEDUSA_BELLEROPHON_HIT_UNTIL_TAG, now))) {
         victim.getPersistentData().putLong(MEDUSA_BELLEROPHON_HIT_UNTIL_TAG, now + 20L);
         victim.invulnerableTime = 0;
         victim.hurt(player.damageSources().playerAttack(player), 300.0F);
         victim.invulnerableTime = 0;
         pushAwayFrom(pegasus, victim, 1.2, 0.45);
         if (player.getPersistentData().getBoolean(MEDUSA_EYES_ACTIVE_TAG)) {
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 0, false, true, true));
         }
      }
   }

   private static void applyMedusaRideCollisionHits(ServerPlayer player, MedusaPegasusEntity pegasus, long now) {
      AABB hitBox = pegasus.getBoundingBox().inflate(1.1, 0.8, 1.1);
      for (LivingEntity victim : pegasus.level().getEntitiesOfClass(LivingEntity.class, hitBox, target -> isMedusaChargeVictim(player, pegasus, target, MEDUSA_BELLEROPHON_COLLISION_HIT_UNTIL_TAG, now))) {
         victim.getPersistentData().putLong(MEDUSA_BELLEROPHON_COLLISION_HIT_UNTIL_TAG, now + 10L);
         victim.invulnerableTime = 0;
         victim.hurt(player.damageSources().playerAttack(player), 50.0F);
         victim.invulnerableTime = 0;
         pushAwayFrom(pegasus, victim, 1.0, 0.3);
      }
   }

   private static boolean isMedusaChargeVictim(ServerPlayer player, MedusaPegasusEntity pegasus, LivingEntity target, String cooldownTag, long now) {
      return target != player
         && target != pegasus
         && target.isAlive()
         && !player.isAlliedTo(target)
         && !EntityUtils.isImmunePlayerTarget(target)
         && target.getPersistentData().getLong(cooldownTag) <= now;
   }

   private static void emitMedusaBellerophonShockwave(ServerPlayer player, MedusaPegasusEntity pegasus) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      AABB area = pegasus.getBoundingBox().inflate(5.0, 2.0, 5.0);
      for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area, target -> isMedusaChargeVictim(player, pegasus, target, MEDUSA_BELLEROPHON_COLLISION_HIT_UNTIL_TAG, level.getGameTime()))) {
         victim.invulnerableTime = 0;
         victim.hurt(player.damageSources().playerAttack(player), 100.0F);
         victim.invulnerableTime = 0;
         pushAwayFrom(pegasus, victim, 1.2, 0.55);
      }
      level.sendParticles(ParticleTypes.EXPLOSION, pegasus.getX(), pegasus.getY() + 0.4, pegasus.getZ(), 6, 1.8, 0.4, 1.8, 0.0);
      level.sendParticles(ParticleTypes.CLOUD, pegasus.getX(), pegasus.getY() + 0.2, pegasus.getZ(), 32, 2.3, 0.2, 2.3, 0.08);
      level.playSound(null, pegasus.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0F, 0.8F);
   }

   private static void endMedusaBellerophon(ServerPlayer player, MedusaPegasusEntity pegasus) {
      player.stopRiding();
      clearMedusaEyes(player);
      clearMedusaBellerophonData(player);
      if (pegasus.isAlive()) {
         pegasus.discard();
      }
   }

   private static void clearMedusaBellerophonData(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      data.remove(MEDUSA_BELLEROPHON_PEGASUS_UUID_TAG);
      data.remove(MEDUSA_BELLEROPHON_RIDE_UNTIL_TAG);
      data.remove(MEDUSA_BELLEROPHON_CHARGE_UNTIL_TAG);
      data.remove(MEDUSA_BELLEROPHON_LAUNCH_TICK_TAG);
      data.remove(MEDUSA_BELLEROPHON_TARGET_X_TAG);
      data.remove(MEDUSA_BELLEROPHON_TARGET_Y_TAG);
      data.remove(MEDUSA_BELLEROPHON_TARGET_Z_TAG);
      data.remove(MEDUSA_BELLEROPHON_SHOCKWAVE_DONE_TAG);
   }

   private static void clearMedusaEyes(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      data.remove(MEDUSA_EYES_ACTIVE_TAG);
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars.servant_card_medusa_mystic_eyes_active) {
         vars.servant_card_medusa_mystic_eyes_active = false;
         vars.syncPlayerVariables(player);
      }
      data.remove(MEDUSA_LAST_CYBELE_TICK_TAG);
   }

   private static void orientMedusaPegasus(ServerPlayer player, MedusaPegasusEntity pegasus, Vec3 desired) {
      if (desired.lengthSqr() < 1.0E-4) {
         return;
      }
      double horizontal = Math.sqrt(desired.x * desired.x + desired.z * desired.z);
      float yaw = (float)(Mth.atan2(desired.z, desired.x) * 180.0F / Math.PI) - 90.0F;
      float pitch = (float)(-(Mth.atan2(desired.y, Math.max(horizontal, 1.0E-4)) * 180.0F / Math.PI));
      pegasus.setYRot(yaw);
      pegasus.yRotO = yaw;
      pegasus.setYHeadRot(yaw);
      pegasus.yHeadRotO = yaw;
      pegasus.yBodyRot = yaw;
      pegasus.yBodyRotO = yaw;
      pegasus.setXRot(Mth.clamp(pitch, -35.0F, 35.0F));
      pegasus.xRotO = pegasus.getXRot();
      player.setYRot(yaw);
      player.yRotO = yaw;
      player.setYHeadRot(yaw);
      player.yHeadRotO = yaw;
      player.yBodyRot = yaw;
      player.yBodyRotO = yaw;
      player.setXRot(Mth.clamp(pitch, -20.0F, 20.0F));
      player.xRotO = player.getXRot();
   }

   private static void orientMedusaControlledPegasus(ServerPlayer player, MedusaPegasusEntity pegasus, Vec3 desired) {
      float yaw = desired.horizontalDistanceSqr() > 1.0E-4
         ? (float)(Mth.atan2(desired.z, desired.x) * 180.0F / Math.PI) - 90.0F
         : player.getYRot();
      pegasus.setYRot(yaw);
      pegasus.yRotO = yaw;
      pegasus.setYHeadRot(yaw);
      pegasus.yHeadRotO = yaw;
      pegasus.yBodyRot = yaw;
      pegasus.yBodyRotO = yaw;
      pegasus.setXRot(Mth.clamp(player.getXRot(), -35.0F, 35.0F));
      pegasus.xRotO = pegasus.getXRot();
   }

   private static void pushAwayFrom(Entity source, LivingEntity victim, double horizontalStrength, double verticalStrength) {
      Vec3 push = victim.position().subtract(source.position());
      double horizontal = Math.sqrt(push.x * push.x + push.z * push.z);
      if (horizontal < 1.0E-4) {
         push = new Vec3(Mth.nextDouble(source.level().random, -1.0, 1.0), 0.0, Mth.nextDouble(source.level().random, -1.0, 1.0));
         horizontal = Math.sqrt(push.x * push.x + push.z * push.z);
      }
      victim.push(push.x / horizontal * horizontalStrength, verticalStrength, push.z / horizontal * horizontalStrength);
      victim.hurtMarked = true;
   }

   private static void breakMedusaRideBlocks(MedusaPegasusEntity pegasus, Vec3 desired) {
      if (!(pegasus.level() instanceof ServerLevel level) || desired.lengthSqr() < 1.0E-4) {
         return;
      }
      Vec3 forward = new Vec3(desired.x, 0.0, desired.z);
      if (forward.lengthSqr() < 1.0E-4) {
         return;
      }
      forward = forward.normalize();
      BlockPos base = pegasus.blockPosition();
      boolean charging = desired.horizontalDistanceSqr() > 1.0;
      int length = charging ? 7 : 4;
      int radius = charging ? 2 : 1;
      int height = charging ? 4 : 2;
      int broken = 0;
      int maxBroken = charging ? 80 : 24;
      for (int i = 0; i < length; i++) {
         BlockPos check = base.offset((int)Math.round(forward.x * (i + 1)), 0, (int)Math.round(forward.z * (i + 1)));
         for (BlockPos pos : BlockPos.betweenClosed(check.offset(-radius, -1, -radius), check.offset(radius, height, radius))) {
            if (destroyMedusaRideBlock(level, pos, charging) && ++broken >= maxBroken) {
               break;
            }
         }
         if (broken >= maxBroken) {
            break;
         }
      }
      if (charging && pegasus.tickCount % 3 == 0) {
         level.sendParticles(ParticleTypes.EXPLOSION, pegasus.getX(), pegasus.getY() + 0.4, pegasus.getZ(), 2, 0.35, 0.2, 0.35, 0.0);
         level.sendParticles(ParticleTypes.CLOUD, pegasus.getX(), pegasus.getY() + 0.2, pegasus.getZ(), 12, 0.9, 0.2, 0.9, 0.08);
      }
   }

   private static boolean destroyMedusaRideBlock(ServerLevel level, BlockPos pos, boolean charging) {
      BlockState state = level.getBlockState(pos);
      float hardness = state.getDestroySpeed(level, pos);
      if (!state.isAir() && hardness >= 0.0F && hardness < (charging ? 75.0F : 30.0F) && !state.is(Blocks.BEDROCK)) {
         level.removeBlock(pos, false);
         return true;
      }
      return false;
   }

   private static void startMedusaBellerophonSummonFx(ServerLevel level, ServerPlayer player) {
      for (int i = 0; i < MEDUSA_BELLEROPHON_WINDUP_TICKS; i += 4) {
         final int step = i;
         TYPE_MOON_WORLD.queueServerWork(step, () -> {
            if (!player.isAlive() || player.level() != level) {
               return;
            }
            double radius = 0.8 + step * 0.03;
            for (int sample = 0; sample < 14; sample++) {
               double angle = (Math.PI * 2.0 * sample) / 14.0 + step * 0.08;
               double x = player.getX() + Math.cos(angle) * radius;
               double z = player.getZ() + Math.sin(angle) * radius;
               double y = player.getY() + 0.15 + (sample % 3) * 0.25;
               level.sendParticles(MEDUSA_SUMMON_LIGHT_PARTICLE, x, y, z, 1, 0.01, 0.01, 0.01, 0.0);
               level.sendParticles(MEDUSA_SUMMON_GOLD_PARTICLE, x, y + 0.1, z, 1, 0.01, 0.01, 0.01, 0.0);
               if ((sample & 1) == 0) {
                  level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.02, 0.02, 0.02, 0.0);
               }
            }
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0, player.getZ(), 8, 0.22, 0.45, 0.22, 0.02);
            level.sendParticles(ParticleTypes.GLOW, player.getX(), player.getY() + 1.1, player.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
         });
      }
   }

   private static void spawnMedusaPegasusArrivalFx(ServerLevel level, MedusaPegasusEntity pegasus) {
      level.sendParticles(ParticleTypes.FLASH, pegasus.getX(), pegasus.getY() + 1.0, pegasus.getZ(), 2, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.END_ROD, pegasus.getX(), pegasus.getY() + 0.9, pegasus.getZ(), 10, 0.22, 0.35, 0.22, 0.015);
      level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, pegasus.getX(), pegasus.getY() + 0.9, pegasus.getZ(), 8, 0.25, 0.35, 0.25, 0.015);
      level.sendParticles(MEDUSA_SUMMON_LIGHT_PARTICLE, pegasus.getX(), pegasus.getY() + 0.8, pegasus.getZ(), 14, 0.28, 0.42, 0.28, 0.0);
      level.sendParticles(MEDUSA_SUMMON_GOLD_PARTICLE, pegasus.getX(), pegasus.getY() + 0.8, pegasus.getZ(), 10, 0.28, 0.42, 0.28, 0.0);
      level.sendParticles(ParticleTypes.CLOUD, pegasus.getX(), pegasus.getY() + 0.2, pegasus.getZ(), 12, 0.45, 0.12, 0.45, 0.03);
      level.playSound(null, pegasus.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.9F, 1.35F);
      level.playSound(null, pegasus.blockPosition(), SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.PLAYERS, 0.85F, 0.8F);
   }

   private static void startMedusaBloodfortSummonSequence(ServerLevel level, Vec3 center, double radius, boolean noble) {
      VFXServerEffects.spawn(level, noble ? "servant_medusa_bloodfort_np" : "servant_medusa_bloodfort", center, 160.0);
      spawnMedusaBloodfortCastBurst(level, center, radius, noble);
      spawnMedusaBloodfortGroundSigil(level, center, radius, noble);
      spawnMedusaBloodfortNodeClusters(level, center, radius, noble);
      TYPE_MOON_WORLD.queueServerWork(5, () -> spawnMedusaBloodfortNodeLinks(level, center, radius, noble));
      TYPE_MOON_WORLD.queueServerWork(10, () -> {
         spawnMedusaBloodfortRisingShell(level, center, radius, noble);
         spawnMedusaBloodfortVerticalPulse(level, center, radius, noble);
      });
      TYPE_MOON_WORLD.queueServerWork(16, () -> {
         spawnMedusaBloodfortGroundSigil(level, center, radius, noble);
         spawnMedusaBloodfortNodeLinks(level, center, radius, noble);
      });
   }

   private static void spawnMedusaBloodfortCastBurst(ServerLevel level, Vec3 center, double radius, boolean noble) {
      int burstCount = noble ? 120 : 70;
      level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + 1.2, center.z, burstCount / 3, radius * 0.25, radius * 0.12, radius * 0.25, 0.06);
      level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y + 1.0, center.z, burstCount / 2, radius * 0.22, 1.2, radius * 0.22, 0.04);
      level.sendParticles(MEDUSA_BLOODFORT_PARTICLE, center.x, center.y + 1.4, center.z, burstCount, radius * 0.25, 1.4, radius * 0.25, 0.0);
      level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y + 0.8, center.z, burstCount / 4, radius * 0.18, 0.8, radius * 0.18, 0.01);
   }

   private static void spawnMedusaBloodfortShell(ServerLevel level, Vec3 center, double radius) {
      int ringSamples = 72;
      for (int ring = 0; ring <= 4; ring++) {
         double phi = (ring / 4.0) * (Math.PI / 2.0);
         double ringRadius = Math.sin(phi) * radius;
         double y = center.y + Math.cos(phi) * radius;
         for (int i = 0; i < ringSamples; i++) {
            double theta = (Math.PI * 2.0 * i) / ringSamples;
            double x = center.x + Math.cos(theta) * ringRadius;
            double z = center.z + Math.sin(theta) * ringRadius;
            level.sendParticles(MEDUSA_BLOODFORT_PARTICLE, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
            if ((i & 3) == 0) {
               level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y, z, 1, 0.01, 0.01, 0.01, 0.0);
            }
            if ((i % 6) == 0) {
               level.sendParticles(MEDUSA_BLOODFORT_LINK_PARTICLE, x, y, z, 1, 0.01, 0.01, 0.01, 0.0);
            }
         }
      }
   }

   private static void spawnMedusaBloodfortInteriorHaze(ServerLevel level, Vec3 center, double radius, boolean noble) {
      int hazeCount = noble ? 90 : 48;
      double verticalSpread = noble ? radius * 0.95 : radius * 0.8;
      for (int i = 0; i < hazeCount; i++) {
         double angle = level.random.nextDouble() * Math.PI * 2.0;
         double dist = Math.sqrt(level.random.nextDouble()) * radius * 0.92;
         double x = center.x + Math.cos(angle) * dist;
         double z = center.z + Math.sin(angle) * dist;
         double y = center.y + 0.3 + level.random.nextDouble() * verticalSpread;
         level.sendParticles(MEDUSA_BLOODFORT_PARTICLE, x, y, z, 1, 0.06, 0.03, 0.06, 0.0);
         if ((i % 3) == 0) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 1, 0.04, 0.02, 0.04, 0.0);
         }
      }
   }

   private static void spawnMedusaBloodfortRisingShell(ServerLevel level, Vec3 center, double radius, boolean noble) {
      level.addFreshEntity(new GravityFieldShellEffectEntity(level, center.x, center.y + 0.02, center.z, (float)(radius * 1.02), (float)(radius * 0.82), noble ? 0.72F : 0.58F, noble ? 32 : 26, 0.86F, 0.06F, 0.11F, noble ? 14 : 10));
   }

   private static void spawnMedusaBloodfortVerticalPulse(ServerLevel level, Vec3 center, double radius, boolean noble) {
      int shafts = noble ? 12 : 8;
      for (int i = 0; i < shafts; i++) {
         double angle = (Math.PI * 2.0 * i) / shafts;
         double ringRadius = radius * 0.78;
         double baseX = center.x + Math.cos(angle) * ringRadius;
         double baseZ = center.z + Math.sin(angle) * ringRadius;
         for (double t = 0.0; t <= 1.0; t += 0.12) {
            double y = center.y + t * radius * 0.9;
            level.sendParticles(MEDUSA_BLOODFORT_SIGIL_PARTICLE, baseX, y, baseZ, 1, 0.0, 0.0, 0.0, 0.0);
         }
      }
      level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + radius * 0.45, center.z, noble ? 36 : 20, radius * 0.22, radius * 0.3, radius * 0.22, 0.01);
   }

   private static void spawnMedusaBloodfortGroundSigil(ServerLevel level, Vec3 center, double radius, boolean noble) {
      double y = center.y + 0.04;
      spawnMedusaBloodfortRing(level, center, y, radius * 0.82, 90, MEDUSA_BLOODFORT_SIGIL_PARTICLE);
      spawnMedusaBloodfortRing(level, center, y, radius * 0.58, 70, MEDUSA_BLOODFORT_LINK_PARTICLE);
      spawnMedusaBloodfortRing(level, center, y, radius * 0.26, 42, MEDUSA_BLOODFORT_NODE_PARTICLE);
      Vec3[] outerPentagram = createMedusaRegularPolygon(center, y, radius * 0.64, 5, -Math.PI / 2.0);
      for (int i = 0; i < outerPentagram.length; i++) {
         spawnMedusaBloodfortLine(level, outerPentagram[i], outerPentagram[(i + 2) % outerPentagram.length], MEDUSA_BLOODFORT_LINK_PARTICLE, 0.42);
         spawnMedusaBloodfortLine(level, outerPentagram[i], center.add(0.0, 0.04, 0.0), MEDUSA_BLOODFORT_SIGIL_PARTICLE, 0.5);
      }
      spawnMedusaBloodfortPeripheralSigils(level, center, radius, noble);
   }

   private static void spawnMedusaBloodfortNodeClusters(ServerLevel level, Vec3 center, double radius, boolean noble) {
      Vec3[] majorNodes = createMedusaRegularPolygon(center, center.y + 0.06, radius * 0.74, 5, -Math.PI / 2.0);
      Vec3[] minorNodes = createMedusaRegularPolygon(center, center.y + 0.06, radius * 0.44, 5, Math.PI / 10.0);
      for (Vec3 node : majorNodes) {
         level.sendParticles(MEDUSA_BLOODFORT_NODE_PARTICLE, node.x, node.y, node.z, 7, 0.08, 0.02, 0.08, 0.0);
         level.sendParticles(ParticleTypes.FLAME, node.x, node.y + 0.1, node.z, 2, 0.06, 0.02, 0.06, 0.0);
      }
      for (Vec3 node : minorNodes) {
         level.sendParticles(MEDUSA_BLOODFORT_SIGIL_PARTICLE, node.x, node.y, node.z, noble ? 5 : 3, 0.08, 0.02, 0.08, 0.0);
      }
   }

   private static void spawnMedusaBloodfortNodeLinks(ServerLevel level, Vec3 center, double radius, boolean noble) {
      Vec3[] majorNodes = createMedusaRegularPolygon(center, center.y + 0.06, radius * 0.74, 5, -Math.PI / 2.0);
      Vec3[] minorNodes = createMedusaRegularPolygon(center, center.y + 0.06, radius * 0.44, 5, Math.PI / 10.0);
      for (int i = 0; i < majorNodes.length; i++) {
         spawnMedusaBloodfortLine(level, majorNodes[i], majorNodes[(i + 1) % majorNodes.length], MEDUSA_BLOODFORT_LINK_PARTICLE, 0.38);
         spawnMedusaBloodfortLine(level, majorNodes[i], minorNodes[i], MEDUSA_BLOODFORT_NODE_PARTICLE, 0.34);
         spawnMedusaBloodfortLine(level, minorNodes[i], center.add(0.0, 0.06, 0.0), MEDUSA_BLOODFORT_SIGIL_PARTICLE, 0.3);
      }
      level.sendParticles(ParticleTypes.FLASH, center.x, center.y + 0.1, center.z, 1, 0.05, 0.02, 0.05, 0.0);
   }

   private static void spawnMedusaBloodfortPeripheralSigils(ServerLevel level, Vec3 center, double radius, boolean noble) {
      double y = center.y + 0.035;
      int sigilCount = noble ? 10 : 7;
      double rotation = level.getGameTime() * (noble ? 0.022 : 0.016);
      for (int i = 0; i < sigilCount; i++) {
         double angle = -Math.PI / 2.0 + (Math.PI * 2.0 * i) / sigilCount + rotation;
         Vec3 sigilCenter = new Vec3(center.x + Math.cos(angle) * radius * 0.86, y, center.z + Math.sin(angle) * radius * 0.86);
         double miniRadius = radius * (noble ? 0.12 : 0.095);
         spawnMedusaBloodfortRing(level, sigilCenter, y, miniRadius, 18, MEDUSA_BLOODFORT_SIGIL_PARTICLE);
         spawnMedusaBloodfortRing(level, sigilCenter, y, miniRadius * 0.52, 10, MEDUSA_BLOODFORT_LINK_PARTICLE);
      }
   }

   private static void spawnMedusaBloodfortRing(ServerLevel level, Vec3 center, double y, double radius, int samples, DustParticleOptions particle) {
      for (int i = 0; i < samples; i++) {
         double theta = (Math.PI * 2.0 * i) / samples;
         level.sendParticles(particle, center.x + Math.cos(theta) * radius, y, center.z + Math.sin(theta) * radius, 1, 0.0, 0.0, 0.0, 0.0);
      }
   }

   private static Vec3[] createMedusaRegularPolygon(Vec3 center, double y, double radius, int sides, double angleOffset) {
      Vec3[] points = new Vec3[sides];
      for (int i = 0; i < sides; i++) {
         double angle = angleOffset + (Math.PI * 2.0 * i) / sides;
         points[i] = new Vec3(center.x + Math.cos(angle) * radius, y, center.z + Math.sin(angle) * radius);
      }
      return points;
   }

   private static void spawnMedusaBloodfortLine(ServerLevel level, Vec3 from, Vec3 to, DustParticleOptions particle, double spacing) {
      Vec3 delta = to.subtract(from);
      double distance = delta.length();
      if (distance < 1.0E-4) {
         return;
      }
      Vec3 step = delta.normalize().scale(spacing);
      for (double traveled = 0.0; traveled <= distance; traveled += spacing) {
         Vec3 pos = from.add(step.scale(traveled / spacing));
         level.sendParticles(particle, pos.x, pos.y, pos.z, 1, 0.01, 0.01, 0.01, 0.0);
      }
   }

   private static void spawnMedusaBloodfortVictimAura(ServerLevel level, LivingEntity victim, float dealt) {
      int bloodCount = Math.max(8, Math.min(22, Math.round(dealt / 2.0F)));
      level.sendParticles(MEDUSA_BLOODFORT_PARTICLE, victim.getX(), victim.getY() + victim.getBbHeight() * 0.55, victim.getZ(), bloodCount, 0.25, 0.35, 0.25, 0.0);
      level.sendParticles(ParticleTypes.DRIPPING_LAVA, victim.getX(), victim.getY() + victim.getBbHeight() * 0.4, victim.getZ(), Math.max(4, bloodCount / 3), 0.18, 0.22, 0.18, 0.0);
      level.sendParticles(ParticleTypes.LARGE_SMOKE, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(), Math.max(3, bloodCount / 4), 0.18, 0.2, 0.18, 0.0);
   }


   public static void performSnare(ServerPlayer player) {
      LivingEntity target = findLookTarget(player, 26.0, 1.8);
      if (target == null) {
         ItemStack dagger = new ItemStack(ModItems.NAMELESS_CHAIN_DAGGER.get());
         PlayerNoblePhantasmHelper.markUbwProjection(dagger);
         ServantCardTransformManager.markGeneratedItem(dagger, true, false);
         player.setItemInHand(InteractionHand.MAIN_HAND, dagger);
         return;
      }
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 4, false, true, true));
      target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 1, false, true, true));
      if (player.level() instanceof ServerLevel level) {
         spawnLineParticles(level, player.getEyePosition(), target.position().add(0.0, target.getBbHeight() * 0.55, 0.0), ParticleTypes.CRIT);
         level.playSound(null, target.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0F, 0.8F);
      }
   }

   public static void performMedusaMonsterStrength(ServerPlayer player) {
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 220, 2, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 1, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 0, false, true, true));
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 dir = PlayerNoblePhantasmHelper.horizontalLook(player);
      hitForwardArc(player, dir, 5.2, 28.0F);
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(4.5, 1.8, 4.5), e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e))) {
         Vec3 away = living.position().subtract(player.position());
         if (away.lengthSqr() > 0.01) {
            Vec3 push = new Vec3(away.x, 0.0, away.z).normalize();
            living.push(push.x * 0.65, 0.18, push.z * 0.65);
            living.hurtMarked = true;
         }
      }
      level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 0.9, player.getZ(), 32, 0.5, 0.45, 0.5, 0.08);
      level.sendParticles(ParticleTypes.DRAGON_BREATH, player.getX(), player.getY() + 0.9, player.getZ(), 18, 0.35, 0.35, 0.35, 0.035);
      level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.1F, 0.72F);
   }

   public static void performViperRush(ServerPlayer player) {
      LivingEntity target = findLookTarget(player, 13.0, 1.8);
      Vec3 dir = target == null ? PlayerNoblePhantasmHelper.horizontalLook(player) : target.position().subtract(player.position()).normalize();
      player.setDeltaMovement(player.getDeltaMovement().add(dir.x * 2.0, 0.16, dir.z * 2.0));
      player.hurtMarked = true;
      hitForwardArc(player, dir, 5.0, 24.0F);
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 0.7, player.getZ(), 20, 0.35, 0.22, 0.35, 0.08);
      }
   }

   public static void performSerpentStep(ServerPlayer player) {
      if (player.level() instanceof ServerLevel level) {
         Vec3 right = new Vec3(-PlayerNoblePhantasmHelper.horizontalLook(player).z, 0.0, PlayerNoblePhantasmHelper.horizontalLook(player).x);
         double side = player.getRandom().nextBoolean() ? 4.0 : -4.0;
         level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 0.5, player.getZ(), 12, 0.18, 0.12, 0.18, 0.03);
         player.teleportTo(player.getX() + right.x * side, player.getY() + 0.1, player.getZ() + right.z * side);
         player.hurtMarked = true;
         level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 0.5, player.getZ(), 12, 0.18, 0.12, 0.18, 0.03);
      }
   }

   private static void performMedusaCybele(ServerPlayer player) {
      LivingEntity target = findLookTarget(player, 18.0, 2.0);
      if (target == null) {
         return;
      }
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 5, false, true, true));
      target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 2, false, true, true));
      target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 160, 2, false, true, true));
      target.invulnerableTime = 0;
      target.hurt(player.damageSources().playerAttack(player), 24.0F);
      target.invulnerableTime = 0;
      if (player.level() instanceof ServerLevel level) {
         VFXServerEffects.spawn(level, "servant_medusa_cybele", target, 128.0);
         level.sendParticles(ParticleTypes.DRAGON_BREATH, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 38, 0.55, 0.7, 0.55, 0.025);
         level.playSound(null, target.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 0.85F, 1.35F);
      }
   }

   public static void performMysticEyesToggle(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      boolean active = !data.getBoolean(MEDUSA_EYES_ACTIVE_TAG);
      data.putBoolean(MEDUSA_EYES_ACTIVE_TAG, active);
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.servant_card_medusa_mystic_eyes_active = active;
      vars.syncPlayerVariables(player);
      if (player.level() instanceof ServerLevel level) {
         if (active) {
            VFXServerEffects.spawn(level, "servant_medusa_cybele", player, 96.0);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getEyeY(), player.getZ(), 14, 0.22, 0.12, 0.22, 0.015);
            level.sendParticles(ParticleTypes.GLOW, player.getX(), player.getEyeY(), player.getZ(), 18, 0.28, 0.16, 0.28, 0.02);
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.85F, 0.75F);
         } else {
            level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getEyeY(), player.getZ(), 10, 0.18, 0.12, 0.18, 0.02);
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.55F, 1.45F);
         }
      }
      player.displayClientMessage(Component.translatable(active ? "message.typemoonworld.servant_card.medusa_eyes_on" : "message.typemoonworld.servant_card.medusa_eyes_off"), true);
   }

   private static void performMedusaCharm(ServerPlayer player) {
      LivingEntity target = findLookTarget(player, 16.0, 2.0);
      if (target == null) {
         return;
      }
      target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0, false, true, true));
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1, false, true, true));
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.HEART, target.getX(), target.getY() + target.getBbHeight(), target.getZ(), 10, 0.35, 0.25, 0.35, 0.02);
         level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.45F);
      }
   }

   public static void performBloodfort(ServerPlayer player, boolean noble) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      double radius = MEDUSA_BLOODFORT_RADIUS;
      Vec3 center = player.position().add(0.0, 0.1, 0.0);
      CompoundTag data = player.getPersistentData();
      long now = level.getGameTime();
      data.putLong(MEDUSA_LAST_BLOODFORT_TICK_TAG, now);
      data.putLong(MEDUSA_BLOODFORT_UNTIL_TAG, now + (noble ? 600L : 300L));
      data.putDouble(MEDUSA_BLOODFORT_RADIUS_TAG, radius);
      data.putDouble(MEDUSA_BLOODFORT_X_TAG, center.x);
      data.putDouble(MEDUSA_BLOODFORT_Y_TAG, center.y);
      data.putDouble(MEDUSA_BLOODFORT_Z_TAG, center.z);
      data.putBoolean(MEDUSA_BLOODFORT_NP_ACTIVE_TAG, noble);
      startMedusaBloodfortSummonSequence(level, center, radius, noble);
      level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8F, 0.65F);
   }

   public static void performBellerophon(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      CompoundTag data = player.getPersistentData();
      MedusaPegasusEntity oldPegasus = getMedusaPegasus(player);
      if (oldPegasus != null && oldPegasus.isAlive()) {
         oldPegasus.discard();
      }
      data.putLong(MEDUSA_BELLEROPHON_LAUNCH_TICK_TAG, level.getGameTime() + MEDUSA_BELLEROPHON_WINDUP_TICKS);
      data.putBoolean(MEDUSA_EYES_ACTIVE_TAG, true);
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.servant_card_medusa_mystic_eyes_active = true;
      vars.syncPlayerVariables(player);
      VFXServerEffects.spawn(level, "servant_medusa_bellerophon", player, 160.0);
      startMedusaBellerophonSummonFx(level, player);
      level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 0.85F);
   }


   private static void remove(AttributeInstance attribute, ResourceLocation id) {
      if (attribute != null) {
         attribute.removeModifier(id);
      }
   }

   private static LivingEntity findLookTarget(ServerPlayer player, double range, double inflate) {
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle().normalize();
      AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(inflate);
      LivingEntity best = null;
      double bestScore = 0.78;
      for (LivingEntity living : player.level().getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e))) {
         Vec3 to = living.position().add(0.0, living.getBbHeight() * 0.5, 0.0).subtract(eye);
         double distance = to.length();
         if (distance <= 0.01 || distance > range) {
            continue;
         }
         double score = look.dot(to.normalize());
         if (score > bestScore) {
            bestScore = score;
            best = living;
         }
      }
      return best;
   }

   private static void hitForwardArc(ServerPlayer player, Vec3 dir, double range, float damage) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 origin = player.position().add(0.0, player.getBbHeight() * 0.45, 0.0);
      Vec3 forward = dir.lengthSqr() < 1.0E-4 ? PlayerNoblePhantasmHelper.horizontalLook(player) : new Vec3(dir.x, 0.0, dir.z).normalize();
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range, 2.0, range), e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e))) {
         Vec3 to = living.position().add(0.0, living.getBbHeight() * 0.35, 0.0).subtract(origin);
         if (to.length() > range || forward.dot(new Vec3(to.x, 0.0, to.z).normalize()) < 0.35) {
            continue;
         }
         living.invulnerableTime = 0;
         living.hurt(player.damageSources().playerAttack(player), damage);
         living.invulnerableTime = 0;
      }
   }

   private static void spawnLineParticles(ServerLevel level, Vec3 start, Vec3 end, net.minecraft.core.particles.SimpleParticleType particle) {
      Vec3 delta = end.subtract(start);
      double length = delta.length();
      if (length <= 0.01) {
         return;
      }
      for (double d = 0.0; d <= length; d += 0.45) {
         Vec3 pos = start.add(delta.normalize().scale(d));
         level.sendParticles(particle, pos.x, pos.y, pos.z, 2, 0.03, 0.03, 0.03, 0.0);
      }
   }
}
