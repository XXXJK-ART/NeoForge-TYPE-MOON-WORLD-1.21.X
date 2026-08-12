package net.xxxjk.TYPE_MOON_WORLD.servant.lancelot;

import java.util.Comparator;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.EmiyaThrownWeaponEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshGateWeaponProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.LancelotWeaponItem;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterProtection;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.LancelotBerserkerEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantVoiceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactService;
import org.joml.Vector3f;

public final class LancelotCombatHelper {
   public static final String KNIGHT_OF_OWNER_TAG = "KnightOfOwner";
   public static final String KNIGHT_OF_OWNER_OWNER_TAG = "KnightOfOwnerOwner";
   public static final String AROUNDIGHT_MODE_TAG = "LancelotAroundightMode";
   public static final String AROUNDIGHT_DRAIN_TICK_TAG = "LancelotAroundightDrainTick";
   public static final String AROUNDIGHT_COOLDOWN_UNTIL_TAG = "LancelotAroundightCooldownUntil";
   public static final String UNABLE_UNTIL_TAG = "LancelotUnableUntil";
   public static final String BATTLE_KILLS_TAG = "LancelotBattleKills";
   public static final String BATTLE_KILL_BUFF_UNTIL_TAG = "LancelotBattleKillBuffUntil";
   public static final String LAST_PROJECTILE_INTERCEPT_TAG = "LancelotLastProjectileIntercept";
   public static final String LAST_THROW_TAG = "LancelotLastThrow";
   public static final String LAST_GROUND_SLAM_TAG = "LancelotLastGroundSlam";
   public static final String AROUNDIGHT_HEALTH_SYNCED_TAG = "LancelotAroundightHealthSynced";
   public static final double KNIGHT_OF_OWNER_MP_COST = 2.0;
   public static final double ETERNAL_ARMS_MASTERSHIP_RECOVERY_MULTIPLIER = 2.0;
   public static final double ETERNAL_ARMS_MASTERSHIP_MAX_RECOVERY_MULTIPLIER = 2.0;
   public static final double ETERNAL_ARMS_MASTERSHIP_WEAPON_DAMAGE_MULTIPLIER = 1.5;
   public static final float ETERNAL_ARMS_MASTERSHIP_BYPASS_DODGE_CHANCE = 0.30F;
   public static final float ETERNAL_ARMS_MASTERSHIP_BYPASS_GUARD_CHANCE = 0.30F;
   public static final double AROUNDIGHT_CORE_ATTRIBUTE_MULTIPLIER = 2.0;
   public static final double AROUNDIGHT_DRAW_MP_COST = 50.0;
   public static final double AROUNDIGHT_DRAIN_MP = 5.0;
   public static final int AROUNDIGHT_DRAIN_INTERVAL = 20;
   public static final int UNABLE_DURATION_TICKS = 45;
   public static final int PROJECTILE_INTERCEPT_COOLDOWN = 2;
   public static final int THROW_COOLDOWN = 76;
   private static final ResourceLocation AROUNDIGHT_DAMAGE_ID =
      ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "lancelot_aroundight_damage");
   private static final ResourceLocation AROUNDIGHT_HEALTH_ID =
      ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "lancelot_aroundight_health");
   private static final ResourceLocation AROUNDIGHT_ARMOR_ID =
      ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "lancelot_aroundight_armor");
   private static final ResourceLocation AROUNDIGHT_SPEED_ID =
      ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "lancelot_aroundight_speed");
   private static final ResourceLocation AROUNDIGHT_ATTACK_SPEED_ID =
      ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "lancelot_aroundight_attack_speed");
   private static final ResourceLocation KILL_BUFF_DAMAGE_ID =
      ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "lancelot_battle_kill_damage");
   private static final DustParticleOptions DARK_DUST = new DustParticleOptions(new Vector3f(0.03F, 0.02F, 0.04F), 1.25F);
   private static final DustParticleOptions PURPLE_DUST = new DustParticleOptions(new Vector3f(0.28F, 0.08F, 0.48F), 1.0F);

   private LancelotCombatHelper() {
   }

   public static void tick(LancelotBerserkerEntity entity, ServerLevel level) {
      long now = level.getGameTime();
      if (isUnable(entity, now)) {
         entity.getNavigation().stop();
         if ((entity.tickCount & 7) == 0) {
            level.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + 1.0, entity.getZ(), 8, 0.35, 0.45, 0.35, 0.04);
         }
         return;
      }
      tickAroundight(entity, level, now);
      ensureCombatLoadout(entity);
      tickBlessingBuff(entity, now);
      maybeInterceptProjectile(entity, level, now);
      spawnModeParticles(entity, level);
   }

   public static boolean isUnable(LivingEntity entity, long now) {
      return entity != null && now < entity.getPersistentData().getLong(UNABLE_UNTIL_TAG);
   }

   public static boolean isAroundightMode(LivingEntity entity) {
      return entity != null && entity.getPersistentData().getBoolean(AROUNDIGHT_MODE_TAG);
   }

   public static boolean canUseKnightOfOwner(LivingEntity entity) {
      return entity != null && !isAroundightMode(entity);
   }

   public static boolean hasEternalArmsMastership(LivingEntity entity) {
      if (entity instanceof LancelotBerserkerEntity) {
         return true;
      }
      if (entity instanceof ServerPlayer player) {
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         return vars.servant_card_transformed && "lancelot_berserker".equals(vars.servant_card_id);
      }
      return false;
   }

   public static boolean rollsEternalArmsDodgeBypass(DamageSource source) {
      return source != null && source.getEntity() instanceof LivingEntity living
         && hasEternalArmsMastership(living)
         && living.getRandom().nextFloat() < capEternalArmsBypassChance(ETERNAL_ARMS_MASTERSHIP_BYPASS_DODGE_CHANCE);
   }

   public static boolean rollsEternalArmsGuardBypass(DamageSource source) {
      return source != null && source.getEntity() instanceof LivingEntity living
         && hasEternalArmsMastership(living)
         && living.getRandom().nextFloat() < capEternalArmsBypassChance(ETERNAL_ARMS_MASTERSHIP_BYPASS_GUARD_CHANCE);
   }

   public static double eternalArmsMastershipRecoveryMultiplier() {
      return capEternalArmsRecoveryMultiplier(ETERNAL_ARMS_MASTERSHIP_RECOVERY_MULTIPLIER);
   }

   public static double capEternalArmsRecoveryMultiplier(double multiplier) {
      return Math.max(1.0, Math.min(multiplier, ETERNAL_ARMS_MASTERSHIP_MAX_RECOVERY_MULTIPLIER));
   }

   public static float capEternalArmsBypassChance(float chance) {
      return Math.max(0.0F, Math.min(chance, 0.30F));
   }

   public static boolean tryDrawAroundight(LancelotBerserkerEntity entity, ServerLevel level, LivingEntity target) {
      long now = level.getGameTime();
      CompoundTag data = entity.getPersistentData();
      if (isAroundightMode(entity) || isUnable(entity, now) || now < data.getLong(AROUNDIGHT_COOLDOWN_UNTIL_TAG)
         || entity.getCurrentMp() < AROUNDIGHT_DRAW_MP_COST) {
         return false;
      }
      entity.setCurrentMp(entity.getCurrentMp() - AROUNDIGHT_DRAW_MP_COST);
      data.putBoolean(AROUNDIGHT_MODE_TAG, true);
      data.putLong(AROUNDIGHT_DRAIN_TICK_TAG, now + AROUNDIGHT_DRAIN_INTERVAL);
      entity.setItemInHand(InteractionHand.MAIN_HAND, knightOfOwnerStack(new ItemStack(ModItems.AROUNDIGHT.get()), entity));
      entity.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
      applyAroundightModifiers(entity);
      level.playSound(null, entity.blockPosition(), ModSounds.LANCELOT_BERSERKER_VOICE_NP.get(), SoundSource.HOSTILE, 1.35F, 0.95F);
      level.sendParticles(ParticleTypes.FLASH, entity.getX(), entity.getY() + 1.0, entity.getZ(), 1, 0.2, 0.2, 0.2, 0.0);
      level.sendParticles(ParticleTypes.ENCHANT, entity.getX(), entity.getY() + 1.0, entity.getZ(), 40, 0.75, 0.8, 0.75, 0.2);
      return true;
   }

   public static void stopAroundight(LancelotBerserkerEntity entity, boolean unable) {
      CompoundTag data = entity.getPersistentData();
      if (!data.getBoolean(AROUNDIGHT_MODE_TAG)) {
         return;
      }
      data.remove(AROUNDIGHT_MODE_TAG);
      data.remove(AROUNDIGHT_DRAIN_TICK_TAG);
      data.putLong(AROUNDIGHT_COOLDOWN_UNTIL_TAG, entity.level().getGameTime() + 220L);
      removeAroundightModifiers(entity);
      if (unable) {
         data.putLong(UNABLE_UNTIL_TAG, entity.level().getGameTime() + UNABLE_DURATION_TICKS);
         if (entity.level() instanceof ServerLevel level) {
            level.playSound(null, entity.blockPosition(), ModSounds.LANCELOT_BERSERKER_VOICE_FAIL.get(), SoundSource.HOSTILE, 1.0F, 0.82F);
         }
      }
      entity.setItemInHand(InteractionHand.MAIN_HAND, knightOfOwnerStack(new ItemStack(ModItems.LANCELOT_IRON_ROD.get()), entity));
   }

   public static void ensureCombatLoadout(LancelotBerserkerEntity entity) {
      if (isAroundightMode(entity)) {
         if (!(entity.getMainHandItem().getItem() instanceof LancelotWeaponItem weapon)
            || weapon.weaponType() != LancelotWeaponItem.WeaponType.AROUNDIGHT) {
            entity.setItemInHand(InteractionHand.MAIN_HAND, knightOfOwnerStack(new ItemStack(ModItems.AROUNDIGHT.get()), entity));
         }
         return;
      }
      ItemStack main = entity.getMainHandItem();
      if (main.isEmpty()) {
         entity.setItemInHand(InteractionHand.MAIN_HAND, knightOfOwnerStack(new ItemStack(ModItems.LANCELOT_IRON_ROD.get()), entity));
         return;
      }
      if (!isKnightOfOwner(main) && canOwnerize(main) && entity.getCurrentMp() >= KNIGHT_OF_OWNER_MP_COST) {
         entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - KNIGHT_OF_OWNER_MP_COST));
         entity.setItemInHand(InteractionHand.MAIN_HAND, knightOfOwnerStack(main, entity));
      }
   }

   public static ItemStack knightOfOwnerStack(ItemStack stack, LivingEntity owner) {
      if (stack.isEmpty()) return stack;
      CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
      tag.putBoolean(KNIGHT_OF_OWNER_TAG, true);
      if (owner != null) tag.putUUID(KNIGHT_OF_OWNER_OWNER_TAG, owner.getUUID());
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
      return stack;
   }

   public static boolean isKnightOfOwner(ItemStack stack) {
      if (stack == null || stack.isEmpty()) return false;
      CustomData data = stack.get(DataComponents.CUSTOM_DATA);
      return data != null && data.copyTag().getBoolean(KNIGHT_OF_OWNER_TAG);
   }

   public static boolean canOwnerize(ItemStack stack) {
      return stack != null && !stack.isEmpty() && stack.getItem() != Items.AIR;
   }

   public static void applyWeaponHit(LivingEntity owner, LivingEntity target, ItemStack stack) {
      if (owner == null || target == null || target == owner) return;
      if (isAroundightMode(owner) && stack.getItem() instanceof LancelotWeaponItem weapon
         && weapon.weaponType() == LancelotWeaponItem.WeaponType.AROUNDIGHT) {
         applyEternalArmsWeaponDamage(owner, target, stack);
         if (ServantIdentityHelper.hasTrait(target, ServantTraitTag.DRAGON)) {
            target.invulnerableTime = 0;
            target.hurt(owner.damageSources().mobAttack(owner), (float)(owner.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.5));
            target.invulnerableTime = 0;
         }
         spawnAroundightHit(owner, target);
      } else if (isKnightOfOwner(stack)) {
         applyEternalArmsWeaponDamage(owner, target, stack);
         target.invulnerableTime = 0;
         target.hurt(owner.damageSources().mobAttack(owner), (float)(owner.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.5));
         target.invulnerableTime = 0;
         spawnKnightHit(owner, target);
      }
   }

   private static void applyEternalArmsWeaponDamage(LivingEntity owner, LivingEntity target, ItemStack stack) {
      if (!hasEternalArmsMastership(owner) || stack == null || stack.isEmpty()) {
         return;
      }
      double weaponDamage = weaponAttackDamage(stack);
      if (weaponDamage <= 0.0) {
         return;
      }
      target.invulnerableTime = 0;
      target.hurt(owner.damageSources().mobAttack(owner), (float)(weaponDamage * (ETERNAL_ARMS_MASTERSHIP_WEAPON_DAMAGE_MULTIPLIER - 1.0)));
      target.invulnerableTime = 0;
   }

   private static double weaponAttackDamage(ItemStack stack) {
      ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
      return Math.max(0.0, modifiers.compute(0.0, net.minecraft.world.entity.EquipmentSlot.MAINHAND));
   }

   public static void recordKill(LancelotBerserkerEntity entity) {
      CompoundTag data = entity.getPersistentData();
      int kills = Math.min(5, data.getInt(BATTLE_KILLS_TAG) + 1);
      data.putInt(BATTLE_KILLS_TAG, kills);
      data.putLong(BATTLE_KILL_BUFF_UNTIL_TAG, entity.level().getGameTime() + 200L);
      updateKillBuff(entity, kills);
   }

   public static boolean throwHeldWeapon(LancelotBerserkerEntity entity, ServerLevel level, LivingEntity target) {
      if (!canUseKnightOfOwner(entity) || target == null || !target.isAlive()) return false;
      CompoundTag data = entity.getPersistentData();
      long now = level.getGameTime();
      if (now - data.getLong(LAST_THROW_TAG) < THROW_COOLDOWN) return false;
      ItemStack held = entity.getMainHandItem();
      if (held.isEmpty()) return false;
      ItemStack thrown = held.copy();
      thrown.setCount(1);
      knightOfOwnerStack(thrown, entity);
      Vec3 start = entity.getEyePosition().add(entity.getLookAngle().scale(0.6));
      Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.52, 0.0);
      Vec3 dir = aim.subtract(start);
      if (dir.lengthSqr() < 1.0E-4) return false;
      EmiyaThrownWeaponEntity projectile = new EmiyaThrownWeaponEntity(level, entity, thrown);
      projectile.setPos(start);
      projectile.setFixedDamage((float)Math.max(18.0, entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5));
      projectile.setNoGravity(true);
      projectile.setPiercingImpact(true);
      projectile.shoot(dir.x, dir.y + 0.03, dir.z, 2.35F, 0.0F);
      projectile.alignPoseToMotion();
      level.addFreshEntity(projectile);
      data.putLong(LAST_THROW_TAG, now);
      entity.setItemInHand(InteractionHand.MAIN_HAND, knightOfOwnerStack(new ItemStack(ModItems.LANCELOT_IRON_ROD.get()), entity));
      ServantVoiceHelper.tryPlayAttack(entity);
      level.playSound(null, entity.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.HOSTILE, 0.9F, 0.72F);
      return true;
   }

   public static boolean tryGroundSlam(LancelotBerserkerEntity entity, ServerLevel level, boolean phaseTwo) {
      long now = level.getGameTime();
      CompoundTag data = entity.getPersistentData();
      if (now - data.getLong(LAST_GROUND_SLAM_TAG) < (phaseTwo ? 42L : 62L) || entity.isSlamming()) {
         return false;
      }
      data.putLong(LAST_GROUND_SLAM_TAG, now);
      entity.getNavigation().stop();
      entity.triggerGroundSlam();

      double radius = phaseTwo ? 5.8 : 4.6;
      float damage = (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * (phaseTwo ? 1.35 : 1.05));
      AABB area = entity.getBoundingBox().inflate(radius, 2.25, radius);
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area, candidate -> isSlamTarget(entity, candidate))) {
         living.invulnerableTime = 0;
         living.hurt(entity.damageSources().mobAttack(entity), damage);
         living.invulnerableTime = 0;
         Vec3 away = living.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
         if (away.lengthSqr() > 1.0E-4) {
            away = away.normalize();
            living.push(away.x * (phaseTwo ? 1.4 : 1.0), phaseTwo ? 0.55 : 0.38, away.z * (phaseTwo ? 1.4 : 1.0));
         } else {
            living.push(0.0, phaseTwo ? 0.65 : 0.45, 0.0);
         }
      }
      TerrainImpactProfile profile = TerrainImpactProfile.of(phaseTwo ? TerrainImpactProfile.Tier.HEAVY : TerrainImpactProfile.Tier.MEDIUM);
      TerrainImpactService.impact(level, entity, entity.position().add(0.0, 0.18, 0.0), profile, TerrainImpactService.Shape.UPPER_SURFACE_CRATER);
      level.sendParticles(DARK_DUST, entity.getX(), entity.getY() + 0.35, entity.getZ(),
         phaseTwo ? 90 : 62, radius * 0.35, 0.22, radius * 0.35, 0.11);
      level.playSound(null, entity.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, phaseTwo ? 1.7F : 1.35F, 0.42F);
      ServantVoiceHelper.tryPlayAttack(entity);
      return true;
   }

   private static void tickAroundight(LancelotBerserkerEntity entity, ServerLevel level, long now) {
      if (!isAroundightMode(entity)) {
         removeAroundightModifiers(entity);
         return;
      }
      applyAroundightModifiers(entity);
      if (now < entity.getPersistentData().getLong(AROUNDIGHT_DRAIN_TICK_TAG)) {
         return;
      }
      entity.getPersistentData().putLong(AROUNDIGHT_DRAIN_TICK_TAG, now + AROUNDIGHT_DRAIN_INTERVAL);
      if (entity.getCurrentMp() < AROUNDIGHT_DRAIN_MP) {
         stopAroundight(entity, true);
         return;
      }
      entity.setCurrentMp(entity.getCurrentMp() - AROUNDIGHT_DRAIN_MP);
      level.sendParticles(PURPLE_DUST, entity.getX(), entity.getY() + 1.05, entity.getZ(), 12, 0.55, 0.65, 0.55, 0.04);
   }

   private static void maybeInterceptProjectile(LancelotBerserkerEntity entity, ServerLevel level, long now) {
      if (!canUseKnightOfOwner(entity) || now - entity.getPersistentData().getLong(LAST_PROJECTILE_INTERCEPT_TAG) < PROJECTILE_INTERCEPT_COOLDOWN) {
         return;
      }
      Entity projectile = level.getEntitiesOfClass(Entity.class, entity.getBoundingBox().inflate(7.0),
         candidate -> isInterceptableProjectile(entity, candidate)).stream()
         .min(Comparator.comparingDouble(entity::distanceToSqr))
         .orElse(null);
      if (projectile == null) return;
      entity.getPersistentData().putLong(LAST_PROJECTILE_INTERCEPT_TAG, now);
      LivingEntity target = entity.getTarget();
      if (target == null || !target.isAlive()) {
         target = scanNearestEnemy(entity, level);
      }
      GilgameshGateWeaponProjectileEntity gateProjectile = projectile instanceof GilgameshGateWeaponProjectileEntity gate ? gate : null;
      ItemStack stack = stackForProjectile(projectile, entity);
      projectile.discard();
      if (target != null) {
         Vec3 start = entity.getEyePosition().add(entity.getLookAngle().scale(0.5));
         Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
         Vec3 dir = aim.subtract(start);
         if (gateProjectile != null) {
            spawnKnightOfOwnerGateCounter(level, entity, target, start, dir, gateProjectile);
         } else {
            EmiyaThrownWeaponEntity counter = new EmiyaThrownWeaponEntity(level, entity, stack);
            counter.setPos(start);
            counter.setFixedDamage(28.0F);
            counter.setNoGravity(true);
            counter.setPiercingImpact(true);
            counter.shoot(dir.x, dir.y + 0.03, dir.z, 2.45F, 0.0F);
            counter.alignPoseToMotion();
            level.addFreshEntity(counter);
         }
      }
      level.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + 1.1, entity.getZ(), 22, 0.55, 0.65, 0.55, 0.08);
      level.playSound(null, entity.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.0F, 0.55F);
   }

   private static boolean isInterceptableProjectile(LancelotBerserkerEntity entity, Entity candidate) {
      if (candidate == entity || !candidate.isAlive()) return false;
      if (candidate instanceof GilgameshGateWeaponProjectileEntity gate) {
         LivingEntity owner = gate.getOwnerEntity();
         return owner != null && owner != entity && !owner.isAlliedTo(entity) && !ServantMasterProtection.isProtectedMaster(entity, owner);
      }
      if (candidate instanceof Projectile projectile) {
         Entity owner = projectile.getOwner();
         return owner instanceof LivingEntity living && living != entity && !living.isAlliedTo(entity)
            && !ServantMasterProtection.isProtectedMaster(entity, living);
      }
      return candidate instanceof ThrowableItemProjectile;
   }

   private static ItemStack stackForProjectile(Entity projectile, LancelotBerserkerEntity owner) {
      if (projectile instanceof ThrownTrident trident) {
         return knightOfOwnerStack(trident.getPickupItemStackOrigin().copy(), owner);
      }
      if (projectile instanceof AbstractArrow arrow) {
         return knightOfOwnerStack(arrow.getPickupItemStackOrigin().copy(), owner);
      }
      if (projectile instanceof ThrowableItemProjectile thrown) {
         ItemStack stack = thrown.getItem().copy();
         if (!stack.isEmpty()) return knightOfOwnerStack(stack, owner);
      }
      if (projectile instanceof GilgameshGateWeaponProjectileEntity gate) {
         return knightOfOwnerStack(gilgameshWeaponStack(gate.getWeaponId()), owner);
      }
      return knightOfOwnerStack(new ItemStack(ModItems.LANCELOT_IRON_ROD.get()), owner);
   }

   public static ItemStack gilgameshWeaponStack(String weaponId) {
      return switch (weaponId == null ? "" : weaponId) {
         case "durandal" -> new ItemStack(ModItems.GILGAMESH_DURANDAL.get());
         case "gram" -> new ItemStack(ModItems.GILGAMESH_GRAM.get());
         case "harpe" -> new ItemStack(ModItems.GILGAMESH_HARPE.get());
         case "vajra" -> new ItemStack(ModItems.GILGAMESH_VAJRA.get());
         case "fangtian_huaji" -> new ItemStack(ModItems.GILGAMESH_FANGTIAN_HUAJI.get());
         case "pseudo_spiral_sword" -> new ItemStack(ModItems.GILGAMESH_SPIRAL_SWORD.get());
         case "gae_bulg" -> new ItemStack(ModItems.GILGAMESH_GAE_BULG.get());
         default -> new ItemStack(ModItems.LANCELOT_IRON_ROD.get());
      };
   }

   public static void spawnKnightOfOwnerGateCounter(ServerLevel level, LivingEntity owner, LivingEntity target, Vec3 start, Vec3 direction,
                                                     GilgameshGateWeaponProjectileEntity original) {
      GilgameshGateWeaponProjectileEntity counter = new GilgameshGateWeaponProjectileEntity(
         level, owner, start, direction, original.getWeaponId(), Math.max(28.0F, original.getDamage()));
      counter.setSourceStyle(GilgameshGateWeaponProjectileEntity.SOURCE_STYLE_LANCELOT);
      counter.setEmpowered(original.isEmpowered());
      counter.setEffectStride(1);
      counter.setHomingTarget(target);
      level.addFreshEntity(counter);
   }

   private static LivingEntity scanNearestEnemy(LancelotBerserkerEntity entity, ServerLevel level) {
      AABB area = entity.getBoundingBox().inflate(24.0);
      return level.getEntitiesOfClass(LivingEntity.class, area,
         candidate -> candidate != entity && candidate.isAlive() && !candidate.isAlliedTo(entity)
            && !EntityUtils.isImmunePlayerTarget(candidate) && !ServantMasterProtection.isProtectedMaster(entity, candidate)
            && EntityUtils.isValidCombatTarget(entity, candidate)).stream()
         .min(Comparator.comparingDouble(entity::distanceToSqr))
         .orElse(null);
   }

   private static void tickBlessingBuff(LancelotBerserkerEntity entity, long now) {
      CompoundTag data = entity.getPersistentData();
      if (now > data.getLong(BATTLE_KILL_BUFF_UNTIL_TAG)) {
         data.remove(BATTLE_KILLS_TAG);
         removeModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), KILL_BUFF_DAMAGE_ID);
         return;
      }
      updateKillBuff(entity, data.getInt(BATTLE_KILLS_TAG));
   }

   private static void updateKillBuff(LancelotBerserkerEntity entity, int kills) {
      if (kills <= 0) return;
      updateModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), KILL_BUFF_DAMAGE_ID, 0.10 * Math.min(5, kills), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
   }

   private static void applyAroundightModifiers(LivingEntity entity) {
      syncAroundightHealthBeforeApply(entity);
      double bonus = AROUNDIGHT_CORE_ATTRIBUTE_MULTIPLIER - 1.0;
      updateModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), AROUNDIGHT_DAMAGE_ID, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      updateModifier(entity.getAttribute(Attributes.MAX_HEALTH), AROUNDIGHT_HEALTH_ID, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      updateModifier(entity.getAttribute(Attributes.ARMOR), AROUNDIGHT_ARMOR_ID, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      updateModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), AROUNDIGHT_SPEED_ID, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      updateModifier(entity.getAttribute(Attributes.ATTACK_SPEED), AROUNDIGHT_ATTACK_SPEED_ID, 0.75, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
   }

   private static void removeAroundightModifiers(LivingEntity entity) {
      removeModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), AROUNDIGHT_DAMAGE_ID);
      removeModifier(entity.getAttribute(Attributes.MAX_HEALTH), AROUNDIGHT_HEALTH_ID);
      removeModifier(entity.getAttribute(Attributes.ARMOR), AROUNDIGHT_ARMOR_ID);
      removeModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), AROUNDIGHT_SPEED_ID);
      removeModifier(entity.getAttribute(Attributes.ATTACK_SPEED), AROUNDIGHT_ATTACK_SPEED_ID);
      entity.getPersistentData().remove(AROUNDIGHT_HEALTH_SYNCED_TAG);
      if (entity.getHealth() > entity.getMaxHealth()) {
         entity.setHealth(entity.getMaxHealth());
      }
   }

   private static void syncAroundightHealthBeforeApply(LivingEntity entity) {
      CompoundTag data = entity.getPersistentData();
      if (data.getBoolean(AROUNDIGHT_HEALTH_SYNCED_TAG)) return;
      float oldHealth = entity.getHealth();
      double oldMaxHealth = entity.getMaxHealth();
      double bonus = AROUNDIGHT_CORE_ATTRIBUTE_MULTIPLIER - 1.0;
      updateModifier(entity.getAttribute(Attributes.MAX_HEALTH), AROUNDIGHT_HEALTH_ID, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      if (oldMaxHealth > 0.0) {
         entity.setHealth(Math.min(entity.getMaxHealth(), (float)(oldHealth * entity.getMaxHealth() / oldMaxHealth)));
      }
      data.putBoolean(AROUNDIGHT_HEALTH_SYNCED_TAG, true);
   }

   private static void updateModifier(AttributeInstance attribute, ResourceLocation id, double value, AttributeModifier.Operation operation) {
      if (attribute == null) return;
      attribute.removeModifier(id);
      attribute.addTransientModifier(new AttributeModifier(id, value, operation));
   }

   private static void removeModifier(AttributeInstance attribute, ResourceLocation id) {
      if (attribute != null) attribute.removeModifier(id);
   }

   private static void spawnModeParticles(LancelotBerserkerEntity entity, ServerLevel level) {
      if ((entity.tickCount & 3) != 0) return;
      if (isAroundightMode(entity)) {
         level.sendParticles(ParticleTypes.ENCHANT, entity.getX(), entity.getY() + 1.0, entity.getZ(), 4, 0.45, 0.65, 0.45, 0.03);
      } else {
         level.sendParticles(DARK_DUST, entity.getX(), entity.getY() + 0.9, entity.getZ(), 16, 0.62, 0.82, 0.62, 0.055);
         level.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + 0.85, entity.getZ(), 5, 0.5, 0.62, 0.5, 0.035);
         if ((entity.tickCount & 7) == 0) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, entity.getX(), entity.getY() + 0.85, entity.getZ(), 4, 0.42, 0.58, 0.42, 0.025);
         }
      }
   }

   private static boolean isSlamTarget(LancelotBerserkerEntity entity, LivingEntity target) {
      return target != entity && target.isAlive()
         && !EntityUtils.isImmunePlayerTarget(target)
         && !entity.isAlliedTo(target) && !target.isAlliedTo(entity)
         && !ServantMasterProtection.isProtectedMaster(entity, target)
         && EntityUtils.isValidCombatTarget(entity, target);
   }

   private static void spawnKnightHit(LivingEntity owner, LivingEntity target) {
      if (owner.level() instanceof ServerLevel level) {
         Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
         level.sendParticles(DARK_DUST, center.x, center.y, center.z, 18, 0.38, 0.35, 0.38, 0.08);
         level.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z, 8, 0.32, 0.25, 0.32, 0.08);
      }
   }

   private static void spawnAroundightHit(LivingEntity owner, LivingEntity target) {
      if (owner.level() instanceof ServerLevel level) {
         Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
         level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y, center.z, 22, 0.45, 0.4, 0.45, 0.1);
         level.sendParticles(PURPLE_DUST, center.x, center.y, center.z, 10, 0.35, 0.32, 0.35, 0.06);
      }
   }
}
