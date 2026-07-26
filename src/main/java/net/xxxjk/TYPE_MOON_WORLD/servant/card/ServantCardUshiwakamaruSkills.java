package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.UshiwakamaruCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.UshiwakamaruCombatRules;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.UshiwakamaruRiderEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

public final class ServantCardUshiwakamaruSkills {
   public static final String SERVANT_ID = "ushiwakamaru_rider";
   public static final String TAG_SHIELD_HP = "ServantCardUshiwakamaruBenkeiShieldHp";
   public static final String TAG_SHIELD_UNTIL = "ServantCardUshiwakamaruBenkeiShieldUntil";
   public static final String TAG_EIGHT_BOAT_UNTIL = "ServantCardUshiwakamaruEightBoatUntil";
   public static final String TAG_EIGHT_BOAT_TARGET = "ServantCardUshiwakamaruEightBoatTarget";
   public static final String TAG_CLONE_UUIDS = "ServantCardUshiwakamaruCloneUuids";
   private static final String TAG_TENGU_UNTIL = "ServantCardUshiwakamaruTenguUntil";
   private static final ResourceLocation TENGU_SPEED_ID = id("servant_card_ushiwakamaru_tengu_speed");
   private static final ResourceLocation TENGU_ATTACK_SPEED_ID = id("servant_card_ushiwakamaru_tengu_attack_speed");
   private static final ResourceLocation TENGU_DAMAGE_ID = id("servant_card_ushiwakamaru_tengu_damage");
   private static final ResourceLocation CHARISMA_DAMAGE_ID = id("servant_card_ushiwakamaru_charisma_damage");
   private static final ResourceLocation SIX_SECRET_ALLY_SPEED_ID = id("servant_card_ushiwakamaru_six_secret_ally_speed");
   private static final ResourceLocation SIX_SECRET_ENEMY_SPEED_ID = id("servant_card_ushiwakamaru_six_secret_enemy_speed");
   private static final ResourceLocation EIGHT_BOAT_SPEED_ID = id("servant_card_ushiwakamaru_eight_boat_speed");

   private ServantCardUshiwakamaruSkills() {
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      CompoundTag data = player.getPersistentData();
      long now = player.level().getGameTime();
      if (data.getLong(TAG_TENGU_UNTIL) <= now) {
         removeTenguModifiers(player);
         data.remove(TAG_TENGU_UNTIL);
      }
      if (data.getLong(TAG_SHIELD_UNTIL) <= now) {
         data.remove(TAG_SHIELD_UNTIL);
         data.remove(TAG_SHIELD_HP);
      } else if (player.level() instanceof ServerLevel level && (player.tickCount & 3) == 0) {
         spawnShieldParticles(player, level);
      }
      if (!isEightBoatActive(player)) {
         finishEightBoat(player, vars);
      } else {
         updateModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), EIGHT_BOAT_SPEED_ID, 2.0,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
         if (!eightBoatTargetAlive(player)) data.remove(TAG_EIGHT_BOAT_TARGET);
      }
   }

   public static void clear(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      removeTenguModifiers(player);
      player.getPersistentData().remove(TAG_TENGU_UNTIL);
      player.getPersistentData().remove(TAG_SHIELD_HP);
      player.getPersistentData().remove(TAG_SHIELD_UNTIL);
      player.getPersistentData().remove(TAG_EIGHT_BOAT_UNTIL);
      finishEightBoat(player, vars);
   }

   public static void performTenguStrategy(ServerPlayer player) {
      long until = player.level().getGameTime() + 12L * 20L;
      CompoundTag data = player.getPersistentData();
      data.putLong(TAG_TENGU_UNTIL, until);
      updateModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), TENGU_SPEED_ID, 0.30,
         AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
      updateModifier(player.getAttribute(Attributes.ATTACK_SPEED), TENGU_ATTACK_SPEED_ID, 0.20,
         AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
      updateModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), TENGU_DAMAGE_ID, 0.15,
         AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
      if (player.level() instanceof ServerLevel level) {
         VFXServerEffects.spawn(level, "servant_ushiwakamaru_tengu_strategy", player, 96.0);
         level.playSound(null, player.blockPosition(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 0.9F, 1.45F);
      }
   }

   public static void performCharisma(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return;
      long until = level.getGameTime() + 20L * 20L;
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(15.0),
         entity -> entity.isAlive() && isAlly(player, entity))) {
         applyTimedModifier(living, Attributes.ATTACK_DAMAGE, CHARISMA_DAMAGE_ID, 0.15,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE, until, 20 * 20);
      }
      VFXServerEffects.spawn(level, "servant_ushiwakamaru_charisma", player, 96.0);
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 28, 1.2, 0.7, 1.2, 0.05);
      level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.35F);
   }

   public static void performMoonlitStep(ServerPlayer player) {
      LivingEntity target = findEnemyLookTarget(player, 12.0);
      if (target == null) return;
      Vec3 forward = target.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (forward.lengthSqr() < 1.0E-4) forward = target.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
      if (forward.lengthSqr() < 1.0E-4) forward = new Vec3(0.0, 0.0, 1.0);
      forward = forward.normalize();
      Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
      Vec3[] candidates = {
         target.position().subtract(forward.scale(1.35)),
         target.position().add(side.scale(1.45)),
         target.position().subtract(side.scale(1.45))
      };
      for (Vec3 candidate : candidates) {
         if (ServantCardSkillUtils.trySafeHorizontalTeleport(player, new Vec3(candidate.x, target.getY(), candidate.z))) break;
      }
      player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,
         target.position().add(0.0, target.getBbHeight() * 0.5, 0.0));
      float damage = (float)(player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.2 + 6.0);
      hurtPhysical(player, target, damage);
      if (player.level() instanceof ServerLevel level) {
         Vec3 direction = target.position().subtract(player.position());
         VFXServerEffects.spawnOriented(level, "servant_ushiwakamaru_slash",
            player.position().add(0.0, player.getBbHeight() * 0.48, 0.0), direction, 96.0);
         level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 0.8, player.getZ(), 18, 0.3, 0.45, 0.3, 0.08);
         level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.55F);
      }
   }

   public static void performSweepingThrust(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return;
      Vec3 direction = horizontalLook(player);
      Vec3 origin = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
      Vec3 end = origin.add(direction.scale(5.0));
      AABB area = new AABB(origin, end).inflate(1.35, 1.25, 1.35);
      float damage = (float)(player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.9 + 5.0);
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
         entity -> isEnemy(player, entity) && insideThrust(player, entity, direction))) {
         hurtPhysical(player, target, damage);
         target.push(direction.x * 0.7, 0.1, direction.z * 0.7);
         target.hurtMarked = true;
      }
      player.setDeltaMovement(direction.x * 1.05, Math.max(0.12, player.getDeltaMovement().y), direction.z * 1.05);
      player.hurtMarked = true;
      VFXServerEffects.spawnOriented(level, "servant_ushiwakamaru_slash", origin, direction, 96.0);
      level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 0.95F, 1.65F);
   }

   public static void performEagleDrop(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return;
      LivingEntity target = findEnemyLookTarget(player, 16.0);
      Vec3 start = player.getEyePosition();
      Vec3 end = start.add(player.getLookAngle().normalize().scale(16.0));
      HitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
      Vec3 destination = target != null ? target.position() : blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
      Vec3 oldPosition = player.position();
      ServantCardSkillUtils.trySafeHorizontalTeleport(player, destination.add(0.0, 0.1, 0.0));
      player.setDeltaMovement(Vec3.ZERO);
      player.fallDistance = 0.0F;
      float damage = (float)(player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5 + 10.0);
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(3.0),
         entity -> isEnemy(player, entity))) {
         hurtPhysical(player, living, damage);
         Vec3 away = living.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
         if (away.lengthSqr() < 1.0E-4) away = horizontalLook(player);
         away = away.normalize();
         living.push(away.x * 1.1, 0.35, away.z * 1.1);
         living.hurtMarked = true;
      }
      ServantCardSkillUtils.spawnLineParticles(level, oldPosition.add(0.0, 1.0, 0.0), player.position().add(0.0, 0.4, 0.0), ParticleTypes.END_ROD);
      VFXServerEffects.spawn(level, "servant_ushiwakamaru_usumidori_impact", player.position(), 96.0);
      level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX(), player.getY() + 0.3, player.getZ(), 12, 1.3, 0.2, 1.3, 0.0);
      level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.2F, 0.8F);
   }

   public static void performSixSecret(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return;
      long until = level.getGameTime() + 8L * 20L;
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(15.0), LivingEntity::isAlive)) {
         boolean ally = isAlly(player, living);
         applyTimedModifier(living, Attributes.MOVEMENT_SPEED,
            ally ? SIX_SECRET_ALLY_SPEED_ID : SIX_SECRET_ENEMY_SPEED_ID,
            ally ? 0.30 : -0.30, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, until, 8 * 20);
         if (!ally) living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 8 * 20, 0, false, true, true));
      }
      VFXServerEffects.spawn(level, "servant_ushiwakamaru_six_secret", player, 96.0);
      level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 0.3, player.getZ(), 80, 7.0, 0.5, 7.0, 0.05);
      level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.1F, 0.85F);
   }

   public static void performUsumidori(ServerPlayer player) {
      LivingEntity target = findEnemyLookTarget(player, 14.0);
      if (target == null || player.distanceTo(target) < 2.8) return;
      Vec3 direction = target.position().subtract(player.position());
      player.setDeltaMovement(direction.normalize().scale(1.8));
      player.hurtMarked = true;
      CompoundTag data = player.getPersistentData();
      long now = player.level().getGameTime();
      data.putLong(UshiwakamaruCombatHelper.TAG_GUARANTEED_HIT_UNTIL, now + 1L);
      try {
         hurtPhysical(player, target, 150.0F);
      } finally {
         data.remove(UshiwakamaruCombatHelper.TAG_GUARANTEED_HIT_UNTIL);
      }
      if (player.level() instanceof ServerLevel level) {
         VFXServerEffects.spawnOriented(level, "servant_ushiwakamaru_usumidori",
            player.position().add(0.0, player.getBbHeight() * 0.48, 0.0), direction, 128.0);
         VFXServerEffects.spawn(level, "servant_ushiwakamaru_usumidori_impact",
            target.position().add(0.0, target.getBbHeight() * 0.5, 0.0), 128.0);
         level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.4F, 1.1F);
      }
   }

   public static void performBenkei(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      data.putFloat(TAG_SHIELD_HP, UshiwakamaruCombatRules.SHIELD_MAX_HP);
      data.putLong(TAG_SHIELD_UNTIL, player.level().getGameTime() + 15L * 20L);
      if (player.level() instanceof ServerLevel level) {
         VFXServerEffects.spawn(level, "servant_ushiwakamaru_benkei_shield", player, 96.0);
         spawnShieldParticles(player, level);
         level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.2F, 0.75F);
      }
   }

   public static boolean performSpiderSlayer(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return false;
      List<LivingEntity> targets = nearbyEnemies(player, 15.0);
      if (targets.isEmpty()) return false;
      VFXServerEffects.spawn(level, "servant_ushiwakamaru_spider_slayer", player, 128.0);
      for (LivingEntity target : targets) {
         float damage = ServantCardUshiwakamaruRules.spiderSlayerDamage(
            ServantCardSkillUtils.hasTrait(target, ServantTraitTag.DEMONIC));
         target.invulnerableTime = 0;
         target.hurt(player.damageSources().magic(), damage);
         target.invulnerableTime = 0;
         VFXServerEffects.spawn(level, "servant_ushiwakamaru_spider_slayer_impact",
            target.position().add(0.0, target.getBbHeight() * 0.5, 0.0), 128.0);
      }
      level.sendParticles(ParticleTypes.SONIC_BOOM, player.getX(), player.getY() + 1.0, player.getZ(), 20, 5.5, 1.0, 5.5, 0.0);
      level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.4F, 1.35F);
      return true;
   }

   public static boolean performEightBoat(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!(player.level() instanceof ServerLevel level)) return false;
      LivingEntity target = findEnemyLookTarget(player, 32.0);
      if (target == null) return false;
      cleanupClones(player);
      long expiresAt = level.getGameTime() + 15L * 20L;
      CompoundTag data = player.getPersistentData();
      data.putLong(TAG_EIGHT_BOAT_UNTIL, expiresAt);
      data.putUUID(TAG_EIGHT_BOAT_TARGET, target.getUUID());
      vars.servant_card_jump_charges = 8;
      vars.servant_card_jump_recovery_ticks = 0;
      vars.servant_card_jump_recovery_end = 0L;
      spawnClones(player, target, level, expiresAt);
      updateModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), EIGHT_BOAT_SPEED_ID, 2.0,
         AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
      VFXServerEffects.spawn(level, "servant_ushiwakamaru_eight_boat", player, 128.0);
      level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 0.9, player.getZ(), 60, 1.4, 0.8, 1.4, 0.12);
      level.playSound(null, player.blockPosition(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1.3F, 1.25F);
      vars.syncServantCardRuntime(player);
      return true;
   }

   public static boolean tryAbsorbShieldDamage(ServerPlayer player, DamageSource source, float amount) {
      if (amount <= 0.0F || source == null || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
      CompoundTag data = player.getPersistentData();
      long now = player.level().getGameTime();
      float shield = data.getFloat(TAG_SHIELD_HP);
      if (shield <= 0.0F || data.getLong(TAG_SHIELD_UNTIL) <= now) return false;
      UshiwakamaruCombatRules.ShieldHit hit = UshiwakamaruCombatRules.absorbShieldHit(shield, amount);
      if (hit.broken()) {
         data.remove(TAG_SHIELD_HP);
         data.remove(TAG_SHIELD_UNTIL);
      } else {
         data.putFloat(TAG_SHIELD_HP, hit.remainingShieldHp());
      }
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.55, 0.65, 0.55, 0.04);
         level.playSound(null, player.blockPosition(), hit.broken() ? SoundEvents.GLASS_BREAK : SoundEvents.SHIELD_BLOCK,
            SoundSource.PLAYERS, 1.1F, hit.broken() ? 0.65F : 1.25F);
      }
      return true;
   }

   public static boolean trySwallowDodge(ServerPlayer player, DamageSource source) {
      if (source == null || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
         || UshiwakamaruCombatHelper.isGuaranteedHit(source, player.level().getGameTime())) return false;
      Entity direct = source.getDirectEntity();
      Entity attacker = source.getEntity();
      if (!(attacker instanceof LivingEntity) || direct != attacker || player.distanceToSqr(attacker) > 25.0) return false;
      boolean fromAbove = attacker.getY() > player.getY() + player.getBbHeight() * 0.75;
      if (player.getRandom().nextFloat() >= UshiwakamaruCombatRules.swallowDodgeChance(fromAbove)) return false;
      Vec3 away = player.position().subtract(attacker.position()).multiply(1.0, 0.0, 1.0);
      if (away.lengthSqr() < 1.0E-4) away = new Vec3(1.0, 0.0, 0.0);
      away = away.normalize();
      player.setDeltaMovement(away.x * 0.95, Math.max(0.18, player.getDeltaMovement().y), away.z * 0.95);
      player.hurtMarked = true;
      if (player.level() instanceof ServerLevel level) {
         VFXServerEffects.spawn(level, "servant_ushiwakamaru_swallow_dodge", player, 96.0);
         level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.8, player.getZ(), 14, 0.35, 0.25, 0.35, 0.05);
         level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8F, 1.7F);
      }
      return true;
   }

   public static boolean hasMoonlitTarget(ServerPlayer player) {
      LivingEntity target = findEnemyLookTarget(player, 12.0);
      return target != null && player.distanceTo(target) >= 4.0;
   }

   public static boolean hasUsumidoriTarget(ServerPlayer player) {
      LivingEntity target = findEnemyLookTarget(player, 14.0);
      return target != null && player.distanceTo(target) >= 2.8;
   }

   public static boolean hasSpiderSlayerTargets(ServerPlayer player) {
      return !nearbyEnemies(player, 15.0).isEmpty();
   }

   public static boolean hasEightBoatTarget(ServerPlayer player) {
      return findEnemyLookTarget(player, 32.0) != null;
   }

   public static boolean isEightBoatActive(LivingEntity entity) {
      return entity != null && entity.getPersistentData().getLong(TAG_EIGHT_BOAT_UNTIL) > entity.level().getGameTime();
   }

   private static void finishEightBoat(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      CompoundTag data = player.getPersistentData();
      boolean hadState = data.contains(TAG_EIGHT_BOAT_UNTIL) || data.contains(TAG_EIGHT_BOAT_TARGET)
         || player.getAttribute(Attributes.MOVEMENT_SPEED) != null
            && player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(EIGHT_BOAT_SPEED_ID) != null;
      removeModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), EIGHT_BOAT_SPEED_ID);
      data.remove(TAG_EIGHT_BOAT_UNTIL);
      data.remove(TAG_EIGHT_BOAT_TARGET);
      if (hadState || data.contains(TAG_CLONE_UUIDS)) cleanupClones(player);
      vars.servant_card_jump_charges = ServantCardUshiwakamaruRules.clampJumpCharges(
         vars.servant_card_jump_charges, false);
      if (hadState) vars.syncServantCardRuntime(player);
   }

   private static void spawnClones(ServerPlayer player, LivingEntity target, ServerLevel level, long expiresAt) {
      ListTag uuids = new ListTag();
      for (int index = 0; index < 7; index++) {
         UshiwakamaruRiderEntity clone = ModEntities.USHIWAKAMARU_RIDER.get().create(level);
         if (clone == null) continue;
         double angle = Math.PI * 2.0 * index / 7.0;
         clone.moveTo(player.getX() + Math.cos(angle) * 1.8, player.getY() + 0.1,
            player.getZ() + Math.sin(angle) * 1.8, player.getYRot(), 0.0F);
         clone.initClone(player, target, expiresAt);
         if (level.addFreshEntity(clone)) {
            uuids.add(StringTag.valueOf(clone.getUUID().toString()));
            VFXServerEffects.spawn(level, "servant_ushiwakamaru_clone_manifest", clone, 128.0);
         }
      }
      player.getPersistentData().put(TAG_CLONE_UUIDS, uuids);
   }

   private static void cleanupClones(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return;
      CompoundTag data = player.getPersistentData();
      ListTag uuids = data.getList(TAG_CLONE_UUIDS, 8);
      for (int index = 0; index < uuids.size(); index++) {
         try {
            Entity entity = level.getEntity(UUID.fromString(uuids.getString(index)));
            if (entity instanceof UshiwakamaruRiderEntity clone && clone.isClone()) clone.discard();
         } catch (IllegalArgumentException ignored) {
         }
      }
      for (UshiwakamaruRiderEntity clone : level.getEntitiesOfClass(UshiwakamaruRiderEntity.class,
         player.getBoundingBox().inflate(128.0), candidate -> candidate.isClone()
            && candidate.getPersistentData().hasUUID(UshiwakamaruRiderEntity.TAG_CLONE_OWNER)
            && player.getUUID().equals(candidate.getPersistentData().getUUID(UshiwakamaruRiderEntity.TAG_CLONE_OWNER)))) {
         clone.discard();
      }
      data.remove(TAG_CLONE_UUIDS);
   }

   private static boolean eightBoatTargetAlive(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      if (!(player.level() instanceof ServerLevel level) || !data.hasUUID(TAG_EIGHT_BOAT_TARGET)) return false;
      Entity target = level.getEntity(data.getUUID(TAG_EIGHT_BOAT_TARGET));
      return target instanceof LivingEntity living && isEnemy(player, living);
   }

   private static LivingEntity findEnemyLookTarget(ServerPlayer player, double range) {
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle().normalize();
      AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.8);
      LivingEntity best = null;
      double bestScore = 0.78;
      for (LivingEntity living : player.level().getEntitiesOfClass(LivingEntity.class, box,
         entity -> isEnemy(player, entity) && player.hasLineOfSight(entity))) {
         Vec3 to = living.position().add(0.0, living.getBbHeight() * 0.5, 0.0).subtract(eye);
         double distance = to.length();
         if (distance <= 0.01 || distance > range) continue;
         double score = look.dot(to.normalize());
         if (score > bestScore) {
            bestScore = score;
            best = living;
         }
      }
      return best;
   }

   private static List<LivingEntity> nearbyEnemies(ServerPlayer player, double radius) {
      if (!(player.level() instanceof ServerLevel level)) return List.of();
      return new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
         entity -> isEnemy(player, entity)));
   }

   private static boolean isEnemy(ServerPlayer player, LivingEntity entity) {
      return entity != player && entity.isAlive() && !player.isAlliedTo(entity) && !entity.isAlliedTo(player)
         && !EntityUtils.isImmunePlayerTarget(entity);
   }

   private static boolean isAlly(ServerPlayer player, LivingEntity entity) {
      return entity == player || player.isAlliedTo(entity) || entity.isAlliedTo(player);
   }

   private static boolean insideThrust(ServerPlayer player, LivingEntity target, Vec3 direction) {
      Vec3 relative = target.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
      if (relative.lengthSqr() < 1.0E-4 || relative.lengthSqr() > 30.25) return false;
      return relative.normalize().dot(direction) >= 0.45;
   }

   private static Vec3 horizontalLook(ServerPlayer player) {
      Vec3 look = player.getLookAngle().multiply(1.0, 0.0, 1.0);
      return look.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : look.normalize();
   }

   private static void hurtPhysical(ServerPlayer player, LivingEntity target, float damage) {
      target.invulnerableTime = 0;
      target.hurt(player.damageSources().playerAttack(player), damage);
      target.invulnerableTime = 0;
   }

   private static void applyTimedModifier(LivingEntity entity,
                                          net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                          ResourceLocation id, double amount, AttributeModifier.Operation operation,
                                          long expiresAt, int duration) {
      updateModifier(entity.getAttribute(attribute), id, amount, operation);
      String expiryTag = "ServantCardTimedModifier_" + id.getPath();
      entity.getPersistentData().putLong(expiryTag, expiresAt);
      TYPE_MOON_WORLD.queueServerWork(duration, () -> {
         if (entity.getPersistentData().getLong(expiryTag) <= entity.level().getGameTime()) {
            removeModifier(entity.getAttribute(attribute), id);
            entity.getPersistentData().remove(expiryTag);
         }
      });
   }

   private static void updateModifier(AttributeInstance attribute, ResourceLocation id, double amount,
                                      AttributeModifier.Operation operation) {
      if (attribute == null) return;
      AttributeModifier existing = attribute.getModifier(id);
      if (existing != null) attribute.removeModifier(id);
      attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
   }

   private static void removeModifier(AttributeInstance attribute, ResourceLocation id) {
      if (attribute != null && attribute.getModifier(id) != null) attribute.removeModifier(id);
   }

   private static void removeTenguModifiers(ServerPlayer player) {
      removeModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), TENGU_SPEED_ID);
      removeModifier(player.getAttribute(Attributes.ATTACK_SPEED), TENGU_ATTACK_SPEED_ID);
      removeModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), TENGU_DAMAGE_ID);
   }

   private static void spawnShieldParticles(ServerPlayer player, ServerLevel level) {
      Vec3 center = player.position().add(0.0, 0.08, 0.0);
      double radius = 2.05;
      int phase = player.tickCount % 3;
      for (int latitude = 0; latitude < 3; latitude++) {
         double elevation = latitude * Math.PI / 6.0;
         double horizontalRadius = Math.cos(elevation) * radius;
         double height = Math.sin(elevation) * radius;
         for (int azimuth = 0; azimuth < 12; azimuth++) {
            if ((latitude * 4 + azimuth) % 3 != phase) continue;
            double angle = Math.PI * 2.0 * azimuth / 12.0 + player.tickCount * 0.025;
            Vec3 point = center.add(Math.cos(angle) * horizontalRadius, height, Math.sin(angle) * horizontalRadius);
            level.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 1, 0.02, 0.02, 0.02, 0.0);
         }
      }
   }

   private static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, path);
   }
}
