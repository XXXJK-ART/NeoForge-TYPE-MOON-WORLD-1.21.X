package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.ChainsOfHeavenBindingEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.EnkiduEarthWeaponProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatPhase;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

public final class EnkiduCombatHelper {
   private static final String TAG_MODE = "EnkiduMode";
   private static final String TAG_LAST_TRANSFIGURATION = "EnkiduLastTransfiguration";
   private static final String TAG_LAST_PRESENCE = "EnkiduLastPresence";
   private static final String TAG_LAST_PERFECT_FORM = "EnkiduLastPerfectForm";
   private static final String TAG_REGEN_UNTIL = "EnkiduPerfectFormUntil";
   private static final String TAG_NEXT_REGEN = "EnkiduNextRegen";
   private static final String TAG_LAST_CHAIN = "EnkiduLastChain";
   private static final String TAG_LAST_SMALL_WEAPON = "EnkiduLastSmallWeapon";
   private static final String TAG_LAST_BIG_VOLLEY = "EnkiduLastBigVolley";
   private static final String TAG_SMALL_WEAPON_TARGET = "EnkiduSmallWeaponTarget";
   private static final String TAG_SMALL_WEAPON_TOKEN = "EnkiduSmallWeaponToken";
   private static final String TAG_BIG_VOLLEY_TARGET = "EnkiduBigVolleyTarget";
   private static final String TAG_BIG_VOLLEY_TOKEN = "EnkiduBigVolleyToken";
   private static final String TAG_LAST_CHAIN_LASH = "EnkiduLastChainLash";
   private static final String TAG_LAST_EARTH_SPIKE = "EnkiduLastEarthSpike";
   private static final String TAG_LAST_CLAY_BULWARK = "EnkiduLastClayBulwark";
   private static final String TAG_LAST_STARDUST_STEP = "EnkiduLastStardustStep";
   private static final String TAG_LAST_NATURE_PULSE = "EnkiduLastNaturePulse";
   private static final String TAG_LAST_MELEE_BASIC = "EnkiduLastMeleeBasic";
   private static final String TAG_MELEE_COMBO_STEP = "EnkiduMeleeComboStep";
   private static final String TAG_LAST_ENUMA = "EnkiduLastEnuma";
   private static final String TAG_ENUMA_RELEASE = "EnkiduEnumaRelease";
   private static final String TAG_ENUMA_FINISH = "EnkiduEnumaFinish";
   private static final String TAG_ENUMA_TARGET = "EnkiduEnumaTarget";
   private static final String TAG_ENUMA_DAMAGE_DONE = "EnkiduEnumaDamageDone";
   private static final String TAG_FLIGHT_UNTIL = "EnkiduFlightUntil";
   private static final String TAG_LAND_UNTIL = "EnkiduLandUntil";
   private static final String TAG_NEXT_FLIGHT_TOGGLE = "EnkiduNextFlightToggle";
   private static final String TAG_BOUND_UNTIL = "EnkiduBoundUntil";
   private static final String TAG_BOUND_OWNER = "EnkiduBoundOwner";
   private static final String TAG_BOUND_PREV_NO_AI = "EnkiduBoundPrevNoAi";
   private static final String TAG_BOUND_X = "EnkiduBoundX";
   private static final String TAG_BOUND_Y = "EnkiduBoundY";
   private static final String TAG_BOUND_Z = "EnkiduBoundZ";

   private static final int TRANSFIGURATION_COOLDOWN = 10 * 20;
   private static final int PRESENCE_COOLDOWN = 15 * 20;
   private static final int PERFECT_FORM_COOLDOWN = 30 * 20;
   private static final int CHAIN_COOLDOWN = 20 * 20;
   private static final int BIG_VOLLEY_COOLDOWN = 18 * 20;
   private static final int CHAIN_LASH_COOLDOWN = 4 * 20;
   private static final int EARTH_SPIKE_COOLDOWN = 6 * 20;
   private static final int CLAY_BULWARK_COOLDOWN = 16 * 20;
   private static final int STARDUST_STEP_COOLDOWN = 7 * 20;
   private static final int NATURE_PULSE_COOLDOWN = 9 * 20;
   private static final int MELEE_BASIC_COOLDOWN = 18;
   private static final int ENUMA_COOLDOWN = 60 * 20;
   private static final int ENUMA_WINDUP = 10 * 20;
   private static final int ENUMA_RELEASE_VISUAL = 5 * 20;

   private static final ResourceLocation MODE_ATTACK_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "enkidu_mode_attack");
   private static final ResourceLocation MODE_ARMOR_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "enkidu_mode_armor");
   private static final ResourceLocation MODE_SPEED_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "enkidu_mode_speed");
   private static final ResourceLocation MODE_HEALTH_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "enkidu_mode_health");

   private static final ItemStack[] EARTH_WEAPONS = new ItemStack[] {
      new ItemStack(Items.IRON_SWORD), new ItemStack(Items.IRON_AXE), new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.IRON_SHOVEL),
      new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.DIAMOND_AXE), new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(Items.TRIDENT),
      new ItemStack(Items.NETHERITE_SWORD), new ItemStack(Items.NETHERITE_AXE), new ItemStack(Items.NETHERITE_PICKAXE)
   };

   private EnkiduCombatHelper() {
   }

   public static void tick(EnkiduEntity entity) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }

      long now = level.getGameTime();
      tickBoundTargets(entity, level, now);
      tickPerfectFormRegen(entity, level, now);
      tickPassivePresence(entity, level, now);
      tickGroundManaRegen(entity, level, now);
      EnkiduTemporaryPlantHelper.cleanupExpired(level, now);
      tickEnumaWindup(entity, level, now);
      if (isEnumaActive(entity, now)) {
         return;
      }

      LivingEntity target = entity.getTarget();
      updateFlight(entity, target, now);
      interceptHostileProjectiles(entity, level, now);

      if (target == null || !target.isAlive() || EntityUtils.isImmunePlayerTarget(target)) {
         entity.setTarget(null);
         clearWeaponBurstState(entity);
         return;
      }
      if (ServantCombatSystem.cannotAct(entity) || ServantCombatSystem.skillsSuppressed(entity)) {
         return;
      }

      entity.faceToward(target.position().add(0.0, target.getBbHeight() * 0.55, 0.0));
      double distance = entity.distanceTo(target);
      updateCombatMovement(entity, target, distance, now);
      ServantCombatPhase phase = ServantCombatSystem.getPhase(entity);
      if (tryPerfectForm(entity, level, now)) {
         return;
      }
      tryTransfiguration(entity, level, target, now);
      if (tryPresenceDetection(entity, level, now)) {
         return;
      }
      if (tryBeginEnumaElish(entity, level, target, now, phase)) {
         return;
      }
      if (tryChainOfHeaven(entity, level, target, now)) {
         return;
      }
      if (tryAgeOfBabylonVolley(entity, level, target, now, phase)) {
         return;
      }
      if (tryEnkiduSmallSkill(entity, level, target, now, phase, distance)) {
         return;
      }
      if (!isFlying(entity) && tryMeleeBasic(entity, level, target, now, phase, distance)) {
         return;
      }
      if (isFlying(entity) && tryFlyingBasicAttack(entity, level, target, now, distance)) {
         return;
      }
      if (distance >= 4.0) {
         trySmallAgeOfBabylon(entity, level, target, now);
      }
   }

   public static boolean isFlying(EnkiduEntity entity) {
      return entity.getPersistentData().getLong(TAG_FLIGHT_UNTIL) > entity.level().getGameTime()
         && entity.getPersistentData().getLong(TAG_LAND_UNTIL) <= entity.level().getGameTime();
   }

   public static void cleanup(EnkiduEntity entity) {
      entity.setNoGravity(false);
      removeModeModifiers(entity);
      if (entity.level() instanceof ServerLevel level) {
         tickBoundTargets(entity, level, Long.MAX_VALUE / 4L);
      }
   }

   public static boolean tryRespondToNoblePhantasm(EnkiduEntity entity, ServantEntity caster, LivingEntity target, long now, boolean ranged) {
      if (!(entity.level() instanceof ServerLevel level) || entity.getCurrentMp() < 12.0) {
         return false;
      }
      CompoundTag data = entity.getPersistentData();
      if (now - data.getLong(TAG_LAST_CLAY_BULWARK) < CLAY_BULWARK_COOLDOWN / 2L) {
         return false;
      }
      data.putLong(TAG_LAST_CLAY_BULWARK, now);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 12.0));
      entity.triggerNamedActionAnimation("perfect_form");
      entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ranged ? 100 : 70, 1, false, true, true));
      Vec3 threat = target != null && target.isAlive() ? target.position() : caster.position();
      growPlantBulwark(entity, level, threat, true);
      return true;
   }

   private static void tickPassivePresence(EnkiduEntity entity, ServerLevel level, long now) {
      if (now % 20L != 0L) {
         return;
      }
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(50.0),
         e -> e != entity && e.isAlive() && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e))) {
         if (living.hasEffect(MobEffects.INVISIBILITY)) {
            living.removeEffect(MobEffects.INVISIBILITY);
         }
      }
   }

   private static boolean tryPresenceDetection(EnkiduEntity entity, ServerLevel level, long now) {
      CompoundTag data = entity.getPersistentData();
      if (entity.getCurrentMp() < 15.0 || now - data.getLong(TAG_LAST_PRESENCE) < PRESENCE_COOLDOWN || entity.getRandom().nextFloat() > 0.08F) {
         return false;
      }
      data.putLong(TAG_LAST_PRESENCE, now);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 15.0));
      entity.triggerNamedActionAnimation("presence_detection");
      VFXServerEffects.spawnReplayable(level, "servant_enkidu_presence_detection", entity, 0.65F);
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(100.0),
         e -> e != entity && e.isAlive() && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e))) {
         living.removeEffect(MobEffects.INVISIBILITY);
         living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 10 * 20, 0, false, true, true));
      }
      return true;
   }

   private static void tickGroundManaRegen(EnkiduEntity entity, ServerLevel level, long now) {
      if (now % 20L != 0L || !entity.onGround()) {
         return;
      }
      BlockPos below = entity.blockPosition().below();
      BlockState state = level.getBlockState(below);
      if (!state.isAir() && state.isSolidRender(level, below)) {
         entity.setCurrentMp(Math.min(entity.getMaxMp(), entity.getCurrentMp() + 4.0));
      }
   }

   private static boolean tryPerfectForm(EnkiduEntity entity, ServerLevel level, long now) {
      CompoundTag data = entity.getPersistentData();
      if (entity.getHealth() > entity.getMaxHealth() * 0.4F
         || entity.getCurrentMp() < 50.0
         || now - data.getLong(TAG_LAST_PERFECT_FORM) < PERFECT_FORM_COOLDOWN) {
         return false;
      }
      data.putLong(TAG_LAST_PERFECT_FORM, now);
      data.putLong(TAG_REGEN_UNTIL, now + 100L);
      data.putLong(TAG_NEXT_REGEN, now);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 50.0));
      clearNegativeEffects(entity);
      entity.triggerNamedActionAnimation("perfect_form");
      VFXServerEffects.spawnReplayable(level, "servant_enkidu_transfiguration", entity, 1.1F);
      level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.2F, 1.45F);
      return true;
   }

   private static void tickPerfectFormRegen(EnkiduEntity entity, ServerLevel level, long now) {
      CompoundTag data = entity.getPersistentData();
      if (data.getLong(TAG_REGEN_UNTIL) <= now) {
         return;
      }
      clearNegativeEffects(entity);
      if (now >= data.getLong(TAG_NEXT_REGEN)) {
         entity.heal(30.0F);
         data.putLong(TAG_NEXT_REGEN, now + 10L);
         level.sendParticles(ParticleTypes.HAPPY_VILLAGER, entity.getX(), entity.getY() + 0.9, entity.getZ(), 20, 0.6, 0.8, 0.6, 0.05);
      }
   }

   private static void tryTransfiguration(EnkiduEntity entity, ServerLevel level, LivingEntity target, long now) {
      CompoundTag data = entity.getPersistentData();
      if (entity.getCurrentMp() < 15.0 || now - data.getLong(TAG_LAST_TRANSFIGURATION) < TRANSFIGURATION_COOLDOWN) {
         return;
      }
      String desired = chooseMode(entity, target);
      if (desired.equals(data.getString(TAG_MODE)) && now - data.getLong(TAG_LAST_TRANSFIGURATION) < TRANSFIGURATION_COOLDOWN * 2L) {
         return;
      }

      data.putLong(TAG_LAST_TRANSFIGURATION, now);
      data.putString(TAG_MODE, desired);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 15.0));
      applyMode(entity, desired);
      entity.triggerNamedActionAnimation("transfiguration");
      VFXServerEffects.spawnReplayable(level, "servant_enkidu_transfiguration", entity, 1.1F);
      level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 0.75F, 1.65F);
   }

   private static String chooseMode(EnkiduEntity entity, LivingEntity target) {
      double distance = entity.distanceTo(target);
      if (target.getMaxHealth() >= 120.0F || target.getArmorValue() >= 12 || hasTrait(target, ServantTraitTag.GIANT) || hasTrait(target, ServantTraitTag.BEAST)) {
         return "attack";
      }
      if (distance > 10.0 || target.hasEffect(MobEffects.MOVEMENT_SPEED) || target instanceof Player) {
         return "agility";
      }
      return "balanced";
   }

   private static void applyMode(EnkiduEntity entity, String mode) {
      removeModeModifiers(entity);
      if ("attack".equals(mode)) {
         addModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), MODE_ATTACK_ID, 10.0, AttributeModifier.Operation.ADD_VALUE);
         addModifier(entity.getAttribute(Attributes.ARMOR), MODE_ARMOR_ID, -4.0, AttributeModifier.Operation.ADD_VALUE);
         addModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), MODE_SPEED_ID, -0.04, AttributeModifier.Operation.ADD_VALUE);
         addModifier(entity.getAttribute(Attributes.MAX_HEALTH), MODE_HEALTH_ID, -100.0, AttributeModifier.Operation.ADD_VALUE);
      } else if ("agility".equals(mode)) {
         addModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), MODE_ATTACK_ID, -10.0, AttributeModifier.Operation.ADD_VALUE);
         addModifier(entity.getAttribute(Attributes.ARMOR), MODE_ARMOR_ID, -4.0, AttributeModifier.Operation.ADD_VALUE);
         addModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), MODE_SPEED_ID, 0.06, AttributeModifier.Operation.ADD_VALUE);
         addModifier(entity.getAttribute(Attributes.MAX_HEALTH), MODE_HEALTH_ID, -100.0, AttributeModifier.Operation.ADD_VALUE);
      }
      if (entity.getHealth() > entity.getMaxHealth()) {
         entity.setHealth(entity.getMaxHealth());
      }
   }

   private static void removeModeModifiers(EnkiduEntity entity) {
      removeModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), MODE_ATTACK_ID);
      removeModifier(entity.getAttribute(Attributes.ARMOR), MODE_ARMOR_ID);
      removeModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), MODE_SPEED_ID);
      removeModifier(entity.getAttribute(Attributes.MAX_HEALTH), MODE_HEALTH_ID);
   }

   private static void updateFlight(EnkiduEntity entity, LivingEntity target, long now) {
      CompoundTag data = entity.getPersistentData();
      if (data.getLong(TAG_LAND_UNTIL) > now || target == null || !target.isAlive()) {
         entity.setNoGravity(false);
         data.remove(TAG_FLIGHT_UNTIL);
         data.remove(TAG_NEXT_FLIGHT_TOGGLE);
         return;
      }
      double distance = entity.distanceTo(target);
      long flightUntil = data.getLong(TAG_FLIGHT_UNTIL);
      if (distance > 13.0 && flightUntil <= now) {
         data.putLong(TAG_FLIGHT_UNTIL, now + 80L + entity.getRandom().nextInt(80));
         data.putLong(TAG_NEXT_FLIGHT_TOGGLE, now + 55L + entity.getRandom().nextInt(45));
      } else if (distance < 7.0 && flightUntil > now) {
         data.putLong(TAG_FLIGHT_UNTIL, now + 15L + entity.getRandom().nextInt(20));
         data.putLong(TAG_NEXT_FLIGHT_TOGGLE, now + 25L + entity.getRandom().nextInt(25));
      } else if (now >= data.getLong(TAG_NEXT_FLIGHT_TOGGLE)) {
         boolean fly = flightUntil <= now ? distance > 9.0 : distance > 12.0;
         data.putLong(TAG_NEXT_FLIGHT_TOGGLE, now + 45L + entity.getRandom().nextInt(40));
         data.putLong(TAG_FLIGHT_UNTIL, fly ? now + 70L + entity.getRandom().nextInt(70) : now + 12L);
      }
      if (isFlying(entity)) {
         entity.getNavigation().stop();
         entity.setNoGravity(true);
         double hoverY = Math.max(entity.getY(), Math.min(target.getY() + 2.4, entity.getY() + 0.16));
         Vec3 away = entity.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
         if (away.lengthSqr() < 1.0E-4) {
            away = new Vec3(entity.getRandom().nextDouble() - 0.5, 0.0, entity.getRandom().nextDouble() - 0.5);
         }
         away = away.normalize();
         double radialDistance = Math.max(0.1, entity.distanceTo(target));
         double radial = radialDistance < 9.0 ? 0.16 : radialDistance > 15.0 ? -0.14 : 0.0;
         Vec3 orbit = new Vec3(-away.z, 0.0, away.x).scale(0.11);
         entity.setDeltaMovement(entity.getDeltaMovement().scale(0.65).add(away.scale(radial)).add(orbit).add(0.0, (hoverY - entity.getY()) * 0.08, 0.0));
      } else {
         entity.setNoGravity(false);
      }
   }

   private static void updateCombatMovement(EnkiduEntity entity, LivingEntity target, double distance, long now) {
      if (isFlying(entity) || ServantCombatSystem.cannotAct(entity)) {
         return;
      }
      if (distance > 16.0) {
         entity.getNavigation().moveTo(target, 1.18);
      } else if (distance > 8.0) {
         entity.getNavigation().moveTo(target, 1.02);
         if (now % 12L == 0L) {
            float side = entity.getRandom().nextBoolean() ? 0.45F : -0.45F;
            entity.getMoveControl().strafe(0.18F, side);
         }
      } else if (distance > 3.2) {
         if (now % 10L == 0L) {
            float side = entity.getRandom().nextBoolean() ? 0.75F : -0.75F;
            entity.getMoveControl().strafe(0.12F, side);
         }
      } else {
         Vec3 away = entity.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
         if (away.lengthSqr() > 1.0E-4) {
            away = away.normalize().scale(3.6);
            entity.getNavigation().moveTo(entity.getX() + away.x, entity.getY(), entity.getZ() + away.z, 1.1);
         }
      }
   }

   private static boolean tryChainOfHeaven(EnkiduEntity entity, ServerLevel level, LivingEntity target, long now) {
      CompoundTag data = entity.getPersistentData();
      boolean divine = hasTrait(target, ServantTraitTag.DIVINE);
      if (entity.distanceTo(target) > 22.0 || entity.getCurrentMp() < 30.0 || now - data.getLong(TAG_LAST_CHAIN) < CHAIN_COOLDOWN) {
         return false;
      }
      if (!divine && entity.getRandom().nextFloat() > 0.28F) {
         return false;
      }

      int divinity = divinityLevel(target);
      int duration = divine ? (3 + Math.max(1, divinity)) * 20 : 20;
      data.putLong(TAG_LAST_CHAIN, now);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 30.0));
      bindTarget(entity, level, target, duration, divine);
      entity.triggerNamedActionAnimation("chain_of_heaven");
      VFXServerEffects.spawnReplayable(level, "servant_enkidu_chain_of_heaven", target, Math.max(1.2F, duration / 20.0F));
      level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.CHAIN_PLACE, SoundSource.HOSTILE, 1.4F, divine ? 1.4F : 1.0F);
      return true;
   }

   private static void bindTarget(EnkiduEntity entity, ServerLevel level, LivingEntity target, int duration, boolean divine) {
      long now = level.getGameTime();
      CompoundTag data = target.getPersistentData();
      data.putLong(TAG_BOUND_UNTIL, now + duration);
      data.putUUID(TAG_BOUND_OWNER, entity.getUUID());
      data.putDouble(TAG_BOUND_X, target.getX());
      data.putDouble(TAG_BOUND_Y, target.getY());
      data.putDouble(TAG_BOUND_Z, target.getZ());
      if (target instanceof Mob mob) {
         data.putBoolean(TAG_BOUND_PREV_NO_AI, mob.isNoAi());
         mob.getNavigation().stop();
         mob.setNoAi(true);
      }
      target.setDeltaMovement(Vec3.ZERO);
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration + 5, 10, false, true, true));
      target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration + 5, 10, false, true, true));
      target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration + 5, 10, false, true, true));
      level.addFreshEntity(new ChainsOfHeavenBindingEntity(level, entity, target, duration + 6, divine));
      TYPE_MOON_WORLD.queueServerWork(duration + 2, () -> restoreBoundTarget(target, entity.getUUID()));
   }

   private static void tickBoundTargets(EnkiduEntity entity, ServerLevel level, long now) {
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(96.0), LivingEntity::isAlive)) {
         CompoundTag data = target.getPersistentData();
         if (!data.hasUUID(TAG_BOUND_OWNER) || !entity.getUUID().equals(data.getUUID(TAG_BOUND_OWNER))) {
            continue;
         }
         if (data.getLong(TAG_BOUND_UNTIL) <= now) {
            restoreBoundTarget(target, entity.getUUID());
            continue;
         }
         target.setDeltaMovement(Vec3.ZERO);
         target.hurtMarked = true;
         target.setPos(data.getDouble(TAG_BOUND_X), data.getDouble(TAG_BOUND_Y), data.getDouble(TAG_BOUND_Z));
         if (target instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.setTarget(null);
            mob.setNoAi(true);
         }
      }
   }

   private static void restoreBoundTarget(LivingEntity target, UUID owner) {
      if (target == null) {
         return;
      }
      CompoundTag data = target.getPersistentData();
      if (!data.hasUUID(TAG_BOUND_OWNER) || !owner.equals(data.getUUID(TAG_BOUND_OWNER))) {
         return;
      }
      if (target instanceof Mob mob) {
         mob.setNoAi(data.getBoolean(TAG_BOUND_PREV_NO_AI));
      }
      data.remove(TAG_BOUND_OWNER);
      data.remove(TAG_BOUND_UNTIL);
      data.remove(TAG_BOUND_PREV_NO_AI);
      data.remove(TAG_BOUND_X);
      data.remove(TAG_BOUND_Y);
      data.remove(TAG_BOUND_Z);
   }

   private static boolean trySmallAgeOfBabylon(EnkiduEntity entity, ServerLevel level, LivingEntity target, long now) {
      if (target == null || !target.isAlive() || EntityUtils.isImmunePlayerTarget(target)) {
         clearWeaponBurstState(entity);
         return false;
      }
      CompoundTag data = entity.getPersistentData();
      long cooldown = isFlying(entity) ? 18L + entity.getRandom().nextInt(16) : 34L + entity.getRandom().nextInt(28);
      if (now - data.getLong(TAG_LAST_SMALL_WEAPON) < cooldown) {
         return false;
      }
      int count = isFlying(entity) ? 2 + entity.getRandom().nextInt(5) : 1 + entity.getRandom().nextInt(8);
      data.putLong(TAG_LAST_SMALL_WEAPON, now);
      data.putUUID(TAG_SMALL_WEAPON_TARGET, target.getUUID());
      data.putLong(TAG_SMALL_WEAPON_TOKEN, now + 24L);
      spawnEarthWeapons(entity, level, target, count, 0, 2.0F, false);
      entity.triggerNamedActionAnimation("age_of_babylon");
      VFXServerEffects.spawn(level, "servant_enkidu_age_of_babylon", entity, 96.0);
      return true;
   }

   private static boolean tryAgeOfBabylonVolley(EnkiduEntity entity, ServerLevel level, LivingEntity target, long now, ServantCombatPhase phase) {
      CompoundTag data = entity.getPersistentData();
      boolean highThreat = target.getMaxHealth() >= 160.0F || hasTrait(target, ServantTraitTag.BEAST) || hasTrait(target, ServantTraitTag.HUMAN_THREAT) || target instanceof EnderDragon;
      int chance = phase == ServantCombatPhase.DECISIVE ? 55 : phase == ServantCombatPhase.NORMAL ? 40 : 24;
      if ((!highThreat && phase != ServantCombatPhase.DECISIVE)
         || entity.getCurrentMp() < 30.0
         || now - data.getLong(TAG_LAST_BIG_VOLLEY) < phasedCooldown(BIG_VOLLEY_COOLDOWN, phase)
         || entity.getRandom().nextInt(100) >= chance) {
         return false;
      }
      data.putLong(TAG_LAST_BIG_VOLLEY, now);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 30.0));
      data.putUUID(TAG_BIG_VOLLEY_TARGET, target.getUUID());
      data.putLong(TAG_BIG_VOLLEY_TOKEN, now + 24L);
      entity.triggerNamedActionAnimation("age_of_babylon");
      VFXServerEffects.spawnReplayable(level, "servant_enkidu_age_of_babylon", entity, 2.2F);
      for (int batch = 0; batch < 8; batch++) {
         final int batchIndex = batch;
         final int delay = batch * 2 + 1;
         TYPE_MOON_WORLD.queueServerWork(delay, () -> {
            if (entity.isAlive() && entity.level() instanceof ServerLevel serverLevel) {
               LivingEntity liveTarget = resolveAgeOfBabylonTarget(serverLevel, entity, data);
               if (liveTarget != null) {
                  spawnEarthWeapons(entity, serverLevel, liveTarget, 14, batchIndex, 2.7F, true);
               }
            }
         });
      }
      return true;
   }

   private static boolean tryEnkiduSmallSkill(EnkiduEntity entity, ServerLevel level, LivingEntity target, long now, ServantCombatPhase phase, double distance) {
      if (tryClayBulwark(entity, level, now, phase)) {
         return true;
      }
      if (tryStardustStep(entity, level, target, now, phase, distance)) {
         return true;
      }
      if (tryNaturePulse(entity, level, now, phase)) {
         return true;
      }
      if (tryEarthSpikeSprout(entity, level, target, now, phase, distance)) {
         return true;
      }
      return tryChainLash(entity, level, target, now, phase, distance, false);
   }

   private static boolean tryFlyingBasicAttack(EnkiduEntity entity, ServerLevel level, LivingEntity target, long now, double distance) {
      if (distance <= 13.5 && entity.getRandom().nextBoolean() && tryChainLash(entity, level, target, now, ServantCombatSystem.getPhase(entity), distance, true)) {
         return true;
      }
      return distance <= 20.0 && trySmallAgeOfBabylon(entity, level, target, now);
   }

   private static boolean tryMeleeBasic(EnkiduEntity entity, ServerLevel level, LivingEntity target, long now, ServantCombatPhase phase, double distance) {
      if (distance > 4.25 || ServantCombatSystem.cannotAct(entity)) {
         return false;
      }
      CompoundTag data = entity.getPersistentData();
      int cooldown = Math.max(10, MELEE_BASIC_COOLDOWN - phase.id() * 3);
      if (now - data.getLong(TAG_LAST_MELEE_BASIC) < cooldown) {
         return false;
      }
      int variant = data.getInt(TAG_MELEE_COMBO_STEP) % 5;
      data.putInt(TAG_MELEE_COMBO_STEP, variant + 1);
      data.putLong(TAG_LAST_MELEE_BASIC, now);
      entity.faceToward(target.position().add(0.0, target.getBbHeight() * 0.55, 0.0));
      return switch (variant) {
         case 0 -> meleePalmStrike(entity, level, target, phase);
         case 1 -> meleeSweepingClayArm(entity, level, target, phase);
         case 2 -> meleeRisingRootUppercut(entity, level, target, phase);
         case 3 -> meleeRootSnareKick(entity, level, target, phase);
         default -> meleeShortChainPierce(entity, level, target, phase);
      };
   }

   private static boolean meleePalmStrike(EnkiduEntity entity, ServerLevel level, LivingEntity target, ServantCombatPhase phase) {
      entity.triggerSlashAnimation();
      Vec3 hit = target.position().add(0.0, target.getBbHeight() * 0.52, 0.0);
      applyMeleeDamage(entity, target, (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * (0.72 + phase.id() * 0.08)), 0.12, false);
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, hit.x, hit.y, hit.z, 12, 0.22, 0.24, 0.22, 0.045);
      level.sendParticles(ParticleTypes.CRIT, hit.x, hit.y, hit.z, 8, 0.18, 0.2, 0.18, 0.08);
      level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 0.85F, 1.35F);
      return true;
   }

   private static boolean meleeSweepingClayArm(EnkiduEntity entity, ServerLevel level, LivingEntity target, ServantCombatPhase phase) {
      entity.triggerSweepAnimation();
      Vec3 look = entity.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (look.lengthSqr() < 1.0E-4) {
         look = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      }
      look = look.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : look.normalize();
      AABB sweepBox = entity.getBoundingBox().inflate(3.2, 1.0, 3.2).move(look.scale(1.25));
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, sweepBox,
         e -> e != entity && e.isAlive() && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e))) {
         applyMeleeDamage(entity, living, (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * (0.58 + phase.id() * 0.08)), 0.28, false);
      }
      Vec3 fx = entity.position().add(look.scale(1.8)).add(0.0, entity.getBbHeight() * 0.48, 0.0);
      level.sendParticles(ParticleTypes.SWEEP_ATTACK, fx.x, fx.y, fx.z, 3, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, fx.x, fx.y - 0.25, fx.z, 18, 0.7, 0.18, 0.7, 0.04);
      spawnPlantPatch(level, fx, 2, 55, false);
      return true;
   }

   private static boolean meleeRisingRootUppercut(EnkiduEntity entity, ServerLevel level, LivingEntity target, ServantCombatPhase phase) {
      entity.triggerUppercutAnimation();
      applyMeleeDamage(entity, target, (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * (0.78 + phase.id() * 0.1) + 3.0), 0.08, false);
      target.setDeltaMovement(target.getDeltaMovement().add(0.0, 0.38 + phase.id() * 0.08, 0.0));
      target.hurtMarked = true;
      Vec3 pos = target.position();
      spawnPlantPatch(level, pos, 2, 65, true);
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y + 0.2, pos.z, 22, 0.35, 0.2, 0.35, 0.07);
      level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y + 0.75, pos.z, 10, 0.25, 0.35, 0.25, 0.1);
      level.playSound(null, target.blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 0.9F, 1.05F);
      return true;
   }

   private static boolean meleeRootSnareKick(EnkiduEntity entity, ServerLevel level, LivingEntity target, ServantCombatPhase phase) {
      entity.triggerHorizontalSwingAnimation();
      applyMeleeDamage(entity, target, (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * (0.62 + phase.id() * 0.08)), 0.16, true);
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 34 + phase.id() * 12, 1, false, true, true));
      spawnPlantPatch(level, target.position(), 3, 70, true);
      Vec3 pos = target.position().add(0.0, 0.25, 0.0);
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y, pos.z, 26, 0.45, 0.18, 0.45, 0.05);
      level.playSound(null, target.blockPosition(), SoundEvents.GRASS_BREAK, SoundSource.HOSTILE, 1.0F, 0.72F);
      return true;
   }

   private static boolean meleeShortChainPierce(EnkiduEntity entity, ServerLevel level, LivingEntity target, ServantCombatPhase phase) {
      entity.triggerNamedActionAnimation("chain_of_heaven");
      applyMeleeDamage(entity, target, (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * (0.7 + phase.id() * 0.1) + (hasTrait(target, ServantTraitTag.DIVINE) ? 5.0 : 1.5)), 0.2, true);
      target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 45 + phase.id() * 15, 0, false, true, true));
      VFXServerEffects.spawn(level, "servant_enkidu_chain_of_heaven", target, 96.0);
      Vec3 pos = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 10, 0.22, 0.22, 0.22, 0.06);
      level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.CHAIN_PLACE, SoundSource.HOSTILE, 0.9F, 1.7F);
      return true;
   }

   private static void applyMeleeDamage(EnkiduEntity entity, LivingEntity target, float damage, double pushStrength, boolean pullToEnkidu) {
      target.invulnerableTime = 0;
      target.hurt(entity.damageSources().mobAttack(entity), damage);
      target.invulnerableTime = 0;
      Vec3 direction = pullToEnkidu ? entity.position().subtract(target.position()) : target.position().subtract(entity.position());
      direction = direction.multiply(1.0, 0.0, 1.0);
      if (direction.lengthSqr() > 1.0E-4) {
         direction = direction.normalize().scale(pushStrength);
         target.push(direction.x, 0.06, direction.z);
         target.hurtMarked = true;
      }
   }

   private static boolean tryChainLash(
      EnkiduEntity entity,
      ServerLevel level,
      LivingEntity target,
      long now,
      ServantCombatPhase phase,
      double distance,
      boolean airborneBasic
   ) {
      CompoundTag data = entity.getPersistentData();
      int cooldown = airborneBasic ? 30 : phasedCooldown(CHAIN_LASH_COOLDOWN, phase);
      int chance = airborneBasic ? 70 : phaseChance(18, phase);
      if (distance > 13.5 || now - data.getLong(TAG_LAST_CHAIN_LASH) < cooldown || entity.getRandom().nextInt(100) >= chance) {
         return false;
      }
      data.putLong(TAG_LAST_CHAIN_LASH, now);
      entity.triggerNamedActionAnimation("chain_of_heaven");
      VFXServerEffects.spawn(level, "servant_enkidu_chain_of_heaven", target, 96.0);
      spawnPlantPatch(level, target.position(), 2, 70, true);
      float damage = (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * (airborneBasic ? 0.46 : 0.58) + (hasTrait(target, ServantTraitTag.DIVINE) ? 8.0 : 3.0));
      target.invulnerableTime = 0;
      target.hurt(entity.damageSources().mobAttack(entity), damage);
      target.invulnerableTime = 0;
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, airborneBasic ? 25 : 45, hasTrait(target, ServantTraitTag.DIVINE) ? 2 : 1, false, true, true));
      Vec3 pull = entity.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
      if (pull.lengthSqr() > 1.0E-4) {
         pull = pull.normalize().scale(airborneBasic ? 0.28 : 0.42);
         target.push(pull.x, 0.08, pull.z);
         target.hurtMarked = true;
      }
      level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.CHAIN_PLACE, SoundSource.HOSTILE, 0.85F, 1.55F);
      return true;
   }

   private static boolean tryEarthSpikeSprout(EnkiduEntity entity, ServerLevel level, LivingEntity target, long now, ServantCombatPhase phase, double distance) {
      CompoundTag data = entity.getPersistentData();
      if (distance > 18.0
         || entity.getCurrentMp() < 8.0
         || now - data.getLong(TAG_LAST_EARTH_SPIKE) < phasedCooldown(EARTH_SPIKE_COOLDOWN, phase)
         || entity.getRandom().nextInt(100) >= phaseChance(16, phase)) {
         return false;
      }
      data.putLong(TAG_LAST_EARTH_SPIKE, now);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 8.0));
      entity.triggerNamedActionAnimation("age_of_babylon");
      Vec3 center = target.position();
      spawnPlantPatch(level, center, 3, 95, true);
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, center.x, target.getY() + 0.12, center.z, 26, 0.75, 0.08, 0.75, 0.05);
      level.sendParticles(ParticleTypes.CRIT, center.x, target.getY() + 0.35, center.z, 18, 0.5, 0.18, 0.5, 0.08);
      level.playSound(null, target.blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 1.15F, 0.85F);
      target.invulnerableTime = 0;
      target.hurt(entity.damageSources().mobAttack(entity), (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.72 + 10.0));
      target.invulnerableTime = 0;
      target.setDeltaMovement(target.getDeltaMovement().add(0.0, 0.42, 0.0));
      target.hurtMarked = true;
      return true;
   }

   private static boolean tryClayBulwark(EnkiduEntity entity, ServerLevel level, long now, ServantCombatPhase phase) {
      CompoundTag data = entity.getPersistentData();
      if (entity.getHealth() > entity.getMaxHealth() * (phase == ServantCombatPhase.DECISIVE ? 0.72F : 0.55F)
         || entity.getCurrentMp() < 12.0
         || now - data.getLong(TAG_LAST_CLAY_BULWARK) < phasedCooldown(CLAY_BULWARK_COOLDOWN, phase)
         || entity.getRandom().nextInt(100) >= phaseChance(24, phase)) {
         return false;
      }
      data.putLong(TAG_LAST_CLAY_BULWARK, now);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 12.0));
      entity.triggerNamedActionAnimation("perfect_form");
      entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 90, 1, false, true, true));
      LivingEntity target = entity.getTarget();
      growPlantBulwark(entity, level, target != null && target.isAlive() ? target.position() : entity.position().add(entity.getLookAngle().scale(8.0)), false);
      return true;
   }

   private static boolean tryStardustStep(EnkiduEntity entity, ServerLevel level, LivingEntity target, long now, ServantCombatPhase phase, double distance) {
      CompoundTag data = entity.getPersistentData();
      boolean badRange = distance < 4.0 || distance > 17.0;
      if (!badRange
         || entity.getCurrentMp() < 10.0
         || now - data.getLong(TAG_LAST_STARDUST_STEP) < phasedCooldown(STARDUST_STEP_COOLDOWN, phase)
         || entity.getRandom().nextInt(100) >= phaseChance(20, phase)) {
         return false;
      }
      data.putLong(TAG_LAST_STARDUST_STEP, now);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 10.0));
      Vec3 away = entity.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
      if (away.lengthSqr() < 1.0E-4) {
         away = entity.getLookAngle().multiply(-1.0, 0.0, -1.0);
      }
      away = away.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
      Vec3 side = new Vec3(-away.z, 0.0, away.x).scale(entity.getRandom().nextBoolean() ? 1.0 : -1.0);
      Vec3 move = distance < 4.0 ? away.scale(2.6).add(side.scale(1.6)) : away.scale(-2.2).add(side.scale(2.4));
      Vec3 before = entity.position().add(0.0, entity.getBbHeight() * 0.45, 0.0);
      entity.setDeltaMovement(entity.getDeltaMovement().add(move.x * 0.32, 0.16, move.z * 0.32));
      entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 70, 1, false, true, true));
      entity.triggerNamedActionAnimation("transfiguration");
      level.sendParticles(ParticleTypes.END_ROD, before.x, before.y, before.z, 22, 0.28, 0.28, 0.28, 0.08);
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, before.x, before.y, before.z, 18, 0.36, 0.36, 0.36, 0.06);
      spawnPlantPatch(level, entity.position().add(move.normalize().scale(1.2)), 2, 60, false);
      level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 0.8F, 1.75F);
      return true;
   }

   private static boolean tryNaturePulse(EnkiduEntity entity, ServerLevel level, long now, ServantCombatPhase phase) {
      CompoundTag data = entity.getPersistentData();
      List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(5.8),
         e -> e != entity && e.isAlive() && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e));
      if (nearby.isEmpty()
         || entity.getCurrentMp() < 10.0
         || now - data.getLong(TAG_LAST_NATURE_PULSE) < phasedCooldown(NATURE_PULSE_COOLDOWN, phase)
         || entity.getRandom().nextInt(100) >= phaseChance(18 + nearby.size() * 4, phase)) {
         return false;
      }
      data.putLong(TAG_LAST_NATURE_PULSE, now);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 10.0));
      entity.triggerNamedActionAnimation("presence_detection");
      VFXServerEffects.spawnReplayable(level, "servant_enkidu_presence_detection", entity, 0.65F);
      spawnPlantPatch(level, entity.position(), 5, 100, true);
      for (LivingEntity living : nearby) {
         living.invulnerableTime = 0;
         living.hurt(entity.damageSources().magic(), 10.0F + phase.id() * 3.0F);
         living.invulnerableTime = 0;
         living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, false, true, true));
         living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 0, false, true, true));
      }
      level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.GRASS_BREAK, SoundSource.HOSTILE, 1.1F, 0.65F);
      return true;
   }

   private static void growPlantBulwark(EnkiduEntity entity, ServerLevel level, Vec3 threat, boolean againstNoblePhantasm) {
      Vec3 forward = threat.subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      if (forward.lengthSqr() < 1.0E-4) {
         forward = entity.getLookAngle().multiply(1.0, 0.0, 1.0);
      }
      if (forward.lengthSqr() < 1.0E-4) {
         forward = new Vec3(0.0, 0.0, 1.0);
      }
      forward = forward.normalize();
      Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
      final Vec3 growthForward = forward;
      final Vec3 growthSide = side;
      BlockPos base = findSurface(level, BlockPos.containing(entity.position().add(forward.scale(3.0))));
      int radius = againstNoblePhantasm ? 14 : 9;
      int height = againstNoblePhantasm ? 15 : 11;
      int duration = againstNoblePhantasm ? 180 : 130;
      List<BlockPos> placed = new ArrayList<>();
      Set<BlockPos> seen = new HashSet<>();
      for (int y = 0; y <= height; y++) {
         final int layer = y;
         TYPE_MOON_WORLD.queueServerWork(layer * 2, () -> {
            if (!entity.isAlive()) {
               return;
            }
            double vertical = (double)layer / Math.max(1.0, height);
            double layerRadius = radius * Math.sqrt(Math.max(0.0, 1.0 - vertical * vertical * 0.82));
            int r = Math.max(1, (int)Math.ceil(layerRadius));
            for (int sx = -r; sx <= r; sx++) {
               for (int depth = -1; depth <= r; depth++) {
                  double normalized = (sx * sx) / Math.max(1.0, layerRadius * layerRadius) + (depth * depth) / Math.max(1.0, radius * radius);
                  double noise = blockNoise(level, base.offset(sx, layer, depth));
                  if (normalized > 1.05 + (noise - 0.5) * 0.32 || noise < 0.08) {
                     continue;
                  }
                  Vec3 offset = growthSide.scale(sx).add(growthForward.scale(depth));
                  BlockPos pos = BlockPos.containing(base.getX() + 0.5 + offset.x, base.getY() + layer, base.getZ() + 0.5 + offset.z);
                  if (!seen.add(pos) || !canGrowTemporaryPlant(level, pos)) {
                     continue;
                  }
                  if (layer > 1 && normalized < 0.22 && noise < 0.36) {
                     continue;
                  }
                  AxisChoice axis = bulwarkAxis(offset, layer, noise);
                  boolean core = layer <= 1 || normalized < 0.34 || noise > 0.82;
                  BlockState plant = temporaryBulwarkState(level, pos, core, layer, height, axis.axis());
                  level.setBlock(pos, plant, 3);
                  EnkiduTemporaryPlantHelper.register(level, pos, level.getGameTime() + duration);
                  placed.add(pos.immutable());
                  if (core && layer > 1 && noise > 0.86) {
                     BlockPos branch = pos.relative(axis.branchDirection());
                     if (seen.add(branch) && canGrowTemporaryPlant(level, branch)) {
                        level.setBlock(branch, orientedLogStateForGround(level, branch, noise, axis.axis()), 3);
                        EnkiduTemporaryPlantHelper.register(level, branch, level.getGameTime() + duration);
                        placed.add(branch.immutable());
                     }
                  }
                  if (layer > height * 0.55 && noise > 0.92) {
                     BlockPos crown = pos.above();
                     if (seen.add(crown) && canGrowTemporaryPlant(level, crown)) {
                        level.setBlock(crown, leafStateForGround(level, crown, noise), 3);
                        EnkiduTemporaryPlantHelper.register(level, crown, level.getGameTime() + duration);
                        placed.add(crown.immutable());
                     }
                  }
                  if (noise > 0.7 && layer > 0) {
                     level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.18, 0.18, 0.18, 0.03);
                  }
               }
            }
            Vec3 layerCenter = Vec3.atCenterOf(base.above(layer));
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, layerCenter.x, layerCenter.y, layerCenter.z, againstNoblePhantasm ? 22 : 14, radius * 0.35, 0.18, radius * 0.35, 0.05);
            level.playSound(null, base, layer == 0 ? SoundEvents.ROOTED_DIRT_BREAK : SoundEvents.WOOD_PLACE, SoundSource.HOSTILE, againstNoblePhantasm ? 1.3F : 0.95F, 0.72F + layer * 0.06F);
         });
      }
      TYPE_MOON_WORLD.queueServerWork(duration, () -> clearTemporaryPlants(level, placed));
   }

   private static void spawnPlantPatch(ServerLevel level, Vec3 center, int radius, int duration, boolean lush) {
      BlockPos base = findSurface(level, BlockPos.containing(center));
      List<BlockPos> placed = new ArrayList<>();
      for (int dx = -radius; dx <= radius; dx++) {
         for (int dz = -radius; dz <= radius; dz++) {
            BlockPos surface = findSurface(level, base.offset(dx, 0, dz));
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > radius + 0.25 || blockNoise(level, surface) < 0.28) {
               continue;
            }
            BlockPos pos = surface.above();
            if (!canGrowTemporaryPlant(level, pos)) {
               continue;
            }
            double noise = blockNoise(level, pos);
            BlockState state = smallPlantState(level, pos);
            level.setBlock(pos, state, 3);
            EnkiduTemporaryPlantHelper.register(level, pos, level.getGameTime() + duration);
            placed.add(pos.immutable());
            if (lush && noise > 0.84) {
               int height = noise > 0.94 ? 3 : 2;
               for (int y = 1; y < height; y++) {
                  BlockPos trunk = pos.above(y);
                  if (!canGrowTemporaryPlant(level, trunk)) {
                     break;
                  }
                  level.setBlock(trunk, orientedLogStateForGround(level, trunk, noise, Direction.Axis.Y), 3);
                  EnkiduTemporaryPlantHelper.register(level, trunk, level.getGameTime() + duration);
                  placed.add(trunk.immutable());
               }
            }
         }
      }
      Vec3 fx = Vec3.atCenterOf(base);
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, fx.x, fx.y + 0.45, fx.z, lush ? 28 : 16, radius * 0.45, 0.18, radius * 0.45, 0.04);
      TYPE_MOON_WORLD.queueServerWork(duration, () -> clearTemporaryPlants(level, placed));
   }

   private static BlockPos findSurface(ServerLevel level, BlockPos start) {
      BlockPos pos = start;
      for (int i = 0; i < 8 && !level.getBlockState(pos.below()).isSolidRender(level, pos.below()); i++) {
         pos = pos.below();
      }
      for (int i = 0; i < 8 && !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty(); i++) {
         pos = pos.above();
      }
      return pos;
   }

   private static boolean canGrowTemporaryPlant(ServerLevel level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      return !state.hasBlockEntity() && (state.isAir() || state.canBeReplaced());
   }

   private static BlockState temporaryBulwarkState(ServerLevel level, BlockPos pos, boolean core, int layer, int height, Direction.Axis axis) {
      double noise = blockNoise(level, pos);
      if (core || noise < 0.88) {
         return orientedLogStateForGround(level, pos, noise, axis);
      }
      if (isWetGround(level, pos.below())) {
         return Blocks.MANGROVE_ROOTS.defaultBlockState();
      }
      if (layer >= height - 1 && noise > 0.96) {
         return leafStateForGround(level, pos, noise);
      }
      if (noise > 0.94) {
         return leafStateForGround(level, pos, noise);
      }
      if (noise > 0.91) {
         return leafStateForGround(level, pos, noise);
      }
      return orientedLogStateForGround(level, pos, noise, axis);
   }

   private static BlockState smallPlantState(ServerLevel level, BlockPos pos) {
      double noise = blockNoise(level, pos);
      if (isWetGround(level, pos.below()) && noise > 0.58) {
         return Blocks.MANGROVE_ROOTS.defaultBlockState();
      }
      if (noise > 0.72) {
         return Blocks.MOSS_BLOCK.defaultBlockState();
      }
      if (noise > 0.58) {
         return Blocks.MOSS_CARPET.defaultBlockState();
      }
      if (noise > 0.42) {
         return Blocks.FERN.defaultBlockState();
      }
      return Blocks.SHORT_GRASS.defaultBlockState();
   }

   private static BlockState logStateForGround(ServerLevel level, BlockPos pos, double noise) {
      return orientedLogStateForGround(level, pos, noise, Direction.Axis.Y);
   }

   private static BlockState orientedLogStateForGround(ServerLevel level, BlockPos pos, double noise, Direction.Axis axis) {
      BlockState state;
      if (isWetGround(level, pos.below())) {
         state = Blocks.MANGROVE_LOG.defaultBlockState();
      } else if (isColdGround(level, pos.below())) {
         state = Blocks.SPRUCE_LOG.defaultBlockState();
      } else if (isDryGround(level, pos.below())) {
         state = Blocks.ACACIA_LOG.defaultBlockState();
      } else if (noise > 0.84) {
         state = Blocks.CHERRY_LOG.defaultBlockState();
      } else if (noise > 0.66) {
         state = Blocks.JUNGLE_LOG.defaultBlockState();
      } else if (noise > 0.42) {
         state = Blocks.OAK_LOG.defaultBlockState();
      } else {
         state = Blocks.BIRCH_LOG.defaultBlockState();
      }
      return state.hasProperty(RotatedPillarBlock.AXIS) ? state.setValue(RotatedPillarBlock.AXIS, axis) : state;
   }

   private static BlockState leafStateForGround(ServerLevel level, BlockPos pos, double noise) {
      if (isColdGround(level, pos.below())) {
         return Blocks.SPRUCE_LEAVES.defaultBlockState();
      }
      if (noise > 0.75) {
         return Blocks.CHERRY_LEAVES.defaultBlockState();
      }
      if (noise > 0.45) {
         return Blocks.JUNGLE_LEAVES.defaultBlockState();
      }
      return Blocks.OAK_LEAVES.defaultBlockState();
   }

   private static AxisChoice bulwarkAxis(Vec3 offset, int layer, double noise) {
      if (layer <= 1 || noise < 0.26) {
         Direction dir = Math.abs(offset.x) > Math.abs(offset.z)
            ? (offset.x >= 0.0 ? Direction.EAST : Direction.WEST)
            : (offset.z >= 0.0 ? Direction.SOUTH : Direction.NORTH);
         return new AxisChoice(dir.getAxis(), dir);
      }
      if (noise > 0.78) {
         return new AxisChoice(Direction.Axis.Y, Direction.UP);
      }
      Direction dir = Math.abs(offset.x) > Math.abs(offset.z)
         ? (offset.x >= 0.0 ? Direction.EAST : Direction.WEST)
         : (offset.z >= 0.0 ? Direction.SOUTH : Direction.NORTH);
      return new AxisChoice(dir.getAxis(), dir);
   }

   private record AxisChoice(Direction.Axis axis, Direction branchDirection) {
   }

   private static void clearTemporaryPlants(ServerLevel level, List<BlockPos> positions) {
      for (BlockPos pos : positions) {
         EnkiduTemporaryPlantHelper.unregister(level, pos);
         if (EnkiduTemporaryPlantHelper.isTemporaryPlantState(level.getBlockState(pos))) {
            level.removeBlock(pos, false);
         }
      }
   }

   private static boolean isWetGround(ServerLevel level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      return state.is(Blocks.MUD)
         || state.is(Blocks.CLAY)
         || state.is(Blocks.MANGROVE_ROOTS)
         || state.is(Blocks.WATER)
         || state.is(Blocks.SEAGRASS);
   }

   private static boolean isColdGround(ServerLevel level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      return state.is(Blocks.SNOW_BLOCK)
         || state.is(Blocks.POWDER_SNOW)
         || state.is(Blocks.ICE)
         || state.is(Blocks.PACKED_ICE)
         || state.is(Blocks.BLUE_ICE)
         || state.is(Blocks.SPRUCE_LOG)
         || state.is(Blocks.PODZOL);
   }

   private static boolean isDryGround(ServerLevel level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      return state.is(Blocks.SAND)
         || state.is(Blocks.RED_SAND)
         || state.is(Blocks.SANDSTONE)
         || state.is(Blocks.RED_SANDSTONE)
         || state.is(Blocks.TERRACOTTA)
         || state.is(Blocks.DEAD_BUSH);
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

   private static void spawnEarthWeapons(EnkiduEntity entity, ServerLevel level, LivingEntity target, int count, int batch, float speed, boolean volley) {
      Vec3 targetCenter = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      Vec3 origin = entity.position().add(0.0, 0.8, 0.0);
      Vec3 toTarget = targetCenter.subtract(origin);
      Vec3 forward = new Vec3(toTarget.x, 0.0, toTarget.z);
      if (forward.lengthSqr() < 1.0E-4) {
         forward = new Vec3(entity.getLookAngle().x, 0.0, entity.getLookAngle().z);
      }
      if (forward.lengthSqr() < 1.0E-4) {
         forward = new Vec3(0.0, 0.0, 1.0);
      }
      forward = forward.normalize();
      Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
      Vec3 volleyCenter = entity.position().lerp(target.position(), 0.62);
      for (int i = 0; i < count; i++) {
         int globalIndex = batch * count + i;
         double spreadIndex = i - (count - 1) * 0.5;
         Vec3 spawn;
         Vec3 aimPoint;
         Vec3 steering;
         if (volley) {
            double angle = globalIndex * 0.61 + batch * 0.42 + (entity.getRandom().nextDouble() - 0.5) * 0.14;
            double radius = 6.0 + (globalIndex % 7) * 2.2 + batch * 0.85 + entity.getRandom().nextDouble() * 2.2;
            double forwardOffset = Math.sin(angle) * radius + ((i % 5) - 2) * 1.85;
            double sideOffset = Math.cos(angle) * radius;
            double rise = 3.0 + (i % 7) * 1.05 + batch * 0.38 + entity.getRandom().nextDouble() * 0.8;
            Vec3 ringOffset = forward.scale(forwardOffset).add(side.scale(sideOffset));
            spawn = volleyCenter.add(ringOffset).add(0.0, rise, 0.0);
            aimPoint = targetCenter
               .add(side.scale((entity.getRandom().nextDouble() - 0.5) * 4.2))
               .add(forward.scale((entity.getRandom().nextDouble() - 0.5) * 3.2))
               .add(0.0, (entity.getRandom().nextDouble() - 0.5) * 2.1, 0.0);
            Vec3 tangent = new Vec3(-ringOffset.z, 0.0, ringOffset.x);
            if (tangent.lengthSqr() < 1.0E-4) {
               tangent = side;
            } else {
               tangent = tangent.normalize();
            }
            steering = tangent.scale(globalIndex % 2 == 0 ? 0.18 : -0.18).add(0.0, -0.035, 0.0);
         } else {
            double sideOffset = spreadIndex * 1.15 + (entity.getRandom().nextDouble() - 0.5) * 0.75;
            double forwardOffset = 1.8 + (i % 3) * 0.7 + entity.getRandom().nextDouble() * 0.8;
            double rise = 0.28 + (i % 4) * 0.46;
            spawn = entity.position().add(forward.scale(forwardOffset)).add(side.scale(sideOffset)).add(0.0, rise, 0.0);
            aimPoint = targetCenter
               .add(side.scale((entity.getRandom().nextDouble() - 0.5) * 1.45))
               .add(0.0, (entity.getRandom().nextDouble() - 0.5) * 0.55, 0.0);
            steering = side.scale((entity.getRandom().nextDouble() - 0.5) * 0.08);
         }
         ItemStack stack = randomWeapon(entity, volley);
         float damage = weaponDamage(stack, volley);
         EnkiduEarthWeaponProjectileEntity projectile = new EnkiduEarthWeaponProjectileEntity(level, entity, stack, damage);
         projectile.setPos(spawn.x, spawn.y, spawn.z);
         Vec3 aim = aimPoint.subtract(spawn);
         double shotSpeed = speed + entity.getRandom().nextDouble() * (volley ? 0.72 : 0.42);
         projectile.setDeltaMovement(aim.normalize().add(steering).normalize().scale(shotSpeed));
         projectile.alignToMotion();
         level.addFreshEntity(projectile);
         spawnEarthWeaponBirthFx(level, spawn, volley);
      }
      level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.AMETHYST_CLUSTER_PLACE, SoundSource.HOSTILE, volley ? 1.4F : 0.75F, 1.35F);
   }

   private static LivingEntity resolveAgeOfBabylonTarget(ServerLevel level, EnkiduEntity entity, CompoundTag data) {
      if (data.getLong(TAG_BIG_VOLLEY_TOKEN) <= level.getGameTime()) {
         clearWeaponBurstState(entity);
         return null;
      }
      Entity targetEntity = data.hasUUID(TAG_BIG_VOLLEY_TARGET) ? level.getEntity(data.getUUID(TAG_BIG_VOLLEY_TARGET)) : null;
      if (targetEntity instanceof LivingEntity living && living.isAlive() && entity.distanceTo(living) <= 48.0) {
         return living;
      }
      clearWeaponBurstState(entity);
      return null;
   }

   private static void clearWeaponBurstState(EnkiduEntity entity) {
      CompoundTag data = entity.getPersistentData();
      data.remove(TAG_SMALL_WEAPON_TARGET);
      data.remove(TAG_SMALL_WEAPON_TOKEN);
      data.remove(TAG_BIG_VOLLEY_TARGET);
      data.remove(TAG_BIG_VOLLEY_TOKEN);
   }

   private static void spawnEarthWeaponBirthFx(ServerLevel level, Vec3 pos, boolean volley) {
      int green = volley ? 9 : 5;
      int shine = volley ? 7 : 4;
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y, pos.z, green, volley ? 0.42 : 0.24, volley ? 0.36 : 0.18, volley ? 0.42 : 0.24, 0.03);
      level.sendParticles(ParticleTypes.ENCHANTED_HIT, pos.x, pos.y, pos.z, shine, volley ? 0.28 : 0.16, volley ? 0.28 : 0.14, volley ? 0.28 : 0.16, 0.045);
      level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y + 0.08, pos.z, volley ? 3 : 1, 0.08, 0.12, 0.08, 0.025);
   }

   private static void interceptHostileProjectiles(EnkiduEntity entity, ServerLevel level, long now) {
      if (now % 7L != 0L) {
         return;
      }
      List<Projectile> projectiles = level.getEntitiesOfClass(Projectile.class, entity.getBoundingBox().inflate(18.0), p -> {
         Entity owner = p.getOwner();
         return p.isAlive() && p.getId() != entity.getId() && (owner == null || !owner.isAlliedTo(entity));
      });
      int spawned = 0;
      for (Projectile hostile : projectiles) {
         if (spawned >= 2) {
            break;
         }
         Vec3 spawn = entity.position().add(0.0, 1.0 + spawned * 0.25, 0.0);
         EnkiduEarthWeaponProjectileEntity interceptor = new EnkiduEarthWeaponProjectileEntity(level, entity, new ItemStack(Items.IRON_SWORD), 8.0F);
         interceptor.setPos(spawn.x, spawn.y, spawn.z);
         interceptor.setDeltaMovement(hostile.position().subtract(spawn).normalize().scale(2.8));
         level.addFreshEntity(interceptor);
         spawned++;
      }
   }

   private static boolean tryBeginEnumaElish(EnkiduEntity entity, ServerLevel level, LivingEntity target, long now, ServantCombatPhase phase) {
      CompoundTag data = entity.getPersistentData();
      if (data.getLong(TAG_ENUMA_RELEASE) > now
         || entity.distanceTo(target) > 32.0
         || entity.getCurrentMp() < 150.0
         || now - data.getLong(TAG_LAST_ENUMA) < ENUMA_COOLDOWN
         || phase != ServantCombatPhase.DECISIVE) {
         return false;
      }
      data.putLong(TAG_LAST_ENUMA, now);
      data.putLong(TAG_ENUMA_RELEASE, now + ENUMA_WINDUP);
      data.putLong(TAG_ENUMA_FINISH, now + ENUMA_WINDUP + ENUMA_RELEASE_VISUAL);
      data.putBoolean(TAG_ENUMA_DAMAGE_DONE, false);
      data.putUUID(TAG_ENUMA_TARGET, target.getUUID());
      data.putLong(TAG_LAND_UNTIL, now + ENUMA_WINDUP + ENUMA_RELEASE_VISUAL + 12L);
      entity.setNoGravity(false);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 150.0));
      entity.triggerNamedActionAnimation("enkidu_enuma_elish");
      ServantVoiceHelper.tryPlayEnkiduNp(entity);
      VFXServerEffects.spawnReplayable(level, "servant_enkidu_enuma_elish", entity, 35.25F);
      level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.6F, 0.85F);
      return true;
   }

   private static void tickEnumaWindup(EnkiduEntity entity, ServerLevel level, long now) {
      CompoundTag data = entity.getPersistentData();
      long release = data.getLong(TAG_ENUMA_RELEASE);
      long finish = data.getLong(TAG_ENUMA_FINISH);
      if (release <= 0L && finish <= 0L) {
         return;
      }
      Entity targetEntity = data.hasUUID(TAG_ENUMA_TARGET) ? level.getEntity(data.getUUID(TAG_ENUMA_TARGET)) : null;
      if (!(targetEntity instanceof LivingEntity target) || !target.isAlive()) {
         if (data.getBoolean(TAG_ENUMA_DAMAGE_DONE) && now < finish) {
            entity.getNavigation().stop();
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.1, 0.0, 0.1));
            return;
         }
         data.remove(TAG_ENUMA_RELEASE);
         data.remove(TAG_ENUMA_FINISH);
         data.remove(TAG_ENUMA_TARGET);
         data.remove(TAG_ENUMA_DAMAGE_DONE);
         return;
      }
      entity.getNavigation().stop();
      entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.1, 0.0, 0.1));
      entity.faceToward(target.position().add(0.0, target.getBbHeight() * 0.55, 0.0));
      if (now < release) {
         return;
      }
      if (data.getBoolean(TAG_ENUMA_DAMAGE_DONE)) {
         if (now >= finish) {
            data.remove(TAG_ENUMA_RELEASE);
            data.remove(TAG_ENUMA_FINISH);
            data.remove(TAG_ENUMA_TARGET);
            data.remove(TAG_ENUMA_DAMAGE_DONE);
         }
         return;
      }
      data.putBoolean(TAG_ENUMA_DAMAGE_DONE, true);
      double damage = 5000.0;
      if (hasTrait(target, ServantTraitTag.DIVINE)) {
         damage *= 2.0;
      }
      if (hasTrait(target, ServantTraitTag.BEAST) || hasTrait(target, ServantTraitTag.HUMAN_THREAT) || target instanceof EnderDragon) {
         damage *= 2.0;
      }
      applyNoDefenseDamage(entity, target, (float)Math.min(Float.MAX_VALUE, damage));
      VFXServerEffects.spawn(level, "servant_enkidu_chain_of_heaven", target, 128.0);
      level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 80, 1.2, 1.0, 1.2, 0.18);
      level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.0F, 1.35F);
   }

   private static boolean isEnumaActive(EnkiduEntity entity, long now) {
      CompoundTag data = entity.getPersistentData();
      return data.getLong(TAG_ENUMA_RELEASE) > now || data.getLong(TAG_ENUMA_FINISH) > now;
   }

   private static void applyNoDefenseDamage(EnkiduEntity entity, LivingEntity target, float amount) {
      target.removeEffect(MobEffects.DAMAGE_RESISTANCE);
      target.removeEffect(MobEffects.ABSORPTION);
      target.setAbsorptionAmount(0.0F);
      target.invulnerableTime = 0;
      float before = target.getHealth();
      target.hurt(entity.damageSources().magic(), amount);
      target.invulnerableTime = 0;
      float expected = before - amount;
      if (target.isAlive() && target.getHealth() > expected) {
         target.setHealth(Math.max(0.0F, expected));
         if (target.getHealth() <= 0.0F) {
            target.die(entity.damageSources().magic());
         }
      }
   }

   private static void clearNegativeEffects(LivingEntity entity) {
      entity.removeEffect(MobEffects.POISON);
      entity.removeEffect(MobEffects.WITHER);
      entity.removeEffect(MobEffects.WEAKNESS);
      entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
      entity.removeEffect(MobEffects.DIG_SLOWDOWN);
      entity.removeEffect(MobEffects.BLINDNESS);
      entity.removeEffect(MobEffects.CONFUSION);
   }

   private static boolean hasTrait(LivingEntity entity, ServantTraitTag trait) {
      if (entity instanceof ServantEntity servant && servant.getDefinition() != null) {
         return servant.getDefinition().traits().contains(trait);
      }
      return trait == ServantTraitTag.BEAST && entity instanceof Enemy && entity.getMaxHealth() >= 200.0F;
   }

   private static int divinityLevel(LivingEntity entity) {
      if (entity instanceof ServantEntity servant && servant.getDefinition() != null) {
         List<String> skills = servant.getDefinition().skillIds();
         if (hasAnySkill(skills, "divinity_a", "god_hand_a")) {
            return 5;
         }
         if (hasAnySkill(skills, "divinity_b_plus")) {
            return 4;
         }
         if (hasAnySkill(skills, "divinity_b")) {
            return 3;
         }
         if (hasAnySkill(skills, "divinity_c")) {
            return 2;
         }
         if (hasAnySkill(skills, "divinity_d", "divinity_e", "divinity_e_minus")) {
            return 1;
         }
      }
      return hasTrait(entity, ServantTraitTag.DIVINE) ? 1 : 0;
   }

   private static boolean hasAnySkill(List<String> skills, String... ids) {
      if (skills == null || skills.isEmpty()) {
         return false;
      }
      for (String skill : skills) {
         String normalized = skill == null ? "" : skill.toLowerCase(Locale.ROOT);
         for (String id : ids) {
            if (normalized.equals(id)) {
               return true;
            }
         }
      }
      return false;
   }

   private static ItemStack randomWeapon(EnkiduEntity entity, boolean volley) {
      int bound = volley ? EARTH_WEAPONS.length : Math.max(4, EARTH_WEAPONS.length - 3);
      return EARTH_WEAPONS[entity.getRandom().nextInt(bound)].copy();
   }

   private static float weaponDamage(ItemStack stack, boolean volley) {
      float base = volley ? 20.0F : 13.0F;
      if (stack.is(Items.NETHERITE_SWORD) || stack.is(Items.NETHERITE_AXE) || stack.is(Items.NETHERITE_PICKAXE)) {
         return base + 12.0F;
      }
      if (stack.is(Items.DIAMOND_SWORD) || stack.is(Items.DIAMOND_AXE) || stack.is(Items.DIAMOND_PICKAXE) || stack.is(Items.TRIDENT)) {
         return base + 7.0F;
      }
      return base;
   }

   private static int phasedCooldown(int baseCooldown, ServantCombatPhase phase) {
      if (phase == ServantCombatPhase.DECISIVE) {
         return Math.max(10, (int)(baseCooldown * 0.65));
      }
      if (phase == ServantCombatPhase.NORMAL) {
         return Math.max(10, (int)(baseCooldown * 0.82));
      }
      return baseCooldown;
   }

   private static int phaseChance(int baseChance, ServantCombatPhase phase) {
      if (phase == ServantCombatPhase.DECISIVE) {
         return Math.min(95, baseChance + 24);
      }
      if (phase == ServantCombatPhase.NORMAL) {
         return Math.min(90, baseChance + 12);
      }
      return baseChance;
   }

   private static void addModifier(AttributeInstance attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
      if (attribute == null) {
         return;
      }
      attribute.removeModifier(id);
      attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
   }

   private static void removeModifier(AttributeInstance attribute, ResourceLocation id) {
      if (attribute != null) {
         attribute.removeModifier(id);
      }
   }
}
